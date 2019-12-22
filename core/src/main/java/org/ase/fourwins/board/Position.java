package org.ase.fourwins.board;

import lombok.Value;

@Value(staticConstructor = "xy")
public class Position {

	private final int column, row;

	public Position increaseColumn(int increaseBy) {
		return xy(column + increaseBy, row);
	}

	public Position increaseRow(int increaseBy) {
		return xy(column, row + increaseBy);
	}

}
