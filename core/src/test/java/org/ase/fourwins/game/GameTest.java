package org.ase.fourwins.game;

import static org.ase.fourwins.board.Board.Score.DRAW;
import static org.ase.fourwins.board.Board.Score.LOSE;
import static org.ase.fourwins.board.Board.Score.WIN;
import static org.ase.fourwins.board.BoardInfo.builder;
import static org.ase.fourwins.board.GameStateMatcher.winnerIs;
import static org.ase.fourwins.game.Game.GameId.random;
import static org.ase.fourwins.game.GameTestUtil.player;
import static org.ase.fourwins.game.GameTestUtil.withMoves;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.ase.fourwins.board.Board;
import org.ase.fourwins.board.BoardInfo;
import org.ase.fourwins.board.mockplayers.ColumnTrackingMockPlayer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class GameTest {

	static final class NoMovePlayer extends Player {
		NoMovePlayer(String token) {
			super(token);
		}

		@Override
		protected int nextColumn() {
			throw new IllegalStateException();
		}
	}

	@Test
	void bothPlayersCanInsertTokensTwoTokensEach() {
		Player firstPlayer = player("X", withMoves(0, 0));
		Player secondPlayer = player("O", withMoves(1, 1));
		Game game = runGame(builder().columns(2).rows(2), firstPlayer, secondPlayer);
		assertThat(game.gameState().getScore(), is(DRAW));
	}

	@Test
	void firstPlayerWins() {
		Player firstPlayer = player("X", withMoves(0, 0, 0, 0));
		Player secondPlayer = player("O", withMoves(1, 1, 1, 1));
		Game game = runGame(builder().columns(2).rows(4), firstPlayer, secondPlayer);
		assertThat(game.gameState().getScore(), is(WIN));
		assertThat(game.gameState().getToken(), is("X"));
	}

	@Test
	void secondPlayerWins() {
		Player firstPlayer = player("X", withMoves(0, 0, 0, 2));
		Player player2 = player("O", withMoves(1, 1, 1, 1));
		Game game = runGame(builder().columns(3).rows(4), firstPlayer, player2);
		assertThat(game.gameState().getScore(), is(WIN));
		assertThat(game.gameState().getToken(), is("O"));
	}

	@Test
	void twoIdenticalPlayerTokensAreNotValid() {
		String token = "someToken";
		Player firstPlayer = new NoMovePlayer(token);
		Player secondPlayer = new NoMovePlayer(token);
		RuntimeException rte = assertThrows(RuntimeException.class, () -> {
			makeGame(BoardInfo.builder().columns(2).rows(2), firstPlayer, secondPlayer);
		});
		assertThat(rte.getMessage(), containsString(token));
	}

	@Test
	void notTheStartingPlayerWillWin() {
		Player firstPlayer = new ColumnTrackingMockPlayer("X") {
			@Override
			public int calcNextColumn() {
				return boardIsEmpty() ? 0 : super.calcNextColumn();
			}
		};
		Player secondPlayer = new ColumnTrackingMockPlayer("O");
		Game game = runGame(BoardInfo.sevenColsSixRows, firstPlayer, secondPlayer);
		assertThat(game.gameState(), winnerIs("O"));
	}

	@Test
	void aPlayerThrowingAnExceptionWillLoseTheGame() {
		String message = "the exception text";
		Player firstPlayer = new Player("X") {
			@Override
			protected int nextColumn() {
				throw new RuntimeException(message);
			}
		};
		Player secondPlayer = player("O", withMoves());
		Game game = runGame(builder().columns(2).rows(2), firstPlayer, secondPlayer);
		assertThat(game.gameState().getScore(), is(LOSE));
		assertThat(game.gameState().getReason(), is(message));
	}

	@Test
	void cantHaveTwoPlayersWithSameToken() {
		Player firstPlayer = player("X", withMoves());
		Player secondPlayer = player("X", withMoves());
		RuntimeException exception = assertThrows(RuntimeException.class, () -> {
			makeGame(BoardInfo.builder().columns(1).rows(1), firstPlayer, secondPlayer);
		});
		assertThat(exception.getMessage(), containsString("same tokens X"));
	}

	private Game runGame(BoardInfo.BoardInfoBuilder boardInfo, Player... players) {
		return runGame(boardInfo.build(), players);
	}

	private Game runGame(BoardInfo boardInfo, Player... players) {
		return makeGame(boardInfo, players).runGame();
	}

	private Game makeGame(BoardInfo.BoardInfoBuilder boardInfo, Player... players) {
		return makeGame(boardInfo.build(), players);
	}

	private Game makeGame(BoardInfo build, Player... players) {
		return new DefaultGame(Board.newBoard(build), random(), players);
	}

}
