package net.felix.utilities.Aincraft.information.essences;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

import static net.felix.utilities.Aincraft.information.essences.EssenceWaveNames.getWaveDisplayForEssenceMobAndTier;
import static net.felix.utilities.Aincraft.information.tooltip.TooltipTextCleaner.cleanTooltipLineForMatching;

/**
 * Wellen-Anzeige in Essenz-Taschen.
 */
public final class EssenceBagTooltip {

	private EssenceBagTooltip() {
	}

	/**
	 * Appends perfect wave info to tier lines in the essence bag UI tooltip.
	 * Format: {@code Tier X - amount -> Welle: NNN} (wave number in green).
	 */
	public static void addEssenceBagWaveDisplayToTooltip(List<Component> lines, ItemStack stack) {
		if (lines == null || lines.isEmpty()) {
			return;
		}
		
		String essenceMobBase = extractEssenceMobBaseFromTooltip(lines, stack);
		if (essenceMobBase == null || essenceMobBase.isEmpty()) {
			return;
		}
		
		java.util.regex.Pattern tierLinePattern = java.util.regex.Pattern.compile(
				"Tier\\s+(\\d+)\\s*-\\s*.+", java.util.regex.Pattern.CASE_INSENSITIVE);
		
		for (int i = 0; i < lines.size(); i++) {
			Component line = lines.get(i);
			if (line == null) {
				continue;
			}
			
			String cleanLine = cleanTooltipLineForMatching(line.getString());
			if (cleanLine.isEmpty() || cleanLine.contains("-> Welle:")) {
				continue;
			}
			
			java.util.regex.Matcher matcher = tierLinePattern.matcher(cleanLine);
			if (!matcher.matches()) {
				continue;
			}
			
			String tier = matcher.group(1);
			String waveText = getWaveDisplayForEssenceMobAndTier(essenceMobBase, tier);
			if (waveText == null) {
				continue;
			}
			
			MutableComponent newLine = line.copy()
					.append(Component.literal(" -> Welle: ").withStyle(style -> style.withColor(0xC0C0C0)))
					.append(Component.literal(waveText).withStyle(style -> style.withColor(0x55FF55)));
			lines.set(i, newLine);
		}
	}

	private static String extractEssenceMobBaseFromTooltip(List<Component> lines, ItemStack stack) {
		for (Component line : lines) {
			if (line == null) {
				continue;
			}
			String clean = cleanTooltipLineForMatching(line.getString());
			if (clean.contains("[Essenz]")) {
				return clean.substring(0, clean.indexOf("[Essenz]")).trim();
			}
		}
		
		if (stack != null && !stack.isEmpty()) {
			String clean = cleanTooltipLineForMatching(stack.getHoverName().getString());
			if (clean.contains("[Essenz]")) {
				return clean.substring(0, clean.indexOf("[Essenz]")).trim();
			}
		}
		
		return null;
	}
}
