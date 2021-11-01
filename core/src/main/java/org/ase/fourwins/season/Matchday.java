package org.ase.fourwins.season;

import static java.util.stream.IntStream.range;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class Matchday<T> {

	private final List<T> teams;

	public Stream<Match<T>> getMatches() {
		return range(0, matchCount(teams)).mapToObj(Matchday.this::makeMatch);
	}

	private static int matchCount(Collection<?> teams) {
		return teams.size() / 2;
	}

	protected Match<T> makeMatch(int offset) {
		return new Match<T>(teams.get(offset), teams.get(teams.size() - offset - 1));
	}

}