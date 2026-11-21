package net.felix.utilities.DragOverlay;

/**
 * Oben und unten derselbe Innenabstand. Der Zeilenschlitz gilt zwischen den Zeilen, nicht noch einmal unter der letzten Glyphe.
 */
public final class OverlayInsets {

    private OverlayInsets() {
    }

    public static int heightForLines(int padding, int lineSlot, int fontHeight, int lines) {
        if (lines <= 0) {
            return padding * 2;
        }
        return padding + Math.max(0, lines - 1) * lineSlot + fontHeight + padding;
    }

    public static int contentSpan(int boxHeight, int contentTop, int inset) {
        return Math.max(0, boxHeight - inset - contentTop);
    }
}
