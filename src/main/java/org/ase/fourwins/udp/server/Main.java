package org.ase.fourwins.udp.server;

import org.ase.fourwins.tournament.DefaultTournament;
import org.ase.fourwins.tournament.listener.TournamentScoreListener;

public class Main {

	public static void main(String[] args) {
		DefaultTournament tournament = new DefaultTournament();
		TournamentScoreListener scoreListener = new TournamentScoreListener();
		tournament.addTournamentListener(scoreListener);

		UdpServer server = new UdpServer(4446, tournament);
		server.startServer();
	}

}
