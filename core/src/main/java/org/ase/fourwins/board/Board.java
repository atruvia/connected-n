package org.ase.fourwins.board;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Spliterator.ORDERED;
import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.IntStream.range;
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
import static org.ase.fourwins.board.Board.Score.DRAW;
import static org.ase.fourwins.board.Board.Score.IN_GAME;
import static org.ase.fourwins.board.Board.Score.LOSE;
import static org.ase.fourwins.board.Board.Score.WIN;
import static org.ase.fourwins.board.Coordinate.xy;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.With;

public abstract class Board {

	@RequiredArgsConstructor
	enum Direction {
		NORTH(c -> c.mutateY(-1)), //
		SOUTH(c -> c.mutateY(+1)), //
		WEST(c -> c.mutateX(-1)), //
		EAST(c -> c.mutateX(+1)), //
		NORTHEAST(NORTH.and(EAST)), //
		SOUTHWEST(SOUTH.and(WEST)), //
		NORTHWEST(NORTH.and(WEST)), //
		SOUTHEAST(SOUTH.and(EAST));

		private final Function<Coordinate, Coordinate> mutator;

		private Function<Coordinate, Coordinate> and(Direction other) {
			return this.mutator.andThen(other.mutator);
		}

		public Coordinate mutate(Coordinate coordinate) {
			return this.mutator.apply(coordinate);
		}

	}

	@Value
	// fails @RequiredArgsConstructor(staticName = "fromTo")
	static class Line {

		Direction from, to;

		public static Line fromTo(Direction from, Direction to) {
			return new Line(from, to);
		}

	}

	@Value
	@Builder(toBuilder = true)
	public static class GameState {
		Score score;
		@With
		Object token;
		@With
		String reason;
		@With
		List<WinningCombination> winningCombinations;

		public List<WinningCombination> getWinningCombinations() {
			return winningCombinations == null ? emptyList() : winningCombinations;
		}

	}

	@Value
	public class WinningCombination {
		Object token;
		Coordinate coordinateTokenInserted;
		Set<Coordinate> coordinates;
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
			delegate = delegate.insertToken(move, token);
			return this;
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
			this.gameState = gameState.withReason("CONNECTED_LINE").withWinningCombinations(winningCombinatios);
		}

	}

	private static class LoserBoard extends UnmodifableBoard {

		public LoserBoard(Object token, String reason, BoardInfo boardInfo) {
			super(LOSE, token, boardInfo);
			this.gameState = gameState.withReason(reason);
		}

	}

	private static class PlayableBoard extends Board {

		private static class Column {

			private final Object[] content;
			private int fillY;

			public Column(int height) {
				content = new Object[height];
				fillY = content.length - 1;
			}

			private String getTokenAt(int y) {
				return content[y] == null ? null : String.valueOf(content[y]);
			}

			public int insertToken(Object token) {
				content[fillY] = token;
				return fillY--;
			}

			public boolean isFull() {
				return fillY < 0;
			}

		}

		private static final List<Line> lines = List.of( //
				fromTo(NORTH, SOUTH), //
				fromTo(WEST, EAST), //
				fromTo(SOUTHWEST, NORTHEAST), //
				fromTo(NORTHWEST, SOUTHEAST) //
		);

		private final BoardInfo boardInfo;
		private final Column[] columns;
		private GameState gameState = GameState.builder().score(IN_GAME).build();

		private PlayableBoard(BoardInfo boardInfo) {
			this.boardInfo = boardInfo;
			int rowCount = boardInfo.getRows();
			this.columns = range(0, boardInfo.getColumns()).mapToObj(__ -> rowCount).map(Column::new)
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
					.filter(l -> connectedTokens(posOfInsertedToken, token, l).count() >= boardInfo.getToConnect())
					.collect(toList());
			if (!connected.isEmpty()) {
				return new WinnerBoard(token, connected.stream()
						.map(l -> new WinningCombination(token, posOfInsertedToken,
								connectedTokens(posOfInsertedToken, token, l).collect(toSet())))
						.collect(toList()), boardInfo);
			}
			return isDraw(column) ? new DrawBoard(boardInfo()) : this;
		}

		private boolean isDraw(Column tokenPlacedInColumn) {
			return tokenPlacedInColumn.isFull() && allColumnsFull();
		}

		private boolean allColumnsFull() {
			return stream(columns).allMatch(Column::isFull);
		}

		private Stream<Coordinate> connectedTokens(Coordinate center, Object token, Line line) {
			Stream<Coordinate> neighbours1 = neighboursOfSameToken(center, token, line.from);
			Stream<Coordinate> self = Stream.of(center);
			Stream<Coordinate> neighbours2 = neighboursOfSameToken(center, token, line.to);
			return concat(concat(neighbours1, self), neighbours2);
		}

		private Stream<Coordinate> neighboursOfSameToken(Coordinate center, Object token, Direction direction) {
			return neighbours(iterator(center, direction)).takeWhile(c -> token.equals(tokenAt(c)));
		}

		private Stream<Coordinate> neighbours(Iterator<Coordinate> iterator) {
			return stream(spliteratorUnknownSize(iterator, ORDERED), false);
		}

		private String tokenAt(Coordinate coordinate) {
			return columns[coordinate.getX()].getTokenAt(coordinate.getY());
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
					int x = coordinate.getX();
					return x >= 0 && x < boardInfo.getColumns();
				}

				private boolean inBoundY(Coordinate coordinate) {
					int y = coordinate.getY();
					return y >= 0 && y < boardInfo.getRows();
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
