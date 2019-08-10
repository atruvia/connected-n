package org.ase.fourwins.tournament.listener.database;

import static java.util.function.Predicate.isEqual;
import static org.ase.fourwins.board.Board.Score.DRAW;
import static org.ase.fourwins.board.Board.Score.LOSE;
import static org.ase.fourwins.board.Board.Score.WIN;
import static org.ase.fourwins.tournament.listener.database.InfluxDBRow.COLUMNNAME_PLAYER_ID;
import static org.ase.fourwins.tournament.listener.database.InfluxDBRow.COLUMNNAME_VALUE;
import static org.ase.fourwins.tournament.listener.database.InfluxDBRow.MEASUREMENT_NAME;

import java.util.stream.Stream;

import org.ase.fourwins.board.Board.Score;
import org.ase.fourwins.game.Game;
import org.ase.fourwins.game.Player;
import org.ase.fourwins.tournament.listener.TournamentListener;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDB.ConsistencyLevel;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.influxdb.dto.Query;

import lombok.Getter;

public class InfluxDBListener implements TournamentListener {

	private static final double POINTS_WIN = 1;
	private static final double POINTS_DRAW = 0.5;

	public static final String DEFAULT_DATABASE_NAME = "GAMES";

	private final InfluxDB influxDB;

	@Getter
	private final String databaseName;

	public InfluxDBListener(String url) {
		this(InfluxDBFactory.connect(url, "root", "root"), DEFAULT_DATABASE_NAME);
	}

	public InfluxDBListener(String url, String databaseName) {
		this(InfluxDBFactory.connect(url, "root", "root"), databaseName);
	}

	public InfluxDBListener(InfluxDB influxDB, String databaseName) {
		this.influxDB = influxDB;
		this.influxDB.setDatabase(databaseName);
		this.databaseName = databaseName;
	}

	public InfluxDBListener createDatabase() {
		influxDB.query(new Query("CREATE DATABASE " + databaseName, databaseName));
		return this;
	}

	public InfluxDBListener dropDatabase() {
		influxDB.query(new Query("DROP DATABASE \"" + databaseName + "\"", databaseName));
		return this;
	}

	@Override
	public void gameEnded(Game game) {
		BatchPoints batchPoints = batchPoints();
		getPoints(game).forEach(batchPoints::point);
		influxDB.write(batchPoints);
	}

	private BatchPoints batchPoints() {
		return BatchPoints.database(databaseName) //
				.tag("async", "true") //
				.consistency(ConsistencyLevel.ALL) //
				.build();
	}

	private Stream<Point> getPoints(Game game) {
		Object lastToken = game.gameState().getToken();
		Score score = game.gameState().getScore();
		if (LOSE.equals(score)) {
			return tokens(game).filter(isEqual(lastToken).negate()).map(t -> points(t, POINTS_WIN));
		} else if (WIN.equals(score)) {
			return Stream.of(points(lastToken, POINTS_WIN));
		} else if (DRAW.equals(score)) {
			return tokens(game).map(t -> points(t, POINTS_DRAW));
		}
		throw new RuntimeException("Game is still in progress!");
	}

	private Stream<String> tokens(Game game) {
		return game.getPlayers().stream().map(Player::getToken);
	}

	private Point points(Object token, double points) {
		return Point.measurement(MEASUREMENT_NAME) //
				.addField(COLUMNNAME_PLAYER_ID, String.valueOf(token)) //
				.tag(COLUMNNAME_PLAYER_ID, String.valueOf(token)) //
				.addField(COLUMNNAME_VALUE, points) //
				.build();
	}

}
