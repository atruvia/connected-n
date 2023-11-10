package org.ase.fourwins.season;

import static java.lang.Math.max;
import static java.util.stream.Stream.iterate;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import org.ase.fourwins.util.ListDelegate;

import lombok.Value;

@Value
public class DefaultRound<T> implements Round<T> {

	/**
	 * When accessing this list at a certain index the result is not the element at
	 * that index but the element next to it (better said n-elements, see
	 * {@link #shiftBy}. This does <b>not</b> apply for the element at index
	 * <code>0</code>.
	 * <p>
	 * So all elements but the first elements are rotated by {@link #shiftBy}.
	 * <p>
	 * Example: When accessing the underlying list with elements A,B,C,D via
	 * {@link #get(int)} you will get the results as follow:
	 * 
	 * <pre>
	 * given {@link #shiftBy} is <code>1</code>
	 * index 0 -> index 0 (A) (element at index <code>0</code> is always fix)
	 * index 1 -> index 2 (C)
	 * index 2 -> index 3 (D)
	 * index 3 -> index 1 (B)
	 * 
	 * given {@link #shiftBy} is <code>2</code>
	 * index 0 -> index 0 (A) (element at index <code>0</code> is always fix)
	 * index 1 -> index 3 (D)
	 * index 2 -> index 1 (B)
	 * index 3 -> index 2 (C)
	 * </pre>
	 *
	 */
	private static final class RotatedLeagueList<T> extends ListDelegate<T> {

		/** how far should the shifting be done */
		private final int shiftBy;

		private RotatedLeagueList(List<T> delegate) {
			this(delegate, 1);
		}

		private RotatedLeagueList(List<T> delegate, int shiftBy) {
			// constructor is optimized by doing decomposition, this is pure performance
			// optimization only, could be removed without changing behavior. Just done for
			// fun (and no profit) :D
			super(delegate instanceof RotatedLeagueList ? ((RotatedLeagueList<T>) delegate).getDelegate() : delegate);
			this.shiftBy = delegate instanceof RotatedLeagueList ? ((RotatedLeagueList<T>) delegate).shiftBy + shiftBy
					: shiftBy;
		}

		@Override
		public T get(int index) {
			return super.get(index == 0 ? 0 : shiftedIndex(index));
		}

		private int shiftedIndex(int index) {
			return (shiftBy + index - 1) % (size() - 1) + 1;
		}

	}

	List<T> teams;

	public DefaultRound(List<T> teams) {
		this.teams = List.copyOf(teams);
	}

	public Stream<Matchday<T>> getMatchdays() {
		return iterate(teams, RotatedLeagueList<T>::new).limit(matchdays(teams)).map(DefaultMatchday::new);
	}

	private static int matchdays(Collection<?> teams) {
		return max(0, teams.size() - 1);
	}

}