package net.felix.features.information.mklevel;

import net.felix.CCLiveUtilitiesConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.Set;
import net.felix.utilities.Overall.KeyBindingUtility;
import net.felix.utilities.Overall.SearchBarUtility;
import net.felix.utilities.Overall.ZeichenUtility;

import static net.felix.features.information.mklevel.MKLevelOverlayRenderer.calculateMKLevelContentHeights;
import static net.felix.features.information.mklevel.MKLevelOverlayRenderer.calculateMKLevelTotalLineHeight;
import static net.felix.features.information.mklevel.MKLevelState.getMKLevelScrollOffset;
import static net.felix.features.information.mklevel.MKLevelState.isInMKLevelInventory;
import static net.felix.features.information.mklevel.MKLevelState.mkLevelActiveTab;
import static net.felix.features.information.mklevel.MKLevelState.mkLevelOverlayHovered;
import static net.felix.features.information.mklevel.MKLevelState.mkLevelScrollbarDragging;
import static net.felix.features.information.mklevel.MKLevelState.mkLevelSearchCursorBlinkTime;
import static net.felix.features.information.mklevel.MKLevelState.mkLevelSearchCursorPosition;
import static net.felix.features.information.mklevel.MKLevelState.mkLevelSearchCursorVisible;
import static net.felix.features.information.mklevel.MKLevelState.mkLevelSearchFocused;
import static net.felix.features.information.mklevel.MKLevelState.mkLevelSearchText;
import static net.felix.features.information.mklevel.MKLevelState.setMKLevelScrollOffset;
import static net.felix.features.information.tooltip.TooltipText.getPlainTextFromText;


/**
 * Maus- und Tastatureingaben des MK-Level-Overlays (Scrollen, Suche).
 */
public final class MKLevelInputHandler {

	private MKLevelInputHandler() {
	}

	/**
	 * Handles scrolling for MKLevel overlay
	 */
	private static void handleMKLevelScrolling(Minecraft client, int inventoryX, int inventoryY, int inventoryWidth, int inventoryHeight) {
		if (client == null || client.getWindow() == null) {
			return;
		}
		
		int windowWidth = client.getWindow().getScreenWidth();
		int windowHeight = client.getWindow().getScreenHeight();
		
		if (windowWidth <= 0 || windowHeight <= 0) {
			return;
		}
		
		int mouseX = (int) client.mouseHandler.xpos() * client.getWindow().getGuiScaledWidth() / windowWidth;
		int mouseY = (int) client.mouseHandler.ypos() * client.getWindow().getGuiScaledHeight() / windowHeight;
		
		// Calculate overlay position using config (same as renderMKLevelOverlay)
		int screenWidth = client.getWindow().getGuiScaledWidth();
		
		if (screenWidth <= 0) {
			return;
		}
		
		// Get position from config (same as renderMKLevelOverlay)
		int xPos = CCLiveUtilitiesConfig.HANDLER.instance().mkLevelX;
		int yOffset = CCLiveUtilitiesConfig.HANDLER.instance().mkLevelY;
		float scale = CCLiveUtilitiesConfig.HANDLER.instance().mkLevelScale;
		if (scale <= 0) scale = 1.0f;
		
		// Unscaled dimensions
		int unscaledWidth = 200;
		int unscaledHeight = inventoryHeight;
		
		// Scaled dimensions
		int overlayWidth = Math.round(unscaledWidth * scale);
		int overlayHeight = Math.round(unscaledHeight * scale);
		
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
		
		// Check if mouse is over the overlay (excluding search bar area for scrolling)
		// Coordinates need to account for scaling
		int searchBarHeight = Math.round(16 * scale);
		int padding = Math.round(5 * scale);
		int contentOffset = Math.round(3 * scale); // Shift content 3px up (same as in render)
		int searchBarY = overlayY + padding - contentOffset; // Shift search bar 3px up
		int contentY = searchBarY + searchBarHeight + Math.round(2 * scale);
		
		// Mouse is over overlay if it's in the content area (not search bar) or search bar itself
		boolean overSearchBar = mouseX >= overlayX + padding && mouseX <= overlayX + overlayWidth - padding &&
								mouseY >= searchBarY && mouseY <= searchBarY + searchBarHeight;
		boolean overContent = mouseX >= overlayX && mouseX <= overlayX + overlayWidth &&
							  mouseY >= contentY && mouseY <= overlayY + overlayHeight - padding;
		
		mkLevelOverlayHovered = overSearchBar || overContent;
	}

