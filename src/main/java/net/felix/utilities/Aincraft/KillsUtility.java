package net.felix.utilities.Aincraft;

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
import net.felix.utilities.Overall.KeyBindingUtility;
import net.felix.utilities.Town.EquipmentDisplayUtility;

import org.joml.Matrix3x2fStack;

import java.util.HashMap;
import java.util.Map;

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
	
	// Next level tracking
	private static int killsUntilNextLevel = -1; // -1 means unknown/not found
	private static long lastTabListCheck = 0; // Cache for tab list checks (every 1 second)
	
	// Chinese character mapping for numbers
	private static final Map<Character, Integer> CHINESE_NUMBERS = new HashMap<>();
	static {
		CHINESE_NUMBERS.put('㚏', 0);
		CHINESE_NUMBERS.put('㚐', 1);
		CHINESE_NUMBERS.put('㚑', 2);
		CHINESE_NUMBERS.put('㚒', 3);
		CHINESE_NUMBERS.put('㚓', 4);
		CHINESE_NUMBERS.put('㚔', 5);
		CHINESE_NUMBERS.put('㚕', 6);
		CHINESE_NUMBERS.put('㚖', 7);
		CHINESE_NUMBERS.put('㚗', 8);
		CHINESE_NUMBERS.put('㚘', 9);
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
				// Reset next level tracking
				killsUntilNextLevel = -1;
				lastTabListCheck = 0; // Force immediate check
			}
		}

		// Read kills from bossbar
		if (isTrackingKills) {
			readKillsFromBossbar(client);
		}

		// Always update KPM calculation (even when not tracking kills)
		updateKPM();
		
		// Update kills until next level from tab list (only when tracking kills)
		if (isTrackingKills) {
			try {
				updateKillsUntilNextLevel(client);
			} catch (Exception e) {
				// Silent error handling
			}
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
		// Reset next level tracking
		killsUntilNextLevel = -1;
		lastTabListCheck = 0;
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
			StringBuilder debugChars = new StringBuilder();
			
			System.out.println("🔍 [KillsUtility] DEBUG decodeChineseNumber - Roher Text: '" + text + "'");
			System.out.println("🔍 [KillsUtility] DEBUG decodeChineseNumber - Text-Länge: " + text.length());
			
			for (char c : text.toCharArray()) {
				Integer digit = CHINESE_NUMBERS.get(c);
				if (digit != null) {
					numberStr.append(digit);
					debugChars.append("'").append(c).append("'(").append((int)c).append(")->").append(digit).append(" ");
				} else {
					debugChars.append("'").append(c).append("'(").append((int)c).append(")->? ");
				}
			}
			
			System.out.println("🔍 [KillsUtility] DEBUG decodeChineseNumber - Zeichen-Mapping: " + debugChars.toString());
			
			if (numberStr.length() > 0) {
				int result = Integer.parseInt(numberStr.toString());
				System.out.println("🔍 [KillsUtility] DEBUG decodeChineseNumber - Ergebnis: " + result + " (aus String: '" + numberStr.toString() + "')");
				return result;
			} else {
				System.out.println("🔍 [KillsUtility] DEBUG decodeChineseNumber - KEINE ZAHL GEFUNDEN!");
			}
		} catch (Exception e) {
			System.err.println("❌ [KillsUtility] DEBUG decodeChineseNumber - FEHLER: " + e.getMessage());
			e.printStackTrace();
		}
		
		return -1; // Invalid or no number found
	}
	
	private static void updateKPM() {
		if (sessionStartTime != 0 && newKills != 0) {
			long currentTime = System.currentTimeMillis();
			long sessionDuration = currentTime - sessionStartTime;
			if (sessionDuration > 0) {
				double minutesElapsed = (double)sessionDuration / 60000.0;
				currentKPM = (double)newKills / minutesElapsed;
			}
		} else {
			currentKPM = 0.0;
		}
	}
	
	/**
	 * Reads the kills needed until next level from the tab list.
	 * Searches for "[Nächste Ebene]" and reads the number from the line below.
	 */
	private static void updateKillsUntilNextLevel(MinecraftClient client) {
		try {
			// Check if player is in a floor dimension
			if (!isInFloorDimension(client)) {
				killsUntilNextLevel = -1;
				return;
			}
			
			// Only check every 1 second to avoid performance issues
			long currentTime = System.currentTimeMillis();
			long timeSinceLastCheck = currentTime - lastTabListCheck;
			
			if (timeSinceLastCheck < 1000) {
				return;
			}
			lastTabListCheck = currentTime;
			
			if (client == null || client.getNetworkHandler() == null) {
				killsUntilNextLevel = -1;
				return;
			}
			
			var playerList = client.getNetworkHandler().getPlayerList();
			if (playerList == null) {
				killsUntilNextLevel = -1;
				return;
			}
			
			// Convert to list to iterate with index
			java.util.List<net.minecraft.client.network.PlayerListEntry> entries = 
				new java.util.ArrayList<>(playerList);
			
			// Helper method to remove Minecraft formatting codes (§ codes)
			java.util.function.Function<String, String> removeFormatting = (text) -> {
				if (text == null) return "";
				// Remove all § codes (formatting codes)
				return text.replaceAll("§[0-9a-fk-or]", "").trim();
			};
			
			// Helper method to get text from an entry
			java.util.function.Function<Integer, String> getEntryText = (entryIndex) -> {
				if (entryIndex < 0 || entryIndex >= entries.size()) {
					return null;
				}
				var entry = entries.get(entryIndex);
				if (entry == null) {
					return null;
				}
				
				net.minecraft.text.Text displayName = entry.getDisplayName();
				if (displayName != null) {
					return displayName.getString();
				} else if (entry.getProfile() != null) {
					return entry.getProfile().getName();
				}
				return null;
			};
			
			// Search for "[Nächste Ebene]" first, then check if the entry below contains "ZAHL Kills"
			java.util.regex.Pattern killsPattern = java.util.regex.Pattern.compile(
				"([\\d,.]+)\\s*[Kk]ills?", 
				java.util.regex.Pattern.CASE_INSENSITIVE
			);
			
			for (int i = 0; i < entries.size(); i++) {
				var entry = entries.get(i);
				if (entry == null) {
					continue;
				}
				
				// Get text from entry
				String entryText = getEntryText.apply(i);
				if (entryText == null) {
					continue;
				}
				
				String cleanEntryText = removeFormatting.apply(entryText);
				
				// Check if this entry contains "[Nächste Ebene]"
				if (!cleanEntryText.contains("[Nächste Ebene]")) {
					continue; // Not "[Nächste Ebene]"
				}
				
				// Found "[Nächste Ebene]"! Search through indices below for Kills or "freigeschaltet"
				// Limit search to next 10 entries to avoid going too far
				for (int checkIndex = i + 1; checkIndex < Math.min(i + 11, entries.size()); checkIndex++) {
					String checkText = getEntryText.apply(checkIndex);
					if (checkText == null) {
						continue;
					}
					
					String cleanCheckText = removeFormatting.apply(checkText);
					if (cleanCheckText == null || cleanCheckText.trim().isEmpty()) {
						continue;
					}
					
					// Check if the next level is already unlocked ("freigeschaltet")
					String lowerCheckText = cleanCheckText.toLowerCase();
					if (lowerCheckText.contains("freigeschaltet") || 
					    lowerCheckText.contains("nächste ebene freigeschaltet")) {
						// Next level is already unlocked, no kills needed
						killsUntilNextLevel = -1;
						return; // Stop searching
					}
					
					// Try to find Kills in this line
					java.util.regex.Matcher matcher = killsPattern.matcher(cleanCheckText);
					if (matcher.find()) {
						// Found potential Kills! Check if the entry above contains "[Nächster Meilenstein]"
						if (checkIndex > 0) {
							String aboveText = getEntryText.apply(checkIndex - 1);
							if (aboveText != null) {
								String cleanAboveText = removeFormatting.apply(aboveText);
								
								if (cleanAboveText != null && cleanAboveText.contains("[Nächster Meilenstein]")) {
									// These are milestone kills, not next level kills - continue searching
									continue;
								}
							}
						}
						
						// These are the correct Kills for next level!
						try {
							String numberStr = matcher.group(1).replaceAll("[,.\\s]", "");
							int parsedKills = Integer.parseInt(numberStr);
							killsUntilNextLevel = parsedKills;
							return; // Successfully found and parsed
						} catch (NumberFormatException e) {
							// Continue searching
						}
					}
				}
				
				// If we found "[Nächste Ebene]" but couldn't find valid kills below, set to -1
				killsUntilNextLevel = -1;
				return;
			}
			
			// Not found
			killsUntilNextLevel = -1;
		} catch (Exception e) {
			// Silent error handling
			killsUntilNextLevel = -1;
		}
	}
	
	/**
	 * Check if the player is currently in a floor dimension
	 */
	private static boolean isInFloorDimension(MinecraftClient client) {
		if (client == null || client.world == null) {
			return false;
		}
		
		try {
			String dimensionId = client.world.getRegistryKey().getValue().toString().toLowerCase();
			// Check if dimension contains "floor" (e.g., "minecraft:floor_1", "minecraft:floor_2", etc.)
			return dimensionId.contains("floor");
		} catch (Exception e) {
			return false;
		}
	}
	
	/**
	 * Calculates the time until next level based on current KPM and kills needed.
	 * @return Time in minutes, or -1 if calculation is not possible
	 */
	private static double calculateTimeUntilNextLevel() {
		if (killsUntilNextLevel < 0 || currentKPM <= 0) {
			return -1;
		}
		
		double time = (double)killsUntilNextLevel / currentKPM;
		return time;
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
		if (client == null || client.player == null) {
			return;
		}
		
		// Hide overlay if F1 menu (debug screen) is open
		if (client.options.hudHidden) {
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
		
		// Calculate time until next level - only show if enabled in config
		String nextLevelText = "";
		if (CCLiveUtilitiesConfig.HANDLER.instance().killsUtilityShowNextLevel) {
			nextLevelText = "Nächste Ebene: ?";
			double timeUntilNextLevel = calculateTimeUntilNextLevel();
			if (timeUntilNextLevel >= 0 && killsUntilNextLevel >= 0) {
				long totalMinutes = (long)timeUntilNextLevel;
				long hours = totalMinutes / 60;
				long minutes = totalMinutes % 60;
				
				if (hours > 0) {
					if (minutes > 0) {
						nextLevelText = String.format("Nächste Ebene: %dh %dmin", hours, minutes);
					} else {
						nextLevelText = String.format("Nächste Ebene: %dh", hours);
					}
				} else if (minutes > 0) {
					nextLevelText = String.format("Nächste Ebene: %dmin", minutes);
				} else {
					nextLevelText = String.format("Nächste Ebene: <1min");
				}
			}
		}
		
		// Calculate required kills text - only show if enabled in config
		String requiredKillsText = "";
		if (CCLiveUtilitiesConfig.HANDLER.instance().killsUtilityShowRequiredKills) {
			requiredKillsText = "Benötigte Kills: ?";
			if (killsUntilNextLevel >= 0) {
				requiredKillsText = String.format("Benötigte Kills: %d", killsUntilNextLevel);
			}
		}
		
		// Calculate dynamic overlay size based on text content
		int titleWidth = client.textRenderer.getWidth(title);
		int kpmWidth = client.textRenderer.getWidth(kpmText);
		int newKillsWidth = client.textRenderer.getWidth(newKillsText);
		int timeWidth = timeText.isEmpty() ? 0 : client.textRenderer.getWidth(timeText);
		int nextLevelWidth = nextLevelText.isEmpty() ? 0 : client.textRenderer.getWidth(nextLevelText);
		int requiredKillsWidth = requiredKillsText.isEmpty() ? 0 : client.textRenderer.getWidth(requiredKillsText);
		
		// Find the widest text
		int maxTextWidth = Math.max(Math.max(titleWidth, kpmWidth), 
			Math.max(Math.max(Math.max(newKillsWidth, timeWidth), nextLevelWidth), requiredKillsWidth));
		
		// Calculate overlay dimensions with padding
		int overlayWidth = Math.max(MIN_OVERLAY_WIDTH, maxTextWidth + (PADDING * 2));
		
		// Calculate height based on actual Y positions of text
		// Title: PADDING (5)
		// KPM: PADDING + 15 (20)
		// Kills: 20 + LINE_HEIGHT (32)
		// Zeit: 32 + LINE_HEIGHT (44) if shown
		// Nächste Ebene: 44 + LINE_HEIGHT (56) or 32 + LINE_HEIGHT (44) if Zeit not shown
		// Benötigte Kills: 56 + LINE_HEIGHT (68) or 44 + LINE_HEIGHT (56) if Zeit not shown
		// Final Y position of last text: 68 (with Zeit) or 56 (without Zeit)
		// Height = last Y position + actual text height + bottom padding (same as top padding)
		int lastTextY = PADDING + 15; // Start with KPM position
		lastTextY += LINE_HEIGHT; // Kills line
		if (!timeText.isEmpty()) {
			lastTextY += LINE_HEIGHT; // Zeit line
		}
		if (!nextLevelText.isEmpty()) {
			lastTextY += LINE_HEIGHT; // Nächste Ebene line (only if enabled)
		}
		if (!requiredKillsText.isEmpty()) {
			lastTextY += LINE_HEIGHT; // Benötigte Kills line (only if enabled)
		}
		int textHeight = client.textRenderer.fontHeight; // Use actual text height
		int overlayHeight = lastTextY + textHeight + PADDING; // Last text Y position + text height + bottom padding (same as top)
		
		// Determine if overlay is on left or right side of screen
		// Base position calculation (assuming default width)
		int baseX = screenWidth - MIN_OVERLAY_WIDTH - xOffset;
		boolean isOnLeftSide = baseX < screenWidth / 2;
		
		// Calculate X position based on side
		// If on left side: expand to the right (keep left edge fixed)
		// If on right side: expand to the left (keep right edge fixed)
		int xPosition;
		if (isOnLeftSide) {
			// Keep left edge fixed, expand to the right
			// Calculate left edge position from offset
			// xOffset is distance from right edge with default width
			int leftEdgeX = screenWidth - MIN_OVERLAY_WIDTH - xOffset;
			xPosition = leftEdgeX;
		} else {
			// Keep right edge fixed, expand to the left
			// Right edge stays at: screenWidth - xOffset
			xPosition = screenWidth - overlayWidth - xOffset;
		}
		
		int yPosition = yOffset;
		
		// Verwende Matrix-Transformationen für Skalierung
		Matrix3x2fStack matrices = context.getMatrices();
		matrices.pushMatrix();
		
		// Skaliere basierend auf der Config
		float scale = CCLiveUtilitiesConfig.HANDLER.instance().killsUtilityScale;
		if (scale <= 0) scale = 1.0f; // Sicherheitscheck
		
		// Übersetze zur Position und skaliere von dort aus
		matrices.translate(xPosition, yPosition);
		matrices.scale(scale, scale);
		
		// Draw semi-transparent background only if enabled (skaliert)
		if (CCLiveUtilitiesConfig.HANDLER.instance().killsUtilityShowBackground) {
			context.fill(0, 0, overlayWidth, overlayHeight, 0x80000000);
		}
		
		// Draw title (skaliert)
		int titleColor = CCLiveUtilitiesConfig.HANDLER.instance().killsUtilityHeaderColor.getRGB();
		context.drawText(
			client.textRenderer,
			title,
			PADDING,
			PADDING,
			titleColor,
			true
		);
		
		// Draw KPM (skaliert)
		int currentY = PADDING + 15;
		int textColor = CCLiveUtilitiesConfig.HANDLER.instance().killsUtilityTextColor.getRGB();
		context.drawText(
			client.textRenderer,
			kpmText,
			PADDING,
			currentY,
			textColor,
			true
		);
		
		currentY += LINE_HEIGHT;
		
		// Draw new kills (skaliert)
		context.drawText(
			client.textRenderer,
			newKillsText,
			PADDING,
			currentY,
			textColor,
			true
		);
		
		// Draw session time if available (skaliert)
		if (!timeText.isEmpty()) {
			currentY += LINE_HEIGHT;
			context.drawText(
				client.textRenderer,
				timeText,
				PADDING,
				currentY,
				textColor,
			true
			);
		}
		
		// Draw time until next level (only if enabled in config)
		if (!nextLevelText.isEmpty()) {
			currentY += LINE_HEIGHT;
			context.drawText(
				client.textRenderer,
				nextLevelText,
				PADDING,
				currentY,
				textColor,
				true
			);
		}
		
		// Draw required kills (only if enabled in config)
		if (!requiredKillsText.isEmpty()) {
			currentY += LINE_HEIGHT;
			context.drawText(
				client.textRenderer,
				requiredKillsText,
				PADDING,
				currentY,
				textColor,
				true
			);
		}
		
		// Matrix-Transformationen wiederherstellen
		matrices.popMatrix();
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
	 * Get current overlay width based on actual text content
	 */
	public static int getCurrentOverlayWidth(MinecraftClient client) {
		if (client == null || client.textRenderer == null) {
			return MIN_OVERLAY_WIDTH;
		}
		
		// Prepare text content (same as in renderKillsDisplay)
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
		
		// Calculate time until next level - only show if enabled in config
		String nextLevelText = "";
		if (CCLiveUtilitiesConfig.HANDLER.instance().killsUtilityShowNextLevel) {
			nextLevelText = "Nächste Ebene: ?";
			double timeUntilNextLevel = calculateTimeUntilNextLevel();
			if (timeUntilNextLevel >= 0 && killsUntilNextLevel >= 0) {
				long totalMinutes = (long)timeUntilNextLevel;
				long hours = totalMinutes / 60;
				long minutes = totalMinutes % 60;
				
				if (hours > 0) {
					if (minutes > 0) {
						nextLevelText = String.format("Nächste Ebene: %dh %dmin", hours, minutes);
					} else {
						nextLevelText = String.format("Nächste Ebene: %dh", hours);
					}
				} else if (minutes > 0) {
					nextLevelText = String.format("Nächste Ebene: %dmin", minutes);
				} else {
					nextLevelText = String.format("Nächste Ebene: <1min");
				}
			}
		}
		
		// Calculate required kills text - only show if enabled in config
		String requiredKillsText = "";
		if (CCLiveUtilitiesConfig.HANDLER.instance().killsUtilityShowRequiredKills) {
			requiredKillsText = "Benötigte Kills: ?";
			if (killsUntilNextLevel >= 0) {
				requiredKillsText = String.format("Benötigte Kills: %d", killsUntilNextLevel);
			}
		}
		
		// Calculate widths
		int titleWidth = client.textRenderer.getWidth(title);
		int kpmWidth = client.textRenderer.getWidth(kpmText);
		int newKillsWidth = client.textRenderer.getWidth(newKillsText);
		int timeWidth = timeText.isEmpty() ? 0 : client.textRenderer.getWidth(timeText);
		int nextLevelWidth = nextLevelText.isEmpty() ? 0 : client.textRenderer.getWidth(nextLevelText);
		int requiredKillsWidth = requiredKillsText.isEmpty() ? 0 : client.textRenderer.getWidth(requiredKillsText);
		
		// Find the widest text
		int maxTextWidth = Math.max(Math.max(titleWidth, kpmWidth), 
			Math.max(Math.max(Math.max(newKillsWidth, timeWidth), nextLevelWidth), requiredKillsWidth));
		
		// Calculate overlay width with padding
		return Math.max(MIN_OVERLAY_WIDTH, maxTextWidth + (PADDING * 2));
	}
	
	/**
	 * Get current overlay height based on actual content
	 * This matches the calculation in renderKillsDisplay exactly
	 * 
	 * Y positions in renderKillsDisplay:
	 * - Title: PADDING (5)
	 * - KPM: PADDING + 15 (20)
	 * - Kills: 20 + LINE_HEIGHT (32)
	 * - Zeit: 32 + LINE_HEIGHT (44) if shown
	 * - Nächste Ebene: only if enabled in config
	 * - Benötigte Kills: only if enabled in config
	 */
	public static int getCurrentOverlayHeight() {
		// Calculate height based on actual Y positions (same as renderKillsDisplay)
		int lastTextY = PADDING + 15; // Start with KPM position
		lastTextY += LINE_HEIGHT; // Kills line
		
		// Zeit line (only added if timeText is not empty, same logic as renderKillsDisplay)
		if (sessionStartTime > 0 || isTrackingKills) {
			lastTextY += LINE_HEIGHT;
		}
		
		// Nächste Ebene line (only if enabled in config, same as renderKillsDisplay)
		if (CCLiveUtilitiesConfig.HANDLER.instance().killsUtilityShowNextLevel) {
			lastTextY += LINE_HEIGHT;
		}
		
		// Benötigte Kills line (only if enabled in config, same as renderKillsDisplay)
		if (CCLiveUtilitiesConfig.HANDLER.instance().killsUtilityShowRequiredKills) {
			lastTextY += LINE_HEIGHT;
		}
		
		// Get actual text height from client
		MinecraftClient client = MinecraftClient.getInstance();
		int textHeight = (client != null && client.textRenderer != null) ? client.textRenderer.fontHeight : 9;
		
		// Height = last text Y position + actual text height + bottom padding (same as top padding)
		return lastTextY + textHeight + PADDING;
	}
	
	/**
	 * Process bossbar kills from mixin
	 * This method is called by BossBarMixin when a bossbar with kill information is found
	 */
	public static void processBossBarKills(String bossBarName) {
		try {
			if (!isTrackingKills) {
				System.out.println("🔍 [KillsUtility] DEBUG processBossBarKills - Tracking nicht aktiv, ignoriere: '" + bossBarName + "'");
				return;
			}
			
			System.out.println("🔍 [KillsUtility] DEBUG processBossBarKills - Bossbar-Name empfangen: '" + bossBarName + "'");
			
			int kills = decodeChineseNumber(bossBarName);
			
			System.out.println("🔍 [KillsUtility] DEBUG processBossBarKills - Dekodierte Kills: " + kills);
			System.out.println("🔍 [KillsUtility] DEBUG processBossBarKills - Vorher: initialKills=" + initialKills + ", currentKills=" + currentKills + ", newKills=" + newKills + ", firstBossBarUpdate=" + firstBossBarUpdate);
			
			if (kills >= 0) {
				if (firstBossBarUpdate) {
					initialKills = kills;
					currentKills = kills;
					newKills = 0;
					firstBossBarUpdate = false;
					System.out.println("🔍 [KillsUtility] DEBUG processBossBarKills - ERSTER UPDATE: initialKills=" + initialKills + ", currentKills=" + currentKills);
				} else if (kills > currentKills) {
					currentKills = kills;
					newKills = currentKills - initialKills;
					System.out.println("🔍 [KillsUtility] DEBUG processBossBarKills - Kills erhöht: currentKills=" + currentKills + ", newKills=" + newKills);
				} else if (kills < currentKills) {
					initialKills = kills;
					currentKills = kills;
					newKills = 0;
					System.out.println("🔍 [KillsUtility] DEBUG processBossBarKills - Kills zurückgesetzt (Floor-Wechsel?): initialKills=" + initialKills + ", currentKills=" + currentKills);
				} else {
					currentKills = kills;
					newKills = currentKills - initialKills;
					System.out.println("🔍 [KillsUtility] DEBUG processBossBarKills - Kills unverändert: currentKills=" + currentKills + ", newKills=" + newKills);
				}
			} else {
				System.out.println("🔍 [KillsUtility] DEBUG processBossBarKills - Ungültige Kills (kills < 0), ignoriere");
			}
			
			System.out.println("🔍 [KillsUtility] DEBUG processBossBarKills - Nachher: initialKills=" + initialKills + ", currentKills=" + currentKills + ", newKills=" + newKills);
		} catch (Exception e) {
			System.err.println("❌ [KillsUtility] DEBUG processBossBarKills - FEHLER: " + e.getMessage());
			e.printStackTrace();
		}
	}
}