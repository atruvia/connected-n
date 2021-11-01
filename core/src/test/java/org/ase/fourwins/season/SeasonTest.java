package org.ase.fourwins.season;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.rangeClosed;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import org.ase.fourwins.util.CollectionUtil;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

class SeasonTest {

	static final String EVEN_TEAMS = "evenTeams";
	static final String ODD_TEAMS = "oddTeams";

	@Example
	void seasonOfTwoTeams() {
		verifyAllProperties(2);
	}

	@Example
	void seasonOfFourTeams() {
		verifyAllProperties(4);
	}

	@Example
	void seasonOfSixTeams() {
		verifyAllProperties(6);
	}

	@Example
	void seasonOfThreeTeams() {
		exceptionWhenAmountOfTeamsIsOdd(3);
	}

	@Example
	void seasonOfFiveTeams() {
		exceptionWhenAmountOfTeamsIsOdd(5);
	}

	@Example
	void seasonOfSevenTeams() {
		exceptionWhenAmountOfTeamsIsOdd(7);
	}

	static List<String> teams(int count) {
		return rangeClosed(1, count).mapToObj(String::valueOf).map("Team "::concat).collect(toList());
	}

	void verifyAllProperties(int numberOfTeams) {
		roundsHaveExpectedCountOfMatchdays(numberOfTeams);
		noDuplicateTeams(numberOfTeams);
		matchesInFirstRound(numberOfTeams);
		matchesInSecondRound(numberOfTeams);
		seasonMatchesAreFirstRoundPlusSecondRoundMatches(numberOfTeams);
		secondRoundGamesAreReversedFirstRoundGames(numberOfTeams);
	}

	@Property
	void seasonMatchesAreFirstRoundPlusSecondRoundMatches(@ForAll(EVEN_TEAMS) int numberOfTeams) {
		Season<String> season = new Season<>(teams(numberOfTeams));
		Stream<Matchday<String>> combined = Stream.of( //
				season.getFirstRound().getMatchdays(), //
				season.getSecondRound().getMatchdays() //
		).flatMap(identity());
		assertThat(combined.collect(toList()), is(season.getMatchdays().collect(toList())));
	}

	@Property
	void secondRoundGamesAreReversedFirstRoundGames(@ForAll(EVEN_TEAMS) int numberOfTeams) {
		Season<String> season = new Season<>(teams(numberOfTeams));
		Stream<List<String>> firstRound = teamsOf(season.getFirstRound());
		Stream<List<String>> secondRound = reversed(teamsOf(season.getSecondRound()));
		assertThat(firstRound.collect(toList()), is(secondRound.collect(toList())));
	}

	@Property
	void noDuplicateTeams(@ForAll(EVEN_TEAMS) int numberOfTeams) {
		List<String> teams = teams(numberOfTeams);
		Season<String> season = new Season<>(teams);
		assertNoDuplicateTeams(teams, season.getFirstRound());
		assertNoDuplicateTeams(teams, season.getSecondRound());
	}

	@Property
	boolean roundsHaveExpectedCountOfMatchdays(@ForAll(EVEN_TEAMS) int numberOfTeams) {
		Season<String> season = new Season<>(teams(numberOfTeams));
		return season.getFirstRound().getMatchdays().count()
				+ season.getSecondRound().getMatchdays().count() == matchesPerTeamPerSeason(numberOfTeams);
	}

	@Property
	boolean matchesInFirstRound(@ForAll(EVEN_TEAMS) int numberOfTeams) {
		Season<String> season = new Season<>(teams(numberOfTeams));
		return matchCount(season.getFirstRound()) == matchesPerRound(numberOfTeams);
	}

	@Property
	boolean matchesInSecondRound(@ForAll(EVEN_TEAMS) int numberOfTeams) {
		Season<String> season = new Season<>(teams(numberOfTeams));
		return matchCount(season.getSecondRound()) == matchesPerRound(numberOfTeams);
	}

	@Property
	void exceptionWhenAmountOfTeamsIsOdd(@ForAll(ODD_TEAMS) int numberOfTeams) {
		List<String> teams = teams(numberOfTeams);
		String message = assertThrows(RuntimeException.class, () -> new Season<>(teams)).getMessage().toLowerCase();
		assertThat(message, allOf(anyOf(containsString("even"), containsString("odd")),
				containsString(String.valueOf(numberOfTeams))));
	}

	@Provide(EVEN_TEAMS)
	Arbitrary<Integer> evenTeamList() {
		return Arbitraries.integers().between(1, 200 / 2).map(i -> i * 2);
	}

	@Provide(ODD_TEAMS)
	Arbitrary<Integer> oddTeamList() {
		return Arbitraries.integers().between(1, 1000 / 2).map(i -> i * 2 - 1);
	}

	static <T> Stream<List<T>> teamsOf(Round<T> round) {
		return matchesOf(round).map(SeasonTest::teamsOf);
	}

	static <T> long matchCount(Round<T> round) {
		return matches(round).flatMap(identity()).count();
	}

	static <T> void assertNoDuplicateTeams(List<T> teams, Round<T> round) {
		matches(round).forEach(m -> assertNoDuplicateTeams(teams, m));
	}

	static <T> Stream<Stream<Match<T>>> matches(Round<T> round) {
		return round.getMatchdays().map(Matchday::getMatches);
	}

	static <T> void assertNoDuplicateTeams(List<T> teams, Stream<Match<T>> matches) {
		Stream<T> teamsInMatches = matches.map(SeasonTest::teamsOf).flatMap(Collection::stream);
		assertThat(teamsInMatches.sorted().collect(toList()), is(teams.stream().sorted().collect(toList())));
	}

	static int matchesPerRound(int teams) {
		return matchesPerMatchday(teams) * matchesPerTeamPerRound(teams);
	}

	static int matchesPerTeamPerSeason(int teams) {
		return matchesPerTeamPerRound(teams) * 2;
	}

	static int matchesPerTeamPerRound(int teams) {
		return teams - 1;
	}

	static int matchesPerMatchday(int teams) {
		return teams / 2;
	}

	static <T> Stream<List<T>> reversed(Stream<List<T>> teams) {
		return teams.map(CollectionUtil::reverse);
	}

	static <T> Stream<Match<T>> matchesOf(Round<T> round) {
		return round.getMatchdays().flatMap(Matchday::getMatches);
	}

	static <T> void assertNoDuplicateTeams(Collection<T> teams, Collection<Match<T>> matches) {
		List<T> collected = matches.stream().map(SeasonTest::teamsOf).flatMap(Collection::stream).sorted()
				.collect(toList());
		assertThat(collected, is(teams.stream().sorted().collect(toList())));
	}

	static <T> List<T> teamsOf(Match<T> match) {
		return List.of(match.getTeam1(), match.getTeam2());
	}

}
