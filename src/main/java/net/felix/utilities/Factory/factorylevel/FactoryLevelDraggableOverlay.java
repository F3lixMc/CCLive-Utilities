package net.felix.utilities.Factory.factorylevel;

import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.DragOverlay.Overall.DraggableOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * F6-Rahmen für den Fabriklevel-Tracker. Die Höhe folgt der aktuellen Zeilenzahl.
 */
public class FactoryLevelDraggableOverlay implements DraggableOverlay {
    @Override
    public String getOverlayName() {
        return "Fabriklevel Progress";
    }

    @Override
    public int getX() {
        return FactoryLevelTracker.x();
    }

    @Override
    public int getY() {
        return FactoryLevelTracker.y();
    }

    @Override
    public int getWidth() {
        Minecraft client = Minecraft.getInstance();
        float scale = scale();
        int unscaled = client != null ? FactoryLevelTracker.unscaledWidth(client) : FactoryLevelTracker.MIN_OVERLAY_WIDTH;
        return (int) (unscaled * scale);
    }

    @Override
    public int getHeight() {
        return (int) (FactoryLevelTracker.unscaledHeight() * scale());
    }

    @Override
    public void setPosition(int x, int y) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.getWindow() == null) {
            return;
        }
        int screenWidth = client.getWindow().getGuiScaledWidth();
        int unscaledWidth = FactoryLevelTracker.unscaledWidth(client);
        int xOffset;
        if (x < screenWidth / 2) {
            xOffset = screenWidth - FactoryLevelTracker.MIN_OVERLAY_WIDTH - x;
        } else {
            xOffset = screenWidth - unscaledWidth - x;
        }
        CCLiveUtilitiesConfig.HANDLER.instance().factoryLevelX = xOffset;
        CCLiveUtilitiesConfig.HANDLER.instance().factoryLevelY = y;
    }

    @Override
    public void setSize(int width, int height) {
        int unscaledWidth = FactoryLevelTracker.unscaledWidth(Minecraft.getInstance());
        int unscaledHeight = FactoryLevelTracker.unscaledHeight();
        if (unscaledWidth <= 0 || unscaledHeight <= 0) {
            return;
        }
        float scaleX = (float) width / unscaledWidth;
        float scaleY = (float) height / unscaledHeight;
        float scale = (scaleX + scaleY) / 2.0f;
        CCLiveUtilitiesConfig.HANDLER.instance().factoryLevelScale = Math.max(0.1f, Math.min(5.0f, scale));
    }

    @Override
    public void renderInEditMode(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        int x = getX();
        int y = getY();
        context.outline(x, y, getWidth(), getHeight(), 0xFFFF0000);
        FactoryLevelTracker.render(context);
    }

    @Override
    public void savePosition() {
        CCLiveUtilitiesConfig.HANDLER.save();
    }

    @Override
    public boolean isEnabled() {
        return CCLiveUtilitiesConfig.isModRuntimeEnabled()
                && isConfigEnabled()
                && FactoryLevelTracker.isInFactory();
    }

    @Override
    public boolean isConfigEnabled() {
        CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
        return config.factoryLevelEnabled && config.showFactoryLevel;
    }

    @Override
    public Component getTooltip() {
        return Component.literal("Fabriklevel Progress");
    }

    @Override
    public void resetToDefault() {
        CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
        config.factoryLevelX = 8;
        config.factoryLevelY = 36;
        config.factoryLevelScale = 1.0f;
    }

    @Override
    public void resetSizeToDefault() {
        CCLiveUtilitiesConfig.HANDLER.instance().factoryLevelScale = 1.0f;
    }

    private static float scale() {
        float scale = CCLiveUtilitiesConfig.HANDLER.instance().factoryLevelScale;
        return scale <= 0f ? 1.0f : scale;
    }
}
