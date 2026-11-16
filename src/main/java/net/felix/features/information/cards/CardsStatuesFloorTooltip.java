package net.felix.features.information.cards;

import java.util.List;
import java.util.Map;
import net.felix.utilities.Overall.glyphs.ZeichenUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import static net.felix.features.information.blueprints.BlueprintFloorTooltip.appendFloorNumberToText;
import static net.felix.features.information.cards.CardsStatuesDatabase.cardsFloors;
import static net.felix.features.information.cards.CardsStatuesDatabase.statuesFloors;

/**
 * Haengt Ebenen-Nummern an Karten- und Statuen-Namen an.
 */
public final class CardsStatuesFloorTooltip {

	private CardsStatuesFloorTooltip() {
	}

	/**
	 * Adds floor numbers to cards and statues names in inventories
	 * Only works in inventories that contain the cards/statues characters (㭆 or 㭂)
	 */
	public static void addFloorNumberToCardsStatuesNames(List<Component> lines, Minecraft client) {
		if (client == null || client.gui.screen() == null) {
			return;
		}
		
		// Check if we're in a cards/statues inventory
		String screenTitle = client.gui.screen().getTitle().getString();
		if (screenTitle == null) {
			return;
		}
		
		// Check if the inventory contains one of the cards/statues characters
		boolean isCardsInventory = ZeichenUtility.isCardsMenuTitle(screenTitle);
		boolean isStatuesInventory = ZeichenUtility.isStatuesMenuTitle(screenTitle);
		
		if (!isCardsInventory && !isStatuesInventory) {
			return;
		}
		
		// Look for lines containing "[Karte]" or "[Statue]"
		for (int i = 0; i < lines.size(); i++) {
			Component line = lines.get(i);
			String lineText = line.getString();
			
			if (lineText == null) {
				continue;
			}
			
			boolean isCard = false;
			boolean isStatue = false;
			String itemName = null;
			
			// Check for [Karte] or [Statue]
			if (lineText.contains("[Karte]")) {
				isCard = true;
				// Extract name: everything before "[Karte]"
				int index = lineText.indexOf("[Karte]");
				if (index > 0) {
					itemName = lineText.substring(0, index).trim();
					// Remove any leading dashes or special characters
					itemName = itemName.replaceAll("^[-\\s]+", "").trim();
					// Remove formatting codes
					itemName = itemName.replaceAll("§[0-9a-fk-or]", "").trim();
					// Remove Unicode formatting characters (like the ones used in inventories)
					itemName = itemName.replaceAll("[\\u3400-\\u4DBF\\u4E00-\\u9FFF]", "").trim();
					// Remove trailing dashes
					itemName = itemName.replaceAll("\\s*-\\s*$", "").trim();
				}
			} else if (lineText.contains("[Statue]")) {
				isStatue = true;
				// Extract name: everything before "[Statue]"
				int index = lineText.indexOf("[Statue]");
				if (index > 0) {
					itemName = lineText.substring(0, index).trim();
					// Remove any leading dashes or special characters
					itemName = itemName.replaceAll("^[-\\s]+", "").trim();
					// Remove formatting codes
					itemName = itemName.replaceAll("§[0-9a-fk-or]", "").trim();
					// Remove Unicode formatting characters (like the ones used in inventories)
					itemName = itemName.replaceAll("[\\u3400-\\u4DBF\\u4E00-\\u9FFF]", "").trim();
					// Remove trailing dashes
					itemName = itemName.replaceAll("\\s*-\\s*$", "").trim();
				}
			}
			
			if (itemName == null || itemName.isEmpty()) {
				continue;
			}
			
			// Check if floor number is already added
			if (lineText.contains("[e")) {
				continue; // Already has floor number
			}
			
			// Get floor numbers from database
			List<Integer> floors = null;
			if (isCard) {
				// Try exact match first
				floors = cardsFloors.get(itemName);
				// If not found, try case-insensitive search
				if (floors == null) {
					for (Map.Entry<String, List<Integer>> entry : cardsFloors.entrySet()) {
						if (entry.getKey().equalsIgnoreCase(itemName)) {
							floors = entry.getValue();
							break;
						}
					}
				}
			} else if (isStatue) {
				// Try exact match first
				floors = statuesFloors.get(itemName);
				// If not found, try case-insensitive search
				if (floors == null) {
					for (Map.Entry<String, List<Integer>> entry : statuesFloors.entrySet()) {
						if (entry.getKey().equalsIgnoreCase(itemName)) {
							floors = entry.getValue();
							break;
						}
					}
				}
			}
			
			if (floors == null || floors.isEmpty()) {
				continue; // No floor data found
			}
			
			// Format floor numbers: if multiple floors, show as "e1, e22", if single, show as "e1"
			String floorTag;
			if (floors.size() == 1) {
				floorTag = " [e" + floors.get(0) + "]";
			} else {
				// Multiple floors: " [e1, e22]"
				StringBuilder floorBuilder = new StringBuilder(" [");
				for (int j = 0; j < floors.size(); j++) {
					if (j > 0) {
						floorBuilder.append(", ");
					}
					floorBuilder.append("e").append(floors.get(j));
				}
				floorBuilder.append("]");
				floorTag = floorBuilder.toString();
			}
			
			// Append the floor tag at the end of the text, preserving styling
			Component newLine = appendFloorNumberToText(line, floorTag);
			if (newLine != null) {
				lines.set(i, newLine);
			}
		}
	}
}
