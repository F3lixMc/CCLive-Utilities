package net.felix.utilities.Factory.essencefarm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Live observations of one wave: the compositions of the last runs and the average cycle time.
 * The composition used for planning is the per-type median of the recent runs, so a single bad run
 * (missed spawns, or spawns of several waves counted together) cannot dominate.
 */
public final class WaveObservation {

	/** Runs kept per wave. */
	static final int KEPT_RUNS = 5;
	/** A run with more mobs than this is certainly several waves counted together. */
	static final int MAX_MOBS_PER_RUN = 2000;
	/** A count more than this factor above the known static count is rejected. */
	static final double MAX_FACTOR_OVER_STATIC = 3.0;

	public List<Map<String, Integer>> recent = new ArrayList<>();
	public int cycles;
	public long avgCycleMs;

	/**
	 * Adds a run composition unless it is implausible.
	 *
	 * @param staticCounts counts known from the wiki table for this wave, or null
	 * @return true if the run was kept
	 */
	public boolean addRun(Map<String, Integer> counts, Map<String, Integer> staticCounts) {
		if (counts == null || counts.isEmpty()) {
			return false;
		}
		int total = 0;
		for (int count : counts.values()) {
			total += count;
		}
		if (total > MAX_MOBS_PER_RUN) {
			return false;
		}
		if (staticCounts != null) {
			for (Map.Entry<String, Integer> known : staticCounts.entrySet()) {
				Integer seen = counts.get(known.getKey());
				if (seen != null && seen > known.getValue() * MAX_FACTOR_OVER_STATIC) {
					return false;
				}
			}
		}
		recent.add(new TreeMap<>(counts));
		while (recent.size() > KEPT_RUNS) {
			recent.remove(0);
		}
		return true;
	}

	public void addCycle(long cycleMs) {
		if (cycleMs <= 0) {
			return;
		}
		avgCycleMs = (avgCycleMs * cycles + cycleMs) / (cycles + 1);
		cycles++;
	}

	/** Per-type median over the recent runs (missing type in a run counts as 0). */
	public Map<String, Integer> medianCounts() {
		Map<String, Integer> result = new TreeMap<>();
		if (recent.isEmpty()) {
			return result;
		}
		TreeSet<String> types = new TreeSet<>();
		for (Map<String, Integer> run : recent) {
			types.addAll(run.keySet());
		}
		for (String type : types) {
			List<Integer> values = new ArrayList<>();
			for (Map<String, Integer> run : recent) {
				values.add(run.getOrDefault(type, 0));
			}
			Collections.sort(values);
			int median = values.get(values.size() / 2);
			if (median > 0) {
				result.put(type, median);
			}
		}
		return result;
	}

	public int medianMobs() {
		int total = 0;
		for (int count : medianCounts().values()) {
			total += count;
		}
		return total;
	}
}
