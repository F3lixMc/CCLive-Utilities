package net.felix.features.information.aspects;

import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.Overall.aspekte.AspectOverlay;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

import static net.felix.features.information.aspects.AspectDatabase.getAspectInfoForBlueprintFull;
import static net.felix.features.information.blueprints.BlueprintChatParser.extractBlueprintNameFromChatMessage;
import static net.felix.features.information.blueprints.BlueprintChatParser.hasEpicColor;
import static net.felix.features.information.hover.HoverEventFactory.createHoverEventForAspect;
import static net.felix.features.information.hover.HoverEventFactory.modifyTextWithHoverEvent;
import static net.felix.features.information.hover.HoverEventReader.extractHoverTextFromEvent;
import static net.felix.features.information.hover.HoverEventReader.findHoverEventInText;

/**
 * Aspekt-Informationen fuer Chatnachrichten und das Aspekt-Overlay.
 */
public final class AspectChatMessages {

	private AspectChatMessages() {
	}

	/**
	 * Modifies chat messages to add aspect information to hover events
	 * Called from ClientPacketListenerTestMixin
	 * Adds aspect info and "Shift für Info" to blueprint hover events
	 * If chat aspect overlay is enabled, no lines are added to hover events
	 */
	public static Component modifyChatMessageForAspectInfo(Component message) {
		if (message == null) {
			return null;
		}
		
		// Check if message contains [Bauplan]
		String fullMessageText = message.getString();
		if (fullMessageText == null || !fullMessageText.contains("[Bauplan]")) {
			return null; // Not a blueprint message
		}
		
		// Check if aspect overlay is enabled
		if (!CCLiveUtilitiesConfig.HANDLER.instance().aspectOverlayEnabled) {
			return null; // Aspect overlay disabled
		}
		
		// If chat aspect overlay is disabled, don't add lines to hover events
		// When enabled, both hover event lines and overlay will be shown
		if (!CCLiveUtilitiesConfig.HANDLER.instance().chatAspectOverlayEnabled) {
			return null; // Chat aspect overlay disabled, don't modify hover event
		}
		
		// Extract blueprint name from message
		String blueprintName = extractBlueprintNameFromChatMessage(message);
		if (blueprintName == null || blueprintName.isEmpty()) {
			return null; // Could not extract blueprint name
		}
		
		// Get aspect information
		AspectInfo aspectInfo = getAspectInfoForBlueprintFull(blueprintName);
		if (aspectInfo == null || aspectInfo.aspectName == null || aspectInfo.aspectName.isEmpty()) {
			return null; // No aspect info found
		}
		
		// Find hover event in message
		HoverEvent hoverEvent = findHoverEventInText(message);
		if (hoverEvent == null || hoverEvent.action() != HoverEvent.Action.SHOW_TEXT) {
			return null; // No hover event found
		}
		
		// Extract original hover text
		Component originalHoverText = extractHoverTextFromEvent(hoverEvent, message);
		
		// Ensure aspectInfo.aspectName is not null or empty
		String aspectName = aspectInfo.aspectName != null && !aspectInfo.aspectName.isEmpty() ? aspectInfo.aspectName : "-";
		
		// Create new hover text with aspect info inserted above "Benötigt:" line
		Component newHoverText;
		try {
			if (originalHoverText != null) {
				// Create aspect info text with the same color as the original text
				Component aspectNameText = Component.literal("Enthält: ")
					.withStyle(style -> style.withColor(0xFFFFFFFF)) // White color
					.append(Component.literal(aspectName)
						.withStyle(style -> style.withColor(0xFFFCA800))); // Aspect name in overlay color
				
				Component shiftInfoText = Component.literal("(Shift für mehr Info)")
					.withStyle(style -> style.withColor(0xFFCCCCCC)); // Light gray
				
				// Build the new text by iterating through OrderedText and inserting before "Benötigt:"
				// This preserves all formatting from the original text character by character
				net.minecraft.network.chat.MutableComponent result = Component.empty();
				final boolean[] inserted = {false};
				java.util.List<Integer> currentLine = new java.util.ArrayList<>();
				final java.util.List<net.minecraft.network.chat.Style> lineStyles = new java.util.ArrayList<>();
				
				net.minecraft.util.FormattedCharSequence orderedText = originalHoverText.getVisualOrderText();
				orderedText.accept((index, style, codePoint) -> {
					char ch = (char) codePoint;
					
					if (ch == '\n') {
						// End of line - check if it contains "Benötigt:"
						StringBuilder lineBuilder = new StringBuilder();
						for (int cp : currentLine) {
							lineBuilder.appendCodePoint(cp);
						}
						String lineText = lineBuilder.toString();
						
						if (!inserted[0] && lineText.contains("Benötigt")) {
							// Insert aspect info before this line
							result.append(aspectNameText);
							result.append(Component.literal("\n"));
							result.append(shiftInfoText);
							result.append(Component.literal("\n"));
							inserted[0] = true;
						}
						
						// Append the line character by character with formatting preserved
						for (int i = 0; i < currentLine.size(); i++) {
							int cp = currentLine.get(i);
							net.minecraft.network.chat.Style charStyle = i < lineStyles.size() ? lineStyles.get(i) : style;
							net.minecraft.network.chat.MutableComponent charText = Component.literal(new String(Character.toChars(cp)));
							if (charStyle != null) {
								charText = charText.withStyle(s -> charStyle);
							}
							result.append(charText);
						}
						result.append(Component.literal("\n"));
						currentLine.clear();
						lineStyles.clear();
					} else {
						currentLine.add(codePoint);
						lineStyles.add(style);
					}
					
					return true;
				});
				
				// Handle the last line
				for (int i = 0; i < currentLine.size(); i++) {
					int cp = currentLine.get(i);
					net.minecraft.network.chat.Style charStyle = i < lineStyles.size() ? lineStyles.get(i) : null;
					net.minecraft.network.chat.MutableComponent charText = Component.literal(new String(Character.toChars(cp)));
					if (charStyle != null) {
						charText = charText.withStyle(s -> charStyle);
					}
					result.append(charText);
				}
				
				// If "Benötigt:" was not found, append at the end
				if (inserted[0] == false) {
					result.append(Component.literal("\n"));
					result.append(aspectNameText);
					result.append(Component.literal("\n"));
					result.append(shiftInfoText);
				}
				
				newHoverText = result;
			} else {
				// Could not extract original text, create new one with just aspect info
				Component aspectNameText = Component.literal("Enthält: ")
					.withStyle(style -> style.withColor(0xFFFFFFFF)) // White color
					.append(Component.literal(aspectName)
						.withStyle(style -> style.withColor(0xFFFCA800))); // Same color as overlay (#FCA800)
				
				Component shiftInfoText = Component.literal("(Shift für mehr Info)")
					.withStyle(style -> style.withColor(0xFFCCCCCC)); // Light gray color
				
				newHoverText = aspectNameText.copy()
					.append(Component.literal("\n"))
					.append(shiftInfoText);
			}
			
			// Validate that newHoverText is not null
			if (newHoverText == null) {
				return null;
			}
		} catch (Exception e) {
			return null;
		}
		
		// Create new hover event
		HoverEvent newHoverEvent = createHoverEventForAspect(newHoverText);
		if (newHoverEvent == null) {
			return null; // Failed to create hover event
		}
		
		// Modify message with new hover event (preserving all formatting)
		Component modified = modifyTextWithHoverEvent(message, newHoverEvent);
		return modified;
	}

