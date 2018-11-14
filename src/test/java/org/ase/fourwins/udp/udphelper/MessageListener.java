package org.ase.fourwins.udp.udphelper;

public interface MessageListener {

	void onMessage(String message);

	default void onStop() {
	}

}
