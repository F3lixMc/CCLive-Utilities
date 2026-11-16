package net.felix.utilities.Aincraft.npcalerts.overlay;

import java.util.ArrayList;
import java.util.List;
import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.Aincraft.npcalerts.data.CapacityData;
import net.felix.utilities.Aincraft.npcalerts.data.MachtkristallSlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

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
import static net.felix.utilities.Aincraft.npcalerts.overlay.MultiLineOverlayRenderer.renderMultiLineOverlay;
import static net.felix.utilities.Aincraft.npcalerts.overlay.PercentFormatter.formatPercent;
import static net.felix.utilities.Aincraft.npcalerts.overlay.SeparateOverlayContent.machtkristallDisplayText;
import static net.felix.utilities.Aincraft.npcalerts.overlay.SingleInfoOverlayRenderer.renderSingleInfoOverlay;
import static net.felix.utilities.Aincraft.npcalerts.parser.KomboKisteReader.getKomboKisteFractionDisplay;

/**
 * Zeichnet die getrennten Einzel-Overlays pro NPC-Alert.
 */
public final class SeparateOverlayRenderer {

	private static final int MACHTKRISTALL_SLOT_COUNT = 3;

	private SeparateOverlayRenderer() {
	}

	/**
	 * Rendert separate Overlays für einzelne Informationen
	 */
	static void renderSeparateOverlays(GuiGraphicsExtractor context, Minecraft client) {
		renderForschung(context, client);
		renderAmboss(context, client);
		renderSchmelzofen(context, client);
		renderJaeger(context, client);
		renderSeelen(context, client);
		renderEssenzen(context, client);
		renderKomboKiste(context, client);
		renderMachtkristalle(context, client);
		renderRecycler(context, client);
	}

