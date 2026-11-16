package net.felix.features.information.tooltip;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import net.felix.CCLiveUtilitiesConfig;
import net.felix.features.information.materials.MaterialFloorInfo;
import net.felix.features.information.materials.MaterialInfo;
import net.felix.utilities.Overall.glyphs.ZeichenUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

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
import static net.felix.features.information.materials.MaterialRarityColors.formatMaterialLocationSuffix;
import static net.felix.features.information.materials.MaterialRarityColors.getMaterialLocationRarityColor;
import static net.felix.features.information.moblexicon.MoblexiconTooltip.addMoblexiconHpToTooltip;
import static net.felix.features.information.ponds.FishingMaterials.appendFishingMaterialLocationToLine;
import static net.felix.features.information.ponds.PondTooltip.checkForPondInformation;
import static net.felix.features.information.tooltip.ClipboardPinTooltip.addClipboardPinTextToTooltip;

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

		// Aspekt-, Ebenen- und Pin-Informationen immer (auch wenn das Informationen-Utility aus ist)
		addAspectNameToTooltip(lines, client, stack);
		addFloorNumberToBlueprintNames(lines, client);
		addFloorNumberToCardsStatuesNames(lines, client);
		// Clipboard-Pin-Texteingabe fuer Forge-Inventare (vor der uebrigen Verarbeitung)
		addClipboardPinTextToTooltip(lines, client);

		// Alles Weitere nur, wenn das Informationen-Utility aktiviert ist
		CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
		if (!CCLiveUtilitiesConfig.isModRuntimeEnabled() || !config.informationenUtilityEnabled) {
			return;
		}

		TooltipInventoryContext inventory = TooltipInventoryContext.of(client);

		if (config.showWaveDisplay && inventory.hasScreen() && ZeichenUtility.containsEssenceBagUi(inventory.title())) {
			addEssenceBagWaveDisplayToTooltip(lines, stack);
		}
		if (inventory.hideFloorNumbers()) {
			return;
		}

		// Teich-Zuordnung in Schnellreise-Inventaren (unabhängig von Ebenen-Einstellung)
		if (inventory.quickTravel() && config.showLicenseInformation) {
			checkForPondInformation(lines);
		}
		if (inventory.special() && config.moblexiconHpEnabled) {
			addMoblexiconHpToTooltip(lines);
		}

		// Ist die Einstellung fuer diesen Inventar-Typ aktiv?
		boolean showFloorInfoInCurrentInventory = inventory.special()
				? config.showEbenenInSpecialInventory
				: config.showEbenenInNormalInventories;
		if (inventory.special() && !showFloorInfoInCurrentInventory && !config.moblexiconCardsStatuesInfoEnabled) {
			return;
		}
		if (!inventory.special() && !showFloorInfoInCurrentInventory) {
			return;
		}

		addLineInformation(lines, inventory, showFloorInfoInCurrentInventory);

		// Lizenz-Informationen im Lizenz-Inventar (ち), wenn das Item in einem Lizenz-Slot liegt
		if (inventory.license() && config.showLicenseInformation && isItemInLicenseSlot(stack, client)) {
			checkForLicenseInformation(lines, client);
		}
		addGadgetInformation(lines, inventory.title(), config);
	}

	/** Gadget-Informationen im Inventar "Module [Upgraden]" bzw. beim Ingenieur. */
	private static void addGadgetInformation(List<Component> lines, String screenTitle, CCLiveUtilitiesConfig config) {
		if (!config.showModuleInformation) {
			return;
		}
		String cleanScreenTitle = screenTitle.replaceAll("§[0-9a-fk-or]", "")
			.replaceAll("[\\u3400-\\u4DBF]", "");
		if (cleanScreenTitle.contains("Module [Upgraden]")) {
			checkForGadgetInformation(lines, false);
		}
		if (cleanScreenTitle.contains("[Ingenieur]")) {
			checkForGadgetInformation(lines, true);
		}
	}

	/** Titel von Inventaren, in denen keine Level-/Ebenen-Informationen ergaenzt werden. */
	private static boolean skipsLevelInformation(String screenTitle) {
		return screenTitle.contains("Essenz [Auswahl]") || screenTitle.contains("Essenz-Tasche")
			|| screenTitle.contains("Essenzernter") || ZeichenUtility.containsLegendPlusUiBackground(screenTitle)
			|| screenTitle.contains("Machtkristalle Verbessern")
			|| screenTitle.contains("Fabrik Skins") || screenTitle.contains("Fabrik Skins Menü")
			|| screenTitle.contains("Factory Bridge Skins")
			|| screenTitle.contains("Factory Tower Skins")
			|| screenTitle.contains("Factory Field Skins")
			|| screenTitle.contains("Factory Banner Skins") || screenTitle.contains("Factory Banner Slots")
			|| ZeichenUtility.containsHunterUiBackground(screenTitle);
	}

	/**
	 * Geht alle Zeilen durch und ergaenzt Essenz-Wellen, Karten-/Statuen-Effekte sowie Material- und Angel-Fundorte.
	 * Das Ende der Schleife steht vorab fest; eingefuegte Zeilen werden uebersprungen.
	 */
	private static void addLineInformation(List<Component> lines, TooltipInventoryContext inventory,
										   boolean showFloorInfoInCurrentInventory) {
		String screenTitle = inventory.title();
		boolean isEssenceImprovementInventory = screenTitle.contains("Machtkristalle Verbessern");
		// Die Legend+-UI (ui_background) ist hier bereits enthalten: keine Material-Ebenen
		boolean skipLevelInformation = skipsLevelInformation(screenTitle);
		int endIndex = lines.size();

		for (int i = 0; i < endIndex; i++) {
			Component line = lines.get(i);
			String lineText = line.getString();
			if (lineText == null || lineText.isEmpty()) {
				continue;
			}

			// Minecraft-Formatierungscodes (§a, §b, ...) und die Unicode-Zeichen 㔻㔶 entfernen
			String cleanLineText = lineText.replaceAll("§[0-9a-fk-or]", "")
				.replaceAll("[\\u3400-\\u4DBF]", "");

			// Essenz-Informationen im Inventar "Machtkristalle Verbessern" zuerst
			if (isEssenceImprovementInventory && lineText.contains("[Essenz]")
					&& addEssenceWaveLine(lines, i, cleanLineText)) {
				i++; // die eben eingefuegte Zeile ueberspringen
				continue;
			}
			if (skipLevelInformation) {
				continue;
			}

			// Nur der Teil vor der ersten '[' ist der Materialname
			String materialNamePart = cleanLineText;
			if (cleanLineText.contains("[")) {
				materialNamePart = cleanLineText.substring(0, cleanLineText.indexOf("[")).trim();
			}
			if (materialNamePart.isEmpty()) {
				continue;
			}

			boolean isCardOrStatue = lineText.contains("[Karte]") || lineText.contains("[Statue]");
			// Moblexikon: Effekt-Informationen fuer [Karte] und [Statue]
			if (isCardOrStatue && inventory.special()
					&& CCLiveUtilitiesConfig.HANDLER.instance().moblexiconCardsStatuesInfoEnabled) {
				appendCardOrStatueEffect(lines, i, line, lineText, cleanLineText);
				continue;
			}
			// In anderen Inventaren werden [Karte]/[Statue]-Zeilen uebersprungen
			if (isCardOrStatue) {
				continue;
			}
			if (!showFloorInfoInCurrentInventory) {
				continue;
			}
			// Im Spezial-Inventar nur die Materialnamen der ersten Zeile
			if (inventory.special() && i > 0) {
				continue;
			}

			boolean materialLocationAdded = appendMaterialLocation(lines, i, line, lineText, materialNamePart, inventory);
			if (!materialLocationAdded) {
				appendFishingMaterialLocationToLine(
					lines, i, line, cleanLineText, inventory.special(), inventory.materialBag());
			}
		}
	}

	/**
	 * Fuegt nach der Zeile " -> Welle: ..." ein.
	 * @return true, wenn eine Zeile eingefuegt wurde (nur wenn die Wellen-Anzeige aktiv ist)
	 */
	private static boolean addEssenceWaveLine(List<Component> lines, int index, String cleanLineText) {
		if (!CCLiveUtilitiesConfig.HANDLER.instance().showWaveDisplay) {
			return false;
		}
		// Text vor dem Doppelpunkt, ohne Mengenangabe
		String textBeforeColon = cleanLineText;
		if (cleanLineText.contains(":")) {
			textBeforeColon = cleanLineText.substring(0, cleanLineText.indexOf(":")).trim();
		}
		// Fuehrenden Bindestrich entfernen
		if (textBeforeColon.startsWith("-")) {
			textBeforeColon = textBeforeColon.substring(1).trim();
		}
		// textBeforeColon enthaelt bereits den vollen Essenznamen inkl. [Essenz] und Tier
		String waveText = getWaveDisplayForEssenceName(textBeforeColon);
		Component essenceInfoText = Component.literal(" -> Welle: ")
			.withStyle(style -> style.withColor(0xC0C0C0)) // Light gray
			.append(Component.literal(waveText)
				.withStyle(style -> style.withColor(0x55FF55))); // Light green
		lines.add(index + 1, essenceInfoText);
		return true;
	}

	/** Haengt den Effekt einer Karte (gruen) oder Statue (aqua) hinter die Zeile, falls bekannt. */
	private static void appendCardOrStatueEffect(List<Component> lines, int index, Component line,
												 String lineText, String cleanLineText) {
		boolean isCard = lineText.contains("[Karte]");
		int markerIndex = cleanLineText.indexOf(isCard ? "[Karte]" : "[Statue]");
		if (markerIndex <= 0) {
			return;
		}
		// Name vor dem Marker, ohne fuehrende Bindestriche oder Leerraum
		String name = cleanLineText.substring(0, markerIndex).trim().replaceAll("^[-\\s]+", "").trim();
		String effect = findEffect(isCard ? cardsEffects : statuesEffects, name);
		if (effect == null || effect.isEmpty()) {
			return;
		}
		int effectColor = isCard ? 0x55FF55 : 0x55FFFF; // Green for cards, Aqua for statues
		// Originalzeile (mit Formatierung) + " - " + Effekt
		MutableComponent newLine = line.copy()
			.append(Component.literal(" - ").withStyle(style -> style.withColor(0xC0C0C0))) // Light gray separator
			.append(Component.literal(effect).withStyle(style -> style.withColor(effectColor)));
		lines.set(index, newLine);
	}

	/** Exakter Treffer, sonst Treffer ohne Beachtung der Gross-/Kleinschreibung. */
	private static String findEffect(Map<String, String> effects, String name) {
		String effect = effects.get(name);
		if (effect == null) {
			for (Map.Entry<String, String> entry : effects.entrySet()) {
				if (entry.getKey().equalsIgnoreCase(name)) {
					return entry.getValue();
				}
			}
		}
		return effect;
	}

	/**
	 * Haengt Ebene/Teich des ersten passenden Materials (laengste Namen zuerst) an die Zeile an.
	 * @return true, wenn die Zeile ergaenzt wurde
	 */
	private static boolean appendMaterialLocation(List<Component> lines, int index, Component line, String lineText,
												  String materialNamePart, TooltipInventoryContext inventory) {
		// Nach Laenge sortieren (laengste zuerst), damit kuerzere Namen nicht vor laengeren matchen
		List<Map.Entry<String, MaterialInfo>> sortedMaterials = new ArrayList<>(materialsDatabase.entrySet());
		sortedMaterials.sort((a, b) -> Integer.compare(b.getKey().length(), a.getKey().length()));

		for (Map.Entry<String, MaterialInfo> entry : sortedMaterials) {
			String materialName = entry.getKey();
			MaterialInfo info = entry.getValue();

			// Wortgrenzen verhindern Teiltreffer ("Schweinehuf" vs. "Kräftiger Schweinehuf"), auch bei "ß"
			String pattern = "\\b" + Pattern.quote(materialName) + "\\b";
			if (!Pattern.compile(pattern, Pattern.UNICODE_CHARACTER_CLASS).matcher(materialNamePart).find()) {
				continue;
			}
			// Bauplan-Zeilen uebernimmt der Blueprint-Viewer
			if (lineText.contains("[Bauplan]")) {
				continue;
			}
			// Im Spezial-Inventar Ebenen nur fuer Mob-Namen, nicht fuer Materialien
			if (inventory.special() && !info.rarity.equals("mob")) {
				continue;
			}
			// Material-Tasche: Ebene nur hinter Materiallisten, nicht im Item-Namen
			if (inventory.materialBag() && index == 0) {
				return false;
			}

			lines.set(index, line.copy().append(createMaterialInfoText(info, inventory.special())));
			return true; // nur der erste Treffer pro Zeile
		}
		return false;
	}

	private static Component createMaterialInfoText(MaterialInfo info, boolean isSpecialInventory) {
		MaterialFloorInfo locationInfo = new MaterialFloorInfo(info.floor, info.pond, null, info.rarity, info.color);
		String locationSuffix = formatMaterialLocationSuffix(locationInfo);
		int color = getMaterialLocationRarityColor(locationInfo);

		String text;
		if (!locationSuffix.isEmpty()) {
			text = locationSuffix;
		} else if (isSpecialInventory) {
			text = " [" + info.floor + "]";
		} else {
			text = " [Ebene " + info.floor + "]";
		}
		return Component.literal(text).withStyle(style -> style.withColor(color));
	}
}