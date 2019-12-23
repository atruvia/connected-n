package org.ase.fourwins.client;

import java.io.IOException;

import org.ase.fourwins.client.udphelper.MessageListener;
import org.ase.fourwins.client.udphelper.UdpCommunicator;

public class Main {

	public static void main(String[] args) throws IOException {
		String serverHost = "localhost";
		int serverPort = 4446;
		String botname = "bot1";
		System.out.println("Starting ... ");
		UdpCommunicator communicator = new UdpCommunicator(serverHost, serverPort);
		MessageListener bot = new SimpleBot(botname, communicator.getMessageSender());
		communicator.addMessageListener(bot);
		communicator.listenForMessages();
	}

}
