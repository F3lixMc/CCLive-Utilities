package net.felix.utilities.Overall.npcalerts.overlay;

import java.util.List;
import net.minecraft.client.Minecraft;

/**
 * Gemeinsame Maße und Positionsberechnung der NPC-Alert-Overlays.
 */
final class OverlayLayout {

	static final int PADDING = 5;

	private OverlayLayout() {
	}

	/** Icon-Größe = 1.5x Text-Höhe. */
	static int iconSize(Minecraft client) {
		return (int) (client.font.lineHeight * 1.5);
	}

	/** Breite der längsten Zeile inklusive Icon, Doppelpunkt und Prozentwert. */
	static int measureMaxLineWidth(Minecraft client, List<LineWithPercent> lines) {
		int maxWidth = 0;
		for (LineWithPercent line : lines) {
			int width = 0;
			if (line.showIcon && AlertIcons.iconForKey(line.configKey) != null) {
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
	static int computeActualLineHeight(Minecraft client, List<LineWithPercent> lines) {
		int actualLineHeight = client.font.lineHeight + 2;
		for (LineWithPercent line : lines) {
			if (line.showIcon && line.configKey != null) {
				actualLineHeight = Math.max(actualLineHeight, iconSize(client));
			}
		}
		return actualLineHeight;
	}

	/**
	 * Linke Kante hängt fest, wenn das Overlay auf der linken Bildschirmhälfte liegt;
	 * auf der rechten Hälfte ist baseX die rechte Kante. Das Ergebnis bleibt im Bildschirm.
	 */
	static int computeOverlayX(int baseX, int overlayWidth, int screenWidth) {
		boolean isOnLeftSide = baseX < screenWidth / 2;
		int posX = isOnLeftSide ? baseX : baseX - overlayWidth;
		return Math.max(0, Math.min(posX, screenWidth - overlayWidth));
	}
}
