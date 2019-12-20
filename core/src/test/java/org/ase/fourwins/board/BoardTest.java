package org.ase.fourwins.board;

import static org.ase.fourwins.board.BoardMatcher.isDraw;
import static org.ase.fourwins.board.BoardMatcher.isGameError;
import static org.ase.fourwins.board.BoardMatcher.isStillInGame;
import static org.ase.fourwins.board.BoardMatcher.winnerIs;
import static org.ase.fourwins.board.BoardTest.BoardBuilder.boardOfSize;
import static org.ase.fourwins.board.Move.column;
import static org.ase.fourwins.board.Move.moveToColumn;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.ase.fourwins.board.Board.MoveListener;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class BoardTest {

	@Test
	void no_score_when_board_still_can_be_played() {
		Board board = a(boardOfSize(1, 1));
		assertThat(board, isStillInGame());
	}

	@Test
	void boardIsFull_but_no_4_in_a_row_1x1() {
		Board board = a(boardOfSize(1, 1).filled("X"));
		assertThat(board, isDraw());
	}

	@Test
	void inserting_token_into_filled_column() {
		Board board = a(boardOfSize(2, 2).filled( //
				"X", //
				"X" //
		));
		assertThat(board.insertToken(moveToColumn(0), "O"), isGameError("COLUMN_IS_FULL").withToken("O"));
	}

	@Test
	void boardIsFull_but_no_4_in_a_row_1x2() {
		Board board = a(boardOfSize(1, 2).filled( //
				"X", //
				"X" //
		));
		assertThat(board, isDraw());
	}

	@Test
	void boardIsFull_but_no_4_in_a_row_2x1() {
		Board board = a(boardOfSize(2, 1).filled("X X"));
		assertThat(board, isDraw());
	}

	@Test
	void player_X_wins_when_having_four_in_a_row() {
		Board board = a(boardOfSize(1, 4).filled( //
				"X", //
				"X", //
				"X", //
				"X" //
		));
		assertThat(board, winnerIs("X"));
	}

	@Test
	void boardIsFull_but_no_4_in_a_row_1x5() {
		Board board = a(boardOfSize(1, 5).filled( //
				"X", //
				"O", //
				"X", //
				"X", //
				"X" //
		));
		assertThat(board, isDraw());
	}

	@Test
	void player_X_wins_when_completing_the_row() {
		Board board = a(boardOfSize(4, 1).filled("X   X X"));
		assertThat(board.insertToken(moveToColumn('B'), "X"), winnerIs("X"));
	}

	@Test
	void player_X_wins_when_completing_the_diagnoal_row_topleft_to_rightbottom() {
		Board board = a(boardOfSize(4, 4).filled( //
				"X O   O", //
				"O X   O", //
				"O O   O", //
				"O O O X" //
		));
		assertThat(board.insertToken(moveToColumn('C'), "X"), winnerIs("X"));
	}

	@Test
	void player_X_wins_when_completing_the_diagnoal_row_bottomleft_to_righttop() {
		Board board = a(boardOfSize(4, 4).filled( //
				"O   O X", //
				"O   X O", //
				"O   O O", //
				"X O O O" //
		));
		assertThat(board.insertToken(moveToColumn('B'), "X"), winnerIs("X"));
	}

	@Test
	void player_X_wins_when_diagonal() {
		Board board = a(boardOfSize(7, 6).filled( //
				"X   O X", //
				"O X X O", //
				"O O X O", //
				"O O O X", //
				"X O O X" //
		));
		assertThat(board, winnerIs("X"));
	}

	@Test
	void a_no_more_playable_board_cannot_be_modified_anymore_and_loser_is_held() {
		Board board = a(boardOfSize(1, 1).filled( //
				"X", //
				"X" //
		));
		assertThat(board.insertToken(moveToColumn('A'), "other"), isGameError("COLUMN_IS_FULL").withToken("X"));
	}

	@Test
	void out_of_tbound() {
		Board board = a(boardOfSize(1, 1));
		assertThat(board.insertToken(moveToColumn('B'), "X"), isGameError("ILLEGAL_COLUMN_ANNOUNCED").withToken("X"));
	}

	@Test
	void multiple_ways_of_success() {
		Board board = a(boardOfSize(4, 4).filled( //
				"X X X  ", //
				"O O X O", //
				"O X O O", //
				"X O O O" //
		));
		assertThat(board.insertToken(moveToColumn('D'), "X"), winnerIs("X").withCombinations(2));
	}

	@Test
	@Disabled
	void todo_fr_what_should_winning_combination_contain() {
		Board board = a(boardOfSize(8, 1).filled("X X X   X X X"));
		assertThat(board.insertToken(moveToColumn('D'), "X"), winnerIs("X"));
		fail("what should the winning combination look alike?"); // TODO FR
	}

	@Test
	void testMoveListenerIsCalled() {
		MoveListener moveListener = mock(MoveListener.class);
		a(boardOfSize(3, 2, moveListener)) //
				.insertToken(moveToColumn('B'), "X") //
				.insertToken(moveToColumn('C'), "O") //
				.insertToken(moveToColumn('C'), "X");
		verify(moveListener, times(1)).newTokenAt("X", column('B'), 0);
		verify(moveListener, times(1)).newTokenAt("O", column('C'), 0);
		verify(moveListener, times(1)).newTokenAt("X", column('C'), 1);
	}

	public static class BoardBuilder {

		private Board board;

		public BoardBuilder(Board board) {
			this.board = board;
		}

		private BoardBuilder filled(String... lines) {
			for (String line : reverse(Arrays.asList(lines))) {
				for (int i = 0; i < line.length(); i += 2) {
					char token = line.charAt(i);
					board = ' ' == token ? board : board.insertToken(moveToColumn(i / 2), String.valueOf(token));
				}
			}
			return this;
		}

		private List<String> reverse(List<String> lines) {
			Collections.reverse(lines);
			return lines;
		}

		public static BoardBuilder boardOfSize(int cols, int rows, MoveListener moveListener) {
			return new BoardBuilder(Board.newBoard(new BoardInfo(cols, rows), moveListener));
		}

		public static BoardBuilder boardOfSize(int cols, int rows) {
			return new BoardBuilder(Board.newBoard(new BoardInfo(cols, rows)));
		}

		public Board build() {
			return board;
		}
	}

	private Board a(BoardBuilder builder) {
		return builder.build();
	}

}
