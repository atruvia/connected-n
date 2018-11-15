package org.ase.fourwins.influxdb;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;

import org.ase.fourwins.board.Board.GameState;
import org.ase.fourwins.board.Board.Score;
import org.ase.fourwins.board.mockplayers.PlayerMock;
import org.ase.fourwins.game.Game;
import org.ase.fourwins.game.Player;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InfluxIT {
	private static final String RETENTION_POLICY = "default";
	private static final String DBNAME = "GAMES";
	private InfluxDB influxDB;
	private InfluxDBListener listener;

	@BeforeEach
	public void setup() {
		influxDB = InfluxDBFactory.connect("http://localhost:8086", "root",
				"root");
		listener = new InfluxDBListener(influxDB, RETENTION_POLICY, DBNAME);
		influxDB.query(new Query("CREATE DATABASE " + DBNAME, DBNAME));
		influxDB.setDatabase(DBNAME);
	}

	@AfterEach
	public void tearDown() {
		influxDB.query(new Query("DROP DATABASE \"" + DBNAME + "\"", DBNAME));
	}

	@Test
	void testInfluxDBIsPingable() {
		assertThat(influxDB.ping().isGood(), is(true));
	}

	@Test
	void testOneGameEndingIsInsertedToInfluxDB() {
		Player p1 = new PlayerMock("P1");
		Game game = buildGame(p1.getToken(), Score.WIN, Arrays.asList(p1));
		listener.gameEnded(game);

		Object pointsForP1 = queryPoints(p1);

		assertThat(pointsForP1, is(1.0));
	}

	@Test
	void testPlayerMakesIllegalMoveAndOpponentGetsAFullPoint() {
		Player p1 = new PlayerMock("P1");
		Player p2 = new PlayerMock("P2");
		Game game = buildGame(p1.getToken(), Score.LOSE, Arrays.asList(p1, p2));
		listener.gameEnded(game);

		Object pointsForP2 = queryPoints(p2);

		assertThat(pointsForP2, is(1.0));
	}

	@Test
	void testBothPlayersGetAHalfPointForADraw() {
		Player p1 = new PlayerMock("P1");
		Player p2 = new PlayerMock("P2");
		Game game = buildGame(p1.getToken(), Score.DRAW, Arrays.asList(p1, p2));
		listener.gameEnded(game);

		assertThat(queryPoints(p1), is(0.5));
		assertThat(queryPoints(p2), is(0.5));
	}

	private Object queryPoints(Player player) {
		QueryResult query = influxDB.query(new Query("SELECT value FROM "
				+ DBNAME + " WHERE \"player_id\" = '" + player.getToken() + "'",
				DBNAME));

		return query.getResults().get(0).getSeries().get(0).getValues().get(0)
				.get(1);
	}

	private Game buildGame(String lastToken, Score score,
			List<Player> players) {
		Game game = new Game() {

			@Override
			public Game runGame() {
				throw new UnsupportedOperationException();
			}

			@Override
			public GameState gameState() {
				return GameState.builder().token(lastToken).score(score)
						.build();
			}

			@Override
			public List<Player> getPlayers() {
				return players;
			}
		};
		return game;
	}

}
