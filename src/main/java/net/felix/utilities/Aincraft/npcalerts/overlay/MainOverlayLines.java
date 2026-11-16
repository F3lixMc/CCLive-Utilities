package net.felix.utilities.Aincraft.npcalerts.overlay;

import java.util.ArrayList;
import java.util.List;
import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.Aincraft.npcalerts.data.CapacityData;
import net.felix.utilities.Aincraft.npcalerts.data.MachtkristallSlot;

import static net.felix.utilities.Aincraft.npcalerts.data.NpcAlertState.ambossCapacity;
import static net.felix.utilities.Aincraft.npcalerts.data.NpcAlertState.essenzenCapacity;
import static net.felix.utilities.Aincraft.npcalerts.data.NpcAlertState.forschung;
import static net.felix.utilities.Aincraft.npcalerts.data.NpcAlertState.jaegerCapacity;
import static net.felix.utilities.Aincraft.npcalerts.data.NpcAlertState.komboKiste;
import static net.felix.utilities.Aincraft.npcalerts.data.NpcAlertState.machtkristallSlots;
import static net.felix.utilities.Aincraft.npcalerts.data.NpcAlertState.recyclerSlot1;
import static net.felix.utilities.Aincraft.npcalerts.data.NpcAlertState.recyclerSlot2;
import static net.felix.utilities.Aincraft.npcalerts.data.NpcAlertState.recyclerSlot3;
import static net.felix.utilities.Aincraft.npcalerts.data.NpcAlertState.schmelzofenCapacity;
import static net.felix.utilities.Aincraft.npcalerts.data.NpcAlertState.seelenCapacity;
import static net.felix.utilities.Aincraft.npcalerts.message.ScreenMessages.getMachtkristallShowWarning;
import static net.felix.utilities.Aincraft.npcalerts.message.ScreenMessages.shouldShowForschungWarning;
import static net.felix.utilities.Aincraft.npcalerts.overlay.PercentFormatter.formatPercent;
import static net.felix.utilities.Aincraft.npcalerts.parser.KomboKisteReader.getKomboKisteFractionDisplay;

/**
 * Berechnet die Textzeilen des Haupt-Overlays (auch fuer den Bearbeitungsmodus).
 */
public final class MainOverlayLines {

	private static final String EDIT_MODE_VALUE = "? / ?";
	private static final String EDIT_MODE_PERCENT = "0.0%";

	private MainOverlayLines() {
	}

	private static CCLiveUtilitiesConfig config() {
		return CCLiveUtilitiesConfig.HANDLER.instance();
	}

	/** Eine Information erscheint im Haupt-Overlay, wenn sie aktiviert und nicht als separates Overlay eingestellt ist. */
	private static boolean isInMainOverlay(boolean enabled, boolean separateOverlay) {
		return enabled && !separateOverlay;
	}

	/**
	 * Gibt die Anzahl der Zeilen im Haupt-Overlay zurück (für DraggableOverlay)
	 */
	public static int getMainOverlayLineCount() {
		CCLiveUtilitiesConfig config = config();
		int count = 0;
		count += isInMainOverlay(config.showNpcAlertsForschung, config.npcAlertsForschungSeparateOverlay) ? 1 : 0;
		count += isInMainOverlay(config.showNpcAlertsAmboss, config.npcAlertsAmbossSeparateOverlay) ? 1 : 0;
		count += isInMainOverlay(config.showNpcAlertsSchmelzofen, config.npcAlertsSchmelzofenSeparateOverlay) ? 1 : 0;
		count += isInMainOverlay(config.showNpcAlertsJaeger, config.npcAlertsJaegerSeparateOverlay) ? 1 : 0;
		count += isInMainOverlay(config.showNpcAlertsSeelen, config.npcAlertsSeelenSeparateOverlay) ? 1 : 0;
		count += isInMainOverlay(config.showNpcAlertsEssenzen, config.npcAlertsEssenzenSeparateOverlay) ? 1 : 0;
		count += isInMainOverlay(config.showNpcAlertsKomboKiste, config.npcAlertsKomboKisteSeparateOverlay) ? 1 : 0;
		// Machtkristalle: immer 3 Slots (auch wenn leer)
		count += isInMainOverlay(config.showNpcAlertsMachtkristalle, config.npcAlertsMachtkristalleSeparateOverlay) ? 3 : 0;
		count += isInMainOverlay(config.showNpcAlertsRecyclerSlot1, config.npcAlertsRecyclerSlot1SeparateOverlay) ? 1 : 0;
		count += isInMainOverlay(config.showNpcAlertsRecyclerSlot2, config.npcAlertsRecyclerSlot2SeparateOverlay) ? 1 : 0;
		count += isInMainOverlay(config.showNpcAlertsRecyclerSlot3, config.npcAlertsRecyclerSlot3SeparateOverlay) ? 1 : 0;
		return Math.max(1, count); // Mindestens 1 Zeile
	}

