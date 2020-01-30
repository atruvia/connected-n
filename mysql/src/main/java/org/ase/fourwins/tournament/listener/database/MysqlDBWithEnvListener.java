package org.ase.fourwins.tournament.listener.database;

import java.sql.SQLException;

import org.ase.fourwins.annos.OnlyActivateWhenEnvSet;
import org.ase.fourwins.game.Game;
import org.ase.fourwins.tournament.listener.TournamentListener;

@OnlyActivateWhenEnvSet("WITH_DATABASE")
//@ActivateIfSet({ MysqlDBWithEnvListener.DATABASE_URL, MysqlDBWithEnvListener.DATABASE_USER,
//	MysqlDBWithEnvListener.DATABASE_PASSWORD })
public class MysqlDBWithEnvListener implements TournamentListener {

	protected static final String DATABASE_URL = "DATABASE_URL";
	protected static final String DATABASE_USER = "DATABASE_USER";
	protected static final String DATABASE_PASSWORD = "DATABASE_PASSWORD";

	private final MysqlDBListener delegate;

	public MysqlDBWithEnvListener() throws ClassNotFoundException, SQLException {
		delegate = new MysqlDBListener(env(DATABASE_URL), env(DATABASE_USER), env(DATABASE_PASSWORD));
	}

	private static String env(String name) {
		return System.getenv(name);
	}

	@Override
	public void gameEnded(Game game) {
		delegate.gameEnded(game);
	}

	@Override
	public void gameStarted(Game game) {
		delegate.gameStarted(game);
	}

	@Override
	public void seasonEnded() {
		delegate.seasonEnded();
	}

}
