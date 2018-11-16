package org.ase.fourwins.game;

import java.util.List;

import org.ase.fourwins.board.Board.GameState;

public interface Game {

	Game runGame();

	GameState gameState();

	List<Player> getPlayers();

	default Player getPlayerForToken(Object token) {
		return getPlayers().stream()
				.filter(player -> player.getToken().equals(token)).findFirst()
				.orElseThrow(() -> new RuntimeException(
						"Token " + token + " not part of the game."));
	}

	default Player getOpponentForToken(Object token) {
		return getPlayers().stream()
				.filter(player -> !player.getToken().equals(token)).findFirst()
				.orElseThrow(() -> new RuntimeException(
						"Token " + token + " not part of the game."));

	}

}
