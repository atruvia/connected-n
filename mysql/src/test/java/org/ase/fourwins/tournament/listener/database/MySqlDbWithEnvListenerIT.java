package org.ase.fourwins.tournament.listener.database;

import static com.github.stefanbirkner.systemlambda.SystemLambda.withEnvironmentVariable;
import static org.ase.fourwins.board.Board.Score.DRAW;
import static org.ase.fourwins.tournament.listener.database.Games.aGameOf;
import static org.ase.fourwins.tournament.listener.database.Games.players;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
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
class MysqlDBWithEnvListenerIT {

	private static final String DATABASE_NAME = "4WINS";

	@Container
	private MySQLContainer<?> mysql = new MySQLContainer<>().withDatabaseName(DATABASE_NAME) //
			.withFileSystemBind("../docker/mysql", "/docker-entrypoint-initdb.d", READ_ONLY) //
	;

	private ScoresDatabase scoreDb() throws SQLException {
		return new ScoresDatabase(connection());
	}

	private Connection connection() throws SQLException {
		System.out.println("MySQL database url " + mysql.getJdbcUrl());
		return DriverManager.getConnection(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
	}

	@Test
	void canReadEnvVars() throws Exception {
		withEnvironmentVariable("DATABASE_URL", mysql.getJdbcUrl()).and("DATABASE_USER", "fourwins_write")
				.and("DATABASE_PASSWORD", "fourwinswrite").execute(() -> {
					try (ScoresDatabase scoreDb = scoreDb()) {
						List<Player> players = players(2);
						gameEnded(aGameOf(players, DRAW, 1));
						assertThat(scoreOf(scoreDb, players.get(0)).size(), is(1));
						assertThat(scoreOf(scoreDb, players.get(1)).size(), is(1));
					}
				});
	}

	private void gameEnded(Game game) throws ClassNotFoundException, SQLException {
		sut().gameEnded(game);
	}

	private MysqlDBWithEnvListener sut() throws ClassNotFoundException, SQLException {
		return new MysqlDBWithEnvListener();
	}

	private List<MysqlDBRow> scoreOf(ScoresDatabase scoreDb, Player player) throws SQLException {
		return scoreDb.scores(player);
	}

}
