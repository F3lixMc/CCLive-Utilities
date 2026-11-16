package net.felix.features.npcalerts.overlay;

import java.util.ArrayList;
import java.util.List;
import net.felix.features.npcalerts.data.MachtkristallSlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fStack;

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
import static net.felix.features.npcalerts.overlay.AlertIcons.AMBOSS_ICON;
import static net.felix.features.npcalerts.overlay.AlertIcons.ESSENZEN_ICON;
import static net.felix.features.npcalerts.overlay.AlertIcons.FORSCHUNG_ICON;
import static net.felix.features.npcalerts.overlay.AlertIcons.JAEGER_ICON;
import static net.felix.features.npcalerts.overlay.AlertIcons.KOMBO_KISTE_ICON;
import static net.felix.features.npcalerts.overlay.AlertIcons.MACHTKRISTALL_ICON;
import static net.felix.features.npcalerts.overlay.AlertIcons.RECYCLER_ICON;
import static net.felix.features.npcalerts.overlay.AlertIcons.SCHMELZOFEN_ICON;
import static net.felix.features.npcalerts.overlay.AlertIcons.SEELEN_ICON;
import static net.felix.features.npcalerts.overlay.AlertSettings.getPercentColorForConfigKey;
import static net.felix.features.npcalerts.overlay.AlertSettings.getTextColorForConfigKey;
import static net.felix.features.npcalerts.overlay.PercentFormatter.formatPercent;
import static net.felix.features.npcalerts.overlay.SeparateOverlayRenderer.renderSeparateOverlays;
import static net.felix.features.npcalerts.parser.KomboKisteReader.getKomboKisteFractionDisplay;

/**
 * Zeichnet das Haupt-Overlay mit allen aktiven NPC-Alerts.
 */
public final class MainOverlayRenderer {

	private MainOverlayRenderer() {
	}

