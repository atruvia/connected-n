package org.ase.fourwins.udp.server;

import static java.time.Duration.ofSeconds;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static org.ase.fourwins.board.Board.Score.LOSE;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import org.ase.fourwins.board.Board.GameState;
import org.ase.fourwins.game.Game;
import org.ase.fourwins.listener.SysoutTournamentListener;
import org.ase.fourwins.tournament.DefaultTournament;
import org.ase.fourwins.tournament.ScoreSheet;
import org.ase.fourwins.tournament.listener.TournamentListener;
import org.ase.fourwins.udp.server.MainTest.DummyClient;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import lombok.Getter;

public class UdpServerRealTournamentIT {

	private static final Duration TIMEOUT = ofSeconds(10);

	private static final String SERVER = "localhost";
	private final int serverPort = freePort();

	private final class GameStateCollector implements TournamentListener {

		@Getter
		private List<GameState> gameStates = new CopyOnWriteArrayList<>();

		@Override
		public void gameEnded(Game game) {
			gameStates.add(game.gameState());
		}

	}

	private final DefaultTournament tournament = new DefaultTournament();

	private UdpServer udpServerInBackground() {
		UdpServer udpServer = new UdpServer().setPort(serverPort);
		runInBackground(() -> udpServer.startServer(tournament));
		return udpServer;
	}

	private static void runInBackground(Runnable runnable) {
		new Thread(runnable).start();
	}

	@Test
	void canReRegister() throws IOException, InterruptedException {
		assertTimeoutPreemptively(TIMEOUT, () -> {
			udpServerInBackground();
			assertWelcomed(playingClient("1", 0));
			assertWelcomed(playingClient("1", 0));
		});
	}

	@Test
	void canPlay_2() throws IOException, InterruptedException {
		assertTimeoutPreemptively(TIMEOUT, () -> {
			udpServerInBackground();
			SysoutTournamentListener scoreListener = new SysoutTournamentListener();
			tournament.addTournamentListener(scoreListener);
			GameStateCollector stateListener = new GameStateCollector();
			tournament.addTournamentListener(stateListener);

			DummyClient client1 = playingClient("1", 0);
			DummyClient client2 = playingClient("2", 1);

			assertWelcomed(client1);
			assertWelcomed(client2);

			/// ...let it run for a while
			SECONDS.sleep(5);

			client1.unregister();
			client2.unregister();

			ScoreSheet scoreSheet = scoreListener.getScoreSheet();
			Double score1 = scoreSheet.scoreOf(client1.getName());
			Double score2 = scoreSheet.scoreOf(client2.getName());
			System.out.println("score 1 " + score1);
			System.out.println("score 2 " + score2);
			assertThat(score1, is(not(0)));
			assertThat(score2, is(not(0)));
			assertEquals(score1, score2, 1.0);

			List<String> results1 = getReceived(client1, s -> s.startsWith("RESULT;"));
			List<String> results2 = getReceived(client2, s -> s.startsWith("RESULT;"));
			assertThat(results1.size(), is(not(0)));
			assertThat(results2.size(), is(not(0)));
			assertThat(results1.size(), is(results2.size()));
			System.out.println("results 1 " + results1);
			System.out.println("results 2 " + results2);

			assertHasTimeout(client1, stateListener, false);
			assertHasTimeout(client2, stateListener, false);
		});
	}

	private List<String> getReceived(DummyClient client, Predicate<String> predicate) {
		return client.getReceived().stream().filter(predicate).collect(toList());
	}

	private void assertHasTimeout(DummyClient client, GameStateCollector gameStateCollector, boolean hadTimeout) {
		List<GameState> timeout = timeouts(gameStateCollector.getGameStates(), client.getName());
		assertThat(timeout.toString(), timeout.isEmpty(), is(!hadTimeout));
	}

	private List<GameState> timeouts(List<GameState> gameStates, Object token) {
		return gameStates.stream() //
				.filter(s -> LOSE.equals(s.getScore())) //
				.filter(s -> s.getToken().equals(token)) //
				.filter(s -> s.getReason().toLowerCase().contains("timeout")) //
				.collect(toList());
	}

	private void assertWelcomed(DummyClient client) throws InterruptedException {
		await().until(client::getReceived, hasItem("WELCOME;" + client.getName()));
	}

