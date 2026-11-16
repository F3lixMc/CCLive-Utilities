package net.felix.features.information.tooltip;

import net.felix.CCLiveUtilitiesConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Pattern;
import net.felix.utilities.Overall.ZeichenUtility;
import net.felix.features.information.materials.MaterialFloorInfo;
import net.felix.features.information.materials.MaterialInfo;

import static net.felix.features.information.aspects.AspectTooltip.addAspectNameToTooltip;
import static net.felix.features.information.blueprints.BlueprintFloorTooltip.addFloorNumberToBlueprintNames;
import static net.felix.features.information.cards.CardsStatuesDatabase.cardsEffects;
import static net.felix.features.information.cards.CardsStatuesDatabase.statuesEffects;
import static net.felix.features.information.cards.CardsStatuesFloorTooltip.addFloorNumberToCardsStatuesNames;
import static net.felix.features.information.essences.EssenceBagTooltip.addEssenceBagWaveDisplayToTooltip;
import static net.felix.features.information.essences.EssenceWaveNames.getWaveDisplayForEssenceName;
import static net.felix.features.information.gadgets.GadgetTooltip.checkForGadgetInformation;
import static net.felix.features.information.licenses.LicenseTooltip.checkForLicenseInformation;
import static net.felix.features.information.licenses.LicenseTooltip.isItemInLicenseSlot;
import static net.felix.features.information.materials.MaterialDatabase.materialsDatabase;
import static net.felix.features.information.moblexicon.MoblexiconTooltip.addMoblexiconHpToTooltip;
import static net.felix.features.information.ponds.FishingMaterials.appendFishingMaterialLocationToLine;
import static net.felix.features.information.ponds.PondTooltip.checkForPondInformation;
import static net.felix.features.information.tooltip.ClipboardPinTooltip.addClipboardPinTextToTooltip;
import static net.felix.features.information.materials.MaterialRarityColors.formatMaterialLocationSuffix;
import static net.felix.features.information.materials.MaterialRarityColors.getMaterialLocationRarityColor;


/**
 * Tooltip-Callback: ergaenzt Item-Tooltips um Aspekt-, Ebenen-, Material-, Lizenz-, Gadget- und Moblexikon-Informationen.
 */
public final class InformationTooltipHandler {

	private InformationTooltipHandler() {
	}

