package org.ase.fourwins.tournament;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

public class ScoreSheet {

	private final Map<String, Double> data = new ConcurrentHashMap<>();

	public void increaseScore(String player, double add) {
		data.merge(player, add, Double::sum);
	}

	public Double scoreOf(String name) {
		return data.get(name);
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
		int maxNameLen = data.keySet().stream().mapToInt(String::length).max().orElse(0);
		int maxPosLen = String.valueOf(data.keySet().size() + 1).length();
		int maxScoreLen = String.valueOf(data.values().stream().mapToDouble(Double::valueOf).max().orElse(0)).length();
		return "%" + maxPosLen + "d: %-" + maxNameLen + "s %" + maxScoreLen + ".1f";
	}

	private String entryString(String format, int pos, Entry<String, Double> entry) {
		return String.format(format, pos + 1, entry.getKey(), entry.getValue());
	}

}
