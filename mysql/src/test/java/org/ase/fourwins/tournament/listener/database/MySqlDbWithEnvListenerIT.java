package org.ase.fourwins.tournament.listener.database;

import static com.github.stefanbirkner.systemlambda.SystemLambda.withEnvironmentVariable;
import static org.ase.fourwins.board.Board.Score.DRAW;
import static org.ase.fourwins.tournament.listener.database.Games.aGameOf;
import static org.ase.fourwins.tournament.listener.database.Games.players;
import static org.testcontainers.containers.BindMode.READ_ONLY;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

import org.ase.fourwins.game.Game;
import org.ase.fourwins.game.Player;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class MySqlDbWithEnvListenerIT {

	private static final String DATABASE_NAME = "4WINS";

	@Container
	MySQLContainer<?> mySql = new MySQLContainer<>(MySQLContainer.NAME) //
			.withDatabaseName(DATABASE_NAME) //
			.withFileSystemBind("../docker/mysql", "/docker-entrypoint-initdb.d", READ_ONLY) //
	;

	private ScoresDatabase scoresDb() throws SQLException {
		return new ScoresDatabase(connection());
	}

	private Connection connection() throws SQLException {
		System.out.println("MySql database url " + mySql.getJdbcUrl());
		return DriverManager.getConnection(mySql.getJdbcUrl(), mySql.getUsername(), mySql.getPassword());
	}

	@Test
	void canReadEnvVars() throws Exception {
		withEnvironmentVariable("DATABASE_URL", mySql.getJdbcUrl()).and("DATABASE_USER", "fourwins_write")
				.and("DATABASE_PASSWORD", "fourwinswrite").execute(() -> {
					try (ScoresDatabase scoresDb = scoresDb()) {
						List<Player> players = players(2);
						gameEnded(aGameOf(players, DRAW, 1));
						scoresDb.assertThatScoresAre(players, 0.5, 0.5);
					}
				});
	}

	private void gameEnded(Game game) throws ClassNotFoundException, SQLException {
		sut().gameEnded(game);
	}

	private MySqlDbWithEnvListener sut() throws ClassNotFoundException, SQLException {
		return new MySqlDbWithEnvListener();
	}

}
