package net.felix.utilities.Farmworld.miningxp.tracker;

import net.felix.utilities.Farmworld.farmzone.FarmzoneResourceRateUtility;
import net.felix.utilities.Overall.hud.SessionRateUtility;
import net.minecraft.client.Minecraft;

/**
 * Verfolgt XP, XP/Min und Sichtbarkeit fuer Bergbau und Holzfaeller.
 */
public final class MiningLumberjackTracker {

	private MiningLumberjackTracker() {
	}

	public static SkillXpData miningXp = new SkillXpData();

	public static SkillXpData lumberjackXp = new SkillXpData();

	public static long lastTabListCheck = 0;

	private static final long OVERLAY_DISPLAY_DURATION = 10000; // 10 seconds in milliseconds

	public static String currentDimension = null; // Track current dimension to detect dimension changes

	private static String miningLumberjackDimension = null; // Track current dimension for mining/lumberjack overlays

	public static boolean showOverlays = true; // Whether overlays should be shown (hidden when Tab or F1 is active)

	/**
	 * Checks if Tab key is pressed and hides overlays accordingly
	 */
	public static void checkTabKey() {
		// Check if player list key is pressed (respects custom key bindings)
		if (net.felix.utilities.Overall.keybinding.KeyBindingUtility.isPlayerListKeyPressed()) {
			showOverlays = false; // Hide overlays when player list key is pressed
		} else {
			showOverlays = true; // Show overlays when player list key is released
		}
	}

	/**
	 * Updates overlay visibility based on timer
	 * Overlay is visible for 10 seconds after last XP change
	 */
	private static void updateOverlayVisibility(SkillXpData xpData) {
		long currentTime = System.currentTimeMillis();
		
		if (xpData.lastXpChangeTime > 0) {
			long timeSinceLastChange = currentTime - xpData.lastXpChangeTime;
			// Show overlay if less than 10 seconds have passed since last XP change
			boolean wasShowing = xpData.shouldShowOverlay;
			xpData.shouldShowOverlay = timeSinceLastChange < OVERLAY_DISPLAY_DURATION;
			
			// Stop timer when overlay is hidden
			if (wasShowing && !xpData.shouldShowOverlay) {
				// Overlay just got hidden - stop timer
				xpData.sessionStartTime = 0;
				xpData.xpPerMinute = 0.0;
				resetXpPerMinuteTracking(xpData);
			}
			
			// Reset XP calculation 10 seconds after overlay is hidden (20 seconds total)
			// This prevents stale data from lingering
			if (timeSinceLastChange >= OVERLAY_DISPLAY_DURATION + 10000) {
				// Reset all tracking data
				xpData.sessionStartTime = 0;
				xpData.newXp = 0;
				xpData.xpPerMinute = 0.0;
				xpData.initialXp = -1;
				resetXpPerMinuteTracking(xpData);
				// Keep lastXpChangeTime for potential future use
			}
		} else {
			xpData.shouldShowOverlay = false;
			// If overlay is not showing and timer is running, stop it
			if (xpData.sessionStartTime > 0) {
				xpData.sessionStartTime = 0;
				xpData.xpPerMinute = 0.0;
				resetXpPerMinuteTracking(xpData);
			}
		}
	}

	/**
	 * Updates XP per minute calculation continuously (like updateKPM in KillsUtility)
	 */
	private static void updateXpPerMinute(SkillXpData xpData) {
		long now = System.currentTimeMillis();
		boolean xpChanged = xpData.newXp != xpData.lastXpmNewXp;
		if (!SessionRateUtility.shouldRecalculate(xpData.lastXpmUpdateTime, xpChanged)) {
			return;
		}

		xpData.lastXpmUpdateTime = now;
		xpData.lastXpmNewXp = xpData.newXp;

		if (xpData.sessionStartTime != 0 && xpData.newXp > 0) {
			long sessionDuration = now - xpData.sessionStartTime;
			if (sessionDuration > 0) {
				double minutesElapsed = (double) sessionDuration / 60000.0;
				xpData.xpPerMinute = (double) xpData.newXp / minutesElapsed;
			}
		} else {
			xpData.xpPerMinute = 0.0;
		}
	}

