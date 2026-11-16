package net.felix.utilities.Aincraft.information.gadgets;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.network.chat.Component;

import static net.felix.utilities.Aincraft.information.gadgets.GadgetDatabase.gadgetsDatabase;
import static net.felix.utilities.Overall.util.NumeralConversion.convertToRomanNumeral;
import static net.felix.utilities.Overall.util.NumeralConversion.romanToArabic;

/**
 * Gadget-Informationen in Modul- und Ingenieur-Inventaren.
 */
public final class GadgetTooltip {

	private GadgetTooltip() {
	}

	/**
	 * Checks for gadget information in tooltip lines and adds location information
	 * @param lines The tooltip lines
	 * @param isEngineerInventory If true, reads level from line 3 with format "[Stufe]: "LEVEL"", otherwise from "Stufe: [LEVEL]"
	 */
	public static void checkForGadgetInformation(List<Component> lines, boolean isEngineerInventory) {
		if (gadgetsDatabase.isEmpty()) {
			return;
		}
		
		// Find the line with level information and extract level, and identify the item
		String itemName = null;
		String level = null;
		int stufeLineIndex = -1;
		
		// First pass: identify the item by checking all lines for gadget names/aliases
		for (int i = 0; i < lines.size(); i++) {
			Component line = lines.get(i);
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
				Component line = lines.get(i);
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
				Component line = lines.get(i);
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
					// Format: " → Nächstes Level: location" (without quotes)
					Component locationText = Component.literal(" → Nächstes Level: " + location)
						.withStyle(style -> style.withColor(0xC0C0C0)); // Light gray
					
					lines.add(stufeLineIndex + 1, locationText);
				}
			}
		}
	}
}
