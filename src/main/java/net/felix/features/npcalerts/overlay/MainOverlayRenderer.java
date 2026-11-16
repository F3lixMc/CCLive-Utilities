package net.felix.features.npcalerts.overlay;

import java.util.List;
import net.felix.CCLiveUtilitiesConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fStack;

import static net.felix.features.npcalerts.overlay.AlertIcons.AMBOSS_ICON;
import static net.felix.features.npcalerts.overlay.AlertIcons.ESSENZEN_ICON;
import static net.felix.features.npcalerts.overlay.AlertIcons.FORSCHUNG_ICON;
import static net.felix.features.npcalerts.overlay.AlertIcons.JAEGER_ICON;
import static net.felix.features.npcalerts.overlay.AlertIcons.KOMBO_KISTE_ICON;
import static net.felix.features.npcalerts.overlay.AlertIcons.MACHTKRISTALL_ICON;
import static net.felix.features.npcalerts.overlay.AlertIcons.RECYCLER_ICON;
import static net.felix.features.npcalerts.overlay.AlertIcons.SCHMELZOFEN_ICON;
import static net.felix.features.npcalerts.overlay.AlertIcons.SEELEN_ICON;
import static net.felix.features.npcalerts.overlay.AlertSettings.getPercentColorForConfigKey;
import static net.felix.features.npcalerts.overlay.AlertSettings.getTextColorForConfigKey;
import static net.felix.features.npcalerts.overlay.MainOverlayLines.getMainOverlayLines;
import static net.felix.features.npcalerts.overlay.SeparateOverlayRenderer.renderSeparateOverlays;

/**
 * Zeichnet das Haupt-Overlay mit allen aktiven NPC-Alerts.
 */
public final class MainOverlayRenderer {

	private static final int PADDING = 5;
	private static final int WARNING_COLOR = 0xFFFF0000; // Rot für Warnungen
	private static final long WARNING_BLINK_MILLIS = 300;
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

		int actualLineHeight = computeActualLineHeight(client, lines);
		int iconLineCount = countIconLines(lines);

		// Unskalierte Dimensionen komplett dynamisch aus dem Inhalt (wie BossHP-Overlay)
		int unscaledWidth = measureMaxLineWidth(client, lines) + (PADDING * 2);
		int unscaledHeight = (lines.size() * actualLineHeight) + (iconLineCount * ICON_LINE_EXTRA_SPACING) + (PADDING * 2);

		float scale = config.npcAlertsMainOverlayScale;
		if (scale <= 0) {
			scale = 1.0f;
		}
		int overlayWidth = Math.round(unscaledWidth * scale);
		int overlayHeight = Math.round(unscaledHeight * scale);

		int screenWidth = client.getWindow().getGuiScaledWidth();
		int posX = computeOverlayX(config.npcAlertsMainOverlayX, overlayWidth, screenWidth);
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

	private static int iconSize(Minecraft client) {
		return (int) (client.font.lineHeight * 1.5); // Icon-Größe = 1.5x Text-Höhe
	}

	/** Zeile wird mit Icon gezeichnet (Icon aktiviert und Config-Key vorhanden). */
	private static boolean hasIcon(LineWithPercent line) {
		return line.showIcon && line.configKey != null;
	}

	/** Breite der längsten Zeile inklusive Icon, Doppelpunkt und Prozentwert. */
	private static int measureMaxLineWidth(Minecraft client, List<LineWithPercent> lines) {
		int maxWidth = 0;
		for (LineWithPercent line : lines) {
			int width = 0;
			if (line.showIcon && iconForKey(line.configKey) != null) {
				width += iconSize(client) + 2; // Icon + Abstand
				width += client.font.width(": "); // Doppelpunkt nach Icon
			}
			width += client.font.width(line.text);
			if (line.showPercent && line.percentText != null) {
				width += client.font.width(" " + line.percentText); // Abstand + Prozente
			}
			maxWidth = Math.max(maxWidth, width);
		}
		return maxWidth;
	}

