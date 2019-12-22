package org.ase.fourwins.tournament.listener.database;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.stream.IntStream;

import org.ase.fourwins.board.Board.GameState;
import org.ase.fourwins.board.Board.Score;
import org.ase.fourwins.board.mockplayers.PlayerMock;
import org.ase.fourwins.game.Game;
import org.ase.fourwins.game.Player;

public final class Games {

	private Games() {
		super();
	}

	static Game aGameOf(List<Player> players, Score score, int lastPlayer) {
		return new Game() {
	
			@Override
			public Game runGame() {
				throw new UnsupportedOperationException();
			}
	
			@Override
			public GameState gameState() {
				return GameState.builder().token(players.get(lastPlayer).getToken()).score(score).build();
			}
	
			@Override
			public List<Player> getPlayers() {
				return players;
			}
		};
	}

	static List<Player> players(int count) {
		return IntStream.range(1, 1 + count).mapToObj(i -> new PlayerMock("P" + i)).collect(toList());
	}

}
