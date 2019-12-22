package org.ase.fourwins.game.listener;

import java.util.List;

import org.ase.fourwins.board.Board.GameState;
import org.ase.fourwins.game.Player;

public interface GameListener {
  default void gameStarted(GameId gameId, List<Player> players) {
  }

  default void moveMade(GameId gameId, Integer column, String token) {
  }

  default void gameEnded(GameId gameId, GameState gameState) {
  }
}
