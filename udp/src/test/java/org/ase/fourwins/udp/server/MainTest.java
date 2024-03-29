package org.ase.fourwins.udp.server;

import static com.github.stefanbirkner.systemlambda.SystemLambda.withEnvironmentVariable;
import static java.util.stream.Collectors.joining;
import static org.ase.fourwins.udp.server.UdpServer.MAX_CLIENT_NAME_LENGTH;
import static org.ase.fourwins.udp.server.listeners.TournamentListenerEnabled2.ENV_NAME_TO_BE_SET;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import org.ase.fourwins.board.Board;
import org.ase.fourwins.board.Board.GameState;
import org.ase.fourwins.board.BoardInfo;
import org.ase.fourwins.board.BoardInfo.BoardInfoBuilder;
import org.ase.fourwins.board.Move.DefaultMove;
import org.ase.fourwins.game.Player;
import org.ase.fourwins.tournament.Tournament;
import org.ase.fourwins.tournament.listener.TournamentListener;
import org.ase.fourwins.udp.server.listeners.TournamentListenerDisabled;
import org.ase.fourwins.udp.server.listeners.TournamentListenerEnabled;
import org.ase.fourwins.udp.server.listeners.TournamentListenerEnabled2;
import org.ase.fourwins.udp.udphelper.UdpCommunicator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@Timeout(10)
public class MainTest {

	@Getter
	@Setter
	@Accessors(chain = true, fluent = true)
	private static class FakeTournament implements Tournament {

		private boolean blockOnPlaySeasonCall;
		private int seasons;
		private GameState state;

		@Override
		public void addTournamentListener(TournamentListener listener) {
			// noop
		}

		@Override
		public void removeTournamentListener(TournamentListener listener) {
			// noop
		}

		@Override
		public void playSeason(Collection<? extends Player> players, Consumer<GameState> consumer) {
			this.seasons++;
			if (this.state != null) {
				players.forEach(p -> p.gameEnded(state));
			}
			if (this.blockOnPlaySeasonCall) {
				waitForever();
			}
		}

