package net.felix.utilities.DragOverlay.Overall;

import net.felix.CCLiveUtilitiesConfig;
import net.felix.leaderboards.panel.LeaderboardInventoryOverlay;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * F6-Eintrag der Rangliste. Sie bleibt an der rechten Inventarkante, nur Größe und Sichtbarkeit sind einstellbar.
 */
public class LeaderboardDraggableOverlay implements DraggableOverlay {
    @Override
    public String getOverlayName() {
        return "Leaderboard";
    }

    @Override
    public int getX() {
        return LeaderboardInventoryOverlay.editorX();
    }

    @Override
    public int getY() {
        return LeaderboardInventoryOverlay.editorY();
    }

    @Override
    public int getWidth() {
        return LeaderboardInventoryOverlay.scaledWidth(LeaderboardInventoryOverlay.scale());
    }

    @Override
    public int getHeight() {
        return LeaderboardInventoryOverlay.scaledHeight(LeaderboardInventoryOverlay.scale());
    }

    @Override
    public void setPosition(int x, int y) {
        // Die Kante bleibt ein GUI-Pixel links vom Inventar, die Höhe bleibt mittig.
    }

    @Override
    public boolean allowsMove() {
        return false;
    }

    @Override
    public void setSize(int width, int height) {
        LeaderboardInventoryOverlay.applyScale(LeaderboardInventoryOverlay.scaleFromSize(width, height));
    }

    @Override
    public void renderInEditMode(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        if (!LeaderboardInventoryOverlay.hasAnchor()) {
            return;
        }
        LeaderboardInventoryOverlay.render(
            context, mouseX, mouseY,
            LeaderboardInventoryOverlay.anchorLeft(),
            LeaderboardInventoryOverlay.anchorTop(),
            LeaderboardInventoryOverlay.anchorHeight());
        int x = getX();
        int y = getY();
        int width = getWidth();
        int height = getHeight();
        context.fill(x - 1, y - 1, x + width + 1, y, 0xFFFFFF00);
        context.fill(x - 1, y + height, x + width + 1, y + height + 1, 0xFFFFFF00);
        context.fill(x - 1, y, x, y + height, 0xFFFFFF00);
        context.fill(x + width, y, x + width + 1, y + height, 0xFFFFFF00);
    }

    @Override
    public void savePosition() {
        CCLiveUtilitiesConfig.HANDLER.save();
    }

    @Override
    public boolean isEnabled() {
        return isConfigEnabled();
    }

    @Override
    public boolean isConfigEnabled() {
        return CCLiveUtilitiesConfig.HANDLER.instance().leaderboardVisible;
    }

    @Override
    public Component getTooltip() {
        return Component.literal("Leaderboard im Spielerinventar");
    }

    @Override
    public void resetToDefault() {
        resetSizeToDefault();
    }

    @Override
    public void resetSizeToDefault() {
        LeaderboardInventoryOverlay.applyScale(LeaderboardInventoryOverlay.DEFAULT_SCALE);
    }
}
