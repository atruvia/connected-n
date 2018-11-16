package org.ase.fourwins.tournament;

import static java.util.stream.Collectors.toMap;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.ase.fourwins.game.Player;
public class ScoreSheet extends ConcurrentHashMap<Player, Double> {

	private static final long serialVersionUID = 1L;

	public LinkedHashMap<Player, Double> getTableau() {
		return entrySet().stream().sorted(Map.Entry.comparingByValue())
				.collect(toMap(e -> e.getKey(), e -> e.getValue(),
						(e1, e2) -> e2, LinkedHashMap::new));

	}

}
