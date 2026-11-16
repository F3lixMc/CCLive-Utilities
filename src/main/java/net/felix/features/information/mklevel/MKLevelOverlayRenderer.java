package net.felix.features.information.mklevel;

import net.felix.CCLiveUtilitiesConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import java.util.List;
import java.util.ArrayList;

import static net.felix.features.information.essences.EssenceWaveNames.getWaveDisplayForMKLevelEssence;
import static net.felix.features.information.mklevel.MKLevelState.getMKLevelScrollOffset;
import static net.felix.features.information.mklevel.MKLevelState.isInMKLevelInventory;
import static net.felix.features.information.mklevel.MKLevelState.mkLevelActiveTab;
import static net.felix.features.information.mklevel.MKLevelState.mkLevelLastKnownHeight;
import static net.felix.features.information.mklevel.MKLevelState.mkLevelScrollbarDragging;
import static net.felix.features.information.mklevel.MKLevelState.mkLevelSearchCursorPosition;
import static net.felix.features.information.mklevel.MKLevelState.mkLevelSearchCursorVisible;
import static net.felix.features.information.mklevel.MKLevelState.mkLevelSearchFocused;
import static net.felix.features.information.mklevel.MKLevelState.mkLevelSearchText;
import static net.felix.features.information.mklevel.MKLevelState.setMKLevelScrollOffset;
import static net.felix.features.information.util.NumberFormatting.formatNumberWithSeparator;
import static net.felix.features.information.mklevel.MKLevelDatabase.filterCombinedWaves;
import static net.felix.features.information.mklevel.MKLevelDatabase.filterMKLevelEntries;
import static net.felix.features.information.mklevel.MKLevelDatabase.filterTotalEssenceEntries;


/**
 * Zeichnet das MK-Level-Overlay.
 */
public final class MKLevelOverlayRenderer {

	private MKLevelOverlayRenderer() {
	}

