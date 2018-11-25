package org.ase.fourwins.tournament.listener;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.ase.fourwins.board.Board.Score.DRAW;
import static org.ase.fourwins.board.Board.Score.LOSE;
import static org.ase.fourwins.board.Board.Score.WIN;

import java.util.List;

import org.ase.fourwins.board.Board.Score;
import org.ase.fourwins.game.Game;
import org.ase.fourwins.game.Player;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDB.ConsistencyLevel;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.influxdb.dto.Query;

public class InfluxDBListener implements TournamentListener {

	public static final String DBNAME = "GAMES";

	private final InfluxDB influxDB;
	private final String databaseName;

	public InfluxDBListener(String url, String databaseName) {
		this(InfluxDBFactory.connect(url, "root", "root"), databaseName);
	}

	public InfluxDBListener(InfluxDB influxDB, String databaseName) {
		this.influxDB = influxDB;
		this.influxDB.setDatabase(databaseName);
		this.databaseName = databaseName;
	}

	public InfluxDBListener createDatabase(String url) {
		influxDB.query(new Query("CREATE DATABASE " + DBNAME, DBNAME));
		return this;
	}

	public InfluxDBListener dropDatabase() {
		influxDB.query(new Query("DROP DATABASE \"" + DBNAME + "\"", DBNAME));
		return this;
	}

	@Override
	public void gameEnded(Game game) {
		getPointList(game).forEach(point -> {
			BatchPoints batchPoint = createBatchPoint().point(point);
			influxDB.write(batchPoint);
		});

	}

	private BatchPoints createBatchPoint() {
		return BatchPoints.database(databaseName).tag("async", "true") //
//				.retentionPolicy(retentionPolicy)
				.consistency(ConsistencyLevel.ALL).build();
	}

	private List<Point> getPointList(Game game) {
		Object lastToken = game.gameState().getToken();
		Score score = game.gameState().getScore();
		if (score.equals(LOSE)) {
			return game.getPlayers().stream().filter(p -> !p.getToken().equals(lastToken)).map(Player::getToken)
					.map(this::createFullPointForToken).collect(toList());
		} else if (score.equals(WIN)) {
			return asList(createFullPointForToken(lastToken));
		} else if (score.equals(DRAW)) {
			return game.getPlayers().stream().map(Player::getToken).map(this::createHalfPointForToken)
					.collect(toList());
		}
		throw new RuntimeException("Game is still in progress!");
	}

	private Point createHalfPointForToken(String token) {
		return createPointForToken(token, 0.5);
	}

	private Point createFullPointForToken(Object token) {
		return createPointForToken(token, 1);
	}

	private Point createPointForToken(Object lastToken, double value) {
		return Point.measurement("GAMES").addField("player_id", lastToken.toString()).addField("value", value).build();
	}

}
