package net.felix.utilities.Farmworld.miningxp.overlay;

import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.Other.hud.RateDisplayUnit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import static net.felix.utilities.Farmworld.miningxp.tracker.MiningLumberjackTracker.calculateTimeToNextLevel;
import static net.felix.utilities.Farmworld.miningxp.tracker.MiningLumberjackTracker.lumberjackXp;
import static net.felix.utilities.Farmworld.miningxp.tracker.MiningLumberjackTracker.miningXp;
import static net.felix.utilities.Other.util.NumberFormatting.formatDoubleWithSeparator;
import static net.felix.utilities.Other.util.NumberFormatting.formatNumberWithSeparator;
import static net.felix.utilities.Other.util.NumberFormatting.formatTime;

/**
 * Zeichnet die Bergbau- und Holzfaeller-Overlays.
 */
public final class MiningLumberjackOverlay {

	private MiningLumberjackOverlay() {
	}

	/**
	 * Gets the header text for mining overlay (includes level if available)
	 */
	public static String getMiningOverlayHeader() {
		return miningXp.level > 0 ? String.format("Bergbau [lvl. %d]", miningXp.level) : "Bergbau";
	}

	/**
	 * Gets the header text for lumberjack overlay (includes level if available)
	 */
	public static String getLumberjackOverlayHeader() {
		return lumberjackXp.level > 0 ? String.format("Holzfäller [lvl. %d]", lumberjackXp.level) : "Holzfäller";
	}

	/**
	 * Gets the current overlay width for mining overlay (for use in overlay editor)
	 */
	public static int getMiningOverlayWidth(Minecraft client) {
		if (client == null || client.font == null) {
			return 200;
		}
		
		int padding = 5;
		String header = getMiningOverlayHeader();
		String lastXp = "Letzte XP: " + (miningXp.lastGainedXp > 0 ? formatNumberWithSeparator(miningXp.lastGainedXp) : "0");
		String xpPerMin = formatXpPerUnitLine(miningXp.xpPerMinute);
		long xpNeeded = miningXp.requiredXp - miningXp.currentXp;
		String requiredXp = "Benötigte XP: " + (xpNeeded > 0 ? formatNumberWithSeparator(xpNeeded) : "0");
		String timeToNext = "Zeit bis Level: " + formatTime(calculateTimeToNextLevel(miningXp));
		
		int maxWidth = Math.max(client.font.width(header), client.font.width(lastXp));
		maxWidth = Math.max(maxWidth, client.font.width(xpPerMin));
		maxWidth = Math.max(maxWidth, client.font.width(requiredXp));
		maxWidth = Math.max(maxWidth, client.font.width(timeToNext));
		maxWidth = Math.max(maxWidth, 100);
		return maxWidth + padding * 2;
	}

	/**
	 * Gets the actual text strings for mining overlay (for use in overlay editor)
	 */
	public static String[] getMiningOverlayTexts(Minecraft client) {
		if (client == null || client.font == null) {
			return new String[]{"Bergbau", "Letzte XP: 0", formatXpPerUnitLine(0), "Zeit bis Level: Unbekannt", "Benötigte XP: 0"};
		}
		
		String header = getMiningOverlayHeader();
		String lastXp = "Letzte XP: " + (miningXp.lastGainedXp > 0 ? formatNumberWithSeparator(miningXp.lastGainedXp) : "0");
		String xpPerMin = formatXpPerUnitLine(miningXp.xpPerMinute);
		long xpNeeded = miningXp.requiredXp - miningXp.currentXp;
		String requiredXp = "Benötigte XP: " + (xpNeeded > 0 ? formatNumberWithSeparator(xpNeeded) : "0");
		String timeToNext = "Zeit bis Level: " + formatTime(calculateTimeToNextLevel(miningXp));

		return new String[]{header, lastXp, xpPerMin, timeToNext, requiredXp};
	}