	private static void renderForschung(GuiGraphicsExtractor context, Minecraft client) {
		CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
		if (!(config.showNpcAlertsForschung && config.npcAlertsForschungSeparateOverlay)) {
			return;
		}

		String percent = null;
		boolean showPercent = config.showNpcAlertsForschungPercent;
		if (showPercent) {
			if (forschung.isValid()) {
				// Forschung zählt runter: wenn current näher an max ist, ist der Prozent höher
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
		// Warnung: wenn verbleibender Wert <= Schwelle (0-23)
		boolean showWarning = shouldShowForschungWarning(config.npcAlertsForschungWarnValue);
		boolean showIcon = config.npcAlertsForschungShowIcon;
		String displayText = showIcon ? forschung.getDisplayString() : "Forschung: " + forschung.getDisplayString();
		renderSingleInfoOverlay(context, client, displayText,
				percent, showPercent, showWarning, "forschung", showIcon,
				config.npcAlertsForschungX, config.npcAlertsForschungY, config.npcAlertsForschungShowBackground);
	}

	private static void renderAmboss(GuiGraphicsExtractor context, Minecraft client) {
		CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
		if (!(config.showNpcAlertsAmboss && config.npcAlertsAmbossSeparateOverlay)) {
			return;
		}
		renderCapacityOverlay(context, client, ambossCapacity, ambossCapacity.getDisplayString(), "Amboss: ", "amboss",
				config.showNpcAlertsAmbossPercent, config.npcAlertsAmbossWarnPercent, config.npcAlertsAmbossShowIcon,
				config.npcAlertsAmbossX, config.npcAlertsAmbossY, config.npcAlertsAmbossShowBackground);
	}

	private static void renderSchmelzofen(GuiGraphicsExtractor context, Minecraft client) {
		CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
		if (!(config.showNpcAlertsSchmelzofen && config.npcAlertsSchmelzofenSeparateOverlay)) {
			return;
		}
		renderCapacityOverlay(context, client, schmelzofenCapacity, schmelzofenCapacity.getDisplayString(), "Schmelzofen: ", "schmelzofen",
				config.showNpcAlertsSchmelzofenPercent, config.npcAlertsSchmelzofenWarnPercent, config.npcAlertsSchmelzofenShowIcon,
				config.npcAlertsSchmelzofenX, config.npcAlertsSchmelzofenY, config.npcAlertsSchmelzofenShowBackground);
	}

	private static void renderJaeger(GuiGraphicsExtractor context, Minecraft client) {
		CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
		if (!(config.showNpcAlertsJaeger && config.npcAlertsJaegerSeparateOverlay)) {
			return;
		}
		renderCapacityOverlay(context, client, jaegerCapacity, jaegerCapacity.getDisplayStringWithoutCurrentSuffix(), "Jäger: ", "jaeger",
				config.showNpcAlertsJaegerPercent, config.npcAlertsJaegerWarnPercent, config.npcAlertsJaegerShowIcon,
				config.npcAlertsJaegerX, config.npcAlertsJaegerY, config.npcAlertsJaegerShowBackground);
	}

	private static void renderSeelen(GuiGraphicsExtractor context, Minecraft client) {
		CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
		if (!(config.showNpcAlertsSeelen && config.npcAlertsSeelenSeparateOverlay)) {
			return;
		}
		renderCapacityOverlay(context, client, seelenCapacity, seelenCapacity.getDisplayString(), "Seelen: ", "seelen",
				config.showNpcAlertsSeelenPercent, config.npcAlertsSeelenWarnPercent, config.npcAlertsSeelenShowIcon,
				config.npcAlertsSeelenX, config.npcAlertsSeelenY, config.npcAlertsSeelenShowBackground);
	}

	private static void renderEssenzen(GuiGraphicsExtractor context, Minecraft client) {
		CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
		if (!(config.showNpcAlertsEssenzen && config.npcAlertsEssenzenSeparateOverlay)) {
			return;
		}
		renderCapacityOverlay(context, client, essenzenCapacity, essenzenCapacity.getDisplayString(), "Essenzen: ", "essenzen",
				config.showNpcAlertsEssenzenPercent, config.npcAlertsEssenzenWarnPercent, config.npcAlertsEssenzenShowIcon,
				config.npcAlertsEssenzenX, config.npcAlertsEssenzenY, config.npcAlertsEssenzenShowBackground);
	}

	/**
	 * Gemeinsame Darstellung der Füllstand-Alerts (Amboss, Schmelzofen, Jäger, Seelen, Essenzen):
	 * Prozentwert und Warnung, sobald der Füllstand die Warnschwelle erreicht.
	 */
	private static void renderCapacityOverlay(GuiGraphicsExtractor context, Minecraft client, CapacityData capacity,
			String valueText, String label, String configKey, boolean showPercent, double warnPercent,
			boolean showIcon, int x, int y, boolean showBackground) {
		String percent = null;
		if (showPercent && capacity.isValid()) {
			percent = formatPercent(capacity.current, capacity.max);
		}
		double currentPercent = capacity.isValid()
				? ((double) capacity.current / (double) capacity.max) * 100.0
				: 0;
		boolean showWarning = warnPercent >= 0 && currentPercent >= warnPercent;
		String displayText = showIcon ? valueText : label + valueText;
		renderSingleInfoOverlay(context, client, displayText,
				percent, showPercent, showWarning, configKey, showIcon,
				x, y, showBackground);
	}

	private static void renderKomboKiste(GuiGraphicsExtractor context, Minecraft client) {
		CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
		if (!(config.showNpcAlertsKomboKiste && config.npcAlertsKomboKisteSeparateOverlay)) {
			return;
		}
		boolean showWarning = komboKiste.isValid() && komboKiste.current >= komboKiste.max;
		boolean showIcon = config.npcAlertsKomboKisteShowIcon;
		String fraction = getKomboKisteFractionDisplay();
		String displayText = showIcon ? fraction : "Kombo Kiste: " + fraction;
		renderSingleInfoOverlay(context, client, displayText,
				null, false, showWarning, "komboKiste", showIcon,
				config.npcAlertsKomboKisteX, config.npcAlertsKomboKisteY, config.npcAlertsKomboKisteShowBackground);
	}

	/**
	 * Machtkristalle: Slots mit eigener Position werden einzeln gezeichnet,
	 * alle anderen zusammen in einem Multi-Line-Overlay.
	 */
	private static void renderMachtkristalle(GuiGraphicsExtractor context, Minecraft client) {
		CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
		if (!(config.showNpcAlertsMachtkristalle && config.npcAlertsMachtkristalleSeparateOverlay)) {
			return;
		}

		boolean showIcon = config.npcAlertsMachtkristalleShowIcon;
		List<LineWithPercent> sharedLines = new ArrayList<>();

		for (int i = 0; i < MACHTKRISTALL_SLOT_COUNT; i++) {
			if (!isMachtkristallSlotEnabled(config, i)) {
				continue;
			}

			MachtkristallSlot slot = machtkristallSlots[i];
			String displayText = machtkristallDisplayText(slot, i, showIcon);
			String percentText = (slot.isNotFound() || slot.isEmpty()) ? null : slot.getPercentText();
			// Warnung: wenn Prozent >= dem Warnwert ist (wie bei Amboss)
			boolean showWarning = getMachtkristallShowWarning(slot, config.npcAlertsMachtkristalleWarnPercent);

			if (isMachtkristallSlotSeparate(config, i)) {
				renderSingleInfoOverlay(context, client,
						displayText,
						percentText, percentText != null, showWarning, "machtkristalleSlot" + (i + 1), showIcon,
						machtkristallSlotX(config, i), machtkristallSlotY(config, i),
						config.npcAlertsMachtkristalleShowBackground);
			} else {
				sharedLines.add(new LineWithPercent(displayText, percentText, percentText != null, showWarning, "machtkristalle", showIcon));
			}
		}

		if (!sharedLines.isEmpty()) {
			renderMultiLineOverlay(context, client, sharedLines,
					config.npcAlertsMachtkristalleX,
					config.npcAlertsMachtkristalleY,
					config.npcAlertsMachtkristalleShowBackground,
					"machtkristalle");
		}
	}

	private static boolean isMachtkristallSlotEnabled(CCLiveUtilitiesConfig config, int index) {
		return switch (index) {
			case 0 -> config.showNpcAlertsMachtkristalleSlot1;
			case 1 -> config.showNpcAlertsMachtkristalleSlot2;
			case 2 -> config.showNpcAlertsMachtkristalleSlot3;
			default -> true;
		};
	}

	private static boolean isMachtkristallSlotSeparate(CCLiveUtilitiesConfig config, int index) {
		return switch (index) {
			case 0 -> config.npcAlertsMachtkristalleSlot1Separate;
			case 1 -> config.npcAlertsMachtkristalleSlot2Separate;
			case 2 -> config.npcAlertsMachtkristalleSlot3Separate;
			default -> false;
		};
	}

	private static int machtkristallSlotX(CCLiveUtilitiesConfig config, int index) {
		return switch (index) {
			case 0 -> config.npcAlertsMachtkristalleSlot1X;
			case 1 -> config.npcAlertsMachtkristalleSlot2X;
			case 2 -> config.npcAlertsMachtkristalleSlot3X;
			default -> config.npcAlertsMachtkristalleX;
		};
	}

	private static int machtkristallSlotY(CCLiveUtilitiesConfig config, int index) {
		return switch (index) {
			case 0 -> config.npcAlertsMachtkristalleSlot1Y;
			case 1 -> config.npcAlertsMachtkristalleSlot2Y;
			case 2 -> config.npcAlertsMachtkristalleSlot3Y;
			default -> config.npcAlertsMachtkristalleY;
		};
	}

	/**
	 * Recycler: Slots mit "Separates Overlay" werden einzeln (eigene Position) oder zusammen
	 * in einem Multi-Line-Overlay gezeichnet.
	 */
	private static void renderRecycler(GuiGraphicsExtractor context, Minecraft client) {
		CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
		boolean slot1Active = config.showNpcAlertsRecyclerSlot1 && config.npcAlertsRecyclerSlot1SeparateOverlay;
		boolean slot2Active = config.showNpcAlertsRecyclerSlot2 && config.npcAlertsRecyclerSlot2SeparateOverlay;
		boolean slot3Active = config.showNpcAlertsRecyclerSlot3 && config.npcAlertsRecyclerSlot3SeparateOverlay;
		if (!(slot1Active || slot2Active || slot3Active)) {
			return;
		}

		List<LineWithPercent> sharedLines = new ArrayList<>();
		if (slot1Active) {
			renderOrCollectRecyclerSlot(context, client, recyclerSlot1, "recyclerSlot1", "Recycler Slot 1: ",
					config.npcAlertsRecyclerSlot1ShowIcon, config.npcAlertsRecyclerSlot1Separate,
					config.npcAlertsRecyclerSlot1X, config.npcAlertsRecyclerSlot1Y, sharedLines);
		}
		if (slot2Active) {
			renderOrCollectRecyclerSlot(context, client, recyclerSlot2, "recyclerSlot2", "Recycler Slot 2: ",
					config.npcAlertsRecyclerSlot2ShowIcon, config.npcAlertsRecyclerSlot2Separate,
					config.npcAlertsRecyclerSlot2X, config.npcAlertsRecyclerSlot2Y, sharedLines);
		}
		if (slot3Active) {
			renderOrCollectRecyclerSlot(context, client, recyclerSlot3, "recyclerSlot3", "Recycler Slot 3: ",
					config.npcAlertsRecyclerSlot3ShowIcon, config.npcAlertsRecyclerSlot3Separate,
					config.npcAlertsRecyclerSlot3X, config.npcAlertsRecyclerSlot3Y, sharedLines);
		}

		if (!sharedLines.isEmpty()) {
			// Gemeinsame Position; "recycler" ist der configKey für die Farben
			renderMultiLineOverlay(context, client, sharedLines,
					config.npcAlertsRecyclerX,
					config.npcAlertsRecyclerY,
					config.npcAlertsRecyclerShowBackground,
					"recycler");
		}
	}

	/** Zeichnet einen Recycler-Slot einzeln oder hängt ihn an die gemeinsame Zeilenliste an. */
	private static void renderOrCollectRecyclerSlot(GuiGraphicsExtractor context, Minecraft client, CapacityData slot,
			String configKey, String label, boolean showIcon, boolean separate, int x, int y,
			List<LineWithPercent> sharedLines) {
		CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();

		boolean showPercent = config.showNpcAlertsRecyclerPercent;
		String percent = null;
		if (showPercent && slot.isValid()) {
			percent = formatPercent(slot.current, slot.max);
		}
		double currentPercent = slot.isValid()
				? ((double) slot.current / (double) slot.max) * 100.0
				: 0;
		double warnPercent = config.npcAlertsRecyclerWarnPercent;
		// Recycler zählt runter, daher warnen wenn currentPercent <= warnPercent
		boolean showWarning = warnPercent >= 0 && currentPercent <= warnPercent;
		// Mit Icon nur die Werte; das Icon wird automatisch vor dem Text gezeichnet
		String displayText = showIcon ? slot.getDisplayString() : label + slot.getDisplayString();

		if (separate) {
			renderSingleInfoOverlay(context, client, displayText,
					percent, showPercent, showWarning, configKey, showIcon,
					x, y, config.npcAlertsRecyclerShowBackground);
		} else {
			sharedLines.add(new LineWithPercent(displayText, percent, showPercent, showWarning, configKey, showIcon));
		}
	}
}
