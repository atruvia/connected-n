package org.ase.fourwins.tournament.listener.database;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MySqlDbRow {

	public static final String TABLE_NAME = "games";

	public static final String COLUMNNAME_PLAYER_ID = "player_id";
	public static final String COLUMNNAME_VALUE = "value";

	private String playerId;
	private double value;

}