package org.ase.fourwins.udp.udphelper;

import java.io.IOException;

public interface MessageSender {

	void send(String message) throws IOException;

}
