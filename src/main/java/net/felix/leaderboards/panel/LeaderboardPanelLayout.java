package net.felix.leaderboards.panel;

import net.minecraft.resources.Identifier;

/**
 * Pixel von leaderboard_ui.png, 231×275.
 * Angaben sind Pixelindizes ab der linken oberen Ecke. Eine Box-Kante ist inklusive.
 * Der gezeichnete Rand des PNG liegt 1px außerhalb der Spaltenboxen.
 */
public final class LeaderboardPanelLayout {
    public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
        "cclive-utilities", "textures/gui/leaderboard_ui.png");

    public static final int TEX_W = 231;
    public static final int TEX_H = 275;

    /** Mittleres Pixel der Breite: 115 | 1 | 115. */
    public static final int CENTER_X = 115;

    public static final int TYPE_COLOR = 0xFF28C5D1;
    public static final int ROW_COLOR = 0xFFFFFFFF;
    public static final int SELF_COLOR = 0xFF00FF0A;
    public static final int PAGE_COLOR = 0xFF38504F;
    public static final int HOVER_FILL = 0x66FFFFFF;
    public static final int HOVER_EDGE = 0xFFFFFFFF;

    public static final int TYPE_CENTER_Y = 37;
    public static final int VALUE_HEADER_X = 180;
    public static final int VALUE_HEADER_Y = 64;

    public static final Hit TYPE_PREV = new Hit(39, 28, 56, 44);
    public static final Hit TYPE_NEXT = new Hit(174, 28, 191, 44);

    /** Rang-Box 17–43. Text 25px, 1px Abstand zur Boxkante. Der Punkt liegt auf x=42. */
    public static final int RANK_PERIOD_X = 42;
    /** Name-Box 45–143. Die Mitte des Namens liegt auf der Mitte der Box (Pixel 94). */
    public static final int NAME_CENTER_X = 94;
    public static final int NAME_MAX = 16;
    /** Wert-Box 145–215. Volle Form startet bei 147, kurze Form endet auf 213. */
    public static final int VALUE_TEXT_X = 147;
    public static final int VALUE_TEXT_RIGHT = 213;

    public static final int[] ROW_CENTER_Y = {96, 108, 120, 132, 144, 156, 168, 180, 192, 204};

    /** Eigene Zeile 231–241, Mitte 236. */
    public static final int SELF_CENTER_Y = 236;

    /** Seitenfeld 258–268, Mitte 263. Die Mitte des Bindestrichs liegt auf CENTER_X. */
    public static final int PAGE_CENTER_Y = 263;
    public static final Hit PAGE_PREV = new Hit(49, 256, 88, 268);
    public static final Hit PAGE_NEXT = new Hit(144, 256, 181, 268);

    private LeaderboardPanelLayout() {
    }

    public record Hit(int x1, int y1, int x2, int y2) {
        public boolean contains(int x, int y) {
            return x >= x1 && x <= x2 && y >= y1 && y <= y2;
        }
    }
}