	@Test
	@Disabled
	void canPlay_Multi() throws IOException, InterruptedException {
		// TEST fails since is canceled after timeout has reached ;-)
		assertTimeoutPreemptively(TIMEOUT, () -> {
			udpServerInBackground();
			tournament.addTournamentListener(new SysoutTournamentListener());
			IntStream.range(0, 10).forEach(i -> {
				try {
					playingClient(String.valueOf(i), i % tournament.getBoardInfo().getColumns());
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
			uuidFaker();
			sometimesNoResponse();

			/// ...let it run for a long while
			TimeUnit.MINUTES.sleep(60);

			fail("add more assertions");

		});
	}

	private PlayingClient uuidFaker() throws IOException {
		return new PlayingClient("UUID Faker", SERVER, serverPort, 0) {

			private final Random random = new Random(System.currentTimeMillis());

			@Override
			protected void messageReceived(String received) {
				if (received.startsWith("NEW SEASON;")) {
					trySend("JOIN;" + (shouldFake() ? fakeUuid(received) : received.split(";")[1]));
				} else {
					super.messageReceived(received);
				}
			}

			private boolean shouldFake() {
				return random.nextInt(100) > 90;
			}

			private String fakeUuid(String received) {
				return "X" + received.split(";")[1] + "X";
			}
		};
	}

	private PlayingClient sometimesNoResponse() throws IOException {
		return new PlayingClient("Sometimes no response", SERVER, serverPort, 0) {

			private final Random random = new Random(System.currentTimeMillis());

			@Override
			protected void messageReceived(String received) {
				if (isMessageWithUuid(received) && shouldSwallow()) {
					try {
						TimeUnit.SECONDS.sleep(1);
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
				}
				super.messageReceived(received);
			}

			private boolean isMessageWithUuid(String received) {
				return received.startsWith("NEW SEASON;") || received.startsWith("YOURTURN;");
			}

			private boolean shouldSwallow() {
				return random.nextInt(100) > 90;
			}
		};
	}

	@Test
	void aClientThatDoNotRespondGetsDeregistered() throws IOException, InterruptedException {
		assertTimeoutPreemptively(TIMEOUT, () -> {
			udpServerInBackground();
			GameStateCollector stateListener = new GameStateCollector();
			tournament.addTournamentListener(stateListener);

			DummyClient client1 = playingClient("1", 0);
			DummyClient client2 = onlyRespondToFirstJoinSeason("2");

			/// ...let it run for a while
			TimeUnit.SECONDS.sleep(5);

			await().until(() -> getReceived(client2, s -> s.toLowerCase().contains("unregister")).size(),
					greaterThan(0));

			client1.unregister();
			client2.unregister();
		});
	}

	@Test
	void aClientWithTimeout() throws IOException, InterruptedException {
		assertTimeoutPreemptively(TIMEOUT, () -> {
			udpServerInBackground();
			GameStateCollector stateListener = new GameStateCollector();
			tournament.addTournamentListener(stateListener);

			DummyClient client1 = playingClient("1", 0);
			DummyClient client2 = onlyRespondToFirstJoinSeason("2");

			/// ...let it run for a while
			TimeUnit.SECONDS.sleep(5);

			await().until(client1.getReceived()::size, greaterThan(0));
			await().until(client2.getReceived()::size, greaterThan(0));

			client1.unregister();
			client2.unregister();

			assertHasTimeout(client1, stateListener, false);
			assertHasTimeout(client2, stateListener, true);

			List<String> newGames1 = newGames(client1);
			List<String> newGames2 = newGames(client2);

			System.out.println("new games 1 " + newGames1.size());
			System.out.println("new games 2 " + newGames2.size());

			assertEquals(newGames1.size(), newGames2.size(), 0.0);
		});
	}

	private DummyClient onlyRespondToFirstJoinSeason(String name) throws IOException {
		return new DummyClient(name, SERVER, serverPort) {
			private boolean responded;

			@Override
			protected void messageReceived(String received) {
				super.messageReceived(received);
				if (!responded && received.startsWith("NEW SEASON;")) {
					trySend("JOIN;" + received.split(";")[1]);
					responded = true;
				}
			}
		};
	}

	private List<String> newGames(DummyClient client) {
		return client.getReceived().stream() //
				.filter(s -> s.startsWith("NEW GAME;")) //
				.collect(toList());
	}

	private DummyClient playingClient(String name, int column) throws IOException {
		return new PlayingClient(name, SERVER, serverPort, column);
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
