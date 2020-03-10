package org.ase.fourwins.listener;

import static com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOut;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.ase.fourwins.board.Board.Score.DRAW;
import static org.ase.fourwins.board.Board.Score.LOSE;
import static org.ase.fourwins.board.Board.Score.WIN;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.text.MessageFormat;
import java.util.List;

import org.ase.fourwins.board.Board.GameState;
import org.ase.fourwins.board.BoardInfo;
import org.ase.fourwins.game.Game;
import org.ase.fourwins.game.Player;
import org.junit.jupiter.api.Test;

class SysoutTournamentListenerTest {

	private final SysoutTournamentListener sut = new SysoutTournamentListener();

	@Test
	void canCount() throws Exception {
		String sysout = playSeasonOfTwoGames();
		assertThat(sysout, is(String.format("Season ended, games won: Z=2, Y=1\n" //
				+ "1: Z %1.1f\n" //
				+ "2: Y %1.1f\n" //
				+ "3: X %1.1f\n", //
				2.5, 1.5, 0.5)));
	}

	@Test
	void doesReset() throws Exception {
		String sysout = null;
		int seasons = 2;
		for (int i = 0; i < seasons; i++) {
			sysout = playSeasonOfTwoGames();
		}
		assertThat(sysout, is(String.format("Season ended, games won: Z=2, Y=1\n" //
				+ "1: Z %1.1f\n" //
				+ "2: Y %1.1f\n" //
				+ "3: X %1.1f\n", //
				2.5 * seasons, 1.5 * seasons, 0.5 * seasons)));
	}

	private String playSeasonOfTwoGames() throws Exception {
		sut.seasonStarted();
		sut.gameEnded(game(GameState.builder().score(WIN).token("Z").build(), "X", "Y", "Z"));
		sut.gameEnded(game(GameState.builder().score(DRAW).token("Y").build(), "X", "Y", "Z"));
		sut.gameEnded(game(GameState.builder().score(LOSE).token("X").build(), "X", "Y", "Z"));
		return tapSystemOut(sut::seasonEnded);
	}

	private Player newPlayer(String name) {
		return new Player(name) {
			@Override
			protected int nextColumn() {
				throw new IllegalStateException();
			}
		};
	}

	private Game game(GameState gameState, String... tokens) {
		List<Player> players = stream(tokens).map(this::newPlayer).collect(toList());
		return new Game() {

			@Override
			public Game runGame() {
				throw new IllegalStateException();
			}

			@Override
			public GameState gameState() {
				return gameState;
			}

			@Override
			public List<Player> getPlayers() {
				return players;
			}

			@Override
			public GameId getId() {
				throw new IllegalStateException();
			}

			@Override
			public BoardInfo getBoardInfo() {
				throw new IllegalStateException();
			}
		};
	}

}
