package net.felix.utilities.Town;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Cached layout + render helper for kit tooltips (avoids per-frame sorting, width calculation and BP lookups).
 */
public final class KitTooltipHelper {

	private static final int ACTION_GREEN = 0xFF16A80C;
	private static final int COLUMN_SPACING = 12;
	private static final String BULLET_POINT = "• ";

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
		if (key != null && key.equals(cacheKey) && cache != null) {
			return cache;
		}

		if (header == null || header.isEmpty()) {
			cacheKey = key;
			cache = null;
			return null;
		}

		List<KitFilterUtility.ItemInfo> items = new ArrayList<>();
		if (rawItems != null) {
			items.addAll(rawItems);
		}
		if (!items.isEmpty()) {
			items.sort((a, b) -> Integer.compare(parseEbeneNumber(a.ebene), parseEbeneNumber(b.ebene)));
		}

		int maxItemTypeWidth = 0;
		int maxItemNameWidth = 0;
		int maxModifierWidth = 0;
		int maxEbeneWidth = 0;
		for (KitFilterUtility.ItemInfo itemInfo : items) {
			maxItemTypeWidth = Math.max(maxItemTypeWidth, calculateItemTypeWidth(textRenderer, itemInfo));
			maxItemNameWidth = Math.max(maxItemNameWidth, calculateItemNameWidth(textRenderer, itemInfo));
			maxModifierWidth = Math.max(maxModifierWidth, calculateModifierWidth(textRenderer, itemInfo));
			maxEbeneWidth = Math.max(maxEbeneWidth, calculateEbeneWidth(textRenderer, itemInfo));
		}

		int bulletWidth = textRenderer.width(BULLET_POINT);
		int statusWidth = 0;
		if (showBpStatus) {
			statusWidth = textRenderer.width(" ✓") + COLUMN_SPACING;
		}

		int headerWidth = textRenderer.width(header);
		int totalWidth = Math.max(
				headerWidth,
				bulletWidth + maxItemTypeWidth + COLUMN_SPACING + maxItemNameWidth + COLUMN_SPACING
						+ maxModifierWidth + COLUMN_SPACING + maxEbeneWidth + statusWidth
		);
		if (showSelectActionHint) {
			totalWidth = Math.max(totalWidth, textRenderer.width("[Linksklick]: Auswählen"));
		}
		if (showEditActionHint) {
			totalWidth = Math.max(totalWidth, textRenderer.width("[Rechtsklick]: Bearbeiten"));
		}

		int textHeight = textRenderer.lineHeight;
		int padding = 4;
		int lineSpacing = 2;

