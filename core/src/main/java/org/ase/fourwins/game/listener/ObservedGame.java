package org.ase.fourwins.game.listener;

import java.util.ArrayList;
import java.util.List;

import org.ase.fourwins.board.Board.Score;
import org.ase.fourwins.game.Player;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Builder
public class ObservedGame {
  private GameId gameId;
  private List<Player> players;
  @Setter
  private Score result;
  private final List<RecordedMove> moves = new ArrayList<>();

  public static ObservedGame fromPlayerListAndGameId(GameId gameId, List<Player> players) {
    return ObservedGame.builder()
        .players(players)
        .gameId(gameId)
        .build();
  }

public String getFirstPlayerName() {
	return players.get(0).getToken();
}

public String getSecondPlayerName() {
	return players.get(1).getToken();
}

}

