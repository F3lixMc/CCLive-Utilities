package net.felix.features.information.tooltip;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.felix.utilities.Overall.ZeichenUtility;

/**
 * Textbereinigung fuer Tooltip-Zeilen und Chat-Komponenten.
 */
public final class TooltipText {

	private TooltipText() {
	}

	public static String cleanTooltipText(String lineText) {
		if (lineText == null) {
			return "";
		}
		return lineText.replaceAll("§[0-9a-fk-or]", "")
			.replaceAll("[\\u3400-\\u4DBF]", "")
			.trim();
	}

	public static String cleanTooltipLineForMatching(String text) {
		if (text == null) {
			return "";
		}
		String clean = text.replaceAll("§[0-9a-fk-orxA-FK-ORX]", "");
		clean = clean.replaceAll("§#[0-9a-fA-F]{6}", "");
		clean = clean.replaceAll("§x(§[0-9a-fA-F]){6}", "");
		clean = clean.replaceAll("[\\u3400-\\u4DBF\\u4E00-\\u9FFF]", "");
		clean = removePixelSpacerCharacters(clean);
		return clean.replaceAll("\\s+", " ").trim();
	}

	private static String removePixelSpacerCharacters(String text) {
		if (text == null || text.isEmpty()) {
			return text;
		}
		String spacers = ZeichenUtility.getPixelSpacer();
		if (spacers == null || spacers.isEmpty()) {
			return text;
		}
		StringBuilder result = new StringBuilder(text);
		for (int i = 0; i < spacers.length(); ) {
			int codePoint = spacers.codePointAt(i);
			String spacer = new String(Character.toChars(codePoint));
			int index = 0;
			while ((index = result.indexOf(spacer, index)) >= 0) {
				result.delete(index, index + spacer.length());
			}
			i += Character.charCount(codePoint);
		}
		return result.toString();
	}

	/**
	 * Extracts plain text from Text component, removing all formatting codes
	 */
	public static String getPlainTextFromText(Component text) {
		if (text == null) {
			return "";
		}
		
		// Get the string representation (includes formatting codes)
		String textString = text.getString();
		
		// Remove all Minecraft formatting codes (including all possible format codes)
		// § followed by any character (0-9, a-f, k-o, r, x for hex colors, etc.)
		textString = textString.replaceAll("§[0-9a-fk-orxA-FK-ORX]", "");
		
		// Remove hex color codes (format: §#RRGGBB or §x§r§r§g§g§b§b)
		textString = textString.replaceAll("§#[0-9a-fA-F]{6}", "");
		textString = textString.replaceAll("§x(§[0-9a-fA-F]){6}", "");
		
		// Also remove Unicode formatting characters that might interfere
		textString = textString.replaceAll("[\\u3400-\\u4DBF]", "");
		
		// Trim whitespace
		return textString.trim();
	}
}