	/**
	 * Gets the actual text strings for lumberjack overlay (for use in overlay editor)
	 */
	public static String[] getLumberjackOverlayTexts(Minecraft client) {
		if (client == null || client.font == null) {
			return new String[]{"Holzfäller", "Letzte XP: 0", formatXpPerUnitLine(0), "Zeit bis Level: Unbekannt", "Benötigte XP: 0"};
		}
		
		String header = getLumberjackOverlayHeader();
		String lastXp = "Letzte XP: " + (lumberjackXp.lastGainedXp > 0 ? formatNumberWithSeparator(lumberjackXp.lastGainedXp) : "0");
		String xpPerMin = formatXpPerUnitLine(lumberjackXp.xpPerMinute);
		long xpNeeded = lumberjackXp.requiredXp - lumberjackXp.currentXp;
		String requiredXp = "Benötigte XP: " + (xpNeeded > 0 ? formatNumberWithSeparator(xpNeeded) : "0");
		String timeToNext = "Zeit bis Level: " + formatTime(calculateTimeToNextLevel(lumberjackXp));

		return new String[]{header, lastXp, xpPerMin, timeToNext, requiredXp};
	}

	/**
	 * Gets the current overlay width for lumberjack overlay (for use in overlay editor)
	 */
	public static int getLumberjackOverlayWidth(Minecraft client) {
		if (client == null || client.font == null) {
			return 200;
		}
		
		int padding = 5;
		String header = getLumberjackOverlayHeader();
		String lastXp = "Letzte XP: " + (lumberjackXp.lastGainedXp > 0 ? formatNumberWithSeparator(lumberjackXp.lastGainedXp) : "0");
		String xpPerMin = formatXpPerUnitLine(lumberjackXp.xpPerMinute);
		long xpNeeded = lumberjackXp.requiredXp - lumberjackXp.currentXp;
		String requiredXp = "Benötigte XP: " + (xpNeeded > 0 ? formatNumberWithSeparator(xpNeeded) : "0");
		String timeToNext = "Zeit bis Level: " + formatTime(calculateTimeToNextLevel(lumberjackXp));
		
		int maxWidth = Math.max(client.font.width(header), client.font.width(lastXp));
		maxWidth = Math.max(maxWidth, client.font.width(xpPerMin));
		maxWidth = Math.max(maxWidth, client.font.width(requiredXp));
		maxWidth = Math.max(maxWidth, client.font.width(timeToNext));
		maxWidth = Math.max(maxWidth, 100);
		return maxWidth + padding * 2;
	}

