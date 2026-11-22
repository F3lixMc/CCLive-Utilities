package net.felix.utilities.Overall.npcalerts.overlay;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fStack;

import static net.felix.utilities.Overall.npcalerts.overlay.AlertSettings.getPercentColorForConfigKey;
import static net.felix.utilities.Overall.npcalerts.overlay.AlertSettings.getSeparateOverlayScale;
import static net.felix.utilities.Overall.npcalerts.overlay.AlertSettings.getTextColorForConfigKey;
import static net.felix.utilities.Overall.npcalerts.overlay.OverlayLayout.PADDING;

/**
 * Zeichnet mehrere Alert-Zeilen (z. B. Machtkristalle oder Recycler) in einem gemeinsamen Overlay.
 */
final class MultiLineOverlayRenderer {

	private MultiLineOverlayRenderer() {
	}

	/**
	 * Rendert ein Multi-Line-Overlay (für Machtkristalle, die alle 3 Slots in einem Overlay anzeigen)
	 */
	static void renderMultiLineOverlay(GuiGraphicsExtractor context, Minecraft client,
			List<LineWithPercent> lines, int xPosition, int yPosition, boolean showBackground, String configKey) {
		if (lines.isEmpty()) {
			return;
		}

		int actualLineHeight = OverlayLayout.computeActualLineHeight(client, lines);

		// Unskalierte Dimensionen komplett dynamisch aus dem Inhalt (wie BossHP-Overlay)
		int unscaledWidth = OverlayLayout.measureMaxLineWidth(client, lines) + (PADDING * 2);
		int unscaledHeight = (lines.size() * actualLineHeight) + (PADDING * 2);

		float scale = getSeparateOverlayScale(configKey);
		if (scale <= 0) {
			scale = 1.0f;
		}
		int overlayWidth = Math.round(unscaledWidth * scale);
		int overlayHeight = Math.round(unscaledHeight * scale);

		int screenWidth = client.getWindow().getGuiScaledWidth();
		int posX = OverlayLayout.computeOverlayX(xPosition, overlayWidth, screenWidth);
		int posY = yPosition;
		net.felix.utilities.DragOverlay.dock.model.OverlayDockRect separateDock = net.felix.utilities.DragOverlay.dock.OverlayDockLayout.placeLive(
				net.felix.utilities.DragOverlay.dock.model.OverlayDockIds.npc(configKey), posX, posY, overlayWidth, overlayHeight);
		if (net.felix.utilities.DragOverlay.dock.OverlayDockLayout.isMeasuring()) {
			return;
		}
		posX = separateDock.x();
		posY = separateDock.y();
		scale = net.felix.utilities.DragOverlay.dock.OverlayDockLayout.effectiveScale(
				net.felix.utilities.DragOverlay.dock.model.OverlayDockIds.npc(configKey), scale);

		if (showBackground) {
			context.fill(posX, posY, posX + separateDock.w(), posY + separateDock.h(), 0x80000000);
		}

		Matrix3x2fStack matrices = context.pose();
		matrices.pushMatrix();
		matrices.translate(posX, posY);
		matrices.scale(scale, scale);
		drawLines(context, client, lines, actualLineHeight, configKey);
		matrices.popMatrix();
	}

	private static void drawLines(GuiGraphicsExtractor context, Minecraft client, List<LineWithPercent> lines,
			int actualLineHeight, String fallbackConfigKey) {
		int currentY = PADDING;
		for (LineWithPercent line : lines) {
			if (line.text == null || line.text.trim().isEmpty()) {
				currentY += actualLineHeight;
				continue;
			}

			// Farben basieren auf line.configKey; ohne eigenen Key gilt der übergebene configKey
			String colorConfigKey = (line.configKey != null && !line.configKey.isEmpty()) ? line.configKey : fallbackConfigKey;
			int textColor = getTextColorForConfigKey(colorConfigKey);
			int percentColor = getPercentColorForConfigKey(colorConfigKey);
			int currentTextColor = WarningBlink.colorFor(line.showWarning, textColor);

			int currentX = drawText(context, client, line, currentTextColor, currentY, actualLineHeight);
			drawPercent(context, client, line, currentX, currentY, actualLineHeight, currentTextColor, percentColor);

			currentY += actualLineHeight;
		}
	}

	/** Zeichnet Icon, Doppelpunkt und Wert (oder nur den Text) und liefert die X-Position dahinter. */
	private static int drawText(GuiGraphicsExtractor context, Minecraft client, LineWithPercent line,
			int currentTextColor, int currentY, int actualLineHeight) {
		Identifier icon = line.showIcon ? AlertIcons.iconForKey(line.configKey) : null;
		if (icon == null) {
			context.text(client.font, line.text, PADDING, currentY, currentTextColor, true);
			return PADDING + client.font.width(line.text);
		}

		int currentX = PADDING;
		int iconSize = OverlayLayout.iconSize(client);
		// Zentriere Icon vertikal: Zeilen-Mitte minus die Hälfte der Icon-Höhe
		int lineCenterY = currentY + actualLineHeight / 2;
		int iconY = lineCenterY - iconSize / 2;
		int textYForIcon = currentY;
		try {
			context.blit(
					RenderPipelines.GUI_TEXTURED,
					icon,
					currentX, iconY,
					0.0f, 0.0f,
					iconSize, iconSize,
					iconSize, iconSize
			);
			currentX += iconSize + 2; // Abstand nach Icon
			textYForIcon = lineCenterY - client.font.lineHeight / 2;
		} catch (Exception e) {
			// Fallback: Label-Text, wenn das Icon nicht geladen werden kann
			String fallbackText = AlertIcons.fallbackLabelForKey(line.configKey);
			if (fallbackText != null) {
				context.text(client.font, Component.literal(fallbackText), currentX, currentY, currentTextColor, true);
				currentX += client.font.width(fallbackText);
			}
		}
		context.text(client.font, Component.literal(": "), currentX, textYForIcon, currentTextColor, true);
		currentX += client.font.width(": ");
		context.text(client.font, Component.literal(line.text), currentX, textYForIcon, currentTextColor, true);
		return currentX + client.font.width(line.text);
	}

	/** Prozentwert hinter dem Text; bei aktiver Warnung blinkt er mit dem Text. */
	private static void drawPercent(GuiGraphicsExtractor context, Minecraft client, LineWithPercent line, int currentX,
			int currentY, int actualLineHeight, int currentTextColor, int percentColor) {
		if (!line.showPercent || line.percentText == null) {
			return;
		}
		int percentY = currentY;
		if (line.showIcon && line.configKey != null) {
			percentY = currentY + actualLineHeight / 2 - client.font.lineHeight / 2;
		}
		int currentPercentColor = line.showWarning ? currentTextColor : percentColor;
		context.text(client.font, " " + line.percentText, currentX, percentY, currentPercentColor, true);
	}
}
