package net.felix.features.information.blueprints;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;

import static net.felix.features.information.hover.HoverEventReader.extractHoverTextFromEvent;
import static net.felix.features.information.hover.HoverEventReader.findHoverEventInText;


/**
 * Liest Bauplan-Namen und Farben aus Chat und Item-Namen.
 */
public final class BlueprintChatParser {

	private BlueprintChatParser() {
	}

	/**
	 * Extracts item name from hover event in chat message
	 * Works for all items, not just blueprints
	 * Returns the first line of the hover event text, which is typically the item name
	 */
	public static String extractItemNameFromHoverEvent(Component message) {
		if (message == null) {
			return null;
		}
		
		// Find hover event in message
		HoverEvent hoverEvent = findHoverEventInText(message);
		if (hoverEvent == null || hoverEvent.action() != HoverEvent.Action.SHOW_TEXT) {
			return null;
		}
		
		// Extract hover text from event
		Component hoverText = extractHoverTextFromEvent(hoverEvent, message);
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
	 * Extracts blueprint name and color from chat message text
	 * Returns the name and rarity (epic or legendary) based on color
	 * Epic: #A134EB, Legendary: #FC7E00
	 */
	public static BlueprintNameAndColor extractBlueprintNameAndColorFromChatMessage(Component message) {
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
		net.minecraft.util.FormattedCharSequence orderedText = message.getVisualOrderText();
		orderedText.accept((index, style, codePoint) -> {
			// Check if this style has one of the target colors
			boolean hasTargetColor = false;
			String currentRarity = null;
			if (style != null && style.getColor() != null) {
				int styleColor = style.getColor().getValue();
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
	 * Extracts blueprint name and color from item name text (for inventory).
	 * Rarity follows the blueprint name color: common (white/default), uncommon (#1EFF00),
	 * rare (#0070DD), epic (#A134EB / #A335EE), legendary (#FC7E00 / #FF8000).
	 * Only considers the blueprint name (before " - " and "[Bauplan]").
	 */
	public static BlueprintNameAndColor extractBlueprintNameAndColorFromItemName(Component itemNameText) {
		if (itemNameText == null) {
			return null;
		}
		
		// Epic: #A134EB or #A335EE; Legendary: #FC7E00 or #FF8000
		int epicColor1 = 0xFFA134EB;
		int epicColor2 = 0xFFA335EE;
		int legendaryColor1 = 0xFFFC7E00;
		int legendaryColor2 = 0xFFFF8000;
		int epicColor1RGB = epicColor1 & 0x00FFFFFF;
		int epicColor2RGB = epicColor2 & 0x00FFFFFF;
		int legendaryColor1RGB = legendaryColor1 & 0x00FFFFFF;
		int legendaryColor2RGB = legendaryColor2 & 0x00FFFFFF;
		// Uncommon / rare (Cactus Clicker style blueprint rows)
		int uncommonColorRGB = 0x001EFF00;
		int rareColorRGB = 0x000070DD;
		int commonWhiteRGB = 0x00FFFFFF;
		
		// First, collect all text with their styles to find the blueprint name section
		final java.util.List<net.minecraft.network.chat.Style> styles = new java.util.ArrayList<>();
		final java.util.List<Integer> codePoints = new java.util.ArrayList<>();
		final StringBuilder fullText = new StringBuilder();
		
		net.minecraft.util.FormattedCharSequence orderedText = itemNameText.getVisualOrderText();
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
		
		int bauplanStart = fullTextStr.indexOf("[Bauplan]");
		if (bauplanStart > 0) {
			bauplanIndex = bauplanStart;
			// Prefer " - " so names with inner hyphens still work; also handles " - §3[Bauplan]" in plain strings
			int sep = fullTextStr.lastIndexOf(" - ", bauplanStart);
			if (sep >= 0) {
				dashIndex = sep + 1; // index of '-' in " - "
			} else {
				// "Meister-Rute [Bauplan]" has no " - " separator — use [Bauplan] as boundary, not inner hyphens
				dashIndex = bauplanStart;
			}
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
			net.minecraft.network.chat.Style style = styles.get(i);
			
			// Check if this codePoint is in the blueprint name section (BEFORE the dash)
			if (stringPos < dashIndex) {
				blueprintName.appendCodePoint(codePoint);
				
				// First matching tier wins (prefix glyphs before the colored name are skipped if still null)
				if (detectedRarity[0] == null && style != null && style.getColor() != null) {
					int styleColor = style.getColor().getValue();
					int styleColorRGB = styleColor & 0x00FFFFFF;
					if (styleColorRGB == epicColor1RGB || styleColorRGB == epicColor2RGB) {
						detectedRarity[0] = "epic";
					} else if (styleColorRGB == legendaryColor1RGB || styleColorRGB == legendaryColor2RGB) {
						detectedRarity[0] = "legendary";
					} else if (styleColorRGB == rareColorRGB) {
						detectedRarity[0] = "rare";
					} else if (styleColorRGB == uncommonColorRGB) {
						detectedRarity[0] = "uncommon";
					} else if (styleColorRGB == commonWhiteRGB) {
						detectedRarity[0] = "common";
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
		
		if (result.isEmpty()) {
			return null;
		}
		// Default white / unstyled names to common so tooltip + shop scan match chat-stored keys
		String rarity = detectedRarity[0] != null ? detectedRarity[0] : "common";
		return new BlueprintNameAndColor(result, rarity);
	}

	/**
	 * Checks if a Text object contains any of the Epic colors (#A134EB or #A335EE)
	 * Returns true if any part of the text has one of these colors
	 */
	public static boolean hasEpicColor(Component text) {
		if (text == null) {
			return false;
		}
		
		// Epic colors: #A134EB (RGB: 161, 52, 235) OR #A335EE (RGB: 163, 53, 238)
		int epicColor1 = 0xFFA134EB;
		int epicColor2 = 0xFFA335EE;
		int epicColor1RGB = epicColor1 & 0x00FFFFFF;
		int epicColor2RGB = epicColor2 & 0x00FFFFFF;
		
		// Convert to OrderedText to iterate through styled parts
		net.minecraft.util.FormattedCharSequence orderedText = text.getVisualOrderText();
		final boolean[] hasEpicColor = {false};
		
		orderedText.accept((index, style, codePoint) -> {
			if (style != null && style.getColor() != null) {
				int styleColor = style.getColor().getValue();
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
	public static String extractBlueprintNameFromChatMessage(Component message) {
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
		net.minecraft.util.FormattedCharSequence orderedText = message.getVisualOrderText();
		orderedText.accept((index, style, codePoint) -> {
			// Check if this style has one of the target colors
			boolean hasTargetColor = false;
			if (style != null && style.getColor() != null) {
				int styleColor = style.getColor().getValue();
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
}
