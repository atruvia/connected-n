package org.ase.fourwins.tournament;

import static java.lang.String.format;
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
import lombok.RequiredArgsConstructor;

public class DefaultTournament implements Tournament {

	@Getter
	private BoardInfo boardInfo = BoardInfo.sevenColsSixRows;

	@RequiredArgsConstructor
	static final class CoffeebreakGame implements Game {

		static final String COFFEE_BREAK_WIN_MESSAGE = "coffee break";

		private static final BoardInfo boardInfo = BoardInfo.builder().columns(0).rows(0).build();

		private final Player other;

		@Getter
		private final GameId id;

		@Override
		public BoardInfo getBoardInfo() {
			return boardInfo;
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

	private static final String COFFEE_BREAK_NAME = "CoffeeBreak";
	static final Player coffeeBreakPlayer = new Player(COFFEE_BREAK_NAME) {
		@Override
		public boolean joinGame(String opposite, BoardInfo boardInfo) {
			return false;
		}

		@Override
		protected int nextColumn() {
			// we don't join so we don't play
			throw new IllegalStateException(format("It's me, the %s, I don't want to play!", COFFEE_BREAK_NAME));
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
		return new Season<>(ensureEvenQuantity(players));
	}

	private List<Player> ensureEvenQuantity(Collection<? extends Player> players) {
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
		Player team1 = match.getTeam1();
		Player team2 = match.getTeam2();
		boolean team1IsCoffeBreak = isCoffeBreak(team1);
		boolean team2IsCoffeBreak = isCoffeBreak(team2);
		return team1IsCoffeBreak || team2IsCoffeBreak //
				? new CoffeebreakGame(team1IsCoffeBreak ? team2 : team1, gameId) //
				: new DefaultGame(moveListener, makeBoard(), gameId, team1, team2);
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
