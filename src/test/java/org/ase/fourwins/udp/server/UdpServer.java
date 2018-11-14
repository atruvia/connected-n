package org.ase.fourwins.udp.server;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.joining;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.ase.fourwins.board.Board.Score;
import org.ase.fourwins.board.BoardInfo;
import org.ase.fourwins.game.Player;
import org.ase.fourwins.tournament.DefaultTournament;
import org.ase.fourwins.tournament.Tournament;

import lombok.Value;

public class UdpServer {

	public static final int MAX_CLIENT_NAME_LENGTH = 30;
//	private static final int SOCKET_TIMEOUT = 250;
	private static final int SOCKET_TIMEOUT = (int) TimeUnit.SECONDS.toMillis(2);

	private final Tournament tournament;
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
			try {
				return Integer.parseInt(sendAndWait("YOURTURN", playerInfo));
			} catch (IOException e) {
				throw new RuntimeException(e);
			} catch (InterruptedException e) {
				throw new IllegalStateException("TIMEOUT");
			}
		}

		@Override
		public boolean joinGame(final String opposite, final BoardInfo boardInfo) {
			// sendAndReceive("JOIN", playerInfo);
			// JOIN Game (not season)
			// TODO send JOIN to the client an wait for JOINING
			return super.joinGame(opposite, boardInfo);
		}
	}

	private String sendAndWait(String command, UdpPlayerInfo playerInfo)
			throws SocketException, IOException, InterruptedException {
		String delimiter = ";";
		String uuid = uuid();
		send(command + delimiter + uuid, playerInfo.getAdressInfo(), playerInfo.getPort());
		String response = receiverQueues.get(playerInfo).poll(SOCKET_TIMEOUT, MILLISECONDS);
		String[] splitted = response.split(delimiter);
		if (splitted.length < 2) {
			throw new IllegalArgumentException("Cannot handle/parse " + response);
		} else if (!splitted[splitted.length - 1].equals(uuid)) {
			throw new IllegalStateException(
					"UUID mismatch, expected " + uuid + " got " + splitted[splitted.length - 1]);
		}
		return Arrays.stream(splitted).limit(splitted.length - 1).collect(joining(delimiter));
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
	}

	public UdpServer(final int port) {
		this(port, new DefaultTournament());
	}

	public UdpServer(final int port, final Tournament tournament) {
		this.tournament = tournament;
		try {
			socket = new DatagramSocket(port);
			System.out.println("Socket created");
		} catch (final SocketException e) {
			throw new RuntimeException(e);
		}
		new Thread(() -> {
			while (true) {
				if (lessThan2Players()) {
					try {
						lock.lock();
						playerRegistered.await(5, SECONDS);
					} catch (final InterruptedException e) {
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
			final DatagramPacket packet = new DatagramPacket(buf, buf.length);
			try {
				socket.receive(packet);
				String received = new String(packet.getData(), 0, packet.getLength());
				// TODO we depend just on the IP not the name -> depend on IP AND name!
				dispatchCommand(new UdpPlayerInfo(packet.getAddress(), packet.getPort()), received);
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
		return this;
	}

	private void dispatchCommand(final UdpPlayerInfo playerInfo, final String received)
			throws SocketException, IOException {
		if (received.startsWith("REGISTER;")) {
			handleRegisterCommand(playerInfo, received);
		} else if ("UNREGISTER".equals(received)) {
			handleUnRegisterCommand(playerInfo);
		} else {
			receiverQueues.get(playerInfo).offer(received);
		}
	}

	private void handleRegisterCommand(final UdpPlayerInfo playerInfo, final String received)
			throws SocketException, IOException {
		String[] split = received.split(";");
		if (split.length < 2) {
			send("NO_NAME_GIVEN", playerInfo.getAdressInfo(), playerInfo.getPort());
			return;
		}
		String playerName = split[1].trim();
		if (playerName.length() > MAX_CLIENT_NAME_LENGTH) {
			send("NAME_TOO_LONG", playerInfo.getAdressInfo(), playerInfo.getPort());
			return;
		}

		Player player = newPlayer(playerInfo, playerName);
		synchronized (players) {
			if (!tournament.registerPlayer(player).isOk()) {
				send("NAME_ALREADY_TAKEN", playerInfo.getAdressInfo(), playerInfo.getPort());
				return;
			}
			players.put(playerInfo, player);
			receiverQueues.put(playerInfo, new ArrayBlockingQueue<>(5));

			System.out.println("Player " + playerName + " registered, we now have " + players.size() + " player(s)");
			send("Welcome " + playerName, playerInfo.getAdressInfo(), playerInfo.getPort());
			try {
				lock.lock();
				playerRegistered.signal();
			} finally {
				lock.unlock();
			}
		}

//		sendMessageToPlayer(registerResult.fold(ErrorMessage::toString, identity()), playerInfo);
	}

	private Player newPlayer(final UdpPlayerInfo playerInfo, String playerName) {
		return new UdpPlayer(playerName, playerInfo);
	}

	private void handleUnRegisterCommand(final UdpPlayerInfo playerInfo) throws SocketException, IOException {
		synchronized (players) {
			Player removed = players.remove(playerInfo);
			receiverQueues.remove(playerInfo);
			send("UNREGISTERED", playerInfo.getAdressInfo(), playerInfo.getPort());
			System.out.println(
					"Player " + removed.getToken() + " unregistered, we now have " + players.size() + " player(s)");
		}
	}

	private String sendAndReceive(String message, InetAddress clientIp, int clientPort)
			throws SocketException, IOException {
		try (DatagramSocket sendSocket = send(message, clientIp, clientPort)) {
			DatagramPacket packet = new DatagramPacket(buf, buf.length);
			sendSocket.receive(packet);
			String string = new String(packet.getData(), 0, packet.getLength());
			System.out.println("+++" + string);
			return string;
		}
	}

	private DatagramSocket send(String message, InetAddress clientIp, int clienPort)
			throws SocketException, IOException {
		byte[] bytes = message.getBytes();
		DatagramSocket sendSocket = new DatagramSocket();
		sendSocket.setSoTimeout(SOCKET_TIMEOUT);
		sendSocket.send(new DatagramPacket(bytes, bytes.length, clientIp, clienPort));
		return sendSocket;
	}

}
