package org.ase.fourwins.tournament.listener.database;

import static org.ase.fourwins.tournament.listener.database.MySqlDbRow.COLUMNNAME_PLAYER_ID;
import static org.ase.fourwins.tournament.listener.database.MySqlDbRow.TABLE_NAME;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.ase.fourwins.game.Player;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class ScoresDatabase implements AutoCloseable {

	private static final String QUERY = "SELECT * FROM " + TABLE_NAME + " WHERE " + COLUMNNAME_PLAYER_ID + " = ?";

	private static final QueryRunner queryRunner = new QueryRunner();
	private static final BeanListHandler<MySqlDbRow> handler = new BeanListHandler<>(MySqlDbRow.class);

	private final Connection connection;

	public List<MySqlDbRow> scores(Player player) {
		try {
			return queryRunner.query(connection, QUERY, handler, player.getToken());
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void close() throws IOException {
		try {
			this.connection.close();
		} catch (SQLException e) {
			throw new IOException(e);
		}
	}

	public void assertThatScoresAre(List<Player> players, double... scores) throws SQLException {
		assertThat(players.stream().map(p -> scoreOf(p)).toArray(Double[]::new), is(scores));
	}

	public double scoreOf(Player player) {
		return rows(player).stream().reduce((a, b) -> b).map(MySqlDbRow::getValue).orElse(0.0);
	}

	public List<MySqlDbRow> rows(Player player) {
		return scores(player);
	}

}
