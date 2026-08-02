package net.felix.utilities.Aincraft;

import net.felix.utilities.Overall.KeyCategories;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.StringArgumentType;
import org.joml.Matrix3x2fStack;
import net.felix.CCLiveUtilitiesConfig;
import net.felix.MaterialTrackerDisplayMode;
import net.felix.utilities.Overall.ActionBarData;
import net.felix.utilities.Overall.KeyBindingUtility;
import net.felix.utilities.Town.EquipmentDisplayUtility;
import net.felix.utilities.Town.OverlayType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import java.util.List;
import java.util.ArrayList;

public class MaterialTrackerUtility {
	
	
	private static boolean isInitialized = false;
	private static boolean isTrackingMaterials = false;
	private static boolean showOverlays = true; // Neue Variable für Overlay-Sichtbarkeit
	private static String lastDimension = null; // Speichert die letzte Dimension für Dimensionswechsel-Erkennung
	private static String lastRateDimension = null;
	
	// Test overlay variables
	private static boolean showTestOverlay = false;
	private static String testText = "Prächtiges Eselhaar [1067]";
	private static final int TEST_LINES_COUNT = 5;
	
	// Hotkey variables
	private static KeyMapping toggleKeyMapping;
	private static KeyMapping resetRateKeyMapping;

	
	// Rendering constants
	private static final int LINE_HEIGHT = 13; // 1 Pixel größer (12 + 1 = 13)
	private static final int OVERLAY_WIDTH = 155;
	private static final int OVERLAY_HEIGHT = 103;
	private static final int TEXT_PADDING = 20;
	private static final int TEXT_LEFT_PADDING = 10;
	private static final int TEXT_RIGHT_PADDING = 10;
	private static final int MIN_TEXT_WIDTH = 100; // Minimale Breite für Text
	
	// Textur-Identifier für den Materialien-Hintergrund
	private static final Identifier MATERIALS_BACKGROUND_TEXTURE = Identifier.fromNamespaceAndPath("cclive-utilities", "textures/gui/materials_background.png");
	
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
			HudElementRegistry.addLast(
					Identifier.fromNamespaceAndPath("cclive-utilities", "material_tracker"),
					MaterialTrackerUtility::onHudRender);
			
