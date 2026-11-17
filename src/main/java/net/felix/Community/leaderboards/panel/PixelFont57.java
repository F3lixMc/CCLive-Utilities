package net.felix.Community.leaderboards.panel;

import com.mojang.blaze3d.platform.NativeImage;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.joml.Matrix3x2fStack;

/**
 * Zeichenfläche 5×7, Abstand zwischen Zeichen 1px. Der Punkt ist 1px breit.
 * Zeichen werden als horizontale Pixel-Läufe gezeichnet, nicht Pixel für Pixel.
 */
public final class PixelFont57 {
    private static final int[][] GLYPHS = new int[128][];
    /** Pro Zeichen: Läufe als {@code {x, y, width}} relativ zur Glyphenecke. */
    private static final int[][][] RUNS = new int[128][][];
    /** Ö ist 5×9. Die zwei oberen Reihen sind die Punkte, der Buchstabe darunter bleibt 7px hoch. */
    private static final int[] OE = {
        0b10001, 0b00000, 0b01110, 0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b01110
    };
    private static final int[][] OE_RUNS;

    static {
        put('0', 0b01110, 0b10001, 0b10011, 0b10101, 0b11001, 0b10001, 0b01110);
        put('1', 0b00100, 0b01100, 0b00100, 0b00100, 0b00100, 0b00100, 0b01110);
        put('2', 0b01110, 0b10001, 0b00001, 0b00010, 0b00100, 0b01000, 0b11111);
        put('3', 0b11110, 0b00001, 0b00001, 0b01110, 0b00001, 0b00001, 0b11110);
        put('4', 0b10001, 0b10001, 0b10001, 0b11111, 0b00001, 0b00001, 0b00001);
        put('5', 0b11111, 0b10000, 0b10000, 0b11110, 0b00001, 0b00001, 0b11110);
        put('6', 0b01110, 0b10000, 0b10000, 0b11110, 0b10001, 0b10001, 0b01110);
        put('7', 0b11111, 0b00001, 0b00010, 0b00100, 0b01000, 0b01000, 0b01000);
        put('8', 0b01110, 0b10001, 0b10001, 0b01110, 0b10001, 0b10001, 0b01110);
        put('9', 0b01110, 0b10001, 0b10001, 0b01111, 0b00001, 0b00001, 0b01110);
        put('A', 0b01110, 0b10001, 0b10001, 0b11111, 0b10001, 0b10001, 0b10001);
        put('B', 0b11110, 0b10001, 0b10001, 0b11110, 0b10001, 0b10001, 0b11110);
        put('C', 0b01110, 0b10001, 0b10000, 0b10000, 0b10000, 0b10001, 0b01110);
        put('D', 0b11110, 0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b11110);
        put('E', 0b11111, 0b10000, 0b10000, 0b11110, 0b10000, 0b10000, 0b11111);
        put('F', 0b11111, 0b10000, 0b10000, 0b11110, 0b10000, 0b10000, 0b10000);
        put('G', 0b01110, 0b10001, 0b10000, 0b10111, 0b10001, 0b10001, 0b01111);
        put('H', 0b10001, 0b10001, 0b10001, 0b11111, 0b10001, 0b10001, 0b10001);
        put('I', 0b01110, 0b00100, 0b00100, 0b00100, 0b00100, 0b00100, 0b01110);
        put('J', 0b00111, 0b00010, 0b00010, 0b00010, 0b00010, 0b10010, 0b01100);
        put('K', 0b10001, 0b10010, 0b11100, 0b10010, 0b10001, 0b10001, 0b10001);
        put('L', 0b10000, 0b10000, 0b10000, 0b10000, 0b10000, 0b10000, 0b11111);
        put('M', 0b10001, 0b11011, 0b10101, 0b10101, 0b10001, 0b10001, 0b10001);
        put('N', 0b10001, 0b11001, 0b10101, 0b10011, 0b10001, 0b10001, 0b10001);
        put('O', 0b01110, 0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b01110);
        put('P', 0b11110, 0b10001, 0b10001, 0b11110, 0b10000, 0b10000, 0b10000);
        put('Q', 0b01110, 0b10001, 0b10001, 0b10001, 0b10101, 0b10010, 0b01101);
        put('R', 0b11110, 0b10001, 0b10001, 0b11110, 0b10001, 0b10001, 0b10001);
        put('S', 0b01111, 0b10000, 0b10000, 0b01110, 0b00001, 0b00001, 0b11110);
        put('T', 0b11111, 0b00100, 0b00100, 0b00100, 0b00100, 0b00100, 0b00100);
        put('U', 0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b01110);
        put('V', 0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b01010, 0b00100);
        put('W', 0b10001, 0b10001, 0b10001, 0b10101, 0b10101, 0b10101, 0b01010);
        put('X', 0b10001, 0b10001, 0b01010, 0b00100, 0b01010, 0b10001, 0b10001);
        put('Y', 0b10001, 0b10001, 0b01010, 0b00100, 0b00100, 0b00100, 0b00100);
        put('Z', 0b11111, 0b00001, 0b00010, 0b00100, 0b01000, 0b10000, 0b11111);
        put('-', 0b00000, 0b00000, 0b00000, 0b11111, 0b00000, 0b00000, 0b00000);
        put('/', 0b00001, 0b00010, 0b00010, 0b00100, 0b01000, 0b01000, 0b10000);
        put('.', 0b0, 0b0, 0b0, 0b0, 0b0, 0b0, 0b1);
        put(' ', 0b0, 0b0, 0b0, 0b0, 0b0, 0b0, 0b0);
        for (int i = 0; i < GLYPHS.length; i++) {
            if (GLYPHS[i] != null) {
                RUNS[i] = buildRuns(GLYPHS[i], i == '.' ? 1 : 5, 0);
            }
        }
        OE_RUNS = buildRuns(OE, 5, -2);
    }

