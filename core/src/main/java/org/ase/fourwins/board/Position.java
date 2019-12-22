package org.ase.fourwins.board;

import lombok.Value;

@Value(staticConstructor = "xy")
public class Position {

	private final int column, row;

	public Position addColumn(int modby) {
		return xy(column + modby, row);
	}

	public Position addRow(int modby) {
		return xy(column, row + modby);
	}

}
