package org.ase.fourwins.udp.server.listeners;

import org.ase.fourwins.tournament.listener.TournamentListener;

import lombok.Getter;

public class TournamentListenerEnabled implements TournamentListener {
	
	@Getter
	private static boolean constructorCalled;

	public TournamentListenerEnabled() {
		constructorCalled = true;
	}

}
