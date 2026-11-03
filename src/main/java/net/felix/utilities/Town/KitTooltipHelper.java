package net.felix.utilities.Town;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

/**
 * Cached layout + render helper for kit tooltips (avoids per-frame sorting, width calculation and BP lookups).
 */
public final class KitTooltipHelper {

	private static final int ACTION_GREEN = 0xFF16A80C;
	private static final int SHIFT_DARK_GREEN = 0xFF00AA00;
	private static final int SECTION_TITLE_YELLOW = 0xFFFFFF55;
	private static final int KIT_NAME_AQUA = 0xFF55FFFF;
	private static final int CARD_EFFECT_GREEN = 0xFF55FF55;
	private static final int COLUMN_SPACING = 8;
	private static final int PAIR_GAP = 6;
	private static final int SCREEN_MARGIN = 8;
	private static final String BULLET_POINT = "• ";
	private static final String PREVIEW_HINT_KEY = "Strg+Linksklick";
	private static final String PREVIEW_HINT_ACTION = "Vorschau";
	private static final String BP_STATUS_FOUND = " ✓";
	private static final String BP_STATUS_MISSING = " ✗";
	private static final int COUNT_YELLOW = 0xFFFFFF55;

	private String cacheKey;
	private CachedTooltip cache;

	public void clear() {
		cacheKey = null;
		cache = null;
	}

	public CachedTooltip getOrBuild(
			String key,
			Font textRenderer,
			String header,
			Collection<KitFilterUtility.ItemInfo> rawItems,
			boolean showSelectActionHint,
			boolean showEditActionHint,
			boolean showBpStatus,
			Function<String, Boolean> blueprintLookup
	) {
		List<KitFilterUtility.ItemInfo> items = new ArrayList<>();
		if (rawItems != null) {
			items.addAll(rawItems);
		}
		if (!items.isEmpty()) {
			items.sort((a, b) -> Integer.compare(parseEbeneNumber(a.ebene), parseEbeneNumber(b.ebene)));
		}
		List<SectionData> sections = new ArrayList<>();
		if (!items.isEmpty()) {
			sections.add(new SectionData(null, items, KitFilterUtility.KitHoverSection.FoundMode.BLUEPRINT));
		}
		return buildInternal(
			key, textRenderer, header, sections,
			showSelectActionHint, showEditActionHint, false, false, showBpStatus,
			blueprintLookup, null, null);
	}

	public CachedTooltip getOrBuildSections(
			String key,
			Font textRenderer,
			String header,
			List<KitFilterUtility.KitHoverSection> hoverSections,
			boolean showSelectActionHint,
			boolean showEditActionHint,
			boolean showBpStatus,
			Function<String, Boolean> blueprintLookup,
			Function<String, Boolean> fishingFoundLookup,
			Function<String, Boolean> cardFoundLookup
	) {
		List<SectionData> sections = new ArrayList<>();
		if (hoverSections != null) {
			for (KitFilterUtility.KitHoverSection section : hoverSections) {
				if (section == null || section.items == null || section.items.isEmpty()) {
					continue;
				}
				sections.add(new SectionData(section.title, new ArrayList<>(section.items), section.foundMode));
			}
		}
		return buildInternal(
			key, textRenderer, header, sections,
			showSelectActionHint, showEditActionHint, true, true, showBpStatus,
			blueprintLookup, fishingFoundLookup, cardFoundLookup);
	}

