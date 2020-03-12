package org.ase.fourwins.listener;

import static java.util.Map.Entry.comparingByValue;
import static java.util.stream.Collectors.joining;

import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.ase.fourwins.annos.OnlyActivateWhenEnvSet;
import org.ase.fourwins.board.Board.GameState;
import org.ase.fourwins.board.Board.Score;
import org.ase.fourwins.game.Game;
import org.ase.fourwins.game.Player;
import org.ase.fourwins.tournament.ScoreSheet;
import org.ase.fourwins.tournament.listener.TournamentListener;

import lombok.Getter;

@OnlyActivateWhenEnvSet("WITH_SYSOUT")
public class SysoutTournamentListener implements TournamentListener {

	private final Map<Object, Integer> gamesWon = new ConcurrentHashMap<>();

	private static final double FULL_POINT = 1;
	private static final double ZERO = 0.0;
	private static final double HALF_POINT = 0.5;

	@Getter
	private final ScoreSheet scoreSheet = new ScoreSheet();

	@Override
	public void seasonStarted() {
		System.out.println("Season starting");
	}

	@Override
	public void gameEnded(Game game) {
		winners(game).forEach(w -> gamesWon.merge(w, 1, Integer::sum));
		updateScoreSheet(game);
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

	private void updateScoreSheet(Game game) {
		Score score = game.gameState().getScore();
		Object lastToken = game.gameState().getToken();
		switch (score) {
		case WIN:
			addPointForPlayer(game.getPlayerForToken(lastToken), FULL_POINT);
			game.getOpponentsForToken(lastToken).forEach(p -> addPointForPlayer(p, ZERO));
			break;
		case LOSE:
			addPointForPlayer(game.getPlayerForToken(lastToken), ZERO);
			game.getOpponentsForToken(lastToken).forEach(p -> addPointForPlayer(p, FULL_POINT));
			break;
		case DRAW:
			game.getPlayers().stream().forEach(p -> addPointForPlayer(p, HALF_POINT));
			break;
		default:
			break;
		}
	}

	private void addPointForPlayer(Player player, double value) {
		scoreSheet.increaseScore(player.getToken(), value);
	}

	@Override
	public void seasonEnded() {
		Comparator<Entry<Object, Integer>> comparingByValue = comparingByValue();
		String collect = gamesWon.entrySet().stream() //
				.sorted(comparingByValue.reversed()) //
				.map(e -> e.getKey() + "=" + e.getValue()).collect(joining(", "));
		System.out.println("Season ended, games won: " + collect);
		System.out.println(scoreSheet);
		gamesWon.clear();
	}

}
