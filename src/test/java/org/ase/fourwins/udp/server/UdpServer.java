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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;

import org.ase.fourwins.board.Board.Score;
import org.ase.fourwins.board.BoardInfo;
import org.ase.fourwins.game.Player;
import org.ase.fourwins.tournament.DefaultTournament;
import org.ase.fourwins.tournament.Tournament;

import lombok.Value;

public class UdpServer {

	public static final int MAX_CLIENT_NAME_LENGTH = 30;
	private final Duration TIMEOUT = Duration.ofMillis(250);

	private final Tournament tournament;
	// TODO switch to ConcurrentHashMap and remove sycnhronizations
	private final Map<UdpPlayerInfo, Player> players = new HashMap<>();
	private final Map<UdpPlayerInfo, ArrayBlockingQueue<String>> receiverQueues = new ConcurrentHashMap<>();

	private final DatagramSocket socket;
	private final byte[] buf = new byte[1024];

	private final Lock lock = new ReentrantLock();
	private final Condition playerRegistered = lock.newCondition();

	private final class UdpPlayer extends Player {

		private final UdpPlayerInfo playerInfo;

		private UdpPlayer(String token, UdpPlayerInfo playerInfo) {
			super(token);
			this.playerInfo = playerInfo;
		}

		@Override
		protected int nextColumn() {
			return Integer.parseInt(sendAndWait("YOURTURN", playerInfo));
		}

		@Override
		public boolean joinGame(String opposite, BoardInfo boardInfo) {
			return "JOINING".equals(sendAndWait("JOIN", playerInfo));
		}

	}

	private String sendAndWait(String command, UdpPlayerInfo playerInfo) {
		String delimiter = ";";
		String uuid = uuid();
		send(command + delimiter + uuid, playerInfo.getAdressInfo(), playerInfo.getPort());
		String response = fromQueue(playerInfo);
		if (response == null) {
			throw new IllegalStateException("TIMEOUT");
		}
		String[] splitted = response.split(delimiter);
		if (splitted.length < 2) {
			throw new IllegalArgumentException("Cannot handle/parse " + response);
		} else if (!splitted[splitted.length - 1].equals(uuid)) {
			throw new IllegalStateException(
					"UUID mismatch, expected " + uuid + " got " + splitted[splitted.length - 1]);
		}
		return Arrays.stream(splitted).limit(splitted.length - 1).collect(joining(delimiter));
	}

	private String fromQueue(UdpPlayerInfo playerInfo) {
		try {
			return receiverQueues.get(playerInfo).poll(TIMEOUT.toMillis(), MILLISECONDS);
		} catch (InterruptedException e) {
			throw new RuntimeException("TIMEOUT");
		}
	}

	private String uuid() {
		String uuid = UUID.randomUUID().toString();
		int pos = uuid.indexOf("-");
		return pos < 0 ? uuid : uuid.substring(0, pos);
	}

	@Value
	private static class UdpPlayerInfo {
		private InetAddress adressInfo;
		private Integer port;
		private String name;
	}

	public UdpServer(int port) {
		this(port, new DefaultTournament());
	}

	public UdpServer(int port, Tournament tournament) {
		this.tournament = tournament;
		try {
			socket = new DatagramSocket(port);
			System.out.println("Socket created");
		} catch (SocketException e) {
			throw new RuntimeException(e);
		}
		new Thread(() -> {
			while (true) {
				if (lessThan2Players()) {
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
					tournament.playSeason().forEach(s -> {
						Score score = s.getScore();
						Object token = s.getToken();
						// TODO find matching PlayerInfo and send score/token
						System.out.println(score + ";" + token);
					});
					// TODO accumulate score? do we still need beside influx?
				}
			}
		}).start();

	}

	private boolean lessThan2Players() {
		synchronized (players) {
			return players.size() < 2;
		}
	}

