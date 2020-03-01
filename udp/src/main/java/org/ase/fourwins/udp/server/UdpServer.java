package org.ase.fourwins.udp.server;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
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
import org.ase.fourwins.tournament.Tournament;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

@Accessors(chain = true)
public class UdpServer {

	public static final int MAX_CLIENT_NAME_LENGTH = 30;

	@Setter
	private int port = 4446;

	@Setter
	private int timeoutMillis = 250;

	private final Map<UdpPlayerInfo, Player> players = new ConcurrentHashMap<>();

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
		private final int timeoutInMillis;
		private final ArrayBlockingQueue<String> responses = new ArrayBlockingQueue<>(10);

		void reponseReceived(String received) {
			responses.offer(received);
		}

		String getResponse(String delimiter, String uuid) throws TimeoutException {
			try {
				do {
					String response = responses.poll(timeoutInMillis, MILLISECONDS);
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
					sendSocket.setSoTimeout(timeoutInMillis);
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
			return getResponse(delimiter, uuid);
		}

		private static String uuid() {
			String uuid = UUID.randomUUID().toString();
			int pos = uuid.indexOf("-");
			return pos < 0 ? uuid : uuid.substring(0, pos);
		}

		@Override
		public int hashCode() {
			return name == null ? 0 : name.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			return this == obj || ((obj != null && obj.getClass() == getClass())
					&& Objects.equals(name, ((UdpPlayerInfo) obj).name));
		}

	}

	private final class UdpPlayer extends Player {

		private final UdpPlayerInfo playerInfo;

		private UdpPlayer(UdpPlayerInfo playerInfo) {
			super(playerInfo.getName());
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
			Object token = state.getToken();
			String message = Stream.of("RESULT", state.getScore(), token == null ? "" : token, reason)
					.map(String::valueOf).collect(joining(";"));
			playerInfo.send(message);
			super.gameEnded(state);
		}

	}

	protected void playSeasonsForever(Tournament tournament) {
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
					tournament.playSeason(playersJoiningNextSeason().map(Entry::getValue).collect(toList()), noop());
				}
			}
		}).start();
	}

	private Consumer<GameState> noop() {
		return s -> {
		};
	}

	private Consumer<GameState> sysout() {
		return s -> System.out.println(s.getScore() + ";" + s.getToken() + " -- " + s.getReason());
	}

	private Stream<Entry<UdpPlayerInfo, Player>> playersJoiningNextSeason() {
		return players.entrySet().parallelStream().filter(p -> {
			try {
				return ("JOIN".equals(p.getKey().sendAndWait("NEW SEASON")));
			} catch (Exception e) {
				System.out.println("Exception while retrieving response for " + "NEW SEASON" + " for "
						+ p.getValue().getToken() + ": " + e.getMessage());
				return false;
			}
		});
	}

	public UdpServer startServer(Tournament tournament) {
		System.out.println("Starting server");
		DatagramSocket socket;
		try {
			socket = new DatagramSocket(port);
			System.out.println("Listening on " + port);
		} catch (SocketException e) {
			throw new RuntimeException(e);
		}
		playSeasonsForever(tournament);

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
				new UdpPlayerInfo(clientIp, clientPort, "", timeoutMillis).send("NO_NAME_GIVEN");
				return;
			}
			String playerName = split[1].trim();
			if (playerName.length() > MAX_CLIENT_NAME_LENGTH) {
				new UdpPlayerInfo(clientIp, clientPort, playerName.substring(0, MAX_CLIENT_NAME_LENGTH), timeoutMillis)
						.send("NAME_TOO_LONG");
				return;
			}
			handleRegisterCommand(findBy(inetAddressAndName(clientIp, playerName)).map(i -> {
				players.remove(i);
				return newPlayer(clientIp, clientPort, playerName, timeoutMillis);
			}).orElseGet(() -> newPlayer(clientIp, clientPort, playerName, timeoutMillis)));
		} else if ("UNREGISTER".equals(received)) {
			findBy(inetAddressAndPort(clientIp, clientPort)).ifPresent(this::handleUnregisterCommand);
		} else {
			findBy(inetAddressAndPort(clientIp, clientPort)).ifPresent(i -> i.reponseReceived(received));
		}
	}

	private UdpPlayerInfo newPlayer(InetAddress clientIp, int clientPort, String playerName, int timeout) {
		return new UdpPlayerInfo(clientIp, clientPort, playerName, timeout);
	}

	private Predicate<UdpPlayerInfo> inetAddressAndPort(InetAddress clientIp, int clientPort) {
		return inetAddress(clientIp).and(port(clientPort));
	}

	private Predicate<UdpPlayerInfo> inetAddressAndName(InetAddress clientIp, String name) {
		return inetAddress(clientIp).and(name(name));
	}

	private Predicate<UdpPlayerInfo> inetAddress(InetAddress inetAddress) {
		return i -> i.getAdressInfo().equals(inetAddress);
	}

	private Predicate<UdpPlayerInfo> port(int port) {
		return i -> i.getPort() == port;
	}

	private Predicate<UdpPlayerInfo> name(String name) {
		return i -> i.getName().equals(name);
	}

	private Optional<UdpPlayerInfo> findBy(Predicate<UdpPlayerInfo> p) {
		return players.keySet().stream().filter(p).findFirst();
	}

	private void handleRegisterCommand(UdpPlayerInfo playerInfo) {
		Player player = new UdpPlayer(playerInfo);
		if (players.putIfAbsent(playerInfo, player) != null) {
			playerInfo.send("NAME_ALREADY_TAKEN");
			return;
		}

		System.out.println(
				"Player " + playerInfo.getName() + " registered, we now have " + players.size() + " player(s)");
		System.out.println(playerInfo.getName() + ":" + playerInfo.getPort());
		playerInfo.send("WELCOME;" + playerInfo.getName());
		try {
			lock.lock();
			playerRegistered.signal();
		} finally {
			lock.unlock();
		}
	}

	private void handleUnregisterCommand(UdpPlayerInfo playerInfo) {
		Player removed = players.remove(playerInfo);
		playerInfo.send("UNREGISTERED");
		System.out.println(
				"Player " + removed.getToken() + " unregistered, we now have " + players.size() + " player(s)");
	}

}
