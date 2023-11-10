package org.ase.fourwins.season;

import java.util.stream.Stream;

public interface Matchday<T> {
	Stream<Match<T>> getMatches();
}