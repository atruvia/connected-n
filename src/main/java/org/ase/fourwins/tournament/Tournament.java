package org.ase.fourwins.tournament;

import java.util.stream.Stream;

import org.ase.fourwins.board.Board.GameState;
import org.ase.fourwins.game.Player;
import org.ase.fourwins.tournament.listener.TournamentListener;

public interface Tournament {

	public interface RegistrationResult {
		boolean isOk();
	}

	void addTournamentListener(TournamentListener listener);

	void removeTournamentListener(TournamentListener listener);

	Stream<GameState> playSeason();

	RegistrationResult registerPlayer(Player player);

	Tournament deregisterPlayer(Player player);

}