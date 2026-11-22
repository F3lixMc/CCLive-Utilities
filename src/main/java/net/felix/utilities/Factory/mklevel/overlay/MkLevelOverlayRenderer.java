package net.felix.utilities.Factory.mklevel.overlay;

import java.util.ArrayList;
import java.util.List;
import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.Aincraft.information.essences.EssenceWaveNames;
import net.felix.utilities.Factory.mklevel.data.CombinedWaveInfo;
import net.felix.utilities.Factory.mklevel.data.MkLevelInfo;
import net.felix.utilities.Factory.mklevel.data.TotalEssenceInfo;
import net.felix.utilities.Overall.clipboard.storage.CollectedMaterialsResourcesStorage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import static net.felix.utilities.Aincraft.information.essences.EssenceWaveNames.getWaveDisplayForMkLevelEssence;
import static net.felix.utilities.Factory.mklevel.data.MkLevelDatabase.filterCombinedWaves;
import static net.felix.utilities.Factory.mklevel.data.MkLevelDatabase.filterMkLevelEntries;
import static net.felix.utilities.Factory.mklevel.data.MkLevelDatabase.filterTotalEssenceEntries;
import static net.felix.utilities.Factory.mklevel.data.MkLevelState.getMkLevelScrollOffset;
import static net.felix.utilities.Factory.mklevel.data.MkLevelState.isInMkLevelInventory;
import static net.felix.utilities.Factory.mklevel.data.MkLevelState.mkLevelActiveTab;
import static net.felix.utilities.Factory.mklevel.data.MkLevelState.mkLevelLastKnownHeight;
import static net.felix.utilities.Factory.mklevel.data.MkLevelState.mkLevelScrollbarDragging;
import static net.felix.utilities.Factory.mklevel.data.MkLevelState.mkLevelSearchCursorPosition;
import static net.felix.utilities.Factory.mklevel.data.MkLevelState.mkLevelSearchCursorVisible;
import static net.felix.utilities.Factory.mklevel.data.MkLevelState.mkLevelSearchFocused;
import static net.felix.utilities.Factory.mklevel.data.MkLevelState.mkLevelSearchText;
import static net.felix.utilities.Factory.mklevel.data.MkLevelState.setMkLevelScrollOffset;
import static net.felix.utilities.Other.util.NumberFormatting.formatNumberWithSeparator;

/**
 * Zeichnet das MK-Level-Overlay.
 */
public final class MkLevelOverlayRenderer {

	private MkLevelOverlayRenderer() {
	}

	public static final int VERTICAL_INSET = 2;
	public static final int SEARCH_BAR_HEIGHT = 16;

	public static int contentTop() {
		return VERTICAL_INSET + SEARCH_BAR_HEIGHT + 2;
	}

	public static int contentBottom(int unscaledHeight) {
		return unscaledHeight - VERTICAL_INSET;
	}

	public static int availableContentHeight(int unscaledHeight) {
		return Math.max(0, contentBottom(unscaledHeight) - contentTop());
	}

	/**
	 * Renders MkLevel overlay
	 */
	public static void contributeLayout(Minecraft client, net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> screen) {
		if (client == null || screen == null) {
			return;
		}
		int inventoryWidth = 176;
		int inventoryHeight = mkLevelLastKnownHeight > 0 ? mkLevelLastKnownHeight : 166;
		int inventoryX = (client.getWindow().getGuiScaledWidth() - inventoryWidth) / 2;
		int inventoryY = (client.getWindow().getGuiScaledHeight() - inventoryHeight) / 2;
		try {
			java.lang.reflect.Field xField = net.minecraft.client.gui.screens.inventory.AbstractContainerScreen.class.getDeclaredField("leftPos");
			java.lang.reflect.Field yField = net.minecraft.client.gui.screens.inventory.AbstractContainerScreen.class.getDeclaredField("topPos");
			java.lang.reflect.Field bgWidthField = net.minecraft.client.gui.screens.inventory.AbstractContainerScreen.class.getDeclaredField("imageWidth");
			java.lang.reflect.Field bgHeightField = net.minecraft.client.gui.screens.inventory.AbstractContainerScreen.class.getDeclaredField("imageHeight");
			xField.setAccessible(true);
			yField.setAccessible(true);
			bgWidthField.setAccessible(true);
			bgHeightField.setAccessible(true);
			inventoryX = xField.getInt(screen);
			inventoryY = yField.getInt(screen);
			inventoryWidth = bgWidthField.getInt(screen);
			inventoryHeight = bgHeightField.getInt(screen);
		} catch (ReflectiveOperationException ignored) {
		}
		renderMkLevelOverlay(null, client, inventoryX, inventoryY, inventoryWidth, inventoryHeight);
	}

