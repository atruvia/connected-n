package org.ase.fourwins.tournament;

import static java.util.stream.Collectors.toMap;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ScoreSheet {

	private final ConcurrentHashMap<String, Double> data = new ConcurrentHashMap<>();

	@Deprecated
	public LinkedHashMap<String, Double> getTableau() {
		return data.entrySet().stream().sorted(Map.Entry.comparingByValue())
				.collect(toMap(e -> e.getKey(), e -> e.getValue(),
						(e1, e2) -> e2, LinkedHashMap::new));
	}

	public Double merge(String player, double value,
			BiFunction<? super Double, ? super Double, ? extends Double> remappingFunction) {
		return data.merge(player, value, remappingFunction);
	}

	public Set<String> players(String player) {
		return getTableau().keySet();
	}

	public Double scoreOf(String name) {
		return getTableau().get(name);
	}

	@Deprecated
	public Collection<Double> values() {
		return data.values();
	}

	@Override
	public String toString() {
		List<Entry<String, Double>> entrySetList = getTableau().entrySet()
				.stream().collect(Collectors.toList());
		return IntStream.range(1, data.size() + 1)
				.mapToObj(i -> i + ": " + entrySetList.get(i - 1).getKey()
						+ "         " + entrySetList.get(i - 1).getValue())
				.collect(Collectors.joining("\n"));
	}

}