	// ------------------------------------------------------------------ Bearbeitungsmodus

	/**
	 * Gibt die Zeilen für das Haupt-Overlay zurück (für F6-Editor mit "?/?" als Werte)
	 */
	public static List<LineWithPercent> getMainOverlayLinesForEditMode() {
		CCLiveUtilitiesConfig config = config();
		List<LineWithPercent> lines = new ArrayList<>();

		if (isInMainOverlay(config.showNpcAlertsForschung, config.npcAlertsForschungSeparateOverlay)) {
			addEditModeLine(lines, "forschung", "Forschung", config.showNpcAlertsForschungPercent, config.npcAlertsForschungShowIcon);
		}
		if (isInMainOverlay(config.showNpcAlertsAmboss, config.npcAlertsAmbossSeparateOverlay)) {
			addEditModeLine(lines, "amboss", "Amboss", config.showNpcAlertsAmbossPercent, config.npcAlertsAmbossShowIcon);
		}
		if (isInMainOverlay(config.showNpcAlertsSchmelzofen, config.npcAlertsSchmelzofenSeparateOverlay)) {
			addEditModeLine(lines, "schmelzofen", "Schmelzofen", config.showNpcAlertsSchmelzofenPercent, config.npcAlertsSchmelzofenShowIcon);
		}
		if (isInMainOverlay(config.showNpcAlertsJaeger, config.npcAlertsJaegerSeparateOverlay)) {
			addEditModeLine(lines, "jaeger", "Jäger", config.showNpcAlertsJaegerPercent, config.npcAlertsJaegerShowIcon);
		}
		if (isInMainOverlay(config.showNpcAlertsSeelen, config.npcAlertsSeelenSeparateOverlay)) {
			addEditModeLine(lines, "seelen", "Seelen", config.showNpcAlertsSeelenPercent, config.npcAlertsSeelenShowIcon);
		}
		if (isInMainOverlay(config.showNpcAlertsEssenzen, config.npcAlertsEssenzenSeparateOverlay)) {
			addEditModeLine(lines, "essenzen", "Essenzen", config.showNpcAlertsEssenzenPercent, config.npcAlertsEssenzenShowIcon);
		}
		if (isInMainOverlay(config.showNpcAlertsKomboKiste, config.npcAlertsKomboKisteSeparateOverlay)) {
			addEditModeLine(lines, "komboKiste", "Kombo Kiste", false, config.npcAlertsKomboKisteShowIcon);
		}
		if (isInMainOverlay(config.showNpcAlertsMachtkristalle, config.npcAlertsMachtkristalleSeparateOverlay)) {
			addMachtkristallEditModeLines(lines, config.npcAlertsMachtkristalleShowIcon);
		}
		addRecyclerEditModeLines(lines, config);
		return lines;
	}

