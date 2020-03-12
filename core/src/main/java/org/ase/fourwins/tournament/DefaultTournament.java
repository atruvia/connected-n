package org.ase.fourwins.tournament;

import static java.util.function.Function.identity;
import static org.ase.fourwins.board.Board.Score.WIN;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.ase.fourwins.board.Board;
import org.ase.fourwins.board.Board.GameState;
import org.ase.fourwins.board.BoardInfo;
import org.ase.fourwins.game.DefaultGame;
import org.ase.fourwins.game.DefaultGame.MoveListener;
import org.ase.fourwins.game.Game;
import org.ase.fourwins.game.Game.GameId;
import org.ase.fourwins.game.Player;
import org.ase.fourwins.season.Match;
import org.ase.fourwins.season.Matchday;
import org.ase.fourwins.season.Season;
import org.ase.fourwins.tournament.listener.TournamentListener;

import lombok.Getter;

public class DefaultTournament implements Tournament {

	@Getter
	private BoardInfo boardInfo = BoardInfo.sevenColsSixRows;

	static final class CoffeebreakGame implements Game {

		static final String COFFEE_BREAK_WIN_MESSAGE = "coffee break";
		private final Player other;
		private final GameId gameId;

		private CoffeebreakGame(Player other, GameId gameId) {
			this.other = other;
			this.gameId = gameId;
		}

		@Override
		public GameId getId() {
			return gameId;
		}

		@Override
		public BoardInfo getBoardInfo() {
			return BoardInfo.builder().columns(0).rows(0).build();
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
			return List.of(other);
		}
	}

	private final List<TournamentListener> tournamentListenerList = new CopyOnWriteArrayList<>();
	private final MoveListener moveListener = (game, token, column) -> tournamentListenerList
			.forEach(l -> l.newTokenAt(game, token, column));

	static final Player coffeeBreakPlayer = new Player("CoffeeBreak") {
		@Override
		protected int nextColumn() {
			throw new IllegalStateException("It's me, the coffee break, I don't want to play!");
		}

		@Override
		public boolean joinGame(String opposite, BoardInfo boardInfo) {
			return false;
		}
	};

	@Override
	public void playSeason(Collection<? extends Player> players, Consumer<GameState> consumer) {
		seasonStarted();
		newSeason(players).getMatchdays().map(Matchday::getMatches) //
				.map(this::runMatches).flatMap(identity()) //
				.forEach(consumer);
		seasonEnded();
	}

	private Stream<GameState> runMatches(Stream<Match<Player>> matches) {
		return matches.map(this::newGame).peek(this::gameStarted).map(Game::runGame).peek(this::gameEnded)
				.map(Game::gameState).parallel();
	}

	private Season<Player> newSeason(Collection<? extends Player> players) {
		return new Season<>(toEventQuantity(players));
	}

	private List<Player> toEventQuantity(Collection<? extends Player> players) {
		List<Player> listOfPlayers = new ArrayList<>(players);
		return evenPlayerCount(listOfPlayers) ? listOfPlayers : addCoffeeBreakPlayer(listOfPlayers);
	}

	private boolean evenPlayerCount(List<Player> players) {
		return players.size() % 2 == 0;
	}

	private List<Player> addCoffeeBreakPlayer(List<Player> players) {
		players.add(coffeeBreakPlayer);
		return players;
	}

	private Game newGame(Match<Player> match) {
		// TODO change gameId to "<season>/<gameNo>"
		GameId gameId = GameId.random();
		if (isCoffeBreak(match.getTeam1())) {
			return new CoffeebreakGame(match.getTeam2(), gameId);
		} else if (isCoffeBreak(match.getTeam2())) {
			return new CoffeebreakGame(match.getTeam1(), gameId);
		}
		return new DefaultGame(moveListener, makeBoard(), gameId, match.getTeam1(), match.getTeam2());
	}

	private static boolean isCoffeBreak(Player player) {
		return player == coffeeBreakPlayer;
	}

	protected Board makeBoard() {
		return Board.newBoard(boardInfo);
	}

	protected void gameStarted(Game game) {
		tournamentListenerList.forEach(l -> l.gameStarted(game));
	}

	protected void gameEnded(Game game) {
		tournamentListenerList.forEach(l -> l.gameEnded(game));
	}

	protected void seasonStarted() {
		tournamentListenerList.forEach(TournamentListener::seasonStarted);
	}

	protected void seasonEnded() {
		tournamentListenerList.forEach(TournamentListener::seasonEnded);
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
