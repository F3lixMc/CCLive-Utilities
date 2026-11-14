package net.felix.leaderboards.panel;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeaderboardFontScaleTest {
    private static final String LONG_NAME = "SCHMIEDEMEISTER1";
    private static final String LEVEL_XP = "100/999.999Q";

    @Test
    void pixelSizeStaysInsideTheColumns() {
        assertEquals(95, PixelFont57.width(LONG_NAME));
        assertEquals(2, PixelFont57.ScreenGrid.choose(2, 1.0f).glyphScreenPixels());
        assertEquals(1, PixelFont57.ScreenGrid.choose(2, 0.85f).glyphScreenPixels());
        assertEquals(2, PixelFont57.ScreenGrid.choose(3, 0.85f).glyphScreenPixels());
        assertEquals(3, PixelFont57.ScreenGrid.choose(4, 0.85f).glyphScreenPixels());
        assertEquals(2, PixelFont57.ScreenGrid.choose(2, 1.2f).glyphScreenPixels());

        assertFits(2, 1.0f);
        assertFits(2, 0.85f);
        assertFits(3, 0.85f);
        assertFits(4, 0.85f);
        assertFits(2, 1.2f);
        assertEquals(14, PixelFont57.ScreenGrid.choose(1, 0.85f).maxNameChars(0.85f));

        long xp = 999_999_000_000_000_000L;
        String widest = SmithSkill.display(100, xp);
        assertEquals("100/999.999Q", widest);
        assertEquals("999.999Q", LeaderboardNumbers.magnitude(xp));
        assertTrue(PixelFont57.width(widest) <= PixelFont57.ScreenGrid.VALUE_BOX);
        assertTrue(PixelFont57.ScreenGrid.choose(2, 1.0f).fits(
            PixelFont57.width(widest), PixelFont57.ScreenGrid.VALUE_BOX, 1.0f));
    }

    @Test
    void fractionalScaleMakesUnevenStrokesAndWholeScreenPixelsDoNot() {
        int[] fractional = widths("M8/A.", 2, 0.85f, Mode.FRACTIONAL, 0);
        int[] exact = widths("M8/A.", 2, 0.85f, Mode.EXACT, 0);
        int[] full = widths("M8/A.", 2, 1.0f, Mode.EXACT, 0);
        int[] otherGui = widths("M8/A.", 3, 0.85f, Mode.EXACT, 0);
        int[] resized = widths("M8/A.", 2, 1.2f, Mode.EXACT, 0);
        int snappedSteps = 0;
        for (int step = 0; step < 20; step++) {
            snappedSteps = Math.max(snappedSteps, stepSpread(2, 0.85f, Mode.SNAPPED, step / 20.0));
        }

        assertTrue(spread(fractional) > 0);
        assertTrue(snappedSteps > 0);
        assertEquals(0, stepSpread(2, 0.85f, Mode.EXACT, 0.35));
        assertEquals(0, stepSpread(3, 0.85f, Mode.EXACT, 0.2));
        assertEquals(0, stepSpread(2, 1.2f, Mode.EXACT, 0.4));
        assertEquals(0, spread(exact));
        assertEquals(0, spread(full));
        assertEquals(0, spread(otherGui));
        assertEquals(0, spread(resized));
        assertEquals(full[0], PixelFont57.ScreenGrid.choose(2, 1.0f).glyphScreenPixels());
    }

    @Test
    void columnsShareOneScreenAnchor() {
        PixelFont57.ScreenGrid grid = PixelFont57.ScreenGrid.choose(3, 0.85f);
        int periodA = period(grid, 0.85f, 40);
        int periodB = period(grid, 0.85f, 80);
        assertEquals(periodA, periodB);
        int nameA = nameLeft(grid, 0.85f, "AB");
        int nameB = nameLeft(grid, 0.85f, LONG_NAME.substring(0, grid.maxNameChars(0.85f)));
        assertEquals(nameCenter(grid, 0.85f), centerOf(nameA, PixelFont57.width("AB"), grid));
        assertEquals(nameCenter(grid, 0.85f), centerOf(nameB, PixelFont57.width(LONG_NAME.substring(0, grid.maxNameChars(0.85f))), grid));
        int valueRight = (int) Math.round(anchorX(120, LeaderboardPanelLayout.VALUE_TEXT_RIGHT + 1, 0.85f) * grid.guiScale());
        assertEquals(valueRight, valueRight(grid, 0.85f, "12"));
        assertEquals(valueRight, valueRight(grid, 0.85f, LEVEL_XP));
    }

    private static void assertFits(int guiScale, float overlayScale) {
        PixelFont57.ScreenGrid grid = PixelFont57.ScreenGrid.choose(guiScale, overlayScale);
        String name = LONG_NAME.substring(0, grid.maxNameChars(overlayScale));
        assertTrue(grid.fits(PixelFont57.width(name), PixelFont57.ScreenGrid.NAME_BOX, overlayScale));
        assertTrue(grid.fits(PixelFont57.ScreenGrid.RANK_TEXT, PixelFont57.ScreenGrid.RANK_BOX, overlayScale));
        assertTrue(grid.fits(PixelFont57.width(LEVEL_XP), PixelFont57.ScreenGrid.VALUE_BOX, overlayScale));
    }

    private static int period(PixelFont57.ScreenGrid grid, float scale, int rowCenter) {
        return (int) Math.round(anchorX(80, LeaderboardPanelLayout.RANK_PERIOD_X, scale) * grid.guiScale());
    }

    private static int nameCenter(PixelFont57.ScreenGrid grid, float scale) {
        return (int) Math.round(anchorX(80, LeaderboardPanelLayout.NAME_CENTER_X, scale) * grid.guiScale());
    }

    private static int nameLeft(PixelFont57.ScreenGrid grid, float scale, String text) {
        return nameCenter(grid, scale) - ((PixelFont57.width(text) - 1) / 2) * grid.glyphScreenPixels();
    }

    private static int centerOf(int left, int textWidth, PixelFont57.ScreenGrid grid) {
        return left + ((textWidth - 1) / 2) * grid.glyphScreenPixels();
    }

    private static int valueRight(PixelFont57.ScreenGrid grid, float scale, String text) {
        int right = (int) Math.round(anchorX(120, LeaderboardPanelLayout.VALUE_TEXT_RIGHT + 1, scale) * grid.guiScale());
        int left = right - PixelFont57.width(text) * grid.glyphScreenPixels();
        return left + PixelFont57.width(text) * grid.glyphScreenPixels();
    }

    private static double anchorX(int originX, int textureX, float scale) {
        return originX + (-1 + textureX) * (double) scale;
    }

    private enum Mode { FRACTIONAL, SNAPPED, EXACT }

    private static int[] widths(String text, int guiScale, float overlayScale, Mode mode, double shiftGui) {
        PixelFont57.ScreenGrid grid = PixelFont57.ScreenGrid.choose(guiScale, overlayScale);
        double guiX = anchorX(100, LeaderboardPanelLayout.NAME_CENTER_X, overlayScale) + shiftGui;
        double guiY = 40 + (-1 + 96) * (double) overlayScale;
        int textWidth = PixelFont57.width(text);
        double step = mode == Mode.EXACT ? grid.guiPixel() : overlayScale;
        double left = guiX - ((textWidth - 1) / 2.0) * step;
        double top = guiY - 3 * step;
        boolean[][] glyph = new boolean[9][textWidth + 2];
        PixelFont57.plot(text, 1, 1, (x, y) -> glyph[y][x] = true);
        List<Integer> widths = new ArrayList<>();
        for (int y = 1; y < glyph.length - 1; y++) {
            for (int x = 1; x < glyph[y].length - 1; x++) {
                if (!glyph[y][x] || glyph[y][x - 1] || glyph[y][x + 1]) {
                    continue;
                }
                widths.add(coveredWidth(left, top, x - 1, y - 1, guiScale, overlayScale, grid, mode));
            }
        }
        return widths.stream().mapToInt(Integer::intValue).toArray();
    }

    private static int coveredWidth(double left, double top, int glyphX, int glyphY, int guiScale, float overlayScale,
                                    PixelFont57.ScreenGrid grid, Mode mode) {
        if (mode == Mode.EXACT) {
            return grid.glyphScreenPixels();
        }
        double x0 = (left + glyphX * overlayScale) * guiScale;
        double y0 = (top + glyphY * overlayScale) * guiScale;
        if (mode == Mode.SNAPPED) {
            x0 = Math.round(x0);
            y0 = Math.round(y0);
        }
        double size = overlayScale * guiScale;
        int count = 0;
        int xStart = (int) Math.floor(x0);
        int xEnd = (int) Math.floor(x0 + size - 1.0e-6);
        double sampleY = Math.floor(y0) + 0.5;
        if (sampleY < y0) {
            sampleY += 1;
        }
        for (int x = xStart; x <= xEnd; x++) {
            if (x + 0.5 >= x0 && x + 0.5 < x0 + size && sampleY >= y0 && sampleY < y0 + size) {
                count++;
            }
        }
        return count;
    }

    private static int stepSpread(int guiScale, float overlayScale, Mode mode, double shiftGui) {
        PixelFont57.ScreenGrid grid = PixelFont57.ScreenGrid.choose(guiScale, overlayScale);
        double left = anchorX(100, LeaderboardPanelLayout.NAME_CENTER_X, overlayScale) + shiftGui;
        int[] steps = new int[8];
        int previous = screenStart(left, 0, guiScale, overlayScale, grid, mode);
        for (int i = 0; i < steps.length; i++) {
            int next = screenStart(left, i + 1, guiScale, overlayScale, grid, mode);
            steps[i] = next - previous;
            previous = next;
        }
        return spread(steps);
    }

    private static int screenStart(double left, int glyphX, int guiScale, float overlayScale,
                                   PixelFont57.ScreenGrid grid, Mode mode) {
        if (mode == Mode.EXACT) {
            return (int) Math.round(left * guiScale) + glyphX * grid.glyphScreenPixels();
        }
        double x0 = (left + glyphX * overlayScale) * guiScale;
        if (mode == Mode.SNAPPED) {
            x0 = Math.round(x0);
        }
        return (int) Math.round(x0);
    }

    private static int spread(int[] runs) {
        int min = runs[0];
        int max = runs[0];
        for (int run : runs) {
            min = Math.min(min, run);
            max = Math.max(max, run);
        }
        return max - min;
    }
}
