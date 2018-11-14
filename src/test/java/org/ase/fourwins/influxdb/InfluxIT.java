package org.ase.fourwins.influxdb;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.ase.fourwins.board.Board.GameState;
import org.ase.fourwins.board.Board.Score;
import org.ase.fourwins.game.Game;
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
	void testOneGameEndingIsInsertedToInfluxDB() throws InterruptedException {
		String token1 = "P1";
		Game game = buildGame(token1, Score.WIN);
		listener.gameEnded(game);

		Object pointsForP1 = queryPlayerPoints(token1);
		assertThat(pointsForP1, is(1.0));

	}

	private Object queryPlayerPoints(String token) {
		QueryResult query = influxDB.query(new Query("SELECT value FROM "
				+ DBNAME + " WHERE \"player_id\" = '" + token + "'", DBNAME));

		return query.getResults().get(0).getSeries().get(0).getValues().get(0)
				.get(1);
	}

	private Game buildGame(String token, Score score) {
		Game game = new Game() {

			@Override
			public Game runGame() {
				return null;
			}

			@Override
			public GameState gameState() {
				return GameState.builder().token(token).score(score).build();
			}
		};
		return game;
	}

}
