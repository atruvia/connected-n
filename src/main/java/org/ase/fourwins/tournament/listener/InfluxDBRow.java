package org.ase.fourwins.tournament.listener;

import org.influxdb.annotation.Column;
import org.influxdb.annotation.Measurement;

import lombok.Data;

@Measurement(name = InfluxDBRow.MEASUREMENT_NAME)
@Data
public class InfluxDBRow {

	public static final String MEASUREMENT_NAME = "SCORES";

	public static final String COLUMNNAME_PLAYER_ID = "player_id";
	public static final String COLUMNNAME_VALUE = "value";

	@Column(name = COLUMNNAME_PLAYER_ID, tag = true)
	private String playerId;

	@Column(name = COLUMNNAME_VALUE)
	private double value;

}