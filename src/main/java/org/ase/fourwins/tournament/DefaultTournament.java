package org.ase.fourwins.tournament;

import static java.util.function.Function.identity;
import static org.ase.fourwins.board.Board.Score.WIN;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.ase.fourwins.board.Board;
import org.ase.fourwins.board.Board.GameState;
import org.ase.fourwins.board.BoardInfo;
import org.ase.fourwins.game.DefaultGame;
import org.ase.fourwins.game.Game;
import org.ase.fourwins.game.Player;
import org.ase.fourwins.season.Match;
import org.ase.fourwins.season.Matchday;
import org.ase.fourwins.season.Season;
import org.ase.fourwins.tournament.listener.TournamentListener;

import lombok.Getter;

public class DefaultTournament implements Tournament {

	private static class DefaultRegistrationResult implements RegistrationResult {
		static RegistrationResult OK = new DefaultRegistrationResult(true);
		@Getter
		private boolean ok;

		public DefaultRegistrationResult(boolean ok) {
			this.ok = ok;
		}

		public static RegistrationResult tokenAlreadyTaken(String token) {
			return new DefaultRegistrationResult(false);
		}
	}

	private BoardInfo boardInfo = BoardInfo.sevenColsSixRows;

	static final class CoffeebreakGame implements Game {

		static final String COFFEE_BREAK_WIN_MESSAGE = "coffee break";
		private final Player other;

		CoffeebreakGame(Player other) {
			this.other = other;
		}

		@Override
		public GameState gameState() {
			return GameState.builder().score(WIN).token(other.getToken()).reason(COFFEE_BREAK_WIN_MESSAGE).build();
		}

		@Override
		public Game runGame() {
			return this;
		}

		@Override
		public List<Player> getPlayers() {
			return Arrays.asList(other);
		}
	}

	private final List<Player> players = new ArrayList<>();
	private final List<TournamentListener> tournamentListenerList = new CopyOnWriteArrayList<>();

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

	@Override
	public void playSeason(Consumer<GameState> consumer) {
		newSeason().getMatchdays().map(Matchday::getMatches) //
				.map(this::runMatches).flatMap(identity()) //
				.forEach(consumer);
		seasonEnded();
	}

	private Stream<GameState> runMatches(Stream<Match<Player>> matches) {
		return matches.map(this::newGame).peek(this::gameStarted).map(Game::runGame).peek(this::gameEnded)
				.map(Game::gameState).parallel();
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

	@Override
	public RegistrationResult registerPlayer(Player player) {
		String token = player.getToken();
		synchronized (players) {
			if (players.stream().map(Player::getToken).anyMatch(token::equals)) {
				return DefaultRegistrationResult.tokenAlreadyTaken(token);
			}
			players.add(player);
		}
		return DefaultRegistrationResult.OK;
	}

	@Override
	public Tournament deregisterPlayer(Player player) {
		synchronized (players) {
			players.remove(player);
		}
		return this;
	}

	private Game newGame(Match<Player> match) {
		if (match.getTeam1() == coffeeBreakPlayer) {
			return new CoffeebreakGame(match.getTeam2());
		} else if (match.getTeam2() == coffeeBreakPlayer) {
			return new CoffeebreakGame(match.getTeam1());
		}
		Board board = makeBoard();
		return new DefaultGame(match.getTeam1(), match.getTeam2(), board);
	}

	protected Board makeBoard() {
		return Board.newBoard(boardInfo);
	}

	protected void gameStarted(Game game) {
		tournamentListenerList.forEach(listener -> listener.gameStarted(game));
	}

	protected void gameEnded(Game game) {
		tournamentListenerList.forEach(listener -> listener.gameEnded(game));
	}

	protected void seasonEnded() {
		tournamentListenerList.forEach(listener -> listener.seasonEnded());
	}

	@Override
	public void addTournamentListener(TournamentListener listener) {
		tournamentListenerList.add(listener);
	}

	@Override
	public void removeTournamentListener(TournamentListener listener) {
		tournamentListenerList.remove(listener);
	}

}
