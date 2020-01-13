package org.ase.fourwins.board;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Spliterator.ORDERED;
import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.concat;
import static java.util.stream.StreamSupport.stream;
import static org.ase.fourwins.board.Board.Direction.EAST;
import static org.ase.fourwins.board.Board.Direction.NORTH;
import static org.ase.fourwins.board.Board.Direction.NORTHEAST;
import static org.ase.fourwins.board.Board.Direction.NORTHWEST;
import static org.ase.fourwins.board.Board.Direction.SOUTH;
import static org.ase.fourwins.board.Board.Direction.SOUTHEAST;
import static org.ase.fourwins.board.Board.Direction.SOUTHWEST;
import static org.ase.fourwins.board.Board.Direction.WEST;
import static org.ase.fourwins.board.Board.Line.fromTo;
import static org.ase.fourwins.board.Board.Mutator.DOWN;
import static org.ase.fourwins.board.Board.Mutator.LEFT;
import static org.ase.fourwins.board.Board.Mutator.RIGHT;
import static org.ase.fourwins.board.Board.Mutator.UP;
import static org.ase.fourwins.board.Board.Score.DRAW;
import static org.ase.fourwins.board.Board.Score.IN_GAME;
import static org.ase.fourwins.board.Board.Score.LOSE;
import static org.ase.fourwins.board.Board.Score.WIN;
import static org.ase.fourwins.board.Coordinate.xy;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;

public abstract class Board {

	@RequiredArgsConstructor
	enum Mutator {
		LEFT(-1, 0), RIGHT(+1, 0), DOWN(0, +1), UP(0, -1);

		private final int mutX, mutY;

		public Coordinate mutate(Coordinate coordinate) {
			return coordinate.mutate(mutX, mutY);
		}
	}

	enum Direction {
		NORTH(UP), SOUTH(DOWN), WEST(LEFT), EAST(RIGHT), //
		NORTHEAST(UP, RIGHT), SOUTHWEST(DOWN, LEFT), NORTHWEST(UP, LEFT), SOUTHEAST(DOWN, RIGHT);

		private final Mutator[] mutators;

		private Direction(Mutator... mutators) {
			this.mutators = mutators;
		}

		public Coordinate mutate(Coordinate coordinate) {
			for (Mutator mutator : mutators) {
				coordinate = mutator.mutate(coordinate);
			}
			return coordinate;
		}
	}

	@Value
	static class Line {

		private final Direction from, to;

		public static Line fromTo(Direction from, Direction to) {
			return new Line(from, to);
		}

	}

	@Value
	@Builder(toBuilder = true)
	public static class GameState {
		private Score score;
		private Object token;
		private String reason;
		private List<WinningCombination> winningCombinations;

		public List<WinningCombination> getWinningCombinations() {
			return winningCombinations == null ? emptyList() : winningCombinations;
		}

	}

	@Value
	public class WinningCombination {
		private final Object token;
		private final Coordinate coordinateTokenInserted;
		private final Set<Coordinate> coordinates;
	}

	private static final class DelegateBoard extends Board {

		private Board delegate;

		public DelegateBoard(Board delegate) {
			this.delegate = delegate;
		}

		@Override
		public GameState gameState() {
			return delegate.gameState();
		}

		@Override
		public BoardInfo boardInfo() {
			return delegate.boardInfo();
		}

		@Override
		public Board insertToken(Move move, Object token) {
			return (delegate = delegate.insertToken(move, token));
		}

	}

	private static class UnmodifableBoard extends Board {

		protected GameState gameState;
		protected BoardInfo boardInfo;

		public UnmodifableBoard(Score score, Object token, BoardInfo boardInfo) {
			this.boardInfo = boardInfo;
			this.gameState = GameState.builder().score(score).token(token).build();
		}

		@Override
		public GameState gameState() {
			return gameState;
		}

		@Override
		public BoardInfo boardInfo() {
			return boardInfo;
		}

		@Override
		public Board insertToken(Move move, Object token) {
			return this;
		}

	}

	private static class DrawBoard extends UnmodifableBoard {
		public DrawBoard(BoardInfo boardInfo) {
			super(DRAW, null, boardInfo);
		}

		@Override
		public Board insertToken(Move move, Object token) {
			return new LoserBoard(token, "COLUMN_IS_FULL", boardInfo);
		}

	}

	private static class WinnerBoard extends UnmodifableBoard {

		public WinnerBoard(Object token, List<WinningCombination> winningCombinatios, BoardInfo boardInfo) {
			super(WIN, token, boardInfo);
			this.gameState = gameState.toBuilder().reason("FOUR_IN_A_ROW").winningCombinations(winningCombinatios)
					.build();
		}

	}

	private static class LoserBoard extends UnmodifableBoard {

		public LoserBoard(Object token, String reason, BoardInfo boardInfo) {
			super(LOSE, token, boardInfo);
			this.gameState = gameState.toBuilder().reason(reason).build();
		}

	}