		int totalHeight = textHeight + padding * 2;
		if (!items.isEmpty()) {
			totalHeight += lineSpacing;
			totalHeight += textHeight * items.size();
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

		boolean[] blueprintFound = null;
		if (showBpStatus && blueprintLookup != null && !items.isEmpty()) {
			blueprintFound = new boolean[items.size()];
			for (int i = 0; i < items.size(); i++) {
				KitFilterUtility.ItemInfo itemInfo = items.get(i);
				blueprintFound[i] = Boolean.TRUE.equals(blueprintLookup.apply(itemInfo.name));
			}
		}

		cacheKey = key;
		cache = new CachedTooltip(
				header,
				items,
				blueprintFound,
				showBpStatus,
				showSelectActionHint,
				showEditActionHint,
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
		if (tooltip == null) {
			return;
		}

		int offset = 10;
		int tooltipWidth = tooltip.totalWidth + tooltip.padding * 2;

		int tooltipX = mouseX + offset;
		if (tooltipX + tooltipWidth > screenWidth) {
			tooltipX = mouseX - tooltipWidth - offset;
			if (tooltipX < tooltip.padding) {
				tooltipX = tooltip.padding;
			}
		}
		if (tooltipX + tooltipWidth > screenWidth) {
			tooltipX = screenWidth - tooltipWidth - tooltip.padding;
		}

		int tooltipY = mouseY - tooltip.totalHeight - offset;
		if (tooltipY < tooltip.padding) {
			tooltipY = mouseY + offset;
			if (tooltipY + tooltip.totalHeight > screenHeight - tooltip.padding) {
				tooltipY = screenHeight - tooltip.totalHeight - tooltip.padding;
			}
		}
		if (tooltipY + tooltip.totalHeight > screenHeight - tooltip.padding) {
			tooltipY = screenHeight - tooltip.totalHeight - tooltip.padding;
		}
		if (tooltipY < tooltip.padding) {
			tooltipY = tooltip.padding;
		}

		int bgX1 = tooltipX - tooltip.padding;
		int bgY1 = tooltipY - tooltip.padding;
		int bgX2 = tooltipX + tooltip.totalWidth + tooltip.padding;
		int bgY2 = tooltipY + tooltip.totalHeight - tooltip.padding;

		context.fill(bgX1, bgY1, bgX2, bgY2, 0xF0000000);
		context.fill(bgX1, bgY1, bgX2, bgY1 + 1, 0xFFFFFFFF);
		context.fill(bgX1, bgY2 - 1, bgX2, bgY2, 0xFFFFFFFF);
		context.fill(bgX1, bgY1, bgX1 + 1, bgY2, 0xFFFFFFFF);
		context.fill(bgX2 - 1, bgY1, bgX2, bgY2, 0xFFFFFFFF);

		context.text(textRenderer, tooltip.header, tooltipX, tooltipY, 0xFFFFFFFF, true);

		int currentY = tooltipY + tooltip.textHeight;
		if (!tooltip.items.isEmpty()) {
			currentY += tooltip.lineSpacing;
		}

		for (int i = 0; i < tooltip.items.size(); i++) {
			KitFilterUtility.ItemInfo itemInfo = tooltip.items.get(i);
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
				context.text(textRenderer, itemInfo.name, currentX, currentY, nameColor, true);
			}
			currentX += tooltip.maxItemNameWidth + COLUMN_SPACING;

			renderModifierColumn(context, textRenderer, itemInfo, currentX, currentY);
			currentX += tooltip.maxModifierWidth + COLUMN_SPACING;

			if (itemInfo.ebene != null && !itemInfo.ebene.isEmpty()) {
				context.text(textRenderer, itemInfo.ebene, currentX, currentY, 0xFFFFFFFF, true);
				currentX += textRenderer.width(itemInfo.ebene);
			} else {
				currentX += tooltip.maxEbeneWidth;
			}

			if (tooltip.showBpStatus && tooltip.blueprintFound != null) {
				boolean isFound = tooltip.blueprintFound[i];
				String statusSymbol = isFound ? " ✓" : " ✗";
				int statusColor = isFound ? 0xFF00FF00 : 0xFFFF0000;
				context.text(textRenderer, statusSymbol, currentX, currentY, statusColor, true);
			}

			currentY += tooltip.textHeight;
		}

		if (tooltip.showSelectActionHint || tooltip.showEditActionHint) {
			currentY += tooltip.lineSpacing;
			if (tooltip.showSelectActionHint) {
				drawActionHintLine(context, textRenderer, tooltipX, currentY, "Linksklick", "Auswählen");
				currentY += tooltip.textHeight;
			}
			if (tooltip.showEditActionHint) {
				drawActionHintLine(context, textRenderer, tooltipX, currentY, "Rechtsklick", "Bearbeiten");
			}
		}
	}

	private static void renderModifierColumn(
			GuiGraphicsExtractor context,
			Font textRenderer,
			KitFilterUtility.ItemInfo itemInfo,
			int x,
			int y
	) {
		if (itemInfo.modifier == null || itemInfo.modifier.isEmpty()) {
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

	private static void drawActionHintLine(
			GuiGraphicsExtractor context,
			Font textRenderer,
			int x,
			int y,
			String bracketContent,
			String actionText
	) {
		int cursor = x;
		context.text(textRenderer, "[", cursor, y, ACTION_GREEN, true);
		cursor += textRenderer.width("[");
		context.text(textRenderer, bracketContent, cursor, y, ACTION_GREEN, true);
		cursor += textRenderer.width(bracketContent);
		context.text(textRenderer, "]", cursor, y, ACTION_GREEN, true);
		cursor += textRenderer.width("]");
		context.text(textRenderer, ":", cursor, y, ACTION_GREEN, true);
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
		if (itemInfo.modifier == null || itemInfo.modifier.isEmpty()) {
			return 0;
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

	public static final class CachedTooltip {
		final String header;
		final List<KitFilterUtility.ItemInfo> items;
		final boolean[] blueprintFound;
		final boolean showBpStatus;
		final boolean showSelectActionHint;
		final boolean showEditActionHint;
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
				List<KitFilterUtility.ItemInfo> items,
				boolean[] blueprintFound,
				boolean showBpStatus,
				boolean showSelectActionHint,
				boolean showEditActionHint,
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
			this.items = Collections.unmodifiableList(items);
			this.blueprintFound = blueprintFound;
			this.showBpStatus = showBpStatus;
			this.showSelectActionHint = showSelectActionHint;
			this.showEditActionHint = showEditActionHint;
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
}