	/** Zeilenhöhe: Maximum aus Text-Höhe und Icon-Höhe, sobald mindestens eine Zeile ein Icon hat. */
	private static int computeActualLineHeight(Minecraft client, List<LineWithPercent> lines) {
		int actualLineHeight = client.font.lineHeight + 2;
		for (LineWithPercent line : lines) {
			if (hasIcon(line)) {
				actualLineHeight = Math.max(actualLineHeight, iconSize(client));
			}
		}
		return actualLineHeight;
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

	/**
	 * Linke Kante hängt fest, wenn das Overlay auf der linken Bildschirmhälfte liegt;
	 * auf der rechten Hälfte ist baseX die rechte Kante. Das Ergebnis bleibt im Bildschirm.
	 */
	private static int computeOverlayX(int baseX, int overlayWidth, int screenWidth) {
		boolean isOnLeftSide = baseX < screenWidth / 2;
		int posX = isOnLeftSide ? baseX : baseX - overlayWidth;
		return Math.max(0, Math.min(posX, screenWidth - overlayWidth));
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
			int currentTextColor = blinkingWarningColor(line, textColor);
			int currentX = hasIcon(line)
					? drawIconAndText(context, client, line, currentTextColor, currentY, actualLineHeight)
					: drawPlainText(context, client, line, currentTextColor, currentY);
			drawPercent(context, client, line, currentX, currentY, actualLineHeight, currentTextColor, percentColor);
		} catch (Exception e) {
			drawLineWithStringFallback(context, client, line, currentY, textColor, percentColor);
		}
	}

	/** Rot und blinkend, solange die Warnung der Zeile aktiv ist. */
	private static int blinkingWarningColor(LineWithPercent line, int textColor) {
		if (!line.showWarning) {
			return textColor;
		}
		boolean isVisible = (System.currentTimeMillis() / WARNING_BLINK_MILLIS) % 2 == 0;
		return isVisible ? WARNING_COLOR : textColor;
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
		Identifier icon = iconForKey(line.configKey);
		if (icon == null) {
			return drawPlainText(context, client, line, currentTextColor, currentY);
		}

		int currentX = PADDING;
		int iconSize = iconSize(client);
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
			String fallbackText = fallbackLabelForKey(line.configKey);
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
			int currentTextColor = blinkingWarningColor(line, textColor);
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

	/** Icon-Textur eines Alerts, oder {@code null} wenn der Key kein Icon hat. */
	private static Identifier iconForKey(String configKey) {
		if (configKey == null) {
			return null;
		}
		return switch (configKey) {
			case "forschung" -> FORSCHUNG_ICON;
			case "amboss" -> AMBOSS_ICON;
			case "schmelzofen" -> SCHMELZOFEN_ICON;
			case "seelen" -> SEELEN_ICON;
			case "essenzen" -> ESSENZEN_ICON;
			case "jaeger" -> JAEGER_ICON;
			case "komboKiste" -> KOMBO_KISTE_ICON;
			case "recyclerSlot1", "recyclerSlot2", "recyclerSlot3" -> RECYCLER_ICON;
			case "machtkristalle" -> MACHTKRISTALL_ICON;
			default -> null;
		};
	}

	/** Label, das statt des Icons gezeichnet wird, wenn die Textur nicht geladen werden kann. */
	private static String fallbackLabelForKey(String configKey) {
		if (configKey == null) {
			return null;
		}
		return switch (configKey) {
			case "forschung" -> "Forschung: ";
			case "amboss" -> "Amboss: ";
			case "schmelzofen" -> "Schmelzofen: ";
			case "seelen" -> "Seelen: ";
			case "essenzen" -> "Essenzen: ";
			case "jaeger" -> "Jäger: ";
			case "komboKiste" -> "Kombo Kiste: ";
			case "recyclerSlot1" -> "Recycler Slot 1: ";
			case "recyclerSlot2" -> "Recycler Slot 2: ";
			case "recyclerSlot3" -> "Recycler Slot 3: ";
			case "machtkristalle" -> "MK: ";
			default -> null;
		};
	}
}