	/**
	 * Handles mouse scroll for MKLevel overlay
	 */
	public static void onMKLevelMouseScroll(double vertical) {
		if (!isInMKLevelInventory) {
			return;
		}
		
		if (mkLevelOverlayHovered) {
			// Scroll by line height for smooth line-by-line scrolling
			int lineHeight = 12;
			int scrollAmount = (int) (vertical * lineHeight);
			int currentOffset = getMKLevelScrollOffset();
			currentOffset -= scrollAmount;
			currentOffset = Math.max(0, currentOffset);
			setMKLevelScrollOffset(currentOffset);
			
			// Limit scroll offset based on actual entry heights (including spacing)
			Minecraft client = Minecraft.getInstance();
			if (client != null && client.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> handledScreen) {
				try {
					java.lang.reflect.Field bgHeightField = net.minecraft.client.gui.screens.inventory.AbstractContainerScreen.class.getDeclaredField("imageHeight");
					bgHeightField.setAccessible(true);
					int inventoryHeight = bgHeightField.getInt(handledScreen);

					int padding = 5;
					int searchBarHeight = 16;
					int contentOffset = 3; // Same as in render
					// Account for contentOffset in available height calculation
					int availableHeight = inventoryHeight - padding * 2 - searchBarHeight - 2 - contentOffset;
					
					int[] heights = calculateMKLevelContentHeights(lineHeight);
					int totalHeight = heights[0];
					int lastEntryHeight = heights[1];
					int maxScroll = Math.max(0, totalHeight - availableHeight + lastEntryHeight);
					setMKLevelScrollOffset(Math.min(getMKLevelScrollOffset(), maxScroll));
				} catch (Exception e) {
					int[] heights = calculateMKLevelContentHeights(lineHeight);
					int totalHeight = heights[0];
					int lastEntryHeight = heights[1];
					int estimatedAvailableHeight = 166 - 16 - 10 - 3;
					int maxScroll = Math.max(0, totalHeight - estimatedAvailableHeight + lastEntryHeight);
					setMKLevelScrollOffset(Math.min(getMKLevelScrollOffset(), maxScroll));
				}
			} else {
				int[] heights = calculateMKLevelContentHeights(lineHeight);
				int totalHeight = heights[0];
				int lastEntryHeight = heights[1];
				int estimatedAvailableHeight = 166 - 16 - 10 - 3;
				int maxScroll = Math.max(0, totalHeight - estimatedAvailableHeight + lastEntryHeight);
				setMKLevelScrollOffset(Math.min(getMKLevelScrollOffset(), maxScroll));
			}
		}
	}

	/**
	 * Handles mouse click for MKLevel search bar
	 * @param inventoryX X position of the inventory (from mixin shadow field)
	 * @param inventoryY Y position of the inventory (from mixin shadow field)
	 * @param inventoryHeight Height of the inventory (from mixin shadow field)
	 */
	public static boolean handleMKLevelSearchClick(double mouseX, double mouseY, int button, int inventoryX, int inventoryY, int inventoryHeight) {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.getWindow() == null) {
			return false;
		}
		
		// Check if we're in the MKLevel inventory - check directly instead of relying on isInMKLevelInventory
		// This ensures it works even if onClientTick hasn't run yet
		boolean inMKLevelInventory = false;
		if (client.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> handledScreen) {
			Component titleText = handledScreen.getTitle();
			String title = getPlainTextFromText(titleText);
			String titleWithUnicode = titleText.getString(); // Behält Unicode-Zeichen für Essence Harvester UI
			// Prüfe sowohl "Machtkristalle Verbessern" als auch Essence Harvester UI
			inMKLevelInventory = ZeichenUtility.isMkLevelInventoryTitle(title, titleWithUnicode);
		}
		
