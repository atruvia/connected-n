package org.ase.fourwins.udp.udphelper;

import static java.nio.channels.SelectionKey.OP_READ;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

public class UdpCommunicator {

	private static final Charset utf8 = Charset.forName("UTF-8");

	private final String remoteHost;
	private final int remotePort;

	private final Collection<MessageListener> listeners = new CopyOnWriteArrayList<>();
	private final DatagramChannel channel;

	private volatile boolean shouldStop;

	/**
	 * Constructs a UdpCommunicator and sets it up to communicate with a remote
	 * server.
	 * 
	 * @param remoteHost the host name or IP to send messages to
	 * @param remotePort the port on the remote host to send messages to
	 */
	public UdpCommunicator(String remoteHost, int remotePort) throws IOException {
		this.remoteHost = remoteHost;
		this.remotePort = remotePort;
		channel = DatagramChannel.open();
		channel.socket().bind(null);
		channel.configureBlocking(false);
	}

	/**
	 * Get the local port on which this UdpCommunicator listens for messages.
	 * 
	 * @return local port
	 */
	public int getLocalPort() {
		return channel.socket().getLocalPort();
	}

	public MessageSender getMessageSender() {
		return new MessageSender() {
			public void send(String message) throws IOException {
				InetSocketAddress destination = new InetSocketAddress(remoteHost, remotePort);
				channel.send(utf8.encode(message), destination);
			}
		};
	}

	/**
	 * Makes this UdpCommunicator listen for incoming messages.
	 * 
	 * <p>
	 * This will <b>not</b> return, but it will notify all registered
	 * {@link MessageListener}s on every incoming message (see
	 * {@link UdpCommunicator#addMessageListener(MessageListener)}).
	 * </p>
	 * 
	 * <p>
	 * The notifications will happen on the same thread which listens for data.
	 * Therefore, don't do any lengthy calculations on that thread, as you won't
	 * receive further message notifications in the mean time!
	 * </p>
	 */
	public void listenForMessages() throws IOException {
		Selector selector = Selector.open();
		SelectionKey selectionKey = channel.register(selector, OP_READ);

		while (true) {
			if (shouldStop) {
				break;
			}

			if (selector.select(SECONDS.toMillis(1)) > 0) {
				selector.selectedKeys().remove(selectionKey);
				if (selectionKey.isReadable()) {
					readIncomingMessage();
				}
			}
		}

	}

	public void stop() {
		shouldStop = true;
		listeners.forEach(MessageListener::onStop);
	}

	public void addMessageListener(MessageListener listener) {
		listeners.add(listener);
	}

	public void removeMessageListener(MessageListener listener) {
		listeners.remove(listener);
	}

	private void readIncomingMessage() throws IOException {
		String message = readMessageFromChannel();
		listeners.forEach(l -> l.onMessage(message));
	}

	private String readMessageFromChannel() throws IOException {
		ByteBuffer bytes = ByteBuffer.allocateDirect(1024);
		channel.receive(bytes);
		bytes.flip();
		return utf8.decode(bytes).toString();
	}

}