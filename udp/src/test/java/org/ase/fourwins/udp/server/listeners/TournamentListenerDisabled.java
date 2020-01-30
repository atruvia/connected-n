package org.ase.fourwins.udp.server.listeners;

import org.ase.fourwins.annos.OnlyActivateWhenEnvSet;
import org.ase.fourwins.tournament.listener.TournamentListener;

import lombok.Getter;

@OnlyActivateWhenEnvSet("envNameThatIsNotSet")
public class TournamentListenerDisabled implements TournamentListener {

	@Getter
	private static boolean constructorCalled;

	public TournamentListenerDisabled() {
		constructorCalled = true;
	}

}
