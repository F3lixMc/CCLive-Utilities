package net.felix.features.information.tooltip;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import java.util.List;

/**
 * Pin-Eingabe im Tooltip fuer Schmiede-Inventare.
 */
public final class ClipboardPinTooltip {

	private ClipboardPinTooltip() {
	}

	/**
	 * Adds floor number to blueprint names in inventory tooltips
	 * Format: "BAUPLAN NAME - [Bauplan] [eX]" where X is the floor number (at the end)
	 */
	/**
	 * Fügt "Bauplan Anpinnen mit Hotkey: [Hotkey]" Text zu Tooltips in Bauplan-Inventaren hinzu
	 * Wird über "[Linksklick]: Herstellen" oder "[Linksklick] Kaufen" eingefügt
	 * Verwendet den gleichen Text wie im ItemViewer-Tooltip
	 */
	static void addClipboardPinTextToTooltip(List<Component> lines, Minecraft client) {
		// Performance-Optimierung: Frühe Returns
		if (lines == null || lines.isEmpty() || client == null) {
			return;
		}
		
		if (client.gui.screen() == null) {
			return;
		}
		
		// Prüfe ob wir in einem Bauplan-Inventar sind (Baupläne [Waffen], Bauplan [Shop], etc.)
		String screenTitle = client.gui.screen().getTitle().getString();
		String cleanScreenTitle = screenTitle.replaceAll("§[0-9a-fk-or]", "")
											.replaceAll("[\\u3400-\\u4DBF]", "");
		
		// Prüfe auf Bauplan-Inventare (Baupläne [Waffen], Baupläne [Rüstung], Bauplan [Shop], etc.)
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
		
		// Hole den Hotkey-Text vom ItemViewer (gleicher Text wie im ItemViewer-Tooltip)
        String hotkeyText = net.felix.utilities.ItemViewer.ItemViewerUtility.getClipboardPinHotkeyText();
        String clipboardText = "An Pinnwand anheften mit Hotkey: " + hotkeyText;
		
		// Durchsuche Tooltip-Zeilen nach "[Linksklick]: Herstellen" oder "[Linksklick] Kaufen"
		for (int i = 0; i < lines.size(); i++) {
			Component line = lines.get(i);
			String lineText = line.getString();
			
			if (lineText == null) {
				continue;
			}
			
			// Entferne Formatierungs-Codes für Vergleich
			String cleanLineText = lineText.replaceAll("§[0-9a-fk-or]", "");
			
			// Prüfe ob die Zeile "[Linksklick]: Herstellen" oder "[Linksklick] Kaufen" enthält
			boolean hasHerstellen = cleanLineText.contains("[Linksklick]: Herstellen");
			boolean hasKaufen = cleanLineText.contains("[Linksklick] Kaufen");
			
			if (hasHerstellen || hasKaufen) {
				// Füge leere Zeile und "Bauplan Anpinnen mit Hotkey: [Hotkey]" in grau hinzu, direkt vor dieser Zeile
				lines.add(i, Component.empty()); // Leere Zeile
				lines.add(i + 1, Component.literal(clipboardText)
					.setStyle(Style.EMPTY.withColor(0xFF808080).withItalic(false))); // Grau
				return; // Nur einmal einfügen
			}
		}
	}
}
