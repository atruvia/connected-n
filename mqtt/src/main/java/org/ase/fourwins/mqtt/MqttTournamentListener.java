package org.ase.fourwins.mqtt;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.IOException;
import java.util.List;

import org.ase.fourwins.board.Board.GameState;
import org.ase.fourwins.game.Game;
import org.ase.fourwins.game.Game.GameId;
import org.ase.fourwins.game.Player;
import org.ase.fourwins.tournament.listener.TournamentListener;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MqttTournamentListener implements TournamentListener, AutoCloseable {

	private MqttClient mqttClient;

	public MqttTournamentListener(String host, int port) throws IOException {
		try {
			mqttClient = new MqttClient("tcp://" + host + ":" + port, getClass().getName(), new MemoryPersistence());
			mqttClient.setTimeToWait(SECONDS.toMillis(1));
			mqttClient.connect(connectOptions());
			System.out.println("MQTT connected to " + host + ":" + port);
		} catch (MqttException e) {
			throw new IOException(e);
		}
	}

	private MqttConnectOptions connectOptions() {
		MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
		mqttConnectOptions.setAutomaticReconnect(true);
		mqttConnectOptions.setMaxInflight(10_000);
		return mqttConnectOptions;
	}

	@Override
	public void gameStarted(Game game) {
		List<Player> players = game.getPlayers();
		GameId gameId = game.getId();
		publish(gameId + "/board/height", game.getBoardInfo().getRows());
		publish(gameId + "/board/width", game.getBoardInfo().getColumns());
		publish(gameId + "/players", players.size());
		for (int i = 0; i < players.size(); i++) {
			publish(gameId + "/player/" + (i + 1), players.get(i).getToken());
		}
		publish(gameId + "/state/start", "");
	}

	@Override
	public void newTokenAt(Game game, String token, int column) {
		publish(game.getId() + "/action/tokeninserted/" + token, column);
	}

	@Override
	public void gameEnded(Game game) {
		GameState gameState = game.gameState();
		publish(game.getId() + "/state/end/score", gameState.getScore());
		publish(game.getId() + "/state/end/reason", gameState.getReason());
		publish(game.getId() + "/state/end/token", gameState.getToken());
	}

	private void publish(String topic, Object payload) {
		try {
			mqttClient.publish(topic, new MqttMessage(String.valueOf(payload).getBytes()));
		} catch (MqttException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean isConnected() {
		return mqttClient.isConnected();
	}

	@Override
	public void close() throws IOException {
		try {
			if (mqttClient.isConnected()) {
				mqttClient.disconnect();
			}
			mqttClient.close();
		} catch (MqttException e) {
			throw new IOException(e);
		}
	}

}
