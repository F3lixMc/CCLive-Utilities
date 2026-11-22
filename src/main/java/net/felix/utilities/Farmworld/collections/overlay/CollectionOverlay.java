package net.felix.utilities.Farmworld.collections.overlay;

import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.Farmworld.collections.tracker.ResourceTrackerDisplayMode;
import net.felix.utilities.Farmworld.farmzone.FarmzoneResourceRateUtility;
import net.felix.utilities.Other.hud.RateDisplayUnit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import static net.felix.utilities.Farmworld.collections.tracker.CollectionTracker.biomDetected;
import static net.felix.utilities.Farmworld.collections.tracker.CollectionTracker.blocksNeededForNextCollection;
import static net.felix.utilities.Farmworld.collections.tracker.CollectionTracker.blocksPerMinute;
import static net.felix.utilities.Farmworld.collections.tracker.CollectionTracker.calculateTimeUntilNextCollection;
import static net.felix.utilities.Farmworld.collections.tracker.CollectionTracker.isTrackingCollections;
import static net.felix.utilities.Farmworld.collections.tracker.CollectionTracker.sessionBlocks;
import static net.felix.utilities.Farmworld.collections.tracker.CollectionTracker.sessionStartTime;
import static net.felix.utilities.Farmworld.farmzone.FarmzoneScoreboard.isInFarmzone;
import static net.felix.utilities.Farmworld.miningxp.tracker.MiningLumberjackTracker.showOverlays;
import static net.felix.utilities.Other.util.NumberFormatting.formatDoubleWithSeparator;
import static net.felix.utilities.Other.util.NumberFormatting.formatNumberWithSeparator;
import static net.felix.utilities.Other.util.NumberFormatting.formatTimeMinutes;

/**
 * Zeichnet das Collection-Overlay und liefert dessen Abmessungen.
 */
public final class CollectionOverlay {

	private CollectionOverlay() {
	}

	public static String getCollectionResourceRateLine(Minecraft client) {
		if (!usesOverlayResourceRateDisplay()) {
			return null;
		}
		if (client == null || !isInFarmzone(client)) {
			return "Ressource: -";
		}
		return FarmzoneResourceRateUtility.getOverlayText();
	}

	public static boolean usesOverlayResourceRateDisplay() {
		CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
		return config.resourceTrackerRateEnabled
				&& config.resourceTrackerDisplayMode == ResourceTrackerDisplayMode.OVERLAY;
	}

