package net.felix.utilities.Aincraft;

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
import org.joml.Matrix3x2fStack;
import net.felix.CCLiveUtilitiesConfig;
import net.felix.OverlayType;
import net.felix.utilities.Overall.ActionBarData;
import net.felix.utilities.Overall.KeyBindingUtility;
import net.felix.utilities.Town.EquipmentDisplayUtility;

import java.util.List;
import java.util.ArrayList;

public class MaterialTrackerUtility {
	
	
	private static boolean isInitialized = false;
	private static boolean isTrackingMaterials = false;
	private static boolean showOverlays = true; // Neue Variable für Overlay-Sichtbarkeit
	private static String lastDimension = null; // Speichert die letzte Dimension für Dimensionswechsel-Erkennung
	
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

		// Check if we're on a floor dimension
		boolean isOnFloor = false;
		String currentDimension = null;
		try {
			if (client.world != null) {
				currentDimension = client.world.getRegistryKey().getValue().toString();
				String dimensionId = currentDimension.toLowerCase();
				isOnFloor = dimensionId.contains("floor");
			}
		} catch (Exception e) {
			// Silent error handling
		}

		// Check if dimension changed - if so, reset materials
		if (lastDimension != null && currentDimension != null && !lastDimension.equals(currentDimension)) {
			// Dimension changed - reset materials for the overlay
			ActionBarData.reset();
			isTrackingMaterials = false;
		}
		
		// Update last dimension
		lastDimension = currentDimension;

		// Only track materials if we're on a floor AND have materials
		// This ensures the overlay disappears when changing dimensions
		boolean hasMaterials = ActionBarData.hasMaterials();
		boolean shouldTrack = isOnFloor && hasMaterials;
		