	/**
	 * Tooltip callback: adds aspect, floor, material, license, gadget and moblexicon information to item tooltips.
	 */
	public static void onItemTooltip(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.Item.TooltipContext context, net.minecraft.world.item.TooltipFlag tooltipType, List<Component> lines) {
		// Performance-Optimierung: Frühe Returns
		if (stack == null || lines == null || lines.isEmpty()) {
			return;
		}
		
		Minecraft client = Minecraft.getInstance();
		if (client == null) {
			return;
		}
		
		// Always process aspect information (even if informationenUtilityEnabled is off)
		// Add aspect name to tooltip (always visible) - works in inventories
		addAspectNameToTooltip(lines, client, stack);
		
		// Add floor number to blueprint names in inventories
		addFloorNumberToBlueprintNames(lines, client);
		
		// Add floor numbers to cards and statues names in inventories
		addFloorNumberToCardsStatuesNames(lines, client);
		
		// Add clipboard pin text input for forge inventories (before other processing)
		addClipboardPinTextToTooltip(lines, client);
		
		// Only process other information if Informationen Utility is enabled in config
		if (!CCLiveUtilitiesConfig.isModRuntimeEnabled() ||
			!CCLiveUtilitiesConfig.HANDLER.instance().informationenUtilityEnabled) {
			return;
		}
		
		// Check if we're in the special inventory (Moblexicon)
		boolean isSpecialInventory = false;
		boolean isLicenseInventory = false;
		boolean isQuickTravelInventory = false;
		boolean isMaterialBagInventory = false;
		boolean hideFloorNumbersInEssenceMenu = false;
		String screenTitle = "";
		if (client.gui.screen() != null) {
			screenTitle = client.gui.screen().getTitle().getString();
			if (ZeichenUtility.containsMoblexicon(screenTitle)) {
				isSpecialInventory = true;
			}
			// Check for license inventory character (friends_request_accept_deny)
			if (ZeichenUtility.containsFriendsRequestAcceptDeny(screenTitle)) {
				isLicenseInventory = true;
			}
			if (ZeichenUtility.containsFarmzoneQuickTravel(screenTitle)) {
				isQuickTravelInventory = true;
			}
			// Essence-Bag, -Auswahl, Harvester: keine Ebenen-Nummern
			hideFloorNumbersInEssenceMenu = ZeichenUtility.shouldHideFloorNumbersInEssenceMenus(screenTitle);
			isMaterialBagInventory = ZeichenUtility.containsUiMaterialBag(screenTitle)
					|| ZeichenUtility.containsUiRessourceBag(screenTitle)
					|| screenTitle.contains("Mob-Materialien");
		}
		
		if (CCLiveUtilitiesConfig.isModRuntimeEnabled() &&
			CCLiveUtilitiesConfig.HANDLER.instance().informationenUtilityEnabled &&
			CCLiveUtilitiesConfig.HANDLER.instance().showWaveDisplay &&
			client.gui.screen() != null &&
			ZeichenUtility.containsEssenceBagUi(screenTitle)) {
			addEssenceBagWaveDisplayToTooltip(lines, stack);
		}
		
		if (hideFloorNumbersInEssenceMenu) {
			return;
		}

		// Teich-Zuordnung in Schnellreise-Inventaren (unabhängig von Ebenen-Einstellung)
		if (isQuickTravelInventory && CCLiveUtilitiesConfig.HANDLER.instance().showLicenseInformation) {
			checkForPondInformation(lines);
		}
		
		if (isSpecialInventory && CCLiveUtilitiesConfig.HANDLER.instance().moblexiconHpEnabled) {
			addMoblexiconHpToTooltip(lines);
		}
		
		// Check if the respective setting is enabled for this inventory type
		boolean showFloorInfoInCurrentInventory = isSpecialInventory
				? CCLiveUtilitiesConfig.HANDLER.instance().showEbenenInSpecialInventory
				: CCLiveUtilitiesConfig.HANDLER.instance().showEbenenInNormalInventories;

		if (isSpecialInventory && !showFloorInfoInCurrentInventory
				&& !CCLiveUtilitiesConfig.HANDLER.instance().moblexiconCardsStatuesInfoEnabled) {
			return;
		}
		if (!isSpecialInventory && !showFloorInfoInCurrentInventory) {
			return;
		}
		
		// In special inventory (㬉), check all lines for [Karte]/[Statue] but only first line for material names
		// In other inventories, check all lines for material names
		int startIndex = 0;
		int endIndex = lines.size(); // Always check all lines to find [Karte] or [Statue]
		
		for (int i = startIndex; i < endIndex; i++) {
			Component line = lines.get(i);
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
				
				if (CCLiveUtilitiesConfig.HANDLER.instance().showWaveDisplay) {
					String waveText = getWaveDisplayForEssenceName(essenceNameToSearch);
					Component essenceInfoText = Component.literal(" -> Welle: ")
						.withStyle(style -> style.withColor(0xC0C0C0)) // Light gray
						.append(Component.literal(waveText)
							.withStyle(style -> style.withColor(0x55FF55))); // Light green
					
					// Add the essence info line after the current line
					lines.add(i + 1, essenceInfoText);
					
					// Skip the next line since we just added essence info
					i++;
					continue; // Skip other processing for this line
				}
			}
			
			// Skip Essenz items that should not show level information (but allow essence improvement inventory)
			if (screenTitle.contains("Essenz [Auswahl]") || screenTitle.contains("Essenz-Tasche") ||
				screenTitle.contains("Essenzernter") || ZeichenUtility.containsLegendPlusUiBackground(screenTitle) ||
				screenTitle.contains("Machtkristalle Verbessern") ||
				screenTitle.contains("Fabrik Skins") || screenTitle.contains("Fabrik Skins Menü") ||
				screenTitle.contains("Factory Bridge Skins") ||
				screenTitle.contains("Factory Tower Skins") ||
				screenTitle.contains("Factory Field Skins") ||
				screenTitle.contains("Factory Banner Skins") || screenTitle.contains("Factory Banner Slots") || 
				
				 ZeichenUtility.containsHunterUiBackground(screenTitle)){ //Hunter ui_background
				
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
			
			// Process lines with [Karte] or [Statue] in Moblexicon to add effect information
			if ((lineText.contains("[Karte]") || lineText.contains("[Statue]")) && isSpecialInventory
					&& CCLiveUtilitiesConfig.HANDLER.instance().moblexiconCardsStatuesInfoEnabled) {
				// Extract the name (text before [Karte] or [Statue])
				String name = "";
				String effect = null;
				boolean isCard = false;
				
				if (lineText.contains("[Karte]")) {
					int index = cleanLineText.indexOf("[Karte]");
					if (index > 0) {
						name = cleanLineText.substring(0, index).trim();
						// Remove any leading dashes or special characters
						name = name.replaceAll("^[-\\s]+", "").trim();
						effect = cardsEffects.get(name);
						isCard = true;
						
						// Try to find the card with case-insensitive search if not found
						if (effect == null) {
							for (Map.Entry<String, String> entry : cardsEffects.entrySet()) {
								if (entry.getKey().equalsIgnoreCase(name)) {
									effect = entry.getValue();
									break;
								}
							}
						}
					}
				} else if (lineText.contains("[Statue]")) {
					int index = cleanLineText.indexOf("[Statue]");
					if (index > 0) {
						name = cleanLineText.substring(0, index).trim();
						// Remove any leading dashes or special characters
						name = name.replaceAll("^[-\\s]+", "").trim();
						effect = statuesEffects.get(name);
						isCard = false;
						
						// Try to find the statue with case-insensitive search if not found
						if (effect == null) {
							for (Map.Entry<String, String> entry : statuesEffects.entrySet()) {
								if (entry.getKey().equalsIgnoreCase(name)) {
									effect = entry.getValue();
									break;
								}
							}
						}
					}
				}
				
				// If effect found, append it to the line
				if (effect != null && !effect.isEmpty()) {
					// Determine color: Green (0x55FF55) for cards, Aqua (0x55FFFF) for statues
					int effectColor = isCard ? 0x55FF55 : 0x55FFFF; // Green for cards, Aqua for statues
					
					// Create new text: original line (with preserved formatting) + " - " + effect
					MutableComponent newLine = line.copy() // Preserve original formatting and style
						.append(Component.literal(" - ").withStyle(style -> style.withColor(0xC0C0C0))) // Light gray separator
						.append(Component.literal(effect).withStyle(style -> style.withColor(effectColor))); // Green for cards, Aqua for statues
					
					// Replace the line
					lines.set(i, newLine);
				}
				continue; // Skip further processing for this line
			}
			
			// Skip if the line contains [Karte] or [Statue] in non-special inventories
			if (lineText.contains("[Karte]") || lineText.contains("[Statue]")) {
				continue;
			}

			if (!showFloorInfoInCurrentInventory) {
				continue;
			}
			
			// In special inventory, only process material names on the first line
			if (isSpecialInventory && i > 0) {
				continue;
			}

			// Legend+-UI (cactusclicker_legend_plus / ui_background): keine Material-Ebenen
			if (ZeichenUtility.containsLegendPlusUiBackground(screenTitle)) {
				continue;
			}
			
			// Sort materials by length (longest first) to avoid shorter names matching before longer ones
			List<Map.Entry<String, MaterialInfo>> sortedMaterials = new ArrayList<>(materialsDatabase.entrySet());
			sortedMaterials.sort((a, b) -> Integer.compare(b.getKey().length(), a.getKey().length()));

			boolean materialLocationAdded = false;
			
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
					
					// Material-Bag: Ebene nur hinter Materiallisten, nicht im Item-Namen
					if (isMaterialBagInventory && i == 0) {
						break;
					}
					
					// Create new text with material info appended to the same line

					MaterialFloorInfo locationInfo = new MaterialFloorInfo(
						info.floor, info.pond, null, info.rarity, info.color);
					String locationSuffix = formatMaterialLocationSuffix(locationInfo);
					int color = getMaterialLocationRarityColor(locationInfo);

					Component materialInfo;
					if (!locationSuffix.isEmpty()) {
						materialInfo = Component.literal(locationSuffix)
							.withStyle(style -> style.withColor(color));
					} else if (isSpecialInventory) {
						materialInfo = Component.literal(" [" + info.floor + "]")
							.withStyle(style -> style.withColor(color));
					} else {
						materialInfo = Component.literal(" [Ebene " + info.floor + "]")
							.withStyle(style -> style.withColor(color));
					}
					
					// Combine original line with material info
					Component combinedText = line.copy().append(materialInfo);
					
					// Replace the original line
					lines.set(i, combinedText);
					materialLocationAdded = true;
					break; // Only add info for the first match in this line
				}
			}

			if (!materialLocationAdded) {
				appendFishingMaterialLocationToLine(
					lines, i, line, cleanLineText, isSpecialInventory, isMaterialBagInventory);
			}
		}
		
		// Check for license information in inventories with ち
		if (isLicenseInventory && CCLiveUtilitiesConfig.HANDLER.instance().showLicenseInformation) {
			// Check if the hovered item is in one of the license slots
			if (isItemInLicenseSlot(stack, client)) {
				checkForLicenseInformation(lines, client);
			}
		}

		// Check for gadget information in "Module [Upgraden]" inventory
		if (CCLiveUtilitiesConfig.HANDLER.instance().showModuleInformation) {
			boolean isModuleUpgradeInventory = false;
			boolean isEngineerInventory = false;
			if (client.gui.screen() != null) {
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
		}
	}
}
