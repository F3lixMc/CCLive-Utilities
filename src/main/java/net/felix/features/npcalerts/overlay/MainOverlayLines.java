package net.felix.features.npcalerts.overlay;

import java.util.ArrayList;
import java.util.List;
import net.felix.features.npcalerts.data.MachtkristallSlot;

import static net.felix.features.npcalerts.data.NpcAlertState.ambossCapacity;
import static net.felix.features.npcalerts.data.NpcAlertState.essenzenCapacity;
import static net.felix.features.npcalerts.data.NpcAlertState.forschung;
import static net.felix.features.npcalerts.data.NpcAlertState.jaegerCapacity;
import static net.felix.features.npcalerts.data.NpcAlertState.komboKiste;
import static net.felix.features.npcalerts.data.NpcAlertState.machtkristallSlots;
import static net.felix.features.npcalerts.data.NpcAlertState.recyclerSlot1;
import static net.felix.features.npcalerts.data.NpcAlertState.recyclerSlot2;
import static net.felix.features.npcalerts.data.NpcAlertState.recyclerSlot3;
import static net.felix.features.npcalerts.data.NpcAlertState.schmelzofenCapacity;
import static net.felix.features.npcalerts.data.NpcAlertState.seelenCapacity;
import static net.felix.features.npcalerts.message.ScreenMessages.getMachtkristallShowWarning;
import static net.felix.features.npcalerts.message.ScreenMessages.shouldShowForschungWarning;
import static net.felix.features.npcalerts.overlay.PercentFormatter.formatPercent;
import static net.felix.features.npcalerts.parser.KomboKisteReader.getKomboKisteFractionDisplay;

/**
 * Berechnet die Textzeilen des Haupt-Overlays (auch fuer den Bearbeitungsmodus).
 */
public final class MainOverlayLines {

	private MainOverlayLines() {
	}

