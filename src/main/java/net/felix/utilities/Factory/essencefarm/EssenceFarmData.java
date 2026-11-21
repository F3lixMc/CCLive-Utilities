package net.felix.utilities.Factory.essencefarm;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
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
import java.util.TreeMap;
import net.felix.CCLiveUtilities;
import net.felix.utilities.Other.util.JsonConfigFiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wave data for the essence farm planner: tier table, farm waves from the wiki analysis
 * and wave compositions counted live (persisted in config/cclive-utilities/essence_farm_observations.json).
 */
public final class EssenceFarmData {

	private static final Logger LOGGER = LoggerFactory.getLogger("cclive-utilities");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String OBSERVATIONS_FILE = "essence_farm_observations.json";
	/** Format version; files from older test builds held merged counts of several waves and are dropped. */
	private static final int OBSERVATIONS_VERSION = 2;

	private static EssenceTierTable tiers = new EssenceTierTable(List.of());
	private static final Map<Integer, EssenceFarmPlanner.WaveData> staticWaves = new TreeMap<>();
	private static final Map<Integer, WaveObservation> observations = new TreeMap<>();
	private static boolean loaded;

	private EssenceFarmData() {
	}

	public static synchronized void load() {
		if (loaded) {
			return;
		}
		loaded = true;
		JsonObject tierJson = readResource("essence_tiers.json");
		if (tierJson != null) {
			tiers = EssenceTierTable.fromJson(tierJson);
		}
		JsonObject wavesJson = readResource("essence_farm_waves.json");
		if (wavesJson != null) {
			for (JsonElement element : wavesJson.getAsJsonArray("waves")) {
				EssenceFarmPlanner.WaveData wave = parseWave(element.getAsJsonObject());
				staticWaves.put(wave.wave(), wave);
			}
		}
		loadObservations();
	}

	public static EssenceTierTable tiers() {
		return tiers;
	}

	/** Candidate waves: live observations replace the static entry of the same wave. */
	public static synchronized List<EssenceFarmPlanner.WaveData> candidateWaves() {
		Map<Integer, EssenceFarmPlanner.WaveData> merged = new TreeMap<>(staticWaves);
		for (Map.Entry<Integer, WaveObservation> entry : observations.entrySet()) {
			Map<String, Integer> counts = entry.getValue().medianCounts();
			int mobs = entry.getValue().medianMobs();
			if (mobs > 0 && !counts.isEmpty()) {
				merged.put(entry.getKey(), new EssenceFarmPlanner.WaveData(entry.getKey(), mobs, Map.copyOf(counts)));
			}
		}
		return new ArrayList<>(merged.values());
	}

	/** Average duration of one run of this wave in ms, or 0 if never measured. */
	public static synchronized long averageRunMs(int wave) {
		WaveObservation obs = observations.get(wave);
		return obs != null ? obs.avgCycleMs : 0L;
	}

	/**
	 * Records a completed run: its composition (empty if the run was not observed from its start)
	 * and one wave cycle in ms (0 if unknown). Implausible compositions are dropped, see {@link WaveObservation}.
	 */
	public static synchronized void recordRun(int wave, Map<String, Integer> counts, long durationMs) {
		if (wave <= 0 || (counts.isEmpty() && durationMs <= 0)) {
			return;
		}
		WaveObservation obs = observations.computeIfAbsent(wave, w -> new WaveObservation());
		EssenceFarmPlanner.WaveData known = staticWaves.get(wave);
		if (!counts.isEmpty() && !obs.addRun(counts, known != null ? known.counts() : null)) {
			LOGGER.info("[EssenceFarm] Dropped implausible run of wave {}: {}", wave, counts);
		}
		obs.addCycle(durationMs);
		saveObservations();
	}

	private static EssenceFarmPlanner.WaveData parseWave(JsonObject obj) {
		Map<String, Integer> counts = new LinkedHashMap<>();
		JsonObject countsJson = obj.getAsJsonObject("counts");
		for (Map.Entry<String, JsonElement> entry : countsJson.entrySet()) {
			counts.put(entry.getKey(), entry.getValue().getAsInt());
		}
		return new EssenceFarmPlanner.WaveData(obj.get("wave").getAsInt(), obj.get("mobs").getAsInt(), counts);
	}

	private static JsonObject readResource(String fileName) {
		Path path = JsonConfigFiles.getConfigFilePath(fileName);
		if (path == null) {
			LOGGER.warn("[EssenceFarm] {} not found", fileName);
			return null;
		}
		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			return JsonParser.parseReader(reader).getAsJsonObject();
		} catch (Exception e) {
			LOGGER.warn("[EssenceFarm] Could not read {}: {}", fileName, e.getMessage());
			return null;
		}
	}

	private static Path observationsPath() {
		return CCLiveUtilities.getConfigDir().resolve("cclive-utilities").resolve(OBSERVATIONS_FILE);
	}

	private static void loadObservations() {
		Path path = observationsPath();
		if (!Files.exists(path)) {
			return;
		}
		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
			if (!root.has("version") || root.get("version").getAsInt() != OBSERVATIONS_VERSION) {
				LOGGER.info("[EssenceFarm] Ignoring observations from an older format");
				return;
			}
			JsonArray waves = root.getAsJsonArray("waves");
			for (JsonElement element : waves) {
				JsonObject obj = element.getAsJsonObject();
				WaveObservation obs = GSON.fromJson(obj, WaveObservation.class);
				if (obs.recent == null) {
					obs.recent = new ArrayList<>();
				}
				observations.put(obj.get("wave").getAsInt(), obs);
			}
		} catch (Exception e) {
			LOGGER.warn("[EssenceFarm] Could not read observations: {}", e.getMessage());
		}
	}

	private static void saveObservations() {
		JsonArray waves = new JsonArray();
		for (Map.Entry<Integer, WaveObservation> entry : observations.entrySet()) {
			JsonObject obj = GSON.toJsonTree(entry.getValue()).getAsJsonObject();
			obj.addProperty("wave", entry.getKey());
			waves.add(obj);
		}
		JsonObject root = new JsonObject();
		root.addProperty("version", OBSERVATIONS_VERSION);
		root.add("waves", waves);
		Path path = observationsPath();
		try {
			Files.createDirectories(path.getParent());
			try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
				GSON.toJson(root, writer);
			}
		} catch (Exception e) {
			LOGGER.warn("[EssenceFarm] Could not save observations: {}", e.getMessage());
		}
	}
}
