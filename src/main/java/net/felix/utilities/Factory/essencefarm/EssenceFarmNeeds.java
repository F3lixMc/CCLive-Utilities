package net.felix.utilities.Factory.essencefarm;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.function.ToLongFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns selected power crystal levels into the essences still missing.
 */
public final class EssenceFarmNeeds {

	private static final Pattern ESSENCE_PATTERN = Pattern.compile("^(.+?)\\s+T(\\d+)$");

	/** Levels to plan for one crystal: from {@code current} (exclusive) up to {@code target} (inclusive). */
	public record LevelRange(int current, int target) {
	}

	/** Essence cost of one level, as in MKLevel.json (e.g. "Pferd T1", 3000). */
	public record LevelCost(String essence, long amount) {
	}

	/** A required essence with total cost and current stock. */
	public record Requirement(String type, int tier, long total, long stock) {
		public long missing() {
			return Math.max(0L, total - stock);
		}
	}

	/** Requirements plus essence names that are not in the tier table (e.g. boss-only essences). */
	public record Result(List<Requirement> requirements, Set<String> unknownEssences) {
		public List<EssenceFarmPlanner.Need> needs() {
			List<EssenceFarmPlanner.Need> needs = new ArrayList<>();
			for (Requirement req : requirements) {
				needs.add(new EssenceFarmPlanner.Need(req.type(), req.tier(), req.missing()));
			}
			return needs;
		}
	}

	private EssenceFarmNeeds() {
	}

	/**
	 * @param ranges      level range of every planned crystal
	 * @param costOfLevel cost to reach a level; null if unknown
	 * @param stockOf       owned amount for a bag key ("Pferde [Essenz] Tier 1")
	 */
	public static Result compute(List<LevelRange> ranges, IntFunction<LevelCost> costOfLevel,
			EssenceTierTable tiers, ToLongFunction<String> stockOf) {
		Map<String, long[]> totals = new LinkedHashMap<>();
		Map<String, String[]> keyParts = new LinkedHashMap<>();
		Set<String> unknown = new LinkedHashSet<>();
		for (LevelRange range : ranges) {
			for (int level = range.current() + 1; level <= range.target(); level++) {
				LevelCost cost = costOfLevel.apply(level);
				if (cost == null || cost.essence() == null) {
					continue;
				}
				Matcher matcher = ESSENCE_PATTERN.matcher(cost.essence().trim());
				String type = matcher.matches() ? tiers.typeForName(matcher.group(1)) : null;
				if (type == null) {
					unknown.add(cost.essence().trim());
					continue;
				}
				String tier = matcher.group(2);
				String key = type + "#" + tier;
				totals.computeIfAbsent(key, k -> new long[1])[0] += cost.amount();
				keyParts.putIfAbsent(key, new String[] {type, tier});
			}
		}
		List<Requirement> requirements = new ArrayList<>();
		for (Map.Entry<String, long[]> entry : totals.entrySet()) {
			String[] parts = keyParts.get(entry.getKey());
			int tier = Integer.parseInt(parts[1]);
			String bagKey = tiers.bagKey(parts[0], tier);
			long stock = bagKey != null ? Math.max(0L, stockOf.applyAsLong(bagKey)) : 0L;
			requirements.add(new Requirement(parts[0], tier, entry.getValue()[0], stock));
		}
		return new Result(requirements, unknown);
	}

	/** Short German duration like "1h 20m" or "45m"; empty for unknown. */
	public static String formatDuration(long ms) {
		if (ms <= 0) {
			return "";
		}
		long minutes = Math.max(1L, Math.round(ms / 60000.0));
		long hours = minutes / 60;
		long rest = minutes % 60;
		if (hours >= 24) {
			return (hours / 24) + "d " + (hours % 24) + "h";
		}
		if (hours > 0) {
			return hours + "h " + rest + "m";
		}
		return rest + "m";
	}
}
