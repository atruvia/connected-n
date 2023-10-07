package org.ase.fourwins.season;

import static java.util.stream.Stream.concat;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import lombok.Getter;

public class Season<T> {

	private static final class ReversedRound<T> extends Round<T> {

		private ReversedRound(List<T> teams) {
			super(teams);
		}

		@Override
		protected Matchday<T> matchday(List<T> teams) {
			return new ReversedMatchday<T>(teams);
		}

	}

	private static final class ReversedMatchday<T> extends Matchday<T> {

		private ReversedMatchday(List<T> teams) {
			super(teams);
		}

		@Override
		protected Match<T> makeMatch(int offset) {
			Match<T> match = super.makeMatch(offset);
			return new Match<T>(match.getTeam2(), match.getTeam1());
		}

	}

	@Getter
	private final Round<T> firstRound, secondRound;

	public Season(List<T> teams) {
		List<T> unmodifableTeams = List.copyOf(teams);
		verifySizeIsEven(unmodifableTeams);
		this.firstRound = new Round<T>(unmodifableTeams);
		this.secondRound = new ReversedRound<T>(unmodifableTeams);
	}

	private void verifySizeIsEven(Collection<T> teams) {
		if (teams.size() % 2 != 0) {
			throw new IllegalArgumentException("Amount of teams must be even (was " + teams.size() + ")");
		}
	}

	public Stream<Matchday<T>> getMatchdays() {
		return concat(firstRound.getMatchdays(), secondRound.getMatchdays());
	}

}