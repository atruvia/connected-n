package org.ase.fourwins.tournament;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;

import org.ase.fourwins.board.Board.GameState;
import org.ase.fourwins.board.mockplayers.PlayerMock;
import org.ase.fourwins.tournament.TournamentTest.TournamentBuilder;
import org.ase.fourwins.tournament.listener.TournamentListener;
import org.junit.jupiter.api.Test;

class TournamentListenerTest {

	@Test
	void testGameEndedListenerMethodIsCalled12TimesForA3PlayerTournament() {
		PlayerMock p1 = mockPlayer("P1");
		PlayerMock p2 = mockPlayer("P2");
		PlayerMock p3 = mockPlayer("P3");

		TournamentListener listener = mock(TournamentListener.class);
		Tournament tournament = TournamentBuilder.tournament().withPlayers(p1, p2, p3).registerListener(listener)
				.build();
		playSeasonOf(tournament);

		verify(listener, times(12)).gameStarted(any());
		verify(listener, times(12)).gameEnded(any());
		verify(listener, times(1)).seasonEnded();
	}

	List<GameState> playSeasonOf(Tournament tournament) {
		List<GameState> states = new ArrayList<>();
		tournament.playSeason(states::add);
		return states;
	}

	PlayerMock mockPlayer(String token) {
		return new PlayerMock(token);
	}

}
