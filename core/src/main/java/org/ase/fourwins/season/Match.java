package org.ase.fourwins.season;

import lombok.Value;

@Value
public class Match<T> {
	private final T team1;
	private final T team2;
}