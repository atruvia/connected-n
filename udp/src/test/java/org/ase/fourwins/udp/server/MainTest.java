package org.ase.fourwins.udp.server;

import static com.github.stefanbirkner.systemlambda.SystemLambda.withEnvironmentVariable;
import static java.lang.Long.MAX_VALUE;
import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.joining;
import static org.ase.fourwins.udp.server.UdpServer.MAX_CLIENT_NAME_LENGTH;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.ase.fourwins.board.Board;
import org.ase.fourwins.board.Board.GameState;
import org.ase.fourwins.board.BoardInfo;
import org.ase.fourwins.board.BoardInfo.BoardInfoBuilder;
import org.ase.fourwins.board.Move.DefaultMove;
import org.ase.fourwins.game.Player;
import org.ase.fourwins.tournament.Tournament;
import org.ase.fourwins.udp.server.listeners.TournamentListenerDisabled;
import org.ase.fourwins.udp.server.listeners.TournamentListenerEnabled;
import org.ase.fourwins.udp.server.listeners.TournamentListenerEnabled2;
import org.ase.fourwins.udp.udphelper.UdpCommunicator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.verification.VerificationMode;

import com.github.stefanbirkner.systemlambda.SystemLambda;

import lombok.Getter;

public class MainTest {

	private static final Duration TIMEOUT = ofSeconds(10);

	private static class BaseClient {

		@Getter
		protected final String name;
		protected final UdpCommunicator communicator;

		public BaseClient(String name, String remoteHost, int remotePort) throws IOException {
			this.name = name;
			this.communicator = new UdpCommunicator(remoteHost, remotePort);
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

	}

	static class DummyClient extends BaseClient {

		@Getter
		private final List<String> received = new CopyOnWriteArrayList<String>();

		public DummyClient(String name, String remoteHost, int remotePort) throws IOException {
			super(name, remoteHost, remotePort);
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
			DummyClient.this.received.add(received);
		}

		protected void register() throws IOException {
			send("REGISTER;" + name);
		}

		protected void unregister() throws IOException {
			send("UNREGISTER");
		}

		void assertReceived(String... messages) {
			await().until(this::getReceived, is(asList(messages)));
		}

	}

	private final int serverPort = freePort();

	private final Tournament tournament = mock(Tournament.class);

