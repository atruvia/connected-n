package org.ase.fourwins.tournament;

import static java.util.Arrays.asList;
import static java.util.Collections.addAll;
import static java.util.stream.Collectors.toList;
import static org.ase.fourwins.board.Board.Score.IN_GAME;
import static org.ase.fourwins.board.Board.Score.LOSE;
import static org.ase.fourwins.board.Board.Score.WIN;
import static org.ase.fourwins.board.GameStateMatcher.isGameError;
import static org.ase.fourwins.tournament.TournamentTest.TournamentBuilder.tournament;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.ase.fourwins.board.Board;
import org.ase.fourwins.board.Board.GameState;
import org.ase.fourwins.board.BoardInfo;
import org.ase.fourwins.board.Move;
import org.ase.fourwins.board.mockplayers.PlayerMock;
import org.ase.fourwins.game.Game;
import org.ase.fourwins.game.Player;
import org.ase.fourwins.tournament.Tournament.CoffeebreakGame;
import org.ase.fourwins.tournament.TournamentTest.TournamentBuilder.DummyBoard;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Disabled;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.arbitraries.IntegerArbitrary;

public class TournamentTest {

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
		private final List<Player> registerAfterwards = new ArrayList<>();
		private final List<Player> deregisterAfterwards = new ArrayList<>();

		public TournamentBuilder withPlayers(Player... withPlayers) {
			addAll(this.withPlayers, withPlayers);
			return this;
		}

		public TournamentBuilder registerAfterwards(Player... registerAfterwards) {
			addAll(this.registerAfterwards, registerAfterwards);
			return this;
		}

		public TournamentBuilder deregisterAfterwards(Player... deregisterAfterwards) {
			addAll(this.deregisterAfterwards, deregisterAfterwards);
			return this;
		}