	private static void resetXpPerMinuteTracking(SkillXpData xpData) {
		xpData.lastXpmUpdateTime = 0;
		xpData.lastXpmNewXp = 0;
	}

	/**
	 * Updates XP data from tab list for both mining and lumberjack
	 * Works even when the tab list is closed - the player list is always available
	 */
	public static void updateXpFromTabList(Minecraft client) {
		// Check for dimension change and reset initialization flags
		if (client.level != null) {
			String newDimension = client.level.dimension().identifier().getPath();
			if (currentDimension == null || !currentDimension.equals(newDimension)) {
				// Dimension changed - reset initialization flags
				miningXp.isInitializedInCurrentDimension = false;
				lumberjackXp.isInitializedInCurrentDimension = false;
				currentDimension = newDimension;
			}
		}
		// Only check every 0.5 seconds to avoid performance issues
		long currentTime = System.currentTimeMillis();
		long timeSinceLastCheck = currentTime - lastTabListCheck;
		
		if (timeSinceLastCheck < 500) {
			return;
		}
		lastTabListCheck = currentTime;
		
		if (client == null || client.getConnection() == null) {
			return;
		}
		
		// Get player list - this works even when tab list is closed
		// The player list is always available in the network handler
		var playerList = client.getConnection().getOnlinePlayers();
		if (playerList == null) {
			return;
		}
		
		// Convert to list to iterate with index
		java.util.List<net.minecraft.client.multiplayer.PlayerInfo> entries = 
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
			
			net.minecraft.network.chat.Component displayName = entry.getTabListDisplayName();
			if (displayName != null) {
				return displayName.getString();
			} else if (entry.getProfile() != null) {
				return entry.getProfile().name();
			}
			return null;
		};
		
