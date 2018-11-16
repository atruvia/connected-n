package org.ase.fourwins.tournament;

import static org.hamcrest.collection.IsMapContaining.hasEntry;
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

		Game game = buildGame(player1, player2, Score.WIN, player1.getToken());

		listener.gameEnded(game);

		assertThat(listener.getResult(), hasEntry(player1, 1.0));
		assertThat(listener.getResult(), hasEntry(player2, 0.0));

	}

	@Test
	void testStatisticForThreeGamesWith3Draws() {
		TournamentScoreListener listener = new TournamentScoreListener();
		PlayerMock player1 = new PlayerMock("P1");
		PlayerMock player2 = new PlayerMock("P2");

		Game game = buildGame(player1, player2, Score.DRAW, player1.getToken());

		listener.gameEnded(game);
		listener.gameEnded(game);
		listener.gameEnded(game);

		assertThat(listener.getResult(), hasEntry(player1, 1.5));
		assertThat(listener.getResult(), hasEntry(player2, 1.5));

	}

	@Test
	void testStatisticForOneLossAnd1Draw() {
		TournamentScoreListener listener = new TournamentScoreListener();
		PlayerMock player1 = new PlayerMock("P1");
		PlayerMock player2 = new PlayerMock("P2");

		Game gameDraw = buildGame(player1, player2, Score.DRAW,
				player1.getToken());
		Game gameLost = buildGame(player1, player2, Score.LOSE,
				player2.getToken());

		listener.gameEnded(gameDraw);
		listener.gameEnded(gameLost);

		assertThat(listener.getResult(), hasEntry(player1, 1.5));
		assertThat(listener.getResult(), hasEntry(player2, 0.5));
	}

	@Test
	void testPlayerWins4GamesInARow() {
		TournamentScoreListener listener = new TournamentScoreListener();
		PlayerMock player1 = new PlayerMock("P1");
		PlayerMock player2 = new PlayerMock("P2");

		Game gameDraw = buildGame(player1, player2, Score.DRAW,
				player1.getToken());
		Game gameLost = buildGame(player1, player2, Score.LOSE,
				player2.getToken());

		listener.gameEnded(gameDraw);
		listener.gameEnded(gameLost);
		listener.gameEnded(gameLost);
		listener.gameEnded(gameLost);
		listener.gameEnded(gameLost);

		assertThat(listener.getResult(), hasEntry(player1, 4.5));
		assertThat(listener.getResult(), hasEntry(player2, 0.5));
	}

	@Test
	void testStatisticForCoffeebreakGame() {
		TournamentScoreListener listener = new TournamentScoreListener();
		PlayerMock player1 = new PlayerMock("P1");

		DefaultTournament.CoffeebreakGame coffeebreakGame = new DefaultTournament.CoffeebreakGame(
				player1);

		listener.gameEnded(coffeebreakGame);

		assertThat(listener.getResult(), hasEntry(player1, 1.0));
	}

	private Game buildGame(Player player1, Player player2, Score score,
			String lastToken) {
		Game game = new Game() {

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
				return GameState.builder().score(score).token(lastToken)
						.build();
			}
		};
		return game;
	}

}
