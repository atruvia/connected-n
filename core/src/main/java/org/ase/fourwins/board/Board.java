package org.ase.fourwins.board;

import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.Spliterator.ORDERED;
import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static java.util.stream.StreamSupport.stream;
import static org.ase.fourwins.board.Board.Score.DRAW;
import static org.ase.fourwins.board.Board.Score.IN_GAME;
import static org.ase.fourwins.board.Board.Score.LOSE;
import static org.ase.fourwins.board.Board.Score.WIN;
import static org.ase.fourwins.board.Position.xy;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import lombok.Builder;
import lombok.Value;

public abstract class Board {

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
		private final Position positionTokenInserted;
		private final Set<Position> positions;
		private final String directions;
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

		private static final int CONNECT_N = 4;

		@FunctionalInterface
		private static interface Modifier {

			default Modifier and(Modifier other) {
				return (c) -> other.modify(modify(c));
			}

			Position modify(Position position);

		}

		private static final Modifier columnLeft = p -> p.increaseColumn(-1);
		private static final Modifier columnRight = p -> p.increaseColumn(+1);
		private static final Modifier rowDown = p -> p.increaseRow(-1);
		private static final Modifier rowUp = p -> p.increaseRow(+1);

		public static enum Direction {
			NORTH(rowUp), //
			NORTHEAST(rowUp.and(columnRight)), //
			EAST(columnRight), //
			SOUTHEAST(rowDown.and(columnRight)), //
			SOUTH(rowDown), //
			SOUTHWEST(rowDown.and(columnLeft)), //
			WEST(columnLeft), //
			NORTHWEST(rowUp.and(columnLeft)), //
			;

			private final Modifier modifier;

			private Direction(Modifier modifier) {
				this.modifier = modifier;
			}

		}

		private final CombinationEvaluator sn = new CombinationEvaluator(Direction.SOUTH, Direction.NORTH);
		private final CombinationEvaluator we = new CombinationEvaluator(Direction.WEST, Direction.EAST);
		private final CombinationEvaluator swne = new CombinationEvaluator(Direction.SOUTHWEST, Direction.NORTHEAST);
		private final CombinationEvaluator senw = new CombinationEvaluator(Direction.SOUTHEAST, Direction.NORTHWEST);
		private final List<CombinationEvaluator> evaluators = Collections
				.unmodifiableList(Arrays.asList(sn, we, swne, senw));

		private class Column {

			private final int columnIdx;
			private int currentRow;

			public Column(int columnIdx) {
				this.columnIdx = columnIdx;
			}

			public int insert(Object token) {
				PlayableBoard.this.values[position(columnIdx, currentRow)] = token;
				return currentRow++;
			}

			private boolean isFilledUp() {
				return currentRow == height;
			}

		}

		private class PositionIterator implements Iterator<Position> {

			private Position position;
			private final Direction direction;

			public PositionIterator(Position position, Direction direction) {
				this.direction = direction;
				this.position = direction.modifier.modify(position);
			}

			@Override
			public Position next() {
				Position old = this.position;
				this.position = direction.modifier.modify(old);
				return old;
			}

			@Override
			public boolean hasNext() {
				return columnInRange() && rowInRange();
			}

			private boolean columnInRange() {
				return position.getColumn() >= 0 && position.getColumn() < columns.size();
			}

			private boolean rowInRange() {
				return position.getRow() >= 0 && position.getRow() < height;
			}

			@Override
			public String toString() {
				return "PositionIterator [position=" + position + ", direction=" + direction + "]";
			}

		}

		private final List<Column> columns;
		private final int height;
		private GameState gameState = GameState.builder().score(IN_GAME).build();

		private final Object[] values;
		private final BoardInfo boardInfo;

		private PlayableBoard(BoardInfo boardInfo) {
			this.boardInfo = boardInfo;
			this.height = boardInfo.getRows();
			this.values = new Object[boardInfo.getColumns() * boardInfo.getRows()];
			this.columns = range(0, boardInfo.getColumns()).mapToObj(Column::new).collect(toList());
		}

		private boolean allAre(Predicate<? super Column> predicate) {
			return this.columns.stream().allMatch(predicate);
		}

