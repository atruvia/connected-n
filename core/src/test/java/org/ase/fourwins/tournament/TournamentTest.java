package org.ase.fourwins.tournament;

import static java.util.Collections.addAll;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static net.jqwik.api.Arbitraries.integers;
import static net.jqwik.api.Arbitraries.strings;
import static org.ase.fourwins.board.Board.Score.IN_GAME;
import static org.ase.fourwins.board.Board.Score.LOSE;
import static org.ase.fourwins.board.Board.Score.WIN;
import static org.ase.fourwins.board.GameStateMatcher.isGameError;
import static org.ase.fourwins.tournament.TournamentTest.TournamentBuilder.tournament;
import static org.ase.fourwins.tournament.TournamentTest.TournamentBuilder.DummyBoard.LOSE_MESSAGE;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.ase.fourwins.board.Board;
import org.ase.fourwins.board.Board.GameState;
import org.ase.fourwins.board.BoardInfo;
import org.ase.fourwins.board.Move;
import org.ase.fourwins.board.mockplayers.PlayerMock;
import org.ase.fourwins.game.Player;
import org.ase.fourwins.tournament.DefaultTournament.CoffeebreakGame;
import org.ase.fourwins.tournament.listener.TournamentListener;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Disabled;

import net.jqwik.api.Arbitrary;
import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.arbitraries.IntegerArbitrary;

class TournamentTest {

	static final class TournamentBuilder {

		static final class DummyBoard extends Board {

			public static final String LOSE_MESSAGE = "dummy board lose message";
			private int moves;
			private GameState gameState = GameState.builder().score(IN_GAME).build();

			@Override
			public GameState gameState() {
				return gameState;
			}

			@Override
			public Board insertToken(Move move, Object token) {
				if (++moves == 7) {
					this.gameState = gameState.toBuilder() //
							.score(LOSE) //
							.token(token) //
							.reason(LOSE_MESSAGE) //
							.build();
				}
				return this;
			}

			@Override
			public BoardInfo boardInfo() {
				return BoardInfo.sevenColsSixRows;
			}
		}

		public static TournamentBuilder tournament() {
			return new TournamentBuilder();
		}

		private final List<Player> withPlayers = new ArrayList<>();
		private final List<TournamentListener> tournamentListenerList = new ArrayList<>();

		public TournamentBuilder withPlayers(Player... withPlayers) {
			addAll(this.withPlayers, withPlayers);
			return this;
		}

		public Tournament build() {
			Tournament tournament = new DefaultTournament() {

				@Override
				protected Board makeBoard() {
					return new DummyBoard();
				}

			};
			tournamentListenerList.forEach(tournament::addTournamentListener);
			return tournament;
		}

		public TournamentBuilder registerListener(TournamentListener listener) {
			tournamentListenerList.add(listener);
			return this;
		}

		public List<GameState> playSeason() {
			List<GameState> states = new ArrayList<>();
			build().playSeason(withPlayers, states::add);
			return states;
		}

	}

	@Example
	void twoPlayersPlayOneSeason() {
		PlayerMock p1 = mock("P1");
		PlayerMock p2 = mock("P2");

		List<GameState> states = tournament().withPlayers(p1, p2).playSeason();
		assertThat(states.size(), is(2));

		assertThat(p1.getMovesMade(), is(7));
		assertThat(p2.getMovesMade(), is(7));

		assertThat(states.get(0), isGameError(LOSE_MESSAGE).withToken("P1"));
		assertThat(states.get(1), isGameError(LOSE_MESSAGE).withToken("P2"));
	}