	/**
	 * Updates aspect overlay from blueprint name (for chat messages)
	 */
	public static void updateAspectOverlayFromBlueprintName(String blueprintName) {
		if (blueprintName == null || blueprintName.isEmpty()) {
			AspectOverlay.onHoverStopped();
			return;
		}
		
		// Get aspect information
		AspectInfo aspectInfo = getAspectInfoForBlueprintFull(blueprintName);
		
		if (aspectInfo == null) {
			AspectOverlay.onHoverStopped();
			return;
		}
		
		// Clean the blueprint name
		String cleanItemName = blueprintName.trim();
		
		// Remove leading dash/minus if present
		if (cleanItemName.startsWith("-")) {
			cleanItemName = cleanItemName.substring(1).trim();
		}
		
		// Remove trailing dash/minus if present
		if (cleanItemName.endsWith("-")) {
			cleanItemName = cleanItemName.substring(0, cleanItemName.length() - 1).trim();
		}
		
		// Remove Minecraft formatting codes and Unicode characters
		cleanItemName = cleanItemName.replaceAll("§[0-9a-fk-or]", "");
		cleanItemName = cleanItemName.replaceAll("[\\u3400-\\u4DBF]", "");
		cleanItemName = cleanItemName.replaceAll("[^a-zA-ZäöüßÄÖÜ\\s-]", "").trim();
		
		// Update overlay for chat
		AspectOverlay.updateAspectInfoFromNameForChat(cleanItemName, aspectInfo);
	}

	/**
	 * Updates aspect overlay from blueprint name with Text object (for color checking)
	 */
	public static void updateAspectOverlayFromBlueprintName(String blueprintName, Component textWithColor) {
		// Check if the text contains Epic colors - if so, don't show overlay
		if (textWithColor != null && hasEpicColor(textWithColor)) {
			AspectOverlay.onHoverStopped();
			return;
		}
		
		// Otherwise, proceed with normal update
		updateAspectOverlayFromBlueprintName(blueprintName);
	}
}
