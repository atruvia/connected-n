package org.ase.fourwins.season;

import static java.util.Arrays.asList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

class SeasonTest {

	private static final String EVEN_TEAMS = "evenTeams";

	@Example
	void seasonOfTwoTeams() {
		verifyAllProperties(asList("T1", "T2"));
	}

	@Example
	void seasonOfFourTeams() {
		verifyAllProperties(asList("T1", "T2", "T3", "T4"));
	}

	@Example
	void seasonOfSixTeams() {
		verifyAllProperties(asList("T1", "T2", "T3", "T4", "T5", "T6"));
	}

	void verifyAllProperties(List<String> teams) {
		roundsHaveExpectedCountOfMatches(teams);
		noDuplicateTeams(teams);
		overallMatchCount(teams);
		seasonMatchesAreFirstRoundPlusBackRoundMatches(teams);
		backRoundGamesAreReversedFirstRoundGames(teams);
	}

	@Property
	void seasonMatchesAreFirstRoundPlusBackRoundMatches(@ForAll(EVEN_TEAMS) List<String> teams) {
		Season<String> season = new Season<>(teams);
		Stream<Matchday<String>> combined = Stream.of( //
				season.getFirstRound().getMatchdays(), //
				season.getBackRound().getMatchdays() //
		).flatMap(identity());
		assertThat(combined.collect(toList()), is(season.getMatchdays().collect(toList())));
	}

	@Property
	void backRoundGamesAreReversedFirstRoundGames(@ForAll(EVEN_TEAMS) List<String> teams) {
		Season<String> season = new Season<>(teams);
		Stream<List<String>> firstRound = teamsOf(season.getFirstRound());
		Stream<List<String>> backRound = reversed(teamsOf(season.getBackRound()));
		assertThat(firstRound.collect(toList()), is(backRound.collect(toList())));
	}

	@Property
	void noDuplicateTeams(@ForAll(EVEN_TEAMS) List<String> teams) {
		Season<String> season = new Season<>(teams);
		assertNoDuplicateTeams(teams, season.getFirstRound());
		assertNoDuplicateTeams(teams, season.getBackRound());
	}

	@Property
	void roundsHaveExpectedCountOfMatches(@ForAll(EVEN_TEAMS) List<String> teams) {
		Season<String> season = new Season<>(teams);
		int dayCount = teams.size() - 1;
		assertRoundHasMatchdays(season.getFirstRound(), dayCount);
		assertRoundHasMatchdays(season.getBackRound(), dayCount);
	}

	@Property
	void overallMatchCount(@ForAll(EVEN_TEAMS) List<String> teams) {
		Season<String> season = new Season<>(teams);
		int matchdays = teams.size() - 1;
		int matchsPerDay = teams.size() / 2;
		int matchCount = matchsPerDay * matchdays;
		assertRoundHasMatches(teams, season.getFirstRound(), matchCount);
		assertRoundHasMatches(teams, season.getBackRound(), matchCount);
	}

	@Provide(EVEN_TEAMS)
	Arbitrary<List<String>> evenTeamList() {
		return Arbitraries.strings().alpha().ofMaxLength(5).unique().list().ofMinSize(2).ofMaxSize(60)
				.filter(l -> l.size() % 2 == 0);
	}

	<T> Stream<List<T>> teamsOf(Round<T> round) {
		return matchesOf(round).map(SeasonTest::teamsOf);
	}

	<T> void assertRoundHasMatchdays(Round<T> round, long count) {
		assertThat(round.getMatchdays().count(), is(count));
	}

	<T> void assertRoundHasMatches(List<T> teams, Round<T> round, long count) {
		assertThat(matches(round).flatMap(identity()).count(), is(count));
	}

	<T> void assertNoDuplicateTeams(List<T> teams, Round<T> round) {
		matches(round).forEach(m -> assertNoDuplicateTeams(teams, m));
	}

	<T> Stream<Stream<Match<T>>> matches(Round<T> round) {
		return round.getMatchdays().map(Matchday::getMatches);
	}

	static <T> void assertNoDuplicateTeams(List<T> teams, Stream<Match<T>> matches) {
		Stream<T> teamsInMatches = matches.map(SeasonTest::teamsOf).flatMap(Collection::stream);
		assertThat(teamsInMatches.sorted().collect(toList()), is(teams.stream().sorted().collect(toList())));
	}

	static <T> Stream<List<T>> reversed(Stream<List<T>> teams) {
		return teams.map(SeasonTest::reversed);
	}

	static <T> List<T> reversed(List<T> in) {
		List<T> out = new ArrayList<>(in);
		Collections.reverse(out);
		return out;
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
		return asList(match.getTeam1(), match.getTeam2());
	}

}
