package org.ase.fourwins.tournament;

import static org.ase.fourwins.tournament.TournamentTest.TournamentBuilder.tournament;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.ase.fourwins.board.Board.GameState;
import org.ase.fourwins.board.mockplayers.PlayerMock;
import org.ase.fourwins.game.Game;
import org.ase.fourwins.game.Game.GameId;
import org.ase.fourwins.tournament.listener.TournamentListener;
import org.junit.jupiter.api.Test;

class TournamentListenerTest {

	private final class TournamentListenerForTest implements TournamentListener {
		private final Map<GameId, Integer> moveCounter = new HashMap<>();

		@Override
		public void newTokenAt(Game game, String token, int column) {
			GameId key = key(game);
			moveCounter.put(key, getCount(key) + 1);
		}

		private GameId key(Game game) {
			return game.getId();
		}

		public Set<GameId> getGames() {
			return moveCounter.keySet();
		}

		public int getCount(GameId key) {
			return moveCounter.getOrDefault(key, 0);
		}

	}

	@Test
	void testGameEndedListenerMethodIsCalled12TimesForA3PlayerTournament() {
		PlayerMock p1 = mockPlayer("P1", 0);
		PlayerMock p2 = mockPlayer("P2", 1);
		PlayerMock p3 = mockPlayer("P3", 2);

		TournamentListener listener = mock(TournamentListener.class);
		tournament().withPlayers(p1, p2, p3).registerListener(listener).playSeason();

		verify(listener, times(12)).gameStarted(any());
		verify(listener, times(12)).gameEnded(any());
		verify(listener, times(1)).seasonEnded();
	}

	@Test
	void tournamentListenerGetsInformedAboutMoves() {
		List<PlayerMock> players = List.of(mockPlayer("P1", 0), mockPlayer("P2", 1), mockPlayer("P3", 2));
		TournamentListenerForTest tournamentListener = new TournamentListenerForTest();
		DefaultTournament sut = new DefaultTournament();
		sut.addTournamentListener(tournamentListener);

		int seasons = 3;
		for (int i = 0; i < seasons; i++) {
			sut.playSeason(players, noop());
		}

		Set<GameId> games = tournamentListener.getGames();
		assertThat(games.size(), is(6 * seasons));
		for (GameId gameId : games) {
			assertThat(tournamentListener.getCount(gameId), is(sut.getBoardInfo().getRows() + 1));
		}
	}

	private Consumer<GameState> noop() {
		return c -> {
		};
	}

	PlayerMock mockPlayer(String token, int columnIdx) {
		return new PlayerMock(token) {
			@Override
			protected int nextColumn() {
				super.nextColumn();
				return columnIdx;
			}
		};
	}

}
