package org.ase.fourwins.mqtt;

import java.io.IOException;
import java.util.Optional;

import org.ase.fourwins.game.Game;
import org.ase.fourwins.tournament.listener.TournamentListener;

public class MqttTournamentListenerWithEnv implements TournamentListener {

	private final MqttTournamentListener delegate;

	public MqttTournamentListenerWithEnv() throws NumberFormatException, IOException {
		delegate = new MqttTournamentListener(tryEnv("MQTT_HOST").orElse("localhost"),
				Integer.parseInt(tryEnv("MQTT_PORT").orElse("1883")));
	}

	private static Optional<String> tryEnv(String name) {
		return Optional.ofNullable(System.getenv(name));
	}

	public void gameStarted(Game game) {
		delegate.gameStarted(game);
	}

	public void newTokenAt(Game game, String token, int column) {
		delegate.newTokenAt(game, token, column);
	}

	public void gameEnded(Game game) {
		delegate.gameEnded(game);
	}

}
