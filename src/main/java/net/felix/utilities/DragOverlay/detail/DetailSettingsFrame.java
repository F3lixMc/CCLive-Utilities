package net.felix.utilities.DragOverlay.detail;

/**
 * Sichtbare Höhe und Scrollversatz der Detailpanels.
 */
public final class DetailSettingsFrame {

    public static final int TOP_MARGIN = 48;
    public static final int BOTTOM_MARGIN = 44;
    public static final int MIN_VISIBLE = 80;

    private DetailSettingsFrame() {
    }

    public static float scale(int screenWidth, int screenHeight, int boxWidth, int contentHeight) {
        float fitWidth = Math.max(1, screenWidth - 24) / (float) boxWidth;
        float widthLimited = Math.min(0.85f, fitWidth);
        int available = Math.max(1, screenHeight - TOP_MARGIN - BOTTOM_MARGIN);
        if (contentHeight * widthLimited <= available) {
            float fitHeight = available / (float) Math.max(1, contentHeight);
            return Math.min(widthLimited, fitHeight);
        }
        return widthLimited;
    }

    public static int visibleHeight(int screenHeight, float scale, int contentHeight) {
        int available = (int) ((screenHeight - TOP_MARGIN - BOTTOM_MARGIN) / Math.max(0.01f, scale));
        return Math.max(MIN_VISIBLE, Math.min(contentHeight, available));
    }

    public static int clampScroll(int scroll, int contentHeight, int visibleHeight) {
        return Math.max(0, Math.min(scroll, Math.max(0, contentHeight - visibleHeight)));
    }
}
