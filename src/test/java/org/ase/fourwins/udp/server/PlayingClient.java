package org.ase.fourwins.udp.server;

import java.io.IOException;

import org.ase.fourwins.udp.server.UdpServerTest.DummyClient;

final class PlayingClient extends DummyClient {
	private final int row;

	PlayingClient(String name, String remoteHost, int remotePort, int row) throws IOException {
		super(name, remoteHost, remotePort);
		this.row = row;
	}

	@Override
	protected void messageReceived(String received) {
		super.messageReceived(received);
		if (received.startsWith("NEW SEASON;")) {
			trySend("JOIN;" + received.split(";")[1]);
		} else if (received.startsWith("YOURTURN;")) {
			trySend("INSERT;" + row + ";" + received.split(";")[1]);
		}
	}
}