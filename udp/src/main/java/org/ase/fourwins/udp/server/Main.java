package org.ase.fourwins.udp.server;

import static java.lang.Integer.parseInt;
import static java.util.Objects.isNull;

import java.util.Arrays;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.stream.Stream;

import org.ase.fourwins.annos.OnlyActivateWhenEnvSet;
import org.ase.fourwins.tournament.DefaultTournament;
import org.ase.fourwins.tournament.Tournament;
import org.ase.fourwins.tournament.listener.TournamentListener;

public class Main {

	private UdpServer udpServer;

	public static void main(String[] args) {
		new Main().port().timeout().doMain(new DefaultTournament());
	}

	private Main port() {
		String port = System.getenv("PORT");
		if (port != null) {
			udpServer.setPort(parseInt(port));
		}
		return this;
	}

	private Main timeout() {
		String timeout = System.getenv("TIMEOUT");
		if (timeout != null) {
			udpServer.setTimeoutMillis(parseInt(timeout));
		}
		return this;
	}

	public Main() {
		udpServer = createUdpServer();
	}

	public void doMain(Tournament tournament) {
		addListeners(tournament);
		udpServer.startServer(tournament);
	}

	protected UdpServer createUdpServer() {
		return new UdpServer();
	}

	private static void addListeners(Tournament tournament) {
		loadListeners().forEach(tournament::addTournamentListener);
	}

	private static Stream<TournamentListener> loadListeners() {
		return ServiceLoader.load(TournamentListener.class).stream().filter(Main::canLoad).map(Provider::get);
	}

	private static boolean canLoad(Provider<TournamentListener> provider) {
		OnlyActivateWhenEnvSet annotation = provider.type().getAnnotation(OnlyActivateWhenEnvSet.class);
		return annotation == null || envIsSet(annotation.value());
	}

	private static boolean envIsSet(String[] envVars) {
		return Arrays.stream(envVars).map(System::getenv).anyMatch(e -> !isNull(e));
	}

}
