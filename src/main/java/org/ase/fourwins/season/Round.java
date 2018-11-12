package org.ase.fourwins.season;

import static java.util.Spliterator.ORDERED;
import static java.util.Spliterators.spliterator;
import static java.util.stream.StreamSupport.stream;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import org.ase.fourwins.util.ListDelegate;

import lombok.Data;

@Data
public class Round<T> {

	private static final class RotatedLeagueList<T> extends ListDelegate<T> {

		private RotatedLeagueList(List<T> delegate) {
			super(delegate);
		}

		@Override
		public T get(int index) {
			return super.get(rotateddIndex(index));
		}

		private int rotateddIndex(int index) {
			if (index != 0) {
				index++;
				if (index >= size()) {
					return index - size() + 1;
				}
			}
			return index;
		}

	}

	private final List<T> teams;
	private int count;

	public Round(List<T> teams) {
		this.teams = Collections.unmodifiableList(teams);
		this.count = teams.size() - 1;
	}

	public Stream<Matchday<T>> getMatchdays() {
		return stream(spliterator(iterator(), count, ORDERED), false);
	}

	private Iterator<Matchday<T>> iterator() {
		return new Iterator<Matchday<T>>() {

			private int i;
			private List<T> teams = Round.this.teams;

			@Override
			public boolean hasNext() {
				return i < count;
			}

			@Override
			public Matchday<T> next() {
				Matchday<T> matchday = matchday(teams);
				teams = new RotatedLeagueList<T>(teams);
				i++;
				return matchday;
			}
		};
	}

	protected Matchday<T> matchday(List<T> teams) {
		return new Matchday<T>(teams);
	}

}