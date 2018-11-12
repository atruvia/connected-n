package org.ase.fourwins.game;

import static org.ase.fourwins.board.Board.Score.DRAW;
import static org.ase.fourwins.board.Board.Score.WIN;
import static org.ase.fourwins.board.BoardInfo.sevenColsSixRows;
import static org.ase.fourwins.board.GameStateMatcher.winnerIs;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.ase.fourwins.board.Board;
import org.ase.fourwins.board.mockplayers.ColumnTrackingMockPlayer;
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
		Game game = new DefaultGame(firstPlayer, secondPlayer, Board.newBoard(2, 2)).runGame();
		assertThat(game.gameState().getScore(), is(DRAW));
	}

	@Test
	void firstPlayerWins() {
		Player firstPlayer = player("X", withMoves(0, 0, 0, 0));
		Player secondPlayer = player("O", withMoves(1, 1, 1, 1));
		Game game = new DefaultGame(firstPlayer, secondPlayer, Board.newBoard(2, 4)).runGame();
		assertThat(game.gameState().getScore(), is(WIN));
		assertThat(game.gameState().getToken(), is("X"));
	}

	@Test
	void secondPlayerWins() {
		Player firstPlayer = player("X", withMoves(0, 0, 0, 2));
		Player player2 = player("O", withMoves(1, 1, 1, 1));
		Game game = new DefaultGame(firstPlayer, player2, Board.newBoard(3, 4)).runGame();
		assertThat(game.gameState().getScore(), is(WIN));
		assertThat(game.gameState().getToken(), is("O"));
	}

	@Test
	void twoIdenticalPlayerTokensAreNotValid() {
		String token = "someToken";
		Player firstPlayer = new NoMovePlayer(token);
		Player secondPlayer = new NoMovePlayer(token);
		assertThrows(RuntimeException.class, () -> {
			new DefaultGame(firstPlayer, secondPlayer, Board.newBoard(2, 2));
		});
	}

	@Test
	// TODO choosing which starting column does the first player win?
	void notTheStartingPlayerWillWin() {
		Player firstPlayer = new ColumnTrackingMockPlayer("X") {
			@Override
			public int calcNextColumn() {
				return boardIsEmpty() ? 0 : super.calcNextColumn();
			}
		};
		Player secondPlayer = new ColumnTrackingMockPlayer("O");
		Game game = new DefaultGame(firstPlayer, secondPlayer, Board.newBoard(sevenColsSixRows)).runGame();
		assertThat(game.gameState(), winnerIs("O"));
	}

	private static List<Integer> withMoves(Integer... moves) {
		return Arrays.asList(moves);
	}

	protected Player player(String token, List<Integer> columns) {
		Iterator<Integer> moves = columns.iterator();
		return new Player(token) {
			protected int nextColumn() {
				return moves.next();
			}
		};
	}

}
