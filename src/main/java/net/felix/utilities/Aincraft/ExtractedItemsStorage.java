package net.felix.utilities.Aincraft;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class ExtractedItemsStorage {
	static final String SECTION_BLUEPRINTS = "blueprints";
	static final String SECTION_FISHING_COMPONENTS = "fishing_components";

	static final class WriteResult {
		final int newBlueprintCount;
		final int newFishingCount;

		WriteResult(int newBlueprintCount, int newFishingCount) {
			this.newBlueprintCount = newBlueprintCount;
			this.newFishingCount = newFishingCount;
		}

		int total() {
			return newBlueprintCount + newFishingCount;
		}
	}

	static File getOutputFile() {
		File outputDir = new File(FabricLoader.getInstance().getGameDir().toFile(), "cclive-utilities");
		outputDir.mkdirs();
		return new File(outputDir, "extracted_items.json");
	}

	static Set<String> loadRegisteredItemNames() {
		Set<String> names = new HashSet<>();
		File outputFile = getOutputFile();
		if (!outputFile.exists()) {
			return names;
		}

		try {
			JsonObject root = readRoot(outputFile);
			collectItemNames(root.getAsJsonArray(SECTION_BLUEPRINTS), names);
			collectItemNames(root.getAsJsonArray(SECTION_FISHING_COMPONENTS), names);
		} catch (Exception e) {
			// Ignore read errors
		}
		return names;
	}

	static WriteResult appendItems(List<ItemInfoData> items) throws Exception {
		File outputFile = getOutputFile();
		JsonObject root = readRoot(outputFile);
		Set<String> existingItemNames = new HashSet<>();
		collectItemNames(root.getAsJsonArray(SECTION_BLUEPRINTS), existingItemNames);
		collectItemNames(root.getAsJsonArray(SECTION_FISHING_COMPONENTS), existingItemNames);

		int newBlueprintCount = 0;
		int newFishingCount = 0;
		for (ItemInfoData item : items) {
			if (item.name == null || item.name.isEmpty() || existingItemNames.contains(item.name)) {
				continue;
			}

			String sectionKey = item.fishingComponent ? SECTION_FISHING_COMPONENTS : SECTION_BLUEPRINTS;
			root.getAsJsonArray(sectionKey).add(toJson(item));
			existingItemNames.add(item.name);

			if (item.fishingComponent) {
				newFishingCount++;
			} else {
				newBlueprintCount++;
			}
		}

		try (FileWriter writer = new FileWriter(outputFile, StandardCharsets.UTF_8)) {
			com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
			gson.toJson(root, writer);
		}

		return new WriteResult(newBlueprintCount, newFishingCount);
	}

	private static JsonObject readRoot(File outputFile) {
		JsonObject root = new JsonObject();
		root.add(SECTION_BLUEPRINTS, new JsonArray());
		root.add(SECTION_FISHING_COMPONENTS, new JsonArray());

		if (!outputFile.exists()) {
			return root;
		}

		try (FileReader reader = new FileReader(outputFile, StandardCharsets.UTF_8)) {
			JsonElement parsed = JsonParser.parseReader(reader);
			if (parsed.isJsonArray()) {
				root.add(SECTION_BLUEPRINTS, parsed.getAsJsonArray());
			} else if (parsed.isJsonObject()) {
				JsonObject existing = parsed.getAsJsonObject();
				if (existing.has(SECTION_BLUEPRINTS)) {
					root.add(SECTION_BLUEPRINTS, existing.getAsJsonArray(SECTION_BLUEPRINTS));
				}
				if (existing.has(SECTION_FISHING_COMPONENTS)) {
					root.add(SECTION_FISHING_COMPONENTS, existing.getAsJsonArray(SECTION_FISHING_COMPONENTS));
				}
			}
		} catch (Exception e) {
			// Keep empty sections on read errors
		}

		return root;
	}

	private static void collectItemNames(JsonArray items, Set<String> names) {
		if (items == null) {
			return;
		}
		for (int i = 0; i < items.size(); i++) {
			JsonObject item = items.get(i).getAsJsonObject();
			if (item.has("name")) {
				String name = item.get("name").getAsString();
				if (name != null && !name.isEmpty()) {
					names.add(name);
				}
			}
		}
	}

	private static JsonObject toJson(ItemInfoData item) {
		JsonObject jsonItem = new JsonObject();

		jsonItem.addProperty("id", item.id);
		if (item.customModelData != null) {
			jsonItem.addProperty("customModelData", item.customModelData);
		}
		jsonItem.addProperty("name", item.name);

		JsonArray foundAtArray = new JsonArray();
		JsonObject foundAtObj = new JsonObject();
		foundAtObj.addProperty("floor", item.floor != null ? item.floor : "");
		foundAtArray.add(foundAtObj);
		jsonItem.add("foundAt", foundAtArray);

		JsonObject priceObj = new JsonObject();
		for (Map.Entry<String, Map<String, Object>> entry : item.price.entrySet()) {
			JsonObject priceItem = new JsonObject();
			Map<String, Object> priceData = entry.getValue();
			priceItem.addProperty("itemName", (String) priceData.get("itemName"));
			Object amount = priceData.get("amount");
			if (amount instanceof Number) {
				priceItem.addProperty("amount", ((Number) amount).intValue());
			} else {
				priceItem.addProperty("amount", amount.toString());
			}
			priceObj.add(entry.getKey(), priceItem);
		}
		jsonItem.add("price", priceObj);

		if (item.blueprintShopPrice != null && !item.blueprintShopPrice.isEmpty()) {
			JsonObject blueprintShopObj = new JsonObject();
			JsonObject blueprintShopPriceObj = new JsonObject();
			for (Map.Entry<String, Map<String, Object>> entry : item.blueprintShopPrice.entrySet()) {
				JsonObject priceItem = new JsonObject();
				Map<String, Object> priceData = entry.getValue();
				priceItem.addProperty("itemName", (String) priceData.get("itemName"));
				Object amount = priceData.get("amount");
				if (amount instanceof Number) {
					priceItem.addProperty("amount", ((Number) amount).intValue());
				} else {
					priceItem.addProperty("amount", amount.toString());
				}
				blueprintShopPriceObj.add(entry.getKey(), priceItem);
			}
			blueprintShopObj.add("price", blueprintShopPriceObj);
			jsonItem.add("blueprint_shop", blueprintShopObj);
		}

		JsonObject infoObj = new JsonObject();
		infoObj.addProperty("aspect", item.aspect);
		infoObj.addProperty("rarity", item.rarity != null ? item.rarity : "");
		infoObj.addProperty("description", "");
		infoObj.addProperty("type", item.type != null ? item.type : "");
		infoObj.addProperty("piece", item.piece != null ? item.piece : "");
		JsonArray modifierArray = new JsonArray();
		if (item.modifiers != null && !item.modifiers.isEmpty()) {
			for (String modifier : item.modifiers) {
				modifierArray.add(modifier);
			}
		}
		infoObj.add("modifier", modifierArray);
		if (item.stats != null && !item.stats.isEmpty()) {
			JsonArray statsArray = new JsonArray();
			for (String stat : item.stats) {
				statsArray.add(stat);
			}
			infoObj.add("stats", statsArray);
		} else {
			infoObj.add("stats", new JsonArray());
		}
		infoObj.addProperty("blueprint", item.blueprint);
		infoObj.addProperty("module", false);
		infoObj.addProperty("ability", false);
		infoObj.addProperty("rune", false);
		infoObj.addProperty("power_crystal", false);
		infoObj.addProperty("essence", false);
		jsonItem.add("info", infoObj);

		JsonArray tagsArray = new JsonArray();
		for (String tag : item.tags) {
			tagsArray.add(tag);
		}
		jsonItem.add("tags", tagsArray);

		return jsonItem;
	}

	private ExtractedItemsStorage() {
	}
}
