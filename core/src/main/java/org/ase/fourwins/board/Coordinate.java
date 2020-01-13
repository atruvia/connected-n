package org.ase.fourwins.board;

import static lombok.AccessLevel.PRIVATE;

import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
@RequiredArgsConstructor(access = PRIVATE)
class Coordinate {

	private final int x, y;

	public Coordinate mutate(int mutX, int mutY) {
		return xy(x + mutX, y + mutY);
	}

	public Coordinate mutateX(int mutateX) {
		return xy(x + mutateX, y);
	}

	public Coordinate mutateY(int mutateY) {
		return xy(x, y + mutateY);
	}

	public static Coordinate xy(int x, int y) {
		return new Coordinate(x, y);
	}

}
