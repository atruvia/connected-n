package org.ase.fourwins.season;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.rangeClosed;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

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

	private static final String EVEN_TEAMS = "evenTeams";

	@Example
	void seasonOfTwoTeams() {
		verifyAllProperties(teams(2));
	}

	@Example
	void seasonOfFourTeams() {
		verifyAllProperties(teams(4));
	}

	@Example
	void seasonOfSixTeams() {
		verifyAllProperties(teams(6));
	}

	static List<String> teams(int count) {
		return rangeClosed(1, count).mapToObj(String::valueOf).map("Team "::concat).collect(toList());
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
	boolean roundsHaveExpectedCountOfMatches(@ForAll(EVEN_TEAMS) List<String> teams) {
		Season<String> season = new Season<>(teams);
		return season.getFirstRound().getMatchdays().count()
				+ season.getBackRound().getMatchdays().count() == seasonMatchCount(teams);
	}

	@Property
	boolean overallMatchCount(@ForAll(EVEN_TEAMS) List<String> teams) {
		Season<String> season = new Season<>(teams);
		return matchCount(season.getFirstRound()) + matchCount(season.getBackRound()) == matchsPerDay(teams)
				* seasonMatchCount(teams);
	}

	@Provide(EVEN_TEAMS)
	Arbitrary<List<String>> evenTeamList() {
		return Arbitraries.strings().alpha().ofMaxLength(5).unique().list().ofMinSize(2).ofMaxSize(60)
				.filter(l -> l.size() % 2 == 0);
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

	static int seasonMatchCount(Collection<String> teams) {
		return (teams.size() - 1) * 2;
	}

	static int matchsPerDay(List<String> teams) {
		return teams.size() / 2;
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
