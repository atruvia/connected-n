package org.ase.fourwins.board;

import static org.ase.fourwins.board.Board.Score.DRAW;
import static org.ase.fourwins.board.Board.Score.IN_GAME;
import static org.ase.fourwins.board.Board.Score.LOSE;
import static org.ase.fourwins.board.Board.Score.WIN;

import java.util.Objects;

import org.ase.fourwins.board.Board.GameState;
import org.ase.fourwins.board.Board.Score;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class GameStateMatcher extends TypeSafeMatcher<GameState> {

	private GameState expected = GameState.builder().build();
	private int combinations;

	public static GameStateMatcher isStillInGame() {
		return scoreIs(IN_GAME);
	}

	public static GameStateMatcher isDraw() {
		return scoreIs(DRAW);
	}

	public static GameStateMatcher winnerIs(Object token) {
		return scoreIs(WIN).withToken(token).withReason("four in a row").withCombinations(1);
	}

	public GameStateMatcher withCombinations(int combinations) {
		this.combinations = combinations;
		return this;
	}

	public static GameStateMatcher loserIs(Object token) {
		return scoreIs(LOSE).withToken(token);
	}

	public static GameStateMatcher isGameError(String gameError) {
		return new GameStateMatcher().error(gameError);
	}

	private static GameStateMatcher scoreIs(Score score) {
		return new GameStateMatcher().score(score);
	}

	@Override
	public void describeTo(Description description) {
		append(description, expected.getScore(), expected.getToken(), expected.getReason(), combinations);
	}

	@Override
	protected void describeMismatchSafely(GameState gameState, Description description) {
		append(description, gameState.getScore(), gameState.getToken(), gameState.getReason(),
				gameState.getWinningCombinations().size());
	}

	private static void append(Description description, Score score, Object token, String reason,
			int winningCombinations) {
		description.appendText(String.valueOf(score)).appendText(" ");
		description.appendText(String.valueOf(token)).appendText(" ");
		description.appendText(String.valueOf(reason)).appendText(" ");
		description.appendText(" combinations ").appendText(String.valueOf(winningCombinations));
	}

	@Override
	protected boolean matchesSafely(GameState gameState) {
		return Objects.equals(gameState.getScore(), expected.getScore()) //
				&& Objects.equals(gameState.getToken(), expected.getToken()) //
				&& Objects.equals(gameState.getReason(), expected.getReason()) //
				&& Objects.equals(gameState.getWinningCombinations().size(), combinations);
	}

	public GameStateMatcher score(Score score) {
		this.expected = this.expected.toBuilder().score(score).build();
		return this;
	}

	public GameStateMatcher error(String error) {
		this.expected = this.expected.toBuilder().score(LOSE).reason(error).build();
		return this;
	}

	public GameStateMatcher withToken(Object token) {
		this.expected = this.expected.toBuilder().token(token).build();
		return this;
	}

	public GameStateMatcher withReason(String reason) {
		this.expected = this.expected.toBuilder().reason(reason).build();
		return this;
	}

}
