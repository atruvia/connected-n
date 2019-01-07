package org.ase.fourwins.influxdb;

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.stream.Collectors.toList;
import static org.ase.fourwins.board.Board.Score.DRAW;
import static org.ase.fourwins.board.Board.Score.IN_GAME;
import static org.ase.fourwins.board.Board.Score.LOSE;
import static org.ase.fourwins.board.Board.Score.WIN;
import static org.ase.fourwins.tournament.listener.InfluxDBRow.COLUMNNAME_PLAYER_ID;
import static org.ase.fourwins.tournament.listener.InfluxDBRow.COLUMNNAME_VALUE;
import static org.ase.fourwins.tournament.listener.InfluxDBRow.MEASUREMENT_NAME;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import org.ase.fourwins.board.Board.GameState;
import org.ase.fourwins.board.Board.Score;
import org.ase.fourwins.board.mockplayers.PlayerMock;
import org.ase.fourwins.game.Game;
import org.ase.fourwins.game.Player;
import org.ase.fourwins.tournament.listener.InfluxDBListener;
import org.ase.fourwins.tournament.listener.InfluxDBRow;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.influxdb.impl.InfluxDBResultMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

class InfluxDBListenerIT {

	private static final int INFLUX_PORT = 8086;

	static GenericContainer influx = new GenericContainer("influxdb").withExposedPorts(INFLUX_PORT);

	private InfluxDBListener sut;
	private InfluxDB influxDB;

	@BeforeEach
	public void setup() {
		influx.start();
		String url = "http://" + influx.getContainerIpAddress() + ":" + influx.getMappedPort(INFLUX_PORT);
		System.out.println("InfluxDB url " + url);
		sut = new InfluxDBListener(url).createDatabase();

		influxDB = InfluxDBFactory.connect(url, "root", "root");
		influxDB.setDatabase(sut.getDatabaseName());
	}

	@AfterEach
	public void tearDown() {
		sut.dropDatabase();
		influx.stop();
	}

	@Test
	void testInfluxDBIsPingable() {
		assertThat(influxDB.ping().isGood(), is(true));
	}

	@Test
	void testOneGameEndingIsInsertedToInfluxDB() {
		List<Player> givenPlayers = players(2);
		whenEnded(aGameOf(givenPlayers, WIN, 0));
		scoresAre(givenPlayers, 1.0, 0.0);
	}

	@Test
	void testPlayerMakesIllegalMoveAndOpponentGetsAFullPoint() {
		List<Player> players = players(2);
		whenEnded(aGameOf(players, LOSE, 0));
		scoresAre(players, 0.0, 1.0);
	}

	@Test
	void testBothPlayersGetAHalfPointForADraw() {
		List<Player> players = players(2);
		whenEnded(aGameOf(players, DRAW, 0));
		scoresAre(players, 0.5, 0.5);
	}

	private void scoresAre(List<Player> players, double... scores) {
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

	private double scoreOf(Player player) {
		return rows(player).stream().mapToDouble(InfluxDBRow::getValue).sum();
	}

	private List<InfluxDBRow> rows(Player player) {
		return new InfluxDBResultMapper().toPOJO(scoreQuery(player), InfluxDBRow.class);
	}

	
	private QueryResult scoreQuery(Player player) {
		return influxDB.query(new Query("SELECT " + COLUMNNAME_VALUE + " FROM " + MEASUREMENT_NAME + " WHERE \""
				+ COLUMNNAME_PLAYER_ID + "\" = '" + player.getToken() + "'", sut.getDatabaseName()));
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
