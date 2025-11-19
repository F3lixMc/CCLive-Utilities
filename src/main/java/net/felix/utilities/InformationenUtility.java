package net.felix.utilities;

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.felix.CCLiveUtilitiesConfig;
import net.fabricmc.loader.api.FabricLoader;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class InformationenUtility {
	
	private static boolean isInitialized = false;
	
	// Materials tracking
	private static Map<String, MaterialInfo> materialsDatabase = new HashMap<>();
	private static final String MATERIALS_CONFIG_FILE = "assets/cclive-utilities/Aincraft.json";
	
	// Essence tracking
	private static Map<String, EssenceInfo> essencesDatabase = new HashMap<>();
	private static final String ESSENCES_CONFIG_FILE = "assets/cclive-utilities/Essenz.json";
	
	// Aspect tracking
	private static Map<String, AspectInfo> aspectsDatabase = new HashMap<>();
	private static final String ASPECTS_CONFIG_FILE = "assets/cclive-utilities/Aspekte.json";

	public static void initialize() {
		if (isInitialized) {
			return;
		}
		
		// Load materials database
		loadMaterialsDatabase();
		
		// Load essences database
		loadEssencesDatabase();
		
		// Load aspects database
		loadAspectsDatabase();
		
		// Initialize aspect overlay and renderer
		AspectOverlay.initialize();
		AspectOverlayRenderer.initialize();
		

		
		// Register tooltip callback for material information
		ItemTooltipCallback.EVENT.register((stack, context, tooltipType, lines) -> {
			// Only process if Informationen Utility is enabled in config
			if (!CCLiveUtilitiesConfig.HANDLER.instance().enableMod ||
				!CCLiveUtilitiesConfig.HANDLER.instance().informationenUtilityEnabled) {
				return;
			}
			
			// Check if we're in the special inventory (the one with 㬉)
			MinecraftClient client = MinecraftClient.getInstance();
			boolean isSpecialInventory = false;
			String screenTitle = "";
			if (client.currentScreen != null) {
				screenTitle = client.currentScreen.getTitle().getString();
				if (screenTitle.contains("㬉")) {
					isSpecialInventory = true;
				}
			}
			
			// Check if the respective setting is enabled for this inventory type
			if (isSpecialInventory && !CCLiveUtilitiesConfig.HANDLER.instance().showEbenenInSpecialInventory) {
				return; // Special inventory disabled
			}
			if (!isSpecialInventory && !CCLiveUtilitiesConfig.HANDLER.instance().showEbenenInNormalInventories) {
				return; // Normal inventories disabled
			}
			
			// In special inventory (㬉), only modify the first line (item name)
			// In other inventories, check all lines for material names
			int startIndex = 0;
			int endIndex = isSpecialInventory ? 1 : lines.size(); // Special inventory: only first line, others: all lines
			
			for (int i = startIndex; i < endIndex; i++) {
				Text line = lines.get(i);
				String lineText = line.getString();
				
				// Skip if lineText is null or empty
				if (lineText == null || lineText.isEmpty()) {
					continue;
				}
				
				// Remove Minecraft formatting codes (like §a, §b, etc.)
				String cleanLineText = lineText.replaceAll("§[0-9a-fk-or]", "");
				
				// Also remove the Unicode formatting characters that appear as 㔻㔶
				cleanLineText = cleanLineText.replaceAll("[\\u3400-\\u4DBF]", "");
				
				// Check if we're in the "Machtkristalle Verbessern" inventory for essence information
				boolean isEssenceImprovementInventory = screenTitle.contains("Machtkristalle Verbessern");
				
				// Check for essence information in "Machtkristalle Verbessern" inventory FIRST
				if (isEssenceImprovementInventory && lineText.contains("[Essenz]")) {
					// Get the text before the colon (if present) to remove the count info
					String textBeforeColon = cleanLineText;
					if (cleanLineText.contains(":")) {
						textBeforeColon = cleanLineText.substring(0, cleanLineText.indexOf(":")).trim();
					}
					
					// Remove leading dash/minus if present
					if (textBeforeColon.startsWith("-")) {
						textBeforeColon = textBeforeColon.substring(1).trim();
					}
					
					// The textBeforeColon already contains the full essence name including [Essenz] and Tier
					String essenceNameToSearch = textBeforeColon;
					
					// Look for essence in database
					EssenceInfo essenceInfo = essencesDatabase.get(essenceNameToSearch);
					if (essenceInfo != null && CCLiveUtilitiesConfig.HANDLER.instance().showWaveDisplay) {
						// Add essence information as a new line with mixed colors
						Text essenceInfoText = Text.literal(" -> Welle: ")
							.styled(style -> style.withColor(0xC0C0C0)) // Light gray
							.append(Text.literal(String.valueOf(essenceInfo.wave))
								.styled(style -> style.withColor(0x55FF55))); // Light green
						
						// Add the essence info line after the current line
						lines.add(i + 1, essenceInfoText);
						
						// Skip the next line since we just added essence info
						i++;
						continue; // Skip other processing for this line
					}
				}
				
				// Skip Essenz items that should not show level information (but allow essence improvement inventory)
				if (screenTitle.contains("Essenz [Auswahl]") || screenTitle.contains("Essenz-Tasche") ||
					screenTitle.contains("Essenzernter") || screenTitle.contains("㨶") || screenTitle.contains("Legend+ Menü")) {
					continue;
				}
				
				// Extract the part before the first '[' to get just the material name
				String materialNamePart = cleanLineText;
				if (cleanLineText.contains("[")) {
					materialNamePart = cleanLineText.substring(0, cleanLineText.indexOf("[")).trim();
				}
				
				// Skip if the material name part is empty
				if (materialNamePart.isEmpty()) {
					continue;
				}
				
				// Skip if the line contains [Karte] or [Statue], but allow [Bauplan] to pass through
				if (lineText.contains("[Karte]") || lineText.contains("[Statue]")) {
					continue;
				}
				
				// Sort materials by length (longest first) to avoid shorter names matching before longer ones
				List<Map.Entry<String, MaterialInfo>> sortedMaterials = new ArrayList<>(materialsDatabase.entrySet());
				sortedMaterials.sort((a, b) -> Integer.compare(b.getKey().length(), a.getKey().length()));
				
				// Look for materials in the material name part (longest first)
				for (Map.Entry<String, MaterialInfo> entry : sortedMaterials) {
					String materialName = entry.getKey();
					MaterialInfo info = entry.getValue();
					
					// Use word boundaries to avoid partial matches
					// This ensures "Schweinehuf" doesn't match "Kräftiger Schweinehuf"
					// Also handles German characters like "ß" properly
					String escapedMaterialName = java.util.regex.Pattern.quote(materialName);
					String pattern = "\\b" + escapedMaterialName + "\\b";
					
					if (java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.UNICODE_CHARACTER_CLASS).matcher(materialNamePart).find()) {
						// Skip if this is a blueprint line - let BPViewerUtility handle it
						if (lineText.contains("[Bauplan]")) {
							continue; // Skip blueprint lines to avoid conflicts
						}
						
						// In special inventory, only show floor info for mob names, not materials
						if (isSpecialInventory && !info.rarity.equals("mob")) {
							continue; // Skip materials in special inventory
						}
						
						// Create new text with material info appended to the same line
						int color = getRarityColor(info.rarity);
						
						// Different format for special inventory
						Text materialInfo;
						if (isSpecialInventory) {
							// In special inventory: show just the floor number behind the mob name
							materialInfo = Text.literal(" [" + info.floor + "]")
								.styled(style -> style.withColor(color));
						} else {
							// In normal inventory: show full floor information
							materialInfo = Text.literal(" [Ebene " + info.floor + "]")
								.styled(style -> style.withColor(color));
						}
						
						// Combine original line with material info
						Text combinedText = line.copy().append(materialInfo);
						
						// Replace the original line
						lines.set(i, combinedText);
						break; // Only add info for the first match in this line
					}
				}
			}
			
			// Check for aspect information in blueprint items and update overlay
			checkForAspectInformationAndUpdateOverlay(lines, client);
			
			// Add aspect name to tooltip (always visible)
			addAspectNameToTooltip(lines, client);
		});
		
		// Register mouse move callback to update aspect overlay
		net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.currentScreen != null) {
				// Update aspect overlay based on current tooltip content
				updateAspectOverlayFromTooltip(client);
			}
		});
		
		isInitialized = true;
	}
	
	/**
	 * Updates the aspect overlay based on current tooltip content
	 */
	private static void updateAspectOverlayFromTooltip(MinecraftClient client) {
		// Check if aspect overlay is enabled in config
		if (!CCLiveUtilitiesConfig.HANDLER.instance().aspectOverlayEnabled) {
			return; // Don't update overlay if aspect overlay is disabled
		}
		
		// Check if shift is pressed
		boolean isShiftPressed = InputUtil.isKeyPressed(client.getWindow().getHandle(), 
													   InputUtil.GLFW_KEY_LEFT_SHIFT) || 
								InputUtil.isKeyPressed(client.getWindow().getHandle(), 
													   InputUtil.GLFW_KEY_RIGHT_SHIFT);
		
		if (!isShiftPressed) {
			AspectOverlay.hideOverlay();
			return;
		}
		
		// For now, we'll use a simple approach - check if we're hovering over a blueprint item
		// In a real implementation, you'd need to access the current tooltip content
		// This is a placeholder that will be improved
		
		// Since we can't easily access the current tooltip content from here,
		// we'll use a different approach - we'll modify the tooltip callback to also update the overlay
	}
	
	/**
	 * Adds aspect name to tooltip (only in specific blueprint inventories)
	 */
	private static void addAspectNameToTooltip(List<Text> lines, MinecraftClient client) {
		// Check if aspect overlay is enabled in config
		if (!CCLiveUtilitiesConfig.HANDLER.instance().aspectOverlayEnabled) {
			return; // Don't show aspect information if aspect overlay is disabled
		}
		
		// Check if we're in a blueprint inventory that should show aspect information
		if (client.currentScreen == null) {
			return;
		}
		
		String screenTitle = client.currentScreen.getTitle().getString();
		
		// Remove Minecraft formatting codes and Unicode characters for comparison
		String cleanScreenTitle = screenTitle.replaceAll("§[0-9a-fk-or]", "")
											.replaceAll("[\\u3400-\\u4DBF]", "");
		
		// Only show aspect information in specific blueprint inventories
		boolean isBlueprintInventory = cleanScreenTitle.contains("Baupläne [Waffen]") ||
									  cleanScreenTitle.contains("Baupläne [Rüstung]") ||
									  cleanScreenTitle.contains("Baupläne [Werkzeuge]") ||
									  cleanScreenTitle.contains("Bauplan [Shop]") || 
									  cleanScreenTitle.contains("Favorisierte [Rüstungsbaupläne]") ||
									  cleanScreenTitle.contains("Favorisierte [Waffenbaupläne]") ||
									  cleanScreenTitle.contains("CACTUS_CLICKER.blueprints.favorites.title.tools");
		
		if (!isBlueprintInventory) {
			return; // Don't show aspect information in other inventories
		}
		
		// Find blueprint line and add aspect name to tooltip
		for (int i = 0; i < lines.size(); i++) {
			Text line = lines.get(i);
			String lineText = line.getString();
			
			// Skip if lineText is null or empty
			if (lineText == null || lineText.isEmpty()) {
				continue;
			}
			
			// Check if this is a blueprint line
			if (lineText.contains("[Bauplan]")) {
				// Extract the item name (everything before "[Bauplan]")
				String itemName = lineText.substring(0, lineText.indexOf("[Bauplan]")).trim();
				
				// Remove leading dash/minus if present
				if (itemName.startsWith("-")) {
					itemName = itemName.substring(1).trim();
				}
				
				// Remove trailing dash/minus if present
				if (itemName.endsWith("-")) {
					itemName = itemName.substring(0, itemName.length() - 1).trim();
				}
				
				// Remove Minecraft formatting codes and Unicode characters
				itemName = itemName.replaceAll("§[0-9a-fk-or]", "");
				itemName = itemName.replaceAll("[\\u3400-\\u4DBF]", "");
				itemName = itemName.replaceAll("[^a-zA-ZäöüßÄÖÜ\\s-]", "").trim();
				
				// Look for this item in the aspects database
				AspectInfo aspectInfo = aspectsDatabase.get(itemName);
				if (aspectInfo != null) {
					// Calculate position: 5th line from bottom
					int targetPosition = Math.max(0, lines.size() - 5);
					
					// Create aspect name text without "(Shift)" suffix
					Text aspectNameText = Text.literal("Enthält: ")
						.styled(style -> style.withColor(0xFFFFFFFF)) // White color
						.append(Text.literal(aspectInfo.aspectName)
							.styled(style -> style.withColor(0xFFFCA800))); // Same color as overlay (#FCA800)
					
					// Insert aspect name at the target position
					lines.add(targetPosition, aspectNameText);
					
					// Add "(Shift für mehr Info)" on the next line
					Text shiftInfoText = Text.literal("(Shift für mehr Info)")
						.styled(style -> style.withColor(0xFFCCCCCC)); // Light gray color
					lines.add(targetPosition + 1, shiftInfoText);
					
					// Add empty line after shift info
					Text emptyLineText = Text.literal(" ");
					lines.add(targetPosition + 2, emptyLineText);
					
					break; // Only process the first blueprint line
				}
			}
		}
	}
	
	/**
	 * Checks for aspect information in blueprint items and updates the overlay
	 */
	private static void checkForAspectInformationAndUpdateOverlay(List<Text> lines, MinecraftClient client) {
		// Check if aspect overlay is enabled in config
		if (!CCLiveUtilitiesConfig.HANDLER.instance().aspectOverlayEnabled) {
			return; // Don't update overlay if aspect overlay is disabled
		}
		
		// Debug output
		// Find blueprint line and update overlay
		for (int i = 0; i < lines.size(); i++) {
			Text line = lines.get(i);
			String lineText = line.getString();
			
			// Skip if lineText is null or empty
			if (lineText == null || lineText.isEmpty()) {
				continue;
			}
			
			// Check if this is a blueprint line
			if (lineText.contains("[Bauplan]")) {
				
				// Update the overlay with this blueprint information
				AspectOverlay.updateFromBlueprintName(lineText);
				break; // Only process the first blueprint line
			}
		}
	}
	
	/**
	 * Gets the item under the mouse cursor (simplified implementation)
	 */
	private static ItemStack getHoveredItem(MinecraftClient client) {
		// This is a simplified approach - in a real implementation you'd need to
		// check the actual GUI elements and their item stacks
		// For now, we'll return an empty stack to indicate no item found
		return ItemStack.EMPTY;
	}
	
	/**
	 * Checks for aspect information in blueprint items and adds it to the tooltip
	 */
	private static void checkForAspectInformation(List<Text> lines, MinecraftClient client) {
		// Check if aspect overlay is enabled in config
		if (!CCLiveUtilitiesConfig.HANDLER.instance().aspectOverlayEnabled) {
			return; // Don't show aspect information if aspect overlay is disabled
		}
		
		// Find blueprint line and add aspect information
		for (int i = 0; i < lines.size(); i++) {
			Text line = lines.get(i);
			String lineText = line.getString();
			
			// Skip if lineText is null or empty
			if (lineText == null || lineText.isEmpty()) {
				continue;
			}
			
			// Check if this is a blueprint line
			if (lineText.contains("[Bauplan]")) {
				
				// Extract the item name (everything before "[Bauplan]")
				String itemName = lineText.substring(0, lineText.indexOf("[Bauplan]")).trim();
				
				// Remove leading dash/minus if present
				if (itemName.startsWith("-")) {
					itemName = itemName.substring(1).trim();
				}
				
				// Remove trailing dash/minus if present
				if (itemName.endsWith("-")) {
					itemName = itemName.substring(0, itemName.length() - 1).trim();
				}
				
				// Remove Minecraft formatting codes and Unicode characters
				itemName = itemName.replaceAll("§[0-9a-fk-or]", "");
				itemName = itemName.replaceAll("[\\u3400-\\u4DBF]", "");
				itemName = itemName.replaceAll("[^a-zA-ZäöüßÄÖÜ\\s]", "").trim();
				
				// Look for this item in the aspects database
				AspectInfo aspectInfo = aspectsDatabase.get(itemName);
				
				if (aspectInfo != null && !aspectInfo.aspectName.isEmpty()) {
					// Calculate position: 5th line from bottom (2 lines higher)
					int targetPosition = Math.max(0, lines.size() - 5);
					
					// Create aspect name text
					Text aspectNameText = Text.literal("Enthält: ")
						.styled(style -> style.withColor(0xFFFFFFFF)) // White color
						.append(Text.literal(aspectInfo.aspectName)
							.styled(style -> style.withColor(0xFFFCA800))); // Same color as overlay (#FCA800)
					
					// Insert aspect name at the target position
					lines.add(targetPosition, aspectNameText);
					
					// Add empty line after aspect name
					Text emptyLineText = Text.literal(" ");
					lines.add(targetPosition + 1, emptyLineText);
					
					// Add aspect description right after the empty line (only if shift is pressed)
					boolean isShiftPressed = InputUtil.isKeyPressed(client.getWindow().getHandle(), 
																   InputUtil.GLFW_KEY_LEFT_SHIFT) || 
											InputUtil.isKeyPressed(client.getWindow().getHandle(), 
																   InputUtil.GLFW_KEY_RIGHT_SHIFT);
					
					if (isShiftPressed && !aspectInfo.aspectDescription.isEmpty()) {
						Text aspectDescText = Text.literal("  ")
							.styled(style -> style.withColor(0xC0C0C0)) // Light gray
							.append(Text.literal(aspectInfo.aspectDescription)
								.styled(style -> style.withColor(0xFFFFFF))); // White color
						
						// Insert description after the empty line
						lines.add(targetPosition + 2, aspectDescText);
					}
					
					break; // Only process the first blueprint line
				}
			}
		}
	}
	
	/**
	 * Loads the materials database from materials.json
	 */
	private static void loadMaterialsDatabase() {
		try {
			// Load from mod resources
			var resource = FabricLoader.getInstance().getModContainer("cclive-utilities")
				.orElseThrow(() -> new RuntimeException("Mod container not found"))
				.findPath(MATERIALS_CONFIG_FILE)
				.orElseThrow(() -> new RuntimeException("Materials config file not found"));
			
			try (var inputStream = java.nio.file.Files.newInputStream(resource)) {
				try (var reader = new java.io.InputStreamReader(inputStream)) {
					JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
					JsonObject floors = json.getAsJsonObject("floors");
					
					for (String floorKey : floors.keySet()) {
						JsonObject floorData = floors.getAsJsonObject(floorKey);
						
						// Extract floor number
						int floorNumber = Integer.parseInt(floorKey.replace("floor_", ""));
						
						// Add mob name to database
						if (floorData.has("mob")) {
							String mobName = floorData.get("mob").getAsString();
							if (!mobName.isEmpty()) {
								materialsDatabase.put(mobName, new MaterialInfo(floorNumber, "mob", "WHITE"));
							}
						}
						
						// Add materials to database
						JsonObject materials = floorData.getAsJsonObject("materials");
						for (String rarityKey : materials.keySet()) {
							JsonObject rarityData = materials.getAsJsonObject(rarityKey);
							String color = rarityData.get("color").getAsString();
							com.google.gson.JsonArray materialsArray = rarityData.getAsJsonArray("materials");
							
							for (var element : materialsArray) {
								String materialName = element.getAsString();
								if (!materialName.isEmpty()) {
									materialsDatabase.put(materialName, new MaterialInfo(floorNumber, rarityKey, color));
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			System.err.println("Failed to load materials database: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * Loads the essences database from Essenz.json
	 */
	private static void loadEssencesDatabase() {
		try {
			// Load from mod resources
			var resource = FabricLoader.getInstance().getModContainer("cclive-utilities")
				.orElseThrow(() -> new RuntimeException("Mod container not found"))
				.findPath(ESSENCES_CONFIG_FILE)
				.orElseThrow(() -> new RuntimeException("Essences config file not found"));
			
			try (var inputStream = java.nio.file.Files.newInputStream(resource)) {
				try (var reader = new java.io.InputStreamReader(inputStream)) {
					JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
					com.google.gson.JsonArray essencesArray = json.getAsJsonArray("essences");
					
					for (var element : essencesArray) {
						JsonObject essenceData = element.getAsJsonObject();
						String name = essenceData.get("name").getAsString();
						int wave = essenceData.get("wave").getAsInt();
						
						// Store the full essence name including Tier information
						if (!name.isEmpty()) {
							essencesDatabase.put(name, new EssenceInfo(name, "Essenz", "Tier", wave));
						}
					}
				}
			}
		} catch (Exception e) {
			System.err.println("Failed to load essences database: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * Loads the aspects database from Aspekte.json
	 */
	private static void loadAspectsDatabase() {
		try {
			// Load from mod resources
			var resource = FabricLoader.getInstance().getModContainer("cclive-utilities")
				.orElseThrow(() -> new RuntimeException("Mod container not found"))
				.findPath(ASPECTS_CONFIG_FILE)
				.orElseThrow(() -> new RuntimeException("Aspects config file not found"));
			
			try (var inputStream = java.nio.file.Files.newInputStream(resource)) {
				try (var reader = new java.io.InputStreamReader(inputStream)) {
					JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
					
					for (String itemName : json.keySet()) {
						JsonObject itemData = json.getAsJsonObject(itemName);
						String aspectName = itemData.get("aspect_name").getAsString();
						String aspectDescription = itemData.get("aspect_description").getAsString();
						
						// Only store items that have aspect information
						if (!aspectName.isEmpty() || !aspectDescription.isEmpty()) {
							aspectsDatabase.put(itemName, new AspectInfo(aspectName, aspectDescription));
						}
					}
				}
			}
		} catch (Exception e) {
			System.err.println("Failed to load aspects database: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * Gets the custom hex color for a rarity level
	 */
	private static int getRarityColor(String rarity) {
		switch (rarity.toLowerCase()) {
			case "common":
				return 0xFFFFFF; // White
			case "uncommon":
				return 0x1EFC00; // #1EFC00
			case "rare":
				return 0x006FDA; // #006FDA
			case "epic":
				return 0xA134EB; // #A134EB
			case "legendary":
				return 0xFC7E00; // #FC7E00
			case "mob":
				return 0xFFFFFF; // White for mob names
			default:
				return 0x808080; // Gray
		}
	}
	
	/**
	 * Gets the formatting for a rarity level
	 * Note: Custom hex colors would be:
	 * - Uncommon: #1EFC00
	 * - Rare: #006FDA  
	 * - Epic: #A134EB
	 * - Legendary: #FC7E00
	 */
	private static Formatting getRarityFormatting(String rarity) {
		switch (rarity.toLowerCase()) {
			case "common":
				return Formatting.WHITE;
			case "uncommon":
				return Formatting.GREEN; // Would be #1EFC00
			case "rare":
				return Formatting.BLUE; // Would be #006FDA
			case "epic":
				return Formatting.LIGHT_PURPLE; // Would be #A134EB
			case "legendary":
				return Formatting.GOLD; // Would be #FC7E00
			default:
				return Formatting.GRAY;
		}
	}
	
	/**
	 * Data class to store material information
	 */
	private static class MaterialInfo {
		public final int floor;
		public final String rarity;
		public final String color;
		
		public MaterialInfo(int floor, String rarity, String color) {
			this.floor = floor;
			this.rarity = rarity;
			this.color = color;
		}
	}
	
	/**
	 * Data class to store essence information
	 */
	private static class EssenceInfo {
		public final String name;
		public final String type;
		public final String tier;
		public final int wave;
		
		public EssenceInfo(String name, String type, String tier, int wave) {
			this.name = name;
			this.type = type;
			this.tier = tier;
			this.wave = wave;
		}
	}
	
	/**
	 * Data class to store aspect information
	 */
	private static class AspectInfo {
		public final String aspectName;
		public final String aspectDescription;
		
		public AspectInfo(String aspectName, String aspectDescription) {
			this.aspectName = aspectName;
			this.aspectDescription = aspectDescription;
		}
	}
}