	/**
	 * Renders mining overlay
	 */
	public static void renderMiningOverlay(GuiGraphicsExtractor context, Minecraft client) {
		if (client.getWindow() == null) {
			return;
		}
		
		CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
		int screenWidth = client.getWindow().getGuiScaledWidth();
		
		// Base position from config
		int baseX = config.miningOverlayX;
		int y = config.miningOverlayY;
		
		// Calculate overlay dimensions
		int padding = 5;
		int lineHeight = 12;
		int lines = 5;
		int overlayHeight = net.felix.utilities.DragOverlay.editor.OverlayInsets.heightForLines(padding, lineHeight, client.font.lineHeight, lines);
		int minOverlayWidth = 100;
		
		// Calculate text width
		String header = miningXp.level > 0 ? String.format("Bergbau [lvl. %d]", miningXp.level) : "Bergbau";
		String lastXp = "Letzte XP: " + (miningXp.lastGainedXp > 0 ? formatNumberWithSeparator(miningXp.lastGainedXp) : "0");
		String xpPerMin = formatXpPerUnitLine(miningXp.xpPerMinute);
		long xpNeeded = miningXp.requiredXp - miningXp.currentXp;
		String requiredXp = "Benötigte XP: " + (xpNeeded > 0 ? formatNumberWithSeparator(xpNeeded) : "0");
		String timeToNext = "Zeit bis Level: " + formatTime(calculateTimeToNextLevel(miningXp));
		
		int maxWidth = Math.max(client.font.width(header), client.font.width(lastXp));
		maxWidth = Math.max(maxWidth, client.font.width(xpPerMin));
		maxWidth = Math.max(maxWidth, client.font.width(requiredXp));
		maxWidth = Math.max(maxWidth, client.font.width(timeToNext));
		maxWidth = Math.max(maxWidth, minOverlayWidth);
		int overlayWidth = maxWidth + padding * 2;
		
		// Determine if overlay is on left or right side of screen
		// baseX is the left edge position at minimum width
		boolean isOnLeftSide = baseX < screenWidth / 2;
		
		// Calculate X position based on side
		int x;
		if (isOnLeftSide) {
			// On left side: keep left edge fixed, expand to the right
			x = baseX;
		} else {
			// On right side: keep right edge fixed, expand to the left
			// Right edge at minimum width is: baseX + minOverlayWidth
			// Keep this right edge fixed, so left edge moves left when width increases
			int rightEdge = baseX + minOverlayWidth;
			x = rightEdge - overlayWidth;
		}
		
		// Get scale
		float scale = config.miningLumberjackOverlayScale;
		if (scale <= 0) scale = 1.0f;
		net.felix.utilities.DragOverlay.dock.model.OverlayDockRect miningDock = net.felix.utilities.DragOverlay.dock.OverlayDockLayout.placeLive(
				net.felix.utilities.DragOverlay.dock.model.OverlayDockIds.MINING,
				x, y, Math.round(overlayWidth * scale), Math.round(overlayHeight * scale));
		if (net.felix.utilities.DragOverlay.dock.OverlayDockLayout.isMeasuring()) {
			return;
		}
		x = miningDock.x();
		y = miningDock.y();
		scale = net.felix.utilities.DragOverlay.dock.OverlayDockLayout.effectiveScale(
				net.felix.utilities.DragOverlay.dock.model.OverlayDockIds.MINING, scale);
		
		// Apply matrix transformations for scaling
		var matrices = context.pose();
		matrices.pushMatrix();
		matrices.translate(x, y);
		matrices.scale(scale, scale);
		
		// Draw background (scaled, relative to matrix)
		if (config.miningOverlayShowBackground) {
			context.fill(0, 0, overlayWidth, overlayHeight, 0x80000000);
		}
		
		// Get colors from config
		int headerColor = config.miningLumberjackOverlayHeaderColor.getRGB();
		int textColor = config.miningLumberjackOverlayTextColor.getRGB();
		
		// Draw text (scaled, relative to matrix)
		int textY = padding;
		context.text(client.font, header, padding, textY, headerColor, true);
		textY += lineHeight;
		context.text(client.font, lastXp, padding, textY, textColor, true);
		textY += lineHeight;
		context.text(client.font, xpPerMin, padding, textY, textColor, true);
		textY += lineHeight;
		context.text(client.font, timeToNext, padding, textY, textColor, true);
		textY += lineHeight;
		context.text(client.font, requiredXp, padding, textY, textColor, true);
		
		matrices.popMatrix();
	}

