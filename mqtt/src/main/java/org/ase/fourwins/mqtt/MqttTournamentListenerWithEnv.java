package org.ase.fourwins.mqtt;

import java.io.IOException;
import java.util.Optional;

import org.ase.fourwins.annos.OnlyActivateWhenEnvSet;
import org.ase.fourwins.tournament.listener.TournamentListener;

import lombok.experimental.Delegate;

@OnlyActivateWhenEnvSet("WITH_MQTT")
public class MqttTournamentListenerWithEnv implements TournamentListener {

	@Delegate
	private final MqttTournamentListener delegate;

	public MqttTournamentListenerWithEnv() throws NumberFormatException, IOException {
		delegate = new MqttTournamentListener(tryEnv("MQTT_HOST").orElse("localhost"),
				Integer.parseInt(tryEnv("MQTT_PORT").orElse("1883")));
	}

	private static Optional<String> tryEnv(String name) {
		return Optional.ofNullable(System.getenv(name));
	}

}