	public static void renderMkLevelOverlay(GuiGraphicsExtractor context, Minecraft client, int inventoryX, int inventoryY, int inventoryWidth, int inventoryHeight) {
		if (client == null || client.getWindow() == null || !isInMkLevelInventory) {
			return;
		}
		
		// Check if MkLevel overlay is enabled
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
		net.felix.utilities.DragOverlay.dock.OverlayDockRect mkDock = net.felix.utilities.DragOverlay.dock.OverlayDockLayout.placeLive(
				net.felix.utilities.DragOverlay.dock.OverlayDockIds.MK_LEVEL,
				overlayX, overlayY, overlayWidth, Math.round(unscaledHeight * scale));
		if (net.felix.utilities.DragOverlay.dock.OverlayDockLayout.isMeasuring() || context == null) {
			return;
		}
		overlayX = mkDock.x();
		overlayY = mkDock.y();
		scale = net.felix.utilities.DragOverlay.dock.OverlayDockLayout.effectiveScale(
				net.felix.utilities.DragOverlay.dock.OverlayDockIds.MK_LEVEL, scale);
		
		// Button height (unscaled)
		int buttonHeight = 20;
		
		// Apply matrix transformation for scaling
		org.joml.Matrix3x2fStack matrices = context.pose();
		matrices.pushMatrix();
		matrices.translate(overlayX, overlayY);
		matrices.scale(scale, scale);
		
		// Unscaled coordinates (will be scaled by matrix)
		int horizontalPadding = 5;
		int lineHeight = 12;
		int searchBarHeight = SEARCH_BAR_HEIGHT;
		int textX = horizontalPadding;
		
		// Draw tab buttons above overlay (at negative Y, aligned with top edge)
		int buttonY = -buttonHeight;
		int tabCount = 3;
		int buttonWidth = unscaledWidth / tabCount;
		String[] tabLabels = {"Einzelne", "Kombiniert", "Gesamt"};
		MkLevelOverlayTab[] tabs = {MkLevelOverlayTab.INDIVIDUAL, MkLevelOverlayTab.COMBINED, MkLevelOverlayTab.TOTAL_ESSENCES};
		
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
		
		int searchBarY = VERTICAL_INSET;
		int contentY = contentTop();
		int maxY = contentBottom(unscaledHeight);
		
		// Draw background (scaled)
		context.fill(0, 0, unscaledWidth, unscaledHeight, 0x80000000);
		
		// Draw border (scaled)
		context.outline(0, 0, unscaledWidth, unscaledHeight, 0xFFFFFFFF);
		
		// Draw search bar background (relative to matrix)
		int searchBarX = horizontalPadding;
		int searchBarWidth = unscaledWidth - horizontalPadding * 2;
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
		int availableHeight = availableContentHeight(unscaledHeight);
		
		List<MkLevelRenderLine> allLines = buildMkLevelRenderLines(client);
		renderMkLevelScrollableLines(context, client, allLines, textX, contentY, maxY, availableHeight, lineHeight);
		
		// Draw scrollbar on the right side
		// Scrollbar goes from bottom of overlay to bottom of search bar
		int scrollbarWidth = 6; // Width of scrollbar
		int scrollbarX = unscaledWidth - scrollbarWidth - horizontalPadding;
		int scrollbarTop = contentY; // Start below search bar
		int scrollbarBottom = maxY;
		int scrollbarHeight = scrollbarBottom - scrollbarTop;
		
		// Only show scrollbar if there's content to scroll
		if (scrollbarHeight > 0) {
			// Calculate total content height using the same method as rendering
			// We need to count the actual number of lines that will be rendered
			int totalContentHeight = calculateMkLevelTotalLineHeight(lineHeight);
			
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
					(float) getMkLevelScrollOffset() / maxScroll : 0.0f;
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

	static int[] calculateMkLevelContentHeights(int lineHeight) {
		int totalHeight = 0;
		int lastEntryHeight = 0;
		
		switch (mkLevelActiveTab) {
			case INDIVIDUAL -> {
				List<MkLevelInfo> entries = filterMkLevelEntries(mkLevelSearchText);
				int height = 5 * lineHeight;
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
				int height = 4 * lineHeight;
				totalHeight += entries.size() * height;
				if (!entries.isEmpty()) {
					lastEntryHeight = height;
				}
			}
		}
		
		return new int[] { totalHeight, lastEntryHeight };
	}

	static int calculateMkLevelTotalLineHeight(int lineHeight) {
		switch (mkLevelActiveTab) {
			case INDIVIDUAL -> {
				return filterMkLevelEntries(mkLevelSearchText).size() * 5 * lineHeight;
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
				return filterTotalEssenceEntries(mkLevelSearchText).size() * 4 * lineHeight;
			}
			default -> {
				return 0;
			}
		}
	}

	public static List<MkLevelRenderLine> buildMkLevelRenderLines(Minecraft client) {
		List<MkLevelRenderLine> allLines = new ArrayList<>();
		
		switch (mkLevelActiveTab) {
			case INDIVIDUAL -> {
				for (MkLevelInfo levelInfo : filterMkLevelEntries(mkLevelSearchText)) {
					allLines.add(new MkLevelRenderLine("-Level " + levelInfo.level, 0xFFFFFF00));
					allLines.add(new MkLevelRenderLine(" " + levelInfo.essence + ", x" + formatNumberWithSeparator(levelInfo.amount), 0xFFFFFFFF));
					addBagLine(allLines, client, levelInfo.essence);
					String waveDisplay = getWaveDisplayForMkLevelEssence(levelInfo.essence);
					if (waveDisplay != null) {
						String wavePrefix = "-> Welle: ";
						allLines.add(new MkLevelRenderLine(wavePrefix, 0xFFC0C0C0, 0, waveDisplay, 0xFF55FF55, client.font.width(wavePrefix)));
					}
					allLines.add(new MkLevelRenderLine("", 0xFFFFFFFF));
				}
			}
			case COMBINED -> {
				for (CombinedWaveInfo waveInfo : filterCombinedWaves(mkLevelSearchText)) {
					allLines.add(new MkLevelRenderLine("-Level (" + waveInfo.level + ")", 0xFFFFFF00));
					String wavePrefix = "-> Welle: ";
					String waveNumber = String.valueOf(waveInfo.wave);
					allLines.add(new MkLevelRenderLine(wavePrefix, 0xFFC0C0C0, 0, waveNumber, 0xFF55FF55, client.font.width(wavePrefix)));
					
					if (waveInfo.without != null && !waveInfo.without.isEmpty()) {
						String withoutHeader = "Ohne: ";
						int withoutHeaderWidth = client.font.width(withoutHeader);
						
						String firstEssence = waveInfo.without.get(0).trim();
						String essenceWaveDisplay = getWaveDisplayForMkLevelEssence(firstEssence);
						if (essenceWaveDisplay != null) {
							String wavePrefixText = " -> Welle: ";
							int essenceNameWidth = client.font.width(firstEssence);
							int wavePrefixX = withoutHeaderWidth + essenceNameWidth;
							int waveNumX = wavePrefixX + client.font.width(wavePrefixText);
							allLines.add(new MkLevelRenderLine(withoutHeader + firstEssence, 0xFFFF5555, 0, wavePrefixText, 0xFFC0C0C0, wavePrefixX, essenceWaveDisplay, 0xFF55FF55, waveNumX));
						} else {
							allLines.add(new MkLevelRenderLine(withoutHeader + firstEssence, 0xFFFF5555));
						}
						
						for (int i = 1; i < waveInfo.without.size(); i++) {
							String essenceName = waveInfo.without.get(i).trim();
							String waveDisplay = getWaveDisplayForMkLevelEssence(essenceName);
							if (waveDisplay != null) {
								String wavePrefixText = " -> Welle: ";
								int essenceNameWidth = client.font.width(essenceName);
								int wavePrefixX = withoutHeaderWidth + essenceNameWidth;
								int waveNumX = wavePrefixX + client.font.width(wavePrefixText);
								allLines.add(new MkLevelRenderLine(essenceName, 0xFFFF5555, withoutHeaderWidth, wavePrefixText, 0xFFC0C0C0, wavePrefixX, waveDisplay, 0xFF55FF55, waveNumX));
							} else {
								allLines.add(new MkLevelRenderLine(essenceName, 0xFFFF5555, withoutHeaderWidth, "", 0, 0));
							}
						}
					}
					
					allLines.add(new MkLevelRenderLine("", 0xFFFFFFFF));
				}
			}
			case TOTAL_ESSENCES -> {
				for (TotalEssenceInfo essenceInfo : filterTotalEssenceEntries(mkLevelSearchText)) {
					allLines.add(new MkLevelRenderLine(" " + essenceInfo.essence + ", x" + formatNumberWithSeparator(essenceInfo.totalAmount), 0xFFFFFFFF));
					addBagLine(allLines, client, essenceInfo.essence);
					String waveDisplay = getWaveDisplayForMkLevelEssence(essenceInfo.essence);
					if (waveDisplay != null) {
						String wavePrefix = "-> Welle: ";
						allLines.add(new MkLevelRenderLine(wavePrefix, 0xFFC0C0C0, 0, waveDisplay, 0xFF55FF55, client.font.width(wavePrefix)));
					}
					allLines.add(new MkLevelRenderLine("", 0xFFFFFFFF));
				}
			}
		}
		
		return allLines;
	}

	/** Owned amount of one essence, between the essence line and the wave line. */
	private static void addBagLine(List<MkLevelRenderLine> lines, Minecraft client, String essence) {
		String prefix = "Tasche: ";
		String amount = formatNumberWithSeparator(bagAmount(essence));
		lines.add(new MkLevelRenderLine(prefix, 0xFFFFFFFF, 0, amount, 0xFF55FF55, client.font.width(prefix)));
	}

	private static long bagAmount(String mkLevelEssence) {
		long found = 0L;
		for (String key : EssenceWaveNames.storageKeysForMkLevelName(mkLevelEssence)) {
			found = Math.max(found, CollectedMaterialsResourcesStorage.getEssenceAmount(key));
		}
		return found;
	}

	private static void renderMkLevelScrollableLines(GuiGraphicsExtractor context, Minecraft client, List<MkLevelRenderLine> allLines,
			int textX, int contentY, int maxY, int availableHeight, int lineHeight) {
		int totalHeight = allLines.size() * lineHeight;
		int maxScrollOffset = Math.max(0, totalHeight - availableHeight);
		
		int scrollOffset = getMkLevelScrollOffset();
		if (scrollOffset > maxScrollOffset) {
			setMkLevelScrollOffset(maxScrollOffset);
			scrollOffset = maxScrollOffset;
		}
		
		int startLine = scrollOffset / lineHeight;
		int pixelOffset = scrollOffset % lineHeight;
		int textY = contentY;
		
		if (startLine > 0) {
			String moreText = mkLevelActiveTab == MkLevelOverlayTab.TOTAL_ESSENCES
				? "↑ Weitere Essenzen (Scrollen)"
				: "↑ Weitere Level (Scrollen)";
			context.text(client.font, moreText, textX, textY, 0x80FFFFFF, true);
			textY += lineHeight;
		}
		
		textY -= pixelOffset;
		int minRenderY = startLine > 0 ? contentY + lineHeight : contentY;
		int fontHeight = client.font.lineHeight;
		boolean hasMoreLines = scrollOffset < maxScrollOffset;
		int contentLimit = hasMoreLines ? maxY - fontHeight : maxY;
		int firstTextY = textY;
		int fitted = 0;
		int probeY = textY;
		for (int i = startLine; i < allLines.size(); i++) {
			if (probeY + fontHeight > contentLimit) {
				break;
			}
			fitted++;
			probeY += lineHeight;
		}
		if (fitted > 0) {
			int lastGlyphBottom = firstTextY + (fitted - 1) * lineHeight + fontHeight;
			int leftover = contentLimit - lastGlyphBottom;
			if (leftover > 0) {
				textY += Math.min(leftover, lineHeight - fontHeight);
			}
		}

		for (int i = startLine; i < allLines.size(); i++) {
			if (textY + fontHeight > contentLimit) {
				break;
			}
			if (textY + fontHeight > minRenderY) {
				MkLevelRenderLine line = allLines.get(i);
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

		if (hasMoreLines) {
			String moreText = mkLevelActiveTab == MkLevelOverlayTab.TOTAL_ESSENCES
				? "↓ Weitere Essenzen (Scrollen)"
				: "↓ Weitere Level (Scrollen)";
			context.text(client.font, moreText, textX, maxY - fontHeight, 0x80FFFFFF, true);
		}
	}
}
