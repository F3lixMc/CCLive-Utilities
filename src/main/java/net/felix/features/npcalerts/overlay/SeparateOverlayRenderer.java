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
import static net.felix.features.npcalerts.overlay.AlertSettings.getSeparateOverlayScale;
import static net.felix.features.npcalerts.overlay.AlertSettings.getTextColorForConfigKey;
import static net.felix.features.npcalerts.overlay.PercentFormatter.formatPercent;
import static net.felix.features.npcalerts.overlay.SingleInfoOverlayRenderer.renderSingleInfoOverlay;
import static net.felix.features.npcalerts.parser.KomboKisteReader.getKomboKisteFractionDisplay;

/**
 * Zeichnet die getrennten Einzel-Overlays pro NPC-Alert.
 */
public final class SeparateOverlayRenderer {

	private SeparateOverlayRenderer() {
	}

	/**
	 * Rendert separate Overlays für einzelne Informationen
	 */
	static void renderSeparateOverlays(GuiGraphicsExtractor context, Minecraft client) {
		// Forschung
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsForschung && 
		    net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsForschungSeparateOverlay) {
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
			renderSingleInfoOverlay(context, client, displayText, 
				percent, showPercent, showWarning, "forschung", showIcon,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsForschungX,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsForschungY,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsForschungShowBackground);
		}
		
