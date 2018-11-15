package org.ase.fourwins.game;

import org.ase.fourwins.board.Board.GameState;

public interface Game {

	Game runGame();

	GameState gameState();

}
