package org.ase.fourwins.listener;

import org.ase.fourwins.annos.OnlyActivateWhenEnvSet;
import org.ase.fourwins.board.Board.Score;
import org.ase.fourwins.game.Game;
import org.ase.fourwins.game.Player;
import org.ase.fourwins.tournament.ScoreSheet;
import org.ase.fourwins.tournament.listener.TournamentListener;

import lombok.Getter;

@OnlyActivateWhenEnvSet("WITH_SYSOUT")
public class TournamentScoreListener implements TournamentListener {

	private static final double FULL_POINT = 1;
	private static final double ZERO = 0.0;
	private static final double HALF_POINT = 0.5;

	@Getter
	private final ScoreSheet scoreSheet = new ScoreSheet();

	@Override
	public void gameEnded(Game game) {
		updateScoreSheet(game);
	}

	private void updateScoreSheet(Game game) {
		Score score = game.gameState().getScore();
		switch (score) {
		case WIN:
			updateScores(game, FULL_POINT, ZERO);
			break;
		case LOSE:
			updateScores(game, ZERO, FULL_POINT);
			break;
		case DRAW:
			updateScores(game, HALF_POINT, HALF_POINT);
			break;
		default:
			break;
		}
	}

	private void updateScores(Game game, double tokenOwner, double others) {
		Object lastToken = game.gameState().getToken();
		addPointForPlayer(game.getPlayerForToken(lastToken), tokenOwner);
		game.getOpponentsForToken(lastToken).forEach(p -> addPointForPlayer(p, others));
	}

	private void addPointForPlayer(Player player, double value) {
		scoreSheet.increaseScore(player.getToken(), value);
	}

	@Override
	public void seasonEnded() {
		System.out.println(scoreSheet);
	}

}
