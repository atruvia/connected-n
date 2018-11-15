package org.ase.fourwins.influxdb;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

import java.util.List;

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

		getPointList(game).forEach(point -> {
			BatchPoints batchPoint = createBatchPoint().point(point);
			influxDB.write(batchPoint);
		});

	}

	private BatchPoints createBatchPoint() {
		return BatchPoints.database(databaseName).tag("async", "true")
				.retentionPolicy(retentionPolicy)
				.consistency(ConsistencyLevel.ALL).build();
	}

	private List<Point> getPointList(Game game) {
		Object lastToken = game.gameState().getToken();
		Score score = game.gameState().getScore();
		if (score.equals(Score.LOSE)) {
			return game.getPlayers().stream()
					.filter(p -> !p.getToken().equals(lastToken))
					.map(Player::getToken).map(this::createFullPointForToken)
					.collect(toList());
		} else if (score.equals(Score.WIN)) {
			return asList(createFullPointForToken(lastToken));
		} else if (score.equals(Score.DRAW)) {
			return game.getPlayers().stream().map(Player::getToken)
					.map(this::createHalfPointForToken).collect(toList());
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
		return Point.measurement("GAMES")
				.addField("player_id", lastToken.toString())
				.addField("value", value).build();
	}

}
