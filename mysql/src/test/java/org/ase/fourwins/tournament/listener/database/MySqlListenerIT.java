package org.ase.fourwins.tournament.listener.database;

import static java.util.Comparator.comparing;
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
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;

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
class MysqlListenerIT {

	private static final String DATABASE_NAME = "4WINS";

	@Container
	private MySQLContainer<?> mysql = new MySQLContainer<>(MySQLContainer.NAME).withDatabaseName(DATABASE_NAME)
			.withInitScript("../docker/mysql/sql.sql");

	private MysqlDBListener sut;

	@BeforeEach
	public void setup() throws Exception {
		this.sut = new MysqlDBListener(mysql.getJdbcUrl(), "fourwins_write", "fourwinswrite");
	}

	private ScoresDatabase scoreDb() throws SQLException {
		return new ScoresDatabase(connection());
	}

	private Connection connection() throws SQLException {
		System.out.println("MySQL database url " + mysql.getJdbcUrl());
		return DriverManager.getConnection(mysql.getJdbcUrl(), "fourwins_read", "fourwinsread");
	}

	@Test
	void testOneGameEndingIsInsertedToDatabase() throws Exception {
		try (ScoresDatabase scoresDb = scoreDb()) {
			List<Player> givenPlayers = players(2);
			whenEnded(aGameOf(givenPlayers, WIN, 0));
			scoresAre(scoresDb, givenPlayers, 1.0, 0.0);
		}
	}

	@Test
	void testPlayerMakesIllegalMoveAndOpponentGetsAFullPoint() throws Exception {
		try (ScoresDatabase scoresDb = scoreDb()) {
			List<Player> players = players(2);
			whenEnded(aGameOf(players, LOSE, 0));
			scoresAre(scoresDb, players, 0.0, 1.0);
		}
	}

	@Test
	void testBothPlayersGetAHalfPointForADraw() throws Exception {
		try (ScoresDatabase scoresDb = scoreDb()) {
			List<Player> players = players(2);
			whenEnded(aGameOf(players, DRAW, 0));
			scoresAre(scoresDb, players, 0.5, 0.5);
		}
	}

	@Test
	void canAccumulateValues() throws Exception {
		try (ScoresDatabase scoreDb = scoreDb()) {
			List<Player> players = players(2);
			whenEnded(aGameOf(players, WIN, 1));
			whenEnded(aGameOf(players, WIN, 1));
			whenEnded(aGameOf(players, DRAW, 1));
			scoresAre(scoreDb, players, 0.5, 2.5);
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

	private void whenEnded(Game game) {
		sut.gameEnded(game);
	}

	private static void scoresAre(ScoresDatabase scoreDb, List<Player> players, double... scores) throws SQLException {
		assertThat("Players size must match scores length", players.size(), is(scores.length));
		for (int i = 0; i < players.size(); i++) {
			assertThat(scoreOf(scoreDb, players.get(i)), is(scores[i]));
		}
	}

	private static double scoreOf(ScoresDatabase scoreDb, Player player) throws SQLException {
		return rows(scoreDb, player).stream().max(comparing(MysqlDBRow::getValue)).map(MysqlDBRow::getValue)
				.orElse(0.0);
	}

	private static List<MysqlDBRow> rows(ScoresDatabase scoreDb, Player player) throws SQLException {
		return scoreDb.scores(player);
	}

}