	/**
	 * Renders lumberjack overlay
	 */
	public static void renderLumberjackOverlay(GuiGraphicsExtractor context, Minecraft client) {
		if (client.getWindow() == null) {
			return;
		}
		
		CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
		int screenWidth = client.getWindow().getGuiScaledWidth();
		
		// Base position from config (same as mining overlay)
		int baseX = config.miningOverlayX;
		int y = config.miningOverlayY;
		
		// Calculate overlay dimensions
		int padding = 5;
		int lineHeight = 12;
		int lines = 5;
		int overlayHeight = net.felix.utilities.DragOverlay.editor.OverlayInsets.heightForLines(padding, lineHeight, client.font.lineHeight, lines);
		int minOverlayWidth = 100;
		
		// Calculate text width
		String header = lumberjackXp.level > 0 ? String.format("Holzfäller [lvl. %d]", lumberjackXp.level) : "Holzfäller";
		String lastXp = "Letzte XP: " + (lumberjackXp.lastGainedXp > 0 ? formatNumberWithSeparator(lumberjackXp.lastGainedXp) : "0");
		String xpPerMin = formatXpPerUnitLine(lumberjackXp.xpPerMinute);
		long xpNeeded = lumberjackXp.requiredXp - lumberjackXp.currentXp;
		String requiredXp = "Benötigte XP: " + (xpNeeded > 0 ? formatNumberWithSeparator(xpNeeded) : "0");
		String timeToNext = "Zeit bis Level: " + formatTime(calculateTimeToNextLevel(lumberjackXp));
		
		int maxWidth = Math.max(client.font.width(header), client.font.width(lastXp));
		maxWidth = Math.max(maxWidth, client.font.width(xpPerMin));
		maxWidth = Math.max(maxWidth, client.font.width(requiredXp));
		maxWidth = Math.max(maxWidth, client.font.width(timeToNext));
		maxWidth = Math.max(maxWidth, minOverlayWidth);
		int overlayWidth = maxWidth + padding * 2;
		
		// Determine if overlay is on left or right side of screen
		// baseX is the left edge position at minimum width
		boolean isOnLeftSide = baseX < screenWidth / 2;
		
		// Calculate X position based on side
		int x;
		if (isOnLeftSide) {
			// On left side: keep left edge fixed, expand to the right
			x = baseX;
		} else {
			// On right side: keep right edge fixed, expand to the left
			// Right edge at minimum width is: baseX + minOverlayWidth
			// Keep this right edge fixed, so left edge moves left when width increases
			int rightEdge = baseX + minOverlayWidth;
			x = rightEdge - overlayWidth;
		}
		
		// Get scale
		float scale = config.miningLumberjackOverlayScale;
		if (scale <= 0) scale = 1.0f;
		net.felix.utilities.DragOverlay.dock.model.OverlayDockRect miningDock = net.felix.utilities.DragOverlay.dock.OverlayDockLayout.placeLive(
				net.felix.utilities.DragOverlay.dock.model.OverlayDockIds.MINING,
				x, y, Math.round(overlayWidth * scale), Math.round(overlayHeight * scale));
		if (net.felix.utilities.DragOverlay.dock.OverlayDockLayout.isMeasuring()) {
			return;
		}
		x = miningDock.x();
		y = miningDock.y();
		scale = net.felix.utilities.DragOverlay.dock.OverlayDockLayout.effectiveScale(
				net.felix.utilities.DragOverlay.dock.model.OverlayDockIds.MINING, scale);
		
		// Apply matrix transformations for scaling
		var matrices = context.pose();
		matrices.pushMatrix();
		matrices.translate(x, y);
		matrices.scale(scale, scale);
		
		// Draw background (scaled, relative to matrix)
		if (config.lumberjackOverlayShowBackground) {
			context.fill(0, 0, overlayWidth, overlayHeight, 0x80000000);
		}
		
		// Get colors from config
		int headerColor = config.miningLumberjackOverlayHeaderColor.getRGB();
		int textColor = config.miningLumberjackOverlayTextColor.getRGB();
		
		// Draw text (scaled, relative to matrix)
		int textY = padding;
		context.text(client.font, header, padding, textY, headerColor, true);
		textY += lineHeight;
		context.text(client.font, lastXp, padding, textY, textColor, true);
		textY += lineHeight;
		context.text(client.font, xpPerMin, padding, textY, textColor, true);
		textY += lineHeight;
		context.text(client.font, timeToNext, padding, textY, textColor, true);
		textY += lineHeight;
		context.text(client.font, requiredXp, padding, textY, textColor, true);
		
		matrices.popMatrix();
	}

	private static String formatXpPerUnitLine(double xpPerMinute) {
		RateDisplayUnit unit = RateDisplayUnit.orDefault(
				CCLiveUtilitiesConfig.HANDLER.instance().miningLumberjackRateUnit);
		if (xpPerMinute <= 0) {
			return unit.formatXpRate("-");
		}
		return unit.formatXpRate(formatDoubleWithSeparator(unit.fromPerMinute(xpPerMinute)));
	}
}
