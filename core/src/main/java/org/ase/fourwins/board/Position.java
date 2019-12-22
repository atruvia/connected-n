package org.ase.fourwins.board;

import lombok.Value;

@Value
public class Position {

	private final int column, row;

	private Position(int column, int row) {
		this.column = column;
		this.row = row;
	}

	public Position addColumn(int modby) {
		return xy(column + modby, row);
	}

	public Position addRow(int modby) {
		return xy(column, row + modby);
	}

	public static Position xy(int column, int row) {
		return new Position(column, row);
	}

}