	/** Zeile mit "? / ?" und (falls aktiviert) dem Beispiel-Prozentwert "0.0%". */
	private static void addEditModeLine(List<LineWithPercent> lines, String key, String label,
										boolean showPercent, boolean showIcon) {
		String displayText = showIcon ? EDIT_MODE_VALUE : label + ": " + EDIT_MODE_VALUE;
		lines.add(new LineWithPercent(displayText, showPercent ? EDIT_MODE_PERCENT : null, showPercent, false, key, showIcon));
	}

	/** Zeigt alle aktivierten Machtkristall-Slots mit Beispielwerten (Prozent immer, notfalls "0.0%"). */
	private static void addMachtkristallEditModeLines(List<LineWithPercent> lines, boolean showIcon) {
		for (int i = 0; i < 3; i++) {
			if (!AlertSettings.isNpcAlertMachtkristallSlotEnabled(i)) {
				continue;
			}
			String displayText = showIcon ? EDIT_MODE_VALUE : "MK " + (i + 1) + ": " + EDIT_MODE_VALUE;
			String percentText = machtkristallSlots[i].getPercentText();
			if (percentText == null) {
				percentText = EDIT_MODE_PERCENT;
			}
			lines.add(new LineWithPercent(displayText, percentText, true, false, "machtkristalle", showIcon));
		}
	}

	private static void addRecyclerEditModeLines(List<LineWithPercent> lines, CCLiveUtilitiesConfig config) {
		boolean showPercent = config.showNpcAlertsRecyclerPercent;
		if (isInMainOverlay(config.showNpcAlertsRecyclerSlot1, config.npcAlertsRecyclerSlot1SeparateOverlay)) {
			addRecyclerEditModeLine(lines, recyclerSlot1, 1, showPercent, config.npcAlertsRecyclerSlot1ShowIcon);
		}
		if (isInMainOverlay(config.showNpcAlertsRecyclerSlot2, config.npcAlertsRecyclerSlot2SeparateOverlay)) {
			addRecyclerEditModeLine(lines, recyclerSlot2, 2, showPercent, config.npcAlertsRecyclerSlot2ShowIcon);
		}
		if (isInMainOverlay(config.showNpcAlertsRecyclerSlot3, config.npcAlertsRecyclerSlot3SeparateOverlay)) {
			addRecyclerEditModeLine(lines, recyclerSlot3, 3, showPercent, config.npcAlertsRecyclerSlot3ShowIcon);
		}
	}

	private static void addRecyclerEditModeLine(List<LineWithPercent> lines, CapacityData slot, int slotNumber,
												boolean showPercent, boolean showIcon) {
		String displayText = showIcon ? EDIT_MODE_VALUE : "Recycler Slot " + slotNumber + ": " + EDIT_MODE_VALUE;
		String percent = showPercent && slot.isValid() ? formatPercent(slot.current, slot.max) : null;
		lines.add(new LineWithPercent(displayText, percent, showPercent && percent != null, false,
			"recyclerSlot" + slotNumber, showIcon));
	}

	// ------------------------------------------------------------------ Echte Werte

