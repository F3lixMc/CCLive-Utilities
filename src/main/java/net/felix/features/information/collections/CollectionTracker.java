package net.felix.features.information.collections;

import net.felix.utilities.Farmworld.FarmzoneResourceRateUtility;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.felix.CCLiveUtilitiesConfig;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import net.felix.utilities.Overall.KeyCategories;

import static net.felix.features.information.collections.CollectionDatabase.collectionsDatabase;
import static net.felix.features.information.farmzone.FarmzoneScoreboard.extractBiomFromLines;
import static net.felix.features.information.farmzone.FarmzoneScoreboard.isInFarmworldDimension;
import static net.felix.features.information.farmzone.FarmzoneScoreboard.readScoreboardLines;
import static net.felix.features.information.farmzone.FarmzoneScoreboard.stripScoreboardDisplayText;
import static net.felix.features.information.util.NumeralConversion.decodeChineseNumber;


/**
 * Verfolgt Collection-Fortschritt, Biom und Bossbar in der Farmworld.
 */
public final class CollectionTracker {

	private CollectionTracker() {
	}

	private static int initialBlocks = -1; // Blocks when tracking started

	private static int currentBlocks = 0; // Current total blocks from bossbar

	static int sessionBlocks = 0; // Blocks mined in this session (currentBlocks - initialBlocks)

	public static long sessionStartTime = 0; // When tracking started

	static double blocksPerMinute = 0.0; // Blocks per minute

	static boolean isTrackingCollections = false; // Whether we're currently tracking

	public static boolean firstBossBarUpdate = true; // First bossbar update flag

	private static String collectionDimension = null; // Track current dimension for collection tracking

	static int blocksNeededForNextCollection = 0; // Blocks needed for next collection

	private static String currentBiomName = null; // Track current biom name from scoreboard

	public static long lastScoreboardCheck = 0; // Last time we checked the scoreboard

	public static boolean biomDetected = false; // Whether a biom was detected in the scoreboard

	private static boolean lastShowCollectionOverlayState = true; // Track previous state of showCollectionOverlay

	private static int pendingResets = 0; // Legacy (nicht mehr genutzt – Verzögerung über bossBarReadAllowedAfterMs)

	private static int collectionTickCounter = 0; // Tick counter for collection overlay (runs every 20 ticks)

	private static long bossBarReadAllowedAfterMs = 0; // Bossbar erst nach Zone-Wechsel lesen (Server braucht ~3s)

	private static final long ZONE_BOSSBAR_READ_DELAY_MS = 5000L;

	// Collection hotkey
	private static KeyMapping collectionResetKeyMapping;

	/**
	 * Process bossbar collection from mixin
	 * This method is called by BossBarMixin when a bossbar with collection information is found
	 */
	public static void processBossBarCollection(String bossBarName) {
		try {
			if (!isTrackingCollections) {
				return;
			}
			if (System.currentTimeMillis() < bossBarReadAllowedAfterMs) {
				return;
			}
			
			int blocks = decodeChineseNumber(bossBarName);
			
			if (blocks >= 0) {
				if (firstBossBarUpdate) {
					initialBlocks = blocks;
					currentBlocks = blocks;
					sessionBlocks = 0;
					firstBossBarUpdate = false;
					updateNextCollection();
				} else if (blocks > currentBlocks) {
					currentBlocks = blocks;
					sessionBlocks = currentBlocks - initialBlocks;
					updateNextCollection();
				} else if (blocks < currentBlocks) {
					// Blocks decreased (dimension change or reset)
					initialBlocks = blocks;
					currentBlocks = blocks;
					sessionBlocks = 0;
					updateNextCollection();
				} else {
					currentBlocks = blocks;
					sessionBlocks = currentBlocks - initialBlocks;
				}
			}
		} catch (Exception e) {
			// Silent error handling
		}
	}

