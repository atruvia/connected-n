package org.ase.fourwins.udp.server;

import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.stream;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
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

	public static final int UNREGISTER_AFTER_N_TIMEOUTS = 10;

	@Setter
	private int port = 4446;

	@Setter
	private int timeoutMillis = 250;

	@Setter
	private int delayMillis = 0;

	@Setter
	private int minPlayers = 2;

	private final Map<UdpPlayerInfo, Player> players = new ConcurrentHashMap<>();

	private final byte[] buf = new byte[1024];

	private final Lock lock = new ReentrantLock();
	private final Condition newPlayerRegistered = lock.newCondition();
	private volatile boolean keepSeasonRunning = true;

	private DatagramSocket socket;

	@Getter
	@RequiredArgsConstructor
	@ToString
	private class UdpPlayerInfo {

		private final InetAddress adressInfo;
		private final Integer port;
		private final String name;
		private final ArrayBlockingQueue<String> responses = new ArrayBlockingQueue<>(10);
		private int timeouts;

		void reponseReceived(String received) {
			responses.offer(received);
		}

		String getResponse(String delimiter, String uuid) throws TimeoutException {
			try {
				long timeoutAt = currentTimeMillis() + timeoutMillis;
				while (true) {
					String response = responses.poll(timeoutAt - currentTimeMillis(), MILLISECONDS);
					if (response == null) {
						if (timeouts++ >= UNREGISTER_AFTER_N_TIMEOUTS) {
							System.out.println("Deregistering " + name + " because of too many timeouts in a row");
							handleUnregisterCommand(this);
						}
						throw new TimeoutException("Timeout while waiting for response for UUID " + uuid);
					}
					timeouts = 0;
					String[] splitted = response.split(delimiter);
					if (splitted.length > 1 && splitted[splitted.length - 1].equals(uuid)) {
						return stream(splitted).limit(splitted.length - 1).collect(joining(delimiter));
					}
				}
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}

		void send(String command, Object... parts) {
			send(concat(Stream.of(command), stream(parts)).map(e -> e == null ? "" : String.valueOf(e))
					.collect(joining(";")));
		}

		synchronized void send(String message) {
			try {
				byte[] bytes = message.getBytes();
				socket.send(new DatagramPacket(bytes, bytes.length, getAdressInfo(), getPort()));
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

		private String uuid() {
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
			playerInfo.send("NEW GAME", opposite);
			return true;
		}

		@Override
		protected void tokenWasInserted(String token, int column) {
			playerInfo.send("TOKEN INSERTED", token, column);
			super.tokenWasInserted(token, column);
		}

		public void gameEnded(GameState state) {
			playerInfo.send("RESULT", state.getScore(), state.getToken(), state.getReason());
			super.gameEnded(state);
		}

	}

	protected void playSeasonsForever(Tournament tournament) {
		new Thread(() -> {
			delay();
			System.out.println("Tournament starting");
			while (keepSeasonRunning) {
				if (players.size() < minPlayers) {
					try {
						lock.lock();
						newPlayerRegistered.await(5, SECONDS);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					} finally {
						lock.unlock();
					}
					System.out.println("Waiting for more players to join");
				} else {
					tournament.playSeason(players.entrySet().parallelStream().filter(this::wantToJoin)
							.map(Entry::getValue).collect(toList()), noop());
				}
			}
			socket.close();
			System.out.println("Tournament stopped");
		}).start();
	}

	public void stop() {
		System.out.println("Tournament will stop");
		keepSeasonRunning = false;
	}

	public void stopAndAwaitSocketClosed() throws InterruptedException {
		stop();
		while (!socket.isClosed()) {
			TimeUnit.MILLISECONDS.sleep(100);
		}
	}

	private void delay() {
		if (delayMillis > 0) {
			System.out.println("Delaying start for " + delayMillis + " millis");
			try {
				TimeUnit.MILLISECONDS.sleep(delayMillis);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

	private Consumer<GameState> noop() {
		return s -> {
		};
	}

	private Consumer<GameState> sysout() {
		return s -> System.out.println(s.getScore() + ";" + s.getToken() + " -- " + s.getReason());
	}

	private boolean wantToJoin(Entry<UdpPlayerInfo, Player> p) {
		try {
			return "JOIN".equals(p.getKey().sendAndWait("NEW SEASON"));
		} catch (Exception e) {
			System.out.println("Exception while retrieving response for " + "NEW SEASON" + " for "
					+ p.getValue().getToken() + ": " + e.getMessage());
			return false;
		}
	}

	public UdpServer startServer(Tournament tournament) {
		System.out.println("Starting server");
		this.socket = createSocket();
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

	private DatagramSocket createSocket() {
		try {
			DatagramSocket socket = new DatagramSocket(port);
			socket.setSoTimeout(timeoutMillis);
			System.out.println("Listening on " + port);
			return socket;
		} catch (SocketException e) {
			throw new RuntimeException(e);
		}
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
			handleRegisterCommand(findBy(inetAddressAndName(clientIp, playerName)).map(i -> {
				players.remove(i);
				return newPlayer(clientIp, clientPort, playerName, timeoutMillis);
			}).orElseGet(() -> newPlayer(clientIp, clientPort, playerName, timeoutMillis)));
		} else {
			findBy(inetAddressAndPort(clientIp, clientPort)).ifPresent(i -> i.reponseReceived(received));
		}
	}

	private UdpPlayerInfo newPlayer(InetAddress clientIp, int clientPort, String playerName, int timeout) {
		return new UdpPlayerInfo(clientIp, clientPort, playerName);
	}

	private Predicate<UdpPlayerInfo> inetAddressAndPort(InetAddress clientIp, int clientPort) {
		return inetAddress(clientIp).and(port(clientPort));
	}

	private Predicate<UdpPlayerInfo> inetAddressAndName(InetAddress clientIp, String name) {
		return inetAddress(clientIp).and(name(name));
	}

	private Predicate<UdpPlayerInfo> inetAddress(InetAddress inetAddress) {
		return i -> Objects.equals(i.getAdressInfo(), inetAddress);
	}

	private Predicate<UdpPlayerInfo> port(int port) {
		return i -> i.getPort() == port;
	}

	private Predicate<UdpPlayerInfo> name(String name) {
		return i -> Objects.equals(i.getName(), name);
	}

	private Optional<UdpPlayerInfo> findBy(Predicate<UdpPlayerInfo> predicate) {
		return players.keySet().stream().filter(predicate).findFirst();
	}

	private void handleRegisterCommand(UdpPlayerInfo playerInfo) {
		Player player = new UdpPlayer(playerInfo);
		if (players.putIfAbsent(playerInfo, player) != null) {
			playerInfo.send("NAME_ALREADY_TAKEN");
			return;
		}

		System.out.println(
				"Player " + playerInfo.getName() + " registered, we now have " + players.size() + " player(s)");
		playerInfo.send("WELCOME", playerInfo.getName());
		try {
			lock.lock();
			newPlayerRegistered.signal();
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
