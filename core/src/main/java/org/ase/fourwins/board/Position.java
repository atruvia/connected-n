package org.ase.fourwins.board;

import static lombok.AccessLevel.PRIVATE;

import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
@RequiredArgsConstructor(access = PRIVATE)
public class Position {

	private final int column, row;

	public Position increaseColumn(int increaseBy) {
		return xy(column + increaseBy, row);
	}

	public Position increaseRow(int increaseBy) {
		return xy(column, row + increaseBy);
	}

	public static Position xy(int column, int row) {
		return new Position(column, row);
	}

}
