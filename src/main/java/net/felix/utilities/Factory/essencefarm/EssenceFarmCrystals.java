package net.felix.utilities.Factory.essencefarm;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.felix.CCLiveUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Known power crystals with their last seen level and the target level the player wants to reach.
 * Persisted in config/cclive-utilities/essence_farm_crystals.json.
 */
public final class EssenceFarmCrystals {

	private static final Logger LOGGER = LoggerFactory.getLogger("cclive-utilities");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String FILE = "essence_farm_crystals.json";

	/** Power crystals known from the game data; crystals seen in the tab list are added on top. */
	private static final List<String> KNOWN = List.of(
			"Gier", "Kaktus", "Materialismus", "Mehrfachtreffer", "Opferung", "Schwäche", "Seelen", "Zeit");

	/** Level -1 = unknown, target 0 = not planned. */
	public static final class Crystal {
		public int level = -1;
		public int target = 0;

		/** Target that is actually planned: never below the current level. */
		public int effectiveTarget() {
			return level > 0 && target > level ? target : 0;
		}
	}

	private static final Map<String, Crystal> crystals = new LinkedHashMap<>();
	private static boolean loaded;

	private EssenceFarmCrystals() {
	}

	public static synchronized void load() {
		if (loaded) {
			return;
		}
		loaded = true;
		for (String name : KNOWN) {
			crystals.put(name, new Crystal());
		}
		Path path = path();
		if (!Files.exists(path)) {
			return;
		}
		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
			for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("crystals").entrySet()) {
				crystals.put(entry.getKey(), GSON.fromJson(entry.getValue(), Crystal.class));
			}
		} catch (Exception e) {
			LOGGER.warn("[EssenceFarm] Could not read crystals: {}", e.getMessage());
		}
	}

	public static synchronized List<String> names() {
		return new ArrayList<>(crystals.keySet());
	}

	public static synchronized Crystal get(String name) {
		return crystals.computeIfAbsent(name, n -> new Crystal());
	}

	static synchronized void forget(String name) {
		crystals.remove(name);
	}

	/** Stores a level read from the game; returns true if it changed. */
	public static synchronized boolean updateLevel(String name, int level) {
		if (name == null || name.isBlank() || level <= 0) {
			return false;
		}
		Crystal crystal = get(name.trim());
		if (crystal.level == level) {
			return false;
		}
		crystal.level = level;
		save();
		return true;
	}

	/** Stores the target level the player typed. {@link Crystal#effectiveTarget()} ignores targets that are not above the current level. */
	public static synchronized void setTarget(String name, int target) {
		if (name == null || name.isBlank()) {
			return;
		}
		get(name).target = Math.max(0, target);
	}

	public static synchronized void save() {
		JsonObject map = new JsonObject();
		for (Map.Entry<String, Crystal> entry : crystals.entrySet()) {
			map.add(entry.getKey(), GSON.toJsonTree(entry.getValue()));
		}
		JsonObject root = new JsonObject();
		root.add("crystals", map);
		Path path = path();
		try {
			Files.createDirectories(path.getParent());
			try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
				GSON.toJson(root, writer);
			}
		} catch (Exception e) {
			LOGGER.warn("[EssenceFarm] Could not save crystals: {}", e.getMessage());
		}
	}

	private static Path path() {
		return CCLiveUtilities.getConfigDir().resolve("cclive-utilities").resolve(FILE);
	}
}