	/**
	 * Updates the next collection information based on current blocks
	 */
	private static void updateNextCollection() {
		if (collectionsDatabase.isEmpty()) {
			blocksNeededForNextCollection = 0;
			return;
		}
		
		// Find the next collection we haven't reached yet
		for (int i = 1; i <= 10; i++) {
			Integer requiredBlocks = collectionsDatabase.get(i);
			if (requiredBlocks != null && currentBlocks < requiredBlocks) {
				blocksNeededForNextCollection = requiredBlocks - currentBlocks;
				return;
			}
		}
		
		// All collections reached
		blocksNeededForNextCollection = 0;
	}

	/**
	 * Checks for dimension changes and resets collection tracking if necessary
	 */
	private static void checkCollectionDimensionChange(Minecraft client) {
		try {
			if (client.level != null && client.player != null) {
				String newDimension = client.level.dimension().identifier().toString();
				
				if (collectionDimension != null && !collectionDimension.equals(newDimension)) {
					// Dimension changed, reset collection tracking
					resetCollectionTracking();
					// Reset biom detection
					biomDetected = false;
					currentBiomName = null;
					// Stop timer
					sessionStartTime = 0;
					// Check if we're in farmworld and if biom is detected
					if (isInFarmworldDimension(client)) {
						checkCollectionBiomChange(client);
						// Start timer if overlay is enabled and biom is detected
						boolean currentShowCollectionOverlay = CCLiveUtilitiesConfig.HANDLER.instance().showCollectionOverlay;
						if (currentShowCollectionOverlay && biomDetected) {
							sessionStartTime = System.currentTimeMillis();
						}
					}
				}
				
				collectionDimension = newDimension;
			}
		} catch (Exception e) {
			// Silent error handling
		}
	}

	/**
	 * Resets collection tracking
	 */
	private static void resetCollectionTracking() {
		initialBlocks = -1;
		currentBlocks = 0;
		sessionBlocks = 0;
		firstBossBarUpdate = true;
		blocksPerMinute = 0.0;
		collectionDimension = null;
		blocksNeededForNextCollection = 0;
		FarmzoneResourceRateUtility.resetSession();
	}

	/**
	 * Checks for biom changes in scoreboard and resets collection tracking if necessary
	 */
	public static void checkCollectionBiomChange(Minecraft client) {
		try {
			// Only check every 500ms (0.5 seconds) to avoid performance issues
			long currentTime = System.currentTimeMillis();
			long timeSinceLastCheck = currentTime - lastScoreboardCheck;
			
			if (timeSinceLastCheck < 500) {
				return;
			}
			lastScoreboardCheck = currentTime;
			
			if (client == null || client.level == null) {
				clearBiomDetection();
				return;
			}
			
			// Get scoreboard
			net.minecraft.world.scores.Scoreboard scoreboard = client.level.getScoreboard();
			if (scoreboard == null) {
				clearBiomDetection();
				return;
			}
			
			// Get sidebar objective
			net.minecraft.world.scores.Objective sidebarObjective = 
				scoreboard.getDisplayObjective(net.minecraft.world.scores.DisplaySlot.SIDEBAR);
			if (sidebarObjective == null) {
				clearBiomDetection();
				return;
			}
			
			// Check if title is "Cactus Clicker" (Unicode-/Format-Glyphen vom Resourcepack entfernen)
			String titleStr = sidebarObjective.getDisplayName().getString();
			String cleanTitle = stripScoreboardDisplayText(titleStr);
			
			if (!cleanTitle.equalsIgnoreCase("Cactus Clicker")) {
				clearBiomDetection();
				return; // Not the right scoreboard
			}
			
			// Read all scoreboard lines
			List<String> lines = readScoreboardLines(scoreboard, sidebarObjective);
			
			// Extract biom and ressource
			String biomName = extractBiomFromLines(lines);
			// Check if biom changed
			if (biomName != null) {
				boolean wasDetected = biomDetected;
				biomDetected = true;
				if (currentBiomName == null || !currentBiomName.equals(biomName)) {
					currentBiomName = biomName;
					bossBarReadAllowedAfterMs = System.currentTimeMillis() + ZONE_BOSSBAR_READ_DELAY_MS;
					resetCollectionTracking();
					FarmzoneResourceRateUtility.resetSession();
					pendingResets = 0;
					if (CCLiveUtilitiesConfig.HANDLER.instance().showCollectionOverlay && isTrackingCollections) {
						sessionStartTime = System.currentTimeMillis();
					} else {
						sessionStartTime = 0;
					}
				} else if (!wasDetected && CCLiveUtilitiesConfig.HANDLER.instance().showCollectionOverlay) {
					// Overlay will automatically show because biomDetected is now true
					// Start timer if we're tracking
					if (isTrackingCollections && sessionStartTime == 0) {
						sessionStartTime = System.currentTimeMillis();
					}
				}
			} else {
				clearBiomDetection();
			}
		} catch (Exception e) {
			// Silent error handling
		}
	}

