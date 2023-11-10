package org.ase.fourwins.season;

import static java.util.stream.Stream.concat;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

public class Season<T> {

	@RequiredArgsConstructor
	private static final class ReversedMatchday<U> implements Matchday<U> {
		private final Matchday<U> delegate;

		public Stream<Match<U>> getMatches() {
			return delegate.getMatches().map(Match::reverse);
		}
	}

	@Getter
	private final Round<T> firstRound, secondRound;

	public Season(List<T> teams) {
		List<T> unmodifableTeams = List.copyOf(teams);
		verifySizeIsEven(unmodifableTeams);
		this.firstRound = new Round<T>(unmodifableTeams, Season::matchday);
		this.secondRound = new Round<T>(unmodifableTeams, Season::reversedMatchday);
	}

	private static <U> Matchday<U> matchday(List<U> teams) {
		return new DefaultMatchday<U>(teams);
	}

	private static <U> Matchday<U> reversedMatchday(List<U> teams) {
		return new ReversedMatchday<U>(matchday(teams));
	}

	private void verifySizeIsEven(Collection<T> teams) {
		if (teams.size() % 2 != 0) {
			throw new IllegalArgumentException("Amount of teams must be even (was " + teams.size() + ")");
		}
	}

	public Stream<Matchday<T>> getMatchdays() {
		return concat(firstRound.getMatchdays(), secondRound.getMatchdays());
	}

}