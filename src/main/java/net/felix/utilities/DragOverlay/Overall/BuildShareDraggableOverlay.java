package net.felix.utilities.DragOverlay.Overall;

import net.felix.CCLiveUtilitiesConfig;
import net.felix.chat.BuildShareManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * F6-draggable Build-Share Overlay (Icon + Übersichtspanel).
 */
public class BuildShareDraggableOverlay implements DraggableOverlay {

    @Override
    public String getOverlayName() {
        return "Build-Share";
    }

    @Override
    public int getX() {
        return BuildShareManager.getInstance().getOverlayX();
    }

    @Override
    public int getY() {
        return BuildShareManager.getInstance().getOverlayY();
    }

    @Override
    public int getWidth() {
        BuildShareManager mgr = BuildShareManager.getInstance();
        return (int) (mgr.getUnscaledWidth() * mgr.getScale());
    }

    @Override
    public int getHeight() {
        BuildShareManager mgr = BuildShareManager.getInstance();
        return (int) (mgr.getUnscaledHeight() * mgr.getScale());
    }

    @Override
    public void setPosition(int x, int y) {
        CCLiveUtilitiesConfig.HANDLER.instance().buildShareOverlayX = x;
        CCLiveUtilitiesConfig.HANDLER.instance().buildShareOverlayY = y;
    }

    @Override
    public void setSize(int width, int height) {
        BuildShareManager mgr = BuildShareManager.getInstance();
        int uw = Math.max(1, mgr.getUnscaledWidth());
        int uh = Math.max(1, mgr.getUnscaledHeight());
        float scaleX = (float) width / uw;
        float scaleY = (float) height / uh;
        float scale = (scaleX + scaleY) / 2.0f;
        scale = Math.max(0.5f, Math.min(3.0f, scale));
        CCLiveUtilitiesConfig.HANDLER.instance().buildShareOverlayScale = scale;
    }

    @Override
    public void savePosition() {
        CCLiveUtilitiesConfig.HANDLER.save();
    }

    @Override
    public boolean isEnabled() {
        if (!CCLiveUtilitiesConfig.isModRuntimeEnabled()) {
            return false;
        }
        return isConfigEnabled();
    }

    @Override
    public boolean isConfigEnabled() {
        return CCLiveUtilitiesConfig.HANDLER.instance().buildShareEnabled;
    }

    @Override
    public Component getTooltip() {
        return Component.literal("Build-Share – Ausrüstung/Karten erkennen und kopieren");
    }

    @Override
    public void resetToDefault() {
        BuildShareManager.resetOverlayToContainerAnchor();
        BuildShareManager.getInstance().setPanelExpanded(false);
    }

    @Override
    public void resetSizeToDefault() {
        CCLiveUtilitiesConfig.HANDLER.instance().buildShareOverlayScale = 1.0f;
    }

    @Override
    public void renderInEditMode(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        BuildShareManager mgr = BuildShareManager.getInstance();
        boolean wasExpanded = mgr.isPanelExpanded();
        mgr.setPanelExpanded(true);
        mgr.renderInGame(context, mouseX, mouseY, delta);
        mgr.setPanelExpanded(wasExpanded);

        context.fill(getX() - 1, getY() - 1, getX() + getWidth() + 1, getY(), 0xFFFFFF00);
        context.fill(getX() - 1, getY() + getHeight(), getX() + getWidth() + 1, getY() + getHeight() + 1, 0xFFFFFF00);
        context.fill(getX() - 1, getY(), getX(), getY() + getHeight(), 0xFFFFFF00);
        context.fill(getX() + getWidth(), getY(), getX() + getWidth() + 1, getY() + getHeight(), 0xFFFFFF00);
    }

    public static void renderInGame(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        if (!CCLiveUtilitiesConfig.HANDLER.instance().buildShareEnabled) {
            return;
        }
        BuildShareManager.getInstance().renderInGame(context, mouseX, mouseY, delta);
    }

    public static boolean handleButtonClick(int mouseX, int mouseY, int button) {
        return BuildShareManager.getInstance().handleOverlayClick(mouseX, mouseY, button);
    }
}
