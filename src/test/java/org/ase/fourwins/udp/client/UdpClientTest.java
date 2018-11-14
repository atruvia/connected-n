package org.ase.fourwins.udp.client;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import org.ase.fourwins.udp.client.UdpClientTest.FourWinsBot;
import org.ase.fourwins.udp.udphelper.UdpCommunicator;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import lombok.RequiredArgsConstructor;

public class UdpClientTest {

	@RequiredArgsConstructor
	public static class FourWinsBot {

		private final String name;
		private UdpCommunicator udpCommunicator;

		public FourWinsBot connect(String remoteHost, int remotePort) throws IOException {
			udpCommunicator = addShutdownHook(new UdpCommunicator(remoteHost, remotePort));
//			udpCommunicator.addMessageListener(bot);
			udpCommunicator.listenForMessages();
			return this;
		}

		private static UdpCommunicator addShutdownHook(final UdpCommunicator communicator) {
			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					communicator.stop();
				}
			});
			return communicator;
		}

	}

	@Test
	@Disabled
	void failsIfServerNotRunning() {
		FourWinsBot fourWinsBot = new FourWinsBot("myname");
		assertThrows(IOException.class, () -> fourWinsBot.connect("localhost", 12345));
	}

}
