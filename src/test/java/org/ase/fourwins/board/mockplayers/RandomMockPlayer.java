package org.ase.fourwins.board.mockplayers;

import java.util.Random;

import org.ase.fourwins.board.BoardInfo;

public final class RandomMockPlayer extends PlayerMock {

	private BoardInfo boardInfo;

	public RandomMockPlayer(String token) {
		super(token);
	}

	@Override
	public boolean joinGame(String opposite, BoardInfo boardInfo) {
		this.boardInfo = boardInfo;
		return super.joinGame(opposite, boardInfo);
	}

	@Override
	protected int nextColumn() {
		return new Random().nextInt(boardInfo.getColumns());
	}
}