	private CachedTooltip buildInternal(
			String key,
			Font textRenderer,
			String header,
			List<SectionData> sections,
			boolean showSelectActionHint,
			boolean showEditActionHint,
			boolean showExtraHint,
			boolean showPreviewHint,
			boolean showBpStatus,
			Function<String, Boolean> blueprintLookup,
			Function<String, Boolean> fishingFoundLookup,
			Function<String, Boolean> cardFoundLookup
	) {
		if (key != null && key.equals(cacheKey) && cache != null) {
			return cache;
		}

		if (header == null || header.isEmpty()) {
			cacheKey = key;
			cache = null;
			return null;
		}

		int maxItemTypeWidth = 0;
		int maxItemNameWidth = 0;
		int maxModifierWidth = 0;
		int maxEbeneWidth = 0;
		boolean anyModifier = false;
		for (SectionData section : sections) {
			boolean skipModifier = section.foundMode == KitFilterUtility.KitHoverSection.FoundMode.FISHING;
			for (KitFilterUtility.ItemInfo itemInfo : section.items) {
				maxItemTypeWidth = Math.max(maxItemTypeWidth, calculateItemTypeWidth(textRenderer, itemInfo));
				maxItemNameWidth = Math.max(maxItemNameWidth, calculateItemNameWidth(textRenderer, itemInfo));
				if (!skipModifier) {
					int modifierWidth = calculateModifierWidth(textRenderer, itemInfo);
					if (modifierWidth > 0) {
						anyModifier = true;
					}
					maxModifierWidth = Math.max(maxModifierWidth, modifierWidth);
				}
				maxEbeneWidth = Math.max(maxEbeneWidth, calculateEbeneWidth(textRenderer, itemInfo));
			}
		}
		if (anyModifier) {
			maxModifierWidth = Math.max(maxModifierWidth, textRenderer.width("–"));
		} else {
			maxModifierWidth = 0;
		}

		int bulletWidth = textRenderer.width(BULLET_POINT);
		int statusWidth = 0;
		if (showBpStatus) {
			statusWidth = textRenderer.width(BP_STATUS_FOUND) + COLUMN_SPACING;
		}

		int totalWidth = Math.max(
				textRenderer.width(header),
				bulletWidth + maxItemTypeWidth + COLUMN_SPACING + maxItemNameWidth + COLUMN_SPACING
						+ maxModifierWidth + COLUMN_SPACING + maxEbeneWidth + statusWidth
		);
		for (SectionData section : sections) {
			if (section.title != null && !section.title.isEmpty()) {
				totalWidth = Math.max(totalWidth, textRenderer.width(section.title));
			}
		}
		if (showSelectActionHint) {
			totalWidth = Math.max(totalWidth, textRenderer.width("[Linksklick]: Auswählen"));
		}
		if (showEditActionHint) {
			totalWidth = Math.max(totalWidth, textRenderer.width("[Rechtsklick]: Bearbeiten"));
		}
		if (showPreviewHint) {
			totalWidth = Math.max(totalWidth, textRenderer.width("[" + PREVIEW_HINT_KEY + "]: " + PREVIEW_HINT_ACTION));
		}
		if (showExtraHint) {
			totalWidth = Math.max(totalWidth, textRenderer.width("[Shift]: Zusätzliche Infos"));
		}

		int textHeight = textRenderer.lineHeight;
		int padding = 4;
		int lineSpacing = 2;

		int totalHeight = textHeight + padding * 2;
		for (SectionData section : sections) {
			totalHeight += lineSpacing;
			if (section.title != null && !section.title.isEmpty()) {
				totalHeight += textHeight + lineSpacing;
			}
			totalHeight += textHeight * section.items.size();
		}
		if (showSelectActionHint || showEditActionHint) {
			totalHeight += lineSpacing;
			if (showSelectActionHint) {
				totalHeight += textHeight;
			}
			if (showEditActionHint) {
				totalHeight += textHeight;
			}
		}
		if (showPreviewHint) {
			totalHeight += textHeight;
		}
		if (showExtraHint) {
			totalHeight += lineSpacing + textHeight;
		}

		List<CachedSection> cachedSections = new ArrayList<>();
		for (SectionData section : sections) {
			boolean[] found = null;
			if (showBpStatus) {
				Function<String, Boolean> lookup = switch (section.foundMode) {
					case FISHING -> fishingFoundLookup != null ? fishingFoundLookup : blueprintLookup;
					case CARD -> cardFoundLookup != null ? cardFoundLookup : blueprintLookup;
					case BLUEPRINT -> blueprintLookup;
					default -> null;
				};
				if (lookup != null && !section.items.isEmpty()) {
					found = new boolean[section.items.size()];
					for (int i = 0; i < section.items.size(); i++) {
						found[i] = Boolean.TRUE.equals(lookup.apply(blueprintLookupName(section.items.get(i).name)));
					}
				}
			}
			cachedSections.add(new CachedSection(section.title, section.items, found, section.foundMode));
		}

		cacheKey = key;
		cache = new CachedTooltip(
				header,
				cachedSections,
				showBpStatus,
				showSelectActionHint,
				showEditActionHint,
				showExtraHint,
				showPreviewHint,
				maxItemTypeWidth,
				maxItemNameWidth,
				maxModifierWidth,
				maxEbeneWidth,
				totalWidth,
				totalHeight,
				bulletWidth,
				textHeight,
				padding,
				lineSpacing
		);
		return cache;
	}

