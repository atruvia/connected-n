package org.ase.fourwins.mqtt;

import static com.github.stefanbirkner.systemlambda.SystemLambda.withEnvironmentVariable;
import static io.moquette.BrokerConstants.HOST_PROPERTY_NAME;
import static io.moquette.BrokerConstants.PORT_PROPERTY_NAME;
import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Awaitility.setDefaultPollInterval;
import static org.awaitility.Awaitility.setDefaultTimeout;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.ase.fourwins.board.Board.GameState;
import org.ase.fourwins.game.Game;
import org.ase.fourwins.game.Player;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.moquette.server.Server;
import io.moquette.server.config.MemoryConfig;

class MqttTournamentListenerTest {

	private static class Message {

		private final String topic;
		private final String payload;

		public Message(String topic, String payload) {
			this.topic = topic;
			this.payload = payload;
		}

		public String getTopic() {
			return topic;
		}

		public String getPayload() {
			return payload;
		}

	}

	private static Duration timeout = ofSeconds(30);
	private static final String LOCALHOST = "localhost";

	static class MqttClientForTest implements Closeable {

		private final IMqttClient client;
		private final List<Message> received = new CopyOnWriteArrayList<>();

		public List<Message> getReceived() {
			return received;
		}

		public MqttClientForTest(String brokerHost, int brokerPort, String name) throws IOException {
			this.client = createClient(brokerHost, brokerPort, name, received);
		}

		private IMqttClient createClient(String brokerHost, int brokerPort, String name, List<Message> received)
				throws IOException {
			try {
				MqttClient client = new MqttClient("tcp://" + brokerHost + ":" + brokerPort, name,
						new MemoryPersistence());
				client.setTimeToWait(TimeUnit.SECONDS.toMillis(1));
				client.connect(connectOptions());
				client.setCallback(new MqttCallbackExtended() {

					@Override
					public void deliveryComplete(IMqttDeliveryToken token) {
					}

					@Override
					public void messageArrived(String topic, MqttMessage message) throws Exception {
						received.add(new Message(topic, new String(message.getPayload())));
					}

					@Override
					public void connectComplete(boolean reconnect, String serverURI) {
						try {
							subscribe(client);
						} catch (MqttException e) {
							throw new RuntimeException(e);
						}
					}

					@Override
					public void connectionLost(Throwable cause) {
					}
				});
				subscribe(client);
				return client;
			} catch (MqttException e) {
				throw new IOException(e);
			}
		}

		private static MqttConnectOptions connectOptions() {
			MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
			mqttConnectOptions.setAutomaticReconnect(true);
			return mqttConnectOptions;
		}

		private static void subscribe(MqttClient client) throws MqttException {
			client.subscribe("#");
		}

		public void close() throws IOException {
			try {
				client.unsubscribe("#");
				client.disconnect();
				client.close();
			} catch (MqttException e) {
				throw new IOException(e);
			}
		}

		public boolean isConnected() {
			return client.isConnected();
		}

		public void publish(String topic, MqttMessage message) throws MqttException, MqttPersistenceException {
			client.publish(topic, message);
		}

	}

	private Server broker;
	private int brokerPort;
	private MqttClientForTest secondClient;
	private MqttTournamentListenerWithEnv sut;

	@BeforeEach
	void setup() throws Exception {
		setDefaultTimeout(timeout.getSeconds() / 2, SECONDS);
		setDefaultPollInterval(500, MILLISECONDS);
		brokerPort = randomPort();
		broker = newMqttServer(LOCALHOST, brokerPort);
		secondClient = new MqttClientForTest(LOCALHOST, brokerPort, "client2");

		withEnvironmentVariable("MQTT_HOST", "localhost").and("MQTT_PORT", String.valueOf(brokerPort))
				.execute(() -> sut = new MqttTournamentListenerWithEnv());
		await().until(sut::isConnected);
	}

	@AfterEach
	void tearDown() throws Exception {
		sut.close();
		secondClient.close();
		broker.stopServer();
	}

	private int randomPort() throws IOException {
		try (ServerSocket socket = new ServerSocket(0);) {
			return socket.getLocalPort();
		}
	}

	private Server newMqttServer(String host, int port) throws IOException {
		Server server = new Server();
		Properties properties = new Properties();
		properties.setProperty(HOST_PROPERTY_NAME, host);
		properties.setProperty(PORT_PROPERTY_NAME, String.valueOf(port));
		server.startServer(new MemoryConfig(properties));
		return server;
	}

	@Test
	void doesPublishGameStart() {
		String id = "someId";
		assertTimeoutPreemptively(timeout, () -> {
			sut.gameStarted(game(id));
			await().until(() -> payloads(id + "/state/start"), is(anEmptyPayload()));
		});
	}

	@Test
	void doesPublishGameEnd() {
		String id = "someId";
		assertTimeoutPreemptively(timeout, () -> {
			sut.gameEnded(game("someId"));
			await().until(() -> payloads(id + "/state/end"), is(anEmptyPayload()));
		});
	}

	@Test
	void doesPublishInsertedTokens() {
		String id = "someId";
		assertTimeoutPreemptively(timeout, () -> {
			sut.newTokenAt(game(id), "X", 0);
			sut.newTokenAt(game(id), "O", 1);
			sut.newTokenAt(game(id), "X", -1);
			await().until(() -> payloads(id + "/action/tokeninserted/X"), is(asList("0", "-1")));
			await().until(() -> payloads(id + "/action/tokeninserted/O"), is(asList("1")));
		});
	}

	private static List<String> anEmptyPayload() {
		return asList("");
	}

	private List<String> payloads(String topic) {
		return messagesWithTopic(secondClient.getReceived(), topic).map(Message::getPayload).collect(toList());
	}

	private Stream<Message> messagesWithTopic(List<Message> messages, String topic) {
		return messages.stream().filter(m -> m.getTopic().equals(topic));
	}

	private Game game(String id) {
		return new Game() {

			@Override
			public String getId() {
				return id;
			}

			@Override
			public Game runGame() {
				throw new IllegalStateException();
			}

			@Override
			public List<Player> getPlayers() {
				throw new IllegalStateException();
			}

			@Override
			public GameState gameState() {
				throw new IllegalStateException();
			}
		};
	}

}
