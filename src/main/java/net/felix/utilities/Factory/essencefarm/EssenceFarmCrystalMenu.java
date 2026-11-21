package net.felix.utilities.Factory.essencefarm;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses crystal items in the "Machtkristalle Verbessern" menu:
 * <pre>
 * [Machtkristall der Opferung]
 * Level 57 [|||||] Level 58
 * ...
 * Kosten:
 *  - Vindicator [Essenz] Tier 3: (46894/59000)
 * </pre>
 */
public final class EssenceFarmCrystalMenu {

	public static final String TITLE = "Machtkristalle Verbessern";

	private static final Pattern NAME = Pattern.compile("Machtkristall (?:der|des) ([^\\]]+)");
	private static final Pattern LEVEL = Pattern.compile("^Level\\s+(\\d+)");
	private static final Pattern ESSENCE_COST = Pattern.compile(
			"([^\\s\\-].*?\\[Essenz\\]\\s*Tier\\s*\\d+)\\s*:\\s*\\(\\s*([\\d.,]+)\\s*/\\s*([\\d.,]+)\\s*\\)");

	/** One crystal: level and, if the next level costs essences, the owned and required amount. */
	public record CrystalInfo(String name, int level, String essenceKey, long owned, long required) {
	}

	private EssenceFarmCrystalMenu() {
	}

	/** @param lines tooltip lines without formatting codes, item name first */
	public static Optional<CrystalInfo> parse(List<String> lines) {
		if (lines == null || lines.isEmpty()) {
			return Optional.empty();
		}
		Matcher name = NAME.matcher(lines.get(0));
		if (!name.find()) {
			return Optional.empty();
		}
		int level = -1;
		String essenceKey = null;
		long owned = -1;
		long required = -1;
		for (int i = 1; i < lines.size(); i++) {
			String line = lines.get(i).trim();
			Matcher levelMatcher = LEVEL.matcher(line);
			if (level < 0 && levelMatcher.find()) {
				level = Integer.parseInt(levelMatcher.group(1));
				continue;
			}
			Matcher cost = ESSENCE_COST.matcher(line);
			if (essenceKey == null && cost.find()) {
				essenceKey = cost.group(1).replaceAll("\\s+", " ").trim();
				owned = parseAmount(cost.group(2));
				required = parseAmount(cost.group(3));
			}
		}
		if (level <= 0) {
			return Optional.empty();
		}
		return Optional.of(new CrystalInfo(name.group(1).trim(), level, essenceKey, owned, required));
	}

	private static long parseAmount(String text) {
		try {
			return Long.parseLong(text.replace(".", "").replace(",", ""));
		} catch (NumberFormatException e) {
			return -1;
		}
	}
}
