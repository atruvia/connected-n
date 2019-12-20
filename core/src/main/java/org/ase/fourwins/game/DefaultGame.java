package org.ase.fourwins.game;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.function.Predicate.isEqual;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.ase.fourwins.board.Board.Score.IN_GAME;
import static org.ase.fourwins.board.Board.Score.LOSE;
import static org.ase.fourwins.board.Move.moveToColumn;

import java.util.ArrayList;
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

	private static class GameLostDueToException implements Game {

		private final GameState state;
		private final List<Player> players;

		public GameLostDueToException(Player lostBy, String reason, List<Player> players) {
			this.players = players;
			this.state = GameState.builder().score(LOSE).token(lostBy.getToken()).reason(reason).build();
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

	@Getter
	private final List<Player> players;
	private final Iterator<Player> nextPlayer;
	private Board board;
	private int moves;

	public DefaultGame(Board board, Player... players) {
		validateTokens(players);
		List<Player> playerList = asList(players);
		playerList.forEach(p -> informPlayer(board.boardInfo(), p,
				playerList.stream().filter(isEqual(p).negate()).collect(toList())));
		this.board = board;
		this.players = unmodifiableList(new ArrayList<>(playerList));
		this.nextPlayer = new InfiniteIterator<Player>(asList(players));
	}

	private boolean informPlayer(BoardInfo boardInfo, Player player, List<Player> opposites) {
		return player.joinGame(opposites.stream().map(Player::getToken).collect(joining(",")), boardInfo);
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
			moves++;
			Player player = nextPlayer.next();
			try {
				makeMove(player);
			} catch (Exception e) {
				return new GameLostDueToException(player, e.getMessage(), players);
			}
		}
		return this;
	}

	private void makeMove(Player player) {
		int column = player.nextColumn();
		String token = player.getToken();
		board = board.insertToken(moveToColumn(column), token);
		this.players.stream().forEach(p -> p.tokenWasInserted(token, column));
	}

	@Override
	public GameState gameState() {
		return board.gameState();
	}

	public int getMoves() {
		return moves;
	}

	public String getStateString() {
		return gameState().getScore() + " " + gameState().getToken() + " (moves " + getMoves()
				+ ", winning combination: " + board.gameState().getWinningCombinations() + ")";

	}

}
