package org.ase.fourwins.board;

import static java.util.stream.IntStream.iterate;

import java.util.OptionalInt;

import lombok.Value;

public interface Move {

	@Value
	public static class DefaultMove implements Move {
		int columnIdx;
	}

	int getColumnIdx();

	static OptionalInt index(char namedColumn) {
		return iterate(0, i -> i + 1).filter(i -> (('A' + i)) == (namedColumn)).findFirst();
	}

	public static Move moveToColumn(char columnName) {
		return new DefaultMove(column(columnName));
	}

	public static int column(char columnName) {
		return index(columnName).getAsInt();
	}

	public static Move moveToColumn(int columnIndex) {
		return new DefaultMove(columnIndex);
	}

}
