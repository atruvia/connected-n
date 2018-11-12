package org.ase.fourwins.season;

import java.util.List;
import java.util.stream.Stream;

public class Season<T> {

	private final Round<T> firstRound;
	private Round<T> backRound;

	public Season(List<T> teams) {
		this.firstRound = new Round<T>(teams);
		this.backRound = new Round<T>(teams) {
			@Override
			protected Matchday<T> matchday(List<T> teams) {
				return new Matchday<T>(teams) {
					@Override
					protected Match<T> makeMatch(int i) {
						Match<T> pair = super.makeMatch(i);
						return new Match<T>(pair.getTeam2(), pair.getTeam1());
					}
				};
			}
		};
	}

	public Round<T> getFirstRound() {
		return this.firstRound;
	}

	public Round<T> getBackRound() {
		return this.backRound;
	}

	public Stream<Matchday<T>> getMatchdays() {
		return Stream.concat(firstRound.getMatchdays(), backRound.getMatchdays());
	}

}