			isInitialized = true;
		} catch (Exception e) {
			// Silent error handling
		}
	}
	
	private static void registerHotkey() {
		// Register toggle hotkey
		toggleKeyMapping = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.cclive-utilities.material-toggle",
			InputConstants.Type.KEYSYM,
			InputConstants.UNKNOWN.getValue(), // Unbound key
			KeyCategories.of("cclive-utilities", "material")
		));

		resetRateKeyMapping = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.cclive-utilities.material-rate-reset",
			InputConstants.Type.KEYSYM,
			InputConstants.UNKNOWN.getValue(), // Unbound by default
			KeyCategories.of("cclive-utilities", "material")
		));
	}
	
	private static void registerCommands() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(ClientCommands.literal("mats")
				.then(ClientCommands.literal("test")
					.then(ClientCommands.literal("show")
						.executes(context -> {
							showTestOverlay = true;
							context.getSource().sendFeedback(Component.literal("§aMaterial Tracker Test-Overlay aktiviert!"));
							return 1;
						})
					)
					.then(ClientCommands.literal("hide")
						.executes(context -> {
							showTestOverlay = false;
							context.getSource().sendFeedback(Component.literal("§cMaterial Tracker Test-Overlay deaktiviert!"));
							return 1;
						})
					)
				)
				.then(ClientCommands.literal("set")
					.then(ClientCommands.argument("text", StringArgumentType.greedyString())
						.executes(context -> {
							String newText = StringArgumentType.getString(context, "text");
							testText = newText;
							context.getSource().sendFeedback(Component.literal("§aTest-Text geändert zu: §f" + newText));
							return 1;
						})
					)
				)
			);
		});
	}

	private static void onClientTick(Minecraft client) {
		ActionBarData.flushPendingUpdates();

		// Check Tab key for overlay visibility
		checkTabKey();
		
		// Handle hotkey
		handleHotkey();

		updateMaterialRates(client);
		
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
			if (client.level != null) {
				currentDimension = client.level.dimension().identifier().toString();
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
			MaterialRateUtility.resetSession();
			isTrackingMaterials = false;
		}
		
		// Update last dimension
		lastDimension = currentDimension;

		// Only track materials if we're on a floor AND have materials
		boolean hasMaterials = ActionBarData.hasMaterials();
		boolean shouldTrack = isOnFloor && hasMaterials;
		
		if (shouldTrack != isTrackingMaterials) {
			isTrackingMaterials = shouldTrack;
		}
	}

	private static void updateMaterialRates(Minecraft client) {
		if (!CCLiveUtilitiesConfig.HANDLER.instance().enableMod
				|| !CCLiveUtilitiesConfig.HANDLER.instance().materialTrackerEnabled
				|| !CCLiveUtilitiesConfig.HANDLER.instance().materialTrackerRateEnabled
				|| client.player == null) {
			MaterialRateUtility.resetSession();
			return;
		}

		ActionBarData.checkDimensionChange();

		boolean isOnFloor = false;
		String currentDimension = null;
		try {
			if (client.level != null) {
				currentDimension = client.level.dimension().identifier().toString();
				isOnFloor = currentDimension.toLowerCase().contains("floor");
			}
		} catch (Exception e) {
			// Silent error handling
		}

		if (lastRateDimension != null && currentDimension != null && !lastRateDimension.equals(currentDimension)) {
			ActionBarData.reset();
			MaterialRateUtility.resetSession();
		}
		lastRateDimension = currentDimension;

		if (isOnFloor && ActionBarData.hasMaterials()) {
			MaterialRateUtility.updateFromActionBar();
		} else if (!isOnFloor) {
			MaterialRateUtility.resetSession();
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
		if (toggleKeyMapping != null && toggleKeyMapping.consumeClick()) {
			boolean newValue = !(CCLiveUtilitiesConfig.HANDLER.instance().materialTrackerEnabled
					&& CCLiveUtilitiesConfig.HANDLER.instance().showMaterialTracker);
			CCLiveUtilitiesConfig.HANDLER.instance().materialTrackerEnabled = newValue;
			CCLiveUtilitiesConfig.HANDLER.instance().showMaterialTracker = newValue;
			CCLiveUtilitiesConfig.HANDLER.save();
		}

		if (resetRateKeyMapping != null && resetRateKeyMapping.consumeClick()) {
			MaterialRateUtility.resetSession();
		}
	}
	
	public static void onHudRender(GuiGraphicsExtractor context, DeltaTracker tickCounter) {
		// Prüfe Konfiguration
		if (!CCLiveUtilitiesConfig.HANDLER.instance().enableMod ||
			!CCLiveUtilitiesConfig.HANDLER.instance().materialTrackerEnabled || 
			!CCLiveUtilitiesConfig.HANDLER.instance().showMaterialTracker) {
			return;
		}
		if (!net.felix.utilities.Overall.HudOverlayVisibility.shouldRenderWorldHudOverlays()) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		if (client == null || client.player == null) {
			return;
		}
		
		// Hide overlay if F1 menu (debug screen) is open
		if (client.options.hideGui) {
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
	
	private static void renderMaterialDisplay(GuiGraphicsExtractor context, Minecraft client) {
		if (client.getWindow() == null) {
			return;
		}
		
		int screenWidth = client.getWindow().getGuiScaledWidth();
		
		// Position aus der Konfiguration
		int xOffset = CCLiveUtilitiesConfig.HANDLER.instance().materialTrackerX;
		int yOffset = CCLiveUtilitiesConfig.HANDLER.instance().materialTrackerY;
		float scale = CCLiveUtilitiesConfig.HANDLER.instance().materialTrackerScale;
		
		// Get materials from ActionBarData
		List<Object> texts = ActionBarData.getFilteredTexts();
		List<Component> displayTexts = buildDisplayTexts(texts);
		
		// Calculate dynamic width based on text content
		int dynamicWidth = calculateRequiredWidth(context, displayTexts);
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
		Matrix3x2fStack matrices = context.pose();
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
					context.blit(
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
		
		for (Component textComponent : displayTexts) {
			// Draw text (scaled by matrix)
			context.text(
				Minecraft.getInstance().font, 
				textComponent, 
				TEXT_LEFT_PADDING,
				currentY - 8, // Y position (relative to matrix)
				0xFFFFFFFF, // Vollständig weiß mit Alpha
				true // Mit Schatten
			);
			
			currentY += LINE_HEIGHT;
		}
		
		// Restore matrix transformations
		matrices.popMatrix();
	}
	
	private static List<Component> buildDisplayTexts(List<Object> texts) {
		List<Component> displayTexts = new ArrayList<>();
		List<String> materialNames = ActionBarData.getSortedMaterialNames();
		boolean showRates = usesOverlayRateDisplay();

		for (int i = 0; i < texts.size(); i++) {
			Object textObj = texts.get(i);
			Component textComponent;
			if (textObj instanceof Component) {
				textComponent = (Component) textObj;
			} else {
				textComponent = Component.literal(textObj.toString());
			}

			if (showRates && i < materialNames.size()) {
				textComponent = MaterialRateUtility.appendRateForMaterial(materialNames.get(i), textComponent);
			}

			displayTexts.add(textComponent);
		}
		return displayTexts;
	}

	private static int calculateRequiredWidth(GuiGraphicsExtractor context, List<Component> texts) {
		int maxWidth = MIN_TEXT_WIDTH;
		
		for (Component textComponent : texts) {
			int textWidth = Minecraft.getInstance().font.width(textComponent);
			int totalWidth = textWidth + TEXT_LEFT_PADDING + TEXT_RIGHT_PADDING;
			maxWidth = Math.max(maxWidth, totalWidth);
		}
		
		return maxWidth;
	}

	public static boolean usesOverlayRateDisplay() {
		CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
		return config.materialTrackerRateEnabled
				&& config.materialTrackerDisplayMode == MaterialTrackerDisplayMode.OVERLAY;
	}

	public static boolean usesScoreboardRateDisplay() {
		CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
		return config.materialTrackerRateEnabled
				&& config.materialTrackerDisplayMode == MaterialTrackerDisplayMode.SCOREBOARD;
	}
	
	private static void renderTestOverlay(GuiGraphicsExtractor context, Minecraft client) {
		if (client.getWindow() == null) {
			return;
		}
		
		int screenWidth = client.getWindow().getGuiScaledWidth();
		
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
		Matrix3x2fStack matrices = context.pose();
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
					context.blit(
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
			context.text(
				Minecraft.getInstance().font, 
				Component.literal(testLine), 
				TEXT_LEFT_PADDING,
				currentY - 8, // Y position (relative to matrix)
				0xFFFFFFFF, // Vollständig weiß mit Alpha
				true // Mit Schatten
			);
			
			currentY += LINE_HEIGHT;
		}
		
		// Restore matrix transformations
		matrices.popMatrix();
	}
	
	private static int calculateRequiredWidthForStrings(GuiGraphicsExtractor context, List<String> strings) {
		int maxWidth = MIN_TEXT_WIDTH;
		
		for (String text : strings) {
			// Berechne die Breite des Textes
			int textWidth = Minecraft.getInstance().font.width(text);
			
			// Füge Padding hinzu (links und rechts)
			int totalWidth = textWidth + TEXT_LEFT_PADDING + TEXT_RIGHT_PADDING;
			
			// Aktualisiere die maximale Breite
			maxWidth = Math.max(maxWidth, totalWidth);
		}
		
		return maxWidth;
	}
} 