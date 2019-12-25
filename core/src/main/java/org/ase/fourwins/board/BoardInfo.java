package org.ase.fourwins.board;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BoardInfo {

	public static BoardInfo sevenColsSixRows = builder().columns(7).rows(6).build();

	private int columns, rows;

	private int toConnect = 4;
	
}