		// Search for "[Bergbau]" and "[Holzfäller]"
		for (int i = 0; i < entries.size(); i++) {
			String entryText = getEntryText.apply(i);
			if (entryText == null) {
				continue;
			}
			
			String cleanEntryText = removeFormatting.apply(entryText);
			
			// Check for "[Bergbau]" - case insensitive and check for variations
			String lowerCleanText = cleanEntryText.toLowerCase();
			if (lowerCleanText.contains("[bergbau]") || cleanEntryText.contains("[Bergbau]")) {
				// Search for XP line after [Bergbau] - it might not be at index + 1
				// Look for a line matching the XP format: "NUMBER / NUMBER [NUMBER]"
				for (int j = i + 1; j < Math.min(i + 5, entries.size()); j++) {
					String xpText = getEntryText.apply(j);
					if (xpText != null) {
						String cleanXpText = removeFormatting.apply(xpText);
						// Check if this line matches the XP format (contains "/" and "[")
						if (cleanXpText.contains("/") && cleanXpText.contains("[")) {
							parseXpLine(cleanXpText, miningXp);
							break;
						}
					}
				}
			}
			
			// Check for "[Holzfäller]" - case insensitive and check for variations
			// Also check for variations like "Holzfaeller" (without umlaut)
			boolean isHolzfaeller = lowerCleanText.contains("[holzfäller]") || 
			                        cleanEntryText.contains("[Holzfäller]") ||
			                        lowerCleanText.contains("[holzfaeller]") ||
			                        cleanEntryText.contains("[Holzfaeller]");
			if (isHolzfaeller) {
				// Search for XP line after [Holzfäller] - it might not be at index + 1
				// Look for a line matching the XP format: "NUMBER / NUMBER [NUMBER]"
				for (int j = i + 1; j < Math.min(i + 5, entries.size()); j++) {
					String xpText = getEntryText.apply(j);
					if (xpText != null) {
						String cleanXpText = removeFormatting.apply(xpText);
						// Check if this line matches the XP format (contains "/" and "[")
						if (cleanXpText.contains("/") && cleanXpText.contains("[")) {
							parseXpLine(cleanXpText, lumberjackXp);
							break;
						}
					}
				}
			}
		}
	}

	/**
	 * Parses XP line in format: "AKTUELLE XP / BENÖTIGTE XP [Level]"
	 */
	private static void parseXpLine(String xpLine, SkillXpData xpData) {
		if (xpLine == null || xpLine.isEmpty()) {
			return;
		}
		
		try {
			// Format: "AKTUELLE XP / BENÖTIGTE XP [Level]"
			// Example: "1234 / 5000 [5]"
			
			// Extract level from [Level]
			java.util.regex.Pattern levelPattern = java.util.regex.Pattern.compile("\\[(\\d+)\\]");
			java.util.regex.Matcher levelMatcher = levelPattern.matcher(xpLine);
			if (levelMatcher.find()) {
				xpData.level = Integer.parseInt(levelMatcher.group(1));
			}
			
			// Extract current XP and required XP
			// Remove level part first
			String xpPart = xpLine.replaceAll("\\[\\d+\\]", "").trim();
			
			// Split by "/"
			String[] parts = xpPart.split("/");
			if (parts.length == 2) {
				// Remove all non-digit characters except dots and commas (for thousands separators)
				String currentXpStr = parts[0].trim().replaceAll("[^0-9.,]", "").replace(",", "").replace(".", "");
				String requiredXpStr = parts[1].trim().replaceAll("[^0-9.,]", "").replace(",", "").replace(".", "");
				
				long newCurrentXp = Long.parseLong(currentXpStr);
				long newRequiredXp = Long.parseLong(requiredXpStr);
				
				// Always update current XP and required XP first (before checking for changes)
				// This ensures we have the latest values for comparison
				long oldCurrentXp = xpData.currentXp;
				xpData.currentXp = newCurrentXp;
				xpData.requiredXp = newRequiredXp;
				
				// Check if this is the first time we're seeing values in this dimension
				boolean isFirstTimeInDimension = !xpData.isInitializedInCurrentDimension;
				
				// Mark as initialized if we have valid XP data
				if (newCurrentXp > 0) {
					xpData.isInitializedInCurrentDimension = true;
				}
				
				// Check if XP changed (but ignore if this is the first time in this dimension)
				boolean xpChanged = (newCurrentXp != oldCurrentXp);
				
				// Track previous overlay state to detect when overlay is shown for the first time
				boolean wasShowingOverlay = xpData.shouldShowOverlay;
				
				// Only show overlay if XP changed AND it's not the first time we're seeing values in this dimension
				if (xpChanged && !isFirstTimeInDimension) {
					long xpGained = newCurrentXp - oldCurrentXp;
					
					// XP increased - track the gain and show overlay
					if (xpGained > 0) {
						xpData.lastGainedXp = xpGained;
						// Reset overlay timer - show overlay for 10 seconds
						xpData.lastXpChangeTime = System.currentTimeMillis();
						xpData.shouldShowOverlay = true;
						
						// Start timer when overlay is shown for the first time
						if (!wasShowingOverlay && xpData.sessionStartTime == 0) {
							xpData.initialXp = newCurrentXp - xpGained; // Set initial to value before this gain
							xpData.newXp = xpGained;
							xpData.sessionStartTime = System.currentTimeMillis();
						} else if (xpData.sessionStartTime > 0) {
							// Timer already running, just update newXp
							if (xpData.initialXp >= 0) {
								xpData.newXp = newCurrentXp - xpData.initialXp;
							}
						}
						
						// Hide the other overlay when this one gets XP
						if (xpData == miningXp) {
							lumberjackXp.shouldShowOverlay = false;
							lumberjackXp.lastXpChangeTime = 0;
						} else if (xpData == lumberjackXp) {
							miningXp.shouldShowOverlay = false;
							miningXp.lastXpChangeTime = 0;
						}
					} else if (xpGained < 0) {
						// XP decreased (level up or reset) - reset tracking like KillsUtility
						xpData.initialXp = newCurrentXp;
						xpData.newXp = 0;
						// Only start timer if overlay is being shown
						if (!wasShowingOverlay) {
						xpData.sessionStartTime = System.currentTimeMillis();
						} else {
							xpData.sessionStartTime = System.currentTimeMillis(); // Reset timer on level up
						}
						// Also reset overlay timer on level up
						xpData.lastXpChangeTime = System.currentTimeMillis();
						xpData.shouldShowOverlay = true;
					}
				}
				
				// Calculate newXp (like newKills in KillsUtility)
				// Only update if timer is running (overlay is showing)
				if (xpData.initialXp >= 0 && xpData.sessionStartTime > 0) {
					xpData.newXp = newCurrentXp - xpData.initialXp;
					if (xpData.newXp < 0) {
						// Level up happened, reset
						xpData.initialXp = newCurrentXp;
						xpData.newXp = 0;
						xpData.sessionStartTime = System.currentTimeMillis();
					}
				}
			}
		} catch (Exception e) {
			// Silently ignore parsing errors
		}
	}

	/**
	 * Calculates time until next level in minutes
	 * Always uses average XP per minute (xpPerMinute) for consistent calculation
	 */
	public static double calculateTimeToNextLevel(SkillXpData xpData) {
		// Always use average XP per minute for time calculation
		// This gives a consistent estimate based on your overall mining speed
		if (xpData.xpPerMinute <= 0) {
			return -1; // Unknown - no data yet
		}
		
		long xpNeeded = xpData.requiredXp - xpData.currentXp;
		if (xpNeeded <= 0) {
			return 0; // Already at max or level up
		}
		
		return xpNeeded / xpData.xpPerMinute;
	}

	/**
	 * Checks for dimension changes for mining/lumberjack overlays and resets tracking if necessary
	 */
	private static void checkMiningLumberjackDimensionChange(Minecraft client) {
		try {
			if (client.level != null && client.player != null) {
				String newDimension = client.level.dimension().identifier().toString();
				
				if (miningLumberjackDimension != null && !miningLumberjackDimension.equals(newDimension)) {
					// Dimension changed, reset mining/lumberjack tracking
					resetMiningLumberjackTracking();
				}
				
				miningLumberjackDimension = newDimension;
			}
		} catch (Exception e) {
			// Silent error handling
		}
	}

	/**
	 * Resets mining/lumberjack tracking (timer and XP data)
	 */
	private static void resetMiningLumberjackTracking() {
		FarmzoneResourceRateUtility.resetSession();

		// Reset mining XP tracking
		miningXp.sessionStartTime = 0;
		miningXp.newXp = 0;
		miningXp.xpPerMinute = 0.0;
		resetXpPerMinuteTracking(miningXp);
		miningXp.initialXp = -1;
		miningXp.shouldShowOverlay = false;
		miningXp.lastXpChangeTime = 0;
		miningXp.isInitializedInCurrentDimension = false;
		
		// Reset lumberjack XP tracking
		lumberjackXp.sessionStartTime = 0;
		lumberjackXp.newXp = 0;
		lumberjackXp.xpPerMinute = 0.0;
		resetXpPerMinuteTracking(lumberjackXp);
		lumberjackXp.initialXp = -1;
		lumberjackXp.shouldShowOverlay = false;
		lumberjackXp.lastXpChangeTime = 0;
		lumberjackXp.isInitializedInCurrentDimension = false;
	}

	/**
	 * Tick-Update fuer Bergbau und Holzfaeller: Tab-Taste, Dimensionswechsel,
	 * XP/Min-Berechnung und Sichtbarkeits-Timer.
	 */
	public static void onClientTick(Minecraft client) {
		checkTabKey();
		checkMiningLumberjackDimensionChange(client);

		// Update XP per minute calculation (only if overlay is currently showing)
		if (miningXp.shouldShowOverlay && miningXp.sessionStartTime > 0) {
			updateXpPerMinute(miningXp);
		}
		if (lumberjackXp.shouldShowOverlay && lumberjackXp.sessionStartTime > 0) {
			updateXpPerMinute(lumberjackXp);
		}

		refreshOverlayVisibility();
	}

	/** Aktualisiert die Sichtbarkeits-Timer beider Overlays. */
	public static void refreshOverlayVisibility() {
		updateOverlayVisibility(miningXp);
		updateOverlayVisibility(lumberjackXp);
	}

	/** {@code false}, solange Tab (Spielerliste) gedrueckt ist. */
	public static boolean areOverlaysAllowed() {
		return showOverlays;
	}

	/** {@code true}, wenn sich die Bergbau-XP kuerzlich geaendert hat und das Overlay sichtbar sein soll. */
	public static boolean isMiningOverlayActive() {
		return miningXp.shouldShowOverlay;
	}

	/** {@code true}, wenn sich die Holzfaeller-XP kuerzlich geaendert hat und das Overlay sichtbar sein soll. */
	public static boolean isLumberjackOverlayActive() {
		return lumberjackXp.shouldShowOverlay;
	}
}
