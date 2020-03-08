package org.ase.fourwins.udp.server;

import org.ase.fourwins.game.Game;
import org.ase.fourwins.tournament.listener.TournamentListener;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class IgnoreExceptionDelegateTournamentListener implements TournamentListener {

	private final TournamentListener delegate;

	public void gameStarted(Game game) {
		try {
			delegate.gameStarted(game);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void gameEnded(Game game) {
		try {
			delegate.gameEnded(game);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void seasonStarted() {
		try {
			delegate.seasonStarted();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void seasonEnded() {
		try {
			delegate.seasonEnded();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void newTokenAt(Game game, String token, int column) {
		try {
			delegate.newTokenAt(game, token, column);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}