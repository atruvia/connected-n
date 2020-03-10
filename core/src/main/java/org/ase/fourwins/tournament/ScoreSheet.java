package org.ase.fourwins.tournament;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.stream.IntStream;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ScoreSheet {

	private final Map<String, Double> data = new ConcurrentHashMap<>();

	public Double merge(String player, double value,
			BiFunction<? super Double, ? super Double, ? extends Double> remappingFunction) {
		return data.merge(player, value, remappingFunction);
	}

	public Set<String> players(String player) {
		return this.data.keySet();
	}

	public Double scoreOf(String name) {
		return this.data.get(name);
	}

	@Deprecated
	public Collection<Double> values() {
		return data.values();
	}

	@Override
	public String toString() {
		String format = createFormat();
		List<Entry<String, Double>> entrySetList = data.entrySet().stream().sorted(byDescendingScore())
				.collect(toList());
		return IntStream.range(0, data.size()).mapToObj(i -> entryString(format, i, entrySetList.get(i)))
				.collect(joining("\n"));
	}

	private Comparator<Entry<String, Double>> byDescendingScore() {
		Comparator<Entry<String, Double>> comparingByValue = Map.Entry.comparingByValue();
		return comparingByValue.reversed();
	}

	private String createFormat() {
		int maxNameLen = this.data.keySet().stream().mapToInt(String::length).max().orElse(0);
		int maxPosLen = String.valueOf(this.data.keySet().size() + 1).length();
		int maxScoreLen = String.valueOf(this.data.values().stream().mapToDouble(Double::valueOf).max().orElse(0))
				.length();
		return "%" + maxPosLen + "d: %-" + maxNameLen + "s %" + maxScoreLen + ".1f";
	}

	private String entryString(String format, int pos, Entry<String, Double> entry) {
		return String.format(format, pos + 1, entry.getKey(), entry.getValue());
	}

}
