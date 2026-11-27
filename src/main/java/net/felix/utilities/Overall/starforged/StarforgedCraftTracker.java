package net.felix.utilities.Overall.starforged;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Remembers what the player last crafted at the smith, so the starforged animation can show the right item.
 * <ul>
 * <li>Single craft: {@code [Legend] <Name> erfolgreich hergestellt!} names the item in chat.</li>
 * <li>Blueprint batch (usually 10x): {@code [Legend] Du hast den Bauplan erfolgreich 10x gebaut!} has no name,
 * so the item last shift-clicked (shift + left click crafts 10) in a blueprint inventory is used.</li>
 * </ul>
 * The smithing state message follows the craft message. Parsing is pure and unit tested; the remembered
 * state is only touched on the client thread.
 */
final class StarforgedCraftTracker {

	/** How long a clicked blueprint stays the candidate for a following state message. */
	static final long CLICK_MEMORY_MS = 120_000L;
	/** How long a chat craft message stays linked to a following state message. */
	static final long CRAFT_MESSAGE_MEMORY_MS = 5_000L;

	private static final Pattern CRAFTED_PATTERN = Pattern.compile(
			"^\\[Legend\\]\\s*(.+?)\\s+erfolgreich hergestellt!");
	private static final Pattern BATCH_PATTERN = Pattern.compile(
			"^\\[Legend\\]\\s*Du hast den Bauplan erfolgreich \\d+x gebaut!");
	private static final Pattern CRAFT_ACTION_PATTERN = Pattern.compile(
			"\\[Linksklick\\]\\s*:?\\s*Herstellen", Pattern.CASE_INSENSITIVE);
	private static final String[] BLUEPRINT_INVENTORY_TITLES = {
			"Baupläne [Waffen]", "Baupläne [Rüstung]", "Baupläne [Werkzeuge]",
			"Favorisierte [Rüstungsbaupläne]", "Favorisierte [Waffenbaupläne]", "Favorisierte [Werkzeugbaupläne]",
			"CACTUS_CLICKER.blueprints.favorites.title.tools" };

	/** Result of the last craft chat message: the named item, or null for a nameless blueprint batch. */
	private static String lastCraftName;
	private static boolean lastCraftBatch;
	private static long lastCraftAtMs = -1L;

	private StarforgedCraftTracker() {
	}

	// ------------------------------------------------------------------ parsing (pure)

	static String clean(String raw) {
		if (raw == null) {
			return "";
		}
		return raw.replaceAll("§[0-9a-fk-or]", "")
				.replaceAll("[\\u3400-\\u4DBF\\u4E00-\\u9FFF\\uE000-\\uF8FF]", "")
				.trim();
	}

	/** Text from "[Legend]" on, so client-side prefixes such as chat timestamps are ignored; null if absent. */
	private static String fromLegend(String raw) {
		String text = clean(raw);
		if (text.contains(">>")) {
			return null;
		}
		int index = text.indexOf("[Legend]");
		return index < 0 ? null : text.substring(index);
	}

	/** Item name of a single craft message, or null. */
	static String craftedName(String raw) {
		String text = fromLegend(raw);
		if (text == null) {
			return null;
		}
		Matcher matcher = CRAFTED_PATTERN.matcher(text);
		return matcher.find() ? matcher.group(1).trim() : null;
	}

	static boolean isBlueprintBatch(String raw) {
		String text = fromLegend(raw);
		return text != null && BATCH_PATTERN.matcher(text).find();
	}

	static boolean isBlueprintInventory(String screenTitle) {
		String title = clean(screenTitle);
		for (String candidate : BLUEPRINT_INVENTORY_TITLES) {
			if (title.contains(candidate)) {
				return true;
			}
		}
		return false;
	}

	static boolean isCraftAction(List<String> loreLines) {
		if (loreLines == null) {
			return false;
		}
		for (String line : loreLines) {
			if (CRAFT_ACTION_PATTERN.matcher(clean(line)).find()) {
				return true;
			}
		}
		return false;
	}

	/** "Schattenstahl Leggings - [Bauplan] [e3]" becomes "Schattenstahl Leggings". */
	static String itemName(String hoverName) {
		String name = clean(hoverName);
		int suffix = name.indexOf(" - [");
		return suffix > 0 ? name.substring(0, suffix).trim() : name;
	}

	/** True if a clicked item's name and a chat item name refer to the same item. */
	static boolean sameItem(String clickedName, String chatName) {
		String a = itemName(clickedName).toLowerCase(Locale.ROOT);
		String b = clean(chatName).toLowerCase(Locale.ROOT);
		if (a.isEmpty() || b.isEmpty()) {
			return false;
		}
		return a.contains(b) || b.contains(a);
	}

	// ------------------------------------------------------------------ remembered state (client thread)

	/** Records a craft chat message; returns true if the message was one. */
	static boolean recordChat(String raw, long nowMs) {
		String name = craftedName(raw);
		if (name != null) {
			lastCraftName = name;
			lastCraftBatch = false;
			lastCraftAtMs = nowMs;
			return true;
		}
		if (isBlueprintBatch(raw)) {
			lastCraftName = null;
			lastCraftBatch = true;
			lastCraftAtMs = nowMs;
			return true;
		}
		return false;
	}

	/** Name from the craft message right before the state message, or null (batch, or no recent message). */
	static String recentCraftName(long nowMs) {
		if (lastCraftAtMs < 0L || nowMs - lastCraftAtMs > CRAFT_MESSAGE_MEMORY_MS) {
			return null;
		}
		return lastCraftName;
	}

	/** True if the craft message right before the state message was a blueprint batch. */
	static boolean recentCraftWasBatch(long nowMs) {
		return lastCraftBatch && lastCraftAtMs >= 0L && nowMs - lastCraftAtMs <= CRAFT_MESSAGE_MEMORY_MS;
	}

	static void reset() {
		lastCraftBatch = false;
		lastCraftName = null;
		lastCraftAtMs = -1L;
	}
}