	@Example
	void tournamentWithOddPlayerCount() {
		PlayerMock p1 = mock("P1");
		PlayerMock p2 = mock("P2");
		PlayerMock p3 = mock("P3");

		List<GameState> states = tournament().withPlayers(p1, p2, p3).playSeason();

		assertOpponentsOf(p1, haveBeen(twoTimes("-- P2 P3")));
		assertOpponentsOf(p2, haveBeen(twoTimes("P3 P1 --")));
		assertOpponentsOf(p3, haveBeen(twoTimes("P2 -- P1")));

		int i = 0;
		assertThat(states.size(), is(3 * 2 * 2));
		assertThat(next(states, i += 2), is(List.of(coffeeBreakWin("P1"), lose("P2"))));
		assertThat(next(states, i += 2), is(List.of(lose("P1"), coffeeBreakWin("P3"))));
		assertThat(next(states, i += 2), is(List.of(lose("P1"), coffeeBreakWin("P2"))));

		assertThat(next(states, i += 2), is(List.of(coffeeBreakWin("P1"), lose("P3"))));
		assertThat(next(states, i += 2), is(List.of(lose("P2"), coffeeBreakWin("P3"))));
		assertThat(next(states, i += 2), is(List.of(lose("P3"), coffeeBreakWin("P2"))));
	}

	private String twoTimes(String string) {
		return string + " " + string;
	}

	private List<GameState> next(List<GameState> states, int index) {
		return states.subList(index - 2, index);
	}

	GameState coffeeBreakWin(String token) {
		return GameState.builder().score(WIN).token(token).reason(CoffeebreakGame.COFFEE_BREAK_WIN_MESSAGE).build();
	}

	GameState lose(String token) {
		return GameState.builder().score(LOSE).token(token).reason(LOSE_MESSAGE).build();
	}

	@Example
	void playersCanUseTheCoffeeBreakToken() {
		PlayerMock p1 = mock(DefaultTournament.coffeeBreakPlayer.getToken());
		tournament().withPlayers(p1).playSeason();
		assertOpponentsOf(p1, haveBeen("-- --"));
	}

	@Example
	@Disabled
	private void whatIsTheResultIfBothPlayersDontWantToPlay_SecondWins_or_Draw() {
		fail("implement");
	}

	@Property
	void jqwikCheckTest(@ForAll("players") List<PlayerMock> players, @ForAll("numberOfSeasons") int numberOfSeasons) {
		TournamentBuilder tournament = tournament().withPlayers(players.toArray(Player[]::new));
		range(0, numberOfSeasons).forEach(i -> tournament.playSeason());
		boolean shouldHaveMoved = numberOfSeasons > 0 && players.size() > 1;
		players.forEach(p -> assertThat(p.getMovesMade(), shouldHaveMoved ? moved() : not(moved())));
	}

	private Matcher<Integer> moved() {
		return not(is(0));
	}

	@Provide
	IntegerArbitrary numberOfSeasons() {
		return integers().between(0, 100);
	}

	@Provide
	Arbitrary<List<PlayerMock>> players() {
		return numberOfPlayers().flatMap( //
				stringsLength -> strings() //
						.alpha() //
						.ofLength(stringsLength) //
						.map(PlayerMock::new) //
						.list().uniqueElements(Player::getToken).ofMinSize(0).ofMaxSize(20));
	}

	private IntegerArbitrary numberOfPlayers() {
		return integers().between(2, 5);
	}

	void assertOpponentsOf(PlayerMock playerMock, List<String> expectedOpponents) {
		assertThat(playerMock.getOpponents(), is(expectedOpponents));
	}

	Tournament a(TournamentBuilder builder) {
		return builder.build();
	}

	List<String> haveBeen(String games) {
		return Stream.of(games.split("\\s")).filter(isCoffeeBreak().negate()).collect(toList());
	}

	Predicate<String> isCoffeeBreak() {
		return "--"::equals;
	}

	List<GameState> playSeasonOf(Tournament tournament, Collection<? extends Player> players) {
		List<GameState> states = new ArrayList<>();
		tournament.playSeason(players, states::add);
		return states;
	}

	PlayerMock mock(String token) {
		return new PlayerMock(token);
	}

}
