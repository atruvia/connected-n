package org.ase.fourwins.game;

import java.util.List;

import org.ase.fourwins.board.Board.GameState;

public interface Game {

	Game runGame();

	GameState gameState();

	List<Player> getPlayers();

}
