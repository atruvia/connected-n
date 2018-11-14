package org.ase.fourwins.influxdb;
import java.util.concurrent.TimeUnit;

import org.ase.fourwins.game.Game;
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
		Object token = game.gameState().getToken();

		BatchPoints batchPoints = BatchPoints.database(databaseName)
				.tag("async", "true").retentionPolicy(retentionPolicy)
				.consistency(ConsistencyLevel.ALL).build();

		Point point = Point.measurement("GAMES")
				.time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
				.addField("player_id", token.toString()).addField("value", 1L)
				.build();

		batchPoints.point(point);
		influxDB.write(batchPoints);
	}

}
