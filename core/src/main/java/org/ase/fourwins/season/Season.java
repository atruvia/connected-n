package org.ase.fourwins.season;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import lombok.Value;

@Value
public class Season<T> {

	Round<T> firstRound, secondRound;

	public Season(List<T> teams) {
		List<T> unmodifableTeams = List.copyOf(teams);
		verifySizeIsEven(unmodifableTeams);
		firstRound = new DefaultRound<T>(unmodifableTeams);
		secondRound = () -> firstRound.getMatchdays().map(m -> () -> m.getMatches().map(Match::reverse));
	}

	private void verifySizeIsEven(Collection<T> teams) {
		if (teams.size() % 2 != 0) {
			throw new IllegalArgumentException("Amount of teams must be even (was " + teams.size() + ")");
		}
	}

	public Stream<Matchday<T>> getMatchdays() {
		return Stream.of(firstRound, secondRound).flatMap(Round<T>::getMatchdays);
	}

}