package net.felix.utilities;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.render.RenderTickCounter;
import net.felix.CCLiveUtilitiesConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.LinkedList;

public class KillsUtility {
	
	private static boolean isInitialized = false;
	private static boolean isTrackingKills = false;
	private static boolean showOverlays = true;
	
	// Hotkey variables
	private static KeyBinding toggleKeyBinding;
	private static KeyBinding resetKeyBinding;
	
	// Bossbar tracking variables
	private static int initialKills = -1; // Kills when entering the floor
	private static int currentKills = 0; // Current kills from bossbar
	private static int newKills = 0; // New kills (current - initial)
	private static String currentDimension = null;
	private static boolean firstBossBarUpdate = true; // Track first bossbar update
	
	// Time tracking for KPM calculation
	private static long sessionStartTime = 0;
	private static double currentKPM = 0.0;
	private static long lastKillTime = 0;
	private static final Queue<Long> killTimes = new LinkedList<>();
	private static final int KPM_WINDOW = 60000; // 1 Minute in Millisekunden
	
	// Chinese character mapping for numbers
	private static final Map<Character, Integer> CHINESE_NUMBERS = new HashMap<>();
	static {
		CHINESE_NUMBERS.put('㚰', 0);
		CHINESE_NUMBERS.put('㚱', 1);
		CHINESE_NUMBERS.put('㚲', 2);
		CHINESE_NUMBERS.put('㚳', 3);
		CHINESE_NUMBERS.put('㚴', 4);
		CHINESE_NUMBERS.put('㚵', 5);
		CHINESE_NUMBERS.put('㚶', 6);
		CHINESE_NUMBERS.put('㚷', 7);
		CHINESE_NUMBERS.put('㚸', 8);
		CHINESE_NUMBERS.put('㚹', 9);
	}
	
	// Rendering constants
	private static final int MIN_OVERLAY_WIDTH = 65;
	private static final int MIN_OVERLAY_HEIGHT = 60;
	private static final int LINE_HEIGHT = 12;
	private static final int PADDING = 5;
	
	// Cache für Floor-Status
	private static Boolean cachedIsOnFloor = null;
	
	public static void initialize() {
		if (isInitialized) {
			return;
		}
		
		try {
			// Register hotkeys
			registerHotkeys();
			
			// Client-seitige Events registrieren
			ClientTickEvents.END_CLIENT_TICK.register(KillsUtility::onClientTick);
			// Registriere HUD-Rendering
			HudRenderCallback.EVENT.register((drawContext, tickDelta) -> onHudRender(drawContext, tickDelta));
			
			isInitialized = true;
		} catch (Exception e) {
			// Silent error handling
		}
	}
	
