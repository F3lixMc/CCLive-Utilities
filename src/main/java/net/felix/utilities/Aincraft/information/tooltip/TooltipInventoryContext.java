package net.felix.utilities.Aincraft.information.tooltip;

import net.felix.utilities.Other.glyphs.ZeichenUtility;
import net.minecraft.client.Minecraft;

/**
 * Art des geoeffneten Inventars, abgeleitet aus dem Screen-Titel (fuer die Tooltip-Ergaenzungen).
 *
 * @param hasScreen            ob ueberhaupt ein Screen offen ist
 * @param title                Titel des Screens (leer ohne Screen)
 * @param special              Moblexikon-Inventar
 * @param license              Lizenz-Inventar
 * @param quickTravel          Farmzonen-Schnellreise
 * @param materialBag          Material-/Ressourcen-Tasche oder Mob-Materialien
 * @param hideFloorNumbers     Essence-Bag, -Auswahl, Harvester: keine Ebenen-Nummern
 */
record TooltipInventoryContext(boolean hasScreen, String title, boolean special, boolean license,
                               boolean quickTravel, boolean materialBag, boolean hideFloorNumbers) {

	static TooltipInventoryContext of(Minecraft client) {
		if (client.gui.screen() == null) {
			return new TooltipInventoryContext(false, "", false, false, false, false, false);
		}
		String title = client.gui.screen().getTitle().getString();
		return new TooltipInventoryContext(
			true,
			title,
			ZeichenUtility.containsMoblexicon(title),
			ZeichenUtility.containsFriendsRequestAcceptDeny(title),
			ZeichenUtility.containsFarmzoneQuickTravel(title),
			ZeichenUtility.containsUiMaterialBag(title)
				|| ZeichenUtility.containsUiRessourceBag(title)
				|| title.contains("Mob-Materialien"),
			ZeichenUtility.shouldHideFloorNumbersInEssenceMenus(title));
	}
}