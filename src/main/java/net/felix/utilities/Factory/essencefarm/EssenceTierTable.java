package net.felix.utilities.Factory.essencefarm;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Essence tiers per mob type (essence_tiers.json): the waves from which a mob drops its next tier,
 * plus the display name (as in MKLevel.json) and the name used in the essence bag.
 */
public final class EssenceTierTable {

	/** One mob type, e.g. {@code horse}. */
	public record Mob(String type, String name, List<String> aliases, String bagName, long[] tierWaves) {
	}

	private final Map<String, Mob> byType = new LinkedHashMap<>();
	private final Map<String, String> typeByName = new HashMap<>();

	public EssenceTierTable(List<Mob> mobs) {
		for (Mob mob : mobs) {
			byType.put(mob.type(), mob);
			if (mob.name() != null) {
				typeByName.put(key(mob.name()), mob.type());
			}
			for (String alias : mob.aliases()) {
				typeByName.put(key(alias), mob.type());
			}
		}
	}

	public static EssenceTierTable fromJson(JsonObject root) {
		List<Mob> mobs = new ArrayList<>();
		JsonArray array = root.getAsJsonArray("mobs");
		for (JsonElement element : array) {
			JsonObject obj = element.getAsJsonObject();
			List<String> aliases = new ArrayList<>();
			if (obj.has("aliases")) {
				for (JsonElement alias : obj.getAsJsonArray("aliases")) {
					aliases.add(alias.getAsString());
				}
			}
			JsonArray wavesJson = obj.getAsJsonArray("tierWaves");
			long[] tierWaves = new long[wavesJson.size()];
			for (int i = 0; i < tierWaves.length; i++) {
				tierWaves[i] = wavesJson.get(i).getAsLong();
			}
			mobs.add(new Mob(
					obj.get("type").getAsString(),
					obj.has("name") ? obj.get("name").getAsString() : null,
					Collections.unmodifiableList(aliases),
					obj.has("bagName") ? obj.get("bagName").getAsString() : null,
					tierWaves));
		}
		return new EssenceTierTable(mobs);
	}

	/** Essence tier a mob drops in this wave: 1 + number of thresholds below the wave. */
	public int tierAt(String type, int wave) {
		Mob mob = byType.get(type);
		if (mob == null || wave <= 0) {
			return -1;
		}
		int tier = 1;
		for (long threshold : mob.tierWaves()) {
			if (threshold < wave) {
				tier++;
			}
		}
		return tier;
	}

	public Mob mob(String type) {
		return byType.get(type);
	}

	public boolean knows(String type) {
		return byType.containsKey(type);
	}

	/** Mob type for a display name such as "Pferd" or "Piglin Brute"; null if unknown. */
	public String typeForName(String name) {
		return name == null ? null : typeByName.get(key(name));
	}

	/** Display name as in MKLevel.json, e.g. "Pferd T1". */
	public String displayName(String type, int tier) {
		Mob mob = byType.get(type);
		String name = mob != null && mob.name() != null ? mob.name() : type;
		return name + " T" + tier;
	}

	/** Essence bag key, e.g. "Pferde [Essenz] Tier 1"; null if no bag name is known. */
	public String bagKey(String type, int tier) {
		Mob mob = byType.get(type);
		if (mob == null || mob.bagName() == null) {
			return null;
		}
		return mob.bagName() + " [Essenz] Tier " + tier;
	}

	private static String key(String name) {
		return name.trim().toLowerCase(Locale.ROOT);
	}
}
