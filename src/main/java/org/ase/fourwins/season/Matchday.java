package org.ase.fourwins.season;

import static java.util.Spliterator.ORDERED;
import static java.util.Spliterators.spliterator;
import static java.util.stream.StreamSupport.stream;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import lombok.Data;

@Data
public class Matchday<T> {

	private final List<T> teams;
	private final int matchCount;

	public Matchday(List<T> teams) {
		this.teams = teams;
		this.matchCount = teams.size() / 2;
	}

	public Stream<Match<T>> getMatches() {
		return stream(spliterator(iterator(), matchCount, ORDERED), false);

	}

	protected Match<T> makeMatch(int i) {
		return new Match<T>(teams.get(i), teams.get(teams.size() - i - 1));
	}

	private Iterator<Match<T>> iterator() {
		return new Iterator<Match<T>>() {

			private int i;

			@Override
			public boolean hasNext() {
				return i < matchCount;
			}

			@Override
			public Match<T> next() {
				return incrementCounter(makeMatch(i));
			}

			private Match<T> incrementCounter(Match<T> match) {
				i++;
				return match;
			}
		};
	}

}