		private static void waitForever() {
			try {
				Object object = new Object();
				synchronized (object) {
					object.wait();
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}

	}

	@RequiredArgsConstructor
	private static class BaseClient {

		@Getter
		protected final String name;
		protected final UdpCommunicator communicator;

		public BaseClient(String name, String remoteHost, int remotePort) throws IOException {
			this(name, new UdpCommunicator(remoteHost, remotePort));
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

		void assertReceived(String... messages) {
			await().until(this::getReceived, is(List.of(messages)));
		}

	}

	private final int serverPort = freePort();
	private int minPlayers = 2;

	private final FakeTournament fakeTournament = new FakeTournament();

	private Main runMainInBackground() {

		final Lock lock = new ReentrantLock();
		final Condition serverIsReady = lock.newCondition();

		Main main = new Main() {
			@Override
			protected UdpServer createUdpServer() {
				return new UdpServer() {
					@Override
					protected void playSeasonsForever(Tournament tournament) {
						signalReady();
						super.playSeasonsForever(tournament);
					}

					private void signalReady() {
						lock.lock();
						try {
							serverIsReady.signal();
						} finally {
							lock.unlock();
						}
					}
				}.setPort(serverPort).setMinPlayers(minPlayers);
			}
		};
		runInBackground(() -> {
			try {
				withEnvironmentVariable(ENV_NAME_TO_BE_SET, "anyValue").execute(() -> main.doMain(fakeTournament));
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

	@Test
	void clientCanConnectToServer() throws IOException {
		runMainInBackground();
		setupInfiniteSeason();
		newClientWithName("1").assertReceived(welcomed("1"));
		assertTournamentNotStartet();
	}

	@Test
	void acceptLongName() throws IOException {
		runMainInBackground();
		setupInfiniteSeason();
		String longestAllowedName = nameOfLength(MAX_CLIENT_NAME_LENGTH);
		newClientWithName(longestAllowedName).assertReceived(welcomed(longestAllowedName));
		assertTournamentNotStartet();
	}

	@Test
	void denyTooLongName() throws IOException {
		runMainInBackground();
		setupInfiniteSeason();
		String tooLongName = nameOfLength(MAX_CLIENT_NAME_LENGTH + 1);
		newClientWithName(tooLongName).assertReceived("NAME_TOO_LONG");
		assertTournamentNotStartet();
	}

	@Test
	void denyEmptyName() throws IOException {
		runMainInBackground();
		setupInfiniteSeason();
		newClientWithName(emptyName()).assertReceived("NO_NAME_GIVEN");
		assertTournamentNotStartet();
	}

	private String emptyName() {
		return "";
	}

	private DummyClient newClientWithName(String name) throws IOException {
		return new DummyClient(name, "localhost", serverPort);
	}

	private DummyClient newPlayingClientWithName(String name) throws IOException {
		return new PlayingClient(name, "localhost", serverPort, -1);
	}

	@Test
	void afterSecondClientConnectsTheTournamentIsStarted() throws IOException {
		runMainInBackground();
		setupInfiniteSeason();
		DummyClient client1 = newClientWithName("1");
		DummyClient client2 = newClientWithName("2");

		assertWelcomed(client1);
		assertWelcomed(client2);
		assertTournamentStartet();
	}

	@Test
	void whenMinPlayerIsOneAfterFirstClientConnectsTheTournamentIsStarted() throws IOException {
		minPlayers = 1;
		runMainInBackground();
		DummyClient client1 = newClientWithName("1");

		assertWelcomed(client1);
		assertTournamentStartet();
	}

	private void assertTournamentNotStartet() {
		verifySeasonsStarted(0);
	}

	private void assertTournamentStartet() {
		verifySeasonsStarted(1);
	}

	private void verifySeasonsStarted(int times) {
		await().untilAsserted(() -> assertThat(this.fakeTournament.seasons(), is(times)));
	}

	@Test
	void seasonWillOnlyBeStartedIfMoreThanOnePlayerIsRegistered() throws IOException {
		runMainInBackground();
		setupInfiniteSeason();
		newClientWithName("1");
		newClientWithName("2");

		assertTournamentStartet();

		// while season is running others can register
		newClientWithName("3").assertReceived(welcomed("3"));
	}

//	TODO joining with long runner
//	TODO test when returning a wrong UUID the next message must be working

	@Test
	void aReRegisterdClientIsNotANewPlayer() throws IOException {
		runMainInBackground();
		setupInfiniteSeason();
		String nameToReuse = "1";
		DummyClient client = newClientWithName(nameToReuse);
		client.assertReceived(welcomed(nameToReuse));
		DummyClient newClientWithSameTokenFromSameIP = newClientWithName(nameToReuse);
		newClientWithSameTokenFromSameIP.assertReceived(welcomed(nameToReuse));

		assertTournamentNotStartet();
	}

	@Test
	void sendsWinMessageToAllPlayers() throws IOException {
		runMainInBackground();
		DummyClient client1 = newPlayingClientWithName("1");
		DummyClient client2 = newPlayingClientWithName("2");
		tournamentOfBoardWithState(makeWinBoard(client1.getName()));
		assertAllReceived("RESULT;WIN;" + client1.getName() + ";" + "CONNECTED_LINE", client1, client2);
	}

	@Test
	void sendsLoseMessageToAllPlayers() throws IOException {
		runMainInBackground();
		DummyClient client1 = newPlayingClientWithName("1");
		DummyClient client2 = newPlayingClientWithName("2");
		tournamentOfBoardWithState(makeLoseBoard(client1.getName()));
		assertAllReceived("RESULT;LOSE;" + client1.getName() + ";ILLEGAL_COLUMN_ANNOUNCED", client1, client2);
	}

	@Test
	void sendsDrawMessageToAllPlayers() throws IOException {
		runMainInBackground();
		DummyClient client1 = newPlayingClientWithName("1");
		DummyClient client2 = newPlayingClientWithName("2");
		tournamentOfBoardWithState(makeDrawBoard());
		assertAllReceived("RESULT;DRAW;;", client1, client2);
	}

	@Test
	void verifyListenersAreLoadedByServiceLoader() {
		runMainInBackground();
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
		for (DummyClient client : clients) {
			await().until(client::getReceived, hasItems(expectedMessage));
		}
	}

	private void tournamentOfBoardWithState(Board board) {
		tournamentOfState(board.gameState());
	}

	private void tournamentOfState(GameState gameState) {
		this.fakeTournament.state(gameState).blockOnPlaySeasonCall(true);
	}

	private void assertWelcomed(DummyClient client) {
		await().until(client::getReceived, hasItem(welcomed(client.getName())));
	}

	private static String welcomed(String name) {
		return "WELCOME;" + name;
	}

	private void setupInfiniteSeason() {
		fakeTournament.blockOnPlaySeasonCall(true);
	}

	private static String nameOfLength(int length) {
		String name = IntStream.range(0, length).mapToObj(i -> "X").collect(joining());
		assert name.length() == length;
		return name;
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
