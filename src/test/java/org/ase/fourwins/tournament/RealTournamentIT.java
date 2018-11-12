package org.ase.fourwins.tournament;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static org.ase.fourwins.board.Board.Score.LOSE;
import static org.ase.fourwins.board.Board.Score.WIN;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.ase.fourwins.board.Board.GameState;
import org.ase.fourwins.board.mockplayers.ColumnTrackingMockPlayer;
import org.ase.fourwins.board.mockplayers.PlayerMock;
import org.ase.fourwins.board.mockplayers.RandomMockPlayer;
import org.ase.fourwins.game.Player;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.IntRange;

public class RealTournamentIT {

	@Example
	void amountOfGames_2Players() {
		// TODO remove comment when fixed! Is actual 2
		playOneSeason(createPlayers(2, RandomMockPlayer::new));
	}

	@Example
	void amountOfGames_3Players() {
		// TODO remove comment when fixed! Is actual 4
		playOneSeason(createPlayers(3, RandomMockPlayer::new));
	}

	@Example
	void amountOfGames_4Players() {
		// TODO remove comment when fixed! Is actual 6
		playOneSeason(createPlayers(4, RandomMockPlayer::new));
	}

	List<PlayerMock> createPlayers(int players, Function<String, PlayerMock> function) {
		return IntStream.range(0, players).mapToObj(String::valueOf).map("P"::concat).map(function).collect(toList());
	}

	void playOneSeason(List<PlayerMock> players) {
		int numberOfSeasons = 1;
		Tournament tournament = tournamentWithPlayers(numberOfSeasons, players);
		playSeasons(numberOfSeasons, tournament).flatMap(identity()).filter(this::isNoCoffeeBreak)
				.forEach(this::verifyGameState);
		verifyPlayers(numberOfSeasons, players);
	}

	@Property
	void jqwikCheckTestRandom(@ForAll("playerNames") List<String> playerNames,
			@ForAll @IntRange(min = 0, max = 5) int numberOfSeasons) {
		List<PlayerMock> players = playerNames.stream().map(RandomMockPlayer::new).collect(toList());
		Tournament tournament = tournamentWithPlayers(numberOfSeasons, players);
		playSeasons(numberOfSeasons, tournament).flatMap(identity()).filter(this::isNoCoffeeBreak)
				.forEach(this::verifyGameState);
		verifyPlayers(numberOfSeasons, players);
	}

	protected Stream<Stream<GameState>> playSeasons(int numberOfSeasons, Tournament tournament) {
		return IntStream.range(0, numberOfSeasons).mapToObj(i -> tournament.playSeason());
	}

	protected void verifyPlayers(int numberOfSeasons, List<PlayerMock> players) {
		if (players.size() > 0) {
			players.forEach(p -> {
				int matchdays = players.size() - 1;
				boolean isCoffeeBreak = players.size() % 2 == 0;
//				if (isCoffeeBreak) {
//					matchdays++;
//				}
				int matchsPerDay = players.size() / 2;
				int expectedJoinedMatches = matchsPerDay * matchdays * numberOfSeasons;
				assertThat(p.getOpposites().size(), is(expectedJoinedMatches));
			});
		}
	}

	protected Tournament tournamentWithPlayers(int numberOfSeasons, List<PlayerMock> players) {
		System.out
				.println("Starting tournament with " + players.size() + " players and " + numberOfSeasons + " seasons");
		Tournament tournament = new Tournament();
		players.forEach(tournament::registerPlayer);
		return tournament;
	}

	private void verifyGameState(GameState score) {
		{
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

	@Property
	void jqwikCheckTestKeepTrack(@ForAll("playerNames") List<String> playerNames,
			@ForAll @IntRange(min = 0, max = 100) int numberOfSeasons) {
		List<PlayerMock> players = playerNames.stream().map(ColumnTrackingMockPlayer::new).collect(toList());
		Tournament tournament = tournamentWithPlayers(numberOfSeasons, players);

		for (int i = 0; i < numberOfSeasons; i++) {
			tournament.playSeason().filter(this::isNoCoffeeBreak)
					.forEach(s -> assertThat(String.valueOf(s), s.getScore(), is(WIN)));
		}
	}

	boolean isNoCoffeeBreak(GameState gameState) {
		return !"coffee break".equals(gameState.getReason());
	}

	@Provide
	Arbitrary<List<String>> playerNames() {
		return Arbitraries.integers().between(2, 5).flatMap( //
				stringSize -> Arbitraries.strings() //
						.alpha() //
						.ofMinLength(stringSize) //
						.ofMaxLength(stringSize) //
						.unique() //
						.list().ofMinSize(0).ofMaxSize(20));
	}

}