	public static void render(
			GuiGraphicsExtractor context,
			Font textRenderer,
			CachedTooltip tooltip,
			int mouseX,
			int mouseY,
			int screenWidth,
			int screenHeight
	) {
		render(context, textRenderer, tooltip, null, mouseX, mouseY, screenWidth, screenHeight, false);
	}

	public static void render(
			GuiGraphicsExtractor context,
			Font textRenderer,
			CachedTooltip tooltip,
			ExtraPanel extra,
			int mouseX,
			int mouseY,
			int screenWidth,
			int screenHeight
	) {
		render(context, textRenderer, tooltip, extra, mouseX, mouseY, screenWidth, screenHeight, extra != null);
	}

	public static void render(
			GuiGraphicsExtractor context,
			Font textRenderer,
			CachedTooltip tooltip,
			ExtraPanel extra,
			int mouseX,
			int mouseY,
			int screenWidth,
			int screenHeight,
			boolean drawExtra
	) {
		if (tooltip == null) {
			return;
		}

		int offset = 10;
		int mainBoxW = tooltip.totalWidth + tooltip.padding * 2;
		int mainBoxH = tooltip.totalHeight;
		int extraBoxW = extra != null ? extra.width + tooltip.padding * 2 : 0;
		int extraBoxH = extra != null ? extra.height : 0;

		boolean sideBySide = extra != null && extraBoxW + PAIR_GAP + mainBoxW <= screenWidth;
		int pairW;
		int pairH;
		if (extra == null) {
			pairW = mainBoxW;
			pairH = mainBoxH;
		} else if (sideBySide) {
			pairW = extraBoxW + PAIR_GAP + mainBoxW;
			pairH = Math.max(extraBoxH, mainBoxH);
		} else {
			pairW = Math.max(extraBoxW, mainBoxW);
			pairH = extraBoxH + PAIR_GAP + mainBoxH;
		}

		float scale = 1f;
		int availW = Math.max(1, screenWidth - SCREEN_MARGIN * 2);
		int availH = Math.max(1, screenHeight - SCREEN_MARGIN * 2);
		if (pairW > availW || pairH > availH) {
			scale = Math.min(1f, Math.min((float) availW / pairW, (float) availH / pairH));
		}
		int scaledW = Math.max(1, Math.round(pairW * scale));
		int scaledH = Math.max(1, Math.round(pairH * scale));

		int pairX = mouseX + offset;
		if (pairX + scaledW > screenWidth - SCREEN_MARGIN) {
			pairX = mouseX - scaledW - offset;
			if (pairX < SCREEN_MARGIN) {
				pairX = SCREEN_MARGIN;
			}
		}
		if (pairX + scaledW > screenWidth - SCREEN_MARGIN) {
			pairX = Math.max(SCREEN_MARGIN, screenWidth - scaledW - SCREEN_MARGIN);
		}

		int pairY = mouseY - scaledH - offset;
		if (pairY < SCREEN_MARGIN) {
			pairY = mouseY + offset;
			if (pairY + scaledH > screenHeight - SCREEN_MARGIN) {
				pairY = screenHeight - scaledH - SCREEN_MARGIN;
			}
		}
		if (pairY + scaledH > screenHeight - SCREEN_MARGIN) {
			pairY = screenHeight - scaledH - SCREEN_MARGIN;
		}
		if (pairY < SCREEN_MARGIN) {
			pairY = SCREEN_MARGIN;
		}

		context.pose().pushMatrix();
		context.pose().translate(pairX, pairY);
		context.pose().scale(scale, scale);

		int extraBoxX = 0;
		int extraBoxY = 0;
		int mainBoxX;
		int mainBoxY;
		if (extra == null) {
			mainBoxX = 0;
			mainBoxY = 0;
		} else if (sideBySide) {
			mainBoxX = extraBoxW + PAIR_GAP;
			mainBoxY = 0;
			extraBoxY = 0;
		} else {
			mainBoxX = 0;
			mainBoxY = extraBoxH + PAIR_GAP;
			extraBoxX = 0;
			extraBoxY = 0;
		}

		if (extra != null && drawExtra) {
			drawPanelBackground(
				context,
				extraBoxX,
				extraBoxY,
				extraBoxX + extraBoxW,
				extraBoxY + extraBoxH);
			renderExtraContent(
				context, textRenderer, extra,
				extraBoxX + extra.padding,
				extraBoxY + extra.padding);
		}

		int tooltipX = mainBoxX + tooltip.padding;
		int tooltipY = mainBoxY;
		drawPanelBackground(
			context,
			tooltipX - tooltip.padding,
			tooltipY - tooltip.padding,
			tooltipX + tooltip.totalWidth + tooltip.padding,
			tooltipY + tooltip.totalHeight - tooltip.padding);

		Component kitName = Component.literal(tooltip.header != null ? tooltip.header : "")
			.setStyle(Style.EMPTY
				.withBold(true)
				.withColor(TextColor.fromRgb(KIT_NAME_AQUA & 0xFFFFFF)));
		context.text(textRenderer, kitName, tooltipX, tooltipY, KIT_NAME_AQUA, true);

		int currentY = tooltipY + tooltip.textHeight;
		for (CachedSection section : tooltip.sections) {
			currentY += tooltip.lineSpacing;
			if (section.title != null && !section.title.isEmpty()) {
				context.text(textRenderer, section.title, tooltipX, currentY, SECTION_TITLE_YELLOW, true);
				currentY += tooltip.textHeight + tooltip.lineSpacing;
			}
			currentY = renderItemRows(
				context, textRenderer, tooltip, section.items, section.foundFlags,
				section.foundMode, tooltipX, currentY);
		}

		if (tooltip.showSelectActionHint || tooltip.showEditActionHint || tooltip.showPreviewHint) {
			currentY += tooltip.lineSpacing;
			if (tooltip.showSelectActionHint) {
				drawActionHintLine(context, textRenderer, tooltipX, currentY, "Linksklick", "Auswählen");
				currentY += tooltip.textHeight;
			}
			if (tooltip.showEditActionHint) {
				drawActionHintLine(context, textRenderer, tooltipX, currentY, "Rechtsklick", "Bearbeiten");
				currentY += tooltip.textHeight;
			}
			if (tooltip.showPreviewHint) {
				drawActionHintLine(context, textRenderer, tooltipX, currentY, PREVIEW_HINT_KEY, PREVIEW_HINT_ACTION);
				currentY += tooltip.textHeight;
			}
		}
		if (tooltip.showExtraHint) {
			currentY += tooltip.lineSpacing;
			drawActionHintLine(context, textRenderer, tooltipX, currentY, SHIFT_DARK_GREEN, "Shift", "Zusätzliche Infos");
		}
		context.pose().popMatrix();
	}

