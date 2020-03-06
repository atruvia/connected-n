package org.ase.fourwins.udp.server;

import static java.lang.Integer.parseInt;
import static java.util.EnumSet.allOf;
import static java.util.function.Predicate.not;

import java.util.Arrays;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.stream.Stream;

import org.ase.fourwins.annos.OnlyActivateWhenEnvSet;
import org.ase.fourwins.tournament.DefaultTournament;
import org.ase.fourwins.tournament.Tournament;
import org.ase.fourwins.tournament.listener.TournamentListener;

import lombok.RequiredArgsConstructor;

public class Main {

	private final UdpServer udpServer;

	@RequiredArgsConstructor
	private enum EnvVar {
		PORT("PORT") {
			@Override
			void setValue(UdpServer udpServer, String value) {
				udpServer.setPort(parseInt(value));
			}
		}, //
		TIMEOUT("TIMEOUT") {
			@Override
			void setValue(UdpServer udpServer, String value) {
				udpServer.setTimeoutMillis(parseInt(value));
			}
		}, //
		DELAY("DELAY") {
			@Override
			void setValue(UdpServer udpServer, String value) {
				udpServer.setDelayMillis(parseInt(value));
			}
		};

		private final String key;

		private void setValueAt(UdpServer udpServer) {
			String value = System.getenv(key);
			if (value != null) {
				setValue(udpServer, value);
			}
		}

		abstract void setValue(UdpServer udpServer, String value);

		private static void setAll(UdpServer udpServer) {
			allOf(EnvVar.class).forEach(v -> v.setValueAt(udpServer));
		}

	}

	public static void main(String[] args) {
		Main main = new Main();
		Runtime.getRuntime().addShutdownHook(new Thread(() -> stop(main)));
		EnvVar.setAll(main.udpServer);
		main.doMain(new DefaultTournament());
	}

	private static void stop(Main main) {
		main.udpServer.stop();
		System.out.flush();
		System.err.flush();
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
		return annotation == null || anyEnvIsSet(annotation.value());
	}

	private static boolean anyEnvIsSet(String[] envVars) {
		return Arrays.stream(envVars).map(System::getenv).anyMatch(not(Objects::isNull));
	}

}
