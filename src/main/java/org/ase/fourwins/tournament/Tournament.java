package org.ase.fourwins.tournament;

import java.util.Collection;
import java.util.function.Consumer;

import org.ase.fourwins.board.Board.GameState;
import org.ase.fourwins.game.Player;
import org.ase.fourwins.tournament.listener.TournamentListener;

public interface Tournament {

	void addTournamentListener(TournamentListener listener);

	void removeTournamentListener(TournamentListener listener);

	void playSeason(Collection<? extends Player> players, Consumer<GameState> consumer);

}