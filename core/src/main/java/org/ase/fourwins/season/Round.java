package org.ase.fourwins.season;

import static java.util.Spliterator.ORDERED;
import static java.util.Spliterators.spliterator;
import static java.util.stream.StreamSupport.stream;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import org.ase.fourwins.util.ListDelegate;

import lombok.Data;

@Data
public class Round<T> {

	private static final class RotatedLeagueList<T> extends ListDelegate<T> {

		private final int rotateBy;

		private RotatedLeagueList(List<T> delegate) {
			this(delegate, 1);
		}

		private RotatedLeagueList(List<T> delegate, int rotateBy) {
			// constructor is optimized by doing decomposition, this is pure performance
			// optimization only, could be removed without changing behavior. Just done for fun (and no profit) :D
			super(delegate instanceof RotatedLeagueList ? ((RotatedLeagueList<T>) delegate).getDelegate() : delegate);
			this.rotateBy = delegate instanceof RotatedLeagueList ? ((RotatedLeagueList<T>) delegate).rotateBy + rotateBy : rotateBy;
		}

		@Override
		public T get(int index) {
			return super.get(index == 0 ? 0 : rotatedIndex(index));
		}

		private int rotatedIndex(int index) {
			return (rotateBy + index - 1) % (size() - 1) + 1;
		}

	}

	private final List<T> teams;

	public Round(List<T> teams) {
		this.teams = List.copyOf(teams);
	}

	public Stream<Matchday<T>> getMatchdays() {
		return stream(spliterator(iterator(), elements(), ORDERED), false);
	}

	private Iterator<Matchday<T>> iterator() {
		return new Iterator<Matchday<T>>() {

			private int cnt;
			private List<T> teams = Round.this.teams;

			@Override
			public boolean hasNext() {
				return cnt < elements();
			}

			@Override
			public Matchday<T> next() {
				Matchday<T> matchday = matchday(teams);
				teams = new RotatedLeagueList<T>(teams);
				cnt++;
				return matchday;
			}
		};
	}

	private int elements() {
		return teams.size() - 1;
	}

	protected Matchday<T> matchday(List<T> teams) {
		return new Matchday<T>(teams);
	}

}