	/** Biom verloren / Scoreboard weg → Overlay aus und Ressourcen/Min zurücksetzen. */
	private static void clearBiomDetection() {
		if (!biomDetected) {
			return;
		}
		biomDetected = false;
		sessionStartTime = 0;
		FarmzoneResourceRateUtility.resetSession();
	}

	/**
	 * Registers collection hotkeys
	 */
	public static void registerCollectionHotkeys() {
		// Register reset hotkey
		collectionResetKeyMapping = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.cclive-utilities.collection-reset",
			InputConstants.Type.KEYBOARD,
			InputConstants.UNKNOWN.getValue(), // Unbound by default
			KeyCategories.of("cclive-utilities", "collection")
		));
	}

	/**
	 * Handles collection hotkeys
	 */
	public static void handleCollectionHotkeys() {
		// Handle reset hotkey
		if (collectionResetKeyMapping != null && collectionResetKeyMapping.consumeClick()) {
			resetCollectionTracking();
			// Restart timer if overlay is enabled and we're tracking
			if (CCLiveUtilitiesConfig.HANDLER.instance().showCollectionOverlay && isTrackingCollections) {
				sessionStartTime = System.currentTimeMillis();
			} else {
				sessionStartTime = 0;
			}
		}
	}

	/**
	 * Updates blocks per minute calculation
	 */
	private static void updateBlocksPerMinute() {
		// Only calculate if overlay is enabled and timer is running
		if (CCLiveUtilitiesConfig.HANDLER.instance().showCollectionOverlay && 
		    sessionStartTime > 0 && sessionBlocks > 0) {
			long elapsedTime = System.currentTimeMillis() - sessionStartTime;
			if (elapsedTime > 0) {
				double minutes = elapsedTime / 60000.0;
				blocksPerMinute = sessionBlocks / minutes;
			}
		} else {
			blocksPerMinute = 0.0;
		}
	}

	/**
	 * Calculates time until next collection in minutes
	 */
	static double calculateTimeUntilNextCollection() {
		if (blocksNeededForNextCollection <= 0 || blocksPerMinute <= 0) {
			return -1;
		}
		
		return (double)blocksNeededForNextCollection / blocksPerMinute;
	}

	/**
	 * Returns whether collection tracking is currently active
	 * Used by CollectionDraggableOverlay to determine if overlay should be enabled
	 */
	public static boolean isTrackingCollections() {
		return isTrackingCollections;
	}

	/**
	 * Returns whether a biome was detected in the scoreboard
	 * Used by OverlayEditorScreen to determine if overlays should be shown
	 */
	public static boolean isBiomDetected() {
		return biomDetected;
	}

	/** Erkannte Farmzone aus dem Cactus-Clicker-Scoreboard, sonst null. */
	public static String getDetectedFarmzoneName() {
		return biomDetected ? currentBiomName : null;
	}

	/**
	 * Tick-Update der Collection-Verfolgung: Overlay-Umschaltung, Dimensionswechsel,
	 * Farmworld-Erkennung und (alle 20 Ticks) Biom- und Blocks-pro-Minute-Berechnung.
	 */
	public static void onClientTick(Minecraft client) {
		if (!CCLiveUtilitiesConfig.isModRuntimeEnabled()) {
			return;
		}

		// Increment tick counter
		collectionTickCounter++;

		// Check if overlay was disabled/enabled and handle timer accordingly (every tick for responsiveness)
		boolean currentShowCollectionOverlay = CCLiveUtilitiesConfig.HANDLER.instance().showCollectionOverlay;
		if (currentShowCollectionOverlay != lastShowCollectionOverlayState) {
			if (!currentShowCollectionOverlay) {
				// Overlay was disabled - stop and reset timer
				resetCollectionTracking();
				sessionStartTime = 0; // Stop timer
			} else if (isTrackingCollections) {
				// Overlay was enabled - restart timer if we're tracking
				resetCollectionTracking();
				sessionStartTime = System.currentTimeMillis();
			}
			lastShowCollectionOverlayState = currentShowCollectionOverlay;
		}

		// Check for dimension changes and reset if necessary (every tick for responsiveness)
		checkCollectionDimensionChange(client);

		// Check if we should track collections (in farmworld dimension) (every tick for responsiveness)
		boolean shouldTrack = isInFarmworldDimension(client);
		if (shouldTrack != isTrackingCollections) {
			isTrackingCollections = shouldTrack;
			if (shouldTrack) {
				// Just entered farmworld, reset everything
				resetCollectionTracking();
				// Reset biom detection
				biomDetected = false;
				currentBiomName = null;
				// Check for biom in scoreboard
				checkCollectionBiomChange(client);
				// Only start timer if overlay is enabled and biom is detected
				if (currentShowCollectionOverlay && biomDetected) {
					sessionStartTime = System.currentTimeMillis();
				} else {
					sessionStartTime = 0;
				}
			} else {
				// Left farmworld, reset biom detection
				biomDetected = false;
				currentBiomName = null;
				sessionStartTime = 0;
			}
		}

		// Collection-specific checks run every 20 ticks (1 second)
		if (collectionTickCounter % 20 == 0) {
			// Check for biom changes (always check when in farmworld, not just when tracking)
			// This ensures biomDetected is up to date for the overlay editor
			if (isTrackingCollections) {
				checkCollectionBiomChange(client);

				// Handle pending resets (multiple resets in quick succession after biome change)
				if (pendingResets > 0) {
					resetCollectionTracking();
					// Restart timer if overlay is enabled
					if (CCLiveUtilitiesConfig.HANDLER.instance().showCollectionOverlay) {
						sessionStartTime = System.currentTimeMillis();
					} else {
						sessionStartTime = 0;
					}
					pendingResets--;
				}

				// Update timer based on biom detection
				if (biomDetected && currentShowCollectionOverlay && sessionStartTime == 0) {
					// Biom detected and overlay enabled, but timer not started - start it
					sessionStartTime = System.currentTimeMillis();
				} else if (!biomDetected && sessionStartTime > 0) {
					// Biom no longer detected, stop timer
					sessionStartTime = 0;
				}
			} else if (isInFarmworldDimension(client)) {
				// Also check when in farmworld but not tracking yet (for overlay editor)
				checkCollectionBiomChange(client);
			}

			// Update blocks per minute calculation (only if overlay is enabled and tracking)
			if (isTrackingCollections && currentShowCollectionOverlay) {
				updateBlocksPerMinute();
			}
		}
	}

	/** {@code true}, wenn die Farmworld getrackt wird und ein Biom erkannt wurde (Overlay darf gezeichnet werden). */
	public static boolean isOverlayActive() {
		return isTrackingCollections && biomDetected;
	}
}
