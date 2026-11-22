package net.felix.utilities.Overall.itemviewer.tooltip;

/**
 * Passt einen fertig vermessenen Tooltip in den Bildschirm ein.
 * Die Maße sind die Inhaltsbox, wie sie der Tooltip-Renderer vor dem Hintergrund hat.
 * Der Hintergrund steht auf jeder Seite zwölf Pixel darüber.
 */
public final class ItemTooltipFit {
	public static final int MARGIN = 5;
	public static final int BACKGROUND_OUTSET = 12;

	public record Placement(float scale, int outerX, int outerY, int outerWidth, int outerHeight, boolean vanilla) {
	}

	private ItemTooltipFit() {
	}

	public static int outerWidth(int contentWidth) {
		return Math.max(0, contentWidth) + BACKGROUND_OUTSET * 2;
	}

	public static int outerHeight(int contentHeight) {
		return Math.max(0, contentHeight) + BACKGROUND_OUTSET * 2;
	}

	/**
	 * {@code contentWidth} und {@code contentHeight} sind die gezeichnete Textfläche.
	 * Passt die Vanilla-Box in den Bildschirm abzüglich des Rands, bleibt die Skalierung 1.
	 * Sonst ist die Schrift die größte Größe aus ganzen Bildschirmpixeln, die noch passt,
	 * und der Rahmen liegt mit dem normalen Innenabstand darum.
	 */
	public static Placement place(int contentWidth, int contentHeight, int screenWidth, int screenHeight,
			int mouseX, int mouseY, int guiScale) {
		int availableW = Math.max(1, screenWidth - MARGIN * 2);
		int availableH = Math.max(1, screenHeight - MARGIN * 2);
		int vanillaOuterW = outerWidth(contentWidth);
		int vanillaOuterH = outerHeight(Math.max(0, contentHeight - 2));
		if (vanillaOuterW <= availableW && vanillaOuterH <= availableH) {
			return new Placement(1f, 0, 0, vanillaOuterW, vanillaOuterH, true);
		}
		int outerW = outerWidth(contentWidth);
		int outerH = outerHeight(contentHeight);
		float needed = Math.min(availableW / (float) Math.max(1, outerW), availableH / (float) Math.max(1, outerH));
		int gui = Math.max(1, guiScale);
		int pixels = (int) Math.floor(needed * gui + 1.0e-4f);
		while (pixels > 0 && !textFillsFrame(pixels, gui, contentWidth, contentHeight, availableW, availableH)) {
			pixels--;
		}
		float scale;
		int scaledW;
		int scaledH;
		if (pixels >= 1) {
			scale = pixels / (float) gui;
			scaledW = framedSize(contentWidth, scale);
			scaledH = framedSize(contentHeight, scale);
		} else {
			scale = needed;
			scaledW = Math.max(1, Math.min(availableW, Math.round(outerW * scale)));
			scaledH = Math.max(1, Math.min(availableH, Math.round(outerH * scale)));
		}
		int x = mouseX;
		int y = mouseY - BACKGROUND_OUTSET;
		int maxX = Math.max(MARGIN, screenWidth - MARGIN - scaledW);
		int maxY = Math.max(MARGIN, screenHeight - MARGIN - scaledH);
		x = Math.max(MARGIN, Math.min(x, maxX));
		y = Math.max(MARGIN, Math.min(y, maxY));
		return new Placement(scale, x, y, scaledW, scaledH, false);
	}

	/** Text mal Skalierung plus der normale Rahmenabstand auf beiden Seiten. */
	private static int framedSize(int content, float scale) {
		return Math.max(1, Math.round(content * scale) + BACKGROUND_OUTSET * 2);
	}

	private static boolean textFillsFrame(int pixels, int gui, int contentWidth, int contentHeight,
			int availableW, int availableH) {
		float scale = pixels / (float) gui;
		return framedSize(contentWidth, scale) <= availableW && framedSize(contentHeight, scale) <= availableH;
	}

	/**
	 * Größte Schriftgröße, bei der ein Quellpixel genau {@code k} Bildschirmpixel belegt.
	 * Passt keine ganze Pixelzahl, bleibt der Faktor.
	 */
	public static float screenPixelScale(float needed, int guiScale) {
		if (needed <= 0f) {
			return needed;
		}
		int gui = Math.max(1, guiScale);
		int pixels = (int) Math.floor(needed * gui + 1.0e-4f);
		if (pixels < 1) {
			return needed;
		}
		return pixels / (float) gui;
	}
}
