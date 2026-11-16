package net.felix.utilities.Aincraft.blueprints.tooltip;

import java.util.List;
import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.Aincraft.blueprints.data.BlueprintNameAndColor;
import net.felix.utilities.Overall.glyphs.ZeichenUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import static net.felix.utilities.Aincraft.blueprints.chat.BlueprintChatParser.extractBlueprintNameAndColorFromItemName;
import static net.felix.utilities.Aincraft.blueprints.data.BlueprintDatabase.blueprintFloorMap;

/**
 * Haengt Ebenen-Nummern an Bauplan-Namen in Inventaren an.
 */
public final class BlueprintFloorTooltip {

	private BlueprintFloorTooltip() {
	}

	public static void addFloorNumberToBlueprintNames(List<Component> lines, Minecraft client) {
		// Performance-Optimierung: Frühe Returns
		if (lines == null || lines.isEmpty() || client == null) {
			return;
		}
		
		// Check if blueprint floor number display is enabled in config
		if (!CCLiveUtilitiesConfig.HANDLER.instance().showBlueprintFloorNumber) {
			return;
		}
		
		if (client.gui.screen() == null) {
			return;
		}
		
		// Check if we're in a blueprint inventory
		String screenTitle = client.gui.screen().getTitle().getString();
		
		if (ZeichenUtility.shouldHideFloorNumbersInEssenceMenus(screenTitle)) {
			return;
		}
		
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
			Component line = lines.get(i);
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
			Component newLine = appendFloorNumberToText(line, floorTag);
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
	public static Integer getDrachenzahnFloorFromColor(Component itemNameText) {
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
		int epicColor1Rgb = epicColor1 & 0x00FFFFFF;
		int epicColor2Rgb = epicColor2 & 0x00FFFFFF;
		int legendaryColor1Rgb = legendaryColor1 & 0x00FFFFFF;
		int legendaryColor2Rgb = legendaryColor2 & 0x00FFFFFF;
		
		// Convert to OrderedText to iterate through styled parts
		net.minecraft.util.FormattedCharSequence orderedText = itemNameText.getVisualOrderText();
		final StringBuilder fullText = new StringBuilder();
		final java.util.List<net.minecraft.network.chat.Style> styles = new java.util.ArrayList<>();
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
			net.minecraft.network.chat.Style style = styles.get(i);
			
			// Check if this codePoint is in the blueprint name section (BEFORE the dash)
			if (stringPos < dashIndex) {
				// Check the color only for the blueprint name section
				if (style != null && style.getColor() != null) {
					int styleColor = style.getColor().getValue();
					int styleColorRgb = styleColor & 0x00FFFFFF; // Remove alpha
					
					// Check for Epic color (floor 1)
					if (styleColorRgb == epicColor1Rgb || styleColorRgb == epicColor2Rgb) {
						return 1;
					}
					// Check for Legendary color (floor 85)
					if (styleColorRgb == legendaryColor1Rgb || styleColorRgb == legendaryColor2Rgb) {
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
	public static Component appendFloorNumberToText(Component originalText, String floorTag) {
		try {
			// Convert to OrderedText to preserve styling
			net.minecraft.util.FormattedCharSequence orderedText = originalText.getVisualOrderText();
			final java.util.List<net.minecraft.network.chat.Style> styles = new java.util.ArrayList<>();
			final java.util.List<Integer> codePoints = new java.util.ArrayList<>();
			
			orderedText.accept((index, style, codePoint) -> {
				styles.add(style);
				codePoints.add(codePoint);
				return true;
			});
			
			// Build new text: original text + floor tag at the end
			net.minecraft.network.chat.MutableComponent result = Component.empty().copy();
			
			// Add all original text - group consecutive characters with same style
			net.minecraft.network.chat.Style currentStyle = null;
			StringBuilder currentSegment = new StringBuilder();
			
			for (int i = 0; i < codePoints.size(); i++) {
				int codePoint = codePoints.get(i);
				net.minecraft.network.chat.Style style = styles.get(i);
				
				if (currentStyle == null || !currentStyle.equals(style)) {
					// Flush current segment
					if (currentSegment.length() > 0) {
						result.append(Component.literal(currentSegment.toString()).setStyle(currentStyle));
						currentSegment = new StringBuilder();
					}
					currentStyle = style;
				}
				currentSegment.appendCodePoint(codePoint);
			}
			
			// Flush remaining segment
			if (currentSegment.length() > 0) {
				result.append(Component.literal(currentSegment.toString()).setStyle(currentStyle));
			}
			
			// Add floor tag at the end with white color
			net.minecraft.network.chat.Style floorStyle = net.minecraft.network.chat.Style.EMPTY.withColor(0xFFFFFF);
			result.append(Component.literal(floorTag).setStyle(floorStyle));
			
			return result;
		} catch (Exception e) {
			// Fallback: simple string append
			String originalString = originalText.getString();
			String newString = originalString + floorTag;
			return Component.literal(newString);
		}
	}
}