	private static void registerHotkeys() {
		// Register toggle hotkey
		toggleKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.cclive-utilities.kills-toggle",
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_K, // Default to K key
			"category.cclive-utilities.kills"
		));
		
		// Register reset hotkey
		resetKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.cclive-utilities.kills-reset",
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_R, // Default to R key
			"category.cclive-utilities.kills"
		));
	}

	private static void onClientTick(MinecraftClient client) {
		// Handle hotkeys first
		handleHotkeys();
		
		// Check Tab key for overlay visibility
		checkTabKey();
		
		// Prüfe Konfiguration
		if (!CCLiveUtilitiesConfig.HANDLER.instance().enableMod ||
			!CCLiveUtilitiesConfig.HANDLER.instance().killsUtilityEnabled ||
			!CCLiveUtilitiesConfig.HANDLER.instance().showKillsUtility) {
			return;
		}
		
		if (client.player == null || client.world == null) {
			isTrackingKills = false;
			return;
		}

		// Check for dimension changes and reset if necessary
		checkDimensionChange(client);

		// Check if we're on a floor and should track kills
		boolean shouldTrack = isOnFloor();
		if (shouldTrack != isTrackingKills) {
			isTrackingKills = shouldTrack;
			if (shouldTrack) {
				// Just entered a floor, reset everything
				initialKills = -1;
				currentKills = 0;
				newKills = 0;
				firstBossBarUpdate = true; // Reset first update flag
				sessionStartTime = System.currentTimeMillis();
				// Reset cache when entering floor
				cachedIsOnFloor = null;
			}
		}

		// Read kills from bossbar
		if (isTrackingKills) {
			readKillsFromBossbar(client);
		}

		// Update KPM calculation
		updateKPM();
	}
	
	private static void checkTabKey() {
		// Check if player list key is pressed (respects custom key bindings)
		if (KeyBindingUtility.isPlayerListKeyPressed()) {
			showOverlays = false; // Hide overlays when player list key is pressed
		} else {
			showOverlays = true; // Show overlays when player list key is released
		}
	}
	
	private static void handleHotkeys() {
		// Handle toggle hotkey
		if (toggleKeyBinding != null && toggleKeyBinding.wasPressed()) {
			boolean currentShow = CCLiveUtilitiesConfig.HANDLER.instance().showKillsUtility;
			CCLiveUtilitiesConfig.HANDLER.instance().showKillsUtility = !currentShow;
			CCLiveUtilitiesConfig.HANDLER.save();
		}
		
		// Handle reset hotkey
		if (resetKeyBinding != null && resetKeyBinding.wasPressed()) {
			reset();
		}
	}
	
	private static void checkDimensionChange(MinecraftClient client) {
		try {
			if (client.world != null && client.player != null) {
				String newDimension = client.world.getRegistryKey().getValue().toString();
				
				if (currentDimension != null && !currentDimension.equals(newDimension)) {
					reset();
				}
				
				// Wenn sich die Dimension geändert hat, den Floor-Cache zurücksetzen
				if (currentDimension == null || !currentDimension.equals(newDimension)) {
					cachedIsOnFloor = null;
				}
				
				currentDimension = newDimension;
			}
		} catch (Exception e) {
			// Silent error handling
		}
	}
	
	private static void reset() {
		initialKills = -1;
		currentKills = 0;
		newKills = 0;
		firstBossBarUpdate = true;
		currentKPM = 0.0;
		sessionStartTime = System.currentTimeMillis(); // Reset timer to current time
		currentDimension = null; // Reset dimension tracking
		// Reset cache when resetting
		cachedIsOnFloor = null;
	}
	
	private static void readKillsFromBossbar(MinecraftClient client) {
		try {
			// Bossbar reading is now handled by BossBarMixin
			// This method is kept for potential fallback or future use
			
			// Initialize tracking if needed
			if (isTrackingKills && initialKills == -1) {
				initialKills = 0;
				currentKills = 0;
				newKills = 0;
			}
			
		} catch (Exception e) {
			// Silent error handling
		}
	}
	
	private static int decodeChineseNumber(String text) {
		try {
			StringBuilder numberStr = new StringBuilder();
			
			for (char c : text.toCharArray()) {
				Integer digit = CHINESE_NUMBERS.get(c);
				if (digit != null) {
					numberStr.append(digit);
				}
			}
			
			if (numberStr.length() > 0) {
				return Integer.parseInt(numberStr.toString());
			}
		} catch (Exception e) {
			// Silent error handling
		}
		
		return -1; // Invalid or no number found
	}
	
	private static void updateKPM() {
		if (sessionStartTime == 0 || newKills == 0) {
			currentKPM = 0.0;
			return;
		}
		
		long currentTime = System.currentTimeMillis();
		long sessionDuration = currentTime - sessionStartTime;
		
		if (sessionDuration > 0) {
			// Calculate KPM based on new kills and session time
			double minutesElapsed = sessionDuration / 60000.0; // Convert ms to minutes
			currentKPM = newKills / minutesElapsed;
		}
	}
	
	private static void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
		// Prüfe Konfiguration
		if (!CCLiveUtilitiesConfig.HANDLER.instance().enableMod) {
			return;
		}
		if (!CCLiveUtilitiesConfig.HANDLER.instance().killsUtilityEnabled) {
			return;
		}
		if (!CCLiveUtilitiesConfig.HANDLER.instance().showKillsUtility) {
			return;
		}
		
		if (!isTrackingKills) {
			return;
		}

		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null) {
			return;
		}

		// Render nur wenn Overlays sichtbar sind und keine Equipment-Overlays aktiv sind
		if (showOverlays && !EquipmentDisplayUtility.isEquipmentOverlayActive()) {
			renderKillsDisplay(context, client);
		}
	}
	
	private static void renderKillsDisplay(DrawContext context, MinecraftClient client) {
		if (client.getWindow() == null) {
			return;
		}
		
		int screenWidth = client.getWindow().getScaledWidth();
		int screenHeight = client.getWindow().getScaledHeight();
		
		// Position aus der Konfiguration
		int xOffset = CCLiveUtilitiesConfig.HANDLER.instance().killsUtilityX;
		int yOffset = CCLiveUtilitiesConfig.HANDLER.instance().killsUtilityY;
		
		// Prepare text content
		String title = "Kills/min";
		String kpmText = String.format("KPM: %.1f", currentKPM);
		String newKillsText = String.format("Kills: %d", newKills);
		
		// Calculate session time text
		String timeText = "";
		if (sessionStartTime > 0 || isTrackingKills) {
			long sessionDuration = sessionStartTime > 0 ? System.currentTimeMillis() - sessionStartTime : 0;
			long minutes = sessionDuration / 60000;
			long seconds = (sessionDuration % 60000) / 1000;
			timeText = String.format("Zeit: %02d:%02d", minutes, seconds);
		}
		
		// Calculate dynamic overlay size based on text content
		int titleWidth = client.textRenderer.getWidth(title);
		int kpmWidth = client.textRenderer.getWidth(kpmText);
		int newKillsWidth = client.textRenderer.getWidth(newKillsText);
		int timeWidth = timeText.isEmpty() ? 0 : client.textRenderer.getWidth(timeText);
		
		// Find the widest text
		int maxTextWidth = Math.max(Math.max(titleWidth, kpmWidth), 
			Math.max(newKillsWidth, timeWidth));
		
		// Calculate overlay dimensions with padding
		int overlayWidth = Math.max(MIN_OVERLAY_WIDTH, maxTextWidth + (PADDING * 2));
		int overlayHeight = MIN_OVERLAY_HEIGHT - LINE_HEIGHT; // Reduced height since we removed total text
		
		// Add height for time text if it exists
		if (!timeText.isEmpty()) {
			overlayHeight += LINE_HEIGHT;
		}
		
		// Berechne Position basierend auf Offsets (rechts ausgerichtet)
		int xPosition = screenWidth - overlayWidth - xOffset;
		int yPosition = yOffset;
		
		// Draw semi-transparent background only if enabled
		if (CCLiveUtilitiesConfig.HANDLER.instance().killsUtilityShowBackground) {
			context.fill(xPosition, yPosition, xPosition + overlayWidth, yPosition + overlayHeight, 0x80000000);
		}
		
		// Draw title
		int titleColor = CCLiveUtilitiesConfig.HANDLER.instance().killsUtilityHeaderColor.getRGB();
		context.drawText(
			client.textRenderer,
			title,
			xPosition + PADDING,
			yPosition + PADDING,
			titleColor,
			true
		);
		
		// Draw KPM
		int currentY = yPosition + PADDING + 15;
		int textColor = CCLiveUtilitiesConfig.HANDLER.instance().killsUtilityTextColor.getRGB();
		context.drawText(
			client.textRenderer,
			kpmText,
			xPosition + PADDING,
			currentY,
			textColor,
			true
		);
		
		currentY += LINE_HEIGHT;
		
		// Draw new kills
		context.drawText(
			client.textRenderer,
			newKillsText,
			xPosition + PADDING,
			currentY,
			textColor,
			true
		);
		
		// Draw session time if available
		if (!timeText.isEmpty()) {
			currentY += LINE_HEIGHT;
			context.drawText(
				client.textRenderer,
				timeText,
				xPosition + PADDING,
				currentY,
				textColor,
				true
			);
		}
	}
	
	private static boolean isOnFloor() {
		// Verwende den gecachten Wert, falls verfügbar
		if (cachedIsOnFloor != null) {
			return cachedIsOnFloor;
		}
		
		// Nur berechnen, wenn der Cache leer ist (beim ersten Aufruf)
		try {
			var client = MinecraftClient.getInstance();
			if (client != null && client.world != null) {
				String dimensionId = client.world.getRegistryKey().getValue().toString().toLowerCase();
				boolean isFloor = dimensionId.contains("floor");
				cachedIsOnFloor = isFloor; // Cache den Wert
				return isFloor;
			}
		} catch (Exception e) {
			// Silent error handling
		}
		return false;
	}
	
	/**
	 * Get current KPM
	 */
	public static double getCurrentKPM() {
		return currentKPM;
	}
	
	/**
	 * Get new kills (kills made since entering the floor)
	 */
	public static int getNewKills() {
		return newKills;
	}
	
	/**
	 * Get total kills from bossbar
	 */
	public static int getTotalKills() {
		return currentKills;
	}
	
	/**
	 * Check if we have any kill data
	 */
	public static boolean hasKills() {
		return newKills > 0 || currentKills > 0;
	}
	
	/**
	 * Process bossbar kills from mixin
	 * This method is called by BossBarMixin when a bossbar with kill information is found
	 */
	public static void processBossBarKills(String bossBarName) {
		try {
			if (!isTrackingKills) {
				return;
			}
			
			int kills = decodeChineseNumber(bossBarName);
			
			if (kills >= 0) {
				// Ignore the first bossbar update when entering a floor
				if (firstBossBarUpdate) {
					initialKills = kills;
					currentKills = kills;
					newKills = 0;
					firstBossBarUpdate = false; // Mark first update as done
				} else {
					// Check if kills increased (new kills were made)
					if (kills > currentKills) {
						// Kills increased, update tracking
						currentKills = kills;
						newKills = currentKills - initialKills;
					} else if (kills < currentKills) {
						// Kills decreased (maybe reset or new floor), reset initial kills
						initialKills = kills;
						currentKills = kills;
						newKills = 0;
					} else {
						// Kills unchanged, just update current kills
						currentKills = kills;
						newKills = currentKills - initialKills;
					}
				}
			}
		} catch (Exception e) {
			// Silent error handling
		}
	}
} 