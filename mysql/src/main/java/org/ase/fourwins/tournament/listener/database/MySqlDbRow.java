package org.ase.fourwins.tournament.listener.database;

import lombok.Data;

@Data
public class MysqlDBRow {

	public static final String TABLE_NAME = "games";

	public static final String COLUMNNAME_PLAYER_ID = "player_id";
	public static final String COLUMNNAME_VALUE = "value";

	private String playerId;
	private double value;

}