	public static boolean isShiftHeld() {
		return com.mojang.blaze3d.platform.InputConstants.isKeyDown(
				com.mojang.blaze3d.platform.InputConstants.KEY_LSHIFT)
			|| com.mojang.blaze3d.platform.InputConstants.isKeyDown(
				com.mojang.blaze3d.platform.InputConstants.KEY_RSHIFT);
	}

	public static ExtraPanel buildExtraPanel(Font textRenderer, CustomKit kit) {
		List<ExtraLine> lines = new ArrayList<>();
		int textHeight = textRenderer.lineHeight;
		int padding = 4;
		int lineSpacing = 2;
		boolean missingBoth = kit == null
			|| (kit.powerCrystalNames == null && kit.aspectNames == null);
		if (missingBoth) {
			lines.add(new ExtraLine("Keine Zusatzdaten gespeichert", false));
		} else {
			appendExtraSection(lines, "Machtkristalle", kit.powerCrystalNames);
			appendExtraSection(lines, "Aspekte", kit.aspectNames);
		}

		int width = textRenderer.width("Zusätzliche Infos");
		for (ExtraLine line : lines) {
			String shown = line.title ? line.text : (BULLET_POINT + line.text);
			width = Math.max(width, textRenderer.width(shown));
		}
		int height = textHeight + padding * 2;
		if (!lines.isEmpty()) {
			height += lineSpacing;
			height += textHeight * lines.size();
		}
		return new ExtraPanel(width, height, textHeight, padding, lineSpacing, lines);
	}

