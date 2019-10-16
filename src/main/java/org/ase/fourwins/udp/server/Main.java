package org.ase.fourwins.udp.server;

import java.sql.SQLException;

import org.ase.fourwins.tournament.DefaultTournament;
import org.ase.fourwins.tournament.listener.TournamentScoreListener;
import org.ase.fourwins.tournament.listener.database.MysqlDBListener;

public class Main {

	public static void main(String[] args) throws ClassNotFoundException, SQLException {
		DefaultTournament tournament = new DefaultTournament();
		tournament.addTournamentListener(new TournamentScoreListener());
		tournament.addTournamentListener(new MysqlDBListener("jdbc:mysql://db:3306/4WINS", "root", "fourWinsPwd"));
		UdpServer server = new UdpServer(4446, tournament);
		server.startServer();
	}

}
