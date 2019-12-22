package org.ase.fourwins.tournament;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static net.jqwik.api.Arbitraries.frequency;
import static net.jqwik.api.Arbitraries.strings;
import static org.ase.fourwins.board.Board.Score.LOSE;
import static org.ase.fourwins.board.Board.Score.WIN;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.ase.fourwins.board.Board.GameState;
import org.ase.fourwins.board.mockplayers.ColumnTrackingMockPlayer;
import org.ase.fourwins.board.mockplayers.PlayerMock;
import org.ase.fourwins.game.Player;
import org.ase.fourwins.tournament.DefaultTournament.CoffeebreakGame;
import org.ase.fourwins.tournament.listener.TournamentListener;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;
import net.jqwik.api.constraints.IntRange;

public class RealTournamentITest {

	private static final String PLAYERS_NAMES_INCLUDING_COFFEE_BREAK = "playersNames";

	static final int MAX_PLAYERS = 14;

	static final int MAX_SEASONS = 8;

	static final Predicate<GameState> isCoffeeBreak = s -> CoffeebreakGame.COFFEE_BREAK_WIN_MESSAGE
			.equals(s.getReason());

	private TournamentListener scoreListener;

	private AtomicInteger seasonEndedCalls;

	@Example
	void twoPlayersOneSeason() {
		verifyAllProperties(1, "P1", "P2");
	}

	@Example
	void threePlayersOneSeason() {
		verifyAllProperties(1, "P1", "P2", "P3");
	}

	@Example
	void fourPlayersOneSeason() {
		verifyAllProperties(1, "P1", "P2", "P3", "P4");
	}

	private void verifyAllProperties(int seasons, String... playersNames) {
		List<String> players = Arrays.asList(playersNames);
		verifyGameState(players, seasons);
		verifyPlayers(players, seasons);
	}

	@Property
	void verifyPlayers(@ForAll(PLAYERS_NAMES_INCLUDING_COFFEE_BREAK) List<String> playersNames,
			@ForAll @IntRange(min = 0, max = MAX_SEASONS) int seasons) {
		List<PlayerMock> players = players(playersNames);
		playSeasons(seasons, players);
		players.forEach(p -> assertThat(p.getOpponents().size(), is(expectedJoinedMatches(players.size(), seasons))));
	}

	@Property
	void verifyGameState(@ForAll(PLAYERS_NAMES_INCLUDING_COFFEE_BREAK) List<String> playersNames,
			@ForAll @IntRange(min = 0, max = MAX_SEASONS) int seasons) {
		Stream<GameState> gameStates = playSeasons(seasons, players(playersNames));
		gameStates.forEach(this::checkGameState);
	}

	@Provide(PLAYERS_NAMES_INCLUDING_COFFEE_BREAK)
	private Arbitrary<List<String>> playersNames() {
		return frequency( //
				Tuple.of(5, Arbitraries.of(DefaultTournament.coffeeBreakPlayer.getToken())), //
				Tuple.of(5, strings().ofMinLength(1)), //
				Tuple.of(90, strings().ofMinLength(1).ofMaxLength(5)) //
		).flatMap(identity()).unique().list().ofMaxSize(MAX_PLAYERS);
	}

	private List<PlayerMock> players(List<String> playersNames) {
		return playersNames.stream().map(ColumnTrackingMockPlayer::new).collect(toList());
	}

	void checkGameState(GameState gameState) {
		String gameStateString = String.valueOf(gameState);
		if (gameState.getScore() == WIN) {
			assertThat(gameStateString, gameState.getWinningCombinations().size(), is(not(0)));
		}
		if (gameState.getScore() != WIN) {
			assertThat(gameStateString, gameState.getWinningCombinations().size(), is(0));
		}
		if (gameState.getScore() == LOSE) {
			assertThat(gameStateString, gameState.getReason().isEmpty(), is(false));
		}
	}

	Stream<GameState> playSeasons(int seasons, List<PlayerMock> players) {
		Tournament tournament = new DefaultTournament();
		seasonEndedCalls = new AtomicInteger(0);
		scoreListener = new TournamentListener() {
			@Override
			public void seasonEnded() {
				seasonEndedCalls.incrementAndGet();
			}
		};
		tournament.addTournamentListener(scoreListener);
		Stream<GameState> states = playSeasons(tournament, players, seasons).filter(isCoffeeBreak.negate());
		assertThat(seasonEndedCalls.get(), is(seasons));
		return states;
	}

	Stream<GameState> playSeasons(Tournament tournament, List<? extends Player> players, int seasons) {
		List<GameState> states = new ArrayList<GameState>();
		IntStream.range(0, seasons).forEach(s -> tournament.playSeason(players, states::add));
		return states.stream();
	}

	double expectedSum(int playersIncludingCoffeeBreak, int seasons) {
		int realPlayers = realPlayers(playersIncludingCoffeeBreak);
		return expectedJoinedMatches(realPlayers, seasons) * gamesPerDay(realPlayers);
	}

	private int realPlayers(int playersIncludingCoffeeBreak) {
		return playersIncludingCoffeeBreak % 2 == 0 ? playersIncludingCoffeeBreak : playersIncludingCoffeeBreak + 1;
	}

	int expectedJoinedMatches(int players, int seasons) {
		return (players - 1) * seasons * 2;
	}

	int gamesPerDay(int players) {
		return players / 2;
	}

}