	/**
	 * Renders MKLevel overlay
	 */
	public static void renderMKLevelOverlay(GuiGraphicsExtractor context, Minecraft client, int inventoryX, int inventoryY, int inventoryWidth, int inventoryHeight) {
		if (client == null || client.getWindow() == null || !isInMKLevelInventory) {
			return;
		}
		
		// Check if MKLevel overlay is enabled
		if (!CCLiveUtilitiesConfig.HANDLER.instance().mkLevelEnabled) {
			return;
		}
		
		int screenWidth = client.getWindow().getGuiScaledWidth();
		
		if (screenWidth <= 0) {
			return;
		}
		
		// Get position from config
		int xPos = CCLiveUtilitiesConfig.HANDLER.instance().mkLevelX;
		int yOffset = CCLiveUtilitiesConfig.HANDLER.instance().mkLevelY;
		float scale = CCLiveUtilitiesConfig.HANDLER.instance().mkLevelScale;
		if (scale <= 0) scale = 1.0f;
		
		// Unscaled dimensions
		int unscaledWidth = 200;
		int unscaledHeight = inventoryHeight; // Same height as inventory
		
		// Cache the height for F6 editor (avoids reflection issues on server)
		mkLevelLastKnownHeight = inventoryHeight;
		
		// Scaled dimensions
		int overlayWidth = Math.round(unscaledWidth * scale);
		
		// Calculate overlay X position
		// Wenn xPos -1 ist, berechne automatisch rechts (für Kompatibilität)
		int overlayX;
		if (xPos == -1) {
			overlayX = screenWidth - overlayWidth - 10; // 10px Abstand vom rechten Rand
		} else {
			// Verwende die absolute X-Position (obere linke Ecke)
			overlayX = xPos;
		}
		
		// Y-Position: Wenn yOffset -1 ist (Standard), verwende die Inventar-Y-Position
		// Ansonsten verwende die absolute Y-Position aus der Config
		int overlayY = (yOffset == -1) ? inventoryY : yOffset;
		
		// Button height (unscaled)
		int buttonHeight = 20;
		
		// Apply matrix transformation for scaling
		org.joml.Matrix3x2fStack matrices = context.pose();
		matrices.pushMatrix();
		matrices.translate(overlayX, overlayY);
		matrices.scale(scale, scale);
		
		// Unscaled coordinates (will be scaled by matrix)
		int padding = 5;
		int lineHeight = 12;
		int searchBarHeight = 16; // Height of search bar
		int contentOffset = 3; // Shift content 3px up
		int textX = padding;
		
		// Draw tab buttons above overlay (at negative Y, aligned with top edge)
		int buttonY = -buttonHeight;
		int tabCount = 3;
		int buttonWidth = unscaledWidth / tabCount;
		String[] tabLabels = {"Einzelne", "Kombiniert", "Gesamt"};
		MKLevelOverlayTab[] tabs = {MKLevelOverlayTab.INDIVIDUAL, MKLevelOverlayTab.COMBINED, MKLevelOverlayTab.TOTAL_ESSENCES};
		
		for (int i = 0; i < tabCount; i++) {
			boolean active = mkLevelActiveTab == tabs[i];
			int buttonX = i * buttonWidth;
			int buttonBgColor = active ? 0xFF404040 : 0xFF202020;
			int buttonBorderColor = active ? 0xFFFFFF00 : 0xFF808080;
			context.fill(buttonX, buttonY, buttonX + buttonWidth, buttonY + buttonHeight, buttonBgColor);
			context.outline(buttonX, buttonY, buttonWidth, buttonHeight, buttonBorderColor);
			
			String buttonText = tabLabels[i];
			int textWidth = client.font.width(buttonText);
			int textXButton = buttonX + (buttonWidth - textWidth) / 2;
			int textYButton = buttonY + (buttonHeight - client.font.lineHeight) / 2;
			context.text(client.font, buttonText, textXButton, textYButton, 0xFFFFFFFF, false);
		}
		
		int searchBarY = padding - contentOffset; // Shift search bar 3px up
		int contentY = searchBarY + searchBarHeight + 2; // Content starts below search bar
		int maxY = unscaledHeight - padding;
		
		// Draw background (scaled)
		context.fill(0, 0, unscaledWidth, unscaledHeight, 0x80000000);
		
		// Draw border (scaled)
		context.outline(0, 0, unscaledWidth, unscaledHeight, 0xFFFFFFFF);
		
		// Draw search bar background (relative to matrix)
		int searchBarX = padding;
		int searchBarWidth = unscaledWidth - padding * 2;
		context.fill(searchBarX, searchBarY, searchBarX + searchBarWidth, searchBarY + searchBarHeight, 0xFF000000);
		context.outline(searchBarX, searchBarY, searchBarWidth, searchBarHeight, mkLevelSearchFocused ? 0xFFFFFF00 : 0xFF808080);
		
		// Draw search text (vertically centered, relative to matrix)
		String displayText = mkLevelSearchText.isEmpty() ? "Suchen..." : mkLevelSearchText;
		int textColor = mkLevelSearchText.isEmpty() ? 0xFF808080 : 0xFFFFFFFF;
		// Calculate text height and center it vertically
		int textHeight = client.font.lineHeight; // Usually 9 pixels
		int searchTextY = searchBarY + (searchBarHeight - textHeight) / 2;
		context.text(client.font, displayText, searchBarX + 2, searchTextY, textColor, false);
		
		// Draw cursor if focused (vertically centered, relative to matrix)
		if (mkLevelSearchFocused && mkLevelSearchCursorVisible) {
			int cursorX = searchBarX + 2;
			if (!mkLevelSearchText.isEmpty() && mkLevelSearchCursorPosition > 0) {
				String textBeforeCursor = mkLevelSearchText.substring(0, Math.min(mkLevelSearchCursorPosition, mkLevelSearchText.length()));
				cursorX += client.font.width(textBeforeCursor);
			}
			// Center cursor vertically
			int cursorY = searchBarY + (searchBarHeight - textHeight) / 2;
			context.fill(cursorX, cursorY, cursorX + 1, cursorY + textHeight, 0xFFFFFFFF);
		}
		
		// Calculate available height for content
		int availableHeight = unscaledHeight - padding * 2 - searchBarHeight - 2 - contentOffset;
		
		List<RenderLine> allLines = buildMKLevelRenderLines(client);
		renderMKLevelScrollableLines(context, client, allLines, textX, contentY, maxY, availableHeight, lineHeight);
		
		// Draw scrollbar on the right side
		// Scrollbar goes from bottom of overlay to bottom of search bar
		int scrollbarWidth = 6; // Width of scrollbar
		int scrollbarX = unscaledWidth - scrollbarWidth - padding;
		int scrollbarTop = contentY; // Start below search bar
		int scrollbarBottom = unscaledHeight - padding; // End at bottom of overlay
		int scrollbarHeight = scrollbarBottom - scrollbarTop;
		
		// Only show scrollbar if there's content to scroll
		if (scrollbarHeight > 0) {
			// Calculate total content height using the same method as rendering
			// We need to count the actual number of lines that will be rendered
			int totalContentHeight = calculateMKLevelTotalLineHeight(lineHeight);
			
			// Only show scrollbar if content is taller than visible area
			if (totalContentHeight > availableHeight) {
				// Draw scrollbar background
				context.fill(scrollbarX, scrollbarTop, scrollbarX + scrollbarWidth, scrollbarBottom, 0xFF404040);
				context.outline(scrollbarX, scrollbarTop, scrollbarWidth, scrollbarHeight, 0xFF808080);
				
				// Calculate handle size and position
				float scrollRatio = (float) availableHeight / totalContentHeight;
				int handleHeight = Math.max(10, (int) (scrollbarHeight * scrollRatio));
				int scrollableTrackHeight = scrollbarHeight - handleHeight;
				
				// Calculate handle position based on scroll offset
				int maxScroll = totalContentHeight - availableHeight;
				float scrollProgress = maxScroll > 0 ? 
					(float) getMKLevelScrollOffset() / maxScroll : 0.0f;
				scrollProgress = Math.max(0.0f, Math.min(1.0f, scrollProgress));
				
				// When scrollProgress is 1.0, handle should be at the bottom
				// handleY = scrollbarTop + scrollableTrackHeight when scrollProgress = 1.0
				// This ensures handleY + handleHeight = scrollbarBottom when at max scroll
				int handleY = scrollbarTop + (int) (scrollableTrackHeight * scrollProgress);
				// Ensure handle doesn't go beyond bounds
				int maxHandleY = scrollbarBottom - handleHeight;
				handleY = Math.max(scrollbarTop, Math.min(handleY, maxHandleY));
				
				// Draw scrollbar handle
				int handleColor = mkLevelScrollbarDragging ? 0xFF808080 : 0xFF606060;
				context.fill(scrollbarX + 1, handleY, scrollbarX + scrollbarWidth - 1, handleY + handleHeight, handleColor);
				context.outline(scrollbarX + 1, handleY, scrollbarWidth - 2, handleHeight, 0xFFC0C0C0);
			}
		}
		
		// Pop matrix transformation
		matrices.popMatrix();
	}