		public Tournament build() {
			Tournament tournament = new Tournament() {

				private final AtomicInteger gamesStarted = new AtomicInteger(0);

				@Override
				protected Board makeBoard() {
					return new DummyBoard();
				}

				@Override
				protected void gameEnded(Game game) {
					super.gameEnded(game);
					if (gamesStarted.incrementAndGet() == 1) {
						registerAfterwards.stream().forEach(this::registerPlayer);
						deregisterAfterwards.stream().forEach(this::deregisterPlayer);
					}
				}

			};
			withPlayers.stream().forEach(tournament::registerPlayer);
			return tournament;
		}

	}

	@Example
	void cannotRegisterWithIdenticalName() {
		String token = "aTokenTakenTwoTimes";
		Tournament tournament = a(tournament().withPlayers(mock(token)));
		assertThrows(RuntimeException.class, () -> {
			tournament.registerPlayer(mock(token));
		});
	}

	@Example
	void twoPlayersPlayOneSeason() {
		PlayerMock p1 = mock("P1");
		PlayerMock p2 = mock("P2");

		List<GameState> states = playSeasonOf(a(tournament().withPlayers(p1, p2)));
		assertThat(states.size(), is(2));

		assertThat(p1.getMovesMade(), is(7));
		assertThat(p2.getMovesMade(), is(7));

		assertThat(states.get(0), isGameError(DummyBoard.LOSE_MESSAGE).withToken("P1"));
		assertThat(states.get(1), isGameError(DummyBoard.LOSE_MESSAGE).withToken("P2"));
	}

	@Example
	void whenJoiningTheStartedSeasonThereWillBeNoInteractionWithTheNewlyJoinedPlayer() {
		PlayerMock p1 = mock("P1");
		PlayerMock p2 = mock("P2");
		PlayerMock p3 = mock("P3");

		playSeasonOf(a(tournament().withPlayers(p1, p2)));
		assertThat(p3.getMovesMade(), is(0));
	}

	@Example
	void theJoinedPlayersWillBePartOfTheNextSeason() {
		PlayerMock p1 = mock("P1");
		PlayerMock p2 = mock("P2");
		PlayerMock p3 = mock("P3");
		PlayerMock p4 = mock("P4");

		Tournament tournament = a(tournament().withPlayers(p1, p2).registerAfterwards(p3, p4));

		playSeasonOf(tournament);
		assertOpponentsOf(p1, haveBeen(twoTimes("P2")));
		assertOpponentsOf(p2, haveBeen(twoTimes("P1")));
		assertOpponentsOf(p3, haveBeen(twoTimes("--")));
		assertOpponentsOf(p4, haveBeen(twoTimes("--")));

		playSeasonOf(tournament);
		assertOpponentsOf(p1, haveBeen("P2 P2 P4 P2 P3 P4 P2 P3"));
		assertOpponentsOf(p2, haveBeen("P1 P1 P3 P1 P4 P3 P1 P4"));
		assertOpponentsOf(p3, haveBeen("-- -- P2 P4 P1 P2 P4 P1"));
		assertOpponentsOf(p4, haveBeen("-- -- P1 P3 P2 P1 P3 P2"));
	}

	@Example
	void aDereigsteredPlayerWillStillBePartOfTheRunningSeason() {
		PlayerMock p1 = mock("P1");
		PlayerMock p2 = mock("P2");
		PlayerMock p3 = mock("P3");
		PlayerMock p4 = mock("P4");

		Tournament tournament = a(tournament().withPlayers(p1, p2, p3, p4).deregisterAfterwards(p1, p4));

		playSeasonOf(tournament);
		assertOpponentsOf(p1, haveBeen(twoTimes("P4 P2 P3")));
		assertOpponentsOf(p2, haveBeen(twoTimes("P3 P1 P4")));
		assertOpponentsOf(p3, haveBeen(twoTimes("P2 P4 P1")));
		assertOpponentsOf(p4, haveBeen(twoTimes("P1 P3 P2")));

		playSeasonOf(tournament);
		assertOpponentsOf(p1, haveBeen(twoTimes("P4 P2 P3") + " " + twoTimes("--")));
		assertOpponentsOf(p2, haveBeen(twoTimes("P3 P1 P4") + " " + twoTimes("P3")));
		assertOpponentsOf(p3, haveBeen(twoTimes("P2 P4 P1") + " " + twoTimes("P2")));
		assertOpponentsOf(p4, haveBeen(twoTimes("P1 P3 P2") + " " + twoTimes("--")));
	}

	private String twoTimes(String string) {
		return string + " " + string;
	}

	@Example
	void tournamentWithOddPlayerCount() {
		PlayerMock p1 = mock("P1");
		PlayerMock p2 = mock("P2");
		PlayerMock p3 = mock("P3");

		List<GameState> states = playSeasonOf(a(tournament().withPlayers(p1, p2, p3)));

		assertOpponentsOf(p1, haveBeen(twoTimes("-- P2 P3")));
		assertOpponentsOf(p2, haveBeen(twoTimes("P3 P1 --")));
		assertOpponentsOf(p3, haveBeen(twoTimes("P2 -- P1")));

		int i = 0;
		assertThat(states.size(), is(3 * 2 * 2));
		assertThat(next(states, i += 2), is(asList(coffeeBreakWin("P1"), lose("P2"))));
		assertThat(next(states, i += 2), is(asList(lose("P1"), coffeeBreakWin("P3"))));
		assertThat(next(states, i += 2), is(asList(lose("P1"), coffeeBreakWin("P2"))));

		assertThat(next(states, i += 2), is(asList(coffeeBreakWin("P1"), lose("P3"))));
		assertThat(next(states, i += 2), is(asList(lose("P2"), coffeeBreakWin("P3"))));
		assertThat(next(states, i += 2), is(asList(lose("P3"), coffeeBreakWin("P2"))));
	}

	private List<GameState> next(List<GameState> states, int index) {
		return states.subList(index - 2, index);
	}

	GameState coffeeBreakWin(String token) {
		return GameState.builder().score(WIN).token(token).reason(CoffeebreakGame.COFFEE_BREAK_WIN_MESSAGE).build();
	}

	GameState lose(String token) {
		return GameState.builder().score(LOSE).token(token).reason(DummyBoard.LOSE_MESSAGE).build();
	}

	@Example
	void playersCanUseTheCoffeeBreakToken() {
		PlayerMock p1 = mock(Tournament.coffeeBreakPlayer.getToken());
		Tournament tournament = a(tournament().withPlayers(p1));
		playSeasonOf(tournament);
		assertOpponentsOf(p1, haveBeen("-- --"));
	}

	@Example
	@Disabled
	private void whatIsTheResultIfBothPlayersDontWantToPlay_SecondWins_or_Draw() {
		fail("implement");
	}

	@Property
	void jqwikCheckTest(@ForAll("players") List<PlayerMock> players, @ForAll("numberOfSeasons") int numberOfSeasons) {
		Tournament tournament = a(tournament().withPlayers(players.toArray(new Player[0])));
		for (int i = 0; i < numberOfSeasons; i++) {
			playSeasonOf(tournament);
		}

		Matcher<Integer> matcher = is(0);
		boolean negate = numberOfSeasons == 0 || players.size() < 2;
		players.forEach(p -> assertThat(p.getMovesMade(), negate ? matcher : not(matcher)));
	}

	@Provide
	IntegerArbitrary numberOfSeasons() {
		return Arbitraries.integers().between(0, 100);
	}

	@Provide
	Arbitrary<List<PlayerMock>> players() {
		return Arbitraries.integers().between(2, 5).flatMap( //
				stringSize -> Arbitraries.strings() //
						.alpha() //
						.ofMinLength(stringSize) //
						.ofMaxLength(stringSize) //
						.unique() //
						.map(PlayerMock::new) //
						.list().ofMinSize(0).ofMaxSize(20));
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

	Predicate<? super String> isCoffeeBreak() {
		return "--"::equals;
	}

	List<GameState> playSeasonOf(Tournament tournament) {
		return tournament.playSeason().collect(toList());
	}

	PlayerMock mock(String token) {
		return new PlayerMock(token);
	}

}