	/**
	 * Renders the collection overlay
	 */
	public static void renderCollectionOverlay(GuiGraphicsExtractor context, Minecraft client) {
		if (!isTrackingCollections || client.player == null) {
			return;
		}
		
		// Hide overlay if F1 menu (debug screen) is open
		if (client.gui.hud.isHidden()) {
			return;
		}
		
		// Render only when overlays are visible
		if (!showOverlays) {
			return;
		}
		
		// Check if overlay is enabled in config
		if (!CCLiveUtilitiesConfig.HANDLER.instance().showCollectionOverlay) {
			return;
		}
		
		// Hide overlay if biom is not detected (scoreboard disappeared)
		if (!biomDetected) {
			return;
		}
		
		// Get position and scale from config
		int x = CCLiveUtilitiesConfig.HANDLER.instance().collectionOverlayX;
		int y = CCLiveUtilitiesConfig.HANDLER.instance().collectionOverlayY;
		float scale = CCLiveUtilitiesConfig.HANDLER.instance().collectionOverlayScale;
		if (scale <= 0) scale = 1.0f;
		
		int padding = 5;
		
		// Calculate elapsed time (format like Kill Tracker: XX:XX)
		long elapsedTime = System.currentTimeMillis() - sessionStartTime;
		long minutes = elapsedTime / 60000;
		long seconds = (elapsedTime % 60000) / 1000;
		String timeText = String.format("Zeit: %02d:%02d", minutes, seconds);
		
		// Build overlay text
		String header = "Collection:";
		String timeLine = timeText; // timeText already contains "Zeit: XX:XX"
		String blocksLine = "Abgebaut: " + formatNumberWithSeparator(sessionBlocks);
		String blocksPerMinLine = formatBlocksPerUnitLine();
		String resourceRateLine = getCollectionResourceRateLine(client);
		String blocksNeededLine = "Benötigte Blöcke: " + formatNumberWithSeparator(blocksNeededForNextCollection);
		String timeToNextLine = "Nächste Collection: " + formatTimeMinutes(calculateTimeUntilNextCollection());
		boolean showBlocksNeeded = isCollectionBlocksNeededVisible();
		boolean showTimeToNext = isCollectionTimeToNextVisible();
		
		// Calculate overlay width (unscaled)
		int maxWidth = Math.max(client.font.width(header), client.font.width(timeLine));
		maxWidth = Math.max(maxWidth, client.font.width(blocksLine));
		maxWidth = Math.max(maxWidth, client.font.width(blocksPerMinLine));
		if (resourceRateLine != null) {
			maxWidth = Math.max(maxWidth, client.font.width(resourceRateLine));
		}
		if (showBlocksNeeded) {
			maxWidth = Math.max(maxWidth, client.font.width(blocksNeededLine));
		}
		if (showTimeToNext) {
			maxWidth = Math.max(maxWidth, client.font.width(timeToNextLine));
		}
		maxWidth = Math.max(maxWidth, 100);
		
		int lineCount = getCollectionOverlayLineCount();
		int unscaledWidth = maxWidth + padding * 2;
		int unscaledHeight = net.felix.utilities.DragOverlay.OverlayInsets.heightForLines(
				padding, 11, client.font.lineHeight, lineCount);
		net.felix.utilities.DragOverlay.dock.OverlayDockRect collectionDock = net.felix.utilities.DragOverlay.dock.OverlayDockLayout.placeLive(
				net.felix.utilities.DragOverlay.dock.OverlayDockIds.COLLECTION,
				x, y, Math.round(unscaledWidth * scale), Math.round(unscaledHeight * scale));
		if (net.felix.utilities.DragOverlay.dock.OverlayDockLayout.isMeasuring()) {
			return;
		}
		x = collectionDock.x();
		y = collectionDock.y();
		scale = net.felix.utilities.DragOverlay.dock.OverlayDockLayout.effectiveScale(
				net.felix.utilities.DragOverlay.dock.OverlayDockIds.COLLECTION, scale);
		
		// Use Matrix transformations for scaling
		org.joml.Matrix3x2fStack matrices = context.pose();
		matrices.pushMatrix();
		
		// Translate to position and scale from there
		matrices.translate(x, y);
		matrices.scale(scale, scale);
		
		// Draw background (scaled, relative to matrix)
		context.fill(0, 0, unscaledWidth, unscaledHeight, 0x80000000);
		
		// Get colors from config
		int headerColor = CCLiveUtilitiesConfig.HANDLER.instance().collectionOverlayHeaderColor.getRGB();
		int textColor = CCLiveUtilitiesConfig.HANDLER.instance().collectionOverlayTextColor.getRGB();
		
		// Draw text (scaled, relative to matrix)
		int textY = padding;
		context.text(client.font, header, padding, textY, headerColor, true);
		textY += 11;
		context.text(client.font, timeLine, padding, textY, textColor, true);
		textY += 11;
		context.text(client.font, blocksLine, padding, textY, textColor, true);
		textY += 11;
		context.text(client.font, blocksPerMinLine, padding, textY, textColor, true);
		textY += 11;
		if (showBlocksNeeded) {
			context.text(client.font, blocksNeededLine, padding, textY, textColor, true);
			textY += 11;
		}
		if (showTimeToNext) {
			context.text(client.font, timeToNextLine, padding, textY, textColor, true);
			textY += 11;
		}
		if (resourceRateLine != null) {
			context.text(client.font, resourceRateLine, padding, textY, textColor, true);
		}
		
		matrices.popMatrix();
	}