	static int[] calculateMKLevelContentHeights(int lineHeight) {
		int totalHeight = 0;
		int lastEntryHeight = 0;
		
		switch (mkLevelActiveTab) {
			case INDIVIDUAL -> {
				List<MKLevelInfo> entries = filterMKLevelEntries(mkLevelSearchText);
				int height = 4 * lineHeight;
				totalHeight += entries.size() * height;
				if (!entries.isEmpty()) {
					lastEntryHeight = height;
				}
			}
			case COMBINED -> {
				for (CombinedWaveInfo waveInfo : filterCombinedWaves(mkLevelSearchText)) {
					int height = 2 * lineHeight;
					if (waveInfo.without != null && !waveInfo.without.isEmpty()) {
						height += waveInfo.without.size() * lineHeight;
					}
					height += lineHeight;
					totalHeight += height;
					lastEntryHeight = height;
				}
			}
			case TOTAL_ESSENCES -> {
				List<TotalEssenceInfo> entries = filterTotalEssenceEntries(mkLevelSearchText);
				int height = 3 * lineHeight;
				totalHeight += entries.size() * height;
				if (!entries.isEmpty()) {
					lastEntryHeight = height;
				}
			}
		}
		
		return new int[] { totalHeight, lastEntryHeight };
	}

	static int calculateMKLevelTotalLineHeight(int lineHeight) {
		switch (mkLevelActiveTab) {
			case INDIVIDUAL -> {
				return filterMKLevelEntries(mkLevelSearchText).size() * 4 * lineHeight;
			}
			case COMBINED -> {
				int totalContentHeight = 0;
				for (CombinedWaveInfo waveInfo : filterCombinedWaves(mkLevelSearchText)) {
					totalContentHeight += 2 * lineHeight;
					if (waveInfo.without != null && !waveInfo.without.isEmpty()) {
						totalContentHeight += lineHeight;
						if (waveInfo.without.size() > 1) {
							totalContentHeight += (waveInfo.without.size() - 1) * lineHeight;
						}
					}
					totalContentHeight += lineHeight;
				}
				return totalContentHeight;
			}
			case TOTAL_ESSENCES -> {
				return filterTotalEssenceEntries(mkLevelSearchText).size() * 3 * lineHeight;
			}
			default -> {
				return 0;
			}
		}
	}

