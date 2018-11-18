package org.ase.fourwins.udp.server;

import static java.time.Duration.ofSeconds;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static org.ase.fourwins.board.Board.Score.LOSE;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.ase.fourwins.board.Board.GameState;
import org.ase.fourwins.game.Game;
import org.ase.fourwins.tournament.DefaultTournament;
import org.ase.fourwins.tournament.ScoreSheet;
import org.ase.fourwins.tournament.Tournament;
import org.ase.fourwins.tournament.listener.TournamentListener;
import org.ase.fourwins.tournament.listener.TournamentScoreListener;
import org.ase.fourwins.udp.server.UdpServerTest.DummyClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import lombok.Getter;

public class UdpServerRealTournamentIT {

	private final class GameStateCollector implements TournamentListener {

		@Getter
		private List<GameState> gameStates = new CopyOnWriteArrayList<>();

		@Override
		public void gameEnded(Game game) {
			gameStates.add(game.gameState());
		}

	}

	private final int serverPort = freePort();

	private final DefaultTournament tournament = new DefaultTournament();

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
	void canReRegister() throws IOException, InterruptedException {
		assertTimeout(ofSeconds(10), () -> {
			assertWelcomed(playingClient("1", 0));
			assertWelcomed(playingClient("1", 0));
		});
	}

	@Test
	void canPlay_2() throws IOException, InterruptedException {
		assertTimeout(ofSeconds(10), () -> {
			TournamentScoreListener scoreListener = new TournamentScoreListener();
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

			assertHasTimeout(client1, stateListener, false);
			assertHasTimeout(client2, stateListener, false);
		});
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
		List<String> received = client.waitUntilReceived(1);
		assertThat(received.toString(), received.get(0), is("Welcome " + client.getName()));
	}

	@Test
	@Disabled
	void canPlay_Multi() throws IOException, InterruptedException {
		assertTimeout(ofSeconds(10), () -> {
			IntStream.range(0, 10).forEach(i -> {
				try {
					playingClient(String.valueOf(i), i % tournament.getBoardInfo().getColumns());
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});

			/// ...let it run for a long while
			TimeUnit.SECONDS.sleep(10);

			fail("add more assertions");

		});
	}

	@Test
	void aClientWithTimeout() throws IOException, InterruptedException {
		assertTimeout(ofSeconds(10), () -> {
			GameStateCollector stateListener = new GameStateCollector();
			tournament.addTournamentListener(stateListener);

			DummyClient client1 = playingClient("1", 0);
			DummyClient client2 = new DummyClient("2", "localhost", serverPort) {
				@Override
				protected void messageReceived(String received) {
					System.out.println("--- " + received + " by " + getName());
					super.messageReceived(received);
					if (received.startsWith("YOURTURN;")) {
						waitForever();
					}
				}

			};

			/// ...let it run for a while
			TimeUnit.SECONDS.sleep(5);

			client1.waitUntilReceived(1);
			client2.waitUntilReceived(1);

			client1.unregister();
			client2.unregister();

			assertHasTimeout(client1, stateListener, false);
			assertHasTimeout(client2, stateListener, true);

			List<String> newGames1 = newGames(client1);
			List<String> newGames2 = newGames(client2);

			System.out.println("new games 1 " + newGames1.size());
			System.out.println("new games 2 " + newGames2.size());

			assertEquals((double) newGames1.size(), (double) newGames2.size(), 0.0);
		});
	}

	private List<String> newGames(DummyClient client) {
		return client.getReceived().stream() //
				.filter(s -> s.equals("NEW GAME")) //
				.collect(toList());
	}

	private DummyClient playingClient(String name, int row) throws IOException {
		return new DummyClient(name, "localhost", serverPort) {
			@Override
			protected void messageReceived(String received) {
				super.messageReceived(received);
				if (received.startsWith("YOURTURN;")) {
					trySend("INSERT;" + row + ";" + received.split(";")[1]);
				}
			}
		};
	}

	private void waitForever() {
		try {
			while (true) {
				TimeUnit.DAYS.sleep(Long.MAX_VALUE);
			}
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
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
