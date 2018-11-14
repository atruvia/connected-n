package org.ase.fourwins.udp.server;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

import org.ase.fourwins.board.Board.GameState;
import org.ase.fourwins.board.BoardInfo;
import org.ase.fourwins.game.Player;
import org.ase.fourwins.tournament.DefaultTournament;
import org.ase.fourwins.tournament.Tournament;
import org.ase.fourwins.udo.server.UdpServer;

import lombok.Value;

public class UdpServer {

	public static final int MAX_CLIENT_NAME_LENGTH = 30;
	private static final int SOCKET_TIMEOUT = 250;

	private final Tournament tournament;
	private final Map<UdpPlayerInfo, Player> players = new HashMap<>();

	private final DatagramSocket socket;
	private final byte[] buf = new byte[1024];

	private final Lock lock = new ReentrantLock();
	private final Condition playerRegistered = lock.newCondition();

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
					final Stream<GameState> results = tournament.playSeason();
					// TODO accumulate
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
				System.out.println("Server received: " + received);
				// TODO we depend on the IP not the name
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
			synchronized (players) {
				Player removed = players.remove(playerInfo);
				send("UNREGISTERED", playerInfo);
				System.out.println(
						"Player " + removed.getToken() + " unregistered, we now have " + players.size() + " player(s)");
			}
		}
	}

	private void handleRegisterCommand(final UdpPlayerInfo playerInfo, final String received)
			throws SocketException, IOException {
		String[] split = received.split(";");
		if (split.length < 2) {
			send("NO_NAME_GIVEN", playerInfo);
			return;
		}
		String playerName = split[1].trim();
		if (playerName.length() > MAX_CLIENT_NAME_LENGTH) {
			send("NAME_TOO_LONG", playerInfo);
			return;
		}

		Player player = newPlayer(playerName);
		synchronized (players) {
			if (!tournament.registerPlayer(player).isOk()) {
				send("NAME_ALREADY_TAKEN", playerInfo);
				return;
			}
			players.put(playerInfo, player);
			System.out.println("Player " + playerName + " registered, we now have " + players.size() + " player(s)");
			send("Welcome " + playerName, playerInfo);
			try {
				lock.lock();
				playerRegistered.signal();
			} finally {
				lock.unlock();
			}
		}

//		sendMessageToPlayer(registerResult.fold(ErrorMessage::toString, identity()), playerInfo);
	}

	private Player newPlayer(String playerName) {
		final Player player = new Player(playerName) {
			@Override
			protected int nextColumn() {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public boolean joinGame(final String opposite, final BoardInfo boardInfo) {
//				sendAndReceive("JOIN", playerInfo);
				// JOIN Game (not season)
				// TODO send JOIN to the client an wait for JOINING
				return super.joinGame(opposite, boardInfo);
			}
		};
		return player;
	}

	private String sendAndReceive(String message, UdpPlayerInfo playerInfo) throws SocketException, IOException {
		try (DatagramSocket sendSocket = send(message, playerInfo)) {
			DatagramPacket packet = new DatagramPacket(buf, buf.length);
			sendSocket.receive(packet);
			return new String(packet.getData(), 0, packet.getLength());
		}
	}

	private DatagramSocket send(String message, UdpPlayerInfo playerInfo) throws SocketException, IOException {
		byte[] bytes = message.getBytes();
		DatagramSocket sendSocket = new DatagramSocket();
		sendSocket.setSoTimeout(SOCKET_TIMEOUT);
		sendSocket.send(new DatagramPacket(bytes, bytes.length, playerInfo.getAdressInfo(), playerInfo.getPort()));
		return sendSocket;
	}

}
