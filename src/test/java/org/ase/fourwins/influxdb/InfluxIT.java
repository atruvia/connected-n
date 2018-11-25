package org.ase.fourwins.influxdb;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toList;
import static org.ase.fourwins.board.Board.Score.DRAW;
import static org.ase.fourwins.board.Board.Score.IN_GAME;
import static org.ase.fourwins.board.Board.Score.LOSE;
import static org.ase.fourwins.board.Board.Score.WIN;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.ase.fourwins.board.Board.GameState;
import org.ase.fourwins.board.Board.Score;
import org.ase.fourwins.board.mockplayers.PlayerMock;
import org.ase.fourwins.game.Game;
import org.ase.fourwins.game.Player;
import org.ase.fourwins.tournament.listener.InfluxDBListener;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

class InfluxIT {

	private static final int INFLUX_PORT = 8086;

	static GenericContainer influx = new GenericContainer("influxdb").withExposedPorts(INFLUX_PORT);

	private static final String RETENTION_POLICY = "default";
	private static final String DBNAME = "GAMES";
	private InfluxDB influxDB;
	private InfluxDBListener listener;

	@BeforeEach
	public void setup() throws InterruptedException {
		influx.start();
		String url = "http://" + influx.getContainerIpAddress() + ":" + influx.getMappedPort(INFLUX_PORT);
		System.out.println(url);
		influxDB = InfluxDBFactory.connect(
				url, "root", "root");
		listener = new InfluxDBListener(influxDB, DBNAME);
		influxDB.query(new Query("CREATE DATABASE " + DBNAME, DBNAME));
		influxDB.setDatabase(DBNAME);
	}

	@AfterEach
	public void tearDown() {
		influxDB.query(new Query("DROP DATABASE \"" + DBNAME + "\"", DBNAME));
		influx.stop();
	}

	@Test
	void testInfluxDBIsPingable() {
		assertThat(influxDB.ping().isGood(), is(true));
	}

	@Test
	void testOneGameEndingIsInsertedToInfluxDB() {
		List<Player> givenPlayers = players(1);
		whenEnded(aGameOf(givenPlayers, WIN, givenPlayers.get(0)));
		assertThat(scoreOf(givenPlayers.get(0)), is(1.0));
	}

	@Test
	void testPlayerMakesIllegalMoveAndOpponentGetsAFullPoint() {
		List<Player> players = players(2);
		whenEnded(aGameOf(players, LOSE, players.get(0)));
		assertThat(scoreOf(players.get(1)), is(1.0));
	}

	@Test
	void testBothPlayersGetAHalfPointForADraw() {
		List<Player> players = players(2);
		whenEnded(aGameOf(players, DRAW, players.get(0)));
		assertThat(scoreOf(players.get(0)), is(0.5));
		assertThat(scoreOf(players.get(1)), is(0.5));
	}

	@Test
	void longRunningIntegrationTest() throws InterruptedException {
		long startTime = System.currentTimeMillis();
		Random random = new Random(startTime);
		List<Score> scores = validGameEndScores();
		do {
			List<Player> players = players(6 + random.nextInt(6));
			Score score = scores.get(random.nextInt(scores.size()));
			Player lastToken = players.get(random.nextInt(players.size()));
			listener.gameEnded(aGameOf(players, score, lastToken));
			MILLISECONDS.sleep(500);
		} while (System.currentTimeMillis() < startTime + TimeUnit.MINUTES.toMillis(30));
	}

	private List<Score> validGameEndScores() {
		List<Score> scores = new ArrayList<Score>(Arrays.asList(Score.values()));
		scores.remove(IN_GAME);
		return scores;
	}

	private void whenEnded(Game buildGame) {
		listener.gameEnded(buildGame);
	}

	private List<Player> players(int count) {
		return IntStream.range(1, 1 + count).mapToObj(i -> new PlayerMock("P" + i)).collect(toList());
	}

	private Object scoreOf(Player player) {
		QueryResult query = influxDB.query(new Query(
				"SELECT value FROM " + DBNAME + " WHERE \"player_id\" = '" + player.getToken() + "'", DBNAME));
		return query.getResults().get(0).getSeries().get(0).getValues().get(0).get(1);
	}

	private Game aGameOf(List<Player> players, Score score, Player lastPlayer) {
		return new Game() {

			@Override
			public Game runGame() {
				throw new UnsupportedOperationException();
			}

			@Override
			public GameState gameState() {
				return GameState.builder().token(lastPlayer.getToken()).score(score).build();
			}

			@Override
			public List<Player> getPlayers() {
				return players;
			}
		};
	}

}
