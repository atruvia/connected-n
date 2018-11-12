package org.ase.fourwins.tournament;

import static java.util.stream.Collectors.toList;
import static org.ase.fourwins.board.Board.Score.LOSE;
import static org.ase.fourwins.board.Board.Score.WIN;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.ase.fourwins.board.Board.GameState;
import org.ase.fourwins.board.mockplayers.ColumnTrackingMockPlayer;
import org.ase.fourwins.board.mockplayers.PlayerMock;
import org.ase.fourwins.board.mockplayers.RandomMockPlayer;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.IntRange;

public class RealTournamentIT {

	@Property
	void jqwikCheckTestRandom(@ForAll("playerNames") List<String> playerNames,
			@ForAll @IntRange(min = 0, max = 100) int numberOfSeasons) {
		List<PlayerMock> players = playerNames.stream().map(RandomMockPlayer::new).collect(toList());
		System.out
				.println("Starting tournament with " + players.size() + " players and " + numberOfSeasons + " seasons");
		Tournament tournament = new Tournament();
		players.forEach(tournament::registerPlayer);

		for (int i = 0; i < numberOfSeasons; i++) {
			tournament.playSeason().filter(this::isNoCoffeeBreak).forEach(s -> {
				if (s.getScore() == WIN) {
					assertThat(String.valueOf(s), s.getWinningCombinations().size(), is(not(0)));
				}
				if (s.getScore() != WIN) {
					assertThat(String.valueOf(s), s.getWinningCombinations().size(), is(0));
				}
				if (s.getScore() == LOSE) {
					assertThat(String.valueOf(s), s.getReason().isEmpty(), is(false));
				}
			});
		}

		int realPlayers = players.size();
		if (realPlayers > 0) {
			players.forEach(p -> {
				int matchdays = realPlayers - 1;
				boolean isCoffeeBreak = players.size()  % 2 == 0;
				if (isCoffeeBreak) {
					matchdays++;
				}
				int matchsPerDay = realPlayers / 2;
				int expectedJoinedMatches = matchsPerDay * matchdays * 2 * numberOfSeasons;
				System.out.println(p.getOpposites().size() + " vs " + expectedJoinedMatches);
				assertThat(p.getOpposites().size(), is(expectedJoinedMatches));
			});
		}
	}

	@Property
	void jqwikCheckTestKeepTrack(@ForAll("playerNames") List<String> playerNames,
			@ForAll @IntRange(min = 0, max = 100) int numberOfSeasons) {
		List<PlayerMock> players = playerNames.stream().map(ColumnTrackingMockPlayer::new).collect(toList());
		System.out
				.println("Starting tournament with " + players.size() + " players and " + numberOfSeasons + " seasons");
		Tournament tournament = new Tournament();
		players.forEach(tournament::registerPlayer);

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
