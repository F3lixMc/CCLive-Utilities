package net.felix.utilities.Overall.npcalerts;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.Other.hud.HudOverlayVisibility;
import net.felix.utilities.Other.keybinding.KeyBindingUtility;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

import static net.felix.utilities.Overall.npcalerts.message.ScreenMessages.renderScreenMessages;
import static net.felix.utilities.Overall.npcalerts.message.ScreenMessages.updateScreenMessages;
import static net.felix.utilities.Overall.npcalerts.overlay.AlertSettings.areNpcAlertsOverlaysActive;
import static net.felix.utilities.Overall.npcalerts.overlay.MainOverlayRenderer.renderNpcAlertsDisplay;
import static net.felix.utilities.Overall.npcalerts.parser.TabListParser.updateNpcAlerts;

/**
 * Einstiegspunkt der NPC-Alerts: registriert Tick- und HUD-Callbacks, steuert die Sichtbarkeit der Overlays (Tab-Taste, Lobby, F1).
 */
public final class NpcAlertsFeature {

	private NpcAlertsFeature() {
	}

	private static boolean isInitialized = false;

	private static boolean showOverlays = true; // Overlay-Sichtbarkeit

	public static void initialize() {
		if (isInitialized) {
			return;
		}
		
		try {
			// Client-seitige Events registrieren
			ClientTickEvents.END_CLIENT_TICK.register(NpcAlertsFeature::onClientTick);
			
			// Registriere HUD-Rendering
			HudElementRegistry.addLast(
					Identifier.fromNamespaceAndPath("cclive-utilities", "npc_alerts"),
					NpcAlertsFeature::onHudRender);
			
			isInitialized = true;
		} catch (Exception e) {
			// Silent error handling
		}
	}

	private static void onClientTick(Minecraft client) {
		// Check Tab key for overlay visibility
		checkTabKey();
		
		if (client == null || client.player == null || client.level == null) {
			return;
		}
		
		try {
			updateNpcAlerts(client);
			updateScreenMessages();
		} catch (Exception e) {
			// Silent error handling
		}
	}

	private static void checkTabKey() {
		// Check if player list key is pressed (respects custom key bindings)
		if (KeyBindingUtility.isPlayerListKeyPressed()) {
			showOverlays = false; // Hide overlays when player list key is pressed
		} else {
			showOverlays = true; // Show overlays when player list key is released
		}
	}

	/**
	 * Prüft, ob der Spieler in der "general_lobby" Dimension ist
	 */
	public static boolean isInGeneralLobby() {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.level == null) {
			return false;
		}
		
		String dimensionPath = client.level.dimension().identifier().getPath();
		return dimensionPath.equals("general_lobby");
	}

	/**
	 * HUD Render callback für das NPC Alerts Overlay
	 */
	public static void onHudRender(GuiGraphicsExtractor context, DeltaTracker tickCounter) {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.player == null || client.level == null) {
			return;
		}
		if (!HudOverlayVisibility.shouldRenderWorldHudOverlays()) {
			return;
		}
		
		// Hide overlay if F1 menu (debug screen) is open
		if (client.gui.hud.isHidden()) {
			return;
		}
		
		// Hide overlay if in general_lobby dimension
		if (isInGeneralLobby()) {
			return;
		}
		
		// Hide overlay if Tab list is open (like other overlays)
		if (KeyBindingUtility.isPlayerListKeyPressed()) {
			return;
		}
		
		if (!showOverlays) {
			return;
		}
		
		CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
		if (config.npcAlertsUtilityEnabled) {
			renderScreenMessages(context, client);
		}
		if (areNpcAlertsOverlaysActive()) {
			renderNpcAlertsDisplay(context, client);
		}
	}
}
