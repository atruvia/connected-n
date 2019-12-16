package org.ase.fourwins.game.listener;

import java.util.ArrayList;
import java.util.List;

import org.ase.fourwins.game.Player;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Builder
public class ObservedGame {
  private GameId gameId;
  private String firstPlayer;
  private String secondPlayer;
  @Setter
  private Result result;
  private final List<RecordedMove> moves = new ArrayList<>();

  public static ObservedGame fromPlayerListAndGameId(GameId gameId, List<Player> players) {
    return ObservedGame.builder()
        .firstPlayer(players.get(0)
            .getToken())
        .secondPlayer(players.get(1)
            .getToken())
        .gameId(gameId)
        .build();
  }

}

