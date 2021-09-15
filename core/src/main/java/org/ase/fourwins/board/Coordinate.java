package org.ase.fourwins.board;

import static lombok.AccessLevel.PRIVATE;

import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
@RequiredArgsConstructor(access = PRIVATE)
class Coordinate {

	int x, y;

	public Coordinate mutate(int mutateX, int mutateY) {
		return xy(x + mutateX, y + mutateY);
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
