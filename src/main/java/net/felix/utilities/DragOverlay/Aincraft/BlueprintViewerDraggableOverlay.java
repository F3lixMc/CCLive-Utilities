package net.felix.utilities.DragOverlay.Aincraft;

import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.Aincraft.BPViewerUtility;
import net.felix.utilities.DragOverlay.DraggableOverlay;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class BlueprintViewerDraggableOverlay implements DraggableOverlay {

    private static final int DEFAULT_WIDTH = 200;
    private static final int DEFAULT_HEIGHT = 100;
    private static final Identifier BLUEPRINT_BACKGROUND_TEXTURE = Identifier.of("cclive-utilities", "textures/gui/blueprint_background.png");

    @Override
    public String getOverlayName() {
        return "Blueprint Viewer";
    }

    @Override
    public int getX() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getWindow() == null) return 0;
        int screenWidth = client.getWindow().getScaledWidth();
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
    public int getY() {
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
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getWindow() == null) return;
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
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
    public void renderInEditMode(DrawContext context, int mouseX, int mouseY, float delta) {
        int x = getX();
        int y = getY();
        int width = getWidth();
        int height = getHeight();
        net.felix.utilities.Town.OverlayType overlayType = CCLiveUtilitiesConfig.HANDLER.instance().blueprintViewerOverlayType;
        if (overlayType == net.felix.utilities.Town.OverlayType.CUSTOM) {
            try {
                context.drawTexture(RenderPipelines.GUI_TEXTURED, BLUEPRINT_BACKGROUND_TEXTURE,
                    x, y, 0.0f, 0.0f, width, height, width, height);
            } catch (Exception e) {
                context.fill(x, y, x + width, y + height, 0x80000000);
            }
        } else if (overlayType == net.felix.utilities.Town.OverlayType.BLACK) {
            context.fill(x, y, x + width, y + height, 0x80000000);
        }
        context.drawStrokedRectangle(x, y, width, height, 0xFFFF0000);
        context.drawText(MinecraftClient.getInstance().textRenderer, getOverlayName(), x + 5, y + 5, 0xFFFFFFFF, true);
        renderBlueprintData(context, x, y, width, height);
    }

    @Override
    public void savePosition() {
        CCLiveUtilitiesConfig.HANDLER.save();
    }

    @Override
    public boolean isEnabled() {
        return CCLiveUtilitiesConfig.HANDLER.instance().showBlueprintViewer;
    }

    @Override
    public Text getTooltip() {
        return Text.literal("Blueprint Viewer - Shows blueprint information and materials");
    }

    @Override
    public void resetToDefault() {
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

    private BPViewerUtility.BlueprintConfig.RarityData getRarityData() {
        try {
            BPViewerUtility instance = BPViewerUtility.getInstance();
            String activeFloor = instance.getActiveFloor();
            if (activeFloor == null) return null;
            BPViewerUtility.BlueprintConfig.FloorData floorData = instance.getConfig().getFloorData(activeFloor);
            if (floorData == null || floorData.blueprints == null) return null;
            return floorData.blueprints.get(instance.getCurrentRarity());
        } catch (Exception e) {
            return null;
        }
    }

    private int calculateUnscaledWidth() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return DEFAULT_WIDTH;
        BPViewerUtility.BlueprintConfig.RarityData rarityData = getRarityData();
        if (rarityData == null || rarityData.items == null) return DEFAULT_WIDTH;
        int maxWidth = DEFAULT_WIDTH;
        for (String blueprint : rarityData.items) {
            String displayText = blueprint.startsWith("- ") ? blueprint.substring(2) : blueprint;
            int textWidth = client.textRenderer.getWidth(displayText) + 45;
            if (textWidth > maxWidth) maxWidth = textWidth;
        }
        return maxWidth - 15;
    }

    private int calculateUnscaledHeightUnscaled() {
        BPViewerUtility.BlueprintConfig.RarityData rarityData = getRarityData();
        if (rarityData == null || rarityData.items == null) return DEFAULT_HEIGHT;
        return 20 + rarityData.items.size() * 12 + 5;
    }

    private int calculateDynamicWidth() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return DEFAULT_WIDTH;
        BPViewerUtility.BlueprintConfig.RarityData rarityData = getRarityData();
        if (rarityData == null || rarityData.items == null) return DEFAULT_WIDTH;
        int maxWidth = DEFAULT_WIDTH;
        for (String blueprint : rarityData.items) {
            String displayText = blueprint.startsWith("- ") ? blueprint.substring(2) : blueprint;
            int textWidth = client.textRenderer.getWidth(displayText) + 45;
            if (textWidth > maxWidth) maxWidth = textWidth;
        }
        int baseWidth = maxWidth - 15;
        float scale = CCLiveUtilitiesConfig.HANDLER.instance().blueprintViewerScale;
        if (scale <= 0) scale = 1.0f;
        return (int) (baseWidth * scale);
    }

    private int calculateDynamicHeight() {
        BPViewerUtility.BlueprintConfig.RarityData rarityData = getRarityData();
        if (rarityData == null || rarityData.items == null) return DEFAULT_HEIGHT;
        int totalHeight = 20 + rarityData.items.size() * 12 + 5;
        float scale = CCLiveUtilitiesConfig.HANDLER.instance().blueprintViewerScale;
        if (scale <= 0) scale = 1.0f;
        return (int) (totalHeight * scale);
    }

    private void renderBlueprintData(DrawContext context, int x, int y, int width, int height) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        try {
            BPViewerUtility instance = BPViewerUtility.getInstance();
            String activeFloor = instance.getActiveFloor();
            if (activeFloor != null) {
                BPViewerUtility.BlueprintConfig.FloorData floorData = instance.getConfig().getFloorData(activeFloor);
                if (floorData != null && floorData.blueprints != null) {
                    String currentRarity = instance.getCurrentRarity();
                    BPViewerUtility.BlueprintConfig.RarityData rarityData = floorData.blueprints.get(currentRarity);
                    if (rarityData != null && rarityData.items != null && !rarityData.items.isEmpty()) {
                        context.drawText(client.textRenderer, currentRarity.toUpperCase(), x + 8, y + 20, getRarityColor(currentRarity), true);
                        int blueprintY = y + 25;
                        int maxItems = Math.min(3, (height - 30) / 12);
                        int count = 0;
                        for (String blueprint : rarityData.items) {
                            if (count >= maxItems) break;
                            String displayText = blueprint.startsWith("- ") ? blueprint.substring(2) : blueprint;
                            int availableWidth = width - 16;
                            int textWidth = client.textRenderer.getWidth(displayText);
                            if (textWidth > availableWidth) {
                                int maxChars = (int) ((double) availableWidth / textWidth * displayText.length());
                                displayText = maxChars > 3 ? displayText.substring(0, maxChars - 3) + "..." : "...";
                            }
                            context.drawText(client.textRenderer, displayText, x + 8, blueprintY, 0xFFFFFFFF, true);
                            blueprintY += 12;
                            count++;
                        }
                        if (rarityData.items.size() > maxItems) {
                            context.drawText(client.textRenderer, "... and " + (rarityData.items.size() - maxItems) + " more", x + 8, blueprintY, 0xFF888888, true);
                        }
                        return;
                    }
                }
            }
        } catch (Exception e) {
            // Fall through to sample data
        }
        context.drawText(client.textRenderer, "COMMON", x + 8, y + 5, 0xFFFFFFFF, true);
        int blueprintY = y + 25;
        int maxItems = Math.min(3, (height - 30) / 12);
        String[] samples = {"Anfänger Hacke", "Anfänger Axt"};
        for (int i = 0; i < Math.min(maxItems, samples.length); i++) {
            String text = samples[i];
            int availableWidth = width - 16;
            int textWidth = client.textRenderer.getWidth(text);
            if (textWidth > availableWidth) {
                int maxChars = (int) ((double) availableWidth / textWidth * text.length());
                text = maxChars > 3 ? text.substring(0, maxChars - 3) + "..." : "...";
            }
            context.drawText(client.textRenderer, text, x + 8, blueprintY, 0xFFFFFFFF, true);
            blueprintY += 12;
        }
        if (maxItems >= 3) {
            context.drawText(client.textRenderer, "... and more", x + 8, blueprintY, 0xFF888888, true);
        }
    }

    private int getRarityColor(String rarity) {
        switch (rarity.toLowerCase()) {
            case "uncommon": return 0xFF00FF00;
            case "rare":     return 0xFF0000FF;
            case "epic":     return 0xFFFF00FF;
            case "legendary": return 0xFFFFFF00;
            default:         return 0xFFFFFFFF;
        }
    }
}
