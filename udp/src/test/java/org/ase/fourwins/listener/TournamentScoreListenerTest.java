package org.ase.fourwins.listener;

import static org.ase.fourwins.board.Board.Score.DRAW;
import static org.ase.fourwins.board.Board.Score.LOSE;
import static org.ase.fourwins.board.Board.Score.WIN;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.ase.fourwins.board.Board.GameState;
import org.ase.fourwins.board.Board.Score;
import org.ase.fourwins.board.BoardInfo;
import org.ase.fourwins.game.Game;
import org.ase.fourwins.game.Player;
import org.ase.fourwins.tournament.ScoreSheet;
import org.junit.jupiter.api.Test;

class TournamentScoreListenerTest {

	@Test
	void testStatisticForOneGameWithPlayer1Winning() {
		TournamentScoreListener listener = new TournamentScoreListener();
		String player1 = "P1";
		String player2 = "P2";

		Game game = buildGame(player1, player2, WIN, player1);
		listener.gameEnded(game);

		ScoreSheet scoreSheet = listener.getScoreSheet();
		assertThat(scoreSheet.scoreOf(player1), is(1.0));
		assertThat(scoreSheet.scoreOf(player2), is(0.0));
	}

	@Test
	void testStatisticForThreeGamesWith3Draws() {
		TournamentScoreListener listener = new TournamentScoreListener();
		String player1 = "P1";
		String player2 = "P2";

		Game game = buildGame(player1, player2, DRAW, player1);
		listener.gameEnded(game);
		listener.gameEnded(game);
		listener.gameEnded(game);

		ScoreSheet scoreSheet = listener.getScoreSheet();
		assertThat(scoreSheet.scoreOf(player1), is(1.5));
		assertThat(scoreSheet.scoreOf(player2), is(1.5));
	}

	@Test
	void testStatisticForOneLossAnd1Draw() {
		TournamentScoreListener listener = new TournamentScoreListener();
		String player1 = "P1";
		String player2 = "P2";

		Game gameDraw = buildGame(player1, player2, DRAW, player1);
		Game gameLost = buildGame(player1, player2, LOSE, player2);
		listener.gameEnded(gameDraw);
		listener.gameEnded(gameLost);

		ScoreSheet scoreSheet = listener.getScoreSheet();
		assertThat(scoreSheet.scoreOf(player1), is(1.5));
		assertThat(scoreSheet.scoreOf(player2), is(0.5));
	}

	@Test
	void testPlayerWins4GamesInARow() {
		TournamentScoreListener listener = new TournamentScoreListener();
		String player1 = "P1";
		String player2 = "P2";

		Game gameDraw = buildGame(player1, player2, DRAW, player1);
		Game gameLost = buildGame(player1, player2, LOSE, player2);
		listener.gameEnded(gameDraw);
		listener.gameEnded(gameLost);
		listener.gameEnded(gameLost);
		listener.gameEnded(gameLost);
		listener.gameEnded(gameLost);

		ScoreSheet scoreSheet = listener.getScoreSheet();
		assertThat(scoreSheet.scoreOf(player1), is(4.5));
		assertThat(scoreSheet.scoreOf(player2), is(0.5));
	}

	private Player dummyPlayer(String token) {
		return new Player(token) {
			@Override
			protected int nextColumn() {
				throw new UnsupportedOperationException();
			}
		};
	}

	private Game buildGame(String token1, String token2, Score score, String lastMoveBy) {
		List<Player> players = List.of(dummyPlayer(token1), dummyPlayer(token2));
		return new Game() {

			@Override
			public Game runGame() {
				throw new UnsupportedOperationException();
			}

			@Override
			public GameId getId() {
				throw new UnsupportedOperationException();
			}

			@Override
			public BoardInfo getBoardInfo() {
				throw new UnsupportedOperationException();
			}

			@Override
			public List<Player> getPlayers() {
				return players;
			}

			@Override
			public GameState gameState() {
				return GameState.builder().score(score).token(lastMoveBy).build();
			}
		};
	}

}