		// Amboss
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsAmboss && 
		    net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsAmbossSeparateOverlay) {
			String percent = null;
			boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsAmbossPercent;
			if (showPercent && ambossCapacity.isValid()) {
				percent = formatPercent(ambossCapacity.current, ambossCapacity.max);
			}
			double currentPercent = ambossCapacity.isValid() ? 
				((double)ambossCapacity.current / (double)ambossCapacity.max) * 100.0 : 0;
			double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsAmbossWarnPercent;
			boolean showWarning = warnPercent >= 0 && currentPercent >= warnPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsAmbossShowIcon;
			String displayText = showIcon ? ambossCapacity.getDisplayString() : "Amboss: " + ambossCapacity.getDisplayString();
			renderSingleInfoOverlay(context, client, displayText, 
				percent, showPercent, showWarning, "amboss", showIcon,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsAmbossX,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsAmbossY,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsAmbossShowBackground);
		}
		
		// Schmelzofen
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsSchmelzofen && 
		    net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSchmelzofenSeparateOverlay) {
			String percent = null;
			boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsSchmelzofenPercent;
			if (showPercent && schmelzofenCapacity.isValid()) {
				percent = formatPercent(schmelzofenCapacity.current, schmelzofenCapacity.max);
			}
			double currentPercent = schmelzofenCapacity.isValid() ? 
				((double)schmelzofenCapacity.current / (double)schmelzofenCapacity.max) * 100.0 : 0;
			double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSchmelzofenWarnPercent;
			boolean showWarning = warnPercent >= 0 && currentPercent >= warnPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSchmelzofenShowIcon;
			String displayText = showIcon ? schmelzofenCapacity.getDisplayString() : "Schmelzofen: " + schmelzofenCapacity.getDisplayString();
			renderSingleInfoOverlay(context, client, displayText, 
				percent, showPercent, showWarning, "schmelzofen", showIcon,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSchmelzofenX,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSchmelzofenY,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSchmelzofenShowBackground);
		}
		
		// Jäger
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsJaeger && 
		    net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsJaegerSeparateOverlay) {
			String percent = null;
			boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsJaegerPercent;
			if (showPercent && jaegerCapacity.isValid()) {
				percent = formatPercent(jaegerCapacity.current, jaegerCapacity.max);
			}
			double currentPercent = jaegerCapacity.isValid() ? 
				((double)jaegerCapacity.current / (double)jaegerCapacity.max) * 100.0 : 0;
			double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsJaegerWarnPercent;
			boolean showWarning = warnPercent >= 0 && currentPercent >= warnPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsJaegerShowIcon;
			String displayText = showIcon ? jaegerCapacity.getDisplayStringWithoutCurrentSuffix() : "Jäger: " + jaegerCapacity.getDisplayStringWithoutCurrentSuffix();
			renderSingleInfoOverlay(context, client, displayText, 
				percent, showPercent, showWarning, "jaeger", showIcon,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsJaegerX,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsJaegerY,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsJaegerShowBackground);
		}
		
		// Seelen
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsSeelen && 
		    net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSeelenSeparateOverlay) {
			String percent = null;
			boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsSeelenPercent;
			if (showPercent && seelenCapacity.isValid()) {
				percent = formatPercent(seelenCapacity.current, seelenCapacity.max);
			}
			double currentPercent = seelenCapacity.isValid() ? 
				((double)seelenCapacity.current / (double)seelenCapacity.max) * 100.0 : 0;
			double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSeelenWarnPercent;
			boolean showWarning = warnPercent >= 0 && currentPercent >= warnPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSeelenShowIcon;
			String displayText = showIcon ? seelenCapacity.getDisplayString() : "Seelen: " + seelenCapacity.getDisplayString();
			renderSingleInfoOverlay(context, client, displayText, 
				percent, showPercent, showWarning, "seelen", showIcon,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSeelenX,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSeelenY,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSeelenShowBackground);
		}
		
		// Essenzen
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsEssenzen && 
		    net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsEssenzenSeparateOverlay) {
			String percent = null;
			boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsEssenzenPercent;
			if (showPercent && essenzenCapacity.isValid()) {
				percent = formatPercent(essenzenCapacity.current, essenzenCapacity.max);
			}
			double currentPercent = essenzenCapacity.isValid() ? 
				((double)essenzenCapacity.current / (double)essenzenCapacity.max) * 100.0 : 0;
			double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsEssenzenWarnPercent;
			boolean showWarning = warnPercent >= 0 && currentPercent >= warnPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsEssenzenShowIcon;
			String displayText = showIcon ? essenzenCapacity.getDisplayString() : "Essenzen: " + essenzenCapacity.getDisplayString();
			renderSingleInfoOverlay(context, client, displayText, 
				percent, showPercent, showWarning, "essenzen", showIcon,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsEssenzenX,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsEssenzenY,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsEssenzenShowBackground);
		}
		
		// Kombo Kiste
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsKomboKiste && 
		    net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsKomboKisteSeparateOverlay) {
			boolean showWarning = komboKiste.isValid() && komboKiste.current >= komboKiste.max;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsKomboKisteShowIcon;
			String fraction = getKomboKisteFractionDisplay();
			String displayText = showIcon ? fraction : "Kombo Kiste: " + fraction;
			renderSingleInfoOverlay(context, client, displayText, 
				null, false, showWarning, "komboKiste", showIcon,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsKomboKisteX,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsKomboKisteY,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsKomboKisteShowBackground);
		}
		
		// Machtkristalle - prüfe ob einzeln oder zusammen gerendert werden soll
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsMachtkristalle && 
		    net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSeparateOverlay) {
			// Prüfe welche Slots einzeln gerendert werden sollen
			boolean slot1Separate = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSlot1Separate;
			boolean slot2Separate = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSlot2Separate;
			boolean slot3Separate = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSlot3Separate;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleShowIcon;
			
			// Liste für Slots, die zusammen gerendert werden sollen
			List<LineWithPercent> mkLines = new ArrayList<>();
			int yOffset = 0; // Für einzelne Overlays
			
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
				boolean slotSeparate = (i == 0 && slot1Separate) || (i == 1 && slot2Separate) || (i == 2 && slot3Separate);
				
				String displayText;
				String percentText = null;
				
				// Prüfe zuerst ob Slot nicht gefunden wurde (keine Machtkristall-Einträge in Tab-Liste)
				// Dies muss VOR isEmpty() geprüft werden, da isNotFound() true ist wenn name = ""
				if (slot.isNotFound()) {
					// Slot nicht gefunden (keine Machtkristall-Einträge in Tab-Liste)
					displayText = showIcon ? "Nicht im Tab-Widget" : "MK " + (i + 1) + ": Nicht im Tab-Widget";
				} else if (slot.isEmpty()) {
					// Slot ist leer (durch "[Kein Machtkristall ausgewählt]" markiert)
					displayText = showIcon ? "-" : "MK " + (i + 1) + ": -";
				} else {
					// Machtkristall gefunden - prüfe ob Daten verfügbar sind
					displayText = slot.getDisplayText();
					percentText = slot.getPercentText();
					if (displayText == null) {
						displayText = showIcon ? "?" : "MK " + (i + 1) + ": ?";
					} else if (showIcon) {
						// Entferne "MK [Name]: " Präfix wenn Icon angezeigt wird
						displayText = MachtkristallSlot.stripIconPrefix(displayText);
					}
				}
				
				// Warnung: wenn Prozent >= dem Warnwert ist (wie bei Amboss)
				double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleWarnPercent;
				boolean showWarning = getMachtkristallShowWarning(slot, warnPercent);
				
				if (slotSeparate) {
					// Rendere einzeln mit individueller Position
					int slotX, slotY;
					switch (i) {
						case 0:
							slotX = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSlot1X;
							slotY = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSlot1Y;
							break;
						case 1:
							slotX = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSlot2X;
							slotY = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSlot2Y;
							break;
						case 2:
							slotX = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSlot3X;
							slotY = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSlot3Y;
							break;
						default:
							slotX = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleX;
							slotY = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleY;
							break;
					}
					// Bestimme den richtigen configKey für die Skalierung
					String slotConfigKey;
					switch (i) {
						case 0:
							slotConfigKey = "machtkristalleSlot1";
							break;
						case 1:
							slotConfigKey = "machtkristalleSlot2";
							break;
						case 2:
							slotConfigKey = "machtkristalleSlot3";
							break;
						default:
							slotConfigKey = "machtkristalle";
							break;
					}
					renderSingleInfoOverlay(context, client, 
						displayText, 
						percentText, percentText != null, showWarning, slotConfigKey, showIcon,
						slotX,
						slotY,
						net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleShowBackground);
				} else {
					// Füge zur Liste hinzu (wird zusammen gerendert)
					mkLines.add(new LineWithPercent(displayText, percentText, percentText != null, showWarning, "machtkristalle", showIcon));
				}
			}
			
			// Rendere alle Zeilen, die zusammen gerendert werden sollen, in einem einzigen Overlay
			if (!mkLines.isEmpty()) {
				// Berechne Y-Position für das Multi-Line-Overlay (nach den einzelnen Overlays)
				int multiLineY = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleY + yOffset;
				renderMultiLineOverlay(context, client, mkLines,
					net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleX,
					multiLineY,
					net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleShowBackground,
					"machtkristalle");
			}
		}
		
		// Recycler Slots - prüfe ob einzeln oder zusammen gerendert werden soll
		// Prüfe ob mindestens ein Recycler-Slot aktiviert ist und "Separates Overlay" hat
		boolean recyclerSlot1Active = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot1 && 
		                              net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot1SeparateOverlay;
		boolean recyclerSlot2Active = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot2 && 
		                              net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot2SeparateOverlay;
		boolean recyclerSlot3Active = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot3 && 
		                              net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot3SeparateOverlay;
		
		if (recyclerSlot1Active || recyclerSlot2Active || recyclerSlot3Active) {
			// Prüfe welche Slots einzeln gerendert werden sollen
			boolean slot1Separate = recyclerSlot1Active && net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot1Separate;
			boolean slot2Separate = recyclerSlot2Active && net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot2Separate;
			boolean slot3Separate = recyclerSlot3Active && net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot3Separate;
			
			// Liste für Slots, die zusammen gerendert werden sollen
			List<LineWithPercent> recyclerLines = new ArrayList<>();
			
			// Recycler Slot 1
			if (recyclerSlot1Active) {
				String percent = null;
				boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerPercent;
				if (showPercent && recyclerSlot1.isValid()) {
					percent = formatPercent(recyclerSlot1.current, recyclerSlot1.max);
				}
				double currentPercent = recyclerSlot1.isValid() ? 
					((double)recyclerSlot1.current / (double)recyclerSlot1.max) * 100.0 : 0;
				double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerWarnPercent;
				// Recycler zählt runter, daher warnen wenn currentPercent <= warnPercent
				boolean showWarning = warnPercent >= 0 && currentPercent <= warnPercent;
				boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot1ShowIcon;
				// Wenn Icon aktiviert ist, zeige nur die Werte (oder "Nicht im Tab-Widget" wenn nicht gültig)
				// Das Icon wird dann automatisch vor dem Text angezeigt
				String displayText = showIcon ? recyclerSlot1.getDisplayString() : "Recycler Slot 1: " + recyclerSlot1.getDisplayString();
				
				if (slot1Separate) {
					// Rendere einzeln mit individueller Position
					renderSingleInfoOverlay(context, client, displayText, 
						percent, showPercent, showWarning, "recyclerSlot1", showIcon,
						net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot1X,
						net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot1Y,
						net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerShowBackground);
				} else {
					// Füge zur Liste hinzu (wird zusammen gerendert)
					recyclerLines.add(new LineWithPercent(displayText, percent, showPercent, showWarning, "recyclerSlot1", showIcon));
				}
			}
			
			// Recycler Slot 2
			if (recyclerSlot2Active) {
				String percent = null;
				boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerPercent;
				if (showPercent && recyclerSlot2.isValid()) {
					percent = formatPercent(recyclerSlot2.current, recyclerSlot2.max);
				}
				double currentPercent = recyclerSlot2.isValid() ? 
					((double)recyclerSlot2.current / (double)recyclerSlot2.max) * 100.0 : 0;
				double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerWarnPercent;
				// Recycler zählt runter, daher warnen wenn currentPercent <= warnPercent
				boolean showWarning = warnPercent >= 0 && currentPercent <= warnPercent;
				boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot2ShowIcon;
				String displayText = showIcon ? recyclerSlot2.getDisplayString() : "Recycler Slot 2: " + recyclerSlot2.getDisplayString();
				
				if (slot2Separate) {
					// Rendere einzeln mit individueller Position
					renderSingleInfoOverlay(context, client, displayText, 
						percent, showPercent, showWarning, "recyclerSlot2", showIcon,
						net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot2X,
						net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot2Y,
						net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerShowBackground);
				} else {
					// Füge zur Liste hinzu (wird zusammen gerendert)
					recyclerLines.add(new LineWithPercent(displayText, percent, showPercent, showWarning, "recyclerSlot2", showIcon));
				}
			}
			
			// Recycler Slot 3
			if (recyclerSlot3Active) {
				String percent = null;
				boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerPercent;
				if (showPercent && recyclerSlot3.isValid()) {
					percent = formatPercent(recyclerSlot3.current, recyclerSlot3.max);
				}
				double currentPercent = recyclerSlot3.isValid() ? 
					((double)recyclerSlot3.current / (double)recyclerSlot3.max) * 100.0 : 0;
				double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerWarnPercent;
				// Recycler zählt runter, daher warnen wenn currentPercent <= warnPercent
				boolean showWarning = warnPercent >= 0 && currentPercent <= warnPercent;
				boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot3ShowIcon;
				String displayText = showIcon ? recyclerSlot3.getDisplayString() : "Recycler Slot 3: " + recyclerSlot3.getDisplayString();
				
				if (slot3Separate) {
					// Rendere einzeln mit individueller Position
					renderSingleInfoOverlay(context, client, displayText, 
						percent, showPercent, showWarning, "recyclerSlot3", showIcon,
						net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot3X,
						net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot3Y,
						net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerShowBackground);
				} else {
					// Füge zur Liste hinzu (wird zusammen gerendert)
					recyclerLines.add(new LineWithPercent(displayText, percent, showPercent, showWarning, "recyclerSlot3", showIcon));
				}
			}
			
			// Rendere alle Zeilen, die zusammen gerendert werden sollen, in einem einzigen Overlay
			if (!recyclerLines.isEmpty()) {
				// Verwende die gemeinsame Position für das Multi-Line-Overlay
				int baseX = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerX;
				int baseY = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerY;
				boolean showBackground = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerShowBackground;
				
				renderMultiLineOverlay(context, client, recyclerLines,
					baseX,
					baseY,
					showBackground,
					"recycler"); // Verwende "recycler" als configKey für Farben
			}
		}
	}

	/**
	 * Rendert ein Multi-Line-Overlay (für Machtkristalle, die alle 3 Slots in einem Overlay anzeigen)
	 */
	private static void renderMultiLineOverlay(GuiGraphicsExtractor context, Minecraft client,
		List<LineWithPercent> lines, int xPosition, int yPosition, boolean showBackground, String configKey) {
		if (lines.isEmpty()) {
			return;
		}
		
		final int PADDING = 5;
		final int LINE_HEIGHT = client.font.lineHeight + 2;
		
		// Berechne die maximale Breite des Textes
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
				width += client.font.width(" " + line.percentText);
			}
			if (width > maxWidth) {
				maxWidth = width;
			}
		}
		
		// Berechne die tatsächliche Zeilenhöhe
		int actualLineHeight = LINE_HEIGHT;
		// Berücksichtige Icons bei der Höhenberechnung
		for (LineWithPercent line : lines) {
			if (line.showIcon && line.configKey != null) {
				int iconSize = (int)(client.font.lineHeight * 1.5);
				actualLineHeight = Math.max(actualLineHeight, iconSize);
			}
		}
		
		// Berechne unskalierte Overlay-Dimensionen komplett dynamisch basierend auf Inhalt (wie BossHP-Overlay)
		int unscaledWidth = maxWidth + (PADDING * 2);
		int unscaledHeight = (lines.size() * actualLineHeight) + (PADDING * 2);
		
		// Get scale
		float scale = getSeparateOverlayScale(configKey);
		if (scale <= 0) scale = 1.0f;
		
		// Berechne skalierte Dimensionen
		int overlayWidth = Math.round(unscaledWidth * scale);
		int overlayHeight = Math.round(unscaledHeight * scale);
		
		// Position aus Config: baseX ist die linke Kante (wie beim Mining-Overlay)
		int baseX = xPosition;
		
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
		
		// Zeichne Hintergrund (wenn aktiviert) - skaliert
		if (showBackground) {
			context.fill(posX, posY, posX + overlayWidth, posY + overlayHeight, 0x80000000);
		}
		
		// Render content with scale using matrix transformation
		Matrix3x2fStack matrices = context.pose();
		matrices.pushMatrix();
		matrices.translate(posX, posY);
		matrices.scale(scale, scale);
		
		// Zeichne alle Zeilen
		int currentY = PADDING;
		int warningColor = 0xFFFF0000; // Rot für Warnungen
		for (LineWithPercent line : lines) {
			if (line.text == null || line.text.trim().isEmpty()) {
				currentY += actualLineHeight;
				continue;
			}
			
			// Hole konfigurierte Farben für diese Zeile (basierend auf line.configKey, nicht configKey)
			// Wenn line.configKey null ist, verwende den übergebenen configKey als Fallback
			String colorConfigKey = (line.configKey != null && !line.configKey.isEmpty()) ? line.configKey : configKey;
			int textColor = getTextColorForConfigKey(colorConfigKey);
			int percentColor = getPercentColorForConfigKey(colorConfigKey);
			
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
			
			int currentX = PADDING;
			
			// Zeichne Icon statt Text, wenn aktiviert (für Forschung, Amboss, Schmelzofen, Seelen, Essenzen, Jäger, Machtkristalle und Recycler)
			if (line.showIcon && (line.configKey != null && ("forschung".equals(line.configKey) || "amboss".equals(line.configKey) || 
			                                        "schmelzofen".equals(line.configKey) || "seelen".equals(line.configKey) || 
			                                        "essenzen".equals(line.configKey) || "jaeger".equals(line.configKey) || "komboKiste".equals(line.configKey) || 
			                                        "machtkristalle".equals(line.configKey) ||
			                                        "recyclerSlot1".equals(line.configKey) || "recyclerSlot2".equals(line.configKey) || 
			                                        "recyclerSlot3".equals(line.configKey)))) {
				int iconSize = (int)(client.font.lineHeight * 1.5);
				// Zentriere Icon vertikal: Zeilen-Mitte minus die Hälfte der Icon-Höhe
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
						// Zentriere Text vertikal zum Icon: Icon-Mitte minus die Hälfte der Text-Höhe
						textYForIcon = lineCenterY - client.font.lineHeight / 2;
					} catch (Exception e) {
						// Fallback: Zeichne Text wenn Icon nicht geladen werden kann
						if (fallbackText != null) {
							context.text(client.font, Component.literal(fallbackText), currentX, currentY, currentTextColor, true);
							currentX += client.font.width(fallbackText);
						}
					}
					// Zeichne Doppelpunkt nach dem Icon (vertikal zentriert zum Icon)
					context.text(client.font, Component.literal(": "), currentX, textYForIcon, currentTextColor, true);
					currentX += client.font.width(": ");
					// Zeichne die Werte nach dem Doppelpunkt (vertikal zentriert zum Icon)
					context.text(client.font, Component.literal(line.text), currentX, textYForIcon, currentTextColor, true);
					currentX += client.font.width(line.text);
				} else {
					// Fallback: Zeichne normalen Text
					context.text(client.font, Component.literal(line.text), currentX, currentY, currentTextColor, true);
					currentX += client.font.width(line.text);
				}
			} else {
				// Zeichne Text ohne Icon
				context.text(
					client.font,
					line.text,
					currentX,
					currentY,
					currentTextColor,
					true
				);
				currentX += client.font.width(line.text);
			}
			
			// Zeichne Prozente, wenn vorhanden
			if (line.showPercent && line.percentText != null) {
				int percentY = currentY;
				// Wenn Icon aktiviert ist, zentriere Prozente vertikal zum Icon
				if (line.showIcon && line.configKey != null) {
					int lineCenterY = currentY + actualLineHeight / 2;
					percentY = lineCenterY - client.font.lineHeight / 2;
				}
				// Wenn Warnung aktiv ist, blinkt auch der Prozentwert rot
				int currentPercentColor = line.showWarning ? currentTextColor : percentColor;
				context.text(
					client.font,
					" " + line.percentText,
					currentX,
					percentY,
					currentPercentColor,
					true
				);
			}
			
			currentY += actualLineHeight;
		}
		
		matrices.popMatrix();
	}
}
