package org.ase.fourwins.listener;

import static java.util.stream.Collectors.toList;
import static org.ase.fourwins.board.Board.Score.LOSE;
import static org.ase.fourwins.board.Board.Score.WIN;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;

import org.ase.fourwins.board.Board.GameState;
import org.ase.fourwins.board.BoardInfo;
import org.ase.fourwins.game.Game;
import org.ase.fourwins.game.Player;
import org.junit.jupiter.api.Test;

import com.github.stefanbirkner.systemlambda.SystemLambda;

class SysoutTournamentListenerTest {

	private final SysoutTournamentListener sut = new SysoutTournamentListener();

	@Test
	void canCount() throws Exception {
		String sysout = playSeasonOfTwoGames();
		assertThat(sysout, is("Season ended, games won: {X=2, Y=1}\n"));
	}

	@Test
	void doesReset() throws Exception {
		String sysout;
		sysout = playSeasonOfTwoGames();
		sysout = playSeasonOfTwoGames();
		assertThat(sysout, is("Season ended, games won: {X=2, Y=1}\n"));
	}

	private String playSeasonOfTwoGames() throws Exception {
		sut.seasonStarted();
		sut.gameEnded(game(GameState.builder().score(WIN).token("X").build(), "X", "Y", "Z"));
		sut.gameEnded(game(GameState.builder().score(LOSE).token("Z").build(), "X", "Y", "Z"));
		return SystemLambda.tapSystemOut(sut::seasonEnded);
	}

	private Game game(GameState gameState, String... tokens) {
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
				return Arrays.stream(tokens).map(t -> new Player(t) {
					@Override
					protected int nextColumn() {
						throw new IllegalStateException();
					}
				}).collect(toList());
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