	private static class PlayableBoard extends Board {

		@FunctionalInterface
		private static interface Modifier {
			Coordinate modify(Coordinate coordinate);
		}

		private static class Column {

			private Object[] content;
			private int fillY;

			public Column(int height) {
				content = new String[height];
				fillY = content.length - 1;
			}

			private String getTokenAt(int y) {
				return content[y] == null ? " " : String.valueOf(content[y]);
			}

			public int insertToken(Object token) {
				content[fillY] = token;
				int cur = fillY;
				fillY--;
				return cur;
			}

			public boolean isFull() {
				return fillY < 0;
			}

		}

		private static final List<Line> lines = asList(fromTo(NORTH, SOUTH), fromTo(WEST, EAST),
				fromTo(SOUTHWEST, NORTHEAST), fromTo(NORTHWEST, SOUTHEAST));

		private final BoardInfo boardInfo;
		private final Column[] columns;
		private GameState gameState = GameState.builder().score(IN_GAME).build();

		private PlayableBoard(BoardInfo boardInfo) {
			this.boardInfo = boardInfo;
			this.columns = IntStream.range(0, boardInfo.getColumns()).mapToObj(i -> new Column(boardInfo.getRows()))
					.toArray(Column[]::new);
		}

		@Override
		public GameState gameState() {
			return gameState;
		}

		@Override
		public BoardInfo boardInfo() {
			return boardInfo;
		}

		/**
		 * not thread-safe, has to be synchronized or synchronized by caller
		 */
		@Override
		public Board insertToken(Move move, Object token) {
			int x = move.getColumnIdx();
			if (x < 0 || x >= columns.length) {
				return new LoserBoard(token, "ILLEGAL_COLUMN_ANNOUNCED", boardInfo);
			}
			Column column = columns[x];
			if (column.isFull()) {
				return new LoserBoard(token, "COLUMN_IS_FULL", boardInfo);
			}

			int y = column.insertToken(token);
			Coordinate posOfInsertedToken = xy(x, y);
			Collection<Line> connected = lines.stream()
					.filter(l -> connectedTokens(posOfInsertedToken, token, l).count() >= 4).collect(toList());
			if (connected.isEmpty()) {
				if (column.isFull() && allColumnsFull()) {
					return new DrawBoard(boardInfo());
				}
			} else {
				return new WinnerBoard(token, connected.stream()
						.map(l -> new WinningCombination(token, posOfInsertedToken,
								connectedTokens(posOfInsertedToken, token, l).collect(toSet())))
						.collect(toList()), boardInfo);
			}
			return this;
		}

		private boolean allColumnsFull() {
			return Arrays.stream(columns).allMatch(Column::isFull);
		}

		private Stream<Coordinate> connectedTokens(Coordinate center, Object token, Line line) {
			Stream<Coordinate> neighbours1 = reverse(neighboursOfSameToken(iterator(center, line.from), token));
			Stream<Coordinate> self = Stream.of(center);
			Stream<Coordinate> neighbours2 = neighboursOfSameToken(iterator(center, line.to), token);
			return concat(concat(neighbours1, self), neighbours2);
		}

		private static <T> Stream<T> reverse(Stream<T> stream) {
			return stream(
					spliteratorUnknownSize(stream.collect(toCollection(LinkedList::new)).descendingIterator(), ORDERED),
					false);
		}

		private Stream<Coordinate> neighboursOfSameToken(Iterator<Coordinate> iterator, Object token) {
			return stream(spliteratorUnknownSize(iterator, ORDERED), false)
					.takeWhile(c -> token.equals(columns[c.getColumn()].getTokenAt(c.getRow())));
		}

		private Iterator<Coordinate> iterator(Coordinate start, Direction direction) {
			return new Iterator<Coordinate>() {

				private Coordinate currentCoordinate = direction.mutate(start);

				@Override
				public boolean hasNext() {
					return inBound(currentCoordinate);
				}

				@Override
				public Coordinate next() {
					Coordinate result = currentCoordinate;
					currentCoordinate = direction.mutate(currentCoordinate);
					return result;
				}

				private boolean inBound(Coordinate coordinate) {
					return inBoundX(coordinate) && inBoundY(coordinate);
				}

				private boolean inBoundX(Coordinate coordinate) {
					return coordinate.getColumn() >= 0 && coordinate.getColumn() < boardInfo.getColumns();
				}

				private boolean inBoundY(Coordinate coordinate) {
					return coordinate.getRow() >= 0 && coordinate.getRow() < boardInfo.getRows();
				}

			};
		}

	}

	public enum Score {
		DRAW, IN_GAME, LOSE, WIN;
	}

	public static Board newBoard(BoardInfo boardInfo) {
		return new DelegateBoard(new PlayableBoard(boardInfo));
	}

	public abstract BoardInfo boardInfo();

	public abstract GameState gameState();

	public abstract Board insertToken(Move move, Object token);

}
