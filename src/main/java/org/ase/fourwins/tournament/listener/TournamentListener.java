package org.ase.fourwins.tournament.listener;

import org.ase.fourwins.game.Game;

public interface TournamentListener<T> {

	default void gameStarted(Game game) {
	}

	default void gameEnded(Game game) {
	}

	default T getResult() {
		return null;
	}

}
