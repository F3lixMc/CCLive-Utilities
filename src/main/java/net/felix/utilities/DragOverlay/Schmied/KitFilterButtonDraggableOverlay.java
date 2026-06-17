package net.felix.utilities.DragOverlay.Schmied;

import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.DragOverlay.DraggableOverlay;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.joml.Matrix3x2fStack;

public class KitFilterButtonDraggableOverlay implements DraggableOverlay {

    private static final int DEFAULT_WIDTH = 100;
    private static final int DEFAULT_HEIGHT = 20;

    private final int buttonIndex;

    public KitFilterButtonDraggableOverlay(int buttonIndex) {
        this.buttonIndex = buttonIndex;
    }

    public int getButtonIndex() {
        return buttonIndex;
    }

    // ---- Config field accessors ----

    private int getConfigX() {
        switch (buttonIndex) {
            case 2: return CCLiveUtilitiesConfig.HANDLER.instance().kitFilterButton2X;
            case 3: return CCLiveUtilitiesConfig.HANDLER.instance().kitFilterButton3X;
            default: return CCLiveUtilitiesConfig.HANDLER.instance().kitFilterButton1X;
        }
    }

    private int getConfigY() {
        switch (buttonIndex) {
            case 2: return CCLiveUtilitiesConfig.HANDLER.instance().kitFilterButton2Y;
            case 3: return CCLiveUtilitiesConfig.HANDLER.instance().kitFilterButton3Y;
            default: return CCLiveUtilitiesConfig.HANDLER.instance().kitFilterButton1Y;
        }
    }

    private float getConfigScale() {
        switch (buttonIndex) {
            case 2: return CCLiveUtilitiesConfig.HANDLER.instance().kitFilterButton2Scale;
            case 3: return CCLiveUtilitiesConfig.HANDLER.instance().kitFilterButton3Scale;
            default: return CCLiveUtilitiesConfig.HANDLER.instance().kitFilterButton1Scale;
        }
    }

    private void setConfigX(int x) {
        switch (buttonIndex) {
            case 2: CCLiveUtilitiesConfig.HANDLER.instance().kitFilterButton2X = x; break;
            case 3: CCLiveUtilitiesConfig.HANDLER.instance().kitFilterButton3X = x; break;
            default: CCLiveUtilitiesConfig.HANDLER.instance().kitFilterButton1X = x; break;
        }
    }

    private void setConfigY(int y) {
        switch (buttonIndex) {
            case 2: CCLiveUtilitiesConfig.HANDLER.instance().kitFilterButton2Y = y; break;
            case 3: CCLiveUtilitiesConfig.HANDLER.instance().kitFilterButton3Y = y; break;
            default: CCLiveUtilitiesConfig.HANDLER.instance().kitFilterButton1Y = y; break;
        }
    }

    private void setConfigScale(float scale) {
        switch (buttonIndex) {
            case 2: CCLiveUtilitiesConfig.HANDLER.instance().kitFilterButton2Scale = scale; break;
            case 3: CCLiveUtilitiesConfig.HANDLER.instance().kitFilterButton3Scale = scale; break;
            default: CCLiveUtilitiesConfig.HANDLER.instance().kitFilterButton1Scale = scale; break;
        }
    }

    private int getBaseY() {
        switch (buttonIndex) {
            case 2: return 75;
            case 3: return 100;
            default: return 50;
        }
    }

    private int getDefaultY() {
        switch (buttonIndex) {
            case 2: return 117;
            case 3: return 115;
            default: return 119;
        }
    }

    // ---- DraggableOverlay implementation ----

    @Override
    public String getOverlayName() {
        return "Kit Filter Button " + buttonIndex;
    }

    @Override
    public int getX() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getWindow() == null) return 0;
        int screenWidth = client.getWindow().getScaledWidth();
        int baseX = screenWidth - DEFAULT_WIDTH - 20;
        return baseX + getConfigX();
    }

    @Override
    public int getY() {
        return getBaseY() + getConfigY();
    }

    @Override
    public int getWidth() {
        float scale = getConfigScale();
        if (scale <= 0) scale = 1.0f;
        return (int) (DEFAULT_WIDTH * scale);
    }

    @Override
    public int getHeight() {
        float scale = getConfigScale();
        if (scale <= 0) scale = 1.0f;
        return (int) (DEFAULT_HEIGHT * scale);
    }

    @Override
    public void setPosition(int x, int y) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getWindow() == null) return;
        int screenWidth = client.getWindow().getScaledWidth();
        int baseX = screenWidth - DEFAULT_WIDTH - 20;
        setConfigX(x - baseX);
        setConfigY(y - getBaseY());
    }

    @Override
    public void setSize(int width, int height) {
        float scaleX = (float) width / DEFAULT_WIDTH;
        float scaleY = (float) height / DEFAULT_HEIGHT;
        float scale = Math.max(0.1f, Math.min(5.0f, (scaleX + scaleY) / 2.0f));
        setConfigScale(scale);
    }

    @Override
    public void renderInEditMode(DrawContext context, int mouseX, int mouseY, float delta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;

        int x = getX();
        int y = getY();
        float scale = getConfigScale();
        if (scale <= 0) scale = 1.0f;
        int scaledWidth = (int) (DEFAULT_WIDTH * scale);
        int scaledHeight = (int) (DEFAULT_HEIGHT * scale);

        Matrix3x2fStack matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(x, y);
        matrices.scale(scale, scale);

        context.fill(0, 0, DEFAULT_WIDTH, DEFAULT_HEIGHT, 0xFF4B6A69);

        String buttonText = "Kit " + buttonIndex;
        int textWidth = client.textRenderer.getWidth(buttonText);
        context.drawText(
            client.textRenderer,
            buttonText,
            (DEFAULT_WIDTH - textWidth) / 2,
            (DEFAULT_HEIGHT - 8) / 2,
            0xFF404040,
            false
        );

        matrices.popMatrix();
        context.drawBorder(x, y, scaledWidth, scaledHeight, 0xFFFF0000);
    }

    @Override
    public void savePosition() {
        // Position is already saved in setPosition()
    }

    @Override
    public boolean isEnabled() {
        return CCLiveUtilitiesConfig.HANDLER.instance().enableMod &&
               CCLiveUtilitiesConfig.HANDLER.instance().kitFilterButtonsEnabled;
    }

    @Override
    public Text getTooltip() {
        return Text.literal("Kit Filter Button " + buttonIndex + " - Filter items by kit type and level");
    }

    @Override
    public void resetToDefault() {
        setConfigX(-215);
        setConfigY(getDefaultY());
        setConfigScale(1.0f);
    }

    @Override
    public void resetSizeToDefault() {
        setConfigScale(1.0f);
    }

    @Override
    public boolean isResizeArea(int mouseX, int mouseY) {
        int x = getX();
        int y = getY();
        int width = getWidth();
        int height = getHeight();
        int resizeAreaSize = 8;
        return mouseX >= x + width - resizeAreaSize && mouseX <= x + width &&
               mouseY >= y + height - resizeAreaSize && mouseY <= y + height;
    }
}