	public UdpServer startServer() {
		System.out.println("Starting server");
		while (!socket.isClosed()) {
			DatagramPacket packet = new DatagramPacket(buf, buf.length);
			try {
				socket.receive(packet);
				String received = new String(packet.getData(), 0, packet.getLength());
				// TODO we depend just on the IP not the name -> depend on IP AND name!
				dispatchCommand(packet.getAddress(), packet.getPort(), null, received);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return this;
	}

	private void dispatchCommand(InetAddress clientIp, int clientPort, UdpPlayerInfo xxxxxxx, String received)
			throws SocketException, IOException {
		if (received.startsWith("REGISTER;")) {
			String[] split = received.split(";");
			if (split.length < 2) {
				send("NO_NAME_GIVEN", clientIp, clientPort);
				return;
			}
			String playerName = split[1].trim();
			if (playerName.length() > MAX_CLIENT_NAME_LENGTH) {
				send("NAME_TOO_LONG", clientIp, clientPort);
				return;
			}
			handleRegisterCommand(findBy(ipAddressAndName(clientIp, playerName)).map(i -> {
				synchronized (players) {
					players.remove(i);
				}
				return new UdpPlayerInfo(clientIp, clientPort, playerName);
			}).orElseGet(() -> new UdpPlayerInfo(clientIp, clientPort, playerName)));
		} else if ("UNREGISTER".equals(received)) {
			findBy(ipAndPort(clientIp, clientPort)).ifPresent(i -> handleUnRegisterCommand(i));
		} else {
			findBy(ipAndPort(clientIp, clientPort)).ifPresent(i -> receiverQueues.get(i).offer(received));
		}
	}

	private Predicate<UdpPlayerInfo> ipAndPort(InetAddress clientIp, int clientPort) {
		return i -> (i.getAdressInfo().equals(clientIp) && i.getPort() == clientPort);
	}

	private Predicate<UdpPlayerInfo> ipAddressAndName(InetAddress clientIp, String name) {
		return i -> (i.getAdressInfo().equals(clientIp) && i.getName().equals(name));
	}

	private Optional<UdpPlayerInfo> findBy(Predicate<UdpPlayerInfo> p) {
		synchronized (players) {
			return players.keySet().stream().filter(p).findFirst();
		}
	}

	private void handleRegisterCommand(UdpPlayerInfo playerInfo) {
		Player player = newPlayer(playerInfo, playerInfo.getName());
		synchronized (players) {
			if (!tournament.registerPlayer(player).isOk()) {
				send("NAME_ALREADY_TAKEN", playerInfo.getAdressInfo(), playerInfo.getPort());
				return;
			}
			players.put(playerInfo, player);
			receiverQueues.put(playerInfo, new ArrayBlockingQueue<>(5));

			System.out.println(
					"Player " + playerInfo.getName() + " registered, we now have " + players.size() + " player(s)");
			send("Welcome " + playerInfo.getName(), playerInfo.getAdressInfo(), playerInfo.getPort());
			try {
				lock.lock();
				playerRegistered.signal();
			} finally {
				lock.unlock();
			}
		}

//		sendMessageToPlayer(registerResult.fold(ErrorMessage::toString, identity()), playerInfo);
	}

	private Player newPlayer(UdpPlayerInfo playerInfo, String playerName) {
		return new UdpPlayer(playerName, playerInfo);
	}

	private void handleUnRegisterCommand(UdpPlayerInfo playerInfo) {
		synchronized (players) {
			Player removed = players.remove(playerInfo);
			receiverQueues.remove(playerInfo);
			send("UNREGISTERED", playerInfo.getAdressInfo(), playerInfo.getPort());
			System.out.println(
					"Player " + removed.getToken() + " unregistered, we now have " + players.size() + " player(s)");
		}
	}

	private DatagramSocket send(String message, InetAddress clientIp, int clienPort) {
		try {
			byte[] bytes = message.getBytes();
			DatagramSocket sendSocket = new DatagramSocket();
			sendSocket.setSoTimeout((int) (TIMEOUT.toMillis() * 2));
			sendSocket.send(new DatagramPacket(bytes, bytes.length, clientIp, clienPort));
			return sendSocket;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}