	private static void appendExtraSection(List<ExtraLine> lines, String title, List<String> names) {
		if (names == null || names.isEmpty()) {
			return;
		}
		lines.add(new ExtraLine(title, true));
		for (String name : names) {
			if (name != null && !name.isEmpty()) {
				lines.add(new ExtraLine(name, false));
			}
		}
	}

	private static void drawPanelBackground(GuiGraphicsExtractor context, int x1, int y1, int x2, int y2) {
		context.fill(x1, y1, x2, y2, 0xF0000000);
		context.fill(x1, y1, x2, y1 + 1, 0xFFFFFFFF);
		context.fill(x1, y2 - 1, x2, y2, 0xFFFFFFFF);
		context.fill(x1, y1, x1 + 1, y2, 0xFFFFFFFF);
		context.fill(x2 - 1, y1, x2, y2, 0xFFFFFFFF);
	}

	private static void renderExtraContent(
			GuiGraphicsExtractor context,
			Font textRenderer,
			ExtraPanel extra,
			int x,
			int y
	) {
		context.text(textRenderer, "Zusätzliche Infos", x, y, KIT_NAME_AQUA, true);
		int currentY = y + extra.textHeight + extra.lineSpacing;
		for (ExtraLine line : extra.lines) {
			if (line.title) {
				context.text(textRenderer, line.text, x, currentY, SECTION_TITLE_YELLOW, true);
			} else {
				context.text(textRenderer, BULLET_POINT + line.text, x, currentY, 0xFFFFFFFF, true);
			}
			currentY += extra.textHeight;
		}
	}

