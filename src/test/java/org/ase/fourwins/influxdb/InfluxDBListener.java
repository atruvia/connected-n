package org.ase.fourwins.influxdb;
import java.util.concurrent.TimeUnit;

import org.ase.fourwins.board.Board.Score;
import org.ase.fourwins.game.Game;
import org.ase.fourwins.game.Player;
import org.ase.fourwins.tournament.TournamentListener;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDB.ConsistencyLevel;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;

public class InfluxDBListener implements TournamentListener {

	private InfluxDB influxDB;
	private String retentionPolicy;
	private String databaseName;

	public InfluxDBListener(InfluxDB influxDB, String retentionPolicy,
			String databaseName) {
		this.influxDB = influxDB;
		this.retentionPolicy = retentionPolicy;
		this.databaseName = databaseName;
	}

	@Override
	public void gameEnded(Game game) {
		Object lastToken = game.gameState().getToken();

		BatchPoints batchPoints = BatchPoints.database(databaseName)
				.tag("async", "true").retentionPolicy(retentionPolicy)
				.consistency(ConsistencyLevel.ALL).build();

		Score score = game.gameState().getScore();
		if (score.equals(Score.LOSE)) {
			game.getPlayers().stream()
					.filter(p -> !p.getToken().equals(lastToken))
					.map(Player::getToken).map(this::createFullPointForToken)
					.forEach(batchPoints::point);
		} else if (score.equals(Score.WIN)) {
			batchPoints.point(createFullPointForToken(lastToken));
		} else if (score.equals(Score.DRAW)) {
			game.getPlayers().stream().map(Player::getToken)
					.map(this::createHalfPointForToken)
					.forEach(batchPoints::point);
		}
		System.out.println(batchPoints);
		influxDB.write(batchPoints);
	}

	private Point createHalfPointForToken(String token) {
		return createPointForToken(token, 0.5);
	}

	private Point createFullPointForToken(Object token) {
		return createPointForToken(token, 1);
	}
	private Point createPointForToken(Object lastToken, double value) {
		Point point = Point.measurement("GAMES")
				.time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
				.addField("player_id", lastToken.toString())
				.addField("value", value).build();
		return point;
	}

}
