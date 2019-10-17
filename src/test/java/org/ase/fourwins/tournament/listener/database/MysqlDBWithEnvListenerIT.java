package org.ase.fourwins.tournament.listener.database;

import static com.github.stefanbirkner.systemlambda.SystemLambda.withEnvironmentVariable;
import static org.ase.fourwins.board.Board.Score.DRAW;
import static org.ase.fourwins.tournament.listener.database.Games.aGameOf;
import static org.ase.fourwins.tournament.listener.database.Games.players;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

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

class MysqlDBWithEnvListenerIT {

	private static final String DATABASE_NAME = "foobar";
	private static final String DATABASE_USER = "foo";
	private static final String DATABASE_PASSWORD = "bar";

	private MySQLContainer<?> mysql = new MySQLContainer<>().withDatabaseName(DATABASE_NAME).withUsername(DATABASE_USER)
			.withPassword(DATABASE_PASSWORD);

	private ScoresDatabase scores;

	@BeforeEach
	public void setup() throws Exception {
		mysql.start();
		System.out.println("MySQL database url " + mysql.getJdbcUrl());
		Connection connection = DriverManager.getConnection(mysql.getJdbcUrl(), mysql.getUsername(),
				mysql.getPassword());
		this.scores = new ScoresDatabase(connection);
		this.scores.init(database(mysql.getJdbcUrl()));
	}

	private String database(String uri) {
		return uri.substring(uri.lastIndexOf('/') + 1);
	}

	@AfterEach
	public void tearDown() throws IOException {
		this.scores.close();
		mysql.stop();
	}

	@Test
	void canReadEnvVars() throws Exception {
		withEnvironmentVariable("DATABASE_URL", mysql.getJdbcUrl()).and("DATABASE_USER", DATABASE_USER)
				.and("DATABASE_PASSWORD", DATABASE_PASSWORD).execute(() -> {
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
