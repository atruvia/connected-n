package org.ase.fourwins.tournament;

import static java.util.function.Function.identity;
import static org.ase.fourwins.board.Board.Score.WIN;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.ase.fourwins.board.Board;
import org.ase.fourwins.board.BoardInfo;
import org.ase.fourwins.board.Board.GameState;
import org.ase.fourwins.game.DefaultGame;
import org.ase.fourwins.game.Game;
import org.ase.fourwins.game.Player;
import org.ase.fourwins.season.Match;
import org.ase.fourwins.season.Matchday;
import org.ase.fourwins.season.Season;

public class Tournament {

	private BoardInfo boardInfo = BoardInfo.sevenColsSixRows;

	static final class CoffeebreakGame implements Game {

		static final String COFFEE_BREAK_WIN_MESSAGE = "coffee break";
		private final Object other;

		CoffeebreakGame(Object other) {
			this.other = other;
		}

		@Override
		public GameState gameState() {
			return GameState.builder().score(WIN).token(other).reason(COFFEE_BREAK_WIN_MESSAGE).build();
		}

		@Override
		public Game runGame() {
			return this;
		}
	}

	private final List<Player> players = new ArrayList<>();

	static final Player coffeeBreakPlayer = new Player("CoffeeBreak") {
		@Override
		protected int nextColumn() {
			throw new RuntimeException("I am the coffee break");
		}

		@Override
		public boolean joinGame(String opposite, BoardInfo boardInfo) {
			return false;
		}
	};

	public Stream<GameState> playSeason() {
		return newSeason().getMatchdays().map(Matchday::getMatches).map(this::runMatches).flatMap(identity());
	}

	private Stream<GameState> runMatches(Stream<Match<Player>> matches) {
		return matches.map(this::newGame).map(Game::runGame).peek(this::gameEnded).map(Game::gameState).parallel();
	}

	private Season<Player> newSeason() {
		synchronized (players) {
			List<Player> playersClone = new ArrayList<>(players);
			return new Season<>(evenPlayerCount() ? playersClone : addCoffeeBreakPlayer(playersClone));
		}
	}

	private boolean evenPlayerCount() {
		synchronized (players) {
			return players.size() % 2 == 0;
		}
	}

	private List<Player> addCoffeeBreakPlayer(List<Player> playersClone) {
		synchronized (players) {
			playersClone.add(coffeeBreakPlayer);
			return playersClone;
		}
	}

	public Tournament registerPlayer(Player player) {
		String token = player.getToken();
		synchronized (players) {
			if (players.stream().map(Player::getToken).anyMatch(token::equals)) {
				throw new RuntimeException("Token " + player.getToken() + " alreay taken");
			}
			players.add(player);
		}
		return this;
	}

	public Tournament deregisterPlayer(Player player) {
		synchronized (players) {
			players.remove(player);
		}
		return this;
	}

	private Game newGame(Match<Player> match) {
		if (match.getTeam1() == coffeeBreakPlayer) {
			return new CoffeebreakGame(match.getTeam2().getToken());
		} else if (match.getTeam2() == coffeeBreakPlayer) {
			return new CoffeebreakGame(match.getTeam1().getToken());
		}
		Board board = makeBoard();
		return new DefaultGame(match.getTeam1(), match.getTeam2(), board);
	}

	protected Board makeBoard() {
		return Board.newBoard(boardInfo);
	}

	protected void gameEnded(Game game) {
		// hook method
	}

}
