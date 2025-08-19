package net.felix.utilities;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.client.render.RenderTickCounter;
import net.felix.CCLiveUtilitiesConfig;


import java.util.List;

public class MaterialTrackerUtility {
	
	
	private static boolean isInitialized = false;
	private static boolean isTrackingMaterials = false;
	private static boolean showOverlays = true; // Neue Variable für Overlay-Sichtbarkeit
	
	// Hotkey variable
	private static KeyBinding toggleKeyBinding;

	
	// Rendering constants
	private static final int LINE_HEIGHT = 13; // 1 Pixel größer (12 + 1 = 13)
	private static final int OVERLAY_WIDTH = 128;
	private static final int OVERLAY_HEIGHT = 96;
	private static final int TEXT_PADDING = 15;
	private static final int MIN_TEXT_WIDTH = 100; // Minimale Breite für Text
	// No texture needed, using colored background instead
	
	public static void initialize() {
		if (isInitialized) {
			return;
		}
		
		try {
			// Register hotkey
			registerHotkey();
			
			// Client-seitige Events registrieren
			ClientTickEvents.END_CLIENT_TICK.register(MaterialTrackerUtility::onClientTick);
			
			// Registriere HUD-Rendering
			HudRenderCallback.EVENT.register((drawContext, tickDelta) -> onHudRender(drawContext, tickDelta));
			
			isInitialized = true;
		} catch (Exception e) {
			// Silent error handling
		}
	}
	
	private static void registerHotkey() {
		// Register toggle hotkey
		toggleKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.cclive-utilities.material-toggle",
			InputUtil.Type.KEYSYM,
			InputUtil.UNKNOWN_KEY.getCode(), // Unbound key
			"category.cclive-utilities.material"
		));
	}

	private static void onClientTick(MinecraftClient client) {
		// Check Tab key for overlay visibility
		checkTabKey();
		
		// Handle hotkey
		handleHotkey();
		
		// Prüfe Konfiguration
		if (!CCLiveUtilitiesConfig.HANDLER.instance().enableMod ||
			!CCLiveUtilitiesConfig.HANDLER.instance().materialTrackerEnabled || 
			!CCLiveUtilitiesConfig.HANDLER.instance().showMaterialTracker) {
			return;
		}
		
		if (client.player == null) {
			isTrackingMaterials = false;
			return;
		}

		// Check for dimension changes and reset if necessary
		ActionBarData.checkDimensionChange();

		// Check if we're on a floor and have materials
		boolean hasMaterials = ActionBarData.hasMaterials();
		if (hasMaterials != isTrackingMaterials) {
			isTrackingMaterials = hasMaterials;
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
	
	private static void handleHotkey() {
		// Handle toggle hotkey
		if (toggleKeyBinding != null && toggleKeyBinding.wasPressed()) {
			boolean currentShow = CCLiveUtilitiesConfig.HANDLER.instance().showMaterialTracker;
			CCLiveUtilitiesConfig.HANDLER.instance().showMaterialTracker = !currentShow;
			CCLiveUtilitiesConfig.HANDLER.save();
		}
	}
	
	private static void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
		// Prüfe Konfiguration
		if (!CCLiveUtilitiesConfig.HANDLER.instance().enableMod ||
			!CCLiveUtilitiesConfig.HANDLER.instance().materialTrackerEnabled || 
			!CCLiveUtilitiesConfig.HANDLER.instance().showMaterialTracker) {
			return;
		}
		
		if (!isTrackingMaterials) {
			return;
		}

		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null) {
			return;
		}

		// Render nur wenn Overlays sichtbar sind und keine Equipment-Overlays aktiv sind
		if (showOverlays && !EquipmentDisplayUtility.isEquipmentOverlayActive()) {
			renderMaterialDisplay(context, client);
		}
	}
	
	private static void renderMaterialDisplay(DrawContext context, MinecraftClient client) {
		if (client.getWindow() == null) {
			return;
		}
		
		int screenWidth = client.getWindow().getScaledWidth();
		
		// Position aus der Konfiguration
		int xOffset = CCLiveUtilitiesConfig.HANDLER.instance().materialTrackerX;
		int yOffset = CCLiveUtilitiesConfig.HANDLER.instance().materialTrackerY;
		
		// Berechne Position basierend auf Offsets vom rechten Rand
		int xPosition = screenWidth - OVERLAY_WIDTH - xOffset; // X-Offset vom rechten Rand
		int yPosition = yOffset; // Y-Offset vom oberen Rand
		
		// Get materials from ActionBarData
		List<Object> texts = ActionBarData.getFilteredTexts();
		
		// Calculate dynamic width based on text content
		int dynamicWidth = calculateRequiredWidth(context, texts);
		int actualOverlayWidth = Math.max(OVERLAY_WIDTH, dynamicWidth);
		
		// Adjust X position so overlay expands to the left (stays at right edge)
		int adjustedXPosition = xPosition - (actualOverlayWidth - OVERLAY_WIDTH);
		
		// Draw semi-transparent background (23 Pixel kleiner von unten) nur wenn aktiviert
		if (CCLiveUtilitiesConfig.HANDLER.instance().materialTrackerShowBackground) {
			context.fill(adjustedXPosition, yPosition, adjustedXPosition + actualOverlayWidth, yPosition + OVERLAY_HEIGHT - 23, 0x80000000);
		}
		
		int currentY = yPosition + TEXT_PADDING;
		
		for (Object textObj : texts) {
			
			Text textComponent;
			if (textObj instanceof net.minecraft.text.Text) {
				// Verwende das originale Text-Objekt mit Farbcodes
				textComponent = (net.minecraft.text.Text) textObj;
			} else {
				// Fallback für String-Objekte
				textComponent = Text.literal(textObj.toString());
			}
			
			// Draw text (simplified without scaling for now)
			context.drawText(
				MinecraftClient.getInstance().textRenderer, 
				textComponent, 
				adjustedXPosition + 2, // Verwende die angepasste X-Position
				currentY - 8, // 2 Pixel nach unten (-10 + 2 = -8)
				0xFFFFFFFF, // Vollständig weiß mit Alpha
				true // Mit Schatten
			);
			
			currentY += LINE_HEIGHT;
		}
	}
	
	private static int calculateRequiredWidth(DrawContext context, List<Object> texts) {
		int maxWidth = MIN_TEXT_WIDTH;
		
		for (Object textObj : texts) {
			Text textComponent;
			if (textObj instanceof net.minecraft.text.Text) {
				textComponent = (net.minecraft.text.Text) textObj;
			} else {
				textComponent = Text.literal(textObj.toString());
			}
			
			// Berechne die Breite des Textes
			int textWidth = MinecraftClient.getInstance().textRenderer.getWidth(textComponent);
			
			// Füge Padding hinzu (links und rechts)
			int totalWidth = textWidth + 15; // 10 Pixel links + 5 Pixel rechts (5 Pixel weniger)
			
			// Aktualisiere die maximale Breite
			maxWidth = Math.max(maxWidth, totalWidth);
		}
		
		return maxWidth;
	}
} 