	public static boolean isCollectionBlocksNeededVisible() {
		return CCLiveUtilitiesConfig.HANDLER.instance().collectionOverlayShowBlocksNeeded;
	}

	public static boolean isCollectionTimeToNextVisible() {
		return CCLiveUtilitiesConfig.HANDLER.instance().collectionOverlayShowTimeToNext;
	}

	public static int getCollectionOverlayLineCount() {
		int lines = 4;
		if (usesOverlayResourceRateDisplay()) {
			lines++;
		}
		if (isCollectionBlocksNeededVisible()) {
			lines++;
		}
		if (isCollectionTimeToNextVisible()) {
			lines++;
		}
		return lines;
	}

	/**
	 * Get current overlay width based on actual text content
	 * Used by CollectionDraggableOverlay to match the actual overlay size
	 */
	public static int getCurrentCollectionOverlayWidth(Minecraft client) {
		if (client == null || client.font == null) {
			return 200; // Default fallback width
		}
		
		int padding = 5;
		
		// Calculate elapsed time (format like Kill Tracker: XX:XX)
		long elapsedTime = sessionStartTime > 0 ? System.currentTimeMillis() - sessionStartTime : 0;
		long minutes = elapsedTime / 60000;
		long seconds = (elapsedTime % 60000) / 1000;
		String timeText = String.format("Zeit: %02d:%02d", minutes, seconds);
		
		// Build overlay text (same as in renderCollectionOverlay)
		String header = "Collection:";
		String timeLine = timeText;
		String blocksLine = "Abgebaut: " + formatNumberWithSeparator(sessionBlocks);
		String blocksPerMinLine = formatBlocksPerUnitLine();
		String resourceRateLine = getCollectionResourceRateLine(client);
		String blocksNeededLine = "Benötigte Blöcke: " + formatNumberWithSeparator(blocksNeededForNextCollection);
		String timeToNextLine = "Nächste Collection: " + formatTimeMinutes(calculateTimeUntilNextCollection());
		boolean showBlocksNeeded = isCollectionBlocksNeededVisible();
		boolean showTimeToNext = isCollectionTimeToNextVisible();
		
		int maxWidth = Math.max(client.font.width(header), client.font.width(timeLine));
		maxWidth = Math.max(maxWidth, client.font.width(blocksLine));
		maxWidth = Math.max(maxWidth, client.font.width(blocksPerMinLine));
		if (resourceRateLine != null) {
			maxWidth = Math.max(maxWidth, client.font.width(resourceRateLine));
		}
		if (showBlocksNeeded) {
			maxWidth = Math.max(maxWidth, client.font.width(blocksNeededLine));
		}
		if (showTimeToNext) {
			maxWidth = Math.max(maxWidth, client.font.width(timeToNextLine));
		}
		maxWidth = Math.max(maxWidth, 100);
		
		return maxWidth + padding * 2;
	}

	/**
	 * Get current overlay height based on actual content
	 */
	public static int getCurrentCollectionOverlayHeight() {
		int padding = 5;
		net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
		int fontHeight = client != null && client.font != null ? client.font.lineHeight : 9;
		return net.felix.utilities.DragOverlay.OverlayInsets.heightForLines(
				padding, 11, fontHeight, getCollectionOverlayLineCount());
	}

	private static String formatBlocksPerUnitLine() {
		RateDisplayUnit unit = RateDisplayUnit.orDefault(
				CCLiveUtilitiesConfig.HANDLER.instance().resourceTrackerRateUnit);
		String value = blocksPerMinute > 0
				? formatDoubleWithSeparator(unit.fromPerMinute(blocksPerMinute))
				: "-";
		return unit.formatLabeledRate("Blöcke", value);
	}
}