		if (!inMKLevelInventory) {
			return false;
		}
		
		// Check if MKLevel overlay is enabled
		if (!CCLiveUtilitiesConfig.HANDLER.instance().mkLevelEnabled) {
			return false;
		}
		
		int screenWidth = client.getWindow().getGuiScaledWidth();
		int xPos = CCLiveUtilitiesConfig.HANDLER.instance().mkLevelX;
		float scale = CCLiveUtilitiesConfig.HANDLER.instance().mkLevelScale;
		if (scale <= 0) scale = 1.0f;
		
		// Unscaled dimensions (width is fixed, height will be set from inventory)
		int unscaledWidth = 200;
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
		
		// Use inventory position and height passed from mixin (from @Shadow fields)
		int yOffset = CCLiveUtilitiesConfig.HANDLER.instance().mkLevelY;
		int overlayY = (yOffset == -1) ? inventoryY : yOffset;
		
		// Use the same unscaledHeight as in render (inventoryHeight)
		int unscaledHeight = inventoryHeight;
		
		// Button dimensions (unscaled, same as in render)
		int unscaledButtonHeight = 20;
		int tabCount = 3;
		int unscaledButtonWidth = unscaledWidth / tabCount;
		
		int buttonY = overlayY - Math.round(unscaledButtonHeight * scale);
		int buttonHeight = Math.round(unscaledButtonHeight * scale);
		MKLevelOverlayTab[] tabs = {MKLevelOverlayTab.INDIVIDUAL, MKLevelOverlayTab.COMBINED, MKLevelOverlayTab.TOTAL_ESSENCES};
		
		for (int i = 0; i < tabCount; i++) {
			int buttonX = overlayX + Math.round(i * unscaledButtonWidth * scale);
			int buttonWidth = Math.round(unscaledButtonWidth * scale);
			if (mouseX >= buttonX && mouseX <= buttonX + buttonWidth &&
				mouseY >= buttonY && mouseY <= buttonY + buttonHeight) {
				if (button == 0) {
					mkLevelActiveTab = tabs[i];
					return true;
				}
			}
		}
		
		// Calculate search bar position in screen coordinates (same as SearchBarUtility)
		// In render: searchBarX = padding (5), searchBarY = padding - contentOffset (5 - 3 = 2)
		// These are unscaled coordinates within the matrix, so we need to scale them
		int unscaledPadding = 5;
		int unscaledSearchBarHeight = 16;
		int unscaledContentOffset = 3; // Shift content 3px up (same as in render)
		int unscaledSearchBarWidth = unscaledWidth - unscaledPadding * 2;
		
		// Calculate absolute screen coordinates of search bar
		// The search bar is rendered at (padding, padding - contentOffset) within the matrix
		// After transformation: (overlayX + padding * scale, overlayY + (padding - contentOffset) * scale)
		int searchBarX = overlayX + Math.round(unscaledPadding * scale);
		int searchBarY = overlayY + Math.round((unscaledPadding - unscaledContentOffset) * scale);
		int searchBarWidth = Math.round(unscaledSearchBarWidth * scale);
		int searchBarHeight = Math.round(unscaledSearchBarHeight * scale);
		
		// Check if click is on search bar (in screen coordinates)
		if (mouseX >= searchBarX && mouseX <= searchBarX + searchBarWidth &&
			mouseY >= searchBarY && mouseY <= searchBarY + searchBarHeight) {
			
			if (button == 0) { // Left click
				mkLevelSearchFocused = true;
				// Set cursor position based on click (in screen coordinates, then convert to text position)
				String textBeforeClick = mkLevelSearchText;
				// Calculate click position relative to search bar, then scale down for text measurement
				int clickX = (int) ((mouseX - searchBarX - 2) / scale);
				mkLevelSearchCursorPosition = findCursorPosition(client, textBeforeClick, clickX);
				return true;
			} else if (button == 1) { // Right click - clear search
				mkLevelSearchText = "";
				mkLevelSearchCursorPosition = 0;
				setMKLevelScrollOffset(0);
				return true;
			}
		} else {
			// Click outside search bar - unfocus
			mkLevelSearchFocused = false;
		}
		