	private static int renderItemRows(
			GuiGraphicsExtractor context,
			Font textRenderer,
			CachedTooltip tooltip,
			List<KitFilterUtility.ItemInfo> rows,
			boolean[] foundFlags,
			KitFilterUtility.KitHoverSection.FoundMode foundMode,
			int tooltipX,
			int currentY
	) {
		for (int i = 0; i < rows.size(); i++) {
			KitFilterUtility.ItemInfo itemInfo = rows.get(i);
			int currentX = tooltipX;

			context.text(textRenderer, BULLET_POINT, currentX, currentY, 0xFFFFFFFF, true);
			currentX += tooltip.bulletWidth;

			if (itemInfo.itemType != null && !itemInfo.itemType.isEmpty()) {
				context.text(textRenderer, itemInfo.itemType, currentX, currentY, 0xFFFFFFFF, true);
			}
			currentX += tooltip.maxItemTypeWidth + COLUMN_SPACING;

			if (itemInfo.name != null && !itemInfo.name.isEmpty()) {
				int nameColor = (itemInfo.nameColorString != null && !itemInfo.nameColorString.isEmpty())
						? itemInfo.nameColor
						: 0xFFFFFFFF;
				drawNameWithCount(context, textRenderer, itemInfo.name, currentX, currentY, nameColor);
			}
			currentX += tooltip.maxItemNameWidth + COLUMN_SPACING;

			if (foundMode != KitFilterUtility.KitHoverSection.FoundMode.FISHING && tooltip.maxModifierWidth > 0) {
				boolean cardEffect = foundMode == KitFilterUtility.KitHoverSection.FoundMode.CARD
					|| "Karte".equals(itemInfo.itemType);
				renderModifierColumn(context, textRenderer, itemInfo, currentX, currentY, cardEffect);
				currentX += tooltip.maxModifierWidth + COLUMN_SPACING;
			} else if (tooltip.maxModifierWidth > 0) {
				currentX += tooltip.maxModifierWidth + COLUMN_SPACING;
			}

			if (itemInfo.ebene != null && !itemInfo.ebene.isEmpty()) {
				context.text(textRenderer, itemInfo.ebene, currentX, currentY, 0xFFFFFFFF, true);
			}
			// Immer volle Spaltenbreite — Found-Status einheitlich rechtsbündig
			currentX += tooltip.maxEbeneWidth;

			if (tooltip.showBpStatus && foundFlags != null && i < foundFlags.length) {
				boolean isFound = foundFlags[i];
				String statusSymbol = isFound ? BP_STATUS_FOUND : BP_STATUS_MISSING;
				int statusColor = isFound ? 0xFF00FF00 : 0xFFFF0000;
				context.text(textRenderer, statusSymbol, currentX, currentY, statusColor, true);
			}

			currentY += tooltip.textHeight;
		}
		return currentY;
	}

	private static void renderModifierColumn(
			GuiGraphicsExtractor context,
			Font textRenderer,
			KitFilterUtility.ItemInfo itemInfo,
			int x,
			int y,
			boolean cardEffect
	) {
		if (itemInfo.modifier == null || itemInfo.modifier.isEmpty()) {
			return;
		}

		if ("–".equals(itemInfo.modifier) || "-".equals(itemInfo.modifier) || "[]".equals(itemInfo.modifier)) {
			context.text(textRenderer, "–", x, y, 0xFFFFFFFF, true);
			return;
		}

		if (cardEffect) {
			context.text(textRenderer, itemInfo.modifier, x, y, CARD_EFFECT_GREEN, true);
			return;
		}

		String[] modifierParts = itemInfo.modifier.split(",\\s*");
		int modifierX = x;
		for (int i = 0; i < modifierParts.length; i++) {
			String part = modifierParts[i].trim();
			if (i > 0) {
				String separator = ", ";
				context.text(textRenderer, separator, modifierX, y, 0xFFFFFFFF, true);
				modifierX += textRenderer.width(separator);
			}

			if (part.startsWith("[") && part.endsWith("]")) {
				String modifierName = part.substring(1, part.length() - 1);
				if (modifierName.isEmpty()) {
					context.text(textRenderer, "–", modifierX, y, 0xFFFFFFFF, true);
					continue;
				}
				int modifierColor = KitFilterUtility.ItemInfo.parseModifierColor(modifierName);

				context.text(textRenderer, "[", modifierX, y, 0xFFFFFFFF, true);
				modifierX += textRenderer.width("[");
				context.text(textRenderer, modifierName, modifierX, y, modifierColor, true);
				modifierX += textRenderer.width(modifierName);
				context.text(textRenderer, "]", modifierX, y, 0xFFFFFFFF, true);
			} else {
				context.text(textRenderer, part, modifierX, y, 0xFFFFFFFF, true);
			}
		}
	}

