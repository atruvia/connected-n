package org.ase.fourwins.game;

import static org.ase.fourwins.game.GameTestUtil.player;
import static org.ase.fourwins.game.GameTestUtil.withMoves;
import static org.ase.fourwins.game.listener.Result.DRAW;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.ase.fourwins.board.Board;
import org.ase.fourwins.board.Board.Score;
import org.ase.fourwins.game.listener.ObservedGame;
import org.ase.fourwins.game.listener.MoveTrackingGameListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GameListenerTest {
  private static final String PLAYER_B = "B";
  private static final String PLAYER_A = "A";
  private static final String PLAYER_O = "O";
  private static final String PLAYER_X = "X";
  private MoveTrackingGameListener sut;

  @BeforeEach
  public void setup() {
    sut = new MoveTrackingGameListener();
  }

  @Test
  void testNameOfPlayersAreCorrectlyStoredForOneGame() {
    runGame(sut, PLAYER_X, PLAYER_O);
    ObservedGame observedGame = sut.getObservedGames()
        .get(0);
    assertThat(observedGame.getFirstPlayerName(), is(PLAYER_X));
    assertThat(observedGame.getSecondPlayerName(), is(PLAYER_O));
  }

  @Test
  void testNameOfPlayersForTwoGames() {
    runGame(sut, PLAYER_X, PLAYER_O);
    runGame(sut, PLAYER_A, PLAYER_B);
    ObservedGame gameAB = sut.getObservedGames()
        .stream()
        .filter(game -> PLAYER_A.equals(game.getFirstPlayerName()))
        .findFirst()
        .get();
    assertThat(gameAB.getSecondPlayerName(), is(PLAYER_B));

    assertThat(sut.getObservedGames()
        .size(), is(2));
  }

  @Test
  void testMovesAreCorrectlyStored() {
    runGame(sut, PLAYER_X, PLAYER_O);
    ObservedGame observedGame = sut.getObservedGames()
        .get(0);

    assertThat(observedGame.getMoves()
        .size(), is(4));
  }

  @Test
  void testResultIsCorrectlyStored() {
    runGame(sut, PLAYER_X, PLAYER_O);
    ObservedGame observedGame = sut.getObservedGames()
        .get(0);

    assertThat(observedGame.getResult(), is(Score.DRAW));
  }

  private void runGame(MoveTrackingGameListener sut, String firstPlayerName, String secondPlayerName) {
    Player firstPlayer = player(firstPlayerName, withMoves(0, 0));
    Player secondPlayer = player(secondPlayerName, withMoves(1, 1));
    Game game = new DefaultGame(Board.newBoard(2, 2), firstPlayer, secondPlayer);
    game.registerGameListener(sut);
    game.runGame();
  }
}
