package org.ase.fourwins.tournament.listener.database;

import static com.github.stefanbirkner.systemlambda.SystemLambda.withEnvironmentVariable;
import static org.ase.fourwins.board.Board.Score.DRAW;
import static org.ase.fourwins.tournament.listener.database.Games.aGameOf;
import static org.ase.fourwins.tournament.listener.database.Games.players;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.testcontainers.containers.BindMode.READ_ONLY;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

import org.ase.fourwins.game.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

	private ScoresDatabase scores;

	@BeforeEach
	public void setup() throws Exception {
		System.out.println("MySQL database url " + mysql.getJdbcUrl());
		Connection connection = DriverManager.getConnection(mysql.getJdbcUrl(), mysql.getUsername(),
				mysql.getPassword());
		this.scores = new ScoresDatabase(connection);
	}

	@AfterEach
	public void tearDown() throws IOException {
		this.scores.close();
	}

	@Test
	void canReadEnvVars() throws Exception {
		withEnvironmentVariable("DATABASE_URL", mysql.getJdbcUrl()).and("DATABASE_USER", "fourwins_write")
				.and("DATABASE_PASSWORD", "fourwinswrite").execute(() -> {
					MysqlDBWithEnvListener sut = new MysqlDBWithEnvListener();
					List<Player> players = players(2);
					sut.gameEnded(aGameOf(players, DRAW, 1));
					assertThat(rows(players.get(0)).size(), is(1));
					assertThat(rows(players.get(1)).size(), is(1));
				});
	}

	private List<MysqlDBRow> rows(Player player) throws SQLException {
		return this.scores.scores(player);
	}
}
