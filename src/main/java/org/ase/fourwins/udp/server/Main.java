package org.ase.fourwins.udp.server;

import java.sql.SQLException;
import java.util.ServiceLoader;

import org.ase.fourwins.tournament.DefaultTournament;
import org.ase.fourwins.tournament.Tournament;
import org.ase.fourwins.tournament.listener.TournamentListener;

public class Main {

	public static void main(String[] args) throws ClassNotFoundException, SQLException {
		new UdpServer(4446, addListeners(new DefaultTournament())).startServer();
	}

	private static Tournament addListeners(Tournament tournament) {
		ServiceLoader.load(TournamentListener.class).forEach(tournament::addTournamentListener);
		return tournament;
	}

}
