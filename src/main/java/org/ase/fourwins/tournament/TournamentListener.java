package org.ase.fourwins.tournament;

import org.ase.fourwins.game.Game;

public interface TournamentListener {

	default void gameStarted(Game game) {
	}

	default void gameEnded(Game game) {
	}

}
