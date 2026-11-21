package net.felix.utilities.Factory.essencefarm;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Picks waves that deliver all required essences with as few kills as possible.
 *
 * <p>Model: a mob drops on average {@code multiplier} essences of its tier in that wave.
 * A wave may serve an essence only if it costs at most {@code factor} times the kills per essence
 * of that essence's best wave (same rule as the wiki analysis).
 * Greedy weighted set cover: each round takes the wave with the best ratio of ideal kills covered
 * to real kills spent, then subtracts its yield from every essence.
 */
public final class EssenceFarmPlanner {

	public static final double DEFAULT_FACTOR = 5.0;

	/** Required amount of one essence (mob type and tier). */
	public record Need(String type, int tier, long amount) {
		String key() {
			return type + "#" + tier;
		}
	}

	/** Known composition of a wave. {@code mobs} = all mobs per run. */
	public record WaveData(int wave, int mobs, Map<String, Integer> counts) {
	}

	/** An essence delivered by a plan step. */
	public record Covered(String type, int tier, long amount, double perRun) {
	}

	/** A plan step: play this wave {@code runs} times. */
	public record Step(int wave, long runs, int mobs, List<Covered> covered) {
		public long kills() {
			return runs * mobs;
		}
	}

	/** Result: steps plus essences that no known wave delivers. */
	public record Plan(List<Step> steps, List<Need> unreachable) {
	}

	private EssenceFarmPlanner() {
	}

	public static Plan plan(List<Need> needs, List<WaveData> waves, EssenceTierTable tiers,
			double multiplier, double factor) {
		Map<String, Need> needByKey = new LinkedHashMap<>();
		Map<String, Double> remaining = new LinkedHashMap<>();
		for (Need need : needs) {
			if (need.amount() <= 0) {
				continue;
			}
			needByKey.put(need.key(), need);
			remaining.merge(need.key(), (double) need.amount(), Double::sum);
		}
		double mult = multiplier > 0 ? multiplier : 1.0;

		// Essences per run for each wave: key -> amount
		Map<WaveData, Map<String, Double>> yields = new LinkedHashMap<>();
		Map<String, Double> bestKillsPerEssence = new LinkedHashMap<>();
		for (WaveData wave : waves) {
			if (wave.mobs() <= 0) {
				continue;
			}
			Map<String, Double> perRun = new LinkedHashMap<>();
			for (Map.Entry<String, Integer> entry : wave.counts().entrySet()) {
				int tier = tiers.tierAt(entry.getKey(), wave.wave());
				if (tier <= 0 || entry.getValue() == null || entry.getValue() <= 0) {
					continue;
				}
				String key = entry.getKey() + "#" + tier;
				if (!needByKey.containsKey(key)) {
					continue;
				}
				double amount = entry.getValue() * mult;
				perRun.merge(key, amount, Double::sum);
			}
			if (perRun.isEmpty()) {
				continue;
			}
			yields.put(wave, perRun);
			for (Map.Entry<String, Double> entry : perRun.entrySet()) {
				double killsPerEssence = wave.mobs() / entry.getValue();
				bestKillsPerEssence.merge(entry.getKey(), killsPerEssence, Math::min);
			}
		}

		List<Need> unreachable = new ArrayList<>();
		for (Map.Entry<String, Need> entry : needByKey.entrySet()) {
			if (!bestKillsPerEssence.containsKey(entry.getKey())) {
				unreachable.add(entry.getValue());
				remaining.remove(entry.getKey());
			}
		}

		Map<Integer, Step> stepsByWave = new LinkedHashMap<>();
		while (!remaining.isEmpty()) {
			WaveData bestWave = null;
			long bestRuns = 0;
			double bestRatio = -1;
			for (Map.Entry<WaveData, Map<String, Double>> entry : yields.entrySet()) {
				WaveData wave = entry.getKey();
				Map<String, Double> perRun = entry.getValue();
				List<Long> candidateRuns = new ArrayList<>();
				for (Map.Entry<String, Double> yield : perRun.entrySet()) {
					Double rest = remaining.get(yield.getKey());
					if (rest == null || rest <= 0) {
						continue;
					}
					double killsPerEssence = wave.mobs() / yield.getValue();
					if (killsPerEssence > factor * bestKillsPerEssence.get(yield.getKey()) + 1e-9) {
						continue;
					}
					candidateRuns.add(runsFor(rest, yield.getValue()));
				}
				for (long runs : candidateRuns) {
					double value = 0;
					for (Map.Entry<String, Double> yield : perRun.entrySet()) {
						Double rest = remaining.get(yield.getKey());
						if (rest == null || rest <= 0) {
							continue;
						}
						double gained = Math.min(rest, runs * yield.getValue());
						value += gained * bestKillsPerEssence.get(yield.getKey());
					}
					double ratio = value / ((double) runs * wave.mobs());
					if (ratio > bestRatio + 1e-12
							|| (Math.abs(ratio - bestRatio) <= 1e-12 && bestWave != null && wave.wave() < bestWave.wave())) {
						bestRatio = ratio;
						bestWave = wave;
						bestRuns = runs;
					}
				}
			}
			if (bestWave == null) {
				break;
			}
			Map<String, Double> perRun = yields.get(bestWave);
			for (Map.Entry<String, Double> yield : perRun.entrySet()) {
				Double rest = remaining.get(yield.getKey());
				if (rest == null) {
					continue;
				}
				double left = rest - bestRuns * yield.getValue();
				if (left <= 1e-9) {
					remaining.remove(yield.getKey());
				} else {
					remaining.put(yield.getKey(), left);
				}
			}
			Step previous = stepsByWave.get(bestWave.wave());
			long totalRuns = bestRuns + (previous != null ? previous.runs() : 0);
			stepsByWave.put(bestWave.wave(), new Step(bestWave.wave(), totalRuns, bestWave.mobs(), List.of()));
		}

		// Essences listed per step: every required essence the wave drops at all
		List<Step> steps = new ArrayList<>();
		for (Step step : stepsByWave.values()) {
			WaveData wave = findWave(yields, step.wave());
			List<Covered> covered = new ArrayList<>();
			for (Map.Entry<String, Double> yield : yields.get(wave).entrySet()) {
				Need need = needByKey.get(yield.getKey());
				covered.add(new Covered(need.type(), need.tier(), need.amount(), yield.getValue()));
			}
			steps.add(new Step(step.wave(), step.runs(), step.mobs(), covered));
		}
		steps.sort(Comparator.comparingInt(Step::wave));
		return new Plan(steps, unreachable);
	}

	private static long runsFor(double rest, double perRun) {
		return Math.max(1L, (long) Math.ceil(rest / perRun - 1e-9));
	}

	private static WaveData findWave(Map<WaveData, Map<String, Double>> yields, int wave) {
		for (WaveData data : yields.keySet()) {
			if (data.wave() == wave) {
				return data;
			}
		}
		throw new IllegalStateException("Missing wave: " + wave);
	}
}
