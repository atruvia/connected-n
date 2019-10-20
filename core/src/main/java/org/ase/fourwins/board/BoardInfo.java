package org.ase.fourwins.board;

import lombok.Value;

@Value
public class BoardInfo {

	public static BoardInfo sevenColsSixRows = new BoardInfo(7, 6);

	private int columns, rows;
}