package net.felix.features.information.licenses;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import static net.felix.features.information.licenses.LicenseDatabase.licensesDatabase;


/**
 * Lizenz-Informationen in Tooltips.
 */
public final class LicenseTooltip {

	private LicenseTooltip() {
	}

	// License slots that should be checked
	private static final int[] LICENSE_SLOTS = {1, 2, 3, 10, 11, 12, 19, 20, 21};

	/**
	 * Checks if the hovered item is in one of the license slots
	 */
	public static boolean isItemInLicenseSlot(ItemStack stack, Minecraft client) {
		if (client.gui.screen() == null || !(client.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen)) {
			return false;
		}
		
		net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> screen = (net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>) client.gui.screen();
		net.minecraft.world.inventory.AbstractContainerMenu handler = screen.getMenu();
		
		// Check if the hovered item is in one of the license slots
		for (int slotIndex : LICENSE_SLOTS) {
			if (slotIndex < handler.slots.size()) {
				net.minecraft.world.inventory.Slot slot = handler.slots.get(slotIndex);
				if (slot.hasItem() && slot.getItem() == stack) {
					return true;
				}
			}
		}
		
		// Also check by comparing item stacks (in case of reference mismatch)
		for (int slotIndex : LICENSE_SLOTS) {
			if (slotIndex < handler.slots.size()) {
				net.minecraft.world.inventory.Slot slot = handler.slots.get(slotIndex);
				if (slot.hasItem()) {
					ItemStack slotStack = slot.getItem();
					if (ItemStack.matches(stack, slotStack)) {
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
	public static void checkForLicenseInformation(List<Component> lines, Minecraft client) {
		// Check if database is loaded
		if (licensesDatabase.isEmpty()) {
			return;
		}
		
		for (int i = 0; i < lines.size(); i++) {
			Component line = lines.get(i);
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
						Component locationText = Component.literal(" → " + licenseInfo.location)
							.withStyle(style -> style.withColor(0xC0C0C0)); // Light gray
						
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
						Component locationText = Component.literal(" → " + licenseInfo.location)
							.withStyle(style -> style.withColor(0xC0C0C0)); // Light gray
						
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
}
