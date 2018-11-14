package org.ase.fourwins.udp.server;

import static java.time.Duration.ofSeconds;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.ServerSocket;

import org.ase.fourwins.tournament.DefaultTournament;
import org.ase.fourwins.tournament.Tournament;
import org.ase.fourwins.udp.server.UdpServerTest.DummyClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class UdpServerTestRealTournament {

	private final int serverPort = freePort();

	private final Tournament tournament = new DefaultTournament();

	private final UdpServer sut = udpServerInBackground();

	private UdpServer udpServerInBackground() {
		UdpServer udpServer = new UdpServer(serverPort, tournament);
		runInBackground(() -> udpServer.startServer());
		return udpServer;
	}

	private static void runInBackground(Runnable runnable) {
		new Thread(runnable).start();
	}

	@AfterEach
	public void tearDown() {
//		sut.shutdown();
	}

	@Test
	void canPlay() throws IOException, InterruptedException {
		assertTimeout(ofSeconds(10), () -> {
			DummyClient client1 = new DummyClient("1", "localhost", serverPort) {
				@Override
				protected void messageReceived(String received) {
					super.messageReceived(received);
					if (received.startsWith("YOURTURN;")) {
						trySend("0;" + received.split(";")[1]);
					}
				}
			};

			DummyClient client2 = new DummyClient("2", "localhost", serverPort) {
				@Override
				protected void messageReceived(String received) {
					super.messageReceived(received);
					if (received.startsWith("YOURTURN;")) {
						trySend("1;" + received.split(";")[1]);
					}
				}
			};
			
			// directly after client2 is instantiated the tournament will start 

			System.out.println(client1.waitUntilReceived(1 + 4));
			System.out.println(client2.waitUntilReceived(1 + 3));

			fail("add more assertions");

			client1.unregister();
			client2.unregister();
		});
	}

	private static int freePort() {
		try {
			ServerSocket socket = new ServerSocket(0);
			socket.close();
			return socket.getLocalPort();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
