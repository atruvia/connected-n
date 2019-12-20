package org.ase.fourwins.tournament;

import static java.util.Arrays.asList;
import static org.ase.fourwins.tournament.TournamentTest.TournamentBuilder.tournament;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.function.Consumer;

import org.ase.fourwins.board.Board.GameState;
import org.ase.fourwins.board.mockplayers.PlayerMock;
import org.ase.fourwins.tournament.listener.TournamentListener;
import org.junit.jupiter.api.Test;

class TournamentListenerTest {

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
		List<PlayerMock> players = asList(mockPlayer("P1", 0), mockPlayer("P2", 1), mockPlayer("P3", 2));
		TournamentListener tournamentListener = mock(TournamentListener.class);
		DefaultTournament sut = new DefaultTournament();
		sut.addTournamentListener(tournamentListener);

		int seasons = 3;
		for (int i = 0; i < seasons; i++) {
			sut.playSeason(players, noop());
		}
		for (PlayerMock player : players) {
			verify(tournamentListener, times(seasons * 14)).newTokenAt(eq(player.getToken()), anyInt(), anyInt());
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
