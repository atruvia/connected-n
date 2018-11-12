package org.ase.fourwins.season;

import static java.util.Arrays.asList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

class SeasonTest {

	@Example
	void seasonOfTwoTeams() {
		List<String> teams = asList("T1", "T2");
		verify(teams, new Round<String>(teams));
	}

	@Example
	void seasonOfFourTeams() {
		List<String> teams = asList("T1", "T2", "T3", "T4");
		verify(teams, new Round<String>(teams));
	}

	@Example
	void seasonOfSixTeams() {
		List<String> teams = asList("T1", "T2", "T3", "T4", "T5", "T6");
		verify(teams, new Round<String>(teams));
	}

	@Property
	void leagueQwikTest(@ForAll("oddTeamList") List<String> teams) {
		verify(teams, new Round<String>(teams));
	}

	@Example
	void seasonOfTwo() {
		List<String> teams = asList("T1", "T2");
		verify(teams, new Season<>(teams));
	}

	@Example
	void seasonOfSix() {
		List<String> teams = asList("T1", "T2", "T3", "T4", "T5", "T6");
		verify(teams, new Season<>(teams));
	}

	@Property
	void seasonQwikTest(@ForAll("oddTeamList") List<String> teams) {
		verify(teams, new Season<>(teams));
	}

	@Provide
	Arbitrary<List<String>> oddTeamList() {
		return Arbitraries.strings().alpha().ofMaxLength(5).unique().list().ofMinSize(2).ofMaxSize(60)
				.filter(l -> l.size() % 2 == 0);
	}

	void verify(List<String> teams, Season<String> season) {
		Round<String> firstRound = season.getFirstRound();
		Round<String> backRound = season.getBackRound();

		verify(teams, firstRound);
		verify(teams, backRound);

		assertThat(Stream.of(firstRound.getMatchdays(), backRound.getMatchdays()).flatMap(identity()).collect(toList()),
				is(season.getMatchdays().collect(toList())));

		Iterator<Matchday<String>> it2 = backRound.getMatchdays().iterator();
		for (Iterator<Matchday<String>> it1 = firstRound.getMatchdays().iterator(); it1.hasNext();) {
			List<Match<String>> matches1 = it1.next().getMatches().collect(toList());
			List<Match<String>> matches2 = it2.next().getMatches().collect(toList());
			Iterator<Match<String>> itm2 = matches2.iterator();
			for (Iterator<Match<String>> itm1 = matches1.iterator(); itm1.hasNext();) {
				assertIsReversed(itm1.next(), itm2.next());
			}
		}
	}

	void assertIsReversed(Match<String> pair1, Match<String> pair2) {
		assertThat(pair1.getTeam1(), is(pair2.getTeam2()));
		assertThat(pair1.getTeam2(), is(pair2.getTeam1()));
	}

	static void verify(List<String> teams, Round<String> round) {
		List<Matchday<String>> matches = round.getMatchdays().collect(toList());
		assertThat(matches.size(), is(teams.size() - 1));
		int size = (int) matches.stream().map(SeasonTest::matchesOf).peek(d -> assertNoDuplicate(teams, d))
				.map(Collection::stream).flatMap(identity()).map(SeasonTest::pairAsStringLowerVsGreater).distinct()
				.count();

		int matchdays = teams.size() - 1;
		int matchsPerDay = teams.size() / 2;

		assertThat(size, is(matchsPerDay * matchdays));
	}

	static <T> Collection<Match<T>> matchesOf(Matchday<T> matchday) {
		return matchday.getMatches().collect(toList());
	}

	static String pairAsStringLowerVsGreater(Match<String> pair) {
		return Stream.of(pair.getTeam1(), pair.getTeam2()).sorted().collect(joining("-"));
	}

	static <T> void assertNoDuplicate(Collection<T> teams, Collection<Match<T>> days) {
		List<T> collected = teamStream(days).flatMap(Collection::stream).sorted().collect(toList());
		assertThat(collected, is(teams.stream().sorted().collect(toList())));
	}

	static <T> Stream<List<T>> teamStream(Collection<Match<T>> games) {
		return games.stream().map(p -> asList(p.getTeam1(), p.getTeam2()));
	}

}
