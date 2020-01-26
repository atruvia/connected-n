package org.ase.fourwins.board;

import static org.ase.fourwins.board.Board.Score.DRAW;
import static org.ase.fourwins.board.Board.Score.IN_GAME;
import static org.ase.fourwins.board.Board.Score.LOSE;
import static org.ase.fourwins.board.Board.Score.WIN;

import org.ase.fourwins.board.Board.Score;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class BoardMatcher extends TypeSafeMatcher<Board> {

	private GameStateMatcher matcher = new GameStateMatcher();

	public static BoardMatcher isStillInGame() {
		return new BoardMatcher().score(IN_GAME);
	}

	public static BoardMatcher isDraw() {
		return scoreIs(DRAW);
	}

	public static BoardMatcher winnerIs(Object token) {
		return scoreIs(WIN).withToken(token).withReason("CONNECTED_ROW").withCombinations(1);
	}

	public static BoardMatcher loserIs(Object token) {
		BoardMatcher r = scoreIs(LOSE);
		r.matcher = r.matcher.withToken(token);
		return r;
	}

	public static BoardMatcher isGameError(String gameError) {
		return new BoardMatcher().error(gameError);
	}

	private static BoardMatcher scoreIs(Score score) {
		return new BoardMatcher().score(score);
	}

	@Override
	public void describeTo(Description description) {
		matcher.describeTo(description);
	}

	@Override
	protected void describeMismatchSafely(Board board, Description description) {
		matcher.describeMismatchSafely(board.gameState(), description);
	}

	@Override
	protected boolean matchesSafely(Board board) {
		return matcher.matchesSafely(board.gameState());
	}

	public BoardMatcher score(Score score) {
		matcher = matcher.score(score);
		return this;
	}

	private BoardMatcher error(String error) {
		matcher = matcher.error(error);
		return this;
	}

	public BoardMatcher withToken(Object token) {
		matcher = matcher.withToken(token);
		return this;
	}

	public BoardMatcher withReason(String reason) {
		matcher = matcher.withReason(reason);
		return this;
	}

	public BoardMatcher withCombinations(int combinations) {
		matcher = matcher.withCombinations(combinations);
		return this;
	}

}
