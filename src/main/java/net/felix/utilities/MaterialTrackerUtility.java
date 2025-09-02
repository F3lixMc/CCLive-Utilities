package net.felix.utilities;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;
import net.minecraft.client.gl.RenderPipelines;
import net.felix.CCLiveUtilitiesConfig;
import net.felix.OverlayType;


import java.util.List;
import java.util.ArrayList;

public class MaterialTrackerUtility {
	
	
	private static boolean isInitialized = false;
	private static boolean isTrackingMaterials = false;
	private static boolean showOverlays = true; // Neue Variable für Overlay-Sichtbarkeit
	
	// Test overlay variables
	private static boolean showTestOverlay = false;
	private static String testText = "Prächtiges Eselhaar [1067]";
	private static final int TEST_LINES_COUNT = 5;
	
	// Hotkey variable
	private static KeyBinding toggleKeyBinding;

	
	// Rendering constants
	private static final int LINE_HEIGHT = 13; // 1 Pixel größer (12 + 1 = 13)
	private static final int OVERLAY_WIDTH = 155;
	private static final int OVERLAY_HEIGHT = 103;
	private static final int TEXT_PADDING = 20;
	private static final int MIN_TEXT_WIDTH = 100; // Minimale Breite für Text
	
	// Textur-Identifier für den Materialien-Hintergrund
	private static final Identifier MATERIALS_BACKGROUND_TEXTURE = Identifier.of("cclive-utilities", "textures/gui/materials_background.png");
	
