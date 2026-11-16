package net.felix.features.information.moblexicon;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Pattern;
import net.felix.utilities.Overall.MobFloorHpData;
import net.felix.utilities.Overall.OpferungMkDamageData;
import net.felix.features.information.materials.MaterialInfo;

import static net.felix.features.information.materials.MaterialDatabase.materialsDatabase;
import static net.felix.features.information.tooltip.TooltipText.cleanTooltipText;


/**
 * Zeigt Mob-HP und Ebene im Moblexikon an.
 */
public final class MoblexiconTooltip {

	private MoblexiconTooltip() {
	}

	public static void addMoblexiconHpToTooltip(List<Component> lines) {
		if (lines == null || lines.isEmpty()) {
			return;
		}
		
		if (!hasGreenAllgemeinInFirstLine(lines.get(0))) {
			return;
		}
		
		for (Component line : lines) {
			if (line != null) {
				String withoutFormatting = line.getString().replaceAll("§[0-9a-fk-or]", "");
				int hpIndex = withoutFormatting.indexOf("HP:");
				if (hpIndex >= 0 && withoutFormatting.substring(hpIndex).startsWith("HP: ")) {
					return;
				}
			}
		}
		
		Integer floor = resolveMobFloorFromMoblexiconTooltip(lines);
		if (floor == null) {
			return;
		}
		
		String hpDisplay = MobFloorHpData.getHpDisplay(floor);
		if (hpDisplay == null) {
			return;
		}

		long mobHp = MobFloorHpData.getHpValue(floor);
		Integer opferungLevel = mobHp >= 0 ? OpferungMkDamageData.getMinLevelForHp(mobHp) : null;
		
		int insertIndex = findMoblexiconHpInsertIndex(lines);
		if (insertIndex < 0 || insertIndex == 0) {
			return;
		}
		
		String dropLineText = lines.get(insertIndex - 1).getString();
		String prefix = extractMoblexiconDropLinePrefix(dropLineText);
		
		net.minecraft.network.chat.MutableComponent hpLine = net.minecraft.network.chat.Component.empty();
		if (!prefix.isEmpty()) {
			hpLine.append(net.minecraft.network.chat.Component.literal(prefix));
		}
		hpLine.append(net.minecraft.network.chat.Component.literal("HP: " + hpDisplay)
			.withStyle(style -> style.withColor(0xFF5555).withItalic(false)));
		if (opferungLevel != null) {
			hpLine.append(net.minecraft.network.chat.Component.literal(" -> Opferung lvl: " + opferungLevel)
				.withStyle(style -> style.withColor(0xFFAA00).withItalic(false)));
		}
		lines.add(insertIndex, hpLine);
	}

	/**
	 * Führende Einrückung der Moblexicon-Drop-Zeile (inkl. unsichtbarer Zeichen), bis zum Item-Namen (z.B. Coins oder ????).
	 */
	private static String extractMoblexiconDropLinePrefix(String lineText) {
		if (lineText == null || lineText.isEmpty()) {
			return "";
		}
		
		String withoutFormatting = lineText.replaceAll("§[0-9a-fk-or]", "");
		int nameStart = withoutFormatting.toLowerCase().indexOf("coins");
		if (nameStart < 0) {
			nameStart = withoutFormatting.indexOf('?');
		}
		if (nameStart < 0) {
			for (int i = 0; i < withoutFormatting.length(); i++) {
				if (Character.isLetter(withoutFormatting.charAt(i))) {
					nameStart = i;
					break;
				}
			}
		}
		if (nameStart <= 0) {
			return "";
		}
		return withoutFormatting.substring(0, nameStart);
	}

	private static boolean hasGreenAllgemeinInFirstLine(Component firstLine) {
		if (firstLine == null) {
			return false;
		}
		
		final StringBuilder segment = new StringBuilder();
		final Style[] currentStyle = {Style.EMPTY};
		final boolean[] found = {false};
		
		firstLine.getVisualOrderText().accept((index, style, codePoint) -> {
			if (found[0]) {
				return false;
			}
			
			Style effectiveStyle = style != null ? style : Style.EMPTY;
			if (segment.length() > 0 && !stylesColorEqual(currentStyle[0], effectiveStyle)) {
				if (segment.indexOf("Allgemein") >= 0 && isMoblexiconGreenStyle(currentStyle[0])) {
					found[0] = true;
					return false;
				}
				segment.setLength(0);
			}
			currentStyle[0] = effectiveStyle;
			segment.appendCodePoint(codePoint);
			return true;
		});
		
		return found[0] || (segment.indexOf("Allgemein") >= 0 && isMoblexiconGreenStyle(currentStyle[0]));
	}

	private static boolean stylesColorEqual(Style a, Style b) {
		if (a == null && b == null) {
			return true;
		}
		if (a == null || b == null) {
			return false;
		}
		if (a.getColor() == null && b.getColor() == null) {
			return true;
		}
		if (a.getColor() == null || b.getColor() == null) {
			return false;
		}
		return (a.getColor().getValue() & 0x00FFFFFF) == (b.getColor().getValue() & 0x00FFFFFF);
	}

	private static boolean isMoblexiconGreenStyle(Style style) {
		if (style == null || style.getColor() == null) {
			return false;
		}
		int rgb = style.getColor().getValue() & 0x00FFFFFF;
		return rgb == 0x55FF55 || rgb == 0x00FF00;
	}

	private static Integer resolveMobFloorFromMoblexiconTooltip(List<Component> lines) {
		if (lines.isEmpty()) {
			return null;
		}
		
		String lineText = lines.get(0).getString();
		if (lineText == null || lineText.isEmpty()) {
			return null;
		}
		
		String cleanLineText = cleanTooltipText(lineText);
		String mobNamePart = cleanLineText;
		if (cleanLineText.contains("[")) {
			mobNamePart = cleanLineText.substring(0, cleanLineText.indexOf("[")).trim();
		}
		if (mobNamePart.isEmpty()) {
			return null;
		}
		
		List<Map.Entry<String, MaterialInfo>> sortedMobs = new ArrayList<>();
		for (Map.Entry<String, MaterialInfo> entry : materialsDatabase.entrySet()) {
			if ("mob".equals(entry.getValue().rarity)) {
				sortedMobs.add(entry);
			}
		}
		sortedMobs.sort((a, b) -> Integer.compare(b.getKey().length(), a.getKey().length()));
		
		for (Map.Entry<String, MaterialInfo> entry : sortedMobs) {
			String mobName = entry.getKey();
			String pattern = "\\b" + java.util.regex.Pattern.quote(mobName) + "\\b";
			if (java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.UNICODE_CHARACTER_CLASS)
					.matcher(mobNamePart).find()) {
				return entry.getValue().floor;
			}
		}
		
		return null;
	}

	private static int findMoblexiconHpInsertIndex(List<Component> lines) {
		for (int i = 0; i < lines.size(); i++) {
			String cleanLineText = cleanTooltipText(lines.get(i).getString());
			if (cleanLineText.isEmpty()) {
				continue;
			}
			if (i == 0 || cleanLineText.contains("Wissensstufe")) {
				continue;
			}
			if (cleanLineText.contains("%)") || cleanLineText.contains("????")) {
				return i + 1;
			}
		}
		return -1;
	}
}
