package org.ase.fourwins.udp.server;

public class Main {

	public static void main(String[] args) {
		UdpServer server = new UdpServer(4446);
		server.startServer();
	}

}