	private static List<RenderLine> buildMKLevelRenderLines(Minecraft client) {
		List<RenderLine> allLines = new ArrayList<>();
		
		switch (mkLevelActiveTab) {
			case INDIVIDUAL -> {
				for (MKLevelInfo levelInfo : filterMKLevelEntries(mkLevelSearchText)) {
					allLines.add(new RenderLine("-Level " + levelInfo.level, 0xFFFFFF00));
					allLines.add(new RenderLine(" " + levelInfo.essence + ", x" + formatNumberWithSeparator(levelInfo.amount), 0xFFFFFFFF));
					String waveDisplay = getWaveDisplayForMKLevelEssence(levelInfo.essence);
					if (waveDisplay != null) {
						String wavePrefix = "-> Welle: ";
						allLines.add(new RenderLine(wavePrefix, 0xFFC0C0C0, 0, waveDisplay, 0xFF55FF55, client.font.width(wavePrefix)));
					}
					allLines.add(new RenderLine("", 0xFFFFFFFF));
				}
			}
			case COMBINED -> {
				for (CombinedWaveInfo waveInfo : filterCombinedWaves(mkLevelSearchText)) {
					allLines.add(new RenderLine("-Level (" + waveInfo.level + ")", 0xFFFFFF00));
					String wavePrefix = "-> Welle: ";
					String waveNumber = String.valueOf(waveInfo.wave);
					allLines.add(new RenderLine(wavePrefix, 0xFFC0C0C0, 0, waveNumber, 0xFF55FF55, client.font.width(wavePrefix)));
					
					if (waveInfo.without != null && !waveInfo.without.isEmpty()) {
						String withoutHeader = "Ohne: ";
						int withoutHeaderWidth = client.font.width(withoutHeader);
						
						String firstEssence = waveInfo.without.get(0).trim();
						String essenceWaveDisplay = getWaveDisplayForMKLevelEssence(firstEssence);
						if (essenceWaveDisplay != null) {
							String wavePrefixText = " -> Welle: ";
							int essenceNameWidth = client.font.width(firstEssence);
							int wavePrefixX = withoutHeaderWidth + essenceNameWidth;
							int waveNumX = wavePrefixX + client.font.width(wavePrefixText);
							allLines.add(new RenderLine(withoutHeader + firstEssence, 0xFFFF5555, 0, wavePrefixText, 0xFFC0C0C0, wavePrefixX, essenceWaveDisplay, 0xFF55FF55, waveNumX));
						} else {
							allLines.add(new RenderLine(withoutHeader + firstEssence, 0xFFFF5555));
						}
						
						for (int i = 1; i < waveInfo.without.size(); i++) {
							String essenceName = waveInfo.without.get(i).trim();
							String waveDisplay = getWaveDisplayForMKLevelEssence(essenceName);
							if (waveDisplay != null) {
								String wavePrefixText = " -> Welle: ";
								int essenceNameWidth = client.font.width(essenceName);
								int wavePrefixX = withoutHeaderWidth + essenceNameWidth;
								int waveNumX = wavePrefixX + client.font.width(wavePrefixText);
								allLines.add(new RenderLine(essenceName, 0xFFFF5555, withoutHeaderWidth, wavePrefixText, 0xFFC0C0C0, wavePrefixX, waveDisplay, 0xFF55FF55, waveNumX));
							} else {
								allLines.add(new RenderLine(essenceName, 0xFFFF5555, withoutHeaderWidth, "", 0, 0));
							}
						}
					}
					
					allLines.add(new RenderLine("", 0xFFFFFFFF));
				}
			}
			case TOTAL_ESSENCES -> {
				for (TotalEssenceInfo essenceInfo : filterTotalEssenceEntries(mkLevelSearchText)) {
					allLines.add(new RenderLine(" " + essenceInfo.essence + ", x" + formatNumberWithSeparator(essenceInfo.totalAmount), 0xFFFFFFFF));
					String waveDisplay = getWaveDisplayForMKLevelEssence(essenceInfo.essence);
					if (waveDisplay != null) {
						String wavePrefix = "-> Welle: ";
						allLines.add(new RenderLine(wavePrefix, 0xFFC0C0C0, 0, waveDisplay, 0xFF55FF55, client.font.width(wavePrefix)));
					}
					allLines.add(new RenderLine("", 0xFFFFFFFF));
				}
			}
		}
		
		return allLines;
	}

