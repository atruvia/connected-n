package org.ase.fourwins.board.mockplayers;

import java.util.ArrayList;
import java.util.List;

import org.ase.fourwins.board.BoardInfo;
import org.ase.fourwins.game.Player;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class PlayerMock extends Player {

	private int movesMade;
	private final List<String> opponents = new ArrayList<>();

	public PlayerMock(String token) {
		super(token);
	}

	@Override
	protected int nextColumn() {
		movesMade++;
		return Integer.MIN_VALUE;
	}

	@Override
	public boolean joinGame(String opposite, BoardInfo boardInfo) {
		opponents.add(opposite);
		return super.joinGame(opposite, boardInfo);
	}

}