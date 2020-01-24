package org.ase.fourwins.udp.server;

import java.io.IOException;

import org.ase.fourwins.udp.server.UdpServerTest.DummyClient;

public class PlayingClient extends DummyClient {

	private final int column;

	public PlayingClient(String name, String remoteHost, int remotePort, int column) throws IOException {
		super(name, remoteHost, remotePort);
		this.column = column;
	}

	@Override
	protected void messageReceived(String received) {
		super.messageReceived(received);
		if (received.startsWith("NEW SEASON;")) {
			trySend("JOIN;" + received.split(";")[1]);
		} else if (received.startsWith("YOURTURN;")) {
			trySend("INSERT;" + column + ";" + received.split(";")[1]);
		}
	}

}