	public static String blueprintLookupName(String name) {
		if (name == null || name.isEmpty()) {
			return "";
		}
		String cleaned = name.replaceAll("\\s*×\\d+$", "").trim();
		return net.felix.chat.ShareKitImportHelper.extractEquipmentBaseName(cleaned);
	}

	private static void drawActionHintLine(
			GuiGraphicsExtractor context,
			Font textRenderer,
			int x,
			int y,
			String bracketContent,
			String actionText
	) {
		drawActionHintLine(context, textRenderer, x, y, ACTION_GREEN, bracketContent, actionText);
	}

	private static void drawActionHintLine(
			GuiGraphicsExtractor context,
			Font textRenderer,
			int x,
			int y,
			int bracketColor,
			String bracketContent,
			String actionText
	) {
		int cursor = x;
		context.text(textRenderer, "[", cursor, y, bracketColor, true);
		cursor += textRenderer.width("[");
		context.text(textRenderer, bracketContent, cursor, y, bracketColor, true);
		cursor += textRenderer.width(bracketContent);
		context.text(textRenderer, "]", cursor, y, bracketColor, true);
		cursor += textRenderer.width("]");
		context.text(textRenderer, ":", cursor, y, bracketColor, true);
		cursor += textRenderer.width(":");
		context.text(textRenderer, " " + actionText, cursor, y, 0xFFFFFFFF, true);
	}

