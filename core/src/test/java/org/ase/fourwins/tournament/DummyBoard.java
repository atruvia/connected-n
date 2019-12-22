package org.ase.fourwins.tournament;

import static org.ase.fourwins.board.Board.Score.IN_GAME;
import static org.ase.fourwins.board.Board.Score.LOSE;

import org.ase.fourwins.board.Board;
import org.ase.fourwins.board.BoardInfo;
import org.ase.fourwins.board.Move;

public final class DummyBoard extends Board {

	public static final String LOSE_MESSAGE = "dummy board lose message";
	private int moves;
	private GameState gameState = GameState.builder().score(IN_GAME).build();

	@Override
	public GameState gameState() {
		return gameState;
	}

	@Override
	public Board insertToken(Move move, Object token) {
		if (++moves == 7) {
			this.gameState = gameState.toBuilder() //
					.score(LOSE) //
					.token(token) //
					.reason(LOSE_MESSAGE) //
					.build();
		}
		return this;
	}

	@Override
	public BoardInfo boardInfo() {
		return BoardInfo.sevenColsSixRows;
	}
}
