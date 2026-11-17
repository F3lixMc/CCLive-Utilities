package net.felix.Community.leaderboards.panel;

import com.mojang.blaze3d.platform.NativeImage;
import java.util.List;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.felix.Community.leaderboards.LeaderboardSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/**
 * Baked Textur für die Rangliste. Der Text wird nur neu gezeichnet,
 * wenn sich Inhalt, Maßstab oder Position ändern — nicht jedes Frame.
 */
final class LeaderboardTextLayer {
    private static final Identifier TEXTURE_ID = Identifier.fromNamespaceAndPath(
        "cclive-utilities", "dynamic/leaderboard_text");

    private static DynamicTexture texture;
    private static long bakedKey = Long.MIN_VALUE;
    private static int bakedGuiWidth;
    private static int bakedGuiHeight;

    static {
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> close());
    }

    private LeaderboardTextLayer() {
    }

    static void blit(
            GuiGraphicsExtractor context,
            int originX,
            int originY,
            float scale,
            PixelFont57.ScreenGrid grid,
            LeaderboardPlaces.Entry entry,
            List<LeaderboardSnapshot.Row> rows,
            LeaderboardSnapshot.Row self,
            String localName,
            int page) {
        int guiWidth = LeaderboardInventoryOverlay.scaledWidth(scale);
        int guiHeight = LeaderboardInventoryOverlay.scaledHeight(scale);
        long key = fingerprint(originX, originY, scale, grid, entry, rows, self, localName, page, guiWidth, guiHeight);
        if (key != bakedKey || texture == null || texture.getPixels() == null
                || texture.getTextureView() == null || texture.getSampler() == null) {
            bake(originX, originY, scale, grid, entry, rows, self, localName, page, guiWidth, guiHeight);
            bakedKey = key;
        }
        if (texture == null || texture.getTextureView() == null || texture.getSampler() == null) {
            return;
        }
        // Volle Textur auf die GUI-Rechteckfläche — nicht nur die linke obere GUI-Pixelregion.
        context.blit(
            texture.getTextureView(),
            texture.getSampler(),
            originX,
            originY,
            originX + bakedGuiWidth,
            originY + bakedGuiHeight,
            0f, 1f, 0f, 1f);
    }

    static void invalidate() {
        bakedKey = Long.MIN_VALUE;
    }

    private static void close() {
        bakedKey = Long.MIN_VALUE;
        Minecraft client = Minecraft.getInstance();
        if (texture != null) {
            if (client != null && client.getTextureManager() != null) {
                client.getTextureManager().release(TEXTURE_ID);
            }
            texture.close();
            texture = null;
        }
    }

    private static void bake(
            int originX,
            int originY,
            float scale,
            PixelFont57.ScreenGrid grid,
            LeaderboardPlaces.Entry entry,
            List<LeaderboardSnapshot.Row> rows,
            LeaderboardSnapshot.Row self,
            String localName,
            int page,
            int guiWidth,
            int guiHeight) {
        int screenW = Math.max(1, guiWidth * grid.guiScale());
        int screenH = Math.max(1, guiHeight * grid.guiScale());
        NativeImage image = ensureImage(screenW, screenH);
        if (image == null) {
            return;
        }
        image.fillRect(0, 0, screenW, screenH, 0);
        int originScreenX = originX * grid.guiScale();
        int originScreenY = originY * grid.guiScale();

        PixelFont57.stampCentered(
            image, entry.title(),
            anchorX(originX, LeaderboardPanelLayout.CENTER_X, scale),
            anchorY(originY, LeaderboardPanelLayout.TYPE_CENTER_Y, scale),
            LeaderboardPanelLayout.TYPE_COLOR, grid, originScreenX, originScreenY);
        PixelFont57.stampCentered(
            image, entry.header(),
            anchorX(originX, LeaderboardPanelLayout.VALUE_HEADER_X, scale),
            anchorY(originY, LeaderboardPanelLayout.VALUE_HEADER_Y, scale),
            LeaderboardPanelLayout.TYPE_COLOR, grid, originScreenX, originScreenY);

        String selfName = self != null ? self.name() : null;
        for (int row = 0; row < rows.size(); row++) {
            LeaderboardSnapshot.Row line = rows.get(row);
            boolean own = selfName != null && selfName.equalsIgnoreCase(line.name());
            stampLine(
                image, originX, originY, LeaderboardPanelLayout.ROW_CENTER_Y[row], scale, grid,
                originScreenX, originScreenY,
                line.rank(), line.name(), format(entry.style(), line),
                own ? LeaderboardPanelLayout.SELF_COLOR : LeaderboardPanelLayout.ROW_COLOR);
        }

        if (self != null) {
            stampLine(
                image, originX, originY, LeaderboardPanelLayout.SELF_CENTER_Y, scale, grid,
                originScreenX, originScreenY,
                self.rank(), self.name(), format(entry.style(), self), LeaderboardPanelLayout.SELF_COLOR);
        } else {
            stampLine(
                image, originX, originY, LeaderboardPanelLayout.SELF_CENTER_Y, scale, grid,
                originScreenX, originScreenY,
                -1, localName, "-", LeaderboardPanelLayout.SELF_COLOR);
        }

        PixelFont57.stampAroundHyphen(
            image,
            Integer.toString(page * 10 + 1),
            Integer.toString(page * 10 + 10),
            anchorX(originX, LeaderboardPanelLayout.CENTER_X, scale),
            anchorY(originY, LeaderboardPanelLayout.PAGE_CENTER_Y, scale),
            LeaderboardPanelLayout.PAGE_COLOR,
            grid, scale, originScreenX, originScreenY);

        texture.upload();
        bakedGuiWidth = guiWidth;
        bakedGuiHeight = guiHeight;
    }

    private static void stampLine(
            NativeImage image,
            int originX,
            int originY,
            int textureCenter,
            float scale,
            PixelFont57.ScreenGrid grid,
            int originScreenX,
            int originScreenY,
            int rank,
            String name,
            String value,
            int color) {
        double top = anchorY(originY, textureCenter, scale) - 3 * grid.guiPixel();
        PixelFont57.stampRank(
            image, rank,
            anchorX(originX, LeaderboardPanelLayout.RANK_PERIOD_X, scale),
            top, color, grid, originScreenX, originScreenY);
        int limit = Math.min(LeaderboardPanelLayout.NAME_MAX, grid.maxNameChars(scale));
        String shown = name.length() > limit ? name.substring(0, limit) : name;
        PixelFont57.stampCentered(
            image, shown,
            anchorX(originX, LeaderboardPanelLayout.NAME_CENTER_X, scale),
            top + 3 * grid.guiPixel(),
            color, grid, originScreenX, originScreenY);
        if ("-".equals(value) || value.indexOf('/') >= 0) {
            PixelFont57.stampCentered(
                image, value,
                anchorX(originX, LeaderboardPanelLayout.VALUE_HEADER_X, scale),
                top + 3 * grid.guiPixel(),
                color, grid, originScreenX, originScreenY);
            return;
        }
        PixelFont57.stampRight(
            image, value,
            anchorX(originX, LeaderboardPanelLayout.VALUE_TEXT_RIGHT + 1, scale),
            top, color, grid, originScreenX, originScreenY);
    }

    private static String format(LeaderboardPlaces.Style style, LeaderboardSnapshot.Row line) {
        return switch (style) {
            case PLAYTIME -> LeaderboardNumbers.playtime(line.score());
            case LEVEL -> line.smithLevel() == null || line.smithXp() == null
                ? "-"
                : SmithSkill.display(line.smithLevel(), line.smithXp());
            case SHORT -> LeaderboardNumbers.magnitude(line.score());
            case FACTORY -> Long.toString(line.score());
        };
    }

    private static double anchorX(int originX, int textureX, float scale) {
        return originX + (-1 + textureX) * (double) scale;
    }

    private static double anchorY(int originY, int textureY, float scale) {
        return originY + (-1 + textureY) * (double) scale;
    }

    private static NativeImage ensureImage(int screenW, int screenH) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return null;
        }
        NativeImage current = texture == null ? null : texture.getPixels();
        if (texture != null && current != null
                && current.getWidth() == screenW && current.getHeight() == screenH) {
            return current;
        }
        if (texture != null) {
            client.getTextureManager().release(TEXTURE_ID);
            texture.close();
            texture = null;
        }
        texture = new DynamicTexture(() -> "cclive leaderboard text", screenW, screenH, true);
        client.getTextureManager().register(TEXTURE_ID, texture);
        return texture.getPixels();
    }

    private static long fingerprint(
            int originX,
            int originY,
            float scale,
            PixelFont57.ScreenGrid grid,
            LeaderboardPlaces.Entry entry,
            List<LeaderboardSnapshot.Row> rows,
            LeaderboardSnapshot.Row self,
            String localName,
            int page,
            int guiWidth,
            int guiHeight) {
        long hash = 1469598103934665603L;
        hash = mix(hash, originX);
        hash = mix(hash, originY);
        hash = mix(hash, Float.floatToIntBits(scale));
        hash = mix(hash, grid.guiScale());
        hash = mix(hash, grid.glyphScreenPixels());
        hash = mix(hash, guiWidth);
        hash = mix(hash, guiHeight);
        hash = mix(hash, page);
        hash = mix(hash, entry.boardId() == null ? 0 : entry.boardId().hashCode());
        hash = mix(hash, entry.title().hashCode());
        hash = mix(hash, entry.header().hashCode());
        hash = mix(hash, entry.style().ordinal());
        hash = mix(hash, rows.size());
        for (LeaderboardSnapshot.Row row : rows) {
            hash = mix(hash, row.rank());
            hash = mix(hash, row.name().hashCode());
            hash = mix(hash, Long.hashCode(row.score()));
            hash = mix(hash, row.smithLevel() == null ? 0 : row.smithLevel());
            hash = mix(hash, row.smithXp() == null ? 0 : Long.hashCode(row.smithXp()));
        }
        if (self == null) {
            hash = mix(hash, -1);
            hash = mix(hash, localName == null ? 0 : localName.hashCode());
        } else {
            hash = mix(hash, self.rank());
            hash = mix(hash, self.name().hashCode());
            hash = mix(hash, Long.hashCode(self.score()));
            hash = mix(hash, self.smithLevel() == null ? 0 : self.smithLevel());
            hash = mix(hash, self.smithXp() == null ? 0 : Long.hashCode(self.smithXp()));
        }
        return hash;
    }

    private static long mix(long hash, long value) {
        hash ^= value;
        hash *= 1099511628211L;
        return hash;
    }
}
