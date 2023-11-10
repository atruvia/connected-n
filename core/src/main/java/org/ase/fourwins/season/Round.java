package org.ase.fourwins.season;

import java.util.stream.Stream;

public interface Round<T> {
	Stream<Matchday<T>> getMatchdays();
}