package org.ase.fourwins.season;

import lombok.Value;

@Value
public class Match<T> {
	T team1, team2;
}