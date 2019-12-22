package org.ase.fourwins.game;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class GameTestUtil {
	protected static List<Integer> withMoves(Integer... moves) {
		return Arrays.asList(moves);
	}

	protected static Player player(String token, List<Integer> columns) {
		Iterator<Integer> moves = columns.iterator();
		return new Player(token) {
			@Override
			protected int nextColumn() {
				return moves.next();
			}
		};
	}

}