    private PixelFont57() {
    }

    public static int width(String text) {
        int width = 0;
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            int glyphWidth = glyphWidth(text.charAt(i));
            if (glyphWidth < 0) {
                continue;
            }
            if (count > 0) {
                width += 1;
            }
            width += glyphWidth;
            count++;
        }
        return width;
    }

    public static void draw(GuiGraphicsExtractor graphics, String text, int x, int y, int color) {
        plotRuns(text, x, y, (px, py, runWidth) -> graphics.fill(px, py, px + runWidth, py + 1, color));
    }

    /**
     * Glyphenpixel in Bildschirmpixeln, nachdem Overlay- und GUI-Skalierung zusammengekommen sind.
     * Die Kanten liegen auf dem Bildschirmraster. Passt die bevorzugte Größe nicht in die Spalten,
     * wird die nächstkleinere ganze Größe genommen.
     */
    public static final class ScreenGrid {
        static final int NAME_BOX = 99;
        static final int NAME_TEXT = 95;
        static final int RANK_BOX = 27;
        static final int RANK_TEXT = 25;
        static final int VALUE_BOX = 71;
        /** Level 100 und 999.999Q. Das ist die breiteste Schmiedeskill-Anzeige. */
        static final int VALUE_TEXT = width("100/999.999Q");

        private final int guiScale;
        private final int glyphScreenPixels;

        private ScreenGrid(int guiScale, int glyphScreenPixels) {
            this.guiScale = guiScale;
            this.glyphScreenPixels = glyphScreenPixels;
        }

        public static ScreenGrid choose(int guiScale, float overlayScale) {
            int safeGui = Math.max(1, guiScale);
            int preferred = Math.max(1, (int) Math.round(overlayScale * safeGui));
            int size = preferred;
            while (size > 1 && !fitsColumns(size, overlayScale, safeGui)) {
                size--;
            }
            return new ScreenGrid(safeGui, size);
        }

        public int guiScale() {
            return guiScale;
        }

        public int glyphScreenPixels() {
            return glyphScreenPixels;
        }

        /** Längster Name, der in der skalierten Namensspalte bleibt. */
        public int maxNameChars(float overlayScale) {
            double budget = NAME_BOX * (double) overlayScale * guiScale / glyphScreenPixels;
            int count = (int) Math.floor((budget + 1.0) / 6.0);
            if (count < 1) {
                return 1;
            }
            return Math.min(16, count);
        }

        public double guiPixel() {
            return glyphScreenPixels / (double) guiScale;
        }

        public boolean fits(int textWidth, int fieldWidth, float overlayScale) {
            return textWidth * (double) glyphScreenPixels <= fieldWidth * (double) overlayScale * guiScale + 1.0e-6;
        }

        private static boolean fitsColumns(int glyph, float overlayScale, int guiScale) {
            double pixels = overlayScale * guiScale;
            return NAME_TEXT * (double) glyph <= NAME_BOX * pixels + 1.0e-6
                && RANK_TEXT * (double) glyph <= RANK_BOX * pixels + 1.0e-6
                && VALUE_TEXT * (double) glyph <= VALUE_BOX * pixels + 1.0e-6;
        }
    }

    public static void draw(GuiGraphicsExtractor graphics, String text, double guiX, double guiY,
                            int color, ScreenGrid grid) {
        onGrid(graphics, guiX, guiY, grid, () -> draw(graphics, text, 0, 0, color));
    }

    /** Mitte der Zeichenkette auf {@code guiCenterX}, Mitte der 7px-Höhe auf {@code guiCenterY}. */
    public static void drawCentered(GuiGraphicsExtractor graphics, String text, double guiCenterX, double guiCenterY,
                                    int color, ScreenGrid grid) {
        int textWidth = width(text);
        if (textWidth <= 0) {
            return;
        }
        int fbLeft = (int) Math.round(guiCenterX * grid.guiScale()) - ((textWidth - 1) / 2) * grid.glyphScreenPixels();
        int fbTop = (int) Math.round((guiCenterY - 3 * grid.guiPixel()) * grid.guiScale());
        onScreen(graphics, fbLeft, fbTop, grid, () -> draw(graphics, text, 0, 0, color));
    }

    public static void drawRight(GuiGraphicsExtractor graphics, String text, double guiRight, double guiTop,
                                 int color, ScreenGrid grid) {
        int textWidth = width(text);
        if (textWidth <= 0) {
            return;
        }
        int fbRight = (int) Math.round(guiRight * grid.guiScale());
        int fbTop = (int) Math.round(guiTop * grid.guiScale());
        onScreen(graphics, fbRight - textWidth * grid.glyphScreenPixels(), fbTop, grid,
            () -> draw(graphics, text, 0, 0, color));
    }

    public static void drawAroundHyphen(GuiGraphicsExtractor graphics, String left, String right,
                                         double hyphenCenterGuiX, double centerGuiY, int color,
                                         ScreenGrid grid, float overlayScale) {
        double pixel = grid.guiPixel();
        double top = centerGuiY - 3 * pixel;
        double hyphenLeft = hyphenCenterGuiX - 2 * overlayScale;
        double hyphenRight = hyphenLeft + 5 * overlayScale;
        int leftWidth = width(left);
        if (leftWidth > 0) {
            draw(graphics, left, hyphenLeft - overlayScale - leftWidth * pixel, top, color, grid);
        }
        if (!right.isEmpty()) {
            draw(graphics, right, hyphenRight + overlayScale, top, color, grid);
        }
    }

    public static void drawRank(GuiGraphicsExtractor graphics, int rank, double periodGuiX, double topGuiY,
                                int color, ScreenGrid grid) {
        onGrid(graphics, periodGuiX, topGuiY, grid, () -> drawRank(graphics, rank, 0, 0, color));
    }

    static void stampCentered(NativeImage image, String text, double guiCenterX, double guiCenterY,
                              int color, ScreenGrid grid, int originScreenX, int originScreenY) {
        int textWidth = width(text);
        if (textWidth <= 0) {
            return;
        }
        int fbLeft = (int) Math.round(guiCenterX * grid.guiScale()) - ((textWidth - 1) / 2) * grid.glyphScreenPixels();
        int fbTop = (int) Math.round((guiCenterY - 3 * grid.guiPixel()) * grid.guiScale());
        stamp(image, text, fbLeft - originScreenX, fbTop - originScreenY, color, grid.glyphScreenPixels());
    }

    static void stampRight(NativeImage image, String text, double guiRight, double guiTop,
                           int color, ScreenGrid grid, int originScreenX, int originScreenY) {
        int textWidth = width(text);
        if (textWidth <= 0) {
            return;
        }
        int fbRight = (int) Math.round(guiRight * grid.guiScale());
        int fbTop = (int) Math.round(guiTop * grid.guiScale());
        stamp(image, text,
            fbRight - textWidth * grid.glyphScreenPixels() - originScreenX,
            fbTop - originScreenY,
            color, grid.glyphScreenPixels());
    }

    static void stamp(NativeImage image, String text, double guiX, double guiY,
                      int color, ScreenGrid grid, int originScreenX, int originScreenY) {
        int fbLeft = (int) Math.round(guiX * grid.guiScale());
        int fbTop = (int) Math.round(guiY * grid.guiScale());
        stamp(image, text, fbLeft - originScreenX, fbTop - originScreenY, color, grid.glyphScreenPixels());
    }

    static void stampAroundHyphen(NativeImage image, String left, String right,
                                  double hyphenCenterGuiX, double centerGuiY, int color,
                                  ScreenGrid grid, float overlayScale,
                                  int originScreenX, int originScreenY) {
        double pixel = grid.guiPixel();
        double top = centerGuiY - 3 * pixel;
        double hyphenLeft = hyphenCenterGuiX - 2 * overlayScale;
        double hyphenRight = hyphenLeft + 5 * overlayScale;
        int leftWidth = width(left);
        if (leftWidth > 0) {
            stamp(image, left, hyphenLeft - overlayScale - leftWidth * pixel, top, color, grid,
                originScreenX, originScreenY);
        }
        if (!right.isEmpty()) {
            stamp(image, right, hyphenRight + overlayScale, top, color, grid, originScreenX, originScreenY);
        }
    }

    static void stampRank(NativeImage image, int rank, double periodGuiX, double topGuiY,
                          int color, ScreenGrid grid, int originScreenX, int originScreenY) {
        int fbLeft = (int) Math.round(periodGuiX * grid.guiScale());
        int fbTop = (int) Math.round(topGuiY * grid.guiScale());
        int localX = fbLeft - originScreenX;
        int localY = fbTop - originScreenY;
        int glyph = grid.glyphScreenPixels();
        fillScaled(image, localX, localY + 6 * glyph, glyph, glyph, color);
        if (rank < 0) {
            stamp(image, "-", localX - 6 * glyph, localY, color, glyph);
            return;
        }
        int value = Math.max(0, rank);
        String digits = Integer.toString(value);
        if (digits.length() > 4) {
            digits = digits.substring(digits.length() - 4);
        }
        int rightEdge = localX - 2 * glyph;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int start = rightEdge - 4 * glyph;
            stamp(image, String.valueOf(digits.charAt(i)), start, localY, color, glyph);
            rightEdge = start - 2 * glyph;
        }
    }

    static void stamp(NativeImage image, String text, int localScreenX, int localScreenY,
                      int color, int glyphScreenPixels) {
        int pixel = Math.max(1, glyphScreenPixels);
        plotRuns(text, 0, 0, (gx, gy, runWidth) -> fillScaled(
            image,
            localScreenX + gx * pixel,
            localScreenY + gy * pixel,
            runWidth * pixel,
            pixel,
            color));
    }

    private static void fillScaled(NativeImage image, int x, int y, int width, int height, int color) {
        if (image == null || width <= 0 || height <= 0) {
            return;
        }
        int x1 = Math.max(0, x);
        int y1 = Math.max(0, y);
        int x2 = Math.min(image.getWidth(), x + width);
        int y2 = Math.min(image.getHeight(), y + height);
        if (x1 >= x2 || y1 >= y2) {
            return;
        }
        image.fillRect(x1, y1, x2 - x1, y2 - y1, color);
    }

    private static void onGrid(GuiGraphicsExtractor graphics, double guiX, double guiY, ScreenGrid grid, Runnable paint) {
        onScreen(graphics,
            (int) Math.round(guiX * grid.guiScale()),
            (int) Math.round(guiY * grid.guiScale()),
            grid, paint);
    }

    private static void onScreen(GuiGraphicsExtractor graphics, int screenX, int screenY, ScreenGrid grid, Runnable paint) {
        float pixel = grid.glyphScreenPixels() / (float) grid.guiScale();
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(screenX / (float) grid.guiScale(), screenY / (float) grid.guiScale());
        pose.scale(pixel, pixel);
        paint.run();
        pose.popMatrix();
    }

    /** Die Mitte der Zeichenkette liegt auf {@code centerX}, die Mitte der 7px-Höhe auf {@code centerY}. */
    public static void drawCentered(GuiGraphicsExtractor graphics, String text, int centerX, int centerY, int color) {
        int textWidth = width(text);
        if (textWidth <= 0) {
            return;
        }
        draw(graphics, text, centerX - (textWidth - 1) / 2, centerY - 3, color);
    }

    /**
     * Zahlen links und rechts vom Bindestrich. Der Strich selbst liegt schon im PNG,
     * 5px breit, Mitte auf {@code hyphenCenterX}.
     */
    public static void drawAroundHyphen(GuiGraphicsExtractor graphics, String left, String right,
                                         int hyphenCenterX, int centerY, int color) {
        int top = centerY - 3;
        int hyphenStart = hyphenCenterX - 2;
        int leftWidth = width(left);
        if (leftWidth > 0) {
            draw(graphics, left, hyphenStart - 1 - leftWidth, top, color);
        }
        if (!right.isEmpty()) {
            draw(graphics, right, hyphenStart + 6, top, color);
        }
    }

    /** Punkt bleibt auf {@code periodX}. Ziffern stehen rechtsbündig davor, höchstens vier. Rang unter 0 zeichnet einen Strich. */
    public static void drawRank(GuiGraphicsExtractor graphics, int rank, int periodX, int top, int color) {
        graphics.fill(periodX, top + 6, periodX + 1, top + 7, color);
        if (rank < 0) {
            draw(graphics, "-", periodX - 6, top, color);
            return;
        }
        int value = Math.max(0, rank);
        String digits = Integer.toString(value);
        if (digits.length() > 4) {
            digits = digits.substring(digits.length() - 4);
        }
        int rightEdge = periodX - 2;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int start = rightEdge - 4;
            draw(graphics, String.valueOf(digits.charAt(i)), start, top, color);
            rightEdge = start - 2;
        }
    }

    static void plot(String text, int x, int y, Pixel plot) {
        plotRuns(text, x, y, (px, py, runWidth) -> {
            for (int i = 0; i < runWidth; i++) {
                plot.pixel(px + i, py);
            }
        });
    }

    static void plotRuns(String text, int x, int y, Run plot) {
        int cursor = x;
        boolean started = false;
        for (int i = 0; i < text.length(); i++) {
            char character = Character.toUpperCase(text.charAt(i));
            int[][] runs = runs(character);
            if (runs == null) {
                continue;
            }
            if (started) {
                cursor += 1;
            }
            for (int[] run : runs) {
                plot.run(cursor + run[0], y + run[1], run[2]);
            }
            cursor += glyphWidth(character);
            started = true;
        }
    }

    interface Pixel {
        void pixel(int x, int y);
    }

    interface Run {
        void run(int x, int y, int width);
    }

    private static int glyphWidth(char character) {
        char upper = Character.toUpperCase(character);
        if (upper == 'Ö') {
            return 5;
        }
        if (rows(upper) == null) {
            return -1;
        }
        return upper == '.' ? 1 : 5;
    }

    private static int[] rows(char character) {
        if (character < 0 || character >= GLYPHS.length) {
            return null;
        }
        return GLYPHS[character];
    }

    private static int[][] runs(char character) {
        if (character == 'Ö') {
            return OE_RUNS;
        }
        if (character < 0 || character >= RUNS.length) {
            return null;
        }
        return RUNS[character];
    }

    private static void put(char character, int... rows) {
        GLYPHS[character] = rows;
    }

    private static int[][] buildRuns(int[] rows, int glyphWidth, int yOffset) {
        List<int[]> runs = new ArrayList<>();
        for (int row = 0; row < rows.length; row++) {
            int bits = rows[row];
            int col = 0;
            while (col < glyphWidth) {
                int mask = glyphWidth == 1 ? 0b1 : (0b10000 >> col);
                if ((bits & mask) == 0) {
                    col++;
                    continue;
                }
                int start = col;
                col++;
                while (col < glyphWidth) {
                    mask = glyphWidth == 1 ? 0b1 : (0b10000 >> col);
                    if ((bits & mask) == 0) {
                        break;
                    }
                    col++;
                }
                runs.add(new int[] {start, row + yOffset, col - start});
            }
        }
        return runs.toArray(int[][]::new);
    }
}