	/**
	 * Gibt die tatsächlichen Zeilen für das Haupt-Overlay zurück (mit echten Werten)
	 * Wird für die Breitenberechnung im F6-Editor verwendet
	 */
	public static List<LineWithPercent> getMainOverlayLines() {
		CCLiveUtilitiesConfig config = config();
		List<LineWithPercent> lines = new ArrayList<>();

		if (isInMainOverlay(config.showNpcAlertsForschung, config.npcAlertsForschungSeparateOverlay)) {
			lines.add(forschungLine(config));
		}
		if (isInMainOverlay(config.showNpcAlertsAmboss, config.npcAlertsAmbossSeparateOverlay)) {
			lines.add(capacityLine(ambossCapacity, "amboss", "Amboss", ambossCapacity.getDisplayString(),
				config.showNpcAlertsAmbossPercent, config.npcAlertsAmbossWarnPercent, config.npcAlertsAmbossShowIcon));
		}
		if (isInMainOverlay(config.showNpcAlertsSchmelzofen, config.npcAlertsSchmelzofenSeparateOverlay)) {
			lines.add(capacityLine(schmelzofenCapacity, "schmelzofen", "Schmelzofen", schmelzofenCapacity.getDisplayString(),
				config.showNpcAlertsSchmelzofenPercent, config.npcAlertsSchmelzofenWarnPercent, config.npcAlertsSchmelzofenShowIcon));
		}
		if (isInMainOverlay(config.showNpcAlertsJaeger, config.npcAlertsJaegerSeparateOverlay)) {
			lines.add(capacityLine(jaegerCapacity, "jaeger", "Jäger", jaegerCapacity.getDisplayStringWithoutCurrentSuffix(),
				config.showNpcAlertsJaegerPercent, config.npcAlertsJaegerWarnPercent, config.npcAlertsJaegerShowIcon));
		}
		if (isInMainOverlay(config.showNpcAlertsSeelen, config.npcAlertsSeelenSeparateOverlay)) {
			lines.add(capacityLine(seelenCapacity, "seelen", "Seelen", seelenCapacity.getDisplayString(),
				config.showNpcAlertsSeelenPercent, config.npcAlertsSeelenWarnPercent, config.npcAlertsSeelenShowIcon));
		}
		if (isInMainOverlay(config.showNpcAlertsEssenzen, config.npcAlertsEssenzenSeparateOverlay)) {
			lines.add(capacityLine(essenzenCapacity, "essenzen", "Essenzen", essenzenCapacity.getDisplayString(),
				config.showNpcAlertsEssenzenPercent, config.npcAlertsEssenzenWarnPercent, config.npcAlertsEssenzenShowIcon));
		}
		if (isInMainOverlay(config.showNpcAlertsKomboKiste, config.npcAlertsKomboKisteSeparateOverlay)) {
			lines.add(komboKisteLine(config.npcAlertsKomboKisteShowIcon));
		}
		if (isInMainOverlay(config.showNpcAlertsMachtkristalle, config.npcAlertsMachtkristalleSeparateOverlay)) {
			addMachtkristallLines(lines, config);
		}
		addRecyclerLines(lines, config);
		return lines;
	}

	/** Forschung zählt runter: Prozent = current / max; Warnung bei verbleibendem Wert <= Schwelle (0-23). */
	private static LineWithPercent forschungLine(CCLiveUtilitiesConfig config) {
		String percent = null;
		boolean showPercent = config.showNpcAlertsForschungPercent;
		if (showPercent) {
			if (forschung.isValid()) {
				percent = formatPercent(forschung.current, forschung.max);
			} else {
				String displayString = forschung.getDisplayString();
				if (displayString != null && displayString.contains("Nicht im Tab-Widget")) {
					// Keine Prozentanzeige wenn "Nicht im Tab-Widget"
					showPercent = false;
				} else {
					// Zeige "?%" wenn Daten noch nicht verfügbar sind
					percent = "?%";
				}
			}
		}
		boolean showWarning = shouldShowForschungWarning(config.npcAlertsForschungWarnValue);
		boolean showIcon = config.npcAlertsForschungShowIcon;
		String displayText = showIcon ? forschung.getDisplayString() : "Forschung: " + forschung.getDisplayString();
		return new LineWithPercent(displayText, percent, showPercent, showWarning, "forschung", showIcon);
	}

	/** Kapazitäts-Zeile (Amboss, Schmelzofen, Jäger, Seelen, Essenzen): Warnung, wenn die Füllung >= Schwelle ist. */
	private static LineWithPercent capacityLine(CapacityData data, String key, String label, String displayString,
												boolean showPercent, double warnPercent, boolean showIcon) {
		String percent = showPercent && data.isValid() ? formatPercent(data.current, data.max) : null;
		boolean showWarning = data.isValid() && warnPercent >= 0 && fillPercent(data) >= warnPercent;
		String displayText = showIcon ? displayString : label + ": " + displayString;
		return new LineWithPercent(displayText, percent, showPercent, showWarning, key, showIcon);
	}

