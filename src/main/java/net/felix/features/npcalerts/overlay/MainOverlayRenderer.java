package net.felix.features.npcalerts.overlay;

import java.util.List;
import net.felix.CCLiveUtilitiesConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fStack;

import static net.felix.features.npcalerts.overlay.AlertSettings.getPercentColorForConfigKey;
import static net.felix.features.npcalerts.overlay.AlertSettings.getTextColorForConfigKey;
import static net.felix.features.npcalerts.overlay.MainOverlayLines.getMainOverlayLines;
import static net.felix.features.npcalerts.overlay.OverlayLayout.PADDING;
import static net.felix.features.npcalerts.overlay.SeparateOverlayRenderer.renderSeparateOverlays;

/**
 * Zeichnet das Haupt-Overlay mit allen aktiven NPC-Alerts.
 */
public final class MainOverlayRenderer {

	private static final int ICON_LINE_EXTRA_SPACING = 2;

	private MainOverlayRenderer() {
	}

	/**
	 * Rendert das NPC Alerts Overlay links auf dem Bildschirm
	 */
	public static void renderNpcAlertsDisplay(GuiGraphicsExtractor context, Minecraft client) {
		if (client.getWindow() == null) {
			return;
		}

		CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
		if (!config.npcAlertsUtilityEnabled || !config.npcAlertsOverlaysVisible) {
			return;
		}

		List<LineWithPercent> lines = getMainOverlayLines();

		// Wenn keine Zeilen vorhanden sind, rendere nur separate Overlays (auch keinen Hintergrund)
		if (lines.isEmpty()) {
			renderSeparateOverlays(context, client);
			return;
		}

		int actualLineHeight = OverlayLayout.computeActualLineHeight(client, lines);
		int iconLineCount = countIconLines(lines);

		// Unskalierte Dimensionen komplett dynamisch aus dem Inhalt (wie BossHP-Overlay)
		int unscaledWidth = OverlayLayout.measureMaxLineWidth(client, lines) + (PADDING * 2);
		int unscaledHeight = (lines.size() * actualLineHeight) + (iconLineCount * ICON_LINE_EXTRA_SPACING) + (PADDING * 2);

		float scale = config.npcAlertsMainOverlayScale;
		if (scale <= 0) {
			scale = 1.0f;
		}
		int overlayWidth = Math.round(unscaledWidth * scale);
		int overlayHeight = Math.round(unscaledHeight * scale);

		int screenWidth = client.getWindow().getGuiScaledWidth();
		int posX = OverlayLayout.computeOverlayX(config.npcAlertsMainOverlayX, overlayWidth, screenWidth);
		int posY = config.npcAlertsMainOverlayY;

		if (config.npcAlertsMainOverlayShowBackground) {
			context.fill(posX, posY, posX + overlayWidth, posY + overlayHeight, 0x80000000);
		}

		Matrix3x2fStack matrices = context.pose();
		matrices.pushMatrix();
		matrices.translate(posX, posY);
		matrices.scale(scale, scale);
		drawLines(context, client, lines, actualLineHeight, unscaledHeight);
		matrices.popMatrix();

		// Separate Overlays für Informationen, die diese Option aktiviert haben
		renderSeparateOverlays(context, client);
	}

	/** Zeile wird mit Icon gezeichnet (Icon aktiviert und Config-Key vorhanden). */
	private static boolean hasIcon(LineWithPercent line) {
		return line.showIcon && line.configKey != null;
	}

	private static int countIconLines(List<LineWithPercent> lines) {
		int count = 0;
		for (LineWithPercent line : lines) {
			if (hasIcon(line)) {
				count++;
			}
		}
		return count;
	}

	/** Zeichnet alle Zeilen vertikal zentriert (Koordinaten relativ zur Overlay-Ecke). */
	private static void drawLines(GuiGraphicsExtractor context, Minecraft client, List<LineWithPercent> lines,
			int actualLineHeight, int unscaledHeight) {
		int totalTextHeight = 0;
		for (LineWithPercent line : lines) {
			totalTextHeight += actualLineHeight;
			if (hasIcon(line)) {
				totalTextHeight += ICON_LINE_EXTRA_SPACING;
			}
		}
		int currentY = unscaledHeight / 2 - totalTextHeight / 2;

		for (LineWithPercent line : lines) {
			if (line.text == null || line.text.trim().isEmpty()) {
				currentY += actualLineHeight;
				continue;
			}
			int textColor = getTextColorForConfigKey(line.configKey);
			int percentColor = getPercentColorForConfigKey(line.configKey);
			drawLine(context, client, line, currentY, actualLineHeight, textColor, percentColor);

			currentY += actualLineHeight;
			if (hasIcon(line)) {
				currentY += ICON_LINE_EXTRA_SPACING;
			}
		}
	}

