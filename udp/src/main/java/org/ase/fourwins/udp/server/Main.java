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

import lombok.Setter;

public class Main {

	@Setter
	protected Tournament tournament = new DefaultTournament();
	@Setter
	protected int port = 4446;

	public static void main(String[] args) {
		Main main = new Main();
		if (args.length > 0) {
			main.setPort(parseInt(args[1]));
		}
		main.doMain();
	}

	public void doMain() {
		addListeners(tournament);
		createUdpServer(tournament).startServer();
	}

	protected UdpServer createUdpServer(Tournament tournament) {
		return new UdpServer(port, tournament);
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
