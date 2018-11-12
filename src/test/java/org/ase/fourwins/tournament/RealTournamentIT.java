package org.ase.fourwins.tournament;

import static java.lang.String.format;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static org.ase.fourwins.board.Board.Score.LOSE;
import static org.ase.fourwins.board.Board.Score.WIN;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.ase.fourwins.board.Board.GameState;
import org.ase.fourwins.board.mockplayers.ColumnTrackingMockPlayer;
import org.ase.fourwins.board.mockplayers.PlayerMock;
import org.ase.fourwins.board.mockplayers.RandomMockPlayer;
import org.ase.fourwins.tournament.Tournament.CoffeebreakGame;

import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;

public class RealTournamentIT {

	private static final int MAX_PLAYERS = 20;

	static final int MAX_SEASONS = 10;

	static final Predicate<GameState> isCoffeeBreak = s -> CoffeebreakGame.COFFEE_BREAK_WIN_MESSAGE
			.equals(s.getReason());

	@Example
	void amountOfGames_2Players() {
		List<PlayerMock> players = createPlayers(2, RandomMockPlayer::new);
		int seasons = 1;
		playSeasons(players, seasons);
		verifyPlayers(players, seasons);
	}

	@Example
	void amountOfGames_3Players() {
		List<PlayerMock> players = createPlayers(3, RandomMockPlayer::new);
		int seasons = 1;
		playSeasons(players, seasons);
		verifyPlayers(players, seasons);
	}

	@Example
	void amountOfGames_4Players() {
		List<PlayerMock> players = createPlayers(4, RandomMockPlayer::new);
		int seasons = 1;
		playSeasons(players, seasons);
		verifyPlayers(players, seasons);
	}

	@Property
	void jqwikCheckTestRandom(@ForAll @IntRange(min = 0, max = MAX_PLAYERS) int playerCount,
			@ForAll @IntRange(min = 0, max = MAX_SEASONS) int seasons) {
		List<PlayerMock> players = createPlayers(playerCount, RandomMockPlayer::new);
		Tournament tournament = tournamentWithPlayers(seasons, players);
		playSeasons(seasons, tournament).filter(isCoffeeBreak.negate()).forEach(this::verifyGameState);
		verifyPlayers(players, seasons);
	}

	@Property
	void jqwikCheckTestKeepTrack(@ForAll @IntRange(min = 0, max = MAX_PLAYERS) int playerCount,
			@ForAll @IntRange(min = 0, max = MAX_SEASONS) int seasons) {
		List<PlayerMock> players = createPlayers(playerCount, ColumnTrackingMockPlayer::new);
		playSeasons(seasons, tournamentWithPlayers(seasons, players)).filter(isCoffeeBreak.negate())
				.forEach(s -> assertThat(String.valueOf(s), s.getScore(), is(WIN)));
	}

	List<PlayerMock> createPlayers(int players, Function<String, PlayerMock> function) {
		return IntStream.range(0, players).mapToObj(String::valueOf).map("P"::concat).map(function).collect(toList());
	}

	void playSeasons(List<PlayerMock> players, int numberOfSeasons) {
		Tournament tournament = tournamentWithPlayers(numberOfSeasons, players);
		playSeasons(numberOfSeasons, tournament).filter(isCoffeeBreak.negate()).forEach(this::verifyGameState);
	}

	Stream<GameState> playSeasons(int seasons, Tournament tournament) {
		return IntStream.range(0, seasons).mapToObj(i -> tournament.playSeason()).flatMap(identity());
	}

	Tournament tournamentWithPlayers(int numberOfSeasons, Collection<PlayerMock> players) {
		System.out
				.println(format("Starting tournament with %s players and %s seasons", players.size(), numberOfSeasons));
		Tournament tournament = new Tournament();
		players.forEach(tournament::registerPlayer);
		return tournament;
	}

	void verifyPlayers(Collection<PlayerMock> players, int numberOfSeasons) {
		players.forEach(p -> {
			int expectedJoinedMatches = (players.size() - 1) * numberOfSeasons * 2;
			assertThat(p.getOpposites().size(), is(expectedJoinedMatches));
		});
	}

	void verifyGameState(GameState score) {
		if (score.getScore() == WIN) {
			assertThat(String.valueOf(score), score.getWinningCombinations().size(), is(not(0)));
		}
		if (score.getScore() != WIN) {
			assertThat(String.valueOf(score), score.getWinningCombinations().size(), is(0));
		}
		if (score.getScore() == LOSE) {
			assertThat(String.valueOf(score), score.getReason().isEmpty(), is(false));
		}
	}

}
