package org.ase.fourwins.tournament.listener.database;

import static java.util.function.Predicate.isEqual;
import static java.util.function.Predicate.not;
import static org.ase.fourwins.board.Board.Score.DRAW;
import static org.ase.fourwins.board.Board.Score.LOSE;
import static org.ase.fourwins.board.Board.Score.WIN;
import static org.ase.fourwins.tournament.listener.database.MySqlDbRow.COLUMNNAME_PLAYER_ID;
import static org.ase.fourwins.tournament.listener.database.MySqlDbRow.COLUMNNAME_VALUE;
import static org.ase.fourwins.tournament.listener.database.MySqlDbRow.TABLE_NAME;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.stream.Stream;

import org.apache.commons.dbutils.QueryRunner;
import org.ase.fourwins.board.Board.Score;
import org.ase.fourwins.game.Game;
import org.ase.fourwins.game.Player;
import org.ase.fourwins.tournament.listener.TournamentListener;

public class MySqlDbListener implements TournamentListener {

	private static final double POINTS_WIN = 1;
	private static final double POINTS_DRAW = 0.5;

	public static final String DEFAULT_DATABASE_NAME = "4WINS";

	private final Connection connection;

	private static final QueryRunner runner = new QueryRunner();
	private static final String insertSQL = "INSERT INTO " + TABLE_NAME + " (" + COLUMNNAME_PLAYER_ID + ","
			+ COLUMNNAME_VALUE + ") SELECT ?,COALESCE(MAX(value),0)+? FROM " + TABLE_NAME + " WHERE "
			+ COLUMNNAME_PLAYER_ID + "=?";

	public MySqlDbListener(String url, String user, String password) throws ClassNotFoundException, SQLException {
		this.connection = DriverManager.getConnection(url, user, password);
	}

	@Override
	public void gameEnded(Game game) {
		rows(game).forEach(this::insertRow);
	}

	private void insertRow(MySqlDbRow row) {
		try {
			runner.update(connection, insertSQL, row.getPlayerId(), row.getValue(), row.getPlayerId());
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private Stream<MySqlDbRow> rows(Game game) {
		Object lastToken = game.gameState().getToken();
		Score score = game.gameState().getScore();
		if (LOSE.equals(score)) {
			return tokens(game).filter(not(isEqual(lastToken))).map(t -> points(t, POINTS_WIN));
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

	private MySqlDbRow points(Object token, double points) {
		return new MySqlDbRow(String.valueOf(token), points);
	}

}
