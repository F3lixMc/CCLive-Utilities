package net.felix.features.npcalerts.overlay;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fStack;

import static net.felix.features.npcalerts.overlay.AlertSettings.getPercentColorForConfigKey;
import static net.felix.features.npcalerts.overlay.AlertSettings.getSeparateOverlayScale;
import static net.felix.features.npcalerts.overlay.AlertSettings.getTextColorForConfigKey;
import static net.felix.features.npcalerts.overlay.OverlayLayout.PADDING;

/**
 * Zeichnet ein einzelnes Info-Overlay (Icon, Text, Prozent).
 */
public final class SingleInfoOverlayRenderer {

	private SingleInfoOverlayRenderer() {
	}

	/**
	 * Rendert ein einzelnes Overlay für eine Information (mit Icon-Support und Hintergrund-Option)
	 */
	static void renderSingleInfoOverlay(GuiGraphicsExtractor context, Minecraft client,
			String text, String percentText, boolean showPercent, boolean showWarning, String configKey,
			boolean showIcon, int xPosition, int yPosition, boolean showBackground) {
		int textColor = getTextColorForConfigKey(configKey);
		int percentColor = getPercentColorForConfigKey(configKey);

		// Breite: Text, optional Icon samt Doppelpunkt und Prozentwert
		boolean drawsIcon = showIcon && AlertIcons.iconForKey(configKey) != null;
		int width = client.font.width(text);
		if (drawsIcon) {
			width += OverlayLayout.iconSize(client) + 2 + client.font.width(": ");
		}
		if (showPercent && percentText != null) {
			width += client.font.width(" " + percentText);
		}

		// Zeilenhöhe: Maximum aus Text-Höhe und Icon-Höhe
		int actualLineHeight = client.font.lineHeight + 2;
		if (showIcon && configKey != null) {
			actualLineHeight = Math.max(actualLineHeight, OverlayLayout.iconSize(client));
		}

		// Unskalierte Dimensionen komplett dynamisch aus dem Inhalt (wie BossHP-Overlay)
		int unscaledWidth = width + (PADDING * 2);
		int unscaledHeight = actualLineHeight + (PADDING * 2);

		float scale = getSeparateOverlayScale(configKey);
		if (scale <= 0) {
			scale = 1.0f;
		}
		int overlayWidth = Math.round(unscaledWidth * scale);
		int overlayHeight = Math.round(unscaledHeight * scale);

		int screenWidth = client.getWindow().getGuiScaledWidth();
		int posX = OverlayLayout.computeOverlayX(xPosition, overlayWidth, screenWidth);
		int posY = yPosition;

		if (showBackground) {
			context.fill(posX, posY, posX + overlayWidth, posY + overlayHeight, 0x80000000);
		}

		Matrix3x2fStack matrices = context.pose();
		matrices.pushMatrix();
		matrices.translate(posX, posY);
		matrices.scale(scale, scale);

		// Vertikal zentriert; unskalierte Höhe, da die Koordinaten nach der Matrix-Transformation relativ sind
		int overlayCenterY = unscaledHeight / 2;
		int currentY = overlayCenterY - client.font.lineHeight / 2;
		int currentTextColor = WarningBlink.colorFor(showWarning, textColor);
		boolean hasIcon = showIcon && configKey != null;
		int percentY = hasIcon ? (overlayCenterY - client.font.lineHeight / 2) : currentY;
		int currentPercentColor = showWarning ? currentTextColor : percentColor;

		try {
			int currentX = drawText(context, client, text, configKey, drawsIcon, currentTextColor,
					overlayCenterY, currentY, true);
			drawPercent(context, client, percentText, showPercent, currentX, percentY, currentPercentColor, true);
		} catch (Exception e) {
			// Fallback: gleiche Darstellung mit String-Overloads statt Component
			try {
				int currentX = drawText(context, client, text, configKey, drawsIcon, currentTextColor,
						overlayCenterY, currentY, false);
				drawPercent(context, client, percentText, showPercent, currentX, percentY, currentPercentColor, false);
			} catch (Exception e2) {
				// Ignoriere Fehler
			}
		}

		matrices.popMatrix();
	}

	/** Zeichnet Icon, Doppelpunkt und Wert (oder nur den Text) und liefert die X-Position dahinter. */
	private static int drawText(GuiGraphicsExtractor context, Minecraft client, String text, String configKey,
			boolean drawsIcon, int currentTextColor, int overlayCenterY, int currentY, boolean asComponent) {
		int currentX = PADDING;
		Identifier icon = drawsIcon ? AlertIcons.iconForKey(configKey) : null;
		if (icon == null) {
			drawString(context, client, text, currentX, currentY, currentTextColor, asComponent);
			return currentX + client.font.width(text);
		}

		int iconSize = OverlayLayout.iconSize(client);
		int iconY = overlayCenterY - iconSize / 2;
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
			textYForIcon = overlayCenterY - client.font.lineHeight / 2;
		} catch (Exception e) {
			// Fallback: Label-Text, wenn das Icon nicht geladen werden kann
			String fallbackText = AlertIcons.fallbackLabelForKey(configKey);
			if (fallbackText != null) {
				drawString(context, client, fallbackText, currentX, currentY, currentTextColor, asComponent);
				currentX += client.font.width(fallbackText);
			}
		}
		drawString(context, client, ": ", currentX, textYForIcon, currentTextColor, asComponent);
		currentX += client.font.width(": ");
		drawString(context, client, text, currentX, textYForIcon, currentTextColor, asComponent);
		return currentX + client.font.width(text);
	}

	/** Prozentwert direkt hinter dem Text; bei aktiver Warnung blinkt er mit dem Text. */
	private static void drawPercent(GuiGraphicsExtractor context, Minecraft client, String percentText,
			boolean showPercent, int currentX, int percentY, int percentColor, boolean asComponent) {
		if (showPercent && percentText != null) {
			drawString(context, client, " " + percentText, currentX, percentY, percentColor, asComponent);
		}
	}

	private static void drawString(GuiGraphicsExtractor context, Minecraft client, String value, int x, int y,
			int color, boolean asComponent) {
		if (asComponent) {
			context.text(client.font, Component.literal(value), x, y, color, true);
		} else {
			context.text(client.font, value, x, y, color, true);
		}
	}
}
