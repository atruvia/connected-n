package org.ase.fourwins.udp.server;

import static java.time.Duration.ofSeconds;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.ase.fourwins.tournament.DefaultTournament;
import org.ase.fourwins.tournament.Tournament;
import org.ase.fourwins.udp.server.UdpServerTest.DummyClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class UdpServerRealTournamentIT {

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
			DummyClient client1 = playingClient("1", 0);
			DummyClient client2 = playingClient("2", 1);

			// directly after client2 is instantiated the tournament will start

			System.out.println(client1.waitUntilReceived(2 + 4));
			System.out.println(client2.waitUntilReceived(2 + 3));

			/// ...let it run for a while
			TimeUnit.SECONDS.sleep(5);

			fail("add more assertions");

			client1.unregister();
			client2.unregister();
		});
	}
	
	// TODO MISSING! message of inserted token (own and other)
	
	// TODO what about moves <non-integer>;<token>
	// TODO Test timeout


	private DummyClient playingClient(String name, int row) throws IOException {
		return new DummyClient(name, "localhost", serverPort) {
			@Override
			protected void messageReceived(String received) {
				super.messageReceived(received);
				if (received.startsWith("YOURTURN;")) {
					trySend("INSERT;" + row + ";" + received.split(";")[1]);
				}
			}
		};
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
