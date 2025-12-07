package net.felix.utilities.Overall;

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.HoverEvent;
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
	
	// License tracking
	private static Map<String, LicenseInfo> licensesDatabase = new HashMap<>();
	private static final String LICENSES_CONFIG_FILE = "assets/cclive-utilities/Farmworld.json";
	
	// Gadget tracking - Maps: "name/alias" -> Map<"level", "location">
	private static Map<String, Map<String, String>> gadgetsDatabase = new HashMap<>();
	
	// Blueprint floor tracking - Maps: "blueprint name" -> floor number (e.g., "Drachenzahn" -> 1)
	private static Map<String, Integer> blueprintFloorMap = new HashMap<>();
	private static final String BLUEPRINTS_CONFIG_FILE = "assets/cclive-utilities/blueprints.json";

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
		
		// Load licenses database
		loadLicensesDatabase();
		
		// Load gadgets database
		loadGadgetsDatabase();
		
		// Load blueprints database for floor numbers
		loadBlueprintsDatabase();
		
		// Initialize aspect overlay and renderer
		AspectOverlay.initialize();
		AspectOverlayRenderer.initialize();
		
		// Register tooltip callback for material information
		ItemTooltipCallback.EVENT.register((stack, context, tooltipType, lines) -> {
			MinecraftClient client = MinecraftClient.getInstance();
			
			// Always process aspect information (even if informationenUtilityEnabled is off)
			// Add aspect name to tooltip (always visible) - works in inventories
			addAspectNameToTooltip(lines, client, stack);
			
			// Add floor number to blueprint names in inventories
			addFloorNumberToBlueprintNames(lines, client);
			
			// Only process other information if Informationen Utility is enabled in config
			if (!CCLiveUtilitiesConfig.HANDLER.instance().enableMod ||
				!CCLiveUtilitiesConfig.HANDLER.instance().informationenUtilityEnabled) {
				return;
			}
			
			// Check if we're in the special inventory (the one with 㬉)
			boolean isSpecialInventory = false;
			boolean isLicenseInventory = false;
			String screenTitle = "";
			if (client.currentScreen != null) {
				screenTitle = client.currentScreen.getTitle().getString();
				if (screenTitle.contains("㬉")) {
					isSpecialInventory = true;
				}
				// Check for license inventory character
				if (screenTitle.contains("ち")) {
					isLicenseInventory = true;
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
			
			// Check for license information in inventories with ち
			if (isLicenseInventory) {
				// Check if the hovered item is in one of the license slots
				if (isItemInLicenseSlot(stack, client)) {
					checkForLicenseInformation(lines, client);
				}
			}
			
			// Check for gadget information in "Module [Upgraden]" inventory
			boolean isModuleUpgradeInventory = false;
			boolean isEngineerInventory = false;
			if (client.currentScreen != null) {
				String cleanScreenTitle = screenTitle.replaceAll("§[0-9a-fk-or]", "")
					.replaceAll("[\\u3400-\\u4DBF]", "");
				if (cleanScreenTitle.contains("Module [Upgraden]")) {
					isModuleUpgradeInventory = true;
				}
				if (cleanScreenTitle.contains("[Ingenieur]")) {
					isEngineerInventory = true;
				}
			}
			
			if (isModuleUpgradeInventory) {
				checkForGadgetInformation(lines, false);
			}
			
			if (isEngineerInventory) {
				checkForGadgetInformation(lines, true);
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
	 * Adds aspect name to tooltip (in blueprint inventories only)
	 */
	public static void addAspectNameToTooltip(List<Text> lines, MinecraftClient client, ItemStack stack) {
		// Check if aspect overlay is enabled in config
		if (!CCLiveUtilitiesConfig.HANDLER.instance().aspectOverlayEnabled) {
			return; // Don't show aspect information if aspect overlay is disabled
		}
		
		boolean isInBlueprintInventory = false;
		
		// Check if we're in a blueprint inventory
		if (client.currentScreen != null) {
		String screenTitle = client.currentScreen.getTitle().getString();
		
		// Remove Minecraft formatting codes and Unicode characters for comparison
		String cleanScreenTitle = screenTitle.replaceAll("§[0-9a-fk-or]", "")
											.replaceAll("[\\u3400-\\u4DBF]", "");
		
		// Only show aspect information in specific blueprint inventories
			isInBlueprintInventory = cleanScreenTitle.contains("Baupläne [Waffen]") ||
									  cleanScreenTitle.contains("Baupläne [Rüstung]") ||
									  cleanScreenTitle.contains("Baupläne [Werkzeuge]") ||
									  cleanScreenTitle.contains("Bauplan [Shop]") || 
									  cleanScreenTitle.contains("Favorisierte [Rüstungsbaupläne]") ||
									  cleanScreenTitle.contains("Favorisierte [Waffenbaupläne]") ||
									  cleanScreenTitle.contains("CACTUS_CLICKER.blueprints.favorites.title.tools");
		}
		
		if (!isInBlueprintInventory) {
			return; // Don't show aspect information in other inventories
		}
		
		// Check tooltip lines for inventory tooltips (they contain "[Bauplan]")
		String itemNameToCheck = null;
		Text blueprintLine = null;
		for (int i = 0; i < lines.size(); i++) {
			Text line = lines.get(i);
			String lineText = line.getString();
			
			// Skip if lineText is null or empty
			if (lineText == null || lineText.isEmpty()) {
				continue;
			}
			
			// Check if this is a blueprint line
			if (lineText.contains("[Bauplan]")) {
				// For inventory tooltips, use the line text directly
				itemNameToCheck = lineText;
				blueprintLine = line;
				break;
			}
		}
		
		// Check if the blueprint line contains Epic colors - if so, don't show aspect info
		if (blueprintLine != null && hasEpicColor(blueprintLine)) {
			return; // Don't show aspect information for Epic blueprints
		}
		
		// If we found a blueprint item, add aspect information
		String itemName = null;
		if (itemNameToCheck != null && itemNameToCheck.contains("[Bauplan]")) {
				// Extract the item name (everything before "[Bauplan]")
			itemName = itemNameToCheck.substring(0, itemNameToCheck.indexOf("[Bauplan]")).trim();
				
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
		}
				
				// Look for this item in the aspects database
		if (itemName != null && !itemName.isEmpty()) {
				AspectInfo aspectInfo = aspectsDatabase.get(itemName);
				if (aspectInfo != null) {
				// Find the position of "Benötigt" line
				int benötigtPosition = -1;
				for (int i = 0; i < lines.size(); i++) {
					Text line = lines.get(i);
					String lineText = line.getString();
					if (lineText != null && lineText.contains("Benötigt")) {
						benötigtPosition = i;
						break;
					}
				}
				
				// If "Benötigt" not found, use fallback position (5th line from bottom)
				int targetPosition;
				if (benötigtPosition >= 0) {
					// Insert right before "Benötigt"
					targetPosition = benötigtPosition;
				} else {
					// Fallback: 5th line from bottom
					targetPosition = Math.max(0, lines.size() - 5);
				}
					
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
					
				// Add empty line after shift info (only if we're not inserting before "Benötigt")
				if (benötigtPosition < 0) {
					Text emptyLineText = Text.literal(" ");
					lines.add(targetPosition + 2, emptyLineText);
				}
			}
		}
	}
	
	/**
	 * Gets the aspect name for a blueprint item name
	 * @param blueprintName The cleaned blueprint name (without "[Bauplan]")
	 * @return The aspect name, or null if not found
	 */
	public static String getAspectInfoForBlueprint(String blueprintName) {
		if (blueprintName == null || blueprintName.isEmpty()) {
			return null;
		}
		
		// Remove any remaining formatting
		String cleanName = blueprintName.replaceAll("§[0-9a-fk-or]", "");
		cleanName = cleanName.replaceAll("[\\u3400-\\u4DBF]", "");
		cleanName = cleanName.replaceAll("[^a-zA-ZäöüßÄÖÜ\\s-]", "").trim();
		
		AspectInfo aspectInfo = aspectsDatabase.get(cleanName);
		if (aspectInfo != null) {
			return aspectInfo.aspectName;
		}
		
		return null;
	}
	
	/**
	 * Gets the full AspectInfo for a blueprint item name
	 * @param blueprintName The cleaned blueprint name (without "[Bauplan]")
	 * @return The AspectInfo, or null if not found
	 */
	public static AspectInfo getAspectInfoForBlueprintFull(String blueprintName) {
		if (blueprintName == null || blueprintName.isEmpty()) {
			return null;
		}
		
		// Remove any remaining formatting
		String cleanName = blueprintName.replaceAll("§[0-9a-fk-or]", "");
		cleanName = cleanName.replaceAll("[\\u3400-\\u4DBF]", "");
		cleanName = cleanName.replaceAll("[^a-zA-ZäöüßÄÖÜ\\s-]", "").trim();
		
		return aspectsDatabase.get(cleanName);
	}
	
	/**
	 * Modifies chat messages to add aspect information to hover events
	 * Called from ClientPlayNetworkHandlerTestMixin
	 * Adds aspect info and "Shift für Info" to blueprint hover events
	 * If chat aspect overlay is enabled, no lines are added to hover events
	 */
	public static Text modifyChatMessageForAspectInfo(Text message) {
		if (message == null) {
			return null;
		}
		
		// Check if message contains [Bauplan]
		String fullMessageText = message.getString();
		if (fullMessageText == null || !fullMessageText.contains("[Bauplan]")) {
			return null; // Not a blueprint message
		}
		
		// Check if aspect overlay is enabled
		if (!CCLiveUtilitiesConfig.HANDLER.instance().aspectOverlayEnabled) {
			return null; // Aspect overlay disabled
		}
		
		// If chat aspect overlay is disabled, don't add lines to hover events
		// When enabled, both hover event lines and overlay will be shown
		if (!CCLiveUtilitiesConfig.HANDLER.instance().chatAspectOverlayEnabled) {
			return null; // Chat aspect overlay disabled, don't modify hover event
		}
		
		// Extract blueprint name from message
		String blueprintName = extractBlueprintNameFromChatMessage(message);
		if (blueprintName == null || blueprintName.isEmpty()) {
			return null; // Could not extract blueprint name
		}
		
		// Get aspect information
		AspectInfo aspectInfo = getAspectInfoForBlueprintFull(blueprintName);
		if (aspectInfo == null || aspectInfo.aspectName == null || aspectInfo.aspectName.isEmpty()) {
			return null; // No aspect info found
		}
		
		// Find hover event in message
		HoverEvent hoverEvent = findHoverEventInText(message);
		if (hoverEvent == null || hoverEvent.getAction() != HoverEvent.Action.SHOW_TEXT) {
			return null; // No hover event found
		}
		
		// Extract original hover text
		Text originalHoverText = extractHoverTextFromEvent(hoverEvent, message);
		
		// Ensure aspectInfo.aspectName is not null or empty
		String aspectName = aspectInfo.aspectName != null && !aspectInfo.aspectName.isEmpty() ? aspectInfo.aspectName : "-";
		
		// Create new hover text with aspect info inserted above "Benötigt:" line
		Text newHoverText;
		try {
			if (originalHoverText != null) {
				// Find the line containing "Benötigt:" and get its style/color
				net.minecraft.text.Style benötigtStyle = findStyleForLineContaining(originalHoverText, "Benötigt");
				
				// Get the default color from the original text if no specific style found
				net.minecraft.text.Style defaultStyle = originalHoverText.getStyle();
				net.minecraft.text.Style infoStyle = benötigtStyle != null ? benötigtStyle : 
					(defaultStyle != null ? defaultStyle : net.minecraft.text.Style.EMPTY);
				
				// Create aspect info text with the same color as the original text
				// Extract color from the style
				final Integer textColor;
				if (infoStyle != null && infoStyle.getColor() != null) {
					textColor = infoStyle.getColor().getRgb();
				} else {
					textColor = null;
				}
				
				Text aspectNameText = Text.literal("Enthält: ")
					.styled(style -> style.withColor(0xFFFFFFFF)) // White color
					.append(Text.literal(aspectName)
						.styled(style -> style.withColor(0xFFFCA800))); // Aspect name in overlay color
				
				Text shiftInfoText = Text.literal("(Shift für mehr Info)")
					.styled(style -> style.withColor(0xFFCCCCCC)); // Light gray
				
				// Build the new text by iterating through OrderedText and inserting before "Benötigt:"
				// This preserves all formatting from the original text character by character
				net.minecraft.text.MutableText result = Text.empty();
				final boolean[] inserted = {false};
				java.util.List<Integer> currentLine = new java.util.ArrayList<>();
				final java.util.List<net.minecraft.text.Style> lineStyles = new java.util.ArrayList<>();
				
				net.minecraft.text.OrderedText orderedText = originalHoverText.asOrderedText();
				orderedText.accept((index, style, codePoint) -> {
					char ch = (char) codePoint;
					
					if (ch == '\n') {
						// End of line - check if it contains "Benötigt:"
						StringBuilder lineBuilder = new StringBuilder();
						for (int cp : currentLine) {
							lineBuilder.appendCodePoint(cp);
						}
						String lineText = lineBuilder.toString();
						
						if (!inserted[0] && lineText.contains("Benötigt")) {
							// Insert aspect info before this line
							result.append(aspectNameText);
							result.append(Text.literal("\n"));
							result.append(shiftInfoText);
							result.append(Text.literal("\n"));
							inserted[0] = true;
						}
						
						// Append the line character by character with formatting preserved
						for (int i = 0; i < currentLine.size(); i++) {
							int cp = currentLine.get(i);
							net.minecraft.text.Style charStyle = i < lineStyles.size() ? lineStyles.get(i) : style;
							net.minecraft.text.MutableText charText = Text.literal(new String(Character.toChars(cp)));
							if (charStyle != null) {
								charText = charText.styled(s -> charStyle);
							}
							result.append(charText);
						}
						result.append(Text.literal("\n"));
						currentLine.clear();
						lineStyles.clear();
					} else {
						currentLine.add(codePoint);
						lineStyles.add(style);
					}
					
					return true;
				});
				
				// Handle the last line
				for (int i = 0; i < currentLine.size(); i++) {
					int cp = currentLine.get(i);
					net.minecraft.text.Style charStyle = i < lineStyles.size() ? lineStyles.get(i) : null;
					net.minecraft.text.MutableText charText = Text.literal(new String(Character.toChars(cp)));
					if (charStyle != null) {
						charText = charText.styled(s -> charStyle);
					}
					result.append(charText);
				}
				
				// If "Benötigt:" was not found, append at the end
				if (inserted[0] == false) {
					result.append(Text.literal("\n"));
					result.append(aspectNameText);
					result.append(Text.literal("\n"));
					result.append(shiftInfoText);
				}
				
				newHoverText = result;
			} else {
				// Could not extract original text, create new one with just aspect info
				Text aspectNameText = Text.literal("Enthält: ")
					.styled(style -> style.withColor(0xFFFFFFFF)) // White color
					.append(Text.literal(aspectName)
						.styled(style -> style.withColor(0xFFFCA800))); // Same color as overlay (#FCA800)
				
				Text shiftInfoText = Text.literal("(Shift für mehr Info)")
					.styled(style -> style.withColor(0xFFCCCCCC)); // Light gray color
				
				newHoverText = aspectNameText.copy()
					.append(Text.literal("\n"))
					.append(shiftInfoText);
			}
			
			// Validate that newHoverText is not null
			if (newHoverText == null) {
				return null;
			}
		} catch (Exception e) {
			return null;
		}
		
		// Create new hover event
		HoverEvent newHoverEvent = createHoverEventForAspect(newHoverText);
		if (newHoverEvent == null) {
			return null; // Failed to create hover event
		}
		
		// Modify message with new hover event (preserving all formatting)
		Text modified = modifyTextWithHoverEvent(message, newHoverEvent);
		return modified;
	}
	
	/**
	 * Finds the style of the line containing the specified text
	 */
	private static net.minecraft.text.Style findStyleForLineContaining(Text text, String searchText) {
		if (text == null || searchText == null) {
			return null;
		}
		
		// Use OrderedText to iterate through the text and find the style at the position
		net.minecraft.text.OrderedText orderedText = text.asOrderedText();
		final net.minecraft.text.Style[] foundStyle = {null};
		final java.lang.StringBuilder currentLine = new java.lang.StringBuilder();
		
		orderedText.accept((index, style, codePoint) -> {
			char ch = (char) codePoint;
			
			if (ch == '\n') {
				// Check if current line contains the search text
				if (currentLine.toString().contains(searchText) && foundStyle[0] == null) {
					foundStyle[0] = style;
					return false; // Stop searching
				}
				currentLine.setLength(0); // Clear for next line
			} else {
				currentLine.appendCodePoint(codePoint);
				// Check if we found it in the current line
				if (currentLine.toString().contains(searchText) && foundStyle[0] == null) {
					foundStyle[0] = style;
					return false; // Stop searching
				}
			}
			
			return true; // Continue
		});
		
		return foundStyle[0];
	}
	
	/**
	 * Splits a Text component into individual lines while preserving formatting
	 */
	private static java.util.List<Text> splitTextIntoLinesWithFormatting(Text text) {
		java.util.List<Text> lines = new java.util.ArrayList<>();
		if (text == null) {
			return lines;
		}
		
		// Use OrderedText to preserve formatting while splitting
		net.minecraft.text.OrderedText orderedText = text.asOrderedText();
		java.util.List<java.lang.StringBuilder> lineBuilders = new java.util.ArrayList<>();
		java.util.List<net.minecraft.text.Style> lineStyles = new java.util.ArrayList<>();
		lineBuilders.add(new java.lang.StringBuilder());
		lineStyles.add(null);
		
		final int[] currentLineIndex = {0};
		final net.minecraft.text.Style[] currentStyle = {null};
		
		orderedText.accept((index, style, codePoint) -> {
			char ch = (char) codePoint;
			
			// Update current style
			currentStyle[0] = style;
			
			if (ch == '\n') {
				// Start a new line
				currentLineIndex[0]++;
				lineBuilders.add(new java.lang.StringBuilder());
				lineStyles.add(style); // Store style for the new line
			} else {
				// Append character to current line
				if (lineBuilders.size() <= currentLineIndex[0]) {
					lineBuilders.add(new java.lang.StringBuilder());
					lineStyles.add(style);
				}
				lineBuilders.get(currentLineIndex[0]).appendCodePoint(codePoint);
				// Update style for current line (use the last style in the line)
				if (lineStyles.size() > currentLineIndex[0]) {
					lineStyles.set(currentLineIndex[0], style);
				}
			}
			
			return true;
		});
		
		// Build Text objects for each line with preserved formatting
		for (int i = 0; i < lineBuilders.size(); i++) {
			String lineContent = lineBuilders.get(i).toString();
			net.minecraft.text.Style lineStyle = i < lineStyles.size() ? lineStyles.get(i) : null;
			
			if (lineContent.isEmpty()) {
				lines.add(Text.empty());
			} else {
				// Create line text - we'll preserve the style information separately
				// For now, create simple text (formatting is complex to preserve exactly)
				lines.add(Text.literal(lineContent));
			}
		}
		
		return lines;
	}
	
	/**
	 * Updates aspect overlay from blueprint name (for chat messages)
	 */
	public static void updateAspectOverlayFromBlueprintName(String blueprintName) {
		if (blueprintName == null || blueprintName.isEmpty()) {
			AspectOverlay.onHoverStopped();
			return;
		}
		
		// Get aspect information
		AspectInfo aspectInfo = getAspectInfoForBlueprintFull(blueprintName);
		
		if (aspectInfo == null) {
			AspectOverlay.onHoverStopped();
			return;
		}
		
		// Clean the blueprint name
		String cleanItemName = blueprintName.trim();
		
		// Remove leading dash/minus if present
		if (cleanItemName.startsWith("-")) {
			cleanItemName = cleanItemName.substring(1).trim();
		}
		
		// Remove trailing dash/minus if present
		if (cleanItemName.endsWith("-")) {
			cleanItemName = cleanItemName.substring(0, cleanItemName.length() - 1).trim();
		}
		
		// Remove Minecraft formatting codes and Unicode characters
		cleanItemName = cleanItemName.replaceAll("§[0-9a-fk-or]", "");
		cleanItemName = cleanItemName.replaceAll("[\\u3400-\\u4DBF]", "");
		cleanItemName = cleanItemName.replaceAll("[^a-zA-ZäöüßÄÖÜ\\s-]", "").trim();
		
		// Update overlay
		AspectOverlay.updateAspectInfoFromName(cleanItemName, aspectInfo);
	}
	
	/**
	 * Updates aspect overlay from blueprint name with Text object (for color checking)
	 */
	public static void updateAspectOverlayFromBlueprintName(String blueprintName, Text textWithColor) {
		// Check if the text contains Epic colors - if so, don't show overlay
		if (textWithColor != null && hasEpicColor(textWithColor)) {
			AspectOverlay.onHoverStopped();
			return;
		}
		
		// Otherwise, proceed with normal update
		updateAspectOverlayFromBlueprintName(blueprintName);
	}
	
	/**
	 * Extracts item name from hover event in chat message
	 * Works for all items, not just blueprints
	 * Returns the first line of the hover event text, which is typically the item name
	 */
	public static String extractItemNameFromHoverEvent(Text message) {
		if (message == null) {
			return null;
		}
		
		// Find hover event in message
		HoverEvent hoverEvent = findHoverEventInText(message);
		if (hoverEvent == null || hoverEvent.getAction() != HoverEvent.Action.SHOW_TEXT) {
			return null;
		}
		
		// Extract hover text from event
		Text hoverText = extractHoverTextFromEvent(hoverEvent, message);
		if (hoverText == null) {
			return null;
		}
		
		// Get the first line of the hover text (typically the item name)
		String hoverTextString = hoverText.getString();
		if (hoverTextString == null || hoverTextString.isEmpty()) {
			return null;
		}
		
		// Split by newline and take the first line
		String[] lines = hoverTextString.split("\n");
		if (lines.length == 0) {
			return null;
		}
		
		String itemName = lines[0].trim();
		
		// Clean the item name
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
		
		// Remove "Du erhältst" if present
		if (itemName.contains("Du erhältst")) {
			int erhältstIndex = itemName.indexOf("Du erhältst");
			if (erhältstIndex >= 0) {
				itemName = itemName.substring(erhältstIndex + "Du erhältst".length()).trim();
			}
		}
		
		// Remove quantity patterns like "1x ", "2x ", "10x " at the start
		itemName = itemName.replaceAll("^\\d+x\\s+", "");
		
		if (itemName.isEmpty()) {
			return null;
		}
		
		return itemName;
	}
	
	/**
	 * Data class to store blueprint name and color information
	 */
	public static class BlueprintNameAndColor {
		public final String name;
		public final String rarity; // "epic" or "legendary" based on color
		
		public BlueprintNameAndColor(String name, String rarity) {
			this.name = name;
			this.rarity = rarity;
		}
	}
	
	/**
	 * Extracts blueprint name and color from chat message text
	 * Returns the name and rarity (epic or legendary) based on color
	 * Epic: #A134EB, Legendary: #FC7E00
	 */
	public static BlueprintNameAndColor extractBlueprintNameAndColorFromChatMessage(Text message) {
		if (message == null) {
			return null;
		}
		
		// Target colors: 
		// Epic: #A134EB (RGB: 161, 52, 235) OR #A335EE (RGB: 163, 53, 238) - both are used in game
		// Legendary: #FC7E00 (RGB: 252, 126, 0) OR #FF8000 (RGB: 255, 128, 0) - both are used in game
		int epicColor1 = 0xFFA134EB; // Original epic color
		int epicColor2 = 0xFFA335EE; // Actual epic color used in game
		int legendaryColor1 = 0xFFFC7E00; // Original legendary color
		int legendaryColor2 = 0xFFFF8000; // Actual legendary color used in game
		int epicColor1RGB = epicColor1 & 0x00FFFFFF;
		int epicColor2RGB = epicColor2 & 0x00FFFFFF;
		int legendaryColor1RGB = legendaryColor1 & 0x00FFFFFF;
		int legendaryColor2RGB = legendaryColor2 & 0x00FFFFFF;
		
		final StringBuilder blueprintName = new StringBuilder();
		final boolean[] inBlueprintNameColor = {false};
		final StringBuilder[] currentSection = {new StringBuilder()};
		final String[] detectedRarity = {null};
		
		// Convert to OrderedText to iterate through styled parts
		net.minecraft.text.OrderedText orderedText = message.asOrderedText();
		orderedText.accept((index, style, codePoint) -> {
			char ch = (char) codePoint;
			
			// Check if this style has one of the target colors
			boolean hasTargetColor = false;
			String currentRarity = null;
			if (style != null && style.getColor() != null) {
				int styleColor = style.getColor().getRgb();
				int styleColorRGB = styleColor & 0x00FFFFFF; // Remove alpha
				
				// Check for Epic color (both variants)
				if (styleColorRGB == epicColor1RGB || styleColorRGB == epicColor2RGB) {
					hasTargetColor = true;
					currentRarity = "epic";
				} else if (styleColorRGB == legendaryColor1RGB || styleColorRGB == legendaryColor2RGB) {
					hasTargetColor = true;
					currentRarity = "legendary";
				}
			}
			
			if (hasTargetColor) {
				// This text has the blueprint name color
				if (!inBlueprintNameColor[0]) {
					// Starting a new section
					inBlueprintNameColor[0] = true;
					currentSection[0] = new StringBuilder();
					if (detectedRarity[0] == null) {
						detectedRarity[0] = currentRarity;
					}
				}
				currentSection[0].appendCodePoint(codePoint);
			} else {
				// Color changed
				if (inBlueprintNameColor[0]) {
					// We were collecting blueprint name
					String section = currentSection[0].toString();
					
					// Check if this section matches "ZAHL x" pattern (e.g., "1x ", "2x ", "10x ")
					if (!section.matches("^\\d+x\\s*$")) {
						// Not a "ZAHL x" pattern, add to blueprint name
						if (blueprintName.length() > 0) {
							blueprintName.append(" ");
						}
						blueprintName.append(section);
					}
					
					currentSection[0] = new StringBuilder();
					inBlueprintNameColor[0] = false;
				}
			}
			
			return true; // Continue processing
		});
		
		// Handle last section if we were still collecting
		if (inBlueprintNameColor[0] && currentSection[0].length() > 0) {
			String section = currentSection[0].toString();
			if (!section.matches("^\\d+x\\s*$")) {
				if (blueprintName.length() > 0) {
					blueprintName.append(" ");
				}
				blueprintName.append(section);
			}
		}
		
		String result = blueprintName.toString().trim();
		
		// Clean up: remove leading/trailing dashes and spaces
		result = result.replaceAll("^[-\\s]+", "").replaceAll("[-\\s]+$", "");
		
		// Remove "Du erhältst" if present
		if (result.contains("Du erhältst")) {
			int erhältstIndex = result.indexOf("Du erhältst");
			if (erhältstIndex >= 0) {
				result = result.substring(erhältstIndex + "Du erhältst".length()).trim();
			}
		}
		
		// Return the result if we found a name with a target color
		if (!result.isEmpty() && detectedRarity[0] != null) {
			return new BlueprintNameAndColor(result, detectedRarity[0]);
		}
		
		return null;
	}
	
	/**
	 * Extracts blueprint name and color from item name text (for inventory)
	 * Returns the name and rarity (epic or legendary) based on color
	 * Epic: #A134EB or #A335EE, Legendary: #FC7E00 or #FF8000
	 * Only considers the color of the blueprint name itself (before "-" and "[Bauplan]")
	 */
	public static BlueprintNameAndColor extractBlueprintNameAndColorFromItemName(Text itemNameText) {
		if (itemNameText == null) {
			return null;
		}
		
		// Target colors: 
		// Epic: #A134EB (RGB: 161, 52, 235) OR #A335EE (RGB: 163, 53, 238) - both are used in game
		// Legendary: #FC7E00 (RGB: 252, 126, 0) OR #FF8000 (RGB: 255, 128, 0) - both are used in game
		int epicColor1 = 0xFFA134EB; // Original epic color
		int epicColor2 = 0xFFA335EE; // Actual epic color used in game
		int legendaryColor1 = 0xFFFC7E00; // Original legendary color
		int legendaryColor2 = 0xFFFF8000; // Actual legendary color used in game
		int epicColor1RGB = epicColor1 & 0x00FFFFFF;
		int epicColor2RGB = epicColor2 & 0x00FFFFFF;
		int legendaryColor1RGB = legendaryColor1 & 0x00FFFFFF;
		int legendaryColor2RGB = legendaryColor2 & 0x00FFFFFF;
		
		// First, collect all text with their styles to find the blueprint name section
		final java.util.List<net.minecraft.text.Style> styles = new java.util.ArrayList<>();
		final java.util.List<Integer> codePoints = new java.util.ArrayList<>();
		final StringBuilder fullText = new StringBuilder();
		
		net.minecraft.text.OrderedText orderedText = itemNameText.asOrderedText();
		orderedText.accept((index, style, codePoint) -> {
			styles.add(style);
			codePoints.add(codePoint);
			fullText.appendCodePoint(codePoint);
			return true;
		});
		
		// Find the blueprint name section (BEFORE "-" and "[Bauplan]")
		String fullTextStr = fullText.toString();
		int dashIndex = -1;
		int bauplanIndex = -1;
		
		// Find the dash before [Bauplan]
		int bauplanStart = fullTextStr.indexOf("[Bauplan]");
		if (bauplanStart > 0) {
			// Look for dash before [Bauplan]
			for (int i = bauplanStart - 1; i >= 0; i--) {
				char ch = fullTextStr.charAt(i);
				if (ch == '-' || ch == '—' || ch == '–') {
					dashIndex = i;
					break;
				} else if (ch != ' ') {
					break; // Stop if we hit non-space, non-dash character
				}
			}
			bauplanIndex = bauplanStart;
		}
		
		if (dashIndex < 0 || bauplanIndex < 0) {
			return null; // Could not find blueprint name section
		}
		
		// The blueprint name is BEFORE the dash, not between dash and [Bauplan]
		// So we need to extract from position 0 to dashIndex (excluding trailing spaces)
		
		// Extract the blueprint name section and check its color
		// The blueprint name is BEFORE the dash, so we extract from position 0 to dashIndex
		final StringBuilder blueprintName = new StringBuilder();
		final String[] detectedRarity = {null};
		
		// Build a mapping from string position to codePoint index
		int stringPos = 0;
		for (int i = 0; i < codePoints.size(); i++) {
			int codePoint = codePoints.get(i);
			net.minecraft.text.Style style = styles.get(i);
			
			// Check if this codePoint is in the blueprint name section (BEFORE the dash)
			if (stringPos < dashIndex) {
				char ch = (char) codePoint;
				
				blueprintName.appendCodePoint(codePoint);
				
				// Check the color only for the blueprint name section
				if (style != null && style.getColor() != null) {
					int styleColor = style.getColor().getRgb();
					int styleColorRGB = styleColor & 0x00FFFFFF; // Remove alpha
					if ((styleColorRGB == epicColor1RGB || styleColorRGB == epicColor2RGB) && detectedRarity[0] == null) {
						detectedRarity[0] = "epic";
					} else if ((styleColorRGB == legendaryColor1RGB || styleColorRGB == legendaryColor2RGB) && detectedRarity[0] == null) {
						detectedRarity[0] = "legendary";
					}
				}
			} else {
				// Stop if we've reached the dash
				if (stringPos >= dashIndex) {
					break;
				}
			}
			
			// Update string position based on codePoint
			if (codePoint < 0x10000) {
				stringPos++;
			} else {
				stringPos += 2; // Surrogate pair
			}
		}
		
		String result = blueprintName.toString().trim();
		
		// Remove Unicode formatting characters
		result = result.replaceAll("[\\u3400-\\u4DBF\\u4E00-\\u9FFF]", "");
		
		// Remove any remaining formatting codes
		result = result.replaceAll("§[0-9a-fk-or]", "");
		
		// Remove trailing spaces, dashes, and other common suffixes
		result = result.replaceAll("\\s*-\\s*$", "").trim();
		
		// Return the result if we found a name with a target color
		if (!result.isEmpty() && detectedRarity[0] != null) {
			return new BlueprintNameAndColor(result, detectedRarity[0]);
		}
		
		return null;
	}
	
	/**
	 * Checks if a Text object contains any of the Epic colors (#A134EB or #A335EE)
	 * Returns true if any part of the text has one of these colors
	 */
	public static boolean hasEpicColor(Text text) {
		if (text == null) {
			return false;
		}
		
		// Epic colors: #A134EB (RGB: 161, 52, 235) OR #A335EE (RGB: 163, 53, 238)
		int epicColor1 = 0xFFA134EB;
		int epicColor2 = 0xFFA335EE;
		int epicColor1RGB = epicColor1 & 0x00FFFFFF;
		int epicColor2RGB = epicColor2 & 0x00FFFFFF;
		
		// Convert to OrderedText to iterate through styled parts
		net.minecraft.text.OrderedText orderedText = text.asOrderedText();
		final boolean[] hasEpicColor = {false};
		
		orderedText.accept((index, style, codePoint) -> {
			if (style != null && style.getColor() != null) {
				int styleColor = style.getColor().getRgb();
				int styleColorRGB = styleColor & 0x00FFFFFF; // Remove alpha
				
				if (styleColorRGB == epicColor1RGB || styleColorRGB == epicColor2RGB) {
					hasEpicColor[0] = true;
					return false; // Stop processing
				}
			}
			return true; // Continue processing
		});
		
		return hasEpicColor[0];
	}
	
	/**
	 * Extracts blueprint name from chat message text
	 * Looks for text with color #FC7E00 or #FF8000, but ignores "ZAHL x" patterns
	 * Works for all items with this color, not just blueprints
	 */
	public static String extractBlueprintNameFromChatMessage(Text message) {
		if (message == null) {
			return null;
		}
		
		// Target colors: #FC7E00 (RGB: 252, 126, 0) OR #FF8000 (RGB: 255, 128, 0) - both are used in game
		int targetColor1 = 0xFFFC7E00; // Original legendary color
		int targetColor2 = 0xFFFF8000; // Alternative legendary color
		int targetColor1RGB = targetColor1 & 0x00FFFFFF; // Remove alpha
		int targetColor2RGB = targetColor2 & 0x00FFFFFF; // Remove alpha
		
		StringBuilder blueprintName = new StringBuilder();
		final boolean[] inBlueprintNameColor = {false};
		final StringBuilder[] currentSection = {new StringBuilder()};
		
		// Convert to OrderedText to iterate through styled parts
		net.minecraft.text.OrderedText orderedText = message.asOrderedText();
		orderedText.accept((index, style, codePoint) -> {
			// Check if this style has one of the target colors
			boolean hasTargetColor = false;
			if (style != null && style.getColor() != null) {
				int styleColor = style.getColor().getRgb();
				int styleColorRGB = styleColor & 0x00FFFFFF; // Remove alpha
				if (styleColorRGB == targetColor1RGB || styleColorRGB == targetColor2RGB) {
					hasTargetColor = true;
				}
			}
			
			if (hasTargetColor) {
				// This text has the blueprint name color
				if (!inBlueprintNameColor[0]) {
					// Starting a new section
					inBlueprintNameColor[0] = true;
					currentSection[0] = new StringBuilder();
				}
				currentSection[0].appendCodePoint(codePoint);
			} else {
				// Color changed
				if (inBlueprintNameColor[0]) {
					// We were collecting blueprint name
					String section = currentSection[0].toString();
					
					// Check if this section matches "ZAHL x" pattern (e.g., "1x ", "2x ", "10x ")
					// Pattern: starts with digits, followed by "x" and optional space
					if (!section.matches("^\\d+x\\s*$")) {
						// Not a "ZAHL x" pattern, add to blueprint name
						if (blueprintName.length() > 0) {
							blueprintName.append(" ");
						}
						blueprintName.append(section);
					}
					
					currentSection[0] = new StringBuilder();
					inBlueprintNameColor[0] = false;
				}
			}
			
			return true; // Continue processing
		});
		
		// Handle last section if we were still collecting
		if (inBlueprintNameColor[0] && currentSection[0].length() > 0) {
			String section = currentSection[0].toString();
			if (!section.matches("^\\d+x\\s*$")) {
				if (blueprintName.length() > 0) {
					blueprintName.append(" ");
				}
				blueprintName.append(section);
			}
		}
		
		String result = blueprintName.toString().trim();
		
		// Clean up: remove leading/trailing dashes and spaces
		result = result.replaceAll("^[-\\s]+", "").replaceAll("[-\\s]+$", "");
		
		// Remove "Du erhältst" if present
		if (result.contains("Du erhältst")) {
			int erhältstIndex = result.indexOf("Du erhältst");
			if (erhältstIndex >= 0) {
				result = result.substring(erhältstIndex + "Du erhältst".length()).trim();
			}
		}
		
		// Remove "+ " prefix if present (e.g., "+ 1x Item")
		result = result.replaceAll("^\\+\\s*", "");
		
		// Remove quantity patterns like "1x ", "2x ", "10x " at the start
		// This handles cases where the entire section is "1x ItemName" in the same color
		result = result.replaceAll("^\\d+x\\s+", "");
		
		// Return the result if we found a name with the target color
		if (!result.isEmpty()) {
			return result;
		}
		
		return null;
	}
	
	/**
	 * Modifies a message's hover event to include aspect information
	 * Returns a new Text with modified hover event
	 */
	private static Text modifyMessageHoverEvent(Text message, AspectInfo aspectInfo) {
		if (message == null || aspectInfo == null) {
			return message;
		}
		
		// Check if there's an existing hover event (we can't read it, but we know it exists)
		HoverEvent existingHoverEvent = findHoverEventInText(message);
		boolean hasExistingHover = existingHoverEvent != null && existingHoverEvent.getAction() == HoverEvent.Action.SHOW_TEXT;
		
		// In Minecraft 1.21.7, we cannot read the existing hover text, so we create a new one
		// with our aspect information. The original hover text will be replaced.
		
		// Create aspect information text
		Text aspectNameText = Text.literal("Enthält: ")
			.styled(style -> style.withColor(0xFFFFFFFF)) // White color
			.append(Text.literal(aspectInfo.aspectName)
				.styled(style -> style.withColor(0xFFFCA800))); // Same color as overlay (#FCA800)
		
		Text shiftInfoText = Text.literal("(Shift für mehr Info)")
			.styled(style -> style.withColor(0xFFCCCCCC)); // Light gray color
		
		// Create new hover text with our information
		// Note: Original hover text cannot be preserved in 1.21.7
		Text newHoverText = aspectNameText.copy()
			.append(Text.literal("\n"))
			.append(shiftInfoText);
		
		// Create new HoverEvent directly (constructor works at runtime even if not visible at compile time)
		HoverEvent newHoverEvent = createHoverEventDirect(newHoverText);
		
		if (newHoverEvent == null) {
			return message;
		}
		
		// Create new text with modified hover event
		Text modified = modifyTextWithHoverEvent(message, newHoverEvent);
		return modified;
	}
	
	/**
	 * Extracts the tooltip from a SHOW_ITEM HoverEvent by getting the ItemStack and generating its tooltip
	 */
	private static Text extractTooltipFromShowItem(HoverEvent hoverEvent) {
		if (hoverEvent == null || hoverEvent.getAction() != HoverEvent.Action.SHOW_ITEM) {
			return null;
		}
		
		try {
			// Get the ShowItem value using method_10892 (same as ShowText)
			Object showItemValue = getShowTextValue(hoverEvent);
			if (showItemValue == null) {
				return null;
			}
			
			// Try to extract ItemStack from ShowItem
			Class<?> showItemClass = showItemValue.getClass();
			
			// Try all methods that might return ItemStack
			java.lang.reflect.Method[] methods = showItemClass.getDeclaredMethods();
			for (java.lang.reflect.Method method : methods) {
				if (method.getParameterCount() == 0 && !method.getReturnType().isPrimitive()) {
					try {
						method.setAccessible(true);
						Object result = method.invoke(showItemValue);
						if (result != null && result instanceof net.minecraft.item.ItemStack) {
							net.minecraft.item.ItemStack itemStack = (net.minecraft.item.ItemStack) result;
							
							// Get tooltip from ItemStack
							net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
							if (client != null && client.player != null) {
								// Use the same approach as SchmiedTrackerUtility - get tooltip via DataComponentTypes
								java.util.List<Text> tooltip = new java.util.ArrayList<>();
								// Add item name
								tooltip.add(itemStack.getName());
								
								// Get lore from DataComponentTypes (1.21.7 API)
								try {
									var loreComponent = itemStack.get(net.minecraft.component.DataComponentTypes.LORE);
									if (loreComponent != null) {
										tooltip.addAll(loreComponent.lines());
									}
								} catch (Exception e) {
									// Ignore
								}
								
								if (!tooltip.isEmpty()) {
									// Combine all tooltip lines into one Text
									net.minecraft.text.MutableText combined = Text.empty();
									boolean first = true;
									for (Text line : tooltip) {
										if (!first) {
											combined.append(Text.literal("\n"));
										}
										combined.append(line);
										first = false;
									}
									return combined;
								}
							}
						}
					} catch (Exception e) {
						// Continue to next method
					}
				}
			}
			
			// Try all fields that might contain ItemStack
			java.lang.reflect.Field[] fields = showItemClass.getDeclaredFields();
			for (java.lang.reflect.Field field : fields) {
				if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
					continue;
				}
				Class<?> fieldType = field.getType();
				if (fieldType == net.minecraft.item.ItemStack.class || fieldType.getName().contains("ItemStack")) {
					try {
						field.setAccessible(true);
						Object value = field.get(showItemValue);
						if (value instanceof net.minecraft.item.ItemStack) {
							net.minecraft.item.ItemStack itemStack = (net.minecraft.item.ItemStack) value;
							
							// Get tooltip from ItemStack
							net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
							if (client != null && client.player != null) {
								// Use the same approach as SchmiedTrackerUtility - get tooltip via DataComponentTypes
								java.util.List<Text> tooltip = new java.util.ArrayList<>();
								// Add item name
								tooltip.add(itemStack.getName());
								
								// Get lore from DataComponentTypes (1.21.7 API)
								try {
									var loreComponent = itemStack.get(net.minecraft.component.DataComponentTypes.LORE);
									if (loreComponent != null) {
										tooltip.addAll(loreComponent.lines());
									}
								} catch (Exception e) {
									// Ignore
								}
								
								if (!tooltip.isEmpty()) {
									// Combine all tooltip lines into one Text
									net.minecraft.text.MutableText combined = Text.empty();
									boolean first = true;
									for (Text line : tooltip) {
										if (!first) {
											combined.append(Text.literal("\n"));
										}
										combined.append(line);
										first = false;
									}
									return combined;
								}
							}
						}
					} catch (Exception e) {
						// Continue to next field
					}
				}
			}
		} catch (Exception e) {
			// Ignore
		}
		
		return null;
	}
	
	/**
	 * Recursively searches for a HoverEvent in a Text component and its siblings
	 */
	private static HoverEvent findHoverEventInText(Text text) {
		if (text == null) {
			return null;
		}
		
		// Check the main text's style
		if (text.getStyle() != null) {
			HoverEvent hoverEvent = text.getStyle().getHoverEvent();
			if (hoverEvent != null) {
				// Accept both SHOW_TEXT and SHOW_ITEM
				if (hoverEvent.getAction() == HoverEvent.Action.SHOW_TEXT || hoverEvent.getAction() == HoverEvent.Action.SHOW_ITEM) {
					return hoverEvent;
				}
			}
		}
		
		// Recursively check siblings
		for (Text sibling : text.getSiblings()) {
			HoverEvent siblingHoverEvent = findHoverEventInText(sibling);
			if (siblingHoverEvent != null) {
				return siblingHoverEvent;
			}
		}
		
		return null;
	}
	
	/**
	 * Extracts the Text from a HoverEvent using multiple approaches
	 * Note: In Minecraft 1.21.7, the text is not stored in the HoverEvent itself,
	 * but is generated dynamically. We try to extract it from the ShowText object.
	 */
	private static Text extractHoverTextFromEvent(HoverEvent hoverEvent, Text message) {
		// Try to extract from HoverEvent first (this is what we want - the actual tooltip text)
		Text result = extractHoverTextFromEvent(hoverEvent);
		if (result != null) {
			return result;
		}
		
		// Fallback: If we can't extract from HoverEvent, try to extract from the message itself
		// (but this is NOT what we want - we want the tooltip text, not the chat message text)
		if (message != null) {
			Text textWithHover = findTextComponentWithHoverEvent(message, hoverEvent);
			if (textWithHover != null) {
				return textWithHover.copy();
			}
		}
		
		return null;
	}
	
	/**
	 * Finds the Text component in a message that has the specified HoverEvent
	 */
	private static Text findTextComponentWithHoverEvent(Text message, HoverEvent targetHoverEvent) {
		if (message == null || targetHoverEvent == null) {
			return null;
		}
		
		// Check if this text component has the hover event
		if (message.getStyle() != null && message.getStyle().getHoverEvent() == targetHoverEvent) {
			return message;
		}
		
		// Recursively check siblings
		for (Text sibling : message.getSiblings()) {
			Text result = findTextComponentWithHoverEvent(sibling, targetHoverEvent);
			if (result != null) {
				return result;
			}
		}
		
		return null;
	}
	
	/**
	 * Extracts the Text from a HoverEvent using multiple approaches
	 * Note: In Minecraft 1.21.7, the text is not stored in the HoverEvent itself,
	 * but is generated dynamically. We try to extract it from the ShowText object.
	 */
	private static Text extractHoverTextFromEvent(HoverEvent hoverEvent) {
		if (hoverEvent == null) {
			return null;
		}
		
		HoverEvent.Action action = hoverEvent.getAction();
		
		// Only handle SHOW_TEXT for now
		if (action != HoverEvent.Action.SHOW_TEXT) {
			return null;
		}
		
		// Try HoverEvent#getValue(Action) - the official API method
		try {
			java.lang.reflect.Method getValueMethod = HoverEvent.class.getDeclaredMethod("getValue", HoverEvent.Action.class);
			getValueMethod.setAccessible(true);
			Object value = getValueMethod.invoke(hoverEvent, HoverEvent.Action.SHOW_TEXT);
			if (value instanceof Text) {
				return (Text) value;
			}
		} catch (Exception e) {
			// Ignore
		}
		
		// Try method getValue() without parameters (might exist in some versions)
		try {
			java.lang.reflect.Method getValueMethod = HoverEvent.class.getDeclaredMethod("getValue");
			getValueMethod.setAccessible(true);
			Object value = getValueMethod.invoke(hoverEvent);
			if (value instanceof Text) {
				return (Text) value;
			}
		} catch (Exception e) {
			// Ignore
		}
		
		// Try method value() (for records)
		try {
			java.lang.reflect.Method valueMethod = HoverEvent.class.getDeclaredMethod("value");
			valueMethod.setAccessible(true);
			Object value = valueMethod.invoke(hoverEvent);
			if (value instanceof Text) {
				return (Text) value;
			}
		} catch (Exception e) {
			// Ignore
		}
		
		// Try reading all fields in HoverEvent
		try {
			java.lang.reflect.Field[] fields = HoverEvent.class.getDeclaredFields();
			for (java.lang.reflect.Field field : fields) {
				// Skip static fields
				if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
					continue;
				}
				
				Class<?> fieldType = field.getType();
				if (fieldType == Text.class || fieldType == Object.class) {
					try {
						field.setAccessible(true);
						Object value = field.get(hoverEvent);
						if (value instanceof Text) {
							return (Text) value;
						}
					} catch (Exception e) {
						// Ignore
					}
				}
			}
		} catch (Exception e) {
			// Ignore
		}
		
		// Try ShowText inner class - FIRST get the ShowText value
		Object showTextValue = getShowTextValue(hoverEvent);
		if (showTextValue != null) {
			// CRITICAL: If showTextValue IS the hoverEvent itself, we need to extract from it directly
			// Don't recurse, just process it as ShowText
			if (showTextValue == hoverEvent) {
				// Continue with extraction below
			} else if (showTextValue instanceof HoverEvent) {
				Text recursiveResult = extractHoverTextFromEvent((HoverEvent) showTextValue);
				if (recursiveResult != null) {
					return recursiveResult;
				}
			}
			
			try {
				Class<?> showTextClass = showTextValue.getClass();
				
				// CRITICAL: Check if ShowText is a Record and extract via Record components
				if (showTextClass.isRecord()) {
					try {
						java.lang.reflect.RecordComponent[] components = showTextClass.getRecordComponents();
						for (java.lang.reflect.RecordComponent component : components) {
							try {
								Object value = component.getAccessor().invoke(showTextValue);
								if (value instanceof Text) {
									// Found a Text object! Copy it to create a fresh instance
									try {
										Text textValue = (Text) value;
										Text copiedText = textValue.copy();
										return copiedText;
									} catch (Exception e) {
										// If copy fails, return the original
										return (Text) value;
									}
								}
							} catch (Exception e) {
								// Ignore
							}
						}
					} catch (Exception e) {
						// Ignore
					}
				}
				
				// CRITICAL: Try to find the Component object and serialize/deserialize it
				// This is the key approach: Component → JSON → Text (to get a fresh Text instance)
				// We need to find the actual Text/Component object first, then serialize/deserialize it
				try {
					// Try to find any Text object in the ShowText instance
					java.lang.reflect.Field[] showTextFields = showTextClass.getDeclaredFields();
					for (java.lang.reflect.Field field : showTextFields) {
						if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
							continue;
						}
						
						Class<?> fieldType = field.getType();
						// Look for fields that might contain a Component (Text or Object that could be Text)
						if (fieldType == Text.class || fieldType == Object.class || 
						    (fieldType.getName().contains("Text") && !fieldType.isPrimitive() && fieldType != String.class)) {
							try {
								field.setAccessible(true);
								Object value = field.get(showTextValue);
								
								if (value instanceof Text) {
									// Found a Text object! Copy it to create a fresh instance
									try {
										Text textValue = (Text) value;
										Text copiedText = textValue.copy();
										return copiedText;
									} catch (Exception e) {
										// If copy fails, return the original
										return (Text) value;
									}
								}
							} catch (Exception e) {
								// Continue to next field
							}
						}
					}
				} catch (Exception e) {
					// Ignore
				}
				
				// CRITICAL: Try method_10893() first - this is the obfuscated method that returns the Text!
				try {
					java.lang.reflect.Method method10893 = showTextClass.getDeclaredMethod("method_10893");
					method10893.setAccessible(true);
					Object textValue = method10893.invoke(showTextValue);
					if (textValue instanceof Text) {
						return (Text) textValue;
					}
				} catch (Exception e) {
					// Ignore
				}
				
				// Try method value() on ShowText (for records)
				try {
					java.lang.reflect.Method valueMethod = showTextClass.getDeclaredMethod("value");
					valueMethod.setAccessible(true);
					Object textValue = valueMethod.invoke(showTextValue);
					if (textValue instanceof Text) {
						return (Text) textValue;
					}
				} catch (Exception e) {
					// Ignore
				}
				
				// Try method text() or getText() on ShowText
				try {
					java.lang.reflect.Method textMethod = showTextClass.getDeclaredMethod("text");
					textMethod.setAccessible(true);
					Object textValue = textMethod.invoke(showTextValue);
					if (textValue instanceof Text) {
						return (Text) textValue;
					}
				} catch (Exception e) {
					// Try getText()
					try {
						java.lang.reflect.Method getTextMethod = showTextClass.getDeclaredMethod("getText");
						getTextMethod.setAccessible(true);
						Object textValue = getTextMethod.invoke(showTextValue);
						if (textValue instanceof Text) {
							return (Text) textValue;
						}
					} catch (Exception e2) {
						// Ignore
					}
				}
				
				// Try all fields in ShowText (excluding static)
				java.lang.reflect.Field[] showTextFields = showTextClass.getDeclaredFields();
				
				// CRITICAL: Check ALL fields first, including ALL types, to find the Text object
				for (java.lang.reflect.Field field : showTextFields) {
					if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
						continue;
					}
					
					Class<?> fieldType = field.getType();
					
					try {
						field.setAccessible(true);
						Object value = field.get(showTextValue);
						
						// Check if it's Text directly
						if (value instanceof Text) {
							Text textValue = (Text) value;
							try {
								Text copiedText = textValue.copy();
								return copiedText;
							} catch (Exception e) {
								return textValue;
							}
						}
					} catch (Exception e) {
						// Ignore
					}
				}
				
				// First pass: Check fields with Text type (highest priority)
				for (java.lang.reflect.Field field : showTextFields) {
					if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
						continue;
					}
					
					Class<?> fieldType = field.getType();
					if (fieldType == Text.class || (fieldType.getName().contains("Text") && !fieldType.isPrimitive() && fieldType != String.class)) {
						try {
							field.setAccessible(true);
							Object value = field.get(showTextValue);
							if (value instanceof Text) {
								return (Text) value;
							}
						} catch (Exception e) {
							// Ignore
						}
					}
				}
				
				// Second pass: Check ALL fields (including primitives for debugging, but only process non-primitives)
				// IMPORTANT: We need to check ALL 9 fields to find the Text!
				for (java.lang.reflect.Field field : showTextFields) {
					if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
						continue;
					}
					
					Class<?> fieldType = field.getType();
					
					// Skip if already checked in first pass
					if (fieldType == Text.class || (fieldType.getName().contains("Text") && !fieldType.isPrimitive() && fieldType != String.class)) {
						continue;
					}
					
					// Check all non-primitive fields for Text
					if (!fieldType.isPrimitive()) {
						try {
							field.setAccessible(true);
							Object value = field.get(showTextValue);
							
							// Check if it's Text directly
							if (value instanceof Text) {
								return (Text) value;
							}
							
							// Check if it's a String (but skip action names)
							if (value instanceof String) {
								String stringValue = (String) value;
								if (!stringValue.equals("show_text") && !stringValue.equals("<action show_text>") && 
								    !stringValue.equals("SHOW_TEXT") && !stringValue.isEmpty()) {
									// Use as literal text
									return Text.literal(stringValue);
								}
							}
							
							// Check if it's a List of Text components
							if (value instanceof java.util.List) {
								java.util.List<?> list = (java.util.List<?>) value;
								if (!list.isEmpty() && list.get(0) instanceof Text) {
									// Combine all Text components into one
									net.minecraft.text.MutableText combined = Text.empty();
									for (Object item : list) {
										if (item instanceof Text) {
											combined.append((Text) item);
										}
									}
									return combined;
								}
							}
							
							// Check if it's an object that might contain Text (recursive search)
							if (value != null && !(value instanceof String) && !(value instanceof java.util.List) && 
							    !value.getClass().isPrimitive() && !value.getClass().getName().startsWith("java.")) {
								// Try to find Text in nested object
								try {
									java.lang.reflect.Field[] nestedFields = value.getClass().getDeclaredFields();
									for (java.lang.reflect.Field nestedField : nestedFields) {
										if (java.lang.reflect.Modifier.isStatic(nestedField.getModifiers())) {
											continue;
										}
										if (nestedField.getType() == Text.class) {
											try {
												nestedField.setAccessible(true);
												Object nestedValue = nestedField.get(value);
												if (nestedValue instanceof Text) {
													return (Text) nestedValue;
												}
											} catch (Exception e) {
												// Continue
											}
										}
									}
								} catch (Exception e) {
									// Continue
								}
							}
						} catch (Exception e) {
							// Ignore
						}
					}
				}
				
				// Try all methods that might return Text (including inherited methods)
				java.lang.reflect.Method[] showTextMethods = showTextClass.getDeclaredMethods();
				java.lang.reflect.Method[] allMethods = showTextClass.getMethods();
				
				// Try declared methods first
				for (java.lang.reflect.Method method : showTextMethods) {
					if (java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
						continue;
					}
					// Check ALL methods that take no parameters and return non-primitive (including String)
					if (method.getParameterCount() == 0 && !method.getReturnType().isPrimitive() && method.getReturnType() != Void.TYPE) {
						try {
							method.setAccessible(true);
							Object textValue = method.invoke(showTextValue);
							if (textValue instanceof Text) {
								return (Text) textValue;
							} else if (textValue instanceof String) {
								// Maybe the text is stored as a String? Use it as literal (but skip action names)
								String stringValue = (String) textValue;
								if (!stringValue.equals("show_text") && !stringValue.equals("<action show_text>") && 
								    !stringValue.equals("SHOW_TEXT") && !stringValue.isEmpty()) {
									// Use as literal text
									return Text.literal(stringValue);
								}
							} else if (textValue instanceof java.util.List) {
								// Maybe it's a list of Text components?
								java.util.List<?> list = (java.util.List<?>) textValue;
								if (!list.isEmpty() && list.get(0) instanceof Text) {
									// Combine all Text components into one
									net.minecraft.text.MutableText combined = Text.empty();
									for (Object item : list) {
										if (item instanceof Text) {
											combined.append((Text) item);
										}
									}
									return combined;
								}
							}
						} catch (Exception e) {
							// Ignore
						}
					}
				}
				
				// Also try inherited methods
				for (java.lang.reflect.Method method : allMethods) {
					if (java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
						continue;
					}
					// Skip if already checked in declared methods
					boolean alreadyChecked = false;
					for (java.lang.reflect.Method declaredMethod : showTextMethods) {
						if (declaredMethod.getName().equals(method.getName()) && 
						    java.util.Arrays.equals(declaredMethod.getParameterTypes(), method.getParameterTypes())) {
							alreadyChecked = true;
							break;
						}
					}
					if (alreadyChecked) continue;
					
					// Check ALL methods that take no parameters and return non-primitive (including String)
					if (method.getParameterCount() == 0 && !method.getReturnType().isPrimitive() && method.getReturnType() != Void.TYPE) {
						try {
							method.setAccessible(true);
							Object textValue = method.invoke(showTextValue);
							if (textValue instanceof Text) {
								return (Text) textValue;
							} else if (textValue instanceof String) {
								// Maybe the text is stored as a String? Use it as literal (but skip action names)
								String stringValue = (String) textValue;
								if (!stringValue.equals("show_text") && !stringValue.equals("<action show_text>") && 
								    !stringValue.equals("SHOW_TEXT") && !stringValue.isEmpty()) {
									// Use as literal text
									return Text.literal(stringValue);
								}
							} else if (textValue instanceof java.util.List) {
								// Maybe it's a list of Text components?
								java.util.List<?> list = (java.util.List<?>) textValue;
								if (!list.isEmpty() && list.get(0) instanceof Text) {
									// Combine all Text components into one
									net.minecraft.text.MutableText combined = Text.empty();
									for (Object item : list) {
										if (item instanceof Text) {
											combined.append((Text) item);
										}
									}
									return combined;
								}
							}
						} catch (Exception e) {
							// Ignore
						}
					}
				}
			} catch (Exception e) {
				// Ignore
			}
		}
		
		return null;
	}
	
	/**
	 * Gets the ShowText value from a HoverEvent
	 * Tries various methods to extract the ShowText instance
	 */
	private static Object getShowTextValue(HoverEvent hoverEvent) {
		try {
			// CRITICAL: In Minecraft 1.21.7, HoverEvent is a sealed interface/class
			// If the action is SHOW_TEXT, the HoverEvent itself IS the ShowText object!
			HoverEvent.Action action = hoverEvent.getAction();
			if (action == HoverEvent.Action.SHOW_TEXT) {
				// Check if the HoverEvent itself is a ShowText instance
				String className = hoverEvent.getClass().getName();
				if (className.contains("ShowText")) {
					return hoverEvent;
				}
				
				// Check if it's an inner class of HoverEvent and has a Text constructor (ShowText has this)
				Class<?> hoverEventClass = hoverEvent.getClass();
				Class<?> enclosingClass = hoverEventClass.getEnclosingClass();
				if (enclosingClass == HoverEvent.class || (enclosingClass != null && enclosingClass.getName().contains("HoverEvent"))) {
					// It's an inner class of HoverEvent - check if it has a constructor that takes Text
					try {
						java.lang.reflect.Constructor<?>[] constructors = hoverEventClass.getDeclaredConstructors();
						for (java.lang.reflect.Constructor<?> constructor : constructors) {
							Class<?>[] paramTypes = constructor.getParameterTypes();
							if (paramTypes.length == 1 && paramTypes[0] == Text.class) {
								return hoverEvent;
							}
						}
					} catch (Exception e) {
						// Ignore
					}
				}
				
				// Try to cast it to ShowText by class name
				try {
					Class<?> showTextClass = Class.forName("net.minecraft.text.HoverEvent$ShowText");
					if (showTextClass.isInstance(hoverEvent)) {
						return hoverEvent;
					}
				} catch (Exception e) {
					// Ignore
				}
			}
			
			// Try method value() first (for records)
			try {
				java.lang.reflect.Method valueMethod = HoverEvent.class.getDeclaredMethod("value");
				valueMethod.setAccessible(true);
				Object value = valueMethod.invoke(hoverEvent);
				if (value != null) {
					return value;
				}
			} catch (Exception e) {
				// Ignore
			}
			
			// Try all declared fields (excluding static)
			java.lang.reflect.Field[] fields = HoverEvent.class.getDeclaredFields();
			for (java.lang.reflect.Field field : fields) {
				// Skip static fields
				if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
					continue;
				}
				
				Class<?> fieldType = field.getType();
				String fieldTypeName = fieldType.getName();
				
				if (fieldTypeName.contains("ShowText") || fieldType == Object.class || !fieldType.isPrimitive()) {
					try {
						field.setAccessible(true);
						Object value = field.get(hoverEvent);
						if (value != null) {
							System.out.println("[InformationenUtility] DEBUG: getShowTextValue - found non-null value in field " + field.getName() + ": " + value.getClass().getName());
							// Check if it's a ShowText-like object
							String className = value.getClass().getName();
							
							boolean isShowText = false;
							if (className.contains("ShowText")) {
								isShowText = true;
							} else {
								// Check if it's an inner class of HoverEvent and assignable to HoverEvent with Text constructor
								Class<?> valueClass = value.getClass();
								Class<?> enclosingClass = valueClass.getEnclosingClass();
								if (enclosingClass == HoverEvent.class || (enclosingClass != null && enclosingClass.getName().contains("HoverEvent"))) {
									if (HoverEvent.class.isAssignableFrom(valueClass)) {
										try {
											java.lang.reflect.Constructor<?>[] constructors = valueClass.getDeclaredConstructors();
											for (java.lang.reflect.Constructor<?> constructor : constructors) {
												Class<?>[] paramTypes = constructor.getParameterTypes();
												if (paramTypes.length == 1 && paramTypes[0] == Text.class) {
													isShowText = true;
													System.out.println("[InformationenUtility] DEBUG: getShowTextValue - detected ShowText in field via constructor check (obfuscated class)");
													break;
												}
											}
										} catch (Exception e) {
											// Continue
										}
									}
								}
							}
							
							if (isShowText) {
								return value;
							}
						}
					} catch (Exception e) {
						// Ignore
					}
				}
			}
			
			// Try all methods that might return the value
			java.lang.reflect.Method[] methods = HoverEvent.class.getDeclaredMethods();
			
			// First, try methods that return ShowText directly (by return type name)
			for (java.lang.reflect.Method method : methods) {
				// Skip static methods
				if (java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
					continue;
				}
				// Skip getAction() - that returns Action, not ShowText!
				if (method.getName().equals("getAction")) {
					continue;
				}
				// Check if return type contains ShowText
				String returnTypeName = method.getReturnType().getName();
				if (returnTypeName.contains("ShowText")) {
					try {
						method.setAccessible(true);
						Object value = method.invoke(hoverEvent);
						if (value != null) {
							if (value.getClass().getName().contains("ShowText")) {
								return value;
							}
						}
					} catch (Exception e) {
						// Ignore
					}
				}
			}
			
			// Then try all other methods that might return ShowText
			for (java.lang.reflect.Method method : methods) {
				// Skip static methods
				if (java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
					continue;
				}
				// Skip getAction() - that returns Action, not ShowText!
				if (method.getName().equals("getAction")) {
					continue;
				}
				// Look for methods that take no parameters and return non-primitive
				if (method.getParameterCount() == 0 && !method.getReturnType().isPrimitive() && method.getReturnType() != Void.TYPE) {
					try {
						method.setAccessible(true);
						Object value = method.invoke(hoverEvent);
						if (value != null) {
							// Check if it's a ShowText-like object
							String className = value.getClass().getName();
							
							// CRITICAL: Check if it contains "ShowText" OR if it's an inner class of HoverEvent that is assignable to HoverEvent
							// (ShowText is assignable to HoverEvent, but Action is NOT)
							boolean isShowText = false;
							
							if (className.contains("ShowText")) {
								isShowText = true;
							} else {
								// Check if it's an inner class of HoverEvent and assignable to HoverEvent
								// This handles obfuscated ShowText classes
								Class<?> valueClass = value.getClass();
								Class<?> enclosingClass = valueClass.getEnclosingClass();
								if (enclosingClass == HoverEvent.class || (enclosingClass != null && enclosingClass.getName().contains("HoverEvent"))) {
									// It's an inner class of HoverEvent
									// Check if it's assignable to HoverEvent (ShowText is, Action is NOT)
									if (HoverEvent.class.isAssignableFrom(valueClass)) {
										// Check if it has a constructor that takes Text (ShowText has this, Action doesn't)
										try {
											java.lang.reflect.Constructor<?>[] constructors = valueClass.getDeclaredConstructors();
											for (java.lang.reflect.Constructor<?> constructor : constructors) {
												Class<?>[] paramTypes = constructor.getParameterTypes();
												if (paramTypes.length == 1 && paramTypes[0] == Text.class) {
													isShowText = true;
													break;
												}
											}
										} catch (Exception e) {
											// Continue
										}
									}
								}
							}
							
							if (isShowText) {
								return value;
							}
						}
					} catch (Exception e) {
						// Ignore
					}
				}
			}
		} catch (Exception e) {
			// Ignore
		}
		return null;
	}
	
	/**
	 * Creates a HoverEvent for aspect information
	 * Uses the same approach as TestUtility.createHoverEvent
	 */
	private static HoverEvent createHoverEventForAspect(Text hoverText) {
		if (hoverText == null) {
			return null;
		}
		
		// Use createHoverEventDirect which has more comprehensive logic
		HoverEvent result = createHoverEventDirect(hoverText);
		if (result != null) {
			return result;
		}
		
		// Try HoverEvent.showText() static factory method first
		try {
			java.lang.reflect.Method showTextMethod = HoverEvent.class.getDeclaredMethod("showText", Text.class);
			showTextMethod.setAccessible(true);
			HoverEvent result2 = (HoverEvent) showTextMethod.invoke(null, hoverText);
			return result2;
		} catch (Exception e) {
			// Ignore
		}
		
		// Try all static methods in HoverEvent
		try {
			for (java.lang.reflect.Method method : HoverEvent.class.getDeclaredMethods()) {
				if (java.lang.reflect.Modifier.isStatic(method.getModifiers()) && method.getReturnType() == HoverEvent.class) {
					Class<?>[] paramTypes = method.getParameterTypes();
					if (paramTypes.length == 1 && paramTypes[0] == Text.class) {
						try {
							method.setAccessible(true);
							HoverEvent result3 = (HoverEvent) method.invoke(null, hoverText);
							return result3;
						} catch (Exception e) {
							// Ignore
						}
					}
				}
			}
		} catch (Exception e) {
			// Ignore
		}
		
		// Try using HoverEvent$ShowText inner class
		try {
			Class<?> showTextClass = Class.forName("net.minecraft.text.HoverEvent$ShowText");
			
			// Try ShowText constructors
			java.lang.reflect.Constructor<?>[] constructors = showTextClass.getDeclaredConstructors();
			for (java.lang.reflect.Constructor<?> constructor : constructors) {
				Class<?>[] paramTypes = constructor.getParameterTypes();
				if (paramTypes.length == 1 && paramTypes[0] == Text.class) {
					try {
						constructor.setAccessible(true);
						Object showTextInstance = constructor.newInstance(hoverText);
						
						// Check if ShowText is assignable to HoverEvent (sealed interface/class pattern)
						if (HoverEvent.class.isAssignableFrom(showTextInstance.getClass())) {
							return (HoverEvent) showTextInstance;
						}
					} catch (Exception e) {
						// Ignore
					}
				}
			}
		} catch (Exception e) {
			// Ignore
		}
		
		return null;
	}
	
	/**
	 * Gets the text from a HoverEvent by reading fields directly
	 * In Minecraft 1.21.7, getValue() doesn't exist, so we try to read fields
	 */
	private static Text getHoverEventText(HoverEvent hoverEvent) {
		if (hoverEvent == null || hoverEvent.getAction() != HoverEvent.Action.SHOW_TEXT) {
			return null;
		}
		
		// Try to read fields directly
		try {
			java.lang.reflect.Field[] fields = HoverEvent.class.getDeclaredFields();
			for (java.lang.reflect.Field field : fields) {
				Class<?> fieldType = field.getType();
				if (fieldType == Text.class || fieldType == Object.class) {
					try {
						field.setAccessible(true);
						Object value = field.get(hoverEvent);
						if (value instanceof Text) {
							return (Text) value;
						}
					} catch (Exception e) {
						// Continue to next field
					}
				}
			}
		} catch (Exception e) {
		}
		return null;
	}
	
	/**
	 * Creates a HoverEvent directly using the constructor
	 * In Minecraft 1.21.7, the constructor works at runtime even if not visible at compile time
	 */
	private static HoverEvent createHoverEventDirect(Text hoverText) {
		if (hoverText == null) {
			return null;
		}
		
		// First, try to find ShowText class dynamically by searching inner classes
		Class<?> showTextClass = null;
		try {
			// Try direct name first (for non-obfuscated environments)
			showTextClass = Class.forName("net.minecraft.text.HoverEvent$ShowText");
		} catch (ClassNotFoundException e) {
			// If direct name fails, search inner classes dynamically
			Class<?>[] innerClasses = HoverEvent.class.getDeclaredClasses();
			for (Class<?> innerClass : innerClasses) {
				// Check if this inner class is assignable to HoverEvent or has a constructor that takes Text
				java.lang.reflect.Constructor<?>[] constructors = innerClass.getDeclaredConstructors();
				for (java.lang.reflect.Constructor<?> constructor : constructors) {
					Class<?>[] paramTypes = constructor.getParameterTypes();
					if (paramTypes.length == 1 && paramTypes[0] == Text.class) {
						// This looks like ShowText!
						showTextClass = innerClass;
						break;
					}
				}
				if (showTextClass != null) break;
			}
		}
		
		// Try using ShowText inner class if we found it
		if (showTextClass != null) {
			try {
			
			// Check for static factory methods in ShowText
			for (java.lang.reflect.Method method : showTextClass.getDeclaredMethods()) {
				if (java.lang.reflect.Modifier.isStatic(method.getModifiers()) && method.getReturnType() == HoverEvent.class) {
					Class<?>[] paramTypes = method.getParameterTypes();
					if (paramTypes.length == 1 && paramTypes[0] == Text.class) {
						try {
							method.setAccessible(true);
							HoverEvent result = (HoverEvent) method.invoke(null, hoverText);
							return result;
						} catch (Exception e) {
							// Ignore
						}
					}
				}
			}
			
			// Try constructors in ShowText
			java.lang.reflect.Constructor<?>[] constructors = showTextClass.getDeclaredConstructors();
			
			for (java.lang.reflect.Constructor<?> constructor : constructors) {
				Class<?>[] paramTypes = constructor.getParameterTypes();
				if (paramTypes.length == 1 && paramTypes[0] == Text.class) {
					try {
						constructor.setAccessible(true);
						Object showTextInstance = constructor.newInstance(hoverText);
						
						// Check if ShowText is directly assignable to HoverEvent (sealed interface/class pattern)
						if (HoverEvent.class.isAssignableFrom(showTextInstance.getClass())) {
							return (HoverEvent) showTextInstance;
						}
						
						// Check interfaces
						Class<?>[] interfaces = showTextClass.getInterfaces();
						for (Class<?> iface : interfaces) {
							if (iface == HoverEvent.class || HoverEvent.class.isAssignableFrom(iface)) {
								return (HoverEvent) showTextInstance;
							}
						}
						
						// Check superclass
						Class<?> superclass = showTextClass.getSuperclass();
						if (superclass != null) {
							if (superclass == HoverEvent.class || HoverEvent.class.isAssignableFrom(superclass)) {
								return (HoverEvent) showTextInstance;
							}
						}
						
						// Check if ShowText has a method to create HoverEvent
						for (java.lang.reflect.Method method : showTextClass.getDeclaredMethods()) {
							if (method.getReturnType() == HoverEvent.class) {
								Class<?>[] methodParamTypes = method.getParameterTypes();
								try {
									method.setAccessible(true);
									HoverEvent result = (HoverEvent) method.invoke(showTextInstance);
									return result;
								} catch (Exception e) {
									// Ignore
								}
							}
						}
						
						// Check if ShowText implements an interface or extends a class that has a method to create HoverEvent
						Class<?>[] showTextInterfaces = showTextClass.getInterfaces();
						for (Class<?> iface : showTextInterfaces) {
							for (java.lang.reflect.Method method : iface.getMethods()) {
								if (method.getReturnType() == HoverEvent.class) {
									try {
										HoverEvent result = (HoverEvent) method.invoke(showTextInstance);
										return result;
									} catch (Exception e) {
										// Ignore
									}
								}
							}
						}
						
						// Try to find a static method in HoverEvent that takes ShowText or Object
						for (java.lang.reflect.Method method : HoverEvent.class.getDeclaredMethods()) {
							if (java.lang.reflect.Modifier.isStatic(method.getModifiers()) && 
							    method.getReturnType() == HoverEvent.class) {
								Class<?>[] methodParamTypes = method.getParameterTypes();
								if (methodParamTypes.length == 1) {
									if (methodParamTypes[0].isAssignableFrom(showTextInstance.getClass()) || 
									    methodParamTypes[0] == Object.class ||
									    methodParamTypes[0] == HoverEvent.Action.class) {
										try {
											method.setAccessible(true);
											Object param = methodParamTypes[0] == HoverEvent.Action.class ? HoverEvent.Action.SHOW_TEXT : showTextInstance;
											HoverEvent result = (HoverEvent) method.invoke(null, param);
											return result;
										} catch (Exception e) {
											// Ignore
										}
									}
								} else if (methodParamTypes.length == 2 && 
								          methodParamTypes[0] == HoverEvent.Action.class &&
								          (methodParamTypes[1].isAssignableFrom(showTextInstance.getClass()) || methodParamTypes[1] == Object.class)) {
									try {
										method.setAccessible(true);
										HoverEvent result = (HoverEvent) method.invoke(null, HoverEvent.Action.SHOW_TEXT, showTextInstance);
										return result;
									} catch (Exception e) {
										// Ignore
									}
								}
							}
						}
						
						// Now try to create HoverEvent with ShowText instance using reflection
						java.lang.reflect.Constructor<?>[] hoverEventConstructors = HoverEvent.class.getDeclaredConstructors();
						for (java.lang.reflect.Constructor<?> hoverEventConstructor : hoverEventConstructors) {
							Class<?>[] hoverEventParamTypes = hoverEventConstructor.getParameterTypes();
							if (hoverEventParamTypes.length == 2 && 
							    hoverEventParamTypes[0] == HoverEvent.Action.class &&
							    hoverEventParamTypes[1].isAssignableFrom(showTextInstance.getClass())) {
								try {
									hoverEventConstructor.setAccessible(true);
									HoverEvent result = (HoverEvent) hoverEventConstructor.newInstance(HoverEvent.Action.SHOW_TEXT, showTextInstance);
									return result;
								} catch (Exception e) {
									// Ignore
								}
							}
						}
					} catch (Exception e) {
						// Ignore
					}
				}
			}
			} catch (Exception e) {
				// Ignore
			}
		}
		
		// Fallback: Try using reflection on HoverEvent directly
		HoverEvent result = createHoverEventWithReflection(hoverText);
		return result;
	}
	
	/**
	 * Creates a HoverEvent using reflection as fallback
	 * In 1.21.7, HoverEvent can be created with: new HoverEvent(Action, Text)
	 * We use reflection since the constructor may not be visible at compile time
	 */
	private static HoverEvent createHoverEventWithReflection(Text hoverText) {
		
		// Check if HoverEvent is a record
		boolean isRecord = HoverEvent.class.isRecord();
		
		// List all constructors for debugging
		java.lang.reflect.Constructor<?>[] declaredConstructors = HoverEvent.class.getDeclaredConstructors();
		for (java.lang.reflect.Constructor<?> constructor : declaredConstructors) {
			Class<?>[] paramTypes = constructor.getParameterTypes();
		}
		
		java.lang.reflect.Constructor<?>[] publicConstructors = HoverEvent.class.getConstructors();
		for (java.lang.reflect.Constructor<?> constructor : publicConstructors) {
			Class<?>[] paramTypes = constructor.getParameterTypes();
		}
		
		// List ALL methods of HoverEvent for debugging
		for (java.lang.reflect.Method method : HoverEvent.class.getMethods()) {
		}
		
		// List all static methods that return HoverEvent
		for (java.lang.reflect.Method method : HoverEvent.class.getDeclaredMethods()) {
			if (java.lang.reflect.Modifier.isStatic(method.getModifiers()) && method.getReturnType() == HoverEvent.class) {
				Class<?>[] paramTypes = method.getParameterTypes();
			}
		}
		
		// Check if there are inner classes
		for (Class<?> innerClass : HoverEvent.class.getDeclaredClasses()) {
			// Check for static factory methods in inner classes
			for (java.lang.reflect.Method method : innerClass.getDeclaredMethods()) {
				if (java.lang.reflect.Modifier.isStatic(method.getModifiers()) && method.getReturnType() == HoverEvent.class) {
					Class<?>[] paramTypes = method.getParameterTypes();
				}
			}
		}
		
		try {
			// Try to find and call the constructor
			for (java.lang.reflect.Constructor<?> constructor : declaredConstructors) {
				Class<?>[] paramTypes = constructor.getParameterTypes();
				if (paramTypes.length == 2 && paramTypes[0] == HoverEvent.Action.class) {
					try {
						constructor.setAccessible(true);
						HoverEvent result = (HoverEvent) constructor.newInstance(HoverEvent.Action.SHOW_TEXT, hoverText);
						return result;
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			
			// Also try public constructors
			for (java.lang.reflect.Constructor<?> constructor : publicConstructors) {
				Class<?>[] paramTypes = constructor.getParameterTypes();
				if (paramTypes.length == 2 && paramTypes[0] == HoverEvent.Action.class) {
					try {
						HoverEvent result = (HoverEvent) constructor.newInstance(HoverEvent.Action.SHOW_TEXT, hoverText);
						return result;
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			
			// Try static factory methods
			for (java.lang.reflect.Method method : HoverEvent.class.getDeclaredMethods()) {
				if (java.lang.reflect.Modifier.isStatic(method.getModifiers()) && method.getReturnType() == HoverEvent.class) {
					Class<?>[] paramTypes = method.getParameterTypes();
					if (paramTypes.length == 2 && paramTypes[0] == HoverEvent.Action.class) {
						try {
							method.setAccessible(true);
							HoverEvent result = (HoverEvent) method.invoke(null, HoverEvent.Action.SHOW_TEXT, hoverText);
							return result;
						} catch (Exception e) {
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Recursively modifies a Text component to replace hover events while preserving all formatting.
	 * Only replaces hover events where they exist (SHOW_TEXT action).
	 */
	private static Text modifyTextWithHoverEvent(Text text, HoverEvent newHoverEvent) {
		if (text == null) return text;
		
		// Copy the text to preserve its structure - copy() returns MutableText which has setStyle()
		net.minecraft.text.MutableText newText = text.copy();
		
		// Check if this text component has a hover event that needs to be replaced
		Style currentStyle = text.getStyle();
		if (currentStyle != null && currentStyle.getHoverEvent() != null) {
			// This text has a hover event - replace it with the new one
			Style newStyle = currentStyle.withHoverEvent(newHoverEvent);
			newText.setStyle(newStyle);
		}
		// If no hover event, the style is already preserved by copy()
		
		// Recursively process all siblings to preserve their formatting
		newText.getSiblings().clear(); // Clear existing siblings to avoid duplication
		for (Text sibling : text.getSiblings()) {
			Text modifiedSibling = modifyTextWithHoverEvent(sibling, newHoverEvent);
			newText.getSiblings().add(modifiedSibling);
		}
		
		return newText;
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
							var mobElement = floorData.get("mob");
							// Support both string and array formats
							if (mobElement.isJsonArray()) {
								// Array format: ["Name1", "Name2", ...]
								com.google.gson.JsonArray mobArray = mobElement.getAsJsonArray();
								for (var element : mobArray) {
									String mobName = element.getAsString();
									if (!mobName.isEmpty()) {
										materialsDatabase.put(mobName, new MaterialInfo(floorNumber, "mob", "WHITE"));
									}
								}
							} else {
								// String format: "Name" (backward compatibility)
								String mobName = mobElement.getAsString();
								if (!mobName.isEmpty()) {
									materialsDatabase.put(mobName, new MaterialInfo(floorNumber, "mob", "WHITE"));
								}
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
	 * Extracts plain text from a Text component recursively, ignoring all formatting
	 */
	private static String extractPlainText(Text text) {
		if (text == null) {
			return "";
		}
		
		// Use getString() which already extracts all text recursively
		return text.getString();
	}
	
	/**
	 * Converts an arabic number to roman numeral
	 */
	private static String convertToRomanNumeral(int number) {
		if (number < 1 || number > 8) {
			return String.valueOf(number); // Return as string if out of range
		}
		
		String[] romanNumerals = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII"};
		return romanNumerals[number - 1];
	}
	
	/**
	 * Converts a roman numeral to arabic number
	 */
	private static int romanToArabic(String roman) {
		java.util.Map<String, Integer> romanMap = new java.util.HashMap<>();
		romanMap.put("I", 1);
		romanMap.put("II", 2);
		romanMap.put("III", 3);
		romanMap.put("IV", 4);
		romanMap.put("V", 5);
		romanMap.put("VI", 6);
		romanMap.put("VII", 7);
		romanMap.put("VIII", 8);
		
		return romanMap.getOrDefault(roman, -1);
	}
	
	// License slots that should be checked
	private static final int[] LICENSE_SLOTS = {1, 2, 3, 10, 11, 12, 19, 20, 21};
	
	/**
	 * Checks if the hovered item is in one of the license slots
	 */
	private static boolean isItemInLicenseSlot(ItemStack stack, MinecraftClient client) {
		if (client.currentScreen == null || !(client.currentScreen instanceof net.minecraft.client.gui.screen.ingame.HandledScreen)) {
			return false;
		}
		
		net.minecraft.client.gui.screen.ingame.HandledScreen<?> screen = (net.minecraft.client.gui.screen.ingame.HandledScreen<?>) client.currentScreen;
		net.minecraft.screen.ScreenHandler handler = screen.getScreenHandler();
		
		// Check if the hovered item is in one of the license slots
		for (int slotIndex : LICENSE_SLOTS) {
			if (slotIndex < handler.slots.size()) {
				net.minecraft.screen.slot.Slot slot = handler.slots.get(slotIndex);
				if (slot.hasStack() && slot.getStack() == stack) {
					return true;
				}
			}
		}
		
		// Also check by comparing item stacks (in case of reference mismatch)
		for (int slotIndex : LICENSE_SLOTS) {
			if (slotIndex < handler.slots.size()) {
				net.minecraft.screen.slot.Slot slot = handler.slots.get(slotIndex);
				if (slot.hasStack()) {
					ItemStack slotStack = slot.getStack();
					if (ItemStack.areEqual(stack, slotStack)) {
						return true;
					}
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Checks for license information in tooltip lines and adds location information
	 */
	private static void checkForLicenseInformation(List<Text> lines, MinecraftClient client) {
		// Check if database is loaded
		if (licensesDatabase.isEmpty()) {
			return;
		}
		
		for (int i = 0; i < lines.size(); i++) {
			Text line = lines.get(i);
			String lineText = line.getString();
			
			// Skip if lineText is null or empty
			if (lineText == null || lineText.isEmpty()) {
				continue;
			}
			
			// Extract plain text from Text component recursively to handle nested formatting
			// This ensures we get all text regardless of color/formatting
			String cleanLineText = line.getString();
			
			// Remove all Minecraft formatting codes (including all possible format codes)
			// § followed by any character (0-9, a-f, k-o, r, x for hex colors, etc.)
			cleanLineText = cleanLineText.replaceAll("§[0-9a-fk-orxA-FK-ORX]", "");
			
			// Remove hex color codes (format: §#RRGGBB or §x§r§r§g§g§b§b)
			cleanLineText = cleanLineText.replaceAll("§#[0-9a-fA-F]{6}", "");
			cleanLineText = cleanLineText.replaceAll("§x(§[0-9a-fA-F]){6}", "");
			
			// Also remove Unicode formatting characters that might interfere
			cleanLineText = cleanLineText.replaceAll("[\\u3400-\\u4DBF]", "");
			
			// Trim whitespace
			cleanLineText = cleanLineText.trim();
			
			// Skip lines that contain "Schaltet das" - these are description lines, not license lines
			if (cleanLineText.contains("Schaltet das")) {
				continue;
			}
			
			// Check if this line contains "Benötigte Lizenz:" and extract the license name/alias
			if (cleanLineText.contains("Benötigte Lizenz:")) {
				// Extract the part after "Benötigte Lizenz:"
				String afterColon = cleanLineText.substring(cleanLineText.indexOf("Benötigte Lizenz:") + "Benötigte Lizenz:".length()).trim();
				
				// Try to find a license name or alias in the remaining text
				// Sort by length (longest first) to avoid shorter names matching before longer ones
				List<Map.Entry<String, LicenseInfo>> sortedLicenses = new ArrayList<>(licensesDatabase.entrySet());
				sortedLicenses.sort((a, b) -> Integer.compare(b.getKey().length(), a.getKey().length()));
				
				for (Map.Entry<String, LicenseInfo> entry : sortedLicenses) {
					String searchTerm = entry.getKey();
					LicenseInfo licenseInfo = entry.getValue();
					
					// Remove formatting from search term as well for comparison
					String cleanSearchTerm = searchTerm.replaceAll("§[0-9a-fk-orx]", "")
						.replaceAll("§#[0-9a-fA-F]{6}", "")
						.replaceAll("[\\u3400-\\u4DBF]", "")
						.trim();
					
					// Case-insensitive search to be more robust
					String lowerAfterColon = afterColon.toLowerCase();
					String lowerSearchTerm = cleanSearchTerm.toLowerCase();
					
					if (lowerAfterColon.contains(lowerSearchTerm)) {
						// Add location information as a new line with format: → Location
						Text locationText = Text.literal(" → " + licenseInfo.location)
							.styled(style -> style.withColor(0xC0C0C0)); // Light gray
						
						// Insert after the current line
						lines.add(i + 1, locationText);
						
						// Skip the next line since we just added location info
						i++;
						break; // Only add info for the first match in this line
					}
				}
			} else {
				// Also check if this line contains any license name or alias directly (fallback)
				// Sort by length (longest first) to avoid shorter names matching before longer ones
				List<Map.Entry<String, LicenseInfo>> sortedLicenses = new ArrayList<>(licensesDatabase.entrySet());
				sortedLicenses.sort((a, b) -> Integer.compare(b.getKey().length(), a.getKey().length()));
				
				for (Map.Entry<String, LicenseInfo> entry : sortedLicenses) {
					String searchTerm = entry.getKey();
					LicenseInfo licenseInfo = entry.getValue();
					
					// Remove formatting from search term as well for comparison
					String cleanSearchTerm = searchTerm.replaceAll("§[0-9a-fk-orx]", "")
						.replaceAll("§#[0-9a-fA-F]{6}", "")
						.replaceAll("[\\u3400-\\u4DBF]", "")
						.trim();
					
					// Case-insensitive search to be more robust
					String lowerCleanLine = cleanLineText.toLowerCase();
					String lowerSearchTerm = cleanSearchTerm.toLowerCase();
					
					if (lowerCleanLine.contains(lowerSearchTerm)) {
						// Add location information as a new line with format: → Location
						Text locationText = Text.literal(" → " + licenseInfo.location)
							.styled(style -> style.withColor(0xC0C0C0)); // Light gray
						
						// Insert after the current line
						lines.add(i + 1, locationText);
						
						// Skip the next line since we just added location info
						i++;
						break; // Only add info for the first match in this line
					}
				}
			}
		}
	}
	
	/**
	 * Checks for gadget information in tooltip lines and adds location information
	 * @param lines The tooltip lines
	 * @param isEngineerInventory If true, reads level from line 3 with format "[Stufe]: "LEVEL"", otherwise from "Stufe: [LEVEL]"
	 */
	private static void checkForGadgetInformation(List<Text> lines, boolean isEngineerInventory) {
		if (gadgetsDatabase.isEmpty()) {
			return;
		}
		
		// Find the line with level information and extract level, and identify the item
		String itemName = null;
		String level = null;
		int stufeLineIndex = -1;
		
		// First pass: identify the item by checking all lines for gadget names/aliases
		for (int i = 0; i < lines.size(); i++) {
			Text line = lines.get(i);
			String lineText = line.getString();
			
			if (lineText == null || lineText.isEmpty()) {
				continue;
			}
			
			// Remove formatting codes
			String cleanLineText = lineText.replaceAll("§[0-9a-fk-orxA-FK-ORX]", "")
				.replaceAll("§#[0-9a-fA-F]{6}", "")
				.replaceAll("§x(§[0-9a-fA-F]){6}", "")
				.replaceAll("[\\u3400-\\u4DBF]", "")
				.trim();
			
			// Try to identify the item by checking if any gadget name/alias appears in the tooltip
			// Sort by length (longest first) to avoid shorter names matching before longer ones
			List<String> sortedGadgetKeys = new ArrayList<>(gadgetsDatabase.keySet());
			sortedGadgetKeys.sort((a, b) -> Integer.compare(b.length(), a.length()));
			
			for (String gadgetKey : sortedGadgetKeys) {
				String cleanGadgetKey = gadgetKey.replaceAll("§[0-9a-fk-orx]", "")
					.replaceAll("§#[0-9a-fA-F]{6}", "")
					.replaceAll("[\\u3400-\\u4DBF]", "")
					.trim();
				
				if (cleanLineText.toLowerCase().contains(cleanGadgetKey.toLowerCase())) {
					itemName = gadgetKey;
					break;
				}
			}
			
			if (itemName != null) {
				break; // Found the item, stop searching
			}
		}
		
		// Second pass: find the level line
		if (isEngineerInventory) {
			// For Engineer inventory: search all lines for "[Stufe]: "LEVEL"" pattern
			for (int i = 0; i < lines.size(); i++) {
				Text line = lines.get(i);
				String lineText = line.getString();
				
				if (lineText == null || lineText.isEmpty()) {
					continue;
				}
				
				// Remove formatting codes
				String cleanLineText = lineText.replaceAll("§[0-9a-fk-orxA-FK-ORX]", "")
					.replaceAll("§#[0-9a-fA-F]{6}", "")
					.replaceAll("§x(§[0-9a-fA-F]){6}", "")
					.replaceAll("[\\u3400-\\u4DBF]", "")
					.trim();
				
				// Check for "[Stufe]: "LEVEL"" pattern (with quotes) or "[Stufe]: LEVEL" pattern (without quotes)
				if (cleanLineText.contains("[Stufe]:")) {
					stufeLineIndex = i;
					// Extract level from "[Stufe]: "LEVEL"" or "[Stufe]: LEVEL"
					int startIndex = cleanLineText.indexOf("[Stufe]:") + "[Stufe]:".length();
					String afterStufe = cleanLineText.substring(startIndex).trim();
					
					// Check if it starts with a quote
					if (afterStufe.startsWith("\"")) {
						// Format: "[Stufe]: "LEVEL""
						int endIndex = afterStufe.indexOf("\"", 1);
						if (endIndex > 1) {
							level = afterStufe.substring(1, endIndex).trim();
						}
					} else {
						// Format: "[Stufe]: LEVEL" - extract everything after ": " until end or space
						level = afterStufe.trim();
						// Remove any trailing text if there is any
						if (level.contains(" ")) {
							level = level.substring(0, level.indexOf(" ")).trim();
						}
					}
					
					if (level != null && !level.isEmpty()) {
						// Level found, continue
					}
					break; // Found the level, stop searching
				}
			}
		} else {
			// For Module [Upgraden] inventory: find the "Stufe: [" line
			for (int i = 0; i < lines.size(); i++) {
				Text line = lines.get(i);
				String lineText = line.getString();
				
				if (lineText == null || lineText.isEmpty()) {
					continue;
				}
				
				// Remove formatting codes
				String cleanLineText = lineText.replaceAll("§[0-9a-fk-orxA-FK-ORX]", "")
					.replaceAll("§#[0-9a-fA-F]{6}", "")
					.replaceAll("§x(§[0-9a-fA-F]){6}", "")
					.replaceAll("[\\u3400-\\u4DBF]", "")
					.trim();
				
				// Check for "Stufe: [" pattern
				if (cleanLineText.contains("Stufe: [") && stufeLineIndex == -1) {
					stufeLineIndex = i;
					// Extract level from "Stufe: [LEVEL]"
					int startIndex = cleanLineText.indexOf("Stufe: [") + "Stufe: [".length();
					int endIndex = cleanLineText.indexOf("]", startIndex);
					if (endIndex > startIndex) {
						level = cleanLineText.substring(startIndex, endIndex).trim();
					}
					break; // Found the level, stop searching
				}
			}
		}
		
		// If we found a level and item name, look up the location
		if (stufeLineIndex >= 0 && level != null && itemName != null) {
			Map<String, String> levelMap = gadgetsDatabase.get(itemName);
			if (levelMap != null) {
				String location = levelMap.get(level);
				
				// If not found and level is a number, try converting to roman numeral
				if (location == null) {
					try {
						int levelNum = Integer.parseInt(level);
						String romanLevel = convertToRomanNumeral(levelNum);
						location = levelMap.get(romanLevel);
					} catch (NumberFormatException e) {
						// Level is not a number, try converting roman to number
						try {
							int levelNum = romanToArabic(level);
							String arabicLevel = String.valueOf(levelNum);
							location = levelMap.get(arabicLevel);
						} catch (Exception ex) {
							// Conversion failed, continue
						}
					}
				}
				
				if (location != null && !location.isEmpty()) {
					// Add location information as a new line after the level line
					// Format: "Nächstes Level: location" (without quotes)
					Text locationText = Text.literal("Nächstes Level: " + location)
						.styled(style -> style.withColor(0xC0C0C0)); // Light gray
					
					lines.add(stufeLineIndex + 1, locationText);
				}
			}
		}
	}
	
	/**
	 * Loads the gadgets database from Farmworld.json
	 */
	private static void loadGadgetsDatabase() {
		try {
			// Load from mod resources
			var resource = FabricLoader.getInstance().getModContainer("cclive-utilities")
				.orElseThrow(() -> new RuntimeException("Mod container not found"))
				.findPath(LICENSES_CONFIG_FILE)
				.orElseThrow(() -> new RuntimeException("Gadgets config file not found"));
			
			try (var inputStream = java.nio.file.Files.newInputStream(resource)) {
				try (var reader = new java.io.InputStreamReader(inputStream)) {
					JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
					
					// Load all gadget categories
					String[] gadgetCategories = {"mining_gadgets", "logging_gadgets", "gadget_bags", 
						"magnet_gadgets", "smelting_gadgets", "distribution_gadgets"};
					
					for (String category : gadgetCategories) {
						if (json.has(category)) {
							com.google.gson.JsonArray gadgetsArray = json.getAsJsonArray(category);
							
							for (var element : gadgetsArray) {
								JsonObject gadgetData = element.getAsJsonObject();
								String name = gadgetData.get("name").getAsString();
								String level = gadgetData.get("level").getAsString();
								String location = gadgetData.get("location").getAsString();
								
								if (!name.isEmpty() && !level.isEmpty() && !location.isEmpty()) {
									// Store by name
									gadgetsDatabase.computeIfAbsent(name, k -> new HashMap<>()).put(level, location);
									
									// Also store by alias if it exists
									if (gadgetData.has("aliases")) {
										String aliases = gadgetData.get("aliases").getAsString();
										if (!aliases.isEmpty()) {
											gadgetsDatabase.computeIfAbsent(aliases, k -> new HashMap<>()).put(level, location);
										}
									}
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			System.err.println("Failed to load gadgets database: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * Loads the licenses database from Farmworld.json
	 */
	private static void loadLicensesDatabase() {
		try {
			// Load from mod resources
			var resource = FabricLoader.getInstance().getModContainer("cclive-utilities")
				.orElseThrow(() -> new RuntimeException("Mod container not found"))
				.findPath(LICENSES_CONFIG_FILE)
				.orElseThrow(() -> new RuntimeException("Licenses config file not found"));
			
			try (var inputStream = java.nio.file.Files.newInputStream(resource)) {
				try (var reader = new java.io.InputStreamReader(inputStream)) {
					JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
					com.google.gson.JsonArray licensesArray = json.getAsJsonArray("licenses");
					
					for (var element : licensesArray) {
						JsonObject licenseData = element.getAsJsonObject();
						String name = licenseData.get("name").getAsString();
						String location = licenseData.get("location").getAsString();
						
						// Store the license with its name as key
						if (!name.isEmpty() && !location.isEmpty()) {
							LicenseInfo licenseInfo = new LicenseInfo(name, location);
							licensesDatabase.put(name, licenseInfo);
							
							// Also store the alias if it exists
							if (licenseData.has("aliases")) {
								String aliases = licenseData.get("aliases").getAsString();
								if (!aliases.isEmpty()) {
									licensesDatabase.put(aliases, licenseInfo);
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			System.err.println("Failed to load licenses database: " + e.getMessage());
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
	 * Loads the blueprints database from blueprints.json to map blueprint names to floor numbers
	 */
	private static void loadBlueprintsDatabase() {
		try {
			// Load from mod resources
			var resource = FabricLoader.getInstance().getModContainer("cclive-utilities")
				.orElseThrow(() -> new RuntimeException("Mod container not found"))
				.findPath(BLUEPRINTS_CONFIG_FILE)
				.orElseThrow(() -> new RuntimeException("Blueprints config file not found"));
			
			try (var inputStream = java.nio.file.Files.newInputStream(resource)) {
				try (var reader = new java.io.InputStreamReader(inputStream)) {
					JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
					JsonObject floors = json.getAsJsonObject("floors");
					
					for (String floorKey : floors.keySet()) {
						// Extract floor number from "floor_X" format
						final int floorNumber;
						try {
							String floorNumStr = floorKey.replace("floor_", "");
							floorNumber = Integer.parseInt(floorNumStr);
						} catch (NumberFormatException e) {
							// If parsing fails, default to 1
							continue; // Skip this floor if we can't parse the number
						}
						
						JsonObject floorData = floors.getAsJsonObject(floorKey);
						JsonObject blueprints = floorData.getAsJsonObject("blueprints");
						
						// Iterate through all rarities
						for (String rarityKey : blueprints.keySet()) {
							JsonObject rarityData = blueprints.getAsJsonObject(rarityKey);
							com.google.gson.JsonArray itemsArray = rarityData.getAsJsonArray("items");
							
							// Add each blueprint item to the map
							itemsArray.forEach(element -> {
								String blueprintName = element.getAsString();
								// Store the floor number for this blueprint
								blueprintFloorMap.put(blueprintName, floorNumber);
							});
						}
					}
				}
			}
		} catch (Exception e) {
			System.err.println("Failed to load blueprints database: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * Adds floor number to blueprint names in inventory tooltips
	 * Format: "BAUPLAN NAME - [Bauplan] [eX]" where X is the floor number (at the end)
	 */
	private static void addFloorNumberToBlueprintNames(List<Text> lines, MinecraftClient client) {
		// Check if blueprint floor number display is enabled in config
		if (!CCLiveUtilitiesConfig.HANDLER.instance().showBlueprintFloorNumber) {
			return;
		}
		
		if (client == null || client.currentScreen == null) {
			return;
		}
		
		// Check if we're in a blueprint inventory
		String screenTitle = client.currentScreen.getTitle().getString();
		String cleanScreenTitle = screenTitle.replaceAll("§[0-9a-fk-or]", "")
											.replaceAll("[\\u3400-\\u4DBF]", "");
		
		boolean isInBlueprintInventory = cleanScreenTitle.contains("Baupläne [Waffen]") ||
										  cleanScreenTitle.contains("Baupläne [Rüstung]") ||
										  cleanScreenTitle.contains("Baupläne [Werkzeuge]") ||
										  cleanScreenTitle.contains("Bauplan [Shop]") ||
										  cleanScreenTitle.contains("Favorisierte [Rüstungsbaupläne]") ||
										  cleanScreenTitle.contains("Favorisierte [Waffenbaupläne]") ||
										  cleanScreenTitle.contains("Favorisierte [Werkzeugbaupläne]") ||
										  cleanScreenTitle.contains("Favorisierte [Shop-Baupläne]") ||
										  cleanScreenTitle.contains("CACTUS_CLICKER.blueprints.favorites.title.tools");
		
		if (!isInBlueprintInventory) {
			return;
		}
		
		// Look for lines containing "[Bauplan]"
		for (int i = 0; i < lines.size(); i++) {
			Text line = lines.get(i);
			String lineText = line.getString();
			
			if (lineText == null || !lineText.contains("[Bauplan]")) {
				continue;
			}
			
			// Check if the floor number is already added
			if (lineText.contains("[e")) {
				continue; // Already has floor number
			}
			
			// Extract blueprint name and color
			BlueprintNameAndColor nameAndColor = extractBlueprintNameAndColorFromItemName(line);
			
			String blueprintName = null;
			if (nameAndColor != null) {
				blueprintName = nameAndColor.name;
			} else {
				// Fallback: extract from string
				blueprintName = lineText.replace(" - [Bauplan]", "");
				// Remove Unicode formatting characters
				blueprintName = blueprintName.replaceAll("[\\u3400-\\u4DBF\\u4E00-\\u9FFF]", "");
				// Remove percentage and parentheses
				blueprintName = blueprintName.replaceAll("\\([^)]*\\)", "").trim();
				// Remove level information if present
				blueprintName = blueprintName.replaceAll("\\[Ebene \\d+\\]", "").trim();
				// Remove trailing dash
				blueprintName = blueprintName.replaceAll("\\s*-\\s*$", "").trim();
			}
			
			if (blueprintName == null || blueprintName.isEmpty()) {
				continue;
			}
			
			// Special handling for "Drachenzahn" which appears in multiple floors
			// We need to check the color to determine the floor
			Integer floorNumber = null;
			if (blueprintName.equals("Drachenzahn")) {
				// Check the color of the blueprint name
				// Epic color (#A134EB or #A335EE) = floor 1
				// Legendary color (#FC7E00 or #FF8000) = floor 85
				if (nameAndColor != null) {
					String rarity = nameAndColor.rarity;
					if ("epic".equals(rarity)) {
						floorNumber = 1;
					} else if ("legendary".equals(rarity)) {
						floorNumber = 85;
					}
				}
				
				// If we couldn't determine from nameAndColor, try to extract color from the line directly
				if (floorNumber == null) {
					floorNumber = getDrachenzahnFloorFromColor(line);
				}
			} else {
				// For other blueprints, get floor number from map
				floorNumber = blueprintFloorMap.get(blueprintName);
			}
			
			if (floorNumber == null) {
				continue; // Blueprint not found in database or couldn't determine floor
			}
			
			// Create new text with floor number added at the end
			// Format: "BAUPLAN NAME - [Bauplan] [eX]"
			String floorTag = " [e" + floorNumber + "]";
			
			// Simply append the floor tag at the end of the text
			// We need to preserve the original styling
			Text newLine = appendFloorNumberToText(line, floorTag);
			if (newLine != null) {
				lines.set(i, newLine);
			}
		}
	}
	
	/**
	 * Gets the floor number for "Drachenzahn" based on the color of the name
	 * Epic color (#A134EB or #A335EE) = floor 1
	 * Legendary color (#FC7E00 or #FF8000) = floor 85
	 */
	private static Integer getDrachenzahnFloorFromColor(Text itemNameText) {
		if (itemNameText == null) {
			return null;
		}
		
		// Target colors: 
		// Epic: #A134EB (RGB: 161, 52, 235) OR #A335EE (RGB: 163, 53, 238) - floor 1
		// Legendary: #FC7E00 (RGB: 252, 126, 0) OR #FF8000 (RGB: 255, 128, 0) - floor 85
		int epicColor1 = 0xFFA134EB;
		int epicColor2 = 0xFFA335EE;
		int legendaryColor1 = 0xFFFC7E00;
		int legendaryColor2 = 0xFFFF8000;
		int epicColor1RGB = epicColor1 & 0x00FFFFFF;
		int epicColor2RGB = epicColor2 & 0x00FFFFFF;
		int legendaryColor1RGB = legendaryColor1 & 0x00FFFFFF;
		int legendaryColor2RGB = legendaryColor2 & 0x00FFFFFF;
		
		// Convert to OrderedText to iterate through styled parts
		net.minecraft.text.OrderedText orderedText = itemNameText.asOrderedText();
		final StringBuilder fullText = new StringBuilder();
		final java.util.List<net.minecraft.text.Style> styles = new java.util.ArrayList<>();
		final java.util.List<Integer> codePoints = new java.util.ArrayList<>();
		
		orderedText.accept((index, style, codePoint) -> {
			styles.add(style);
			codePoints.add(codePoint);
			fullText.appendCodePoint(codePoint);
			return true;
		});
		
		// Find the blueprint name section (BEFORE "-" and "[Bauplan]")
		String fullTextStr = fullText.toString();
		int dashIndex = -1;
		
		// Find the dash before [Bauplan]
		int bauplanStart = fullTextStr.indexOf("[Bauplan]");
		if (bauplanStart > 0) {
			// Look for dash before [Bauplan]
			for (int i = bauplanStart - 1; i >= 0; i--) {
				char ch = fullTextStr.charAt(i);
				if (ch == '-' || ch == '—' || ch == '–') {
					dashIndex = i;
					break;
				} else if (ch != ' ') {
					break; // Stop if we hit non-space, non-dash character
				}
			}
		}
		
		if (dashIndex < 0) {
			return null; // Could not find blueprint name section
		}
		
		// Check the color of the blueprint name section (BEFORE the dash)
		int stringPos = 0;
		for (int i = 0; i < codePoints.size(); i++) {
			int codePoint = codePoints.get(i);
			net.minecraft.text.Style style = styles.get(i);
			
			// Check if this codePoint is in the blueprint name section (BEFORE the dash)
			if (stringPos < dashIndex) {
				// Check the color only for the blueprint name section
				if (style != null && style.getColor() != null) {
					int styleColor = style.getColor().getRgb();
					int styleColorRGB = styleColor & 0x00FFFFFF; // Remove alpha
					
					// Check for Epic color (floor 1)
					if (styleColorRGB == epicColor1RGB || styleColorRGB == epicColor2RGB) {
						return 1;
					}
					// Check for Legendary color (floor 85)
					if (styleColorRGB == legendaryColor1RGB || styleColorRGB == legendaryColor2RGB) {
						return 85;
					}
				}
			} else {
				// Stop if we've reached the dash
				if (stringPos >= dashIndex) {
					break;
				}
			}
			
			// Update string position based on codePoint
			if (codePoint < 0x10000) {
				stringPos++;
			} else {
				stringPos += 2; // Surrogate pair
			}
		}
		
		return null; // Could not determine floor from color
	}
	
	/**
	 * Appends the floor number tag at the end of the text (after "[Bauplan]")
	 */
	private static Text appendFloorNumberToText(Text originalText, String floorTag) {
		try {
			// Convert to OrderedText to preserve styling
			net.minecraft.text.OrderedText orderedText = originalText.asOrderedText();
			final java.util.List<net.minecraft.text.Style> styles = new java.util.ArrayList<>();
			final java.util.List<Integer> codePoints = new java.util.ArrayList<>();
			
			orderedText.accept((index, style, codePoint) -> {
				styles.add(style);
				codePoints.add(codePoint);
				return true;
			});
			
			// Build new text: original text + floor tag at the end
			net.minecraft.text.MutableText result = Text.empty().copy();
			
			// Add all original text - group consecutive characters with same style
			net.minecraft.text.Style currentStyle = null;
			StringBuilder currentSegment = new StringBuilder();
			
			for (int i = 0; i < codePoints.size(); i++) {
				int codePoint = codePoints.get(i);
				net.minecraft.text.Style style = styles.get(i);
				
				if (currentStyle == null || !currentStyle.equals(style)) {
					// Flush current segment
					if (currentSegment.length() > 0) {
						result.append(Text.literal(currentSegment.toString()).setStyle(currentStyle));
						currentSegment = new StringBuilder();
					}
					currentStyle = style;
				}
				currentSegment.appendCodePoint(codePoint);
			}
			
			// Flush remaining segment
			if (currentSegment.length() > 0) {
				result.append(Text.literal(currentSegment.toString()).setStyle(currentStyle));
			}
			
			// Add floor tag at the end with white color
			net.minecraft.text.Style floorStyle = net.minecraft.text.Style.EMPTY.withColor(0xFFFFFF);
			result.append(Text.literal(floorTag).setStyle(floorStyle));
			
			return result;
		} catch (Exception e) {
			// Fallback: simple string append
			String originalString = originalText.getString();
			String newString = originalString + floorTag;
			return Text.literal(newString);
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
	public static class AspectInfo {
		public final String aspectName;
		public final String aspectDescription;
		
		public AspectInfo(String aspectName, String aspectDescription) {
			this.aspectName = aspectName;
			this.aspectDescription = aspectDescription;
		}
	}
	
	/**
	 * Data class to store license information
	 */
	private static class LicenseInfo {
		public final String name;
		public final String location;
		
		public LicenseInfo(String name, String location) {
			this.name = name;
			this.location = location;
		}
	}
}

