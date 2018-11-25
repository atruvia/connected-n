package org.ase.fourwins.udp.server;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.joining;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.ase.fourwins.board.Board.GameState;
import org.ase.fourwins.board.BoardInfo;
import org.ase.fourwins.game.Player;
import org.ase.fourwins.tournament.DefaultTournament;
import org.ase.fourwins.tournament.Tournament;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

public class UdpServer {

	public static final int MAX_CLIENT_NAME_LENGTH = 30;
	private static final Duration TIMEOUT = Duration.ofMillis(250);

	private final Tournament tournament;
	private final Map<UdpPlayerInfo, Player> players = new ConcurrentHashMap<>();

	private final DatagramSocket socket;
	private final byte[] buf = new byte[1024];

	private final Lock lock = new ReentrantLock();
	private final Condition playerRegistered = lock.newCondition();

	@Getter
	@RequiredArgsConstructor
	@ToString
	private static class UdpPlayerInfo {

		private final InetAddress adressInfo;
		private final Integer port;
		private final String name;

		private ArrayBlockingQueue<String> responses = new ArrayBlockingQueue<>(10);

		void reponseReceived(String received) {
			responses.offer(received);
		}

		String getResponse(Duration timeout, String delimiter, String uuid) throws TimeoutException {
			try {
				do {
					String response = responses.poll(timeout.toMillis(), MILLISECONDS);
					if (response == null) {
						throw new TimeoutException("TIMEOUT");
					}
					String[] splitted = response.split(delimiter);
					if (splitted.length > 1 && splitted[splitted.length - 1].equals(uuid)) {
						return Arrays.stream(splitted).limit(splitted.length - 1).collect(joining(delimiter));
					}
				} while (!responses.isEmpty());
				throw new IllegalStateException("No response for UUID " + uuid);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}

		synchronized void send(String message) {
			try {
				byte[] bytes = message.getBytes();
				try (DatagramSocket sendSocket = new DatagramSocket()) {
					sendSocket.setSoTimeout((int) TIMEOUT.toMillis());
					sendSocket.send(new DatagramPacket(bytes, bytes.length, getAdressInfo(), getPort()));
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		synchronized String sendAndWait(String command) throws TimeoutException {
			String delimiter = ";";
			String uuid = uuid();
			send(command + delimiter + uuid);
			return getResponse(TIMEOUT, delimiter, uuid);
		}

		private String uuid() {
			String uuid = UUID.randomUUID().toString();
			int pos = uuid.indexOf("-");
			return pos < 0 ? uuid : uuid.substring(0, pos);
		}
	}

	private final class UdpPlayer extends Player {

		private final UdpPlayerInfo playerInfo;

		private UdpPlayer(String token, UdpPlayerInfo playerInfo) {
			super(token);
			this.playerInfo = playerInfo;
		}

		@Override
		protected int nextColumn() {
			try {
				String response = playerInfo.sendAndWait("YOURTURN");
				if (!response.startsWith("INSERT;")) {
					throw new IllegalArgumentException("Unexpected response " + response);
				}
				String[] split = response.split(";");
				if (split.length < 2) {
					throw new IllegalArgumentException("Unexpected response " + response);
				}
				return Integer.parseInt(split[1]);
			} catch (TimeoutException e) {
				throw new IllegalArgumentException(e.getMessage());
			}
		}

		@Override
		public boolean joinGame(String opposite, BoardInfo boardInfo) {
			playerInfo.send("NEW GAME;" + opposite);
			return true;
		}

		@Override
		protected void tokenWasInserted(String token, int column) {
			playerInfo.send("TOKEN INSERTED;" + token + ";" + column);
			super.tokenWasInserted(token, column);
		}

		public void gameEnded(GameState state) {
			Object reason = state.getReason() == null ? "" : state.getReason();
			String message = Stream.of("RESULT", state.getScore(), state.getToken(), reason).map(Object::toString)
					.collect(joining(";"));
			playerInfo.send(message);
			super.gameEnded(state);
		}

	}

	public UdpServer(int port) {
		this(port, new DefaultTournament());
	}

	public UdpServer(int port, Tournament tournament) {
		this.tournament = tournament;
		try {
			socket = new DatagramSocket(port);
			System.out.println("Socket created");
			System.out.println("Listening on " + port);
		} catch (SocketException e) {
			throw new RuntimeException(e);
		}
		new Thread(() -> {
			while (true) {
				if (players.size() < 2) {
					try {
						lock.lock();
						playerRegistered.await(5, SECONDS);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					} finally {
						lock.unlock();
					}
					System.out.println("Waiting for more players to join");
				} else {
					System.out.println("Season starting");
					reregisterAllPlayers(tournament);
					tournament.playSeason(noop());
				}
			}
		}).start();

	}

	private Consumer<GameState> noop() {
		return s -> {
		};
	}

	private Consumer<GameState> sysout() {
		return s -> System.out.println(s.getScore() + ";" + s.getToken() + " --" + s.getReason());
	}

	private void reregisterAllPlayers(Tournament tournament) {
		players.entrySet().parallelStream().forEach(p -> {
			tournament.deregisterPlayer(p.getValue());
			try {
				if ("JOIN".equals(p.getKey().sendAndWait("NEW SEASON"))) {
					tournament.registerPlayer(p.getValue());
				}
			} catch (Exception e) {
				System.out.println("Exception while retrieving response for " + "NEW SEASON" + " for "
						+ p.getValue().getToken() + ": " + e.getMessage());
			}
		});
	}

	public UdpServer startServer() {
		System.out.println("Starting server");
		while (!socket.isClosed()) {
			DatagramPacket packet = new DatagramPacket(buf, buf.length);
			try {
				socket.receive(packet);
				String received = new String(packet.getData(), 0, packet.getLength());
				dispatchCommand(packet.getAddress(), packet.getPort(), received);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return this;
	}

	private void dispatchCommand(InetAddress clientIp, int clientPort, String received)
			throws SocketException, IOException {
		if (received.startsWith("REGISTER;")) {
			String[] split = received.split(";");
			if (split.length < 2) {
				new UdpPlayerInfo(clientIp, clientPort, "").send("NO_NAME_GIVEN");
				return;
			}
			String playerName = split[1].trim();
			if (playerName.length() > MAX_CLIENT_NAME_LENGTH) {
				new UdpPlayerInfo(clientIp, clientPort, playerName.substring(0, MAX_CLIENT_NAME_LENGTH))
						.send("NAME_TOO_LONG");
				return;
			}
			handleRegisterCommand(findBy(ipAddressAndName(clientIp, playerName)).map(i -> {
				tournament.deregisterPlayer(players.remove(i));
				return new UdpPlayerInfo(clientIp, clientPort, playerName);
			}).orElseGet(() -> new UdpPlayerInfo(clientIp, clientPort, playerName)));
		} else if ("UNREGISTER".equals(received)) {
			findBy(ipAndPort(clientIp, clientPort)).ifPresent(this::handleUnregisterCommand);
		} else {
			findBy(ipAndPort(clientIp, clientPort)).ifPresent(i -> i.reponseReceived(received));
		}
	}

	private Predicate<UdpPlayerInfo> ipAndPort(InetAddress clientIp, int clientPort) {
		return i -> (i.getAdressInfo().equals(clientIp) && i.getPort() == clientPort);
	}

	private Predicate<UdpPlayerInfo> ipAddressAndName(InetAddress clientIp, String name) {
		return i -> (i.getAdressInfo().equals(clientIp) && i.getName().equals(name));
	}

	private Optional<UdpPlayerInfo> findBy(Predicate<UdpPlayerInfo> p) {
		return players.keySet().stream().filter(p).findFirst();
	}

	private void handleRegisterCommand(UdpPlayerInfo playerInfo) {
		Player player = newPlayer(playerInfo, playerInfo.getName());
		if (!tournament.registerPlayer(player).isOk()) {
			playerInfo.send("NAME_ALREADY_TAKEN");
			return;
		}
		players.put(playerInfo, player);

		System.out.println(
				"Player " + playerInfo.getName() + " registered, we now have " + players.size() + " player(s)");
		playerInfo.send("Welcome " + playerInfo.getName());
		try {
			lock.lock();
			playerRegistered.signal();
		} finally {
			lock.unlock();
		}
	}

	private Player newPlayer(UdpPlayerInfo playerInfo, String playerName) {
		return new UdpPlayer(playerName, playerInfo);
	}

	private void handleUnregisterCommand(UdpPlayerInfo playerInfo) {
		Player removed = players.remove(playerInfo);
		playerInfo.send("UNREGISTERED");
		System.out.println(
				"Player " + removed.getToken() + " unregistered, we now have " + players.size() + " player(s)");
	}

}
