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
import static org.junit.Assert.assertThat;
import static org.testcontainers.containers.BindMode.READ_ONLY;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Random;

import org.ase.fourwins.board.Board.Score;
import org.ase.fourwins.game.Game;
import org.ase.fourwins.game.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;

class MysqlListenerIT {

	private static final String DATABASE_NAME = "4WINS";

	private MySQLContainer<?> mysql = new MySQLContainer<>().withDatabaseName(DATABASE_NAME)//
			.withFileSystemBind("../docker/mysql", "/docker-entrypoint-initdb.d", READ_ONLY) //
	;

	private MysqlDBListener sut;

	private ScoresDatabase scores;

	@BeforeEach
	public void setup() throws Exception {
		mysql.start();
		System.out.println("MySQL database url " + mysql.getJdbcUrl());
		Connection connection = DriverManager.getConnection(mysql.getJdbcUrl(), "fourwins_read", "fourwinsread");
		this.scores = new ScoresDatabase(connection);
		this.sut = new MysqlDBListener(mysql.getJdbcUrl(), "fourwins_write", "fourwinswrite");
	}

	@AfterEach
	public void tearDown() throws IOException {
		this.scores.close();
		mysql.stop();
	}

	@Test
	void testOneGameEndingIsInsertedToDatabase() throws SQLException {
		List<Player> givenPlayers = players(2);
		whenEnded(aGameOf(givenPlayers, WIN, 0));
		scoresAre(givenPlayers, 1.0, 0.0);
	}

	@Test
	void testPlayerMakesIllegalMoveAndOpponentGetsAFullPoint() throws SQLException {
		List<Player> players = players(2);
		whenEnded(aGameOf(players, LOSE, 0));
		scoresAre(players, 0.0, 1.0);
	}

	@Test
	void testBothPlayersGetAHalfPointForADraw() throws SQLException {
		List<Player> players = players(2);
		whenEnded(aGameOf(players, DRAW, 0));
		scoresAre(players, 0.5, 0.5);
	}

	@Test
	void canAccumulateValues() throws SQLException {
		List<Player> players = players(2);
		whenEnded(aGameOf(players, WIN, 1));
		whenEnded(aGameOf(players, WIN, 1));
		whenEnded(aGameOf(players, DRAW, 1));
		scoresAre(players, 0.5, 2.5);
	}

	private void scoresAre(List<Player> players, double... scores) throws SQLException {
		assertThat("Players size must match scores length", players.size(), is(scores.length));
		for (int i = 0; i < players.size(); i++) {
			assertThat(scoreOf(players.get(i)), is(scores[i]));
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

	private double scoreOf(Player player) throws SQLException {
		return rows(player).stream().max(comparing(MysqlDBRow::getValue)).map(MysqlDBRow::getValue).orElse(0.0);
	}

	private List<MysqlDBRow> rows(Player player) throws SQLException {
		return this.scores.scores(player);
	}

}
