package org.ase.fourwins.database;

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.stream.Collectors.toList;
import static org.ase.fourwins.board.Board.Score.DRAW;
import static org.ase.fourwins.board.Board.Score.IN_GAME;
import static org.ase.fourwins.board.Board.Score.LOSE;
import static org.ase.fourwins.board.Board.Score.WIN;
import static org.ase.fourwins.tournament.listener.database.MysqlDBRow.COLUMNNAME_PLAYER_ID;
import static org.ase.fourwins.tournament.listener.database.MysqlDBRow.TABLE_NAME;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.ase.fourwins.board.Board.GameState;
import org.ase.fourwins.board.Board.Score;
import org.ase.fourwins.board.mockplayers.PlayerMock;
import org.ase.fourwins.game.Game;
import org.ase.fourwins.game.Player;
import org.ase.fourwins.tournament.listener.database.MysqlDBListener;
import org.ase.fourwins.tournament.listener.database.MysqlDBRow;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;

class MysqlListenerIT {

	private MySQLContainer<?> mysql = new MySQLContainer<>().withDatabaseName(MysqlDBListener.DEFAULT_DATABASE_NAME)
			.withUsername("foo").withPassword("bar");

	private MysqlDBListener sut;

	private Connection connection;

	@BeforeEach
	public void setup() throws ClassNotFoundException, SQLException {
		mysql.start();
		System.out.println("MySQL database url " + mysql.getJdbcUrl());
		this.sut = new MysqlDBListener(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
		this.connection = DriverManager.getConnection(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
	}

	@AfterEach
	public void tearDown() {
		DbUtils.closeQuietly(connection);
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
		List<Score> scores = new ArrayList<Score>(asList(Score.values()));
		scores.remove(IN_GAME);
		return scores;
	}

	private void whenEnded(Game buildGame) {
		sut.gameEnded(buildGame);
	}

	private List<Player> players(int count) {
		return IntStream.range(1, 1 + count).mapToObj(i -> new PlayerMock("P" + i)).collect(toList());
	}

	private double scoreOf(Player player) throws SQLException {
		return rows(player).stream().mapToDouble(MysqlDBRow::getValue).sum();
	}

	private List<MysqlDBRow> rows(Player player) throws SQLException {
		return scoreQuery(player);
	}

	private List<MysqlDBRow> scoreQuery(Player player) throws SQLException {
		QueryRunner runner = new QueryRunner();
		BeanListHandler<MysqlDBRow> handler = new BeanListHandler<>(MysqlDBRow.class);
		String query = "SELECT * FROM " + TABLE_NAME + " WHERE " + COLUMNNAME_PLAYER_ID + " = ?";
		return runner.query(connection, query, handler, player.getToken());
	}

	private Game aGameOf(List<Player> players, Score score, int lastPlayer) {
		return new Game() {

			@Override
			public Game runGame() {
				throw new UnsupportedOperationException();
			}

			@Override
			public GameState gameState() {
				return GameState.builder().token(players.get(lastPlayer).getToken()).score(score).build();
			}

			@Override
			public List<Player> getPlayers() {
				return players;
			}
		};
	}

}
