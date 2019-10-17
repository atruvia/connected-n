package org.ase.fourwins.tournament.listener.database;

import static org.ase.fourwins.tournament.listener.database.MysqlDBRow.COLUMNNAME_PLAYER_ID;
import static org.ase.fourwins.tournament.listener.database.MysqlDBRow.TABLE_NAME;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.ase.fourwins.game.Player;

public final class ScoresDatabase implements AutoCloseable {

	private Connection connection;

	public ScoresDatabase(Connection connection) {
		this.connection = connection;
	}

	public void init(String database) throws SQLException, IOException {
		Statement statement = connection.createStatement();
		for (String line : content("docker/mysql/sql.sql")) {
			if (!line.toUpperCase().startsWith("CREATE USER ") && !line.toUpperCase().startsWith("GRANT ")) {
				statement.executeUpdate(line);
			}
		}
	}

	private List<String> content(String script) throws IOException, FileNotFoundException {
		try (BufferedReader br = new BufferedReader(new FileReader(script))) {
			return br.lines().collect(Collectors.toList());
		}
	}

	public List<MysqlDBRow> scores(Player player) throws SQLException {
		QueryRunner runner = new QueryRunner();
		BeanListHandler<MysqlDBRow> handler = new BeanListHandler<>(MysqlDBRow.class);
		String query = "SELECT * FROM " + TABLE_NAME + " WHERE " + COLUMNNAME_PLAYER_ID + " = ?";
		return runner.query(connection, query, handler, player.getToken());
	}

	@Override
	public void close() throws IOException {
		try {
			this.connection.close();
		} catch (SQLException e) {
			throw new IOException(e);
		}
	}

}
