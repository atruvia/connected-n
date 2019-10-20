package org.ase.fourwins.board;

import lombok.Value;

@Value
public class Position {

	private final int column, row;

	public Position addColumn(int modby) {
		return new Position(column + modby, row);
	}

	public Position addRow(int modby) {
		return new Position(column, row + modby);
	}

	public static Position xy(int column, int row) {
		return new Position(column, row);
	}

}
