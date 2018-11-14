package org.ase.fourwins.game;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static org.ase.fourwins.board.Board.Score.IN_GAME;
import static org.ase.fourwins.board.Board.Score.LOSE;
import static org.ase.fourwins.board.Move.moveToColumn;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

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

		private Iterable<T> elements;
		private Iterator<T> iterator;

		private InfiniteIterator(Iterable<T> elements) {
			this.iterator = (this.elements = elements).iterator();
		}

		@Override
		public T next() {
			if (!iterator.hasNext()) {
				iterator = elements.iterator();
			}
			return iterator.next();
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

	public DefaultGame(Player player1, Player player2, Board board) {
		validateTokens(player1, player2);

		informPlayer(player1, player2, board.boardInfo());
		informPlayer(player2, player1, board.boardInfo());

		this.board = board;
		this.players = unmodifiableList(new ArrayList<>(asList(player1, player2)));
		this.nextPlayer = new InfiniteIterator<Player>(players);
	}

	private boolean informPlayer(Player player1, Player player2, BoardInfo boardInfo) {
		return player1.joinGame(player2.getToken(), boardInfo);
	}

	protected void validateTokens(Player player1, Player player2) {
		String token1 = player1.getToken();
		String token2 = player2.getToken();
		if (Objects.equals(token1, token2)) {
			throw new RuntimeException("Players (" + player1 + ", " + player2 + ") with same token " + token1);
		}
	}

	@Override
	public Game runGame() {
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
		// TODO what todo if one of these methods will throw RTE? (player should lose?)
		int column = player.nextColumn();
		String token = player.getToken();
		// TODO log
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