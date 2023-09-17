package org.ase.fourwins.tournament.listener.database;

import static java.util.EnumSet.allOf;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toList;
import static org.ase.fourwins.board.Board.Score.DRAW;
import static org.ase.fourwins.board.Board.Score.IN_GAME;
import static org.ase.fourwins.board.Board.Score.LOSE;
import static org.ase.fourwins.board.Board.Score.WIN;
import static org.ase.fourwins.tournament.listener.database.Games.aGameOf;
import static org.ase.fourwins.tournament.listener.database.Games.players;
import static org.testcontainers.containers.BindMode.READ_ONLY;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Random;

import org.ase.fourwins.board.Board.Score;
import org.ase.fourwins.game.Game;
import org.ase.fourwins.game.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class MySqlListenerIT {

	private static final String DATABASE_NAME = "4WINS";

	@Container
	MySQLContainer<?> mySql = new MySQLContainer<>(MySQLContainer.NAME) //
			.withDatabaseName(DATABASE_NAME) //
			.withFileSystemBind("../docker/mysql", "/docker-entrypoint-initdb.d", READ_ONLY) //
	;

	private MySqlDbListener sut;

	@BeforeEach
	void setup() throws Exception {
		this.sut = new MySqlDbListener(mySql.getJdbcUrl(), "fourwins_write", "fourwinswrite");
	}

	private ScoresDatabase scoresDb() throws SQLException {
		return new ScoresDatabase(connection());
	}

	private Connection connection() throws SQLException {
		System.out.println("MySql database url " + mySql.getJdbcUrl());
		return DriverManager.getConnection(mySql.getJdbcUrl(), "fourwins_read", "fourwinsread");
	}

	@Test
	void testOneGameEndingIsInsertedToDatabase() throws Exception {
		try (ScoresDatabase scoresDb = scoresDb()) {
			List<Player> givenPlayers = players(2);
			gameEnded(aGameOf(givenPlayers, WIN, 0));
			scoresDb.assertThatScoresAre(givenPlayers, 1.0, 0.0);
		}
	}

	@Test
	void testPlayerMakesIllegalMoveAndOpponentGetsAFullPoint() throws Exception {
		try (ScoresDatabase scoresDb = scoresDb()) {
			List<Player> players = players(2);
			gameEnded(aGameOf(players, LOSE, 0));
			scoresDb.assertThatScoresAre(players, 0.0, 1.0);
		}
	}

	@Test
	void testBothPlayersGetAHalfPointForADraw() throws Exception {
		try (ScoresDatabase scoresDb = scoresDb()) {
			List<Player> players = players(2);
			gameEnded(aGameOf(players, DRAW, 0));
			scoresDb.assertThatScoresAre(players, 0.5, 0.5);
		}
	}

	@Test
	void canAccumulateValues() throws Exception {
		try (ScoresDatabase scoresDb = scoresDb()) {
			List<Player> players = players(2);
			gameEnded(aGameOf(players, WIN, 1));
			gameEnded(aGameOf(players, WIN, 1));
			gameEnded(aGameOf(players, DRAW, 1));
			scoresDb.assertThatScoresAre(players, 0.5, 2.5);
		}
	}

	@Test
	@Disabled
	void longRunningIntegrationTest() throws InterruptedException {
		long startTime = System.currentTimeMillis();
		Random random = new Random(startTime);
		List<Score> scores = validGameEndScores();
		do {
			List<Player> players = players(6 + random.nextInt(6));
			Score score = scores.get(random.nextInt(scores.size()));
			int lastPlayer = random.nextInt(players.size());
			sut.gameEnded(aGameOf(players, score, lastPlayer));
			MILLISECONDS.sleep(500);
		} while (System.currentTimeMillis() < startTime + MINUTES.toMillis(30));
	}

	private List<Score> validGameEndScores() {
		return allOf(Score.class).stream().filter(not(IN_GAME::equals)).collect(toList());
	}

	private void gameEnded(Game game) {
		sut.gameEnded(game);
	}

}
