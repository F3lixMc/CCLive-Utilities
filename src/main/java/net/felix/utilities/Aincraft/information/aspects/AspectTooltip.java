package net.felix.utilities.Aincraft.information.aspects;

import java.util.List;
import net.felix.CCLiveUtilitiesConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import static net.felix.utilities.Aincraft.information.aspects.AspectDatabase.aspectsDatabase;
import static net.felix.utilities.Aincraft.information.aspects.AspectDatabase.extractBracketAspectNameFromStarLine;
import static net.felix.utilities.Aincraft.information.aspects.AspectDatabase.getAspectInfoByTooltipDisplayName;
import static net.felix.utilities.Aincraft.information.blueprints.BlueprintChatParser.hasEpicColor;

/**
 * Zeigt Aspektnamen in Bauplan-Tooltips.
 */
public final class AspectTooltip {

	private AspectTooltip() {
	}

	/**
	 * Adds aspect name to tooltip (in blueprint inventories only)
	 */
	public static void addAspectNameToTooltip(List<Component> lines, Minecraft client, ItemStack stack) {
		// Performance-Optimierung: Frühe Returns
		if (lines == null || client == null || stack == null) {
			return;
		}
		
		// Check if aspect overlay is enabled in config
		if (!CCLiveUtilitiesConfig.HANDLER.instance().aspectOverlayEnabled) {
			return; // Don't show aspect information if aspect overlay is disabled
		}
		
		// Performance-Optimierung: Prüfe Screen früher
		if (client.gui.screen() == null) {
			return;
		}
		
		// First, extract the item name from the tooltip (usually the first line)
		String itemName = extractItemNameFromTooltip(lines, stack);
		
		// Check for lines containing "⭐" (star symbol) - this is for items with aspect info in tooltip
		for (int i = 0; i < lines.size(); i++) {
			Component line = lines.get(i);
			String lineText = line.getString();
			
			// Skip if lineText is null or empty
			if (lineText == null || lineText.isEmpty()) {
				continue;
			}
			
			// Check if this line contains "⭐"
			if (!lineText.contains("⭐")) {
				continue;
			}

			AspectInfo aspectInfo = null;
			String lineLower = lineText.toLowerCase();
			boolean isTransferred = lineLower.contains("(transferiert)");

			if (isTransferred) {
				String tooltipAspectName = extractBracketAspectNameFromStarLine(lineText);
				aspectInfo = getAspectInfoByTooltipDisplayName(tooltipAspectName);
			} else if (itemName != null && !itemName.isEmpty()) {
				aspectInfo = aspectsDatabase.get(itemName);
			}

			if (aspectInfo != null && !aspectInfo.aspectName.equals("-") && itemName != null && !itemName.isEmpty()) {
				Component modifiedLine = modifyStarLineWithShiftInfo(line, aspectInfo, itemName);
				if (modifiedLine != null) {
					lines.set(i, modifiedLine);
					net.felix.utilities.Overall.aspekte.AspectOverlay.updateAspectInfoFromName(itemName, aspectInfo);
				}
			}
		}
		
		// Note: The "⭐" check above works in ALL inventories, not just blueprint inventories
		// The code below is only for blueprint items with "[Bauplan]" in the tooltip
		
		boolean isInBlueprintInventory = false;
		
		// Check if we're in a blueprint inventory
		String screenTitle = client.gui.screen().getTitle().getString();
		
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
								  cleanScreenTitle.contains("Favorisierte [Werkzeugbaupläne]") ||
								  cleanScreenTitle.contains("CACTUS_CLICKER.blueprints.favorites.title.tools");
		
		// Only process blueprint items if we're in a blueprint inventory
		// Items with "⭐" are processed above and work in all inventories
		if (!isInBlueprintInventory) {
			return; // Don't show aspect information for blueprint items in other inventories
		}
		
		// Check tooltip lines for inventory tooltips (they contain "[Bauplan]")
		String itemNameToCheck = null;
		Component blueprintLine = null;
		for (int i = 0; i < lines.size(); i++) {
			Component line = lines.get(i);
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
		// Note: itemName from tooltip extraction above is for "⭐" items, here we need a separate variable for blueprint items
		String blueprintItemName = null;
		if (itemNameToCheck != null && itemNameToCheck.contains("[Bauplan]")) {
				// Extract the item name (everything before "[Bauplan]")
			blueprintItemName = itemNameToCheck.substring(0, itemNameToCheck.indexOf("[Bauplan]")).trim();
				
				// Remove leading dash/minus if present
				if (blueprintItemName.startsWith("-")) {
					blueprintItemName = blueprintItemName.substring(1).trim();
				}
				
				// Remove trailing dash/minus if present
				if (blueprintItemName.endsWith("-")) {
					blueprintItemName = blueprintItemName.substring(0, blueprintItemName.length() - 1).trim();
				}
				
				// Remove Minecraft formatting codes and Unicode characters
				blueprintItemName = blueprintItemName.replaceAll("§[0-9a-fk-or]", "");
				blueprintItemName = blueprintItemName.replaceAll("[\\u3400-\\u4DBF]", "");
				blueprintItemName = blueprintItemName.replaceAll("[^a-zA-ZäöüßÄÖÜ\\s-]", "").trim();
		}
				
				// Look for this item in the aspects database
		if (blueprintItemName != null && !blueprintItemName.isEmpty()) {
				AspectInfo aspectInfo = aspectsDatabase.get(blueprintItemName);
				if (aspectInfo != null) {
				// Find the position of "Benötigt" line
				int benötigtPosition = -1;
				for (int i = 0; i < lines.size(); i++) {
					Component line = lines.get(i);
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
					Component aspectNameText = Component.literal("Enthält: ")
						.withStyle(style -> style.withColor(0xFFFFFFFF)) // White color
						.append(Component.literal(aspectInfo.aspectName)
							.withStyle(style -> style.withColor(0xFFFCA800))); // Same color as overlay (#FCA800)
					
					// Insert aspect name at the target position
					lines.add(targetPosition, aspectNameText);
					
					// Add "(Shift für mehr Info)" on the next line
					Component shiftInfoText = Component.literal("(Shift für mehr Info)")
						.withStyle(style -> style.withColor(0xFFCCCCCC)); // Light gray color
					lines.add(targetPosition + 1, shiftInfoText);
					
				// Add empty line after shift info (only if we're not inserting before "Benötigt")
				if (benötigtPosition < 0) {
					Component emptyLineText = Component.literal(" ");
					lines.add(targetPosition + 2, emptyLineText);
				}
			}
		}
	}

	/**
	 * Extracts the item name from the tooltip lines
	 * Usually the first line contains the item name, but we need to clean it
	 * @param lines The tooltip lines
	 * @param stack The item stack (as fallback)
	 * @return The cleaned item name, or null if not found
	 */
	public static String extractItemNameFromTooltip(List<Component> lines, ItemStack stack) {
		if (lines == null || lines.isEmpty()) {
			return null;
		}
		
		// Try to get item name from first line (usually the item name)
		if (lines.size() > 0) {
			Component firstLine = lines.get(0);
			if (firstLine != null) {
				String itemName = firstLine.getString();
				if (itemName != null && !itemName.isEmpty()) {
					// Remove Minecraft formatting codes
					itemName = itemName.replaceAll("§[0-9a-fk-or]", "");
					itemName = itemName.replaceAll("§#[0-9a-fA-F]{6}", "");
					
					// Remove Unicode formatting characters (like 㔜㔙㔕)
					itemName = itemName.replaceAll("[\\u3400-\\u4DBF]", "");
					
					// Remove any leading/trailing whitespace
					itemName = itemName.trim();
					
					// Remove prefixes like [+XXX] at the beginning
					itemName = itemName.replaceAll("^\\[\\+[^\\]]+\\]\\s*", "");
					
					// Remove suffixes like [SCHMIEDEZUSTAND] at the end
					itemName = itemName.replaceAll("\\s*\\[[^\\]]+\\]$", "");
					
					// Remove any other brackets and their content (like [Bauplan], etc.)
					// But keep the item name itself
					itemName = itemName.replaceAll("\\[[^\\]]+\\]", "");
					
					// Remove any leading/trailing whitespace again
					itemName = itemName.trim();
					
					if (!itemName.isEmpty()) {
						return itemName;
					}
				}
			}
		}
		
		// Fallback: try to get name from ItemStack
		if (stack != null && !stack.isEmpty()) {
			Component displayName = stack.getHoverName();
			if (displayName != null) {
				String itemName = displayName.getString();
				if (itemName != null && !itemName.isEmpty()) {
					// Remove Minecraft formatting codes
					itemName = itemName.replaceAll("§[0-9a-fk-or]", "");
					itemName = itemName.replaceAll("§#[0-9a-fA-F]{6}", "");
					// Remove Unicode formatting characters
					itemName = itemName.replaceAll("[\\u3400-\\u4DBF]", "");
					
					// Remove prefixes like [+XXX] at the beginning
					itemName = itemName.replaceAll("^\\[\\+[^\\]]+\\]\\s*", "");
					
					// Remove suffixes like [SCHMIEDEZUSTAND] at the end
					itemName = itemName.replaceAll("\\s*\\[[^\\]]+\\]$", "");
					
					// Remove any other brackets and their content
					itemName = itemName.replaceAll("\\[[^\\]]+\\]", "");
					
					itemName = itemName.trim();
					
					return itemName;
				}
			}
		}
		
		return null;
	}

	/**
	 * Modifies a line containing "⭐ [ASPEKT DES ITEMS]" to add "(shift für info)" after the "]"
	 * @param originalLine The original Text line
	 * @param aspectInfo The aspect info for this item
	 * @param itemName The item name
	 * @return The modified Text line, or null if modification failed
	 */
	private static Component modifyStarLineWithShiftInfo(Component originalLine, AspectInfo aspectInfo, String itemName) {
		if (originalLine == null || aspectInfo == null || itemName == null) {
			return null;
		}
		
		String lineText = originalLine.getString();
		if (lineText == null || !lineText.contains("⭐") || !lineText.contains("]")) {
			return null;
		}
		
		// Check if "(shift für info)" is already in the line
		if (lineText.contains("(Shift für info)") || lineText.contains("(Shift für Info)")) {
			return originalLine; // Already modified
		}
		
		// Find the position of "]" after "⭐"
		int starIndex = lineText.indexOf("⭐");
		int bracketEnd = lineText.indexOf("]", starIndex);
		if (bracketEnd == -1) {
			return null;
		}
		
		// Create the shift info text in italic and gray
		net.minecraft.network.chat.MutableComponent shiftInfoText = Component.literal(" (Shift für Info)")
			.withStyle(style -> style
				.withColor(0xFF808080) // Gray color
				.withItalic(true)); // Italic
		
		// Copy the original line to preserve its format and structure
		net.minecraft.network.chat.MutableComponent result = originalLine.copy();
		
		// Simply append the shift info text as a sibling
		// This preserves the original format while adding the new text
		result.append(shiftInfoText);
		
		return result;
	}
}