	private Main runMainInBackground() {

		Lock lock = new ReentrantLock();
		Condition serverIsReady = lock.newCondition();

		Main main = new Main() {
			@Override
			protected UdpServer createUdpServer(Tournament listeners) {
				return new UdpServer(port, tournament) {
					@Override
					protected void playSeasonsForever(Tournament tournament) {
						lock.lock();
						signalReady();
						super.playSeasonsForever(tournament);
					}

					private void signalReady() {
						try {
							serverIsReady.signal();
						} finally {
							lock.unlock();
						}
					}
				};
			}
		};
		main.setPort(serverPort);
		main.setTournament(tournament);
		runInBackground(() -> {
			try {
				withEnvironmentVariable("envNameThatIsSet", "anyValue").execute(main::doMain);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
		waitReady(lock, serverIsReady);
		return main;
	}

	private void waitReady(Lock lock, Condition ready) {
		lock.lock();
		try {
			ready.awaitUninterruptibly();
		} finally {
			lock.unlock();
		}
	}

	private static void runInBackground(Runnable runnable) {
		new Thread(runnable).start();
	}

	@BeforeEach
	public void setup() {
		runMainInBackground();
	}

	@AfterEach
	public void tearDown() {
//		sut.shutdown();
	}

	@Test
	void clientCanConnectToServer() throws IOException {
		setupInfiniteSeason(tournament);
		assertTimeoutPreemptively(TIMEOUT, () -> {
			newClientWithName("1").assertReceived(welcomed("1"));
			assertTournamentNotStartet();
		});
	}

	@Test
	void canHandleEmptyCorruptedRegisterMessage() throws IOException {
		setupInfiniteSeason(tournament);
		assertTimeoutPreemptively(TIMEOUT, () -> {
			new DummyClient("1", "localhost", serverPort) {
				protected void register() throws IOException {
					send("REGISTER;");
				}
			}.assertReceived("NO_NAME_GIVEN");
			assertTournamentNotStartet();
		});
	}

	@Test
	void acceptLongName() throws IOException {
		setupInfiniteSeason(tournament);
		assertTimeoutPreemptively(TIMEOUT, () -> {
			String longestAllowedName = nameOfLength(MAX_CLIENT_NAME_LENGTH);
			newClientWithName(longestAllowedName).assertReceived(welcomed(longestAllowedName));
			assertTournamentNotStartet();
		});
	}

	@Test
	void denyTooLongName() throws IOException {
		setupInfiniteSeason(tournament);
		assertTimeoutPreemptively(TIMEOUT, () -> {
			String tooLongName = nameOfLength(MAX_CLIENT_NAME_LENGTH + 1);
			newClientWithName(tooLongName).assertReceived("NAME_TOO_LONG");
			assertTournamentNotStartet();
		});
	}

	private DummyClient newClientWithName(String name) throws IOException {
		return new DummyClient(name, "localhost", serverPort);
	}

	private DummyClient newPlayingClientWithName(String name) throws IOException {
		return new PlayingClient(name, "localhost", serverPort, -1);
	}

	@Test
	void denyEmptyName() throws IOException {
		setupInfiniteSeason(tournament);
		assertTimeoutPreemptively(TIMEOUT, () -> {
			String emptyName = "";
			newClientWithName(emptyName).assertReceived("NO_NAME_GIVEN");
			assertTournamentNotStartet();
		});
	}

	@Test
	void afterSecondClientConnectsTheTournamentIsStarted() throws IOException {
		setupInfiniteSeason(tournament);
		assertTimeoutPreemptively(TIMEOUT, () -> {
			DummyClient client1 = newClientWithName("1");
			DummyClient client2 = newClientWithName("2");

			assertWelcomed(client1);
			assertWelcomed(client2);
			assertTournamentStartet();
		});
	}

	private void assertTournamentNotStartet() {
		verifySeasonsStarted(0);
	}

	private void assertTournamentStartet() {
		verifySeasonsStarted(1);
	}

	private void verifySeasonsStarted(int times) {
		verify(tournament, timesWithTimeout(times)).playSeason(anyCollection(), anyGameStateConsumer());
	}

	@Test
	void seasonWillOnlyBeStartedIfTwoOreMorePlayersAreRegistered() throws IOException {
		setupInfiniteSeason(tournament);
		assertTimeoutPreemptively(TIMEOUT, () -> {
			newClientWithName("1");
			newClientWithName("2");

			assertTournamentStartet();

			// while season is running others can register
			newClientWithName("3").assertReceived(welcomed("3"));
		});
	}

	@Test
	void canUnregister() throws IOException {
		setupInfiniteSeason(tournament);
		assertTimeoutPreemptively(TIMEOUT, () -> {
			DummyClient client = newClientWithName("1");
			client.unregister();
			client.assertReceived(welcomed("1"), unregistered());
		});
	}

	@Test
	void whenDeregisteringNoNextSeasonIsStarted() throws IOException {
		AtomicInteger seasonsStarted = new AtomicInteger(0);
		doAnswer(s -> {
			seasonsStarted.incrementAndGet();
			MILLISECONDS.sleep(25);
			return Stream.empty();
		}).when(tournament).playSeason(anyCollection(), anyGameStateConsumer());
		assertTimeoutPreemptively(TIMEOUT, () -> {
			newPlayingClientWithName("1");
			DummyClient client2 = newPlayingClientWithName("2");

			assertTournamentStartet();

			// while season is running others can register
			DummyClient client3 = newPlayingClientWithName("3");
			assertWelcomed(client3);

			// TODO signal to Mockito answer to delay until...

			client3.unregister();
			client2.unregister();

			assertWelcomed(client3);
			assertWelcomed(client2);

			await().until(client3::getReceived, hasItems(welcomed("3"), unregistered()));
			await().until(client2::getReceived, hasItems(welcomed("2"), unregistered()));

			int seasonsStartedBeforeUnregister = seasonsStarted.get();

			// TODO ...here

			// TODO eliminate wait
			SECONDS.sleep(3);
			assertThat(seasonsStarted.get(), is(seasonsStartedBeforeUnregister));
		});
	}

//	TODO joining with long runner
//	TODO test when returning a wrong UUID the next message must be working

	@Test
	void aReRegisterdClientIsNotANewPlayer() throws IOException {
		setupInfiniteSeason(tournament);
		assertTimeoutPreemptively(TIMEOUT, () -> {
			String nameToReuse = "1";
			DummyClient client = newClientWithName(nameToReuse);
			client.assertReceived(welcomed(nameToReuse));
			DummyClient newClientWithSameTokenFromSameIP = newClientWithName(nameToReuse);
			newClientWithSameTokenFromSameIP.assertReceived(welcomed(nameToReuse));

			assertTournamentNotStartet();
			client.unregister();
			newClientWithSameTokenFromSameIP.unregister();
		});
	}

	@Test
	void sendsWinMessageToAllPlayers() throws IOException {
		DummyClient client1 = newPlayingClientWithName("1");
		DummyClient client2 = newPlayingClientWithName("2");
		tournamentOfBoardWithState(makeWinBoard(client1.getName()));
		assertAllReceived("RESULT;WIN;" + client1.getName() + ";CONNECTED_ROW", client1, client2);
	}

	@Test
	void sendsLoseMessageToAllPlayers() throws IOException {
		DummyClient client1 = newPlayingClientWithName("1");
		DummyClient client2 = newPlayingClientWithName("2");
		tournamentOfBoardWithState(makeLoseBoard(client1.getName()));
		assertAllReceived("RESULT;LOSE;" + client1.getName() + ";ILLEGAL_COLUMN_ANNOUNCED", client1, client2);
	}

	@Test
	void sendsDrawMessageToAllPlayers() throws IOException {
		DummyClient client1 = newPlayingClientWithName("1");
		DummyClient client2 = newPlayingClientWithName("2");
		tournamentOfBoardWithState(makeDrawBoard());
		assertAllReceived("RESULT;DRAW;;", client1, client2);
	}

	@Test
	void verifyListenersAreLoadedByServiceLoader() {
		assertThat(TournamentListenerDisabled.isConstructorCalled(), is(false));
		assertThat(TournamentListenerEnabled.isConstructorCalled(), is(true));
		assertThat(TournamentListenerEnabled2.isConstructorCalled(), is(true));
	}

	private static Board makeWinBoard(String winnerToken) {
		return aBoard(oneOfOne().toConnect(1)).insertToken(new DefaultMove(0), winnerToken);
	}

	private static Board makeLoseBoard(String loseToken) {
		return aBoard(oneOfOne().toConnect(1)).insertToken(new DefaultMove(-1), loseToken);
	}

	private static Board makeDrawBoard() {
		return aBoard(oneOfOne().toConnect(2)).insertToken(new DefaultMove(0), "anyLastToken");
	}

	private static Board aBoard(BoardInfoBuilder connect) {
		return Board.newBoard(connect.build());
	}

	private static BoardInfoBuilder oneOfOne() {
		return BoardInfo.builder().rows(1).columns(1);
	}

	private void assertAllReceived(String expectedMessage, DummyClient... clients) {
		assertTimeoutPreemptively(TIMEOUT, () -> {
			for (DummyClient client : clients) {
				await().until(client::getReceived, hasItems(expectedMessage));
			}
			for (DummyClient client : clients) {
				client.unregister();
			}
		});
	}

	private void tournamentOfBoardWithState(Board board) {
		tournamentOfState(board.gameState());
	}

	@SuppressWarnings("unchecked")
	private void tournamentOfState(GameState gameState) {
		ArgumentCaptor<Collection<Player>> playerCaptor = ArgumentCaptor.forClass(Collection.class);
		ArgumentCaptor<Consumer<GameState>> consumerCaptor = ArgumentCaptor.forClass(Consumer.class);
		doAnswer(s -> callGameEnded(gameState, playerCaptor.getValue())).when(tournament)
				.playSeason(playerCaptor.capture(), consumerCaptor.capture());
	}

	private Object callGameEnded(GameState gameState, Collection<Player> players) {
		players.forEach(p -> p.gameEnded(gameState));
		waitForever();
		return null;
	}

	private void assertWelcomed(DummyClient client) {
		await().until(client::getReceived, hasItem(welcomed(client.getName())));
	}

	private static String welcomed(String name) {
		return "WELCOME;" + name;
	}

	private static String unregistered() {
		return "UNREGISTERED";
	}

	private static void setupInfiniteSeason(Tournament mock) {
		doAnswer(s -> {
			waitForever();
			throw new IllegalStateException();
		}).when(mock).playSeason(anyCollection(), anyGameStateConsumer());
	}

	private static void waitForever() {
		try {
			while (true) {
				DAYS.sleep(MAX_VALUE);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private static String nameOfLength(int length) {
		String name = IntStream.range(0, length).mapToObj(i -> "X").collect(joining());
		assert name.length() == length;
		return name;
	}

	private static VerificationMode timesWithTimeout(int times) {
		return timeout(SECONDS.toMillis(5)).times(times);
	}

	private static Consumer<GameState> anyGameStateConsumer() {
		return anyConsumer(GameState.class);
	}

	@SuppressWarnings("unchecked")
	private static <T> Consumer<T> anyConsumer(Class<T> clazz) {
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
