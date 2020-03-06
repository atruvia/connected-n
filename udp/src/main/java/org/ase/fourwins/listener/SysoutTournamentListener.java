package org.ase.fourwins.listener;

import static java.util.Comparator.comparing;
import static java.util.Map.Entry.comparingByValue;
import static java.util.stream.Collectors.joining;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.ase.fourwins.annos.OnlyActivateWhenEnvSet;
import org.ase.fourwins.board.Board.GameState;
import org.ase.fourwins.game.Game;
import org.ase.fourwins.game.Player;
import org.ase.fourwins.tournament.listener.TournamentListener;

@OnlyActivateWhenEnvSet("WITH_SYSOUT")
public class SysoutTournamentListener implements TournamentListener {

	private final ConcurrentMap<Object, AtomicInteger> gamesWon = new ConcurrentHashMap<>();

	@Override
	public void seasonStarted() {
		System.out.println("Season starting");
	}

	@Override
	public void gameEnded(Game game) {
		winners(game).map(w -> gamesWon.computeIfAbsent(w, n -> new AtomicInteger())).forEach(c -> c.incrementAndGet());
	}

	private Stream<Object> winners(Game game) {
		GameState gameState = game.gameState();
		Object token = gameState.getToken();
		switch (gameState.getScore()) {
		case WIN:
			return Stream.of(token);
		case LOSE:
			return game.getOpponentsForToken(token).map(Player::getToken);
		default:
			return Stream.empty();
		}
	}

	@Override
	public void seasonEnded() {
		String collect = gamesWon.entrySet().stream() //
				.sorted(comparingByValue(comparing(AtomicInteger::get)).reversed()) //
				.map(e -> e.getKey() + "=" + e.getValue()).collect(joining(", "));
		System.out.println("Season ended, games won: " + collect);
		gamesWon.clear();
	}

}