	private static int parseEbeneNumber(String ebene) {
		if (ebene == null || ebene.isEmpty()) {
			return 0;
		}
		String cleaned = ebene.trim().toLowerCase();
		if (cleaned.startsWith("e")) {
			cleaned = cleaned.substring(1);
		}
		try {
			return Integer.parseInt(cleaned);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static void drawNameWithCount(
			GuiGraphicsExtractor context,
			Font textRenderer,
			String name,
			int x,
			int y,
			int nameColor
	) {
		int times = name.lastIndexOf(" ×");
		if (times > 0 && times < name.length() - 2) {
			String suffix = name.substring(times);
			if (suffix.matches(" ×\\d+")) {
				String base = name.substring(0, times);
				context.text(textRenderer, base, x, y, nameColor, true);
				context.text(textRenderer, suffix, x + textRenderer.width(base), y, COUNT_YELLOW, true);
				return;
			}
		}
		context.text(textRenderer, name, x, y, nameColor, true);
	}

	private static int calculateItemTypeWidth(Font textRenderer, KitFilterUtility.ItemInfo itemInfo) {
		if (itemInfo.itemType != null && !itemInfo.itemType.isEmpty()) {
			return textRenderer.width(itemInfo.itemType);
		}
		return 0;
	}

	private static int calculateItemNameWidth(Font textRenderer, KitFilterUtility.ItemInfo itemInfo) {
		if (itemInfo.name != null && !itemInfo.name.isEmpty()) {
			return textRenderer.width(itemInfo.name);
		}
		return 0;
	}

	private static int calculateEbeneWidth(Font textRenderer, KitFilterUtility.ItemInfo itemInfo) {
		if (itemInfo.ebene != null && !itemInfo.ebene.isEmpty()) {
			return textRenderer.width(itemInfo.ebene);
		}
		return 0;
	}

	private static int calculateModifierWidth(Font textRenderer, KitFilterUtility.ItemInfo itemInfo) {
		if (itemInfo.modifier == null || itemInfo.modifier.isEmpty()
				|| "–".equals(itemInfo.modifier) || "-".equals(itemInfo.modifier)
				|| "[]".equals(itemInfo.modifier)) {
			return textRenderer.width("–");
		}

		int width = 0;
		String[] modifierParts = itemInfo.modifier.split(",\\s*");
		for (int i = 0; i < modifierParts.length; i++) {
			String part = modifierParts[i].trim();
			if (i > 0) {
				width += textRenderer.width(", ");
			}
			if (part.startsWith("[") && part.endsWith("]")) {
				String modifierName = part.substring(1, part.length() - 1);
				width += textRenderer.width("[");
				width += textRenderer.width(modifierName);
				width += textRenderer.width("]");
			} else {
				width += textRenderer.width(part);
			}
		}
		return width;
	}

	private static final class SectionData {
		final String title;
		final List<KitFilterUtility.ItemInfo> items;
		final KitFilterUtility.KitHoverSection.FoundMode foundMode;

		SectionData(String title, List<KitFilterUtility.ItemInfo> items,
				KitFilterUtility.KitHoverSection.FoundMode foundMode) {
			this.title = title;
			this.items = items;
			this.foundMode = foundMode;
		}
	}

	public static final class CachedSection {
		final String title;
		final List<KitFilterUtility.ItemInfo> items;
		final boolean[] foundFlags;
		final KitFilterUtility.KitHoverSection.FoundMode foundMode;

		CachedSection(
				String title,
				List<KitFilterUtility.ItemInfo> items,
				boolean[] foundFlags,
				KitFilterUtility.KitHoverSection.FoundMode foundMode) {
			this.title = title;
			this.items = Collections.unmodifiableList(items);
			this.foundFlags = foundFlags;
			this.foundMode = foundMode != null ? foundMode : KitFilterUtility.KitHoverSection.FoundMode.NONE;
		}
	}

	public static final class CachedTooltip {
		final String header;
		final List<CachedSection> sections;
		final boolean showBpStatus;
		final boolean showSelectActionHint;
		final boolean showEditActionHint;
		final boolean showExtraHint;
		final boolean showPreviewHint;
		final int maxItemTypeWidth;
		final int maxItemNameWidth;
		final int maxModifierWidth;
		final int maxEbeneWidth;
		final int totalWidth;
		final int totalHeight;
		final int bulletWidth;
		final int textHeight;
		final int padding;
		final int lineSpacing;

		private CachedTooltip(
				String header,
				List<CachedSection> sections,
				boolean showBpStatus,
				boolean showSelectActionHint,
				boolean showEditActionHint,
				boolean showExtraHint,
				boolean showPreviewHint,
				int maxItemTypeWidth,
				int maxItemNameWidth,
				int maxModifierWidth,
				int maxEbeneWidth,
				int totalWidth,
				int totalHeight,
				int bulletWidth,
				int textHeight,
				int padding,
				int lineSpacing
		) {
			this.header = header;
			this.sections = Collections.unmodifiableList(sections);
			this.showBpStatus = showBpStatus;
			this.showSelectActionHint = showSelectActionHint;
			this.showEditActionHint = showEditActionHint;
			this.showExtraHint = showExtraHint;
			this.showPreviewHint = showPreviewHint;
			this.maxItemTypeWidth = maxItemTypeWidth;
			this.maxItemNameWidth = maxItemNameWidth;
			this.maxModifierWidth = maxModifierWidth;
			this.maxEbeneWidth = maxEbeneWidth;
			this.totalWidth = totalWidth;
			this.totalHeight = totalHeight;
			this.bulletWidth = bulletWidth;
			this.textHeight = textHeight;
			this.padding = padding;
			this.lineSpacing = lineSpacing;
		}
	}

	public static final class ExtraPanel {
		final int width;
		final int height;
		final int textHeight;
		final int padding;
		final int lineSpacing;
		final List<ExtraLine> lines;

		ExtraPanel(int width, int height, int textHeight, int padding, int lineSpacing, List<ExtraLine> lines) {
			this.width = width;
			this.height = height;
			this.textHeight = textHeight;
			this.padding = padding;
			this.lineSpacing = lineSpacing;
			this.lines = lines;
		}
	}

	private static final class ExtraLine {
		final String text;
		final boolean title;

		ExtraLine(String text, boolean title) {
			this.text = text;
			this.title = title;
		}
	}
}
