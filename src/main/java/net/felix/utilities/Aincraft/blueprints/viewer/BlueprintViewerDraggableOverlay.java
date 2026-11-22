package net.felix.utilities.Aincraft.blueprints.viewer;

import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.DragOverlay.DraggableOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class BlueprintViewerDraggableOverlay implements DraggableOverlay {

    private static final int DEFAULT_WIDTH = 200;
    private static final int DEFAULT_HEIGHT = 100;
    private static final Identifier BLUEPRINT_BACKGROUND_TEXTURE = Identifier.fromNamespaceAndPath("cclive-utilities", "textures/gui/blueprint_background.png");

    @Override
    public String getOverlayName() {
        return "Blueprint Viewer";
    }

    @Override
    public String dockId() {
        return net.felix.utilities.DragOverlay.dock.model.OverlayDockIds.BLUEPRINT;
    }

    @Override
    public int getX() {
        return net.felix.utilities.DragOverlay.dock.OverlayDockLayout.x(this);
    }

    @Override
    public int getY() {
        return net.felix.utilities.DragOverlay.dock.OverlayDockLayout.y(this);
    }

    @Override
    public int undockedX() {
        Minecraft client = Minecraft.getInstance();
        if (client.getWindow() == null) return 0;
        int screenWidth = client.getWindow().getGuiScaledWidth();
        int xOffset = CCLiveUtilitiesConfig.HANDLER.instance().blueprintViewerX;
        int unscaledWidth = calculateUnscaledWidth();
        int baseX = screenWidth - DEFAULT_WIDTH - xOffset;
        boolean isOnLeftSide = baseX < screenWidth / 2;
        if (isOnLeftSide) {
            return baseX;
        } else {
            return screenWidth - unscaledWidth - xOffset;
        }
    }

    @Override
    public int undockedY() {
        return CCLiveUtilitiesConfig.HANDLER.instance().blueprintViewerY;
    }

    @Override
    public int getWidth() {
        return calculateDynamicWidth();
    }

    @Override
    public int getHeight() {
        return calculateDynamicHeight();
    }

    @Override
    public void setPosition(int x, int y) {
        Minecraft client = Minecraft.getInstance();
        if (client.getWindow() == null) return;
        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();
        int unscaledWidth = calculateUnscaledWidth();
        boolean isOnLeftSide = x < screenWidth / 2;
        int xOffset;
        if (isOnLeftSide) {
            xOffset = screenWidth - DEFAULT_WIDTH - x;
        } else {
            xOffset = screenWidth - unscaledWidth - x;
        }
        int clampedY = Math.max(0, Math.min(y, Math.max(0, screenHeight - getHeight())));
        CCLiveUtilitiesConfig.HANDLER.instance().blueprintViewerX = xOffset;
        CCLiveUtilitiesConfig.HANDLER.instance().blueprintViewerY = clampedY;
        CCLiveUtilitiesConfig.HANDLER.instance().blueprintViewerYStoredAsPixels = true;
    }

    @Override
    public void setSize(int width, int height) {
        int unscaledWidth = calculateUnscaledWidth();
        int unscaledHeight = calculateUnscaledHeightUnscaled();
        float scaleX = (float) width / unscaledWidth;
        float scaleY = (float) height / unscaledHeight;
        float scale = Math.max(0.1f, Math.min(5.0f, (scaleX + scaleY) / 2.0f));
        CCLiveUtilitiesConfig.HANDLER.instance().blueprintViewerScale = scale;
    }

    @Override
    public void renderInEditMode(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        int x = getX();
        int y = getY();
        int width = net.felix.utilities.DragOverlay.dock.OverlayDockLayout.shownWidth(this);
        int height = net.felix.utilities.DragOverlay.dock.OverlayDockLayout.shownHeight(this);
        net.felix.utilities.DragOverlay.OverlayType overlayType = CCLiveUtilitiesConfig.HANDLER.instance().blueprintViewerOverlayType;
        if (overlayType == net.felix.utilities.DragOverlay.OverlayType.CUSTOM) {
            try {
                context.blit(RenderPipelines.GUI_TEXTURED, BLUEPRINT_BACKGROUND_TEXTURE,
                    x, y, 0.0f, 0.0f, width, height, width, height);
            } catch (Exception e) {
                context.fill(x, y, x + width, y + height, 0x80000000);
            }
        } else if (overlayType == net.felix.utilities.DragOverlay.OverlayType.BLACK) {
            context.fill(x, y, x + width, y + height, 0x80000000);
        }
        context.outline(x, y, width, height, 0xFFFF0000);
        context.text(Minecraft.getInstance().font, getOverlayName(), x + 5, y + 5, 0xFFFFFFFF, true);
        renderBlueprintData(context, x, y, width, height);
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
return CCLiveUtilitiesConfig.HANDLER.instance().showBlueprintViewer;
    }

    @Override
    public Component getTooltip() {
        return Component.literal("Blueprint Viewer - Shows blueprint information and materials");
    }

    @Override
    public void resetToDefault() {
        net.felix.utilities.DragOverlay.dock.OverlayDockStore.removeLinksInvolving(dockId());
        CCLiveUtilitiesConfig.HANDLER.instance().blueprintViewerX = 654;
        CCLiveUtilitiesConfig.HANDLER.instance().blueprintViewerY = 199;
        CCLiveUtilitiesConfig.HANDLER.instance().blueprintViewerYStoredAsPixels = true;
        CCLiveUtilitiesConfig.HANDLER.instance().blueprintViewerScale = 1.0f;
    }

    @Override
    public void resetSizeToDefault() {
        CCLiveUtilitiesConfig.HANDLER.instance().blueprintViewerScale = 1.0f;
    }

    // ---- Helpers using public getters instead of reflection ----

    private int calculateUnscaledWidth() {
        try {
            return BPViewerUtility.getInstance().layoutUnscaledWidth();
        } catch (Exception e) {
            return DEFAULT_WIDTH;
        }
    }

    private int calculateUnscaledHeightUnscaled() {
        try {
            return BPViewerUtility.getInstance().layoutUnscaledHeight();
        } catch (Exception e) {
            return DEFAULT_HEIGHT;
        }
    }

    private int scaled(int unscaled) {
        float scale = CCLiveUtilitiesConfig.HANDLER.instance().blueprintViewerScale;
        if (scale <= 0) scale = 1.0f;
        return Math.round(unscaled * scale);
    }

    private int calculateDynamicWidth() {
        return scaled(calculateUnscaledWidth());
    }

    private int calculateDynamicHeight() {
        return scaled(calculateUnscaledHeightUnscaled());
    }

    private void renderBlueprintData(GuiGraphicsExtractor context, int x, int y, int width, int height) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return;
        try {
            BPViewerUtility instance = BPViewerUtility.getInstance();
            java.util.List<String> labels = instance.layoutPreviewLabels();
            if (labels != null) {
                context.text(client.font, instance.layoutTitle(), x + 8, y + 20, instance.layoutTitleColor(), true);
                int blueprintY = y + 25;
                int maxItems = Math.min(3, Math.max(0, (height - 30) / 12));
                int count = 0;
                for (String displayText : labels) {
                    if (count >= maxItems) break;
                    int availableWidth = width - 16;
                    int textWidth = client.font.width(displayText);
                    if (textWidth > availableWidth && displayText.length() > 0) {
                        int maxChars = (int) ((double) availableWidth / textWidth * displayText.length());
                        displayText = maxChars > 3 ? displayText.substring(0, maxChars - 3) + "..." : "...";
                    }
                    context.text(client.font, displayText, x + 8, blueprintY, 0xFFFFFFFF, true);
                    blueprintY += 12;
                    count++;
                }
                if (labels.size() > maxItems) {
                    context.text(client.font, "... and " + (labels.size() - maxItems) + " more", x + 8, blueprintY, 0xFF888888, true);
                }
                return;
            }
        } catch (Exception e) {
            // Fall through to sample data
        }
        context.text(client.font, "COMMON", x + 8, y + 5, 0xFFFFFFFF, true);
        int blueprintY = y + 25;
        int maxItems = Math.min(3, (height - 30) / 12);
        String[] samples = {"Anfänger Hacke", "Anfänger Axt"};
        for (int i = 0; i < Math.min(maxItems, samples.length); i++) {
            String text = samples[i];
            int availableWidth = width - 16;
            int textWidth = client.font.width(text);
            if (textWidth > availableWidth) {
                int maxChars = (int) ((double) availableWidth / textWidth * text.length());
                text = maxChars > 3 ? text.substring(0, maxChars - 3) + "..." : "...";
            }
            context.text(client.font, text, x + 8, blueprintY, 0xFFFFFFFF, true);
            blueprintY += 12;
        }
        if (maxItems >= 3) {
            context.text(client.font, "... and more", x + 8, blueprintY, 0xFF888888, true);
        }
    }
}
