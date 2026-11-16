package net.felix.utilities.Overall.npcalerts.message;

import java.util.ArrayList;
import java.util.List;
import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.Overall.npcalerts.data.CapacityData;
import net.felix.utilities.Overall.npcalerts.data.MachtkristallSlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.joml.Matrix3x2fStack;

import static net.felix.utilities.Overall.npcalerts.data.CapacityStatus.isDepleted;
import static net.felix.utilities.Overall.npcalerts.data.CapacityStatus.isFull;
import static net.felix.utilities.Overall.npcalerts.data.CapacityStatus.percentOf;
import static net.felix.utilities.Overall.npcalerts.data.NpcAlertState.ambossCapacity;
import static net.felix.utilities.Overall.npcalerts.data.NpcAlertState.essenzenCapacity;
import static net.felix.utilities.Overall.npcalerts.data.NpcAlertState.forschung;
import static net.felix.utilities.Overall.npcalerts.data.NpcAlertState.jaegerCapacity;
import static net.felix.utilities.Overall.npcalerts.data.NpcAlertState.komboKiste;
import static net.felix.utilities.Overall.npcalerts.data.NpcAlertState.machtkristallSlots;
import static net.felix.utilities.Overall.npcalerts.data.NpcAlertState.recyclerSlot1;
import static net.felix.utilities.Overall.npcalerts.data.NpcAlertState.recyclerSlot2;
import static net.felix.utilities.Overall.npcalerts.data.NpcAlertState.recyclerSlot3;
import static net.felix.utilities.Overall.npcalerts.data.NpcAlertState.schmelzofenCapacity;
import static net.felix.utilities.Overall.npcalerts.data.NpcAlertState.seelenCapacity;
import static net.felix.utilities.Overall.npcalerts.overlay.AlertSettings.isNpcAlertCollectorEnabled;
import static net.felix.utilities.Overall.npcalerts.overlay.AlertSettings.isNpcAlertMachtkristallSlotEnabled;
import static net.felix.utilities.Overall.npcalerts.overlay.AlertSettings.isNpcAlertRecyclerSlotEnabled;

/**
 * Warn-Meldungen auf dem Bildschirm (Kapazitaet voll/leer, Forschung, Machtkristall) berechnen und rendern.
 */
public final class ScreenMessages {

	private ScreenMessages() {
	}

	private static final List<String> activeScreenMessages = new ArrayList<>();

	private static final int SCREEN_MESSAGE_COLOR = 0xFFFF5555;

	private static boolean shouldShowHighCapacityScreenMessage(CapacityData data, double warnPercent) {
		if (!data.isValid()) {
			return false;
		}
		double currentPercent = percentOf(data);
		return isFull(data) || (warnPercent >= 0 && currentPercent >= warnPercent);
	}

	private static boolean shouldShowLowCapacityScreenMessage(CapacityData data, double warnPercent) {
		if (!data.isValid()) {
			return false;
		}
		double currentPercent = percentOf(data);
		return isDepleted(data) || (warnPercent >= 0 && currentPercent <= warnPercent);
	}

	public static boolean shouldShowForschungWarning(int warnValue) {
		if (!forschung.isValid() || warnValue < 0) {
			return false;
		}
		return forschung.current <= warnValue;
	}

	private static boolean shouldShowMachtkristallScreenMessage(MachtkristallSlot slot, double warnPercent) {
		return getMachtkristallShowWarning(slot, warnPercent);
	}

	public static boolean shouldShowMachtkristallWarning(MachtkristallSlot slot) {
		return getMachtkristallShowWarning(
			slot,
			CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleWarnPercent
		);
	}

	public static boolean getMachtkristallShowWarning(MachtkristallSlot slot, double warnPercent) {
		if (slot.isEmpty() || slot.isNotFound() || !slot.xpData.isValid()) {
			return false;
		}
		double currentPercent = slot.getDisplayPercentValue();
		return currentPercent >= 0 && warnPercent >= 0 && currentPercent >= warnPercent;
	}