	private static void drawLine(GuiGraphicsExtractor context, Minecraft client, LineWithPercent line, int currentY,
			int actualLineHeight, int textColor, int percentColor) {
		try {
			int currentTextColor = WarningBlink.colorFor(line.showWarning, textColor);
			int currentX = hasIcon(line)
					? drawIconAndText(context, client, line, currentTextColor, currentY, actualLineHeight)
					: drawPlainText(context, client, line, currentTextColor, currentY);
			drawPercent(context, client, line, currentX, currentY, actualLineHeight, currentTextColor, percentColor);
		} catch (Exception e) {
			drawLineWithStringFallback(context, client, line, currentY, textColor, percentColor);
		}
	}

	/** Zeichnet den Text ohne Icon und liefert die X-Position hinter dem Text. */
	private static int drawPlainText(GuiGraphicsExtractor context, Minecraft client, LineWithPercent line,
			int currentTextColor, int currentY) {
		context.text(client.font, Component.literal(line.text), PADDING, currentY, currentTextColor, true);
		return PADDING + client.font.width(line.text);
	}

	/**
	 * Zeichnet Icon, Doppelpunkt und Wert (vertikal zum Icon zentriert) und liefert die X-Position hinter dem Text.
	 * Ohne bekanntes Icon wird der normale Text gezeichnet; schlägt das Icon fehl, erscheint der Label-Text.
	 */
	private static int drawIconAndText(GuiGraphicsExtractor context, Minecraft client, LineWithPercent line,
			int currentTextColor, int currentY, int actualLineHeight) {
		Identifier icon = AlertIcons.iconForKey(line.configKey);
		if (icon == null) {
			return drawPlainText(context, client, line, currentTextColor, currentY);
		}

		int currentX = PADDING;
		int iconSize = OverlayLayout.iconSize(client);
		int lineCenterY = currentY + actualLineHeight / 2;
		int iconY = lineCenterY - iconSize / 2;
		boolean iconDrawn = false;
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
			iconDrawn = true;
			textYForIcon = lineCenterY - client.font.lineHeight / 2;
		} catch (Exception e) {
			// Fallback: Label-Text, wenn das Icon nicht geladen werden kann
			String fallbackText = AlertIcons.fallbackLabelForKey(line.configKey);
			if (fallbackText != null) {
				context.text(client.font, Component.literal(fallbackText), currentX, currentY, currentTextColor, true);
				currentX += client.font.width(fallbackText);
			}
		}
		if (iconDrawn) {
			context.text(client.font, Component.literal(": "), currentX, textYForIcon, currentTextColor, true);
			currentX += client.font.width(": ");
		}
		context.text(client.font, Component.literal(line.text), currentX, textYForIcon, currentTextColor, true);
		return currentX + client.font.width(line.text);
	}

	/** Prozentwert direkt hinter dem Text; bei aktiver Warnung blinkt er mit dem Text. */
	private static void drawPercent(GuiGraphicsExtractor context, Minecraft client, LineWithPercent line, int currentX,
			int currentY, int actualLineHeight, int currentTextColor, int percentColor) {
		if (!line.showPercent || line.percentText == null) {
			return;
		}
		int currentPercentColor = line.showWarning ? currentTextColor : percentColor;
		int percentY = hasIcon(line)
				? (currentY + actualLineHeight / 2 - client.font.lineHeight / 2)
				: currentY;
		context.text(client.font, Component.literal(" " + line.percentText), currentX, percentY, currentPercentColor, true);
	}

	/** Notfall-Darstellung mit String-Overloads, falls das normale Zeichnen einer Zeile fehlschlägt. */
	private static void drawLineWithStringFallback(GuiGraphicsExtractor context, Minecraft client, LineWithPercent line,
			int currentY, int textColor, int percentColor) {
		try {
			int currentTextColor = WarningBlink.colorFor(line.showWarning, textColor);
			context.text(client.font, line.text, PADDING, currentY, currentTextColor, true);

			if (line.showPercent && line.percentText != null) {
				int currentX = PADDING + client.font.width(line.text);
				int currentPercentColor = line.showWarning ? currentTextColor : percentColor;
				context.text(client.font, " " + line.percentText, currentX, currentY, currentPercentColor, true);
			}
		} catch (Exception e) {
			// Ignoriere Fehler
		}
	}
}