	private static void renderMKLevelScrollableLines(GuiGraphicsExtractor context, Minecraft client, List<RenderLine> allLines,
			int textX, int contentY, int maxY, int availableHeight, int lineHeight) {
		int totalHeight = allLines.size() * lineHeight;
		int maxScrollOffset = Math.max(0, totalHeight - availableHeight);
		
		int scrollOffset = getMKLevelScrollOffset();
		if (scrollOffset > maxScrollOffset) {
			setMKLevelScrollOffset(maxScrollOffset);
			scrollOffset = maxScrollOffset;
		}
		
		int startLine = scrollOffset / lineHeight;
		int pixelOffset = scrollOffset % lineHeight;
		int textY = contentY;
		
		if (startLine > 0) {
			String moreText = mkLevelActiveTab == MKLevelOverlayTab.TOTAL_ESSENCES
				? "↑ Weitere Essenzen (Scrollen)"
				: "↑ Weitere Level (Scrollen)";
			context.text(client.font, moreText, textX, textY, 0x80FFFFFF, true);
			textY += lineHeight;
		}
		
		textY -= pixelOffset;
		int minRenderY = startLine > 0 ? contentY + lineHeight : contentY;
		boolean mightHaveMoreLines = startLine + (availableHeight / lineHeight) < allLines.size();
		int maxRenderY = mightHaveMoreLines ? maxY - lineHeight : maxY;
		
		for (int i = startLine; i < allLines.size(); i++) {
			if (textY >= maxRenderY) {
				break;
			}
			if (textY + lineHeight > minRenderY) {
				RenderLine line = allLines.get(i);
				if (line.hasSecondPart) {
					context.text(client.font, line.text, textX + line.xOffset, textY, line.color, true);
					context.text(client.font, line.secondPartText, textX + line.secondPartXOffset, textY, line.secondPartColor, true);
					if (line.hasThirdPart) {
						context.text(client.font, line.thirdPartText, textX + line.thirdPartXOffset, textY, line.thirdPartColor, true);
					}
				} else {
					context.text(client.font, line.text, textX + line.xOffset, textY, line.color, true);
				}
			}
			textY += lineHeight;
		}
		
		boolean hasMoreLines = getMKLevelScrollOffset() < maxScrollOffset;
		if (hasMoreLines && textY < maxY) {
			String moreText = mkLevelActiveTab == MKLevelOverlayTab.TOTAL_ESSENCES
				? "↓ Weitere Essenzen (Scrollen)"
				: "↓ Weitere Level (Scrollen)";
			context.text(client.font, moreText, textX, textY, 0x80FFFFFF, true);
		}
	}
}