	private static boolean isScreenMessageEnabled(String configKey) {
		CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
		return switch (configKey) {
			case "forschung" -> config.npcAlertsForschungScreenMessage;
			case "amboss" -> config.npcAlertsAmbossScreenMessage;
			case "schmelzofen" -> config.npcAlertsSchmelzofenScreenMessage;
			case "jaeger" -> config.npcAlertsJaegerScreenMessage;
			case "seelen" -> config.npcAlertsSeelenScreenMessage;
			case "essenzen" -> config.npcAlertsEssenzenScreenMessage;
			case "komboKiste" -> config.npcAlertsKomboKisteScreenMessage;
			case "machtkristalle" -> config.npcAlertsMachtkristalleScreenMessage;
			case "recycler", "recyclerSlot1", "recyclerSlot2", "recyclerSlot3" -> config.npcAlertsRecyclerScreenMessage;
			default -> false;
		};
	}

	private static boolean isScreenMessageWhenOverlayDisabledEnabled(String configKey) {
		CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
		return switch (configKey) {
			case "forschung" -> config.npcAlertsForschungScreenMessageWhenOverlayDisabled;
			case "amboss" -> config.npcAlertsAmbossScreenMessageWhenOverlayDisabled;
			case "schmelzofen" -> config.npcAlertsSchmelzofenScreenMessageWhenOverlayDisabled;
			case "jaeger" -> config.npcAlertsJaegerScreenMessageWhenOverlayDisabled;
			case "seelen" -> config.npcAlertsSeelenScreenMessageWhenOverlayDisabled;
			case "essenzen" -> config.npcAlertsEssenzenScreenMessageWhenOverlayDisabled;
			case "komboKiste" -> config.npcAlertsKomboKisteScreenMessageWhenOverlayDisabled;
			case "machtkristalle" -> config.npcAlertsMachtkristalleScreenMessageWhenOverlayDisabled;
			case "recycler", "recyclerSlot1", "recyclerSlot2", "recyclerSlot3" -> config.npcAlertsRecyclerScreenMessageWhenOverlayDisabled;
			default -> false;
		};
	}

	private static boolean shouldMonitorAlertForScreenMessage(String configKey) {
		if (isNpcAlertCollectorEnabled(configKey)) {
			return true;
		}
		return isScreenMessageWhenOverlayDisabledEnabled(configKey);
	}

	private static boolean shouldMonitorMachtkristallSlotForScreenMessage(int slotIndex) {
		if (isNpcAlertMachtkristallSlotEnabled(slotIndex)) {
			return true;
		}
		return isScreenMessageWhenOverlayDisabledEnabled("machtkristalle");
	}

	private static boolean shouldMonitorRecyclerSlotForScreenMessage(int slotIndex) {
		if (isNpcAlertRecyclerSlotEnabled(slotIndex)) {
			return true;
		}
		return isScreenMessageWhenOverlayDisabledEnabled("recycler");
	}

	private static boolean shouldShowScreenMessageForAlert(String configKey) {
		if (isNpcAlertCollectorEnabled(configKey)) {
			return isScreenMessageEnabled(configKey);
		}
		return isScreenMessageWhenOverlayDisabledEnabled(configKey);
	}

	private static void addScreenMessageIfEnabled(String configKey, boolean condition, String message) {
		if (condition && shouldShowScreenMessageForAlert(configKey)) {
			activeScreenMessages.add(message);
		}
	}

