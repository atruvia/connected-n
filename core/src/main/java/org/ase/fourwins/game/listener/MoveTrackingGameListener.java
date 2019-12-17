package org.ase.fourwins.game.listener;

import static org.ase.fourwins.game.listener.ObservedGame.fromPlayerListAndGameId;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.ase.fourwins.board.Board.GameState;
import org.ase.fourwins.game.Player;

public class MoveTrackingGameListener implements GameListener {
  private Map<GameId, ObservedGame> gameList = new ConcurrentHashMap<>();

  @Override
  public void gameStarted(GameId gameId, List<Player> players) {
    gameList.put(gameId, fromPlayerListAndGameId(gameId, players));
  }

  @Override
  public void moveMade(GameId gameId, Integer column, String token) {
    gameList.get(gameId)
        .getMoves()
        .add(new RecordedMove(column, token));
  }

  @Override
  public void gameEnded(GameId gameId, GameState gameState) {
    gameList.get(gameId)
        .setResult(gameState.getScore());

  }

  public List<ObservedGame> getObservedGames() {
    return new ArrayList<>(gameList.values());
  }
}