	public static void initialize() {
		if (isInitialized) {
			return;
		}
		
		try {
			// Register hotkey
			registerHotkey();
			
			// Register commands
			registerCommands();
			
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
	
	private static void registerCommands() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(ClientCommandManager.literal("mats")
				.then(ClientCommandManager.literal("test")
					.then(ClientCommandManager.literal("show")
						.executes(context -> {
							showTestOverlay = true;
							context.getSource().sendFeedback(Text.literal("§aMaterial Tracker Test-Overlay aktiviert!"));
							return 1;
						})
					)
					.then(ClientCommandManager.literal("hide")
						.executes(context -> {
							showTestOverlay = false;
							context.getSource().sendFeedback(Text.literal("§cMaterial Tracker Test-Overlay deaktiviert!"));
							return 1;
						})
					)
				)
				.then(ClientCommandManager.literal("set")
					.then(ClientCommandManager.argument("text", StringArgumentType.greedyString())
						.executes(context -> {
							String newText = StringArgumentType.getString(context, "text");
							testText = newText;
							context.getSource().sendFeedback(Text.literal("§aTest-Text geändert zu: §f" + newText));
							return 1;
						})
					)
				)
			);
		});
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

		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null) {
			return;
		}

		// Render nur wenn Overlays sichtbar sind und keine Equipment-Overlays aktiv sind
		if (showOverlays && !EquipmentDisplayUtility.isEquipmentOverlayActive()) {
			// Render test overlay if enabled
			if (showTestOverlay) {
				renderTestOverlay(context, client);
			}
			// Render normal material display if tracking materials
			else if (isTrackingMaterials) {
				renderMaterialDisplay(context, client);
			}
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
		float scale = CCLiveUtilitiesConfig.HANDLER.instance().materialTrackerScale;
		
		// Berechne Position basierend auf Offsets vom rechten Rand
		int xPosition = screenWidth - OVERLAY_WIDTH - xOffset; // X-Offset vom rechten Rand
		int yPosition = yOffset; // Y-Offset vom oberen Rand
		
		// Get materials from ActionBarData
		List<Object> texts = ActionBarData.getFilteredTexts();
		
		// Calculate dynamic width based on text content and scale
		int dynamicWidth = calculateRequiredWidth(context, texts);
		int scaledOverlayWidth = (int) (Math.max(OVERLAY_WIDTH, dynamicWidth) * scale);
		int actualOverlayWidth = Math.max(OVERLAY_WIDTH, scaledOverlayWidth);
		
		// Adjust X position so overlay expands to the left (stays at right edge)
		int adjustedXPosition = xPosition - (actualOverlayWidth - OVERLAY_WIDTH);
		
		// Draw background based on overlay type
		OverlayType overlayType = CCLiveUtilitiesConfig.HANDLER.instance().materialTrackerOverlayType;
		
		switch (overlayType) {
			case CUSTOM:
				// Draw texture background
				try {
					context.drawTexture(
						RenderPipelines.GUI_TEXTURED,
						MATERIALS_BACKGROUND_TEXTURE,
						adjustedXPosition, yPosition, // Position
						0.0f, 0.0f, // UV-Koordinaten (Start der Textur)
						actualOverlayWidth, (int)((OVERLAY_HEIGHT - 23) * scale), // Größe
						actualOverlayWidth, (int)((OVERLAY_HEIGHT - 23) * scale) // Textur-Größe
					);
				} catch (Exception e) {
					// Fallback: Verwende den ursprünglichen schwarzen Hintergrund wenn Textur-Loading fehlschlägt
					context.fill(adjustedXPosition, yPosition, adjustedXPosition + actualOverlayWidth, yPosition + (int)((OVERLAY_HEIGHT - 23) * scale), 0x80000000);
				}
				break;
			case BLACK:
				// Draw colored background
				context.fill(adjustedXPosition, yPosition, adjustedXPosition + actualOverlayWidth, yPosition + (int)((OVERLAY_HEIGHT - 23) * scale), 0x80000000);
				break;
			case NONE:
				// No background
				break;
		}
		
		int currentY = yPosition + (int)(TEXT_PADDING * scale);
		
		for (Object textObj : texts) {
			
			Text textComponent;
			if (textObj instanceof net.minecraft.text.Text) {
				// Verwende das originale Text-Objekt mit Farbcodes
				textComponent = (net.minecraft.text.Text) textObj;
			} else {
				// Fallback für String-Objekte
				textComponent = Text.literal(textObj.toString());
			}
			
			// Draw text with scaling
			context.drawText(
				MinecraftClient.getInstance().textRenderer, 
				textComponent, 
				adjustedXPosition + (int)(8 * scale), // Verwende die angepasste X-Position + 6 Pixel nach rechts
				currentY - (int)(8 * scale), // 2 Pixel nach unten (-10 + 2 = -8)
				0xFFFFFFFF, // Vollständig weiß mit Alpha
				true // Mit Schatten
			);
			
			currentY += (int)(LINE_HEIGHT * scale);
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
	
	private static void renderTestOverlay(DrawContext context, MinecraftClient client) {
		if (client.getWindow() == null) {
			return;
		}
		
		int screenWidth = client.getWindow().getScaledWidth();
		
		// Position aus der Konfiguration
		int xOffset = CCLiveUtilitiesConfig.HANDLER.instance().materialTrackerX;
		int yOffset = CCLiveUtilitiesConfig.HANDLER.instance().materialTrackerY;
		float scale = CCLiveUtilitiesConfig.HANDLER.instance().materialTrackerScale;
		
		// Berechne Position basierend auf Offsets vom rechten Rand
		int xPosition = screenWidth - OVERLAY_WIDTH - xOffset; // X-Offset vom rechten Rand
		int yPosition = yOffset; // Y-Offset vom oberen Rand
		
		// Create test lines
		List<String> testLines = new ArrayList<>();
		for (int i = 0; i < TEST_LINES_COUNT; i++) {
			testLines.add(testText);
		}
		
		// Calculate dynamic width based on test text content and scale
		int dynamicWidth = calculateRequiredWidthForStrings(context, testLines);
		int scaledOverlayWidth = (int) (Math.max(OVERLAY_WIDTH, dynamicWidth) * scale);
		int actualOverlayWidth = Math.max(OVERLAY_WIDTH, scaledOverlayWidth);
		
		// Adjust X position so overlay expands to the left (stays at right edge)
		int adjustedXPosition = xPosition - (actualOverlayWidth - OVERLAY_WIDTH);
		
		// Draw background based on overlay type
		OverlayType overlayType = CCLiveUtilitiesConfig.HANDLER.instance().materialTrackerOverlayType;
		
		switch (overlayType) {
			case CUSTOM:
				// Draw texture background
				try {
					context.drawTexture(
						RenderPipelines.GUI_TEXTURED,
						MATERIALS_BACKGROUND_TEXTURE,
						adjustedXPosition, yPosition, // Position
						0.0f, 0.0f, // UV-Koordinaten (Start der Textur)
						actualOverlayWidth, (int)((OVERLAY_HEIGHT - 23) * scale), // Größe
						actualOverlayWidth, (int)((OVERLAY_HEIGHT - 23) * scale) // Textur-Größe
					);
				} catch (Exception e) {
					// Fallback: Verwende den ursprünglichen schwarzen Hintergrund wenn Textur-Loading fehlschlägt
					context.fill(adjustedXPosition, yPosition, adjustedXPosition + actualOverlayWidth, yPosition + (int)((OVERLAY_HEIGHT - 23) * scale), 0x80000000);
				}
				break;
			case BLACK:
				// Draw colored background
				context.fill(adjustedXPosition, yPosition, adjustedXPosition + actualOverlayWidth, yPosition + (int)((OVERLAY_HEIGHT - 23) * scale), 0x80000000);
				break;
			case NONE:
				// No background
				break;
		}
		
		int currentY = yPosition + (int)(TEXT_PADDING * scale);
		
		for (String testLine : testLines) {
			// Draw test text with scaling
			context.drawText(
				MinecraftClient.getInstance().textRenderer, 
				Text.literal(testLine), 
				adjustedXPosition + (int)(8 * scale), // Verwende die angepasste X-Position + 6 Pixel nach rechts
				currentY - (int)(8 * scale), // 2 Pixel nach unten (-10 + 2 = -8)
				0xFFFFFFFF, // Vollständig weiß mit Alpha
				true // Mit Schatten
			);
			
			currentY += (int)(LINE_HEIGHT * scale);
		}
	}
	
	private static int calculateRequiredWidthForStrings(DrawContext context, List<String> strings) {
		int maxWidth = MIN_TEXT_WIDTH;
		
		for (String text : strings) {
			// Berechne die Breite des Textes
			int textWidth = MinecraftClient.getInstance().textRenderer.getWidth(text);
			
			// Füge Padding hinzu (links und rechts)
			int totalWidth = textWidth + 15; // 10 Pixel links + 5 Pixel rechts (5 Pixel weniger)
			
			// Aktualisiere die maximale Breite
			maxWidth = Math.max(maxWidth, totalWidth);
		}
		
		return maxWidth;
	}
} 