		if (shouldTrack != isTrackingMaterials) {
			isTrackingMaterials = shouldTrack;
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
		if (client == null || client.player == null) {
			return;
		}
		
		// Hide overlay if F1 menu (debug screen) is open
		if (client.options.hudHidden) {
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
		
		// Get materials from ActionBarData
		List<Object> texts = ActionBarData.getFilteredTexts();
		
		// Calculate dynamic width based on text content
		int dynamicWidth = calculateRequiredWidth(context, texts);
		int overlayWidth = Math.max(OVERLAY_WIDTH, dynamicWidth);
		
		// Determine if overlay is on left or right side of screen
		// Calculate base position to determine side
		int baseX = screenWidth - OVERLAY_WIDTH - xOffset;
		boolean isOnLeftSide = baseX < screenWidth / 2;
		
		// Calculate position (unscaled)
		// If on left side: expand to the right (keep left edge fixed)
		// If on right side: expand to the left (keep right edge fixed)
		int xPosition;
		if (isOnLeftSide) {
			// Keep left edge fixed, expand to the right
			xPosition = baseX;
		} else {
			// Keep right edge fixed, expand to the left
			xPosition = screenWidth - overlayWidth - xOffset;
		}
		int yPosition = yOffset;
		
		// Use Matrix transformations for scaling (like Blueprint Viewer)
		Matrix3x2fStack matrices = context.getMatrices();
		matrices.pushMatrix();
		
		// Scale based on config
		if (scale <= 0) scale = 1.0f; // Sicherheitscheck
		
		// Translate to position and scale from there
		matrices.translate(xPosition, yPosition);
		matrices.scale(scale, scale);
		
		// Draw background based on overlay type (scaled)
		OverlayType overlayType = CCLiveUtilitiesConfig.HANDLER.instance().materialTrackerOverlayType;
		
		switch (overlayType) {
			case CUSTOM:
				// Draw texture background
				try {
					context.drawTexture(
						RenderPipelines.GUI_TEXTURED,
						MATERIALS_BACKGROUND_TEXTURE,
						0, 0, // Position (relative to matrix)
						0.0f, 0.0f, // UV-Koordinaten (Start der Textur)
						overlayWidth, OVERLAY_HEIGHT - 23, // Größe (unscaled, will be scaled by matrix)
						overlayWidth, OVERLAY_HEIGHT - 23 // Textur-Größe
					);
				} catch (Exception e) {
					// Fallback: Verwende den ursprünglichen schwarzen Hintergrund wenn Textur-Loading fehlschlägt
					context.fill(0, 0, overlayWidth, OVERLAY_HEIGHT - 23, 0x80000000);
				}
				break;
			case BLACK:
				// Draw colored background
				context.fill(0, 0, overlayWidth, OVERLAY_HEIGHT - 23, 0x80000000);
				break;
			case NONE:
				// No background
				break;
		}
		
		// Render materials (scaled)
		int currentY = TEXT_PADDING;
		
		for (Object textObj : texts) {
			Text textComponent;
			if (textObj instanceof net.minecraft.text.Text) {
				// Verwende das originale Text-Objekt mit Farbcodes
				textComponent = (net.minecraft.text.Text) textObj;
			} else {
				// Fallback für String-Objekte
				textComponent = Text.literal(textObj.toString());
			}
			
			// Draw text (scaled by matrix)
			context.drawText(
				MinecraftClient.getInstance().textRenderer, 
				textComponent, 
				8, // X position (relative to matrix)
				currentY - 8, // Y position (relative to matrix)
				0xFFFFFFFF, // Vollständig weiß mit Alpha
				true // Mit Schatten
			);
			
			currentY += LINE_HEIGHT;
		}
		
		// Restore matrix transformations
		matrices.popMatrix();
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
		
		// Create test lines
		List<String> testLines = new ArrayList<>();
		for (int i = 0; i < TEST_LINES_COUNT; i++) {
			testLines.add(testText);
		}
		
		// Calculate dynamic width based on test text content
		int dynamicWidth = calculateRequiredWidthForStrings(context, testLines);
		int overlayWidth = Math.max(OVERLAY_WIDTH, dynamicWidth);
		
		// Calculate position (unscaled)
		int xPosition = screenWidth - overlayWidth - xOffset;
		int yPosition = yOffset;
		
		// Use Matrix transformations for scaling (like Blueprint Viewer)
		Matrix3x2fStack matrices = context.getMatrices();
		matrices.pushMatrix();
		
		// Scale based on config
		if (scale <= 0) scale = 1.0f; // Sicherheitscheck
		
		// Translate to position and scale from there
		matrices.translate(xPosition, yPosition);
		matrices.scale(scale, scale);
		
		// Draw background based on overlay type (scaled)
		OverlayType overlayType = CCLiveUtilitiesConfig.HANDLER.instance().materialTrackerOverlayType;
		
		switch (overlayType) {
			case CUSTOM:
				// Draw texture background
				try {
					context.drawTexture(
						RenderPipelines.GUI_TEXTURED,
						MATERIALS_BACKGROUND_TEXTURE,
						0, 0, // Position (relative to matrix)
						0.0f, 0.0f, // UV-Koordinaten (Start der Textur)
						overlayWidth, OVERLAY_HEIGHT - 23, // Größe (unscaled, will be scaled by matrix)
						overlayWidth, OVERLAY_HEIGHT - 23 // Textur-Größe
					);
				} catch (Exception e) {
					// Fallback: Verwende den ursprünglichen schwarzen Hintergrund wenn Textur-Loading fehlschlägt
					context.fill(0, 0, overlayWidth, OVERLAY_HEIGHT - 23, 0x80000000);
				}
				break;
			case BLACK:
				// Draw colored background
				context.fill(0, 0, overlayWidth, OVERLAY_HEIGHT - 23, 0x80000000);
				break;
			case NONE:
				// No background
				break;
		}
		
		// Render test lines (scaled)
		int currentY = TEXT_PADDING;
		
		for (String testLine : testLines) {
			// Draw test text (scaled by matrix)
			context.drawText(
				MinecraftClient.getInstance().textRenderer, 
				Text.literal(testLine), 
				8, // X position (relative to matrix)
				currentY - 8, // Y position (relative to matrix)
				0xFFFFFFFF, // Vollständig weiß mit Alpha
				true // Mit Schatten
			);
			
			currentY += LINE_HEIGHT;
		}
		
		// Restore matrix transformations
		matrices.popMatrix();
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