		// Check if click is on scrollbar
		int scrollbarWidth = 6;
		int scrollbarX = overlayX + Math.round((unscaledWidth - scrollbarWidth - unscaledPadding) * scale);
		int scrollbarTop = overlayY + Math.round((unscaledPadding - unscaledContentOffset + unscaledSearchBarHeight + 2) * scale);
		int scrollbarBottom = overlayY + Math.round((unscaledHeight - unscaledPadding) * scale);
		int scrollbarHeight = scrollbarBottom - scrollbarTop;
		
		if (mouseX >= scrollbarX && mouseX <= scrollbarX + Math.round(scrollbarWidth * scale) &&
			mouseY >= scrollbarTop && mouseY <= scrollbarBottom) {
			
			if (button == 0) { // Left click
				// Calculate total content height
				int lineHeight = 12;
				int totalContentHeight = calculateMKLevelTotalLineHeight(lineHeight);
				int availableHeight = unscaledHeight - unscaledPadding * 2 - unscaledSearchBarHeight - 2 - unscaledContentOffset;
				
				if (totalContentHeight > availableHeight) {
					// Calculate handle size and position
					float scrollRatio = (float) availableHeight / totalContentHeight;
					int handleHeight = Math.max(10, (int) (scrollbarHeight * scrollRatio));
					
					// Calculate current handle position
					float scrollProgress = (float) getMKLevelScrollOffset() / (totalContentHeight - availableHeight);
					scrollProgress = Math.max(0.0f, Math.min(1.0f, scrollProgress));
					int handleY = scrollbarTop + (int) ((scrollbarHeight - handleHeight) * scrollProgress);
					
					// Check if click is on handle
					if (mouseY >= handleY && mouseY <= handleY + Math.round(handleHeight * scale)) {
						// Start dragging
						mkLevelScrollbarDragging = true;
						return true;
					} else {
						// Click on scrollbar track - jump to position
						float clickProgress = (float) (mouseY - scrollbarTop) / scrollbarHeight;
						clickProgress = Math.max(0.0f, Math.min(1.0f, clickProgress));
						int newOffset = (int) (clickProgress * (totalContentHeight - availableHeight));
						setMKLevelScrollOffset(Math.max(0, Math.min(newOffset, totalContentHeight - availableHeight)));
						return true;
					}
				}
			}
		}
		
		return false;
	}

	/**
	 * Handles mouse drag for MKLevel scrollbar
	 */
	public static boolean handleMKLevelScrollbarDrag(double mouseX, double mouseY, int button, int inventoryX, int inventoryY, int inventoryHeight) {
		if (!mkLevelScrollbarDragging || button != 0) {
			return false;
		}
		
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.getWindow() == null) {
			return false;
		}
		
		int yOffset = CCLiveUtilitiesConfig.HANDLER.instance().mkLevelY;
		float scale = CCLiveUtilitiesConfig.HANDLER.instance().mkLevelScale;
		if (scale <= 0) scale = 1.0f;
		
		int unscaledHeight = inventoryHeight;
		int overlayY = (yOffset == -1) ? inventoryY : yOffset;
		
		int padding = 5;
		int lineHeight = 12;
		int searchBarHeight = 16;
		int contentOffset = 3;
		
		// Calculate scrollbar position
		int scrollbarTop = overlayY + Math.round((padding - contentOffset + searchBarHeight + 2) * scale);
		int scrollbarBottom = overlayY + Math.round((unscaledHeight - padding) * scale);
		int scrollbarHeight = scrollbarBottom - scrollbarTop;
		
		// Calculate total content height
		int availableHeight = unscaledHeight - padding * 2 - searchBarHeight - 2 - contentOffset;
		int totalContentHeight = calculateMKLevelTotalLineHeight(lineHeight);
		
		if (totalContentHeight <= availableHeight) {
			return false;
		}
		
		// Calculate new scroll position based on mouse Y position
		float scrollRatio = (float) availableHeight / totalContentHeight;
		int handleHeight = Math.max(10, (int) (scrollbarHeight * scrollRatio));
		int scrollableHeight = scrollbarHeight - handleHeight;
		
		// Calculate progress based on mouse position
		float progress = (float) (mouseY - scrollbarTop - handleHeight / 2) / scrollableHeight;
		progress = Math.max(0.0f, Math.min(1.0f, progress));
		
		// Calculate new scroll offset
		int maxScroll = totalContentHeight - availableHeight;
		int newOffset = (int) (progress * maxScroll);
		setMKLevelScrollOffset(Math.max(0, Math.min(newOffset, maxScroll)));
		
		return true;
	}

	/**
	 * Handles mouse release for MKLevel scrollbar
	 */
	public static boolean handleMKLevelScrollbarRelease(double mouseX, double mouseY, int button) {
		if (mkLevelScrollbarDragging && button == 0) {
			mkLevelScrollbarDragging = false;
			return true;
		}
		return false;
	}

	/**
	 * Finds cursor position based on click X position
	 */
	private static int findCursorPosition(Minecraft client, String text, int clickX) {
		if (text.isEmpty()) {
			return 0;
		}
		
		for (int i = 0; i <= text.length(); i++) {
			String substring = text.substring(0, i);
			int width = client.font.width(substring);
			if (width >= clickX) {
				// Check if we're closer to this position or the previous one
				if (i > 0) {
					int prevWidth = client.font.width(text.substring(0, i - 1));
					if (clickX - prevWidth < width - clickX) {
						return i - 1;
					}
				}
				return i;
			}
		}
		
		return text.length();
	}

	/**
	 * Handles character input for MKLevel search bar
	 */
	public static boolean handleMKLevelCharTyped(char chr, int modifiers) {
		if (!isInMKLevelInventory || !mkLevelSearchFocused) {
			return false;
		}
		
		// Handle backspace
		if (chr == '\b') {
			if (!mkLevelSearchText.isEmpty() && mkLevelSearchCursorPosition > 0) {
				mkLevelSearchText = mkLevelSearchText.substring(0, mkLevelSearchCursorPosition - 1) +
									mkLevelSearchText.substring(mkLevelSearchCursorPosition);
				mkLevelSearchCursorPosition--;
				setMKLevelScrollOffset(0); // Reset scroll when search changes
			}
			return true;
		}
		
		// Handle delete
		if (chr == 127) {
			if (mkLevelSearchCursorPosition < mkLevelSearchText.length()) {
				mkLevelSearchText = mkLevelSearchText.substring(0, mkLevelSearchCursorPosition) +
									mkLevelSearchText.substring(mkLevelSearchCursorPosition + 1);
				setMKLevelScrollOffset(0); // Reset scroll when search changes
			}
			return true;
		}
		
		// Handle printable characters
		if (chr >= 32 && chr != 127) {
			mkLevelSearchText = mkLevelSearchText.substring(0, mkLevelSearchCursorPosition) +
								chr +
								mkLevelSearchText.substring(mkLevelSearchCursorPosition);
			mkLevelSearchCursorPosition++;
			setMKLevelScrollOffset(0); // Reset scroll when search changes
			return true;
		}
		
		return false;
	}

	/**
	 * Handles key press for MKLevel search bar
	 * Based on SearchBarUtility.handleKeyPress
	 */
	public static boolean handleMKLevelKeyPressed(int keyCode, int scanCode, int modifiers) {
		// Check if search bar is focused - if it is, we're definitely in the MKLevel inventory
		if (!mkLevelSearchFocused) {
			return false;
		}
		
		Minecraft client = Minecraft.getInstance();
		if (client == null) {
			return false;
		}
		
		// Double-check that we're still in the MKLevel inventory
		boolean inMKLevelInventory = false;
		if (client.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> handledScreen) {
			Component titleText = handledScreen.getTitle();
			String title = getPlainTextFromText(titleText);
			String titleWithUnicode = titleText.getString(); // Behält Unicode-Zeichen für Essence Harvester UI
			// Prüfe sowohl "Machtkristalle Verbessern" als auch Essence Harvester UI
			inMKLevelInventory = ZeichenUtility.isMkLevelInventoryTitle(title, titleWithUnicode);
		}
		
		if (!inMKLevelInventory) {
			// Unfocus if we're no longer in the inventory
			mkLevelSearchFocused = false;
			return false;
		}
		
		// Cursor sichtbar machen bei Tastendruck
		mkLevelSearchCursorVisible = true;
		mkLevelSearchCursorBlinkTime = System.currentTimeMillis();
		
		boolean ctrl = KeyBindingUtility.isCtrlDown(modifiers) && !KeyBindingUtility.isAltGrDown(modifiers);

		if (keyCode == InputConstants.KEY_ESCAPE) {
			mkLevelSearchFocused = false;
			return true;
		}
		
		if (keyCode == InputConstants.KEY_BACKSPACE) {
			if (ctrl) {
				mkLevelSearchText = "";
				mkLevelSearchCursorPosition = 0;
				setMKLevelScrollOffset(0);
			} else if (!mkLevelSearchText.isEmpty() && mkLevelSearchCursorPosition > 0) {
				mkLevelSearchText = mkLevelSearchText.substring(0, mkLevelSearchCursorPosition - 1) +
									mkLevelSearchText.substring(mkLevelSearchCursorPosition);
				mkLevelSearchCursorPosition--;
				setMKLevelScrollOffset(0);
			}
			return true;
		}
		
		if (keyCode == InputConstants.KEY_DELETE) {
			if (mkLevelSearchCursorPosition < mkLevelSearchText.length()) {
				mkLevelSearchText = mkLevelSearchText.substring(0, mkLevelSearchCursorPosition) +
									mkLevelSearchText.substring(mkLevelSearchCursorPosition + 1);
				setMKLevelScrollOffset(0);
			}
			return true;
		}
		
		if (keyCode == InputConstants.KEY_LEFT) {
			if (mkLevelSearchCursorPosition > 0) {
				mkLevelSearchCursorPosition--;
			}
			return true;
		}
		
		if (keyCode == InputConstants.KEY_RIGHT) {
			if (mkLevelSearchCursorPosition < mkLevelSearchText.length()) {
				mkLevelSearchCursorPosition++;
			}
			return true;
		}
		
		if (keyCode == InputConstants.KEY_HOME) {
			mkLevelSearchCursorPosition = 0;
			return true;
		}
		
		if (keyCode == InputConstants.KEY_END) {
			mkLevelSearchCursorPosition = mkLevelSearchText.length();
			return true;
		}
		
		if (ctrl && keyCode == InputConstants.KEY_C) {
			if (!mkLevelSearchText.isEmpty()) {
				client.keyboardHandler.setClipboard(mkLevelSearchText);
			}
			return true;
		}
		if (ctrl && keyCode == InputConstants.KEY_V) {
			String clipboardText = client.keyboardHandler.getClipboard();
			if (clipboardText != null && !clipboardText.isEmpty()) {
				mkLevelSearchText = mkLevelSearchText.substring(0, mkLevelSearchCursorPosition) +
									clipboardText +
									mkLevelSearchText.substring(mkLevelSearchCursorPosition);
				mkLevelSearchCursorPosition += clipboardText.length();
				setMKLevelScrollOffset(0);
			}
			return true;
		}
		if (ctrl) {
			return true;
		}

		String typed = KeyBindingUtility.characterFromScancode(keyCode, modifiers);
		if (typed != null && !typed.isEmpty()) {
			for (int i = 0; i < typed.length(); i++) {
				insertMKLevelCharacter(typed.charAt(i));
			}
			return true;
		}
		
		return false;
	}

	/**
	 * Inserts a character at the cursor position
	 */
	private static void insertMKLevelCharacter(char character) {
		mkLevelSearchText = mkLevelSearchText.substring(0, mkLevelSearchCursorPosition) +
							character +
							mkLevelSearchText.substring(mkLevelSearchCursorPosition);
		mkLevelSearchCursorPosition++;
		setMKLevelScrollOffset(0); // Reset scroll when search changes
	}

	/**
	 * Tick-Update: erkennt das MK-Level-Inventar, setzt die Suche beim Verlassen zurueck,
	 * steuert das Cursor-Blinken und verarbeitet das Scrollen.
	 */
	public static void onClientTick(Minecraft client) {
		// Check if we're in MKLevel inventory
		if (client.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> handledScreen) {
			Component titleText = handledScreen.getTitle();
			String title = getPlainTextFromText(titleText);
			String titleWithUnicode = titleText.getString(); // Behält Unicode-Zeichen für Essence Harvester UI
			boolean wasInMKLevelInventory = isInMKLevelInventory;
			// Prüfe sowohl "Machtkristalle Verbessern" als auch Essence Harvester UI
			isInMKLevelInventory = ZeichenUtility.isMkLevelInventoryTitle(title, titleWithUnicode);

			// Reset search when leaving inventory (nur wenn man wirklich ein anderes Inventar öffnet)
			// Die Scroll-Position wird NICHT zurückgesetzt, damit sie beim erneuten Öffnen erhalten bleibt
			if (wasInMKLevelInventory && !isInMKLevelInventory) {
				mkLevelSearchText = "";
				mkLevelSearchFocused = false;
				mkLevelSearchCursorPosition = 0;
			}

			if (isInMKLevelInventory) {
				// Update cursor blink
				if (mkLevelSearchFocused) {
					long currentTime = System.currentTimeMillis();
					if (currentTime - mkLevelSearchCursorBlinkTime > 500) {
						mkLevelSearchCursorVisible = !mkLevelSearchCursorVisible;
						mkLevelSearchCursorBlinkTime = currentTime;
					}
				} else {
					mkLevelSearchCursorVisible = false;
				}
				// Get actual inventory dimensions using reflection
				try {
					java.lang.reflect.Field xField = net.minecraft.client.gui.screens.inventory.AbstractContainerScreen.class.getDeclaredField("leftPos");
					java.lang.reflect.Field yField = net.minecraft.client.gui.screens.inventory.AbstractContainerScreen.class.getDeclaredField("topPos");
					java.lang.reflect.Field bgWidthField = net.minecraft.client.gui.screens.inventory.AbstractContainerScreen.class.getDeclaredField("imageWidth");
					java.lang.reflect.Field bgHeightField = net.minecraft.client.gui.screens.inventory.AbstractContainerScreen.class.getDeclaredField("imageHeight");

					xField.setAccessible(true);
					yField.setAccessible(true);
					bgWidthField.setAccessible(true);
					bgHeightField.setAccessible(true);

					int inventoryX = xField.getInt(handledScreen);
					int inventoryY = yField.getInt(handledScreen);
					int inventoryWidth = bgWidthField.getInt(handledScreen);
					int inventoryHeight = bgHeightField.getInt(handledScreen);

					handleMKLevelScrolling(client, inventoryX, inventoryY, inventoryWidth, inventoryHeight);
				} catch (Exception e) {
					// Fallback to default values
					int inventoryWidth = 176;
					int inventoryHeight = 166;
					int inventoryX = (client.getWindow().getGuiScaledWidth() - inventoryWidth) / 2;
					int inventoryY = (client.getWindow().getGuiScaledHeight() - inventoryHeight) / 2;
					handleMKLevelScrolling(client, inventoryX, inventoryY, inventoryWidth, inventoryHeight);
				}
			}
		} else {
			isInMKLevelInventory = false;
		}
	}
}
