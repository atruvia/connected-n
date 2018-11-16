package org.ase.fourwins.tournament;

import static java.util.stream.Collectors.toList;
import static org.ase.fourwins.board.Board.Score.LOSE;
import static org.ase.fourwins.board.Board.Score.WIN;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.ase.fourwins.board.Board.GameState;
import org.ase.fourwins.board.mockplayers.ColumnTrackingMockPlayer;
import org.ase.fourwins.board.mockplayers.PlayerMock;
import org.ase.fourwins.board.mockplayers.RandomMockPlayer;
import org.ase.fourwins.tournament.DefaultTournament.CoffeebreakGame;
import org.ase.fourwins.tournament.Tournament.RegistrationResult;

import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;

public class RealTournamentIT {

	static final int MAX_PLAYERS = 20;

	static final int MAX_SEASONS = 10;

	static final Predicate<GameState> isCoffeeBreak = s -> CoffeebreakGame.COFFEE_BREAK_WIN_MESSAGE
			.equals(s.getReason());

	@Example
	void amountOfGames_2Players() {
		List<PlayerMock> players = createPlayers(2, RandomMockPlayer::new);
		int seasons = 1;
		playSeasons(seasons, players).forEach(this::verifyGameState);
		verifyPlayers(players, seasons);
	}

	@Example
	void amountOfGames_3Players() {
		List<PlayerMock> players = createPlayers(3, RandomMockPlayer::new);
		int seasons = 1;
		playSeasons(seasons, players).forEach(this::verifyGameState);
		verifyPlayers(players, seasons);
	}

	@Example
	void amountOfGames_4Players() {
		List<PlayerMock> players = createPlayers(4, RandomMockPlayer::new);
		int seasons = 1;
		playSeasons(seasons, players).forEach(this::verifyGameState);
		verifyPlayers(players, seasons);
	}

	@Property
	void jqwikCheckTestRandom(@ForAll @IntRange(min = 0, max = MAX_PLAYERS) int playerCount,
			@ForAll @IntRange(min = 0, max = MAX_SEASONS) int seasons) {
		List<PlayerMock> players = createPlayers(playerCount, RandomMockPlayer::new);
		playSeasons(seasons, players).forEach(this::verifyGameState);
		verifyPlayers(players, seasons);
	}

	@Property
	void jqwikCheckTestKeepTrack(@ForAll @IntRange(min = 0, max = MAX_PLAYERS) int playerCount,
			@ForAll @IntRange(min = 0, max = MAX_SEASONS) int seasons) {
		List<PlayerMock> players = createPlayers(playerCount, ColumnTrackingMockPlayer::new);
		playSeasons(seasons, players).forEach(this::verifyGameState);
		verifyPlayers(players, seasons);
	}

	List<PlayerMock> createPlayers(int players, Function<String, PlayerMock> function) {
		return IntStream.range(0, players).mapToObj(String::valueOf).map("P"::concat).map(function).collect(toList());
	}

	Stream<GameState> playSeasons(int seasons, List<PlayerMock> players) {
		Tournament tournament = registerPlayers(new DefaultTournament(), players);
		return playSeasons(seasons, tournament).filter(isCoffeeBreak.negate());
	}

	Stream<GameState> playSeasons(int seasons, Tournament tournament) {
		List<GameState> states = new ArrayList<GameState>();
		IntStream.range(0, seasons).forEach(s -> tournament.playSeason(states::add));
		return states.stream();
	}

	protected Tournament registerPlayers(Tournament tournament, Collection<PlayerMock> players) {
		players.forEach(safeRegister(tournament));
		return tournament;
	}

	protected Consumer<? super PlayerMock> safeRegister(Tournament tournament) {
		return t -> {
			RegistrationResult registerPlayer = tournament.registerPlayer(t);
			if (!registerPlayer.isOk()) {
				throw new RuntimeException("REGISTRATION_ERROR");
			}
		};
	}

	void verifyPlayers(Collection<PlayerMock> players, int numberOfSeasons) {
		players.forEach(p -> {
			int expectedJoinedMatches = (players.size() - 1) * numberOfSeasons * 2;
			assertThat(p.getOpponents().size(), is(expectedJoinedMatches));
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
