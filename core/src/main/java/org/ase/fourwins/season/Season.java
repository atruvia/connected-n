package org.ase.fourwins.season;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import lombok.Value;

@Value
public class Season<T> {

	Round<T> firstRound, secondRound;

	public Season(List<T> teams) {
		firstRound = new DefaultRound<T>(ensureSizeIsEven(List.copyOf(teams)));
		secondRound = reverseEachMatch(firstRound);
	}

	private <C extends Collection<T>> C ensureSizeIsEven(C teams) {
		if (teams.size() % 2 != 0) {
			throw new IllegalArgumentException("Amount of teams must be even (was " + teams.size() + ")");
		}
		return teams;
	}

	private static <U> Round<U> reverseEachMatch(Round<U> round) {
		return () -> round.getMatchdays().map(m -> () -> m.getMatches().map(Match::reverse));
	}

	public Stream<Matchday<T>> getMatchdays() {
		return Stream.of(firstRound, secondRound).flatMap(Round<T>::getMatchdays);
	}

}