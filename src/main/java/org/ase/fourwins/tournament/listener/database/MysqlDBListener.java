package org.ase.fourwins.tournament.listener.database;

import static java.util.function.Predicate.isEqual;
import static org.ase.fourwins.board.Board.Score.DRAW;
import static org.ase.fourwins.board.Board.Score.LOSE;
import static org.ase.fourwins.board.Board.Score.WIN;
import static org.ase.fourwins.tournament.listener.database.MysqlDBRow.COLUMNNAME_PLAYER_ID;
import static org.ase.fourwins.tournament.listener.database.MysqlDBRow.COLUMNNAME_VALUE;
import static org.ase.fourwins.tournament.listener.database.MysqlDBRow.TABLE_NAME;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Stream;

import org.apache.commons.dbutils.QueryRunner;
import org.ase.fourwins.board.Board.Score;
import org.ase.fourwins.game.Game;
import org.ase.fourwins.game.Player;
import org.ase.fourwins.tournament.listener.TournamentListener;

public class MysqlDBListener implements TournamentListener {

	private static final double POINTS_WIN = 1;
	private static final double POINTS_DRAW = 0.5;

	public static final String DEFAULT_DATABASE_NAME = "4WINS";

	private final Connection connection;

	public MysqlDBListener(String url, String user, String password) throws ClassNotFoundException, SQLException {
		connection = DriverManager.getConnection(url, user, password);
		Statement statement = connection.createStatement();
		statement.executeUpdate("CREATE DATABASE IF NOT EXISTS " + database(url));
		statement.executeUpdate("" + //
				"CREATE TABLE IF NOT EXISTS games(" + //
//				"	id int NOT NULL PRIMARY KEY auto_increment," + //
				"	ts TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," + //
				"	player_id varchar(255)," + //
				"	value double" + //
				")");
	}

	private String database(String uri) {
		return uri.substring(uri.lastIndexOf('/') + 1);
	}

	private static final QueryRunner runner = new QueryRunner();
	private static final String insertSQL = "INSERT INTO " + TABLE_NAME + " (" + COLUMNNAME_PLAYER_ID + ","
			+ COLUMNNAME_VALUE + ") VALUES (?, ?)";

	@Override
	public void gameEnded(Game game) {
		rows(game).forEach(this::insertRow);
	}

	private void insertRow(MysqlDBRow row) {
		try {
			runner.update(connection, insertSQL, row.getPlayerId(), row.getValue());
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private Stream<MysqlDBRow> rows(Game game) {
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

	private MysqlDBRow points(Object token, double points) {
		MysqlDBRow row = new MysqlDBRow();
		row.setPlayerId(String.valueOf(token));
		row.setValue(points);
		return row;
	}

}
