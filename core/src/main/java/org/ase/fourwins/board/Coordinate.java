package org.ase.fourwins.board;

import static lombok.AccessLevel.PRIVATE;

import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
@RequiredArgsConstructor(access = PRIVATE)
class Coordinate {

	private final int column, row;

	public Coordinate mutate(int mutX, int mutY) {
		return xy(column + mutX, row + mutY);
	}

	public Coordinate increaseColumn(int increaseBy) {
		return xy(column + increaseBy, row);
	}

	public Coordinate increaseRow(int increaseBy) {
		return xy(column, row + increaseBy);
	}

	public static Coordinate xy(int column, int row) {
		return new Coordinate(column, row);
	}

}
