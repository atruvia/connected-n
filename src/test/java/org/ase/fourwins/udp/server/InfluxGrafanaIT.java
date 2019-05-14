package org.ase.fourwins.udp.server;

import static java.time.Duration.ofSeconds;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static org.ase.fourwins.board.Board.Score.LOSE;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.IntStream;

import org.ase.fourwins.board.Board.GameState;
import org.ase.fourwins.game.Game;
import org.ase.fourwins.tournament.DefaultTournament;
import org.ase.fourwins.tournament.ScoreSheet;
import org.ase.fourwins.tournament.listener.InfluxDBListener;
import org.ase.fourwins.tournament.listener.TournamentListener;
import org.ase.fourwins.tournament.listener.TournamentScoreListener;
import org.ase.fourwins.udp.server.UdpServerTest.DummyClient;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import lombok.Getter;

public class InfluxGrafanaIT {

	private static final Duration TIMEOUT = ofSeconds(10);

	private static final String PORT = "8086";
	private static final String SERVER = "localhost";
	private static final String RETENTION_POLICY = "default";
	private static final String DBNAME = "GAMES";
	private static final String USERNAME = "root";
	private static final String PASSWORD = "root";
	private InfluxDB influxDB;
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

	@BeforeEach
	public void setup() {
		influxDB = InfluxDBFactory.connect("http://" + SERVER + ":" + PORT, USERNAME, PASSWORD);
		influxDB.query(new Query("CREATE DATABASE " + DBNAME, DBNAME));
		influxDB.setDatabase(DBNAME);
	}

	@AfterEach
	public void tearDown() {
		influxDB.query(new Query("DROP DATABASE \"" + DBNAME + "\"", DBNAME));
	}

	@Test
	void canPlay_2() throws IOException, InterruptedException {
		assertTimeoutPreemptively(TIMEOUT, () -> {
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
	void canPlay_Multi() throws IOException, InterruptedException {
		InfluxDBListener influxDBListener = new InfluxDBListener(influxDB, DBNAME);
		tournament.addTournamentListener(influxDBListener);
		assertTimeoutPreemptively(TIMEOUT, () -> {
			IntStream.range(0, 10).forEach(i -> {
				try {
					playingClient(String.valueOf(i), i % tournament.getBoardInfo().getColumns());
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
			new PlayingClient("UUID Faker", SERVER, serverPort, 0) {
				@Override
				protected void messageReceived(String received) {
					if (received.startsWith("NEW SEASON;")) {
						trySend("JOIN;" + "fakeduuid");
					} else {
						super.messageReceived(received);
					}
				}
			};

			/// ...let it run for a long while
			MINUTES.sleep(30);

			fail("add more assertions");

		});
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
