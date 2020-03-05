package org.ase.fourwins.game;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static org.ase.fourwins.board.Board.Score.IN_GAME;
import static org.ase.fourwins.board.Board.Score.LOSE;
import static org.ase.fourwins.board.Move.moveToColumn;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.ase.fourwins.board.Board;
import org.ase.fourwins.board.Board.GameState;
import org.ase.fourwins.board.BoardInfo;

import lombok.Getter;

public class DefaultGame implements Game {

	public interface MoveListener {
		MoveListener NULL = (game, token, column) -> {
		};

		void newTokenAt(Game game, String token, int column);
	}

	private static class GameLostDueToException implements Game {

		private final BoardInfo boardInfo;
		private final GameId gameId;
		private final GameState state;
		private final List<Player> players;

		public GameLostDueToException(BoardInfo boardInfo, GameId gameId, Player lostBy, String reason,
				List<Player> players) {
			this.boardInfo = boardInfo;
			this.gameId = gameId;
			this.players = players;
			this.state = GameState.builder().score(LOSE).token(lostBy.getToken()).reason(reason).build();
		}

		@Override
		public GameId getId() {
			return gameId;
		}

		@Override
		public BoardInfo getBoardInfo() {
			return boardInfo;
		}

		@Override
		public Game runGame() {
			return this;
		}

		@Override
		public GameState gameState() {
			return state;
		}

		@Override
		public List<Player> getPlayers() {
			return players;
		}

	}

	private final static class InfiniteIterator<T> implements Iterator<T> {

		private final Iterable<T> elements;
		private Iterator<T> iterator;

		private InfiniteIterator(Iterable<T> elements) {
			this.iterator = (this.elements = elements).iterator();
		}

		@Override
		public T next() {
			return (iterator = iterator.hasNext() ? iterator : elements.iterator()).next();
		}

		@Override
		public boolean hasNext() {
			return true;
		}
	}

	private final MoveListener moveListener;
	private Board board;
	@Getter
	private final List<Player> players;
	private final Iterator<Player> nextPlayer;
	private final GameId gameId;

	public DefaultGame(Board board, GameId gameId, Player... players) {
		this(MoveListener.NULL, board, gameId, players);
	}

	public DefaultGame(MoveListener moveListener, Board board, GameId gameId, Player... players) {
		validateTokens(players);
		this.gameId = gameId;
		this.players = List.of(players);
		this.players.forEach(p -> informPlayer(board.boardInfo(), p));
		this.moveListener = moveListener;
		this.board = board;
		this.nextPlayer = new InfiniteIterator<Player>(List.of(players));
	}

	@Override
	public GameId getId() {
		return this.gameId;
	}

	@Override
	public BoardInfo getBoardInfo() {
		return board.boardInfo();
	}

	private boolean informPlayer(BoardInfo boardInfo, Player player) {
		return player.joinGame(getOpponentsForToken(player.getToken()).map(Player::getToken).collect(joining(",")),
				boardInfo);
	}

	protected void validateTokens(Player... players) {
		Set<String> allTokens = new HashSet<>();
		Set<Player> duplicates = Stream.of(players).filter(p -> !allTokens.add(p.getToken())).collect(toSet());
		if (!duplicates.isEmpty()) {
			throw new RuntimeException("Players (" + duplicates.stream().map(Object::toString).collect(joining(", "))
					+ ") with same tokens " + duplicates.stream().map(Player::getToken).collect(joining(", ")));
		}
	}

	@Override
	public Game runGame() {
		Game game = executeGame();
		GameState gameState = game.gameState();
		players.forEach(p -> p.gameEnded(gameState));
		return game;
	}

	private Game executeGame() {
		while (gameState().getScore() == IN_GAME) {
			Player player = nextPlayer.next();
			try {
				makeMove(player);
			} catch (Exception e) {
				return new GameLostDueToException(board.boardInfo(), getId(), player, e.getMessage(), players);
			}
		}
		return this;
	}

	private void makeMove(Player player) {
		int column = player.nextColumn();
		String token = player.getToken();
		this.moveListener.newTokenAt(this, token, column);
		this.board = this.board.insertToken(moveToColumn(column), token);
		this.players.stream().forEach(p -> sendTokenWasInserted(p, token, column));
	}

	private void sendTokenWasInserted(Player player, String token, int column) {
		try {
			player.tokenWasInserted(token, column);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public GameState gameState() {
		return board.gameState();
	}

}
