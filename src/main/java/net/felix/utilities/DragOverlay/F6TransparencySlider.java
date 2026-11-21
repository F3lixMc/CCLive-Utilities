package net.felix.utilities.DragOverlay;

import com.mojang.blaze3d.platform.InputConstants;
import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.Overall.language.ModTexts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * F6-Regler mit gelbem Rahmen. Linksklick ist in 26.3 die Taste 1.
 */
public final class F6TransparencySlider extends AbstractWidget {

    private static final int FRAME = 0xFFFFFF00;
    private boolean dragging;

    public F6TransparencySlider(int x, int y, int width, int height) {
        super(x, y, width, height, ModTexts.text("cclive.f6.transparency"));
    }

    public boolean isDragging() {
        return dragging;
    }

    public void stopDragging() {
        dragging = false;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT || !isMouseOver(event.x(), event.y())) {
            return false;
        }
        dragging = true;
        setFocused(true);
        apply(event.x());
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (!dragging || event.button() != InputConstants.MOUSE_BUTTON_LEFT) {
            return false;
        }
        apply(event.x());
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!isFocused()) {
            return false;
        }
        int delta = 0;
        if (event.isLeft()) {
            delta = -1;
        } else if (event.isRight()) {
            delta = 1;
        } else {
            return false;
        }
        int percent = Math.max(0, Math.min(100, currentPercent() + delta));
        CCLiveUtilitiesConfig.HANDLER.instance().f6MenuBackgroundTransparency = percent;
        return true;
    }

    @Override
    public void onRelease(MouseButtonEvent event) {
        dragging = false;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.outline(getX(), getY(), getWidth(), getHeight(), FRAME);
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return;
        }
        int percent = currentPercent();
        graphics.text(client.font, ModTexts.get("cclive.f6.transparency"), getX() + 4, getY() + 3, 0xFFFFFFFF, true);
        int trackLeft = getX() + 4;
        int trackRight = getX() + getWidth() - 32;
        int trackY = getY() + getHeight() - 7;
        graphics.fill(trackLeft, trackY - 1, Math.max(trackLeft + 8, trackRight), trackY + 1, 0xFF8A8A8A);
        int span = Math.max(1, trackRight - trackLeft - 4);
        int handleX = trackLeft + (int) Math.round(percent / 100.0 * span);
        graphics.fill(handleX, trackY - 5, handleX + 4, trackY + 5, 0xFFFFFFFF);
        graphics.text(client.font, percent + "%", trackRight + 4, trackY - 4, 0xFFFFFFFF, true);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }

    private void apply(double mouseX) {
        int trackLeft = getX() + 4;
        int trackRight = getX() + getWidth() - 32;
        int span = Math.max(1, trackRight - trackLeft);
        double t = (mouseX - trackLeft) / span;
        int percent = (int) Math.round(Math.max(0.0, Math.min(1.0, t)) * 100.0);
        CCLiveUtilitiesConfig.HANDLER.instance().f6MenuBackgroundTransparency = percent;
    }

    private static int currentPercent() {
        return Math.max(0, Math.min(100, CCLiveUtilitiesConfig.HANDLER.instance().f6MenuBackgroundTransparency));
    }
}
