package org.ase.fourwins.board.mockplayers;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.ase.fourwins.board.BoardInfo;

import lombok.Getter;

public class ColumnTrackingMockPlayer extends PlayerMock {

	private BoardInfo boardInfo;
	private Map<Integer, Integer> columns = new HashMap<>();
	@Getter
	private Integer lastColumn;

	public ColumnTrackingMockPlayer(String token) {
		super(token);
	}

	@Override
	public boolean joinGame(String opposite, BoardInfo boardInfo) {
		this.boardInfo = boardInfo;
		columns.clear();
		lastColumn = null;
		return super.joinGame(opposite, boardInfo);
	}

	@Override
	protected void tokenWasInserted(String token, int column) {
		this.lastColumn = column;
		columns.put(column, columnCount(column) + 1);
	}

	public int columnCount(int column) {
		Integer count = columns.get(column);
		return count == null ? 0 : count;
	}

	@Override
	protected int nextColumn() {
		return calcNextColumn();
	}

	public int calcNextColumn() {
		if (boardIsEmpty()) {
			return new Random().nextInt(boardInfo.getColumns());
		}
		int column = increase(lastColumn);
//		while (isFilled(column)) {
//			column = increase(column);
//		}
		return column;
	}

	protected boolean boardIsEmpty() {
		return getLastColumn() == null;
	}

	protected int increase(int column) {
		int r = column + 1;
		return r >= boardInfo.getColumns() ? 0 : r;
	}

	public boolean isFilled(int column) {
		return columnCount(column) >= boardInfo.getRows();
	}

}