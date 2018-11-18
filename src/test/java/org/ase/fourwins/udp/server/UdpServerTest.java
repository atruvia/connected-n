package org.ase.fourwins.udp.server;

import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.joining;
import static org.ase.fourwins.udp.server.UdpServer.MAX_CLIENT_NAME_LENGTH;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.ase.fourwins.board.Board.GameState;
import org.ase.fourwins.game.Player;
import org.ase.fourwins.tournament.Tournament;
import org.ase.fourwins.tournament.Tournament.RegistrationResult;
import org.ase.fourwins.udp.udphelper.UdpCommunicator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.verification.VerificationMode;

import lombok.Getter;

public class UdpServerTest {

	static class DummyClient {

		@Getter
		private final String name;
		private final UdpCommunicator communicator;
		@Getter
		private final List<String> received = new CopyOnWriteArrayList<String>();

		public DummyClient(String name, String remoteHost, int remotePort) throws IOException {
			this.name = name;
			this.communicator = new UdpCommunicator(remoteHost, remotePort);
			this.communicator.addMessageListener(received -> messageReceived(received));
			runInBackground(() -> {
				try {
					this.communicator.listenForMessages();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
			register();
		}
		
		protected void messageReceived(String received) {
			System.out.println("--- " + received + " by " + getName());
			DummyClient.this.received.add(received);
		}

		protected void register() throws IOException {
			send("REGISTER;" + name);
		}

		protected void unregister() throws IOException {
			send("UNREGISTER");
		}

		void send(String message) throws IOException {
			this.communicator.getMessageSender().send(message);
		}

		void trySend(String message) {
			try {
				send(message);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		void assertReceived(String... messages) throws InterruptedException {
			List<String> expected = asList(messages);
			while (getReceived().size() < expected.size()) {
				MILLISECONDS.sleep(25);
			}
			assertThat(getReceived(), is(expected));
		}

		List<String> waitUntilReceived(int expectedSize) throws InterruptedException {
			List<String> received;
			while ((received = getReceived()).size() < expectedSize) {
				MILLISECONDS.sleep(25);
			}
			return received;
		}

	}

	private final int serverPort = freePort();

	private final Tournament tournament = tournamentMock();

	private Tournament tournamentMock() {
		Tournament tournament = mock(Tournament.class);
		when(tournament.registerPlayer(Mockito.any(Player.class))).thenReturn((RegistrationResult) () -> true);
		return tournament;
	}

	private final UdpServer sut = udpServerInBackground();

	private UdpServer udpServerInBackground() {
		UdpServer udpServer = new UdpServer(serverPort, tournament);
		runInBackground(() -> udpServer.startServer());
		return udpServer;
	}

	private static void runInBackground(Runnable runnable) {
		new Thread(runnable).start();
	}

	@AfterEach
	public void tearDown() {
//		sut.shutdown();
	}

	@Test
	void clientCanConnectToServer() throws IOException, InterruptedException {
		infiniteSeason(tournament);
		assertTimeout(ofSeconds(10), () -> {
			DummyClient client1 = new DummyClient("1", "localhost", serverPort);
			verify(tournament, timesWithTimeout(1)).registerPlayer(Mockito.any(Player.class));
			client1.assertReceived("Welcome 1");
			verify(tournament, times(0)).playSeason(anyGameStateConsumer());
		});
	}

	@Test
	void canHandleEmptyCorruptedRegisterMessage() throws IOException {
		infiniteSeason(tournament);
		assertTimeout(ofSeconds(10), () -> {
			new DummyClient("1", "localhost", serverPort) {
				protected void register() throws IOException {
					send("REGISTER;");
				}
			};
			verify(tournament, timesWithTimeout(0)).registerPlayer(Mockito.any(Player.class));
			verify(tournament, timesWithTimeout(0)).playSeason(anyGameStateConsumer());
		});
	}

	@Test
	void acceptLongName() throws IOException, InterruptedException {
		infiniteSeason(tournament);
		assertTimeout(ofSeconds(10), () -> {
			String longName = nameOfLength(MAX_CLIENT_NAME_LENGTH);
			DummyClient client = new DummyClient(longName, "localhost", serverPort);
			verify(tournament, timesWithTimeout(1)).registerPlayer(Mockito.any(Player.class));
			client.assertReceived("Welcome " + longName);
			verify(tournament, timesWithTimeout(0)).playSeason(anyGameStateConsumer());
		});
	}

	@Test
	void denyTooLongName() throws IOException, InterruptedException {
		infiniteSeason(tournament);
		assertTimeout(ofSeconds(10), () -> {
			String longName = nameOfLength(MAX_CLIENT_NAME_LENGTH + 1);
			DummyClient client = new DummyClient(longName, "localhost", serverPort);
			verify(tournament, timesWithTimeout(0)).registerPlayer(Mockito.any(Player.class));
			client.assertReceived("NAME_TOO_LONG");
			verify(tournament, timesWithTimeout(0)).playSeason(anyGameStateConsumer());
		});
	}

	@Test
	void afterSecondClientConnectsTheTournamentIsStarted() throws IOException, InterruptedException {
		infiniteSeason(tournament);
		assertTimeout(ofSeconds(10), () -> {
			DummyClient client1 = new DummyClient("1", "localhost", serverPort);
			DummyClient client2 = new DummyClient("2", "localhost", serverPort);

			verify(tournament, timesWithTimeout(2)).registerPlayer(Mockito.any(Player.class));
			client1.assertReceived("Welcome 1");
			client2.assertReceived("Welcome 2");
			verify(tournament, timesWithTimeout(1)).playSeason(anyGameStateConsumer());
		});
	}

	@Test
	void seasonWillOnlyBeStartedIfTwoOreMorePlayersAreRegistered() throws IOException, InterruptedException {
		infiniteSeason(tournament);
		assertTimeout(ofSeconds(10), () -> {
			new DummyClient("1", "localhost", serverPort);
			new DummyClient("2", "localhost", serverPort);

			verify(tournament, timesWithTimeout(1)).playSeason(anyGameStateConsumer());
			verify(tournament, timesWithTimeout(2)).registerPlayer(Mockito.any(Player.class));

			// while season is running others can register
			DummyClient client3 = new DummyClient("3", "localhost", serverPort);
			client3.assertReceived("Welcome 3");
			verify(tournament, timesWithTimeout(3)).registerPlayer(Mockito.any(Player.class));
		});
	}

	@Test
	void canUnregister() throws IOException, InterruptedException {
		infiniteSeason(tournament);
		assertTimeout(ofSeconds(10), () -> {
			DummyClient client = new DummyClient("1", "localhost", serverPort);
			client.unregister();
			client.assertReceived("Welcome 1", "UNREGISTERED");
		});
	}

	@Test
	void whenDeregisteringNoNextSeasonIsStarted() throws IOException, InterruptedException {
		AtomicInteger seasonsStarted = new AtomicInteger(0);
		doAnswer(s -> {
			seasonsStarted.incrementAndGet();
			MILLISECONDS.sleep(25);
			return Stream.empty();
		}).when(tournament).playSeason(anyGameStateConsumer());
		assertTimeout(ofSeconds(10), () -> {
			new DummyClient("1", "localhost", serverPort);
			DummyClient client2 = new DummyClient("2", "localhost", serverPort);

			verify(tournament, timesWithTimeout(1)).playSeason(anyGameStateConsumer());

			// while season is running others can register
			DummyClient client3 = new DummyClient("3", "localhost", serverPort);
			client3.assertReceived("Welcome 3");
			verify(tournament, timesWithTimeout(3)).registerPlayer(Mockito.any(Player.class));

			// TODO signal to Mockito answer to delay until...

			client3.unregister();
			client2.unregister();
			client3.assertReceived("Welcome 3", "UNREGISTERED");
			client2.assertReceived("Welcome 2", "UNREGISTERED");
			int seasonsStartedBeforeUnregister = seasonsStarted.get();

			// TODO ...here

			// TODO eliminate wait
			SECONDS.sleep(1);
			assertThat(seasonsStarted.get(), is(seasonsStartedBeforeUnregister));

		});
	}

//	TODO joining with long runner
//	TODO test when returning a wrong UUID the next message must be working

	@Test
	void aReRegisterdClientIsNotANewPlayer() throws IOException {
		infiniteSeason(tournament);
		assertTimeout(ofSeconds(10), () -> {
			String nameToReuse = "1";
			DummyClient client = new DummyClient(nameToReuse, "localhost", serverPort);
			client.assertReceived("Welcome " + nameToReuse);
			DummyClient newClientWithSameTokenFromSameIP = new DummyClient(nameToReuse, "localhost", serverPort);
			newClientWithSameTokenFromSameIP.assertReceived("Welcome " + nameToReuse);

			verify(tournament, timesWithTimeout(0)).playSeason(anyGameStateConsumer());
			client.unregister();
			newClientWithSameTokenFromSameIP.unregister();
		});
	}

	private void infiniteSeason(Tournament mock) {
		doAnswer(s -> {
			while (true) {
				DAYS.sleep(Long.MAX_VALUE);
			}
		}).when(tournament).playSeason(anyGameStateConsumer());
	}

	private String nameOfLength(int len) {
		return IntStream.range(0, len).mapToObj(i -> "X").collect(joining());
	}

	private VerificationMode timesWithTimeout(int times) {
		return timeout(SECONDS.toMillis(5)).times(times);
	}

	private Consumer<GameState> anyGameStateConsumer() {
		return anyConsumer(GameState.class);
	}

	@SuppressWarnings("unchecked")
	private <T> Consumer<T> anyConsumer(Class<T> clazz) {
		return any(Consumer.class);
	}


	private static int freePort() {
		try {
			ServerSocket socket = new ServerSocket(0);
			socket.close();
			return socket.getLocalPort();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
