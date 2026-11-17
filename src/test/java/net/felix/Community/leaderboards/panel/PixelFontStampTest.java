package net.felix.Community.leaderboards.panel;

import com.mojang.blaze3d.platform.NativeImage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PixelFontStampTest {
    @Test
    void stampWritesVisiblePixelsInsideThePanel() {
        int guiScale = 2;
        float scale = 0.85f;
        PixelFont57.ScreenGrid grid = PixelFont57.ScreenGrid.choose(guiScale, scale);
        int guiW = LeaderboardInventoryOverlay.scaledWidth(scale);
        int guiH = LeaderboardInventoryOverlay.scaledHeight(scale);
        int originX = 100;
        int originY = 40;
        try (NativeImage image = new NativeImage(guiW * guiScale, guiH * guiScale, true)) {
            int originScreenX = originX * guiScale;
            int originScreenY = originY * guiScale;
            double top = originY + (-1 + LeaderboardPanelLayout.ROW_CENTER_Y[0]) * (double) scale
                - 3 * grid.guiPixel();
            PixelFont57.stampCentered(
                image, "PLAYERONE",
                originX + (-1 + LeaderboardPanelLayout.NAME_CENTER_X) * (double) scale,
                top + 3 * grid.guiPixel(),
                LeaderboardPanelLayout.ROW_COLOR, grid, originScreenX, originScreenY);
            PixelFont57.stampRight(
                image, "12.3K",
                originX + (-1 + LeaderboardPanelLayout.VALUE_TEXT_RIGHT + 1) * (double) scale,
                top, LeaderboardPanelLayout.ROW_COLOR, grid, originScreenX, originScreenY);
            PixelFont57.stampRank(
                image, 1,
                originX + (-1 + LeaderboardPanelLayout.RANK_PERIOD_X) * (double) scale,
                top, LeaderboardPanelLayout.ROW_COLOR, grid, originScreenX, originScreenY);

            int lit = 0;
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    if (image.getPixel(x, y) != 0) {
                        lit++;
                    }
                }
            }
            assertTrue(lit > 50, "expected stamped leaderboard glyphs, got " + lit);
        }
    }
}