	public static void updateScreenMessages() {
		activeScreenMessages.clear();
		CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
		if (!config.npcAlertsUtilityEnabled) {
			return;
		}
		
		if (shouldMonitorAlertForScreenMessage("forschung")) {
			addScreenMessageIfEnabled("forschung",
				shouldShowForschungWarning(config.npcAlertsForschungWarnValue),
				"Forschungen sind leer");
		}
		if (shouldMonitorAlertForScreenMessage("amboss")) {
			addScreenMessageIfEnabled("amboss",
				shouldShowHighCapacityScreenMessage(ambossCapacity, config.npcAlertsAmbossWarnPercent),
				"Amboss ist voll");
		}
		if (shouldMonitorAlertForScreenMessage("schmelzofen")) {
			addScreenMessageIfEnabled("schmelzofen",
				shouldShowHighCapacityScreenMessage(schmelzofenCapacity, config.npcAlertsSchmelzofenWarnPercent),
				"Schmelzofen ist voll");
		}
		if (shouldMonitorAlertForScreenMessage("jaeger")) {
			addScreenMessageIfEnabled("jaeger",
				shouldShowHighCapacityScreenMessage(jaegerCapacity, config.npcAlertsJaegerWarnPercent),
				"Jäger ist voll");
		}
		if (shouldMonitorAlertForScreenMessage("seelen")) {
			addScreenMessageIfEnabled("seelen",
				shouldShowHighCapacityScreenMessage(seelenCapacity, config.npcAlertsSeelenWarnPercent),
				"Seelen sind voll");
		}
		if (shouldMonitorAlertForScreenMessage("essenzen")) {
			addScreenMessageIfEnabled("essenzen",
				shouldShowHighCapacityScreenMessage(essenzenCapacity, config.npcAlertsEssenzenWarnPercent),
				"Essenzen sind voll");
		}
		if (shouldMonitorAlertForScreenMessage("komboKiste")) {
			addScreenMessageIfEnabled("komboKiste",
				komboKiste.isValid() && komboKiste.current >= komboKiste.max,
				"Kombo Kiste ist voll");
		}
		if (shouldMonitorAlertForScreenMessage("machtkristalle")) {
			for (int i = 0; i < 3; i++) {
				if (shouldMonitorMachtkristallSlotForScreenMessage(i)
					&& shouldShowMachtkristallScreenMessage(machtkristallSlots[i], config.npcAlertsMachtkristalleWarnPercent)) {
					addScreenMessageIfEnabled("machtkristalle", true, "Machtkristall ist voll");
					break;
				}
			}
		}
		if (shouldMonitorRecyclerSlotForScreenMessage(0)) {
			addScreenMessageIfEnabled("recycler",
				shouldShowLowCapacityScreenMessage(recyclerSlot1, config.npcAlertsRecyclerWarnPercent),
				"Recycler Slot 1 ist leer");
		}
		if (shouldMonitorRecyclerSlotForScreenMessage(1)) {
			addScreenMessageIfEnabled("recycler",
				shouldShowLowCapacityScreenMessage(recyclerSlot2, config.npcAlertsRecyclerWarnPercent),
				"Recycler Slot 2 ist leer");
		}
		if (shouldMonitorRecyclerSlotForScreenMessage(2)) {
			addScreenMessageIfEnabled("recycler",
				shouldShowLowCapacityScreenMessage(recyclerSlot3, config.npcAlertsRecyclerWarnPercent),
				"Recycler Slot 3 ist leer");
		}
	}

	public static void renderScreenMessages(GuiGraphicsExtractor context, Minecraft client) {
		if (activeScreenMessages.isEmpty()) {
			return;
		}
		
		int screenWidth = client.getWindow().getGuiScaledWidth();
		int screenHeight = client.getWindow().getGuiScaledHeight();
		int baseY = screenHeight / 4;
		int lineSpacing = 24;
		int startY = baseY - ((activeScreenMessages.size() - 1) * lineSpacing) / 2;
		
		Matrix3x2fStack matrices = context.pose();
		for (int i = 0; i < activeScreenMessages.size(); i++) {
			String message = activeScreenMessages.get(i);
			matrices.pushMatrix();
			matrices.translate(screenWidth / 2f, startY + i * lineSpacing);
			matrices.scale(2.0f, 2.0f);
			int textWidth = client.font.width(message);
			context.text(client.font, message, -textWidth / 2, 0, SCREEN_MESSAGE_COLOR, true);
			matrices.popMatrix();
		}
	}
}
