package net.felix.utilities.Overall.npcalerts.overlay;

/**
 * Blink-Animation für Warnungen: abwechselnd Rot und normale Textfarbe.
 */
final class WarningBlink {

	private static final int WARNING_COLOR = 0xFFFF0000;
	private static final long BLINK_MILLIS = 300;

	private WarningBlink() {
	}

	/** Rot und blinkend, solange die Warnung aktiv ist; sonst die normale Textfarbe. */
	static int colorFor(boolean showWarning, int textColor) {
		if (!showWarning) {
			return textColor;
		}
		boolean isVisible = (System.currentTimeMillis() / BLINK_MILLIS) % 2 == 0;
		return isVisible ? WARNING_COLOR : textColor;
	}
}
