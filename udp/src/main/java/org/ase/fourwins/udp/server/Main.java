package org.ase.fourwins.udp.server;

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

	public static void main(String[] args) {
		new UdpServer(4446, addListeners(new DefaultTournament())).startServer();
	}

	private static Tournament addListeners(Tournament tournament) {
		loadListeners().forEach(tournament::addTournamentListener);
		return tournament;
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
