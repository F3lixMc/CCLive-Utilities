package net.felix.utilities.Aincraft.information.cards;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import static net.felix.utilities.Aincraft.information.util.JsonConfigFiles.getConfigFilePath;

/**
 * Effekte und Ebenen von Karten und Statuen.
 */
public final class CardsStatuesDatabase {

	private CardsStatuesDatabase() {
	}

	// Cards and Statues effects tracking
	public static Map<String, String> cardsEffects = new HashMap<>();

	public static Map<String, String> statuesEffects = new HashMap<>();

	// Cards and Statues floor tracking
	static Map<String, List<Integer>> cardsFloors = new HashMap<>();

	static Map<String, List<Integer>> statuesFloors = new HashMap<>();

	/** Effekt einer Karte aus CardsStatues.json (exakter oder case-insensitive Name). */
	public static String getCardEffectByName(String cardName) {
		if (cardName == null || cardName.isEmpty()) {
			return null;
		}
		String effect = cardsEffects.get(cardName);
		if (effect != null) {
			return effect;
		}
		for (Map.Entry<String, String> entry : cardsEffects.entrySet()) {
			if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(cardName)) {
				return entry.getValue();
			}
		}
		return null;
	}

	/** Ebenen einer Karte aus CardsStatues.json, z. B. [8, 29]. */
	public static List<Integer> getCardFloorsByName(String cardName) {
		if (cardName == null || cardName.isEmpty()) {
			return null;
		}
		List<Integer> floors = cardsFloors.get(cardName);
		if (floors == null) {
			for (Map.Entry<String, List<Integer>> entry : cardsFloors.entrySet()) {
				if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(cardName)) {
					floors = entry.getValue();
					break;
				}
			}
		}
		return floors != null && !floors.isEmpty() ? new ArrayList<>(floors) : null;
	}

	/**
	 * Gets floor numbers for a card or statue from CardsStatues.json
	 * @param stack The item stack (name/lore may contain [Karte] or [Statue])
	 * @return Floor numbers, or null if not found
	 */
	public static List<Integer> getFloorsForCardOrStatue(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return null;
		}
		
		String cardStatueName = extractCardOrStatueNameFromStack(stack);
		if (cardStatueName == null || cardStatueName.isEmpty()) {
			return null;
		}
		
		List<Integer> floors = cardsFloors.get(cardStatueName);
		if (floors == null) {
			for (Map.Entry<String, List<Integer>> entry : cardsFloors.entrySet()) {
				if (entry.getKey().equalsIgnoreCase(cardStatueName)) {
					floors = entry.getValue();
					break;
				}
			}
		}
		if (floors == null) {
			floors = statuesFloors.get(cardStatueName);
			if (floors == null) {
				for (Map.Entry<String, List<Integer>> entry : statuesFloors.entrySet()) {
					if (entry.getKey().equalsIgnoreCase(cardStatueName)) {
						floors = entry.getValue();
						break;
					}
				}
			}
		}
		
		return floors != null && !floors.isEmpty() ? new ArrayList<>(floors) : null;
	}

	private static String extractCardOrStatueNameFromStack(ItemStack stack) {
		StringBuilder textBuilder = new StringBuilder(stack.getHoverName().getString());
		var loreComponent = stack.get(net.minecraft.core.component.DataComponents.LORE);
		if (loreComponent != null) {
			for (Component line : loreComponent.lines()) {
				textBuilder.append(' ').append(line.getString());
			}
		}
		
		String text = textBuilder.toString().replaceAll("§[0-9a-fk-or]", "");
		
		for (String tag : new String[]{"[Karte]", "[Statue]"}) {
			if (text.contains(tag)) {
				int index = text.indexOf(tag);
				if (index > 0) {
					String itemName = text.substring(0, index).trim();
					itemName = itemName.replaceAll("^[-\\s]+", "").trim();
					itemName = itemName.replaceAll("[\\u3400-\\u4DBF\\u4E00-\\u9FFF]", "").trim();
					itemName = itemName.replaceAll("\\s*-\\s*$", "").trim();
					if (!itemName.isEmpty()) {
						return itemName;
					}
				}
			}
		}
		
		String name = stack.getHoverName().getString().replaceAll("§[0-9a-fk-or]", "").trim();
		name = name.replaceAll("[\\u3400-\\u4DBF\\u4E00-\\u9FFF]", "").trim();
		return name.isEmpty() ? null : name;
	}

	/**
	 * Loads cards and statues effects from JSON file
	 */
	public static void loadCardsStatuesDatabase() {
		try {
			// Versuche zuerst lokale Datei zu laden, sonst Mod-Ressourcen
			java.nio.file.Path resource = getConfigFilePath("CardsStatues.json");
			if (resource == null) {
				return;
			}
			
			try (var inputStream = java.nio.file.Files.newInputStream(resource)) {
				try (var reader = new java.io.InputStreamReader(inputStream)) {
					JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
					
					// Load cards effects
					if (json.has("karten")) {
						JsonObject kartenObject = json.getAsJsonObject("karten");
						for (var entry : kartenObject.entrySet()) {
							String cardName = entry.getKey();
							String effect = null;
							List<Integer> floors = new ArrayList<>();
							
							// Support both old format (string) and new format (object with effect and floor)
							if (entry.getValue().isJsonObject()) {
								// New format: object with "effect" and "floor"
								JsonObject cardObject = entry.getValue().getAsJsonObject();
								if (cardObject.has("effect")) {
									effect = cardObject.get("effect").getAsString();
								}
								if (cardObject.has("floor")) {
									var floorElement = cardObject.get("floor");
									if (floorElement.isJsonArray()) {
										var floorArray = floorElement.getAsJsonArray();
										for (var floorValue : floorArray) {
											if (floorValue.isJsonPrimitive() && floorValue.getAsJsonPrimitive().isNumber()) {
												floors.add(floorValue.getAsInt());
											}
										}
									}
								}
							} else {
								// Old format: direct string
								effect = entry.getValue().getAsString();
							}
							
							if (!cardName.isEmpty() && effect != null && !effect.isEmpty()) {
								cardsEffects.put(cardName, effect);
							}
							if (!floors.isEmpty()) {
								cardsFloors.put(cardName, floors);
							}
						}
					}
					
					// Load statues effects
					if (json.has("statuen")) {
						JsonObject statuenObject = json.getAsJsonObject("statuen");
						for (var entry : statuenObject.entrySet()) {
							String statueName = entry.getKey();
							String effect = null;
							List<Integer> floors = new ArrayList<>();
							
							// Support both old format (string) and new format (object with effect and floor)
							if (entry.getValue().isJsonObject()) {
								// New format: object with "effect" and "floor"
								JsonObject statueObject = entry.getValue().getAsJsonObject();
								if (statueObject.has("effect")) {
									effect = statueObject.get("effect").getAsString();
								}
								if (statueObject.has("floor")) {
									var floorElement = statueObject.get("floor");
									if (floorElement.isJsonArray()) {
										var floorArray = floorElement.getAsJsonArray();
										for (var floorValue : floorArray) {
											if (floorValue.isJsonPrimitive() && floorValue.getAsJsonPrimitive().isNumber()) {
												floors.add(floorValue.getAsInt());
											}
										}
									}
								}
							} else {
								// Old format: direct string
								effect = entry.getValue().getAsString();
							}
							
							if (!statueName.isEmpty() && effect != null && !effect.isEmpty()) {
								statuesEffects.put(statueName, effect);
							}
							if (!floors.isEmpty()) {
								statuesFloors.put(statueName, floors);
							}
						}
					}
				}
			}
		} catch (Exception e) {
			// Silent error handling("Failed to load cards/statues database: " + e.getMessage());
			// Silent error handling
		}
	}

	/**
	 * Lädt die Cards/Statues-Datenbank neu (nach Update vom Server)
	 */
	public static void reloadCardsStatuesDatabase() {
		cardsEffects.clear();
		statuesEffects.clear();
		cardsFloors.clear();
		statuesFloors.clear();
		loadCardsStatuesDatabase();
	}
}
