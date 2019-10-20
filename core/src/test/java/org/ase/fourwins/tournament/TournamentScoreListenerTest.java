package org.ase.fourwins.tournament;

import static org.ase.fourwins.board.Board.Score.DRAW;
import static org.ase.fourwins.board.Board.Score.LOSE;
import static org.ase.fourwins.board.Board.Score.WIN;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;

import org.ase.fourwins.board.Board.GameState;
import org.ase.fourwins.board.Board.Score;
import org.ase.fourwins.board.mockplayers.PlayerMock;
import org.ase.fourwins.game.Game;
import org.ase.fourwins.game.Player;
import org.ase.fourwins.tournament.listener.TournamentScoreListener;
import org.junit.jupiter.api.Test;

class TournamentScoreListenerTest {

	@Test
	void testStatisticForOneGameWithPlayer1Winning() {
		TournamentScoreListener listener = new TournamentScoreListener();
		PlayerMock player1 = new PlayerMock("P1");
		PlayerMock player2 = new PlayerMock("P2");

		Game game = buildGame(player1, player2, WIN, player1);
		listener.gameEnded(game);

		ScoreSheet scoreSheet = listener.getScoreSheet();
		assertThat(scoreSheet.scoreOf(player1.getToken()), is(1.0));
		assertThat(scoreSheet.scoreOf(player2.getToken()), is(0.0));
	}

	@Test
	void testStatisticForThreeGamesWith3Draws() {
		TournamentScoreListener listener = new TournamentScoreListener();
		PlayerMock player1 = new PlayerMock("P1");
		PlayerMock player2 = new PlayerMock("P2");

		Game game = buildGame(player1, player2, DRAW, player1);
		listener.gameEnded(game);
		listener.gameEnded(game);
		listener.gameEnded(game);

		ScoreSheet scoreSheet = listener.getScoreSheet();
		assertThat(scoreSheet.scoreOf(player1.getToken()), is(1.5));
		assertThat(scoreSheet.scoreOf(player2.getToken()), is(1.5));

	}

	@Test
	void testStatisticForOneLossAnd1Draw() {
		TournamentScoreListener listener = new TournamentScoreListener();
		PlayerMock player1 = new PlayerMock("P1");
		PlayerMock player2 = new PlayerMock("P2");

		Game gameDraw = buildGame(player1, player2, DRAW, player1);
		Game gameLost = buildGame(player1, player2, LOSE, player2);
		listener.gameEnded(gameDraw);
		listener.gameEnded(gameLost);

		ScoreSheet scoreSheet = listener.getScoreSheet();
		assertThat(scoreSheet.scoreOf(player1.getToken()), is(1.5));
		assertThat(scoreSheet.scoreOf(player2.getToken()), is(0.5));
	}

	@Test
	void testPlayerWins4GamesInARow() {
		TournamentScoreListener listener = new TournamentScoreListener();
		PlayerMock player1 = new PlayerMock("P1");
		PlayerMock player2 = new PlayerMock("P2");

		Game gameDraw = buildGame(player1, player2, DRAW, player1);
		Game gameLost = buildGame(player1, player2, LOSE, player2);
		listener.gameEnded(gameDraw);
		listener.gameEnded(gameLost);
		listener.gameEnded(gameLost);
		listener.gameEnded(gameLost);
		listener.gameEnded(gameLost);

		ScoreSheet scoreSheet = listener.getScoreSheet();
		assertThat(scoreSheet.scoreOf(player1.getToken()), is(4.5));
		assertThat(scoreSheet.scoreOf(player2.getToken()), is(0.5));

		System.out.println(scoreSheet);
	}

	@Test
	void testStatisticForCoffeebreakGame() {
		TournamentScoreListener listener = new TournamentScoreListener();
		PlayerMock player1 = new PlayerMock("P1");

		DefaultTournament.CoffeebreakGame coffeebreakGame = new DefaultTournament.CoffeebreakGame(
				player1);
		listener.gameEnded(coffeebreakGame);

		ScoreSheet scoreSheet = listener.getScoreSheet();
		assertThat(scoreSheet.scoreOf(player1.getToken()), is(1.0));
	}

	private Game buildGame(Player player1, Player player2, Score score,
			Player lastMoveBy) {
		return new Game() {

			@Override
			public Game runGame() {
				throw new UnsupportedOperationException();
			}

			@Override
			public List<Player> getPlayers() {
				return Arrays.asList(player1, player2);
			}

			@Override
			public GameState gameState() {
				return GameState.builder().score(score)
						.token(lastMoveBy.getToken()).build();
			}
		};
	}

}