	/**
	 * Gibt die Anzahl der Zeilen im Haupt-Overlay zurück (für DraggableOverlay)
	 */
	public static int getMainOverlayLineCount() {
		int count = 0;
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsForschung && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsForschungSeparateOverlay) {
			count++;
		}
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsAmboss && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsAmbossSeparateOverlay) {
			count++;
		}
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsSchmelzofen && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSchmelzofenSeparateOverlay) {
			count++;
		}
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsJaeger && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsJaegerSeparateOverlay) {
			count++;
		}
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsSeelen && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSeelenSeparateOverlay) {
			count++;
		}
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsEssenzen && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsEssenzenSeparateOverlay) {
			count++;
		}
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsKomboKiste && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsKomboKisteSeparateOverlay) {
			count++;
		}
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsMachtkristalle && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSeparateOverlay) {
			count += 3; // Immer 3 Slots (auch wenn leer)
		}
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot1 && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot1SeparateOverlay) {
			count++;
		}
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot2 && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot2SeparateOverlay) {
			count++;
		}
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot3 && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot3SeparateOverlay) {
			count++;
		}
		return Math.max(1, count); // Mindestens 1 Zeile
	}

	/**
	 * Gibt die Zeilen für das Haupt-Overlay zurück (für F6-Editor mit "?/?" als Werte)
	 */
	public static List<LineWithPercent> getMainOverlayLinesForEditMode() {
		List<LineWithPercent> lines = new ArrayList<>();
		
		// Forschung
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsForschung && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsForschungSeparateOverlay) {
			boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsForschungPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsForschungShowIcon;
			String displayText = showIcon ? "? / ?" : "Forschung: ? / ?";
			lines.add(new LineWithPercent(displayText, showPercent ? "0.0%" : null, showPercent, false, "forschung", showIcon));
		}
		
		// Amboss Kapazität
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsAmboss && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsAmbossSeparateOverlay) {
			boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsAmbossPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsAmbossShowIcon;
			String displayText = showIcon ? "? / ?" : "Amboss: ? / ?";
			lines.add(new LineWithPercent(displayText, showPercent ? "0.0%" : null, showPercent, false, "amboss", showIcon));
		}
		
		// Schmelzofen Kapazität
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsSchmelzofen && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSchmelzofenSeparateOverlay) {
			boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsSchmelzofenPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSchmelzofenShowIcon;
			String displayText = showIcon ? "? / ?" : "Schmelzofen: ? / ?";
			lines.add(new LineWithPercent(displayText, showPercent ? "0.0%" : null, showPercent, false, "schmelzofen", showIcon));
		}
		
		// Jäger Kapazität
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsJaeger && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsJaegerSeparateOverlay) {
			boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsJaegerPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsJaegerShowIcon;
			String displayText = showIcon ? "? / ?" : "Jäger: ? / ?";
			lines.add(new LineWithPercent(displayText, showPercent ? "0.0%" : null, showPercent, false, "jaeger", showIcon));
		}
		
		// Seelen Kapazität
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsSeelen && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSeelenSeparateOverlay) {
			boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsSeelenPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSeelenShowIcon;
			String displayText = showIcon ? "? / ?" : "Seelen: ? / ?";
			lines.add(new LineWithPercent(displayText, showPercent ? "0.0%" : null, showPercent, false, "seelen", showIcon));
		}
		
		// Essenzen Kapazität
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsEssenzen && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsEssenzenSeparateOverlay) {
			boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsEssenzenPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsEssenzenShowIcon;
			String displayText = showIcon ? "? / ?" : "Essenzen: ? / ?";
			lines.add(new LineWithPercent(displayText, showPercent ? "0.0%" : null, showPercent, false, "essenzen", showIcon));
		}
		
		// Kombo Kiste
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsKomboKiste && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsKomboKisteSeparateOverlay) {
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsKomboKisteShowIcon;
			String displayText = showIcon ? "? / ?" : "Kombo Kiste: ? / ?";
			lines.add(new LineWithPercent(displayText, null, false, false, "komboKiste", showIcon));
		}
		
		// Machtkristalle
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsMachtkristalle && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSeparateOverlay) {
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleShowIcon;
			// Zeige alle 3 Slots mit Beispielwerten im Edit-Modus
			// Im Edit-Modus zeigen wir immer "0.0%" als Beispiel, um zu zeigen wie es aussehen würde
			for (int i = 0; i < 3; i++) {
				// Prüfe ob dieser Slot aktiviert ist
				boolean slotEnabled;
				switch (i) {
					case 0:
						slotEnabled = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsMachtkristalleSlot1;
						break;
					case 1:
						slotEnabled = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsMachtkristalleSlot2;
						break;
					case 2:
						slotEnabled = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsMachtkristalleSlot3;
						break;
					default:
						slotEnabled = true;
						break;
				}
				
				// Überspringe diesen Slot, wenn er deaktiviert ist
				if (!slotEnabled) {
					continue;
				}
				
				String displayText = showIcon ? "? / ?" : "MK " + (i + 1) + ": ? / ?";
				// Prüfe ob Prozente in echten Daten verfügbar wären (für Anzeige im Edit-Modus)
				MachtkristallSlot slot = machtkristallSlots[i];
				String percentText = slot.getPercentText();
				// Wenn keine Prozente verfügbar sind, zeige trotzdem "0.0%" als Beispiel im Edit-Modus
				if (percentText == null) {
					percentText = "0.0%";
				}
				lines.add(new LineWithPercent(displayText, percentText, true, false, "machtkristalle", showIcon));
			}
		}
		
		// Recycler Slots
		boolean showRecyclerPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerPercent;
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot1 && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot1SeparateOverlay) {
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot1ShowIcon;
			String displayText = showIcon ? "? / ?" : "Recycler Slot 1: ? / ?";
			String percent = null;
			if (showRecyclerPercent && recyclerSlot1.isValid()) {
				percent = formatPercent(recyclerSlot1.current, recyclerSlot1.max);
			}
			lines.add(new LineWithPercent(displayText, percent, showRecyclerPercent && percent != null, false, "recyclerSlot1", showIcon));
		}
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot2 && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot2SeparateOverlay) {
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot2ShowIcon;
			String displayText = showIcon ? "? / ?" : "Recycler Slot 2: ? / ?";
			String percent = null;
			if (showRecyclerPercent && recyclerSlot2.isValid()) {
				percent = formatPercent(recyclerSlot2.current, recyclerSlot2.max);
			}
			lines.add(new LineWithPercent(displayText, percent, showRecyclerPercent && percent != null, false, "recyclerSlot2", showIcon));
		}
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot3 && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot3SeparateOverlay) {
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot3ShowIcon;
			String displayText = showIcon ? "? / ?" : "Recycler Slot 3: ? / ?";
			String percent = null;
			if (showRecyclerPercent && recyclerSlot3.isValid()) {
				percent = formatPercent(recyclerSlot3.current, recyclerSlot3.max);
			}
			lines.add(new LineWithPercent(displayText, percent, showRecyclerPercent && percent != null, false, "recyclerSlot3", showIcon));
		}
		
		return lines;
	}

	/**
	 * Gibt die tatsächlichen Zeilen für das Haupt-Overlay zurück (mit echten Werten)
	 * Wird für die Breitenberechnung im F6-Editor verwendet
	 */
	public static List<LineWithPercent> getMainOverlayLines() {
		List<LineWithPercent> lines = new ArrayList<>();
		
		// Forschung
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsForschung && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsForschungSeparateOverlay) {
			String percent = null;
			boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsForschungPercent;
			if (showPercent) {
				if (forschung.isValid()) {
					// Forschung zählt runter: wenn current näher an max ist, ist der Prozent höher
					// Prozent = (current / max) * 100
					percent = formatPercent(forschung.current, forschung.max);
				} else {
					// Prüfe ob "Nicht im Tab-Widget" angezeigt wird
					String displayString = forschung.getDisplayString();
					if (displayString != null && displayString.contains("Nicht im Tab-Widget")) {
						// Keine Prozentanzeige wenn "Nicht im Tab-Widget"
						showPercent = false;
						percent = null;
					} else {
						// Zeige "?%" wenn Daten noch nicht verfügbar sind
						percent = "?%";
					}
				}
			}
			// Warnung: wenn verbleibender Wert <= Schwelle (0-23)
			int warnValue = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsForschungWarnValue;
			boolean showWarning = shouldShowForschungWarning(warnValue);
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsForschungShowIcon;
			String displayText = showIcon ? forschung.getDisplayString() : "Forschung: " + forschung.getDisplayString();
			lines.add(new LineWithPercent(displayText, percent, showPercent, showWarning, "forschung", showIcon));
		}
		
		// Amboss Kapazität
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsAmboss && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsAmbossSeparateOverlay) {
			String percent = null;
			boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsAmbossPercent;
			if (showPercent && ambossCapacity.isValid()) {
				percent = formatPercent(ambossCapacity.current, ambossCapacity.max);
			}
			double currentPercent = ambossCapacity.isValid() ? 
				((double)ambossCapacity.current / (double)ambossCapacity.max) * 100.0 : 0;
			double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsAmbossWarnPercent;
			boolean showWarning = ambossCapacity.isValid() && warnPercent >= 0 && currentPercent >= warnPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsAmbossShowIcon;
			String displayText = showIcon ? ambossCapacity.getDisplayString() : "Amboss: " + ambossCapacity.getDisplayString();
			lines.add(new LineWithPercent(displayText, percent, showPercent, showWarning, "amboss", showIcon));
		}
		
		// Schmelzofen Kapazität
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsSchmelzofen && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSchmelzofenSeparateOverlay) {
			String percent = null;
			boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsSchmelzofenPercent;
			if (showPercent && schmelzofenCapacity.isValid()) {
				percent = formatPercent(schmelzofenCapacity.current, schmelzofenCapacity.max);
			}
			double currentPercent = schmelzofenCapacity.isValid() ? 
				((double)schmelzofenCapacity.current / (double)schmelzofenCapacity.max) * 100.0 : 0;
			double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSchmelzofenWarnPercent;
			boolean showWarning = schmelzofenCapacity.isValid() && warnPercent >= 0 && currentPercent >= warnPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSchmelzofenShowIcon;
			String displayText = showIcon ? schmelzofenCapacity.getDisplayString() : "Schmelzofen: " + schmelzofenCapacity.getDisplayString();
			lines.add(new LineWithPercent(displayText, percent, showPercent, showWarning, "schmelzofen", showIcon));
		}
		
		// Jäger Kapazität
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsJaeger && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsJaegerSeparateOverlay) {
			String percent = null;
			boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsJaegerPercent;
			if (showPercent && jaegerCapacity.isValid()) {
				percent = formatPercent(jaegerCapacity.current, jaegerCapacity.max);
			}
			double currentPercent = jaegerCapacity.isValid() ? 
				((double)jaegerCapacity.current / (double)jaegerCapacity.max) * 100.0 : 0;
			double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsJaegerWarnPercent;
			boolean showWarning = jaegerCapacity.isValid() && warnPercent >= 0 && currentPercent >= warnPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsJaegerShowIcon;
			String displayText = showIcon ? jaegerCapacity.getDisplayStringWithoutCurrentSuffix() : "Jäger: " + jaegerCapacity.getDisplayStringWithoutCurrentSuffix();
			lines.add(new LineWithPercent(displayText, percent, showPercent, showWarning, "jaeger", showIcon));
		}
		
		// Seelen Kapazität
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsSeelen && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSeelenSeparateOverlay) {
			String percent = null;
			boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsSeelenPercent;
			if (showPercent && seelenCapacity.isValid()) {
				percent = formatPercent(seelenCapacity.current, seelenCapacity.max);
			}
			double currentPercent = seelenCapacity.isValid() ? 
				((double)seelenCapacity.current / (double)seelenCapacity.max) * 100.0 : 0;
			double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSeelenWarnPercent;
			boolean showWarning = seelenCapacity.isValid() && warnPercent >= 0 && currentPercent >= warnPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSeelenShowIcon;
			String displayText = showIcon ? seelenCapacity.getDisplayString() : "Seelen: " + seelenCapacity.getDisplayString();
			lines.add(new LineWithPercent(displayText, percent, showPercent, showWarning, "seelen", showIcon));
		}
		
		// Essenzen Kapazität
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsEssenzen && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsEssenzenSeparateOverlay) {
			String percent = null;
			boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsEssenzenPercent;
			if (showPercent && essenzenCapacity.isValid()) {
				percent = formatPercent(essenzenCapacity.current, essenzenCapacity.max);
			}
			double currentPercent = essenzenCapacity.isValid() ? 
				((double)essenzenCapacity.current / (double)essenzenCapacity.max) * 100.0 : 0;
			double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsEssenzenWarnPercent;
			boolean showWarning = essenzenCapacity.isValid() && warnPercent >= 0 && currentPercent >= warnPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsEssenzenShowIcon;
			String displayText = showIcon ? essenzenCapacity.getDisplayString() : "Essenzen: " + essenzenCapacity.getDisplayString();
			lines.add(new LineWithPercent(displayText, percent, showPercent, showWarning, "essenzen", showIcon));
		}
		
		// Kombo Kiste (Werte später z. B. aus Bossbar/Leaderboard)
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsKomboKiste && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsKomboKisteSeparateOverlay) {
			boolean showWarning = komboKiste.isValid() && komboKiste.current >= komboKiste.max;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsKomboKisteShowIcon;
			String fraction = getKomboKisteFractionDisplay();
			String displayText = showIcon ? fraction : "Kombo Kiste: " + fraction;
			lines.add(new LineWithPercent(displayText, null, false, showWarning, "komboKiste", showIcon));
		}
		
		// Machtkristalle
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsMachtkristalle && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSeparateOverlay) {
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleShowIcon;
			for (int i = 0; i < 3; i++) {
				// Prüfe ob dieser Slot aktiviert ist
				boolean slotEnabled;
				switch (i) {
					case 0:
						slotEnabled = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsMachtkristalleSlot1;
						break;
					case 1:
						slotEnabled = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsMachtkristalleSlot2;
						break;
					case 2:
						slotEnabled = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsMachtkristalleSlot3;
						break;
					default:
						slotEnabled = true;
						break;
				}
				
				// Überspringe diesen Slot, wenn er deaktiviert ist
				if (!slotEnabled) {
					continue;
				}
				
				MachtkristallSlot slot = machtkristallSlots[i];
				// Prüfe zuerst ob Slot nicht gefunden wurde (keine Machtkristall-Einträge in Tab-Liste)
				// Dies muss VOR isEmpty() geprüft werden, da isNotFound() true ist wenn name = ""
				if (slot.isNotFound()) {
					// Slot nicht gefunden (keine Machtkristall-Einträge in Tab-Liste)
						String displayText = showIcon ? "Nicht im Tab-Widget" : "MK " + (i + 1) + ": Nicht im Tab-Widget";
					lines.add(new LineWithPercent(displayText, null, false, false, "machtkristalle", showIcon));
				} else if (slot.isEmpty()) {
					// Slot ist leer (durch "[Kein Machtkristall ausgewählt]" markiert)
					String displayText = showIcon ? "-" : "MK " + (i + 1) + ": -";
					lines.add(new LineWithPercent(displayText, null, false, false, "machtkristalle", showIcon));
				} else {
					// Machtkristall gefunden - prüfe ob Daten verfügbar sind
					String displayText = slot.getDisplayText();
					String percentText = slot.getPercentText();
					// displayText sollte nie null sein, wenn isEmpty() und isNotFound() beide false sind
					// Aber zur Sicherheit prüfen wir trotzdem
					if (displayText == null) {
						// Fallback: sollte nicht passieren, aber falls doch, zeige Slot-Nummer
						displayText = showIcon ? "?" : "MK " + (i + 1) + ": ?";
					} else if (showIcon) {
						displayText = MachtkristallSlot.stripIconPrefix(displayText);
					}
					double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleWarnPercent;
					boolean showWarning = getMachtkristallShowWarning(slot, warnPercent);
					lines.add(new LineWithPercent(displayText, percentText, percentText != null, showWarning, "machtkristalle", showIcon));
				}
			}
		}
		
		// Recycler Slots
		boolean showRecyclerPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerPercent;
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot1 && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot1SeparateOverlay) {
			String percent = null;
			if (showRecyclerPercent && recyclerSlot1.isValid()) {
				percent = formatPercent(recyclerSlot1.current, recyclerSlot1.max);
			}
			double currentPercent = recyclerSlot1.isValid() ? 
				((double)recyclerSlot1.current / (double)recyclerSlot1.max) * 100.0 : 0;
			double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerWarnPercent;
			// Recycler zählt runter, daher warnen wenn currentPercent <= warnPercent
			boolean showWarning = recyclerSlot1.isValid() && warnPercent >= 0 && currentPercent <= warnPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot1ShowIcon;
			String displayText = showIcon ? recyclerSlot1.getDisplayString() : "Recycler Slot 1: " + recyclerSlot1.getDisplayString();
			lines.add(new LineWithPercent(displayText, percent, showRecyclerPercent && percent != null, showWarning, "recyclerSlot1", showIcon));
		}
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot2 && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot2SeparateOverlay) {
			String percent = null;
			if (showRecyclerPercent && recyclerSlot2.isValid()) {
				percent = formatPercent(recyclerSlot2.current, recyclerSlot2.max);
			}
			double currentPercent = recyclerSlot2.isValid() ? 
				((double)recyclerSlot2.current / (double)recyclerSlot2.max) * 100.0 : 0;
			double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerWarnPercent;
			// Recycler zählt runter, daher warnen wenn currentPercent <= warnPercent
			boolean showWarning = recyclerSlot2.isValid() && warnPercent >= 0 && currentPercent <= warnPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot2ShowIcon;
			String displayText = showIcon ? recyclerSlot2.getDisplayString() : "Recycler Slot 2: " + recyclerSlot2.getDisplayString();
			lines.add(new LineWithPercent(displayText, percent, showRecyclerPercent && percent != null, showWarning, "recyclerSlot2", showIcon));
		}
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot3 && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot3SeparateOverlay) {
			String percent = null;
			if (showRecyclerPercent && recyclerSlot3.isValid()) {
				percent = formatPercent(recyclerSlot3.current, recyclerSlot3.max);
			}
			double currentPercent = recyclerSlot3.isValid() ? 
				((double)recyclerSlot3.current / (double)recyclerSlot3.max) * 100.0 : 0;
			double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerWarnPercent;
			// Recycler zählt runter, daher warnen wenn currentPercent <= warnPercent
			boolean showWarning = recyclerSlot3.isValid() && warnPercent >= 0 && currentPercent <= warnPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot3ShowIcon;
			String displayText = showIcon ? recyclerSlot3.getDisplayString() : "Recycler Slot 3: " + recyclerSlot3.getDisplayString();
			lines.add(new LineWithPercent(displayText, percent, showRecyclerPercent && percent != null, showWarning, "recyclerSlot3", showIcon));
		}
		
		return lines;
	}
}
