package org.ase.fourwins.tournament.listener.database;

import java.sql.SQLException;

import org.ase.fourwins.annos.OnlyActivateWhenEnvSet;
import org.ase.fourwins.tournament.listener.TournamentListener;

import lombok.experimental.Delegate;

@OnlyActivateWhenEnvSet("WITH_DATABASE")
//@ActivateIfSet({ MysqlDBWithEnvListener.DATABASE_URL, MysqlDBWithEnvListener.DATABASE_USER,
//	MysqlDBWithEnvListener.DATABASE_PASSWORD })
public class MySqlDbWithEnvListener implements TournamentListener {

	protected static final String DATABASE_URL = "DATABASE_URL";
	protected static final String DATABASE_USER = "DATABASE_USER";
	protected static final String DATABASE_PASSWORD = "DATABASE_PASSWORD";

	@Delegate
	private final MySqlDbListener delegate;

	public MySqlDbWithEnvListener() throws ClassNotFoundException, SQLException {
		delegate = new MySqlDbListener(env(DATABASE_URL), env(DATABASE_USER), env(DATABASE_PASSWORD));
	}

	private static String env(String name) {
		return System.getenv(name);
	}

}