		private int position(int columnIdx, int row) {
			return columnIdx * height + row;
		}

		@Deprecated
		private String dumpBoard() {
			StringBuilder sb = new StringBuilder();
			for (int row = height - 1; row >= 0; row--) {
				for (int col = 0; col < columns.size(); col++) {
					Object object = tokenAt(col, row);
					sb = sb.append(object == null ? " " : object);
				}
				sb.append("\n");

			}
			return sb.toString();
		}

		@Override
		public GameState gameState() {
			return gameState;
		}

		@Override
		public BoardInfo boardInfo() {
			return boardInfo;
		}

		private List<WinningCombination> getWinningCombinatios(Object token, int atLeast, Position startAt) {
			return evaluators.stream().filter(e -> e.neighboursOfSameToken(startAt, token).count() + 1 >= atLeast) //
					.map(e -> toWinningCombination(token, startAt, e)) //
					.collect(toList());
		}

		private WinningCombination toWinningCombination(Object token, Position startAt,
				CombinationEvaluator evaluator) {
			return new WinningCombination(token, startAt,
					winningPositionAndNeighbours(token, startAt, evaluator)
							.sorted(comparing(Position::getColumn).thenComparing(Position::getRow))
							.collect(toCollection(LinkedHashSet::new)),
					evaluator.getDirections());

		}

		protected Stream<Position> winningPositionAndNeighbours(Object token, Position startAt,
				CombinationEvaluator evaluator) {
			return Stream.concat(evaluator.neighboursOfSameToken(startAt, token).collect(toList()).stream(),
					Stream.of(startAt));
		}

		@Value
		private class CombinationEvaluator {

			private final Direction from;
			private final Direction to;

			private Stream<Position> neighboursOfSameToken(Position startAt, Object token) {
				return Stream.concat(stream(startAt, token, from), stream(startAt, token, to));
			}

			private Stream<Position> stream(Position startAt, Object token, Direction direction) {
				return connectedTokens(iter(startAt, direction), token);
			}

			public String getDirections() {
				return from + " -> " + to;
			}

			@Override
			public String toString() {
				return "CombinationEvaluator [from=" + from + ", to=" + to + "]";
			}

		}

		/**
		 * not thread-safe, has to be synchronized or synchronized by caller
		 */
		@Override
		public Board insertToken(Move move, Object token) {
			int columnIdx = move.getColumnIdx();
			if (columnIdx < 0 || columnIdx >= columns.size()) {
				return new LoserBoard(token, "ILLEGAL_COLUMN_ANNOUNCED", boardInfo);
			}

			Column column = this.columns.get(columnIdx);
			if (column.isFilledUp()) {
				return new LoserBoard(token, "COLUMN_IS_FULL", boardInfo);
			} else {
				List<WinningCombination> winningCombinatios = getWinningCombinatios(token, CONNECT_N,
						xy(columnIdx, column.insert(token)));
				if (winningCombinatios.size() > 0) {
					return new WinnerBoard(token, winningCombinatios, boardInfo);
				} else if (column.isFilledUp() && allAre(Column::isFilledUp)) {
					return new DrawBoard(boardInfo());
				} else {
					return this;
				}
			}
		}

		private PositionIterator iter(Position position, Direction direction) {
			return new PositionIterator(position, direction);
		}

		private Stream<Position> connectedTokens(Iterator<Position> iterator, Object token) {
			return stream(spliteratorUnknownSize(iterator, ORDERED), false).takeWhile(c -> token.equals(tokenAt(c)));
		}

		private Object tokenAt(Position position) {
			return tokenAt(position.getColumn(), position.getRow());
		}

		private Object tokenAt(int col, int row) {
			return values[position(col, row)];
		}

	}

	public enum Score {
		DRAW, IN_GAME, LOSE, WIN;
	}

	public static Board newBoard(int columnCount, int rowCount) {
		return new DelegateBoard(new PlayableBoard(new BoardInfo(columnCount, rowCount)));
	}

	public static Board newBoard(BoardInfo boardInfo) {
		return new DelegateBoard(new PlayableBoard(boardInfo));
	}

	public abstract BoardInfo boardInfo();

	public abstract GameState gameState();

	public abstract Board insertToken(Move move, Object token);

}
