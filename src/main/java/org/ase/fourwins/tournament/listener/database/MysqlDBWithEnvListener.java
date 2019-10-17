package org.ase.fourwins.tournament.listener.database;

import java.sql.SQLException;

import org.ase.fourwins.game.Game;
import org.ase.fourwins.tournament.listener.TournamentListener;

public class MysqlDBWithEnvListener implements TournamentListener {

	private final MysqlDBListener delegate;

	public MysqlDBWithEnvListener() throws ClassNotFoundException, SQLException {
		delegate = new MysqlDBListener(env("DATABASE_URL"), env("DATABASE_USER"), env("DATABASE_PASSWORD"));
	}

	protected MysqlDBListener getDelegate() {
		return delegate;
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
