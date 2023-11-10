package org.ase.fourwins.season;

import static java.util.stream.Stream.concat;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import lombok.Getter;
import lombok.Value;

public class Season<T> {

	@Value
	private static final class ReversedRound<U> implements Round<U> {

		@Value
		private static final class ReversedMatchday<V> implements Matchday<V> {

			Matchday<V> delegate;

			public Stream<Match<V>> getMatches() {
				return delegate.getMatches().map(Match::reverse);
			}

		}

		Round<U> delegate;

		@Override
		public Stream<Matchday<U>> getMatchdays() {
			return delegate.getMatchdays().map(ReversedMatchday<U>::new);
		}

	}

	@Getter
	private final Round<T> firstRound, secondRound;

	public Season(List<T> teams) {
		List<T> unmodifableTeams = List.copyOf(teams);
		verifySizeIsEven(unmodifableTeams);
		this.firstRound = new DefaultRound<T>(unmodifableTeams);
		this.secondRound = new ReversedRound<T>(this.firstRound);
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