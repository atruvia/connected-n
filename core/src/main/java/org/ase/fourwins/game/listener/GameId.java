package org.ase.fourwins.game.listener;

import java.util.UUID;

import lombok.Value;

@Value
public class GameId {
  private String gameId;

  public static GameId generateGameId() {
    return new GameId(UUID.randomUUID()
        .toString());
  }
}
