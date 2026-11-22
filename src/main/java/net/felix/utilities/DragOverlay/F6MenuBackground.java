package net.felix.utilities.DragOverlay;

/**
 * 0 Prozent ist vollständig dunkel, 100 Prozent vollständig durchsichtig.
 */
public final class F6MenuBackground {

    private static boolean suppressUnderlyingDim;

    private F6MenuBackground() {
    }

    public static void setSuppressUnderlyingDim(boolean suppress) {
        suppressUnderlyingDim = suppress;
    }

    public static boolean suppressUnderlyingDim() {
        return suppressUnderlyingDim;
    }

    public static int alpha(int percent) {
        int clamped = Math.max(0, Math.min(100, percent));
        return Math.round((100 - clamped) * 255 / 100.0f);
    }

    public static int color(int percent) {
        return alpha(percent) << 24;
    }
}
