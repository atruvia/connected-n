package org.ase.fourwins.game;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.ase.fourwins.board.Board.GameState;
import org.ase.fourwins.board.BoardInfo;

import lombok.Value;

public interface Game {

	@Value
	public class GameId {

		private final String gameId;

		public static GameId random() {
			return new GameId(UUID.randomUUID().toString());
		}
		
		@Override
		public String toString() {
			return gameId;
		}

	}

	Game runGame();

	GameState gameState();

	List<Player> getPlayers();

	GameId getId();

	BoardInfo getBoardInfo();

	default Player getPlayerForToken(Object token) {
		return getPlayers().stream().filter(player -> player.getToken().equals(token)).findFirst()
				.orElseThrow(() -> new RuntimeException("Token " + token + " not part of the game."));
	}

	default Stream<Player> getOpponentsForToken(Object token) {
		return getPlayers().stream().filter(player -> !player.getToken().equals(token));

	}

}
