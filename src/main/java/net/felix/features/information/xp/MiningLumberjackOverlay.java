package net.felix.features.information.xp;

import net.felix.CCLiveUtilitiesConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import static net.felix.features.information.util.NumberFormatting.formatNumberWithSeparator;
import static net.felix.features.information.xp.MiningLumberjackTracker.lumberjackXP;
import static net.felix.features.information.xp.MiningLumberjackTracker.miningXP;
import static net.felix.features.information.util.NumberFormatting.formatDoubleWithSeparator;
import static net.felix.features.information.xp.MiningLumberjackTracker.calculateTimeToNextLevel;
import static net.felix.features.information.util.NumberFormatting.formatTime;


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
		return miningXP.level > 0 ? String.format("Bergbau [lvl. %d]", miningXP.level) : "Bergbau";
	}

	/**
	 * Gets the header text for lumberjack overlay (includes level if available)
	 */
	public static String getLumberjackOverlayHeader() {
		return lumberjackXP.level > 0 ? String.format("Holzfäller [lvl. %d]", lumberjackXP.level) : "Holzfäller";
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
		String lastXP = "Letzte XP: " + (miningXP.lastGainedXP > 0 ? formatNumberWithSeparator(miningXP.lastGainedXP) : "0");
		String xpPerMin = miningXP.xpPerMinute > 0 ? "XP/Min: " + formatDoubleWithSeparator(miningXP.xpPerMinute) : "XP/Min: -";
		long xpNeeded = miningXP.requiredXP - miningXP.currentXP;
		String requiredXP = "Benötigte XP: " + (xpNeeded > 0 ? formatNumberWithSeparator(xpNeeded) : "0");
		String timeToNext = "Zeit bis Level: " + formatTime(calculateTimeToNextLevel(miningXP));
		
		int maxWidth = Math.max(client.font.width(header), client.font.width(lastXP));
		maxWidth = Math.max(maxWidth, client.font.width(xpPerMin));
		maxWidth = Math.max(maxWidth, client.font.width(requiredXP));
		maxWidth = Math.max(maxWidth, client.font.width(timeToNext));
		maxWidth = Math.max(maxWidth, 100);
		return maxWidth + padding * 2;
	}

	/**
	 * Gets the actual text strings for mining overlay (for use in overlay editor)
	 */
	public static String[] getMiningOverlayTexts(Minecraft client) {
		if (client == null || client.font == null) {
			return new String[]{"Bergbau", "Letzte XP: 0", "XP/Min: -", "Zeit bis Level: Unbekannt", "Benötigte XP: 0"};
		}
		
		String header = getMiningOverlayHeader();
		String lastXP = "Letzte XP: " + (miningXP.lastGainedXP > 0 ? formatNumberWithSeparator(miningXP.lastGainedXP) : "0");
		String xpPerMin = miningXP.xpPerMinute > 0 ? "XP/Min: " + formatDoubleWithSeparator(miningXP.xpPerMinute) : "XP/Min: -";
		long xpNeeded = miningXP.requiredXP - miningXP.currentXP;
		String requiredXP = "Benötigte XP: " + (xpNeeded > 0 ? formatNumberWithSeparator(xpNeeded) : "0");
		String timeToNext = "Zeit bis Level: " + formatTime(calculateTimeToNextLevel(miningXP));

		return new String[]{header, lastXP, xpPerMin, timeToNext, requiredXP};
	}

	/**
	 * Gets the actual text strings for lumberjack overlay (for use in overlay editor)
	 */
	public static String[] getLumberjackOverlayTexts(Minecraft client) {
		if (client == null || client.font == null) {
			return new String[]{"Holzfäller", "Letzte XP: 0", "XP/Min: -", "Zeit bis Level: Unbekannt", "Benötigte XP: 0"};
		}
		
		String header = getLumberjackOverlayHeader();
		String lastXP = "Letzte XP: " + (lumberjackXP.lastGainedXP > 0 ? formatNumberWithSeparator(lumberjackXP.lastGainedXP) : "0");
		String xpPerMin = lumberjackXP.xpPerMinute > 0 ? "XP/Min: " + formatDoubleWithSeparator(lumberjackXP.xpPerMinute) : "XP/Min: -";
		long xpNeeded = lumberjackXP.requiredXP - lumberjackXP.currentXP;
		String requiredXP = "Benötigte XP: " + (xpNeeded > 0 ? formatNumberWithSeparator(xpNeeded) : "0");
		String timeToNext = "Zeit bis Level: " + formatTime(calculateTimeToNextLevel(lumberjackXP));

		return new String[]{header, lastXP, xpPerMin, timeToNext, requiredXP};
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
		String lastXP = "Letzte XP: " + (lumberjackXP.lastGainedXP > 0 ? formatNumberWithSeparator(lumberjackXP.lastGainedXP) : "0");
		String xpPerMin = lumberjackXP.xpPerMinute > 0 ? "XP/Min: " + formatDoubleWithSeparator(lumberjackXP.xpPerMinute) : "XP/Min: -";
		long xpNeeded = lumberjackXP.requiredXP - lumberjackXP.currentXP;
		String requiredXP = "Benötigte XP: " + (xpNeeded > 0 ? formatNumberWithSeparator(xpNeeded) : "0");
		String timeToNext = "Zeit bis Level: " + formatTime(calculateTimeToNextLevel(lumberjackXP));
		
		int maxWidth = Math.max(client.font.width(header), client.font.width(lastXP));
		maxWidth = Math.max(maxWidth, client.font.width(xpPerMin));
		maxWidth = Math.max(maxWidth, client.font.width(requiredXP));
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
		int overlayHeight = padding * 2 + lineHeight * lines;
		int minOverlayWidth = 100;
		
		// Calculate text width
		String header = miningXP.level > 0 ? String.format("Bergbau [lvl. %d]", miningXP.level) : "Bergbau";
		String lastXP = "Letzte XP: " + (miningXP.lastGainedXP > 0 ? formatNumberWithSeparator(miningXP.lastGainedXP) : "0");
		String xpPerMin = miningXP.xpPerMinute > 0 ? "XP/Min: " + formatDoubleWithSeparator(miningXP.xpPerMinute) : "XP/Min: -";
		long xpNeeded = miningXP.requiredXP - miningXP.currentXP;
		String requiredXP = "Benötigte XP: " + (xpNeeded > 0 ? formatNumberWithSeparator(xpNeeded) : "0");
		String timeToNext = "Zeit bis Level: " + formatTime(calculateTimeToNextLevel(miningXP));
		
		int maxWidth = Math.max(client.font.width(header), client.font.width(lastXP));
		maxWidth = Math.max(maxWidth, client.font.width(xpPerMin));
		maxWidth = Math.max(maxWidth, client.font.width(requiredXP));
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
		
		// Ensure overlay stays within screen bounds
		x = Math.max(0, Math.min(x, screenWidth - overlayWidth));
		
		// Get scale
		float scale = config.miningLumberjackOverlayScale;
		if (scale <= 0) scale = 1.0f;
		
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
		context.text(client.font, lastXP, padding, textY, textColor, true);
		textY += lineHeight;
		context.text(client.font, xpPerMin, padding, textY, textColor, true);
		textY += lineHeight;
		context.text(client.font, timeToNext, padding, textY, textColor, true);
		textY += lineHeight;
		context.text(client.font, requiredXP, padding, textY, textColor, true);
		
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
		int overlayHeight = padding * 2 + lineHeight * lines;
		int minOverlayWidth = 100;
		
		// Calculate text width
		String header = lumberjackXP.level > 0 ? String.format("Holzfäller [lvl. %d]", lumberjackXP.level) : "Holzfäller";
		String lastXP = "Letzte XP: " + (lumberjackXP.lastGainedXP > 0 ? formatNumberWithSeparator(lumberjackXP.lastGainedXP) : "0");
		String xpPerMin = lumberjackXP.xpPerMinute > 0 ? "XP/Min: " + formatDoubleWithSeparator(lumberjackXP.xpPerMinute) : "XP/Min: -";
		long xpNeeded = lumberjackXP.requiredXP - lumberjackXP.currentXP;
		String requiredXP = "Benötigte XP: " + (xpNeeded > 0 ? formatNumberWithSeparator(xpNeeded) : "0");
		String timeToNext = "Zeit bis Level: " + formatTime(calculateTimeToNextLevel(lumberjackXP));
		
		int maxWidth = Math.max(client.font.width(header), client.font.width(lastXP));
		maxWidth = Math.max(maxWidth, client.font.width(xpPerMin));
		maxWidth = Math.max(maxWidth, client.font.width(requiredXP));
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
		
		// Ensure overlay stays within screen bounds
		x = Math.max(0, Math.min(x, screenWidth - overlayWidth));
		
		// Get scale
		float scale = config.miningLumberjackOverlayScale;
		if (scale <= 0) scale = 1.0f;
		
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
		context.text(client.font, lastXP, padding, textY, textColor, true);
		textY += lineHeight;
		context.text(client.font, xpPerMin, padding, textY, textColor, true);
		textY += lineHeight;
		context.text(client.font, timeToNext, padding, textY, textColor, true);
		textY += lineHeight;
		context.text(client.font, requiredXP, padding, textY, textColor, true);
		
		matrices.popMatrix();
	}
}
