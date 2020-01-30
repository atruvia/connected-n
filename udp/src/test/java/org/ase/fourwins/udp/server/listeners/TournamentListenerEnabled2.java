package org.ase.fourwins.udp.server.listeners;

import org.ase.fourwins.annos.OnlyActivateWhenEnvSet;
import org.ase.fourwins.tournament.listener.TournamentListener;

import lombok.Getter;

@OnlyActivateWhenEnvSet(TournamentListenerEnabled2.ENV_NAME_TO_BE_SET)
public class TournamentListenerEnabled2 implements TournamentListener {

	public static final String ENV_NAME_TO_BE_SET = "envNameThatIsSet";

	@Getter
	private static boolean constructorCalled;

	public TournamentListenerEnabled2() {
		constructorCalled = true;
	}

}
