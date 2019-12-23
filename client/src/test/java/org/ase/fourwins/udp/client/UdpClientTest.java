package org.ase.fourwins.udp.client;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import org.ase.fourwins.client.udphelper.UdpCommunicator;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class UdpClientTest {

	public static class FourWinsBot {

		private UdpCommunicator udpCommunicator;

		public FourWinsBot connect(String remoteHost, int remotePort) throws IOException {
			udpCommunicator = addShutdownHook(new UdpCommunicator(remoteHost, remotePort));
//			udpCommunicator.addMessageListener(bot);
			udpCommunicator.listenForMessages();
			return this;
		}

		private static UdpCommunicator addShutdownHook(UdpCommunicator communicator) {
			Runtime.getRuntime().addShutdownHook(new Thread(() -> communicator.stop()));
			return communicator;
		}

	}

	@Test
	@Disabled
	void failsIfServerNotRunning() {
		FourWinsBot fourWinsBot = new FourWinsBot();
		assertThrows(IOException.class, () -> fourWinsBot.connect("localhost", aPortNoServerIsRunning()));
	}

	private int aPortNoServerIsRunning() {
		return 12345;
	}

}