	/**
	 * Rendert das NPC Alerts Overlay links auf dem Bildschirm
	 */
	public static void renderNpcAlertsDisplay(GuiGraphicsExtractor context, Minecraft client) {
		if (client.getWindow() == null) {
			return;
		}
		
		// Prüfe ob NPC Alerts Utility aktiviert ist
		if (!net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsUtilityEnabled) {
			return;
		}
		if (!net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsOverlaysVisible) {
			return;
		}
		
		// Hilfsklasse für Zeilen mit optionalen Prozenten und Warnung
		
		// Sammle alle anzuzeigenden Zeilen
		List<LineWithPercent> lines = new ArrayList<>();
		
		// Forschung
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsForschung && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsForschungSeparateOverlay) {
			// Zeige immer an, auch wenn noch keine Werte gefunden wurden
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
			// Zeige immer an, auch wenn noch keine Werte gefunden wurden
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
			// Zeige immer an, auch wenn noch keine Werte gefunden wurden
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
			// Zeige immer an, auch wenn noch keine Werte gefunden wurden
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
			// Zeige immer an, auch wenn noch keine Werte gefunden wurden
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
			// Zeige immer an, auch wenn noch keine Werte gefunden wurden
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
		boolean showRecyclerPercent2 = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerPercent;
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot1 && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot1SeparateOverlay) {
			// Zeige immer an, auch wenn noch keine Werte gefunden wurden
			String percent = null;
			if (showRecyclerPercent2 && recyclerSlot1.isValid()) {
				percent = formatPercent(recyclerSlot1.current, recyclerSlot1.max);
			}
			double currentPercent = recyclerSlot1.isValid() ? 
				((double)recyclerSlot1.current / (double)recyclerSlot1.max) * 100.0 : 0;
			double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerWarnPercent;
			// Recycler zählt runter, daher warnen wenn currentPercent <= warnPercent
			boolean showWarning = recyclerSlot1.isValid() && warnPercent >= 0 && currentPercent <= warnPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot1ShowIcon;
			String displayText = showIcon ? recyclerSlot1.getDisplayString() : "Recycler Slot 1: " + recyclerSlot1.getDisplayString();
			lines.add(new LineWithPercent(displayText, percent, showRecyclerPercent2 && percent != null, showWarning, "recyclerSlot1", showIcon));
		}
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot2 && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot2SeparateOverlay) {
			// Zeige immer an, auch wenn noch keine Werte gefunden wurden
			String percent = null;
			if (showRecyclerPercent2 && recyclerSlot2.isValid()) {
				percent = formatPercent(recyclerSlot2.current, recyclerSlot2.max);
			}
			double currentPercent = recyclerSlot2.isValid() ? 
				((double)recyclerSlot2.current / (double)recyclerSlot2.max) * 100.0 : 0;
			double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerWarnPercent;
			// Recycler zählt runter, daher warnen wenn currentPercent <= warnPercent
			boolean showWarning = recyclerSlot2.isValid() && warnPercent >= 0 && currentPercent <= warnPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot2ShowIcon;
			String displayText = showIcon ? recyclerSlot2.getDisplayString() : "Recycler Slot 2: " + recyclerSlot2.getDisplayString();
			lines.add(new LineWithPercent(displayText, percent, showRecyclerPercent2 && percent != null, showWarning, "recyclerSlot2", showIcon));
		}
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot3 && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot3SeparateOverlay) {
			// Zeige immer an, auch wenn noch keine Werte gefunden wurden
			String percent = null;
			if (showRecyclerPercent2 && recyclerSlot3.isValid()) {
				percent = formatPercent(recyclerSlot3.current, recyclerSlot3.max);
			}
			double currentPercent = recyclerSlot3.isValid() ? 
				((double)recyclerSlot3.current / (double)recyclerSlot3.max) * 100.0 : 0;
			double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerWarnPercent;
			// Recycler zählt runter, daher warnen wenn currentPercent <= warnPercent
			boolean showWarning = recyclerSlot3.isValid() && warnPercent >= 0 && currentPercent <= warnPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot3ShowIcon;
			String displayText = showIcon ? recyclerSlot3.getDisplayString() : "Recycler Slot 3: " + recyclerSlot3.getDisplayString();
			lines.add(new LineWithPercent(displayText, percent, showRecyclerPercent2 && percent != null, showWarning, "recyclerSlot3", showIcon));
		}
		
		// Das Overlay zeigt jetzt immer die eingeschalteten Informationen an, auch wenn noch keine Werte gefunden wurden
		// Wenn keine Informationen für das Haupt-Overlay eingeschaltet sind, wird nichts angezeigt
		
		// Berechne die maximale Breite des Textes (inklusive Prozente und Warnung direkt danach)
		int maxWidth = 0;
		for (LineWithPercent line : lines) {
			int width = 0;
			// Wenn Icon aktiviert ist, füge Icon-Breite hinzu
			if (line.showIcon && (line.configKey != null && ("forschung".equals(line.configKey) || "amboss".equals(line.configKey) || 
			                                                   "schmelzofen".equals(line.configKey) || "seelen".equals(line.configKey) || 
			                                                   "essenzen".equals(line.configKey) || "jaeger".equals(line.configKey) || "komboKiste".equals(line.configKey) || 
			                                                   "machtkristalle".equals(line.configKey) ||
			                                                   "recyclerSlot1".equals(line.configKey) || "recyclerSlot2".equals(line.configKey) || 
			                                                   "recyclerSlot3".equals(line.configKey)))) {
				int iconSize = (int)(client.font.lineHeight * 1.5);
				width += iconSize + 2; // Icon + Abstand
				width += client.font.width(": "); // Doppelpunkt nach Icon
			}
			width += client.font.width(line.text);
			if (line.showPercent && line.percentText != null) {
				width += client.font.width(" " + line.percentText); // Abstand + Prozente
			}
			if (width > maxWidth) {
				maxWidth = width;
			}
		}
		
		// Padding
		final int PADDING = 5;
		final int LINE_HEIGHT = client.font.lineHeight + 2; // Verwende tatsächliche Schrift-Höhe
		
		// Berechne die tatsächliche Zeilenhöhe unter Berücksichtigung von Icons
		int actualLineHeight = LINE_HEIGHT;
		int iconLineCount = 0; // Zähle Zeilen mit Icons für zusätzlichen Abstand
		for (LineWithPercent line : lines) {
			if (line.showIcon && line.configKey != null) {
				int iconSize = (int)(client.font.lineHeight * 1.5);
				// Die tatsächliche Höhe ist das Maximum aus Icon-Höhe und Text-Höhe
				actualLineHeight = Math.max(actualLineHeight, iconSize);
				iconLineCount++;
			}
		}
		
		// Wenn keine Zeilen vorhanden sind, rendere nichts (auch keinen Hintergrund)
		// Rendere nur separate Overlays
		if (lines.isEmpty()) {
			renderSeparateOverlays(context, client);
			return;
		}
		
		// Berechne unskalierte Overlay-Dimensionen komplett dynamisch basierend auf Inhalt (wie BossHP-Overlay)
		// Berücksichtige zusätzlichen Abstand für Zeilen mit Icons (2 Pixel pro Icon-Zeile)
		int unscaledWidth = maxWidth + (PADDING * 2);
		int unscaledHeight = (lines.size() * actualLineHeight) + (iconLineCount * 2) + (PADDING * 2);
		
		// Get scale
		float scale = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMainOverlayScale;
		if (scale <= 0) scale = 1.0f;
		
		// Berechne skalierte Dimensionen
		int overlayWidth = Math.round(unscaledWidth * scale);
		int overlayHeight = Math.round(unscaledHeight * scale);
		
		// Position aus Config: baseX ist die linke Kante (wie beim Mining-Overlay)
		int baseX = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMainOverlayX;
		int yPosition = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMainOverlayY;
		
		// Determine if overlay is on left or right side of screen
		int screenWidth = client.getWindow().getGuiScaledWidth();
		boolean isOnLeftSide = baseX < screenWidth / 2;
		
		// Calculate X position based on side (same logic as Mining overlays)
		int posX;
		if (isOnLeftSide) {
			// On left side: keep left edge fixed, expand to the right
			posX = baseX;
		} else {
			// On right side: keep right edge fixed, expand to the left
			// Right edge is: baseX (since baseX is on the right side, it represents the right edge)
			// Keep this right edge fixed, so left edge moves left when width increases
			posX = baseX - overlayWidth;
		}
		
		// Ensure overlay stays within screen bounds
		posX = Math.max(0, Math.min(posX, screenWidth - overlayWidth));
		int posY = yPosition;
		
		// Zeichne semi-transparenten Hintergrund (wenn aktiviert) - skaliert
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMainOverlayShowBackground) {
			context.fill(posX, posY, posX + overlayWidth, posY + overlayHeight, 0x80000000);
		}
		
		// Render content with scale using matrix transformation
		Matrix3x2fStack matrices = context.pose();
		matrices.pushMatrix();
		matrices.translate(posX, posY);
		matrices.scale(scale, scale);
		
		// Zeichne alle Zeilen - vertikal zentriert (nur wenn Zeilen vorhanden sind)
		if (!lines.isEmpty()) {
			// Berechne die tatsächliche Text-Höhe (inkl. zusätzlicher Abstände für Icon-Zeilen)
			int totalTextHeight = 0;
			for (LineWithPercent line : lines) {
				totalTextHeight += actualLineHeight;
				if (line.showIcon && line.configKey != null) {
					totalTextHeight += 2; // Zusätzlicher Abstand für Icon-Zeilen
				}
			}
			// Zentriere: Overlay-Mitte, dann verschiebe nach oben um die Hälfte der Text-Höhe
			// Die erste Zeile beginnt bei overlayCenterY - totalTextHeight / 2
			// Da die Text-Baseline unten ist, müssen wir die erste Zeile etwas nach oben verschieben
			// Verwende unskalierte Höhe für Zentrierung (Koordinaten sind nach Matrix-Transformation relativ)
			int overlayCenterY = unscaledHeight / 2;
			int currentY = overlayCenterY - totalTextHeight / 2;
			int warningColor = 0xFFFF0000; // Rot für Warnungen
			
			for (LineWithPercent line : lines) {
			// Hole konfigurierte Farben für diese Zeile
			int textColor = getTextColorForConfigKey(line.configKey);
			int percentColor = getPercentColorForConfigKey(line.configKey);
			// Prüfe, ob die Zeile nicht leer ist
			if (line.text == null || line.text.trim().isEmpty()) {
				currentY += actualLineHeight;
				continue;
			}
			
			// Zeichne Haupttext
			Component textComponent = Component.literal(line.text);
			
			try {
				int currentX = PADDING; // Relativ zu (xPosition, yPosition) nach Matrix-Transformation
				
				// Bestimme Textfarbe: rot und blinkend wenn Warnung aktiv ist
				int currentTextColor = textColor;
				if (line.showWarning) {
					// Blink-Animation: alle 300ms wechseln
					boolean isVisible = (System.currentTimeMillis() / 300) % 2 == 0;
					if (isVisible) {
						currentTextColor = warningColor;
					} else {
						currentTextColor = textColor;
					}
				}
				
				// Zeichne Icon statt Text, wenn aktiviert (für Forschung, Amboss, Schmelzofen, Seelen, Essenzen und Recycler)
				if (line.showIcon && (line.configKey != null)) {
					int iconSize = (int)(client.font.lineHeight * 1.5); // Icon-Größe = 1.5x Text-Höhe
					// Zentriere Icon vertikal: Overlay-Mitte minus die Hälfte der Icon-Höhe
					int lineCenterY = currentY + actualLineHeight / 2;
					int iconY = lineCenterY - iconSize / 2;
					Identifier iconToUse = null;
					String fallbackText = null;
					
					if ("forschung".equals(line.configKey)) {
						iconToUse = FORSCHUNG_ICON;
						fallbackText = "Forschung: ";
					} else if ("amboss".equals(line.configKey)) {
						iconToUse = AMBOSS_ICON;
						fallbackText = "Amboss: ";
					} else if ("schmelzofen".equals(line.configKey)) {
						iconToUse = SCHMELZOFEN_ICON;
						fallbackText = "Schmelzofen: ";
					} else if ("seelen".equals(line.configKey)) {
						iconToUse = SEELEN_ICON;
						fallbackText = "Seelen: ";
					} else if ("essenzen".equals(line.configKey)) {
						iconToUse = ESSENZEN_ICON;
						fallbackText = "Essenzen: ";
					} else if ("jaeger".equals(line.configKey)) {
						iconToUse = JAEGER_ICON;
						fallbackText = "Jäger: ";
					} else if ("komboKiste".equals(line.configKey)) {
						iconToUse = KOMBO_KISTE_ICON;
						fallbackText = "Kombo Kiste: ";
					} else if ("recyclerSlot1".equals(line.configKey)) {
						iconToUse = RECYCLER_ICON;
						fallbackText = "Recycler Slot 1: ";
					} else if ("recyclerSlot2".equals(line.configKey)) {
						iconToUse = RECYCLER_ICON;
						fallbackText = "Recycler Slot 2: ";
					} else if ("recyclerSlot3".equals(line.configKey)) {
						iconToUse = RECYCLER_ICON;
						fallbackText = "Recycler Slot 3: ";
					} else if ("machtkristalle".equals(line.configKey)) {
						iconToUse = MACHTKRISTALL_ICON;
						fallbackText = "MK: ";
					}
					
					if (iconToUse != null) {
						boolean iconDrawn = false;
						int textYForIcon = currentY; // Standard Y-Position für Text
						try {
							context.blit(
								RenderPipelines.GUI_TEXTURED,
								iconToUse,
								currentX, iconY,
								0.0f, 0.0f,
								iconSize, iconSize,
								iconSize, iconSize
							);
							currentX += iconSize + 2; // Abstand nach Icon
							iconDrawn = true;
							// Zentriere Text vertikal zum Icon: Icon-Mitte minus die Hälfte der Text-Höhe
							textYForIcon = lineCenterY - client.font.lineHeight / 2;
						} catch (Exception e) {
							// Fallback: Zeichne Text wenn Icon nicht geladen werden kann
							if (fallbackText != null) {
								context.text(
									client.font,
									Component.literal(fallbackText),
									currentX,
									currentY,
									currentTextColor,
									true
								);
								currentX += client.font.width(fallbackText);
							}
						}
						// Zeichne Doppelpunkt nach dem Icon (vertikal zentriert zum Icon)
						if (iconDrawn) {
							context.text(
								client.font,
								Component.literal(": "),
								currentX,
								textYForIcon,
								currentTextColor,
								true
							);
							currentX += client.font.width(": ");
						}
						// Zeichne die Werte nach dem Doppelpunkt (vertikal zentriert zum Icon)
						context.text(
							client.font,
							Component.literal(line.text),
							currentX,
							textYForIcon,
							currentTextColor,
							true
						);
						currentX += client.font.width(line.text);
					} else {
						// Fallback: Zeichne normalen Text
						context.text(
							client.font,
							textComponent,
							currentX,
							currentY,
							currentTextColor,
							true
						);
						currentX += client.font.width(line.text);
					}
				} else {
					// Zeichne Haupttext (inkl. "Amboss:" wenn kein Icon)
					context.text(
						client.font,
						textComponent,
						currentX,
						currentY,
						currentTextColor,
						true // Mit Schatten für bessere Lesbarkeit
					);
					currentX += client.font.width(line.text);
				}
				
				// Zeichne Prozente direkt nach dem Text, wenn vorhanden
				// Wenn Warnung aktiv ist, blinkt auch der Prozentwert rot
				if (line.showPercent && line.percentText != null) {
					Component percentComponent = Component.literal(" " + line.percentText);
					int currentPercentColor = line.showWarning ? currentTextColor : percentColor;
					// Wenn Icon vorhanden ist, verwende die zentrierte Y-Position
					int percentY = (line.showIcon && line.configKey != null) ? 
						(currentY + actualLineHeight / 2 - client.font.lineHeight / 2) : currentY;
					context.text(
						client.font,
						percentComponent,
						currentX,
						percentY,
						currentPercentColor,
						true // Mit Schatten für bessere Lesbarkeit
					);
					currentX += client.font.width(" " + line.percentText);
				}
			} catch (Exception e) {
				// Fallback: Versuche mit String direkt
				try {
					// Bestimme Textfarbe: rot und blinkend wenn Warnung aktiv ist
					int currentTextColor = textColor;
					if (line.showWarning) {
						// Blink-Animation: alle 300ms wechseln
						boolean isVisible = (System.currentTimeMillis() / 300) % 2 == 0;
						if (isVisible) {
							currentTextColor = warningColor;
						} else {
							currentTextColor = textColor;
						}
					}
					
					context.text(
						client.font,
						line.text,
						PADDING, // Relativ zu (xPosition, yPosition) nach Matrix-Transformation
						currentY,
						currentTextColor,
						true
					);
					
					// Zeichne Prozente direkt nach dem Text, wenn vorhanden
					// Wenn Warnung aktiv ist, blinkt auch der Prozentwert rot
					int currentX = PADDING + client.font.width(line.text);
					if (line.showPercent && line.percentText != null) {
						int currentPercentColor = line.showWarning ? currentTextColor : percentColor;
						context.text(
							client.font,
							" " + line.percentText,
							currentX,
							currentY,
							currentPercentColor,
							true
						);
						currentX += client.font.width(" " + line.percentText);
					}
				} catch (Exception e2) {
					// Ignoriere Fehler
				}
			}
			// Verwende actualLineHeight statt LINE_HEIGHT, um größere Icons zu berücksichtigen
			// Wenn diese Zeile ein Icon hat, füge zusätzlichen Abstand hinzu
			int lineSpacing = actualLineHeight;
			if (line.showIcon && line.configKey != null) {
				// Zusätzlicher Abstand für Zeilen mit Icons
				lineSpacing += 2;
			}
			currentY += lineSpacing;
		}
		} // Ende if (!lines.isEmpty())
		
		matrices.popMatrix();
		
		// Rendere separate Overlays für Informationen, die diese Option aktiviert haben
		renderSeparateOverlays(context, client);
	}
}
