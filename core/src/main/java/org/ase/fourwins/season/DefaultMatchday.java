package org.ase.fourwins.season;

import static java.util.stream.IntStream.range;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import lombok.Value;

@Value
public class DefaultMatchday<T> implements Matchday<T> {

	List<T> teams;

	public Stream<Match<T>> getMatches() {
		return range(0, matchCount(teams)).mapToObj(this::makeMatch);
	}

	private static int matchCount(Collection<?> teams) {
		return teams.size() / 2;
	}

	private Match<T> makeMatch(int offset) {
		return new Match<T>(team1(offset), team2(offset));
	}

	private T team1(int offset) {
		return teams.get(offset);
	}

	private T team2(int offset) {
		return teams.get(teams.size() - offset - 1);
	}

}