package org.ase.fourwins.tournament;

import static java.util.stream.Collectors.toList;
import static org.ase.fourwins.board.Board.Score.LOSE;
import static org.ase.fourwins.board.Board.Score.WIN;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.ase.fourwins.board.Board.GameState;
import org.ase.fourwins.board.Board.Score;
import org.ase.fourwins.board.mockplayers.ColumnTrackingMockPlayer;
import org.ase.fourwins.board.mockplayers.PlayerMock;
import org.ase.fourwins.game.Player;
import org.ase.fourwins.tournament.DefaultTournament.CoffeebreakGame;
import org.ase.fourwins.tournament.listener.TournamentScoreListener;

import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;

public class RealTournamentITest {

	static final int MAX_PLAYERS = 20;

	static final int MAX_SEASONS = 10;

	static final Predicate<GameState> isCoffeeBreak = s -> CoffeebreakGame.COFFEE_BREAK_WIN_MESSAGE
			.equals(s.getReason());

	private TournamentScoreListener scoreListener;

	@Example
	void twoPlayersOneSeason() {
		int players = 2;
		int seasons = 1;
		verifyAllProperties(players, seasons);
	}

	@Example
	void threePlayersOneSeason() {
		int players = 3;
		int seasons = 1;
		verifyAllProperties(players, seasons);
	}

	@Example
	void fourPlayersOneSeason() {
		int players = 4;
		int seasons = 1;
		verifyAllProperties(players, seasons);
	}

	private void verifyAllProperties(int players, int seasons) {
		verifyGameState(players, seasons);
		verifyPlayers(players, seasons);
		verifySumOfPoints(players, seasons);
	}

	@Property
	void verifySumOfPoints(@ForAll @IntRange(min = 0, max = MAX_PLAYERS) int playerCount,
			@ForAll @IntRange(min = 0, max = MAX_SEASONS) int seasons) {
		List<PlayerMock> players = createPlayers(playerCount, ColumnTrackingMockPlayer::new);
		playSeasons(seasons, players);
		assertThat(sumPoints(scoreListener.getScoreSheet()), is(expectedSumOfAllPoints(players.size(), seasons)));
	}

	@Property
	void verifyPlayers(@ForAll @IntRange(min = 0, max = MAX_PLAYERS) int playerCount,
			@ForAll @IntRange(min = 0, max = MAX_SEASONS) int seasons) {
		List<PlayerMock> players = createPlayers(playerCount, ColumnTrackingMockPlayer::new);
		playSeasons(seasons, players);
		players.forEach(p -> assertThat(p.getOpponents().size(), is(expectedJoinedMatches(seasons, players))));
	}

	@Property
	void verifyGameState(@ForAll @IntRange(min = 0, max = MAX_PLAYERS) int playerCount,
			@ForAll @IntRange(min = 0, max = MAX_SEASONS) int seasons) {
		List<PlayerMock> players = createPlayers(playerCount, ColumnTrackingMockPlayer::new);
		Stream<GameState> gameStates = playSeasons(seasons, players);
		gameStates.forEach(this::checkGameState);
	}

	private double sumPoints(ScoreSheet scoreSheet) {
		return scoreSheet.values().stream().mapToDouble(Double::valueOf).sum();
	}

	void checkGameState(GameState gameState) {
		Score score = gameState.getScore();
		if (score == WIN) {
			assertThat(String.valueOf(gameState), gameState.getWinningCombinations().size(), is(not(0)));
		}
		if (gameState.getScore() != WIN) {
			assertThat(String.valueOf(gameState), gameState.getWinningCombinations().size(), is(0));
		}
		if (gameState.getScore() == LOSE) {
			assertThat(String.valueOf(gameState), gameState.getReason().isEmpty(), is(false));
		}
	}

	List<PlayerMock> createPlayers(int players, Function<String, PlayerMock> function) {
		return IntStream.range(0, players).mapToObj(String::valueOf).map("P"::concat).map(function).collect(toList());
	}

	Stream<GameState> playSeasons(int seasons, List<PlayerMock> players) {
		Tournament tournament = new DefaultTournament();
		scoreListener = new TournamentScoreListener();
		tournament.addTournamentListener(scoreListener);
		return playSeasons(tournament, players, seasons).filter(isCoffeeBreak.negate());
	}

	Stream<GameState> playSeasons(Tournament tournament, List<? extends Player> players, int seasons) {
		List<GameState> states = new ArrayList<GameState>();
		IntStream.range(0, seasons).forEach(s -> tournament.playSeason(players, states::add));
		return states.stream();
	}

	double expectedSumOfAllPoints(int players, int seasons) {
		int realPlayers = players % 2 == 0 ? players : players + 1;
		int gamesPerDay = realPlayers / 2;
		double expectedSumOfAllPoints = 2 * (realPlayers - 1) * gamesPerDay * seasons;
		return expectedSumOfAllPoints;
	}

	int expectedJoinedMatches(int seasons, List<PlayerMock> players) {
		return (players.size() - 1) * seasons * 2;
	}

}
