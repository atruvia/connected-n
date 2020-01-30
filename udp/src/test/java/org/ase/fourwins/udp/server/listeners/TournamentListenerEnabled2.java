package org.ase.fourwins.udp.server.listeners;

import org.ase.fourwins.annos.OnlyActivateWhenEnvSet;
import org.ase.fourwins.tournament.listener.TournamentListener;

import lombok.Getter;

@OnlyActivateWhenEnvSet("envNameThatIsSet")
public class TournamentListenerEnabled2 implements TournamentListener {

	@Getter
	private static boolean constructorCalled;

	public TournamentListenerEnabled2() {
		constructorCalled = true;
	}

}