	/** Kombo Kiste: Warnung, wenn die Kiste voll ist. */
	private static LineWithPercent komboKisteLine(boolean showIcon) {
		boolean showWarning = komboKiste.isValid() && komboKiste.current >= komboKiste.max;
		String fraction = getKomboKisteFractionDisplay();
		String displayText = showIcon ? fraction : "Kombo Kiste: " + fraction;
		return new LineWithPercent(displayText, null, false, showWarning, "komboKiste", showIcon);
	}

	private static void addMachtkristallLines(List<LineWithPercent> lines, CCLiveUtilitiesConfig config) {
		boolean showIcon = config.npcAlertsMachtkristalleShowIcon;
		for (int i = 0; i < 3; i++) {
			if (!AlertSettings.isNpcAlertMachtkristallSlotEnabled(i)) {
				continue;
			}
			MachtkristallSlot slot = machtkristallSlots[i];
			String displayText = SeparateOverlayContent.machtkristallDisplayText(slot, i, showIcon);
			// Slot nicht in der Tab-Liste gefunden oder leer: nur Text, weder Prozent noch Warnung
			if (slot.isNotFound() || slot.isEmpty()) {
				lines.add(new LineWithPercent(displayText, null, false, false, "machtkristalle", showIcon));
				continue;
			}
			String percentText = slot.getPercentText();
			boolean showWarning = getMachtkristallShowWarning(slot, config.npcAlertsMachtkristalleWarnPercent);
			lines.add(new LineWithPercent(displayText, percentText, percentText != null, showWarning, "machtkristalle", showIcon));
		}
	}

	private static void addRecyclerLines(List<LineWithPercent> lines, CCLiveUtilitiesConfig config) {
		boolean showPercent = config.showNpcAlertsRecyclerPercent;
		double warnPercent = config.npcAlertsRecyclerWarnPercent;
		if (isInMainOverlay(config.showNpcAlertsRecyclerSlot1, config.npcAlertsRecyclerSlot1SeparateOverlay)) {
			lines.add(recyclerLine(recyclerSlot1, 1, showPercent, warnPercent, config.npcAlertsRecyclerSlot1ShowIcon));
		}
		if (isInMainOverlay(config.showNpcAlertsRecyclerSlot2, config.npcAlertsRecyclerSlot2SeparateOverlay)) {
			lines.add(recyclerLine(recyclerSlot2, 2, showPercent, warnPercent, config.npcAlertsRecyclerSlot2ShowIcon));
		}
		if (isInMainOverlay(config.showNpcAlertsRecyclerSlot3, config.npcAlertsRecyclerSlot3SeparateOverlay)) {
			lines.add(recyclerLine(recyclerSlot3, 3, showPercent, warnPercent, config.npcAlertsRecyclerSlot3ShowIcon));
		}
	}

	/** Recycler zählt runter, daher warnen wenn die Füllung <= Schwelle ist. */
	private static LineWithPercent recyclerLine(CapacityData slot, int slotNumber, boolean showPercent,
												double warnPercent, boolean showIcon) {
		String percent = showPercent && slot.isValid() ? formatPercent(slot.current, slot.max) : null;
		boolean showWarning = slot.isValid() && warnPercent >= 0 && fillPercent(slot) <= warnPercent;
		String displayText = showIcon ? slot.getDisplayString() : "Recycler Slot " + slotNumber + ": " + slot.getDisplayString();
		return new LineWithPercent(displayText, percent, showPercent && percent != null, showWarning,
			"recyclerSlot" + slotNumber, showIcon);
	}

	private static double fillPercent(CapacityData data) {
		return data.isValid() ? ((double) data.current / (double) data.max) * 100.0 : 0;
	}
}