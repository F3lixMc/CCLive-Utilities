package net.felix.utilities.DragOverlay.Overall;

import net.felix.CCLiveUtilitiesConfig;
import net.felix.CoinTrackerDisplayMode;
import net.felix.utilities.Overall.BossBarHudValueDecoder;
import net.felix.utilities.Overall.CoinTrackerUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2fStack;

/**
 * Draggable Overlay für den Coin Tracker
 */
public class CoinTrackerDraggableOverlay implements DraggableOverlay {

    private static final int MIN_OVERLAY_WIDTH = 120;
    private static final int LINE_HEIGHT = 12;
    private static final int PADDING = 5;

    @Override
    public String getOverlayName() {
        return "Coin Tracker";
    }

    @Override
    public int getX() {
        Minecraft client = Minecraft.getInstance();
        if (client.getWindow() == null) {
            return 0;
        }

        int screenWidth = client.getWindow().getGuiScaledWidth();
        int xOffset = CCLiveUtilitiesConfig.HANDLER.instance().coinTrackerX;
        int unscaledWidth = CoinTrackerUtility.getCurrentOverlayWidth(client);

        int baseX = screenWidth - MIN_OVERLAY_WIDTH - xOffset;
        boolean isOnLeftSide = baseX < screenWidth / 2;

        if (isOnLeftSide) {
            return screenWidth - MIN_OVERLAY_WIDTH - xOffset;
        }
        return screenWidth - unscaledWidth - xOffset;
    }

    @Override
    public int getY() {
        return CCLiveUtilitiesConfig.HANDLER.instance().coinTrackerY;
    }

    @Override
    public int getWidth() {
        Minecraft client = Minecraft.getInstance();
        int unscaledWidth = client != null ? CoinTrackerUtility.getCurrentOverlayWidth(client) : MIN_OVERLAY_WIDTH;
        float scale = CCLiveUtilitiesConfig.HANDLER.instance().coinTrackerScale;
        if (scale <= 0) {
            scale = 1.0f;
        }
        return (int) (unscaledWidth * scale);
    }

    @Override
    public int getHeight() {
        float scale = CCLiveUtilitiesConfig.HANDLER.instance().coinTrackerScale;
        if (scale <= 0) {
            scale = 1.0f;
        }
        return (int) (CoinTrackerUtility.getCurrentOverlayHeight() * scale);
    }

    @Override
    public void setPosition(int x, int y) {
        Minecraft client = Minecraft.getInstance();
        if (client.getWindow() == null) {
            return;
        }

        int screenWidth = client.getWindow().getGuiScaledWidth();
        int unscaledWidth = CoinTrackerUtility.getCurrentOverlayWidth(client);
        boolean isOnLeftSide = x < screenWidth / 2;

        int xOffset;
        if (isOnLeftSide) {
            xOffset = screenWidth - MIN_OVERLAY_WIDTH - x;
        } else {
            xOffset = screenWidth - unscaledWidth - x;
        }

        CCLiveUtilitiesConfig.HANDLER.instance().coinTrackerX = xOffset;
        CCLiveUtilitiesConfig.HANDLER.instance().coinTrackerY = y;
    }

    @Override
    public void setSize(int width, int height) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return;
        }

        int unscaledWidth = CoinTrackerUtility.getCurrentOverlayWidth(client);
        int unscaledHeight = CoinTrackerUtility.getCurrentOverlayHeight();

        float scaleX = (float) width / unscaledWidth;
        float scaleY = (float) height / unscaledHeight;
        float scale = (scaleX + scaleY) / 2.0f;
        scale = Math.max(0.1f, Math.min(5.0f, scale));

        CCLiveUtilitiesConfig.HANDLER.instance().coinTrackerScale = scale;
    }

    @Override
    public void renderInEditMode(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return;
        }

        int unscaledWidth = CoinTrackerUtility.getCurrentOverlayWidth(client);
        int unscaledHeight = CoinTrackerUtility.getCurrentOverlayHeight();
        int x = getX();
        int y = getY();

        float scale = CCLiveUtilitiesConfig.HANDLER.instance().coinTrackerScale;
        if (scale <= 0) {
            scale = 1.0f;
        }

        int scaledWidth = (int) (unscaledWidth * scale);
        int scaledHeight = (int) (unscaledHeight * scale);

        context.outline(x, y, scaledWidth, scaledHeight, 0xFFFF0000);

        Matrix3x2fStack matrices = context.pose();
        matrices.pushMatrix();
        matrices.translate(x, y);
        matrices.scale(scale, scale);

        if (CCLiveUtilitiesConfig.HANDLER.instance().coinTrackerShowBackground) {
            context.fill(0, 0, unscaledWidth, unscaledHeight, 0x80000000);
        }

        int headerColor = CCLiveUtilitiesConfig.HANDLER.instance().coinTrackerHeaderColor.getRGB();
        int textColor = CCLiveUtilitiesConfig.HANDLER.instance().coinTrackerTextColor.getRGB();

        int textY = PADDING;
        context.text(client.font, Component.literal("Coin Tracker"), PADDING, textY, headerColor, true);
        textY += LINE_HEIGHT;
        context.text(client.font, Component.literal("Coins: 1,256k"), PADDING, textY, textColor, true);
        textY += LINE_HEIGHT;
        context.text(client.font, Component.literal("Gewinn: +250"), PADDING, textY, textColor, true);
        textY += LINE_HEIGHT;
        context.text(client.font, Component.literal("CPM: 1,2k"), PADDING, textY, textColor, true);
        textY += LINE_HEIGHT;
        context.text(client.font, Component.literal("Zeit: 05:30"), PADDING, textY, textColor, true);

        matrices.popMatrix();
    }

    @Override
    public void savePosition() {
        CCLiveUtilitiesConfig.HANDLER.save();
    }

    @Override
    public boolean isEnabled() {
        return isConfigEnabled()
                && CCLiveUtilitiesConfig.HANDLER.instance().coinTrackerDisplayMode == CoinTrackerDisplayMode.OVERLAY
                && BossBarHudValueDecoder.isFloorDimension();
    }

    @Override
    public boolean isConfigEnabled() {
        return CCLiveUtilitiesConfig.HANDLER.instance().coinTrackerEnabled
                && CCLiveUtilitiesConfig.HANDLER.instance().showCoinTracker;
    }

    @Override
    public Component getTooltip() {
        return Component.literal("Coin Tracker - Liest Coins aus der HUD-Bossbar und zeigt Session-Statistiken");
    }

    @Override
    public void resetToDefault() {
        CCLiveUtilitiesConfig.HANDLER.instance().coinTrackerX = 734;
        CCLiveUtilitiesConfig.HANDLER.instance().coinTrackerY = 286;
        CCLiveUtilitiesConfig.HANDLER.instance().coinTrackerScale = 1.0f;
    }

    @Override
    public void resetSizeToDefault() {
        CCLiveUtilitiesConfig.HANDLER.instance().coinTrackerScale = 1.0f;
    }
}
