package org.ase.fourwins.season;

import lombok.Value;

@Value
public class Match<T> {

	T team1, team2;

	public Match<T> reverse() {
		return new Match<T>(getTeam2(), getTeam1());
	}

}