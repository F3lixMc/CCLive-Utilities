package net.felix.utilities.Overall.Aspekte;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.joml.Matrix3x2fStack;

import java.util.HashMap;
import java.util.Map;

public class AspectOverlay {
    
    private static boolean isCurrentlyHovering = false; // Track if currently hovering over a valid blueprint item
    private static String currentAspectName = "";
    private static String currentAspectDescription = "";
    private static String currentItemName = "";
    private static long lastTooltipUpdateTime = 0; // Track when the tooltip was last updated
    private static final long TOOLTIP_TIMEOUT_MS = 50; // Consider tooltip inactive after 50ms without update (very short delay to handle frame timing)
    
    // Separate state for chat overlay (different from star item overlay)
    private static boolean isChatHovering = false;
    
    // Tooltip bounds for collision detection
    private static int tooltipX = -1;
    private static int tooltipY = -1;
    private static int tooltipWidth = 0;
    private static int tooltipHeight = 0;
    private static boolean tooltipActive = false;
    
    // Aspects database
    private static Map<String, AspectInfo> aspectsDatabase = new HashMap<>();
    private static final String ASPECTS_CONFIG_FILE = "assets/cclive-utilities/Aspekte.json";
    
    public static void initialize() {
        loadAspectsDatabase();
    }
    
    public static void updateAspectInfo(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) {
            isCurrentlyHovering = false;
            currentAspectName = "";
            currentAspectDescription = "";
            currentItemName = "";
            return;
        }
        
        // Check if the item name contains Epic colors - if so, don't show overlay
        net.minecraft.text.Text itemNameText = itemStack.getName();
        if (itemNameText != null && net.felix.utilities.Overall.InformationenUtility.hasEpicColor(itemNameText)) {
            isCurrentlyHovering = false;
            currentAspectName = "";
            currentAspectDescription = "";
            currentItemName = "";
            return;
        }
        
        // Get the item name
        String itemName = getItemName(itemStack);
        if (itemName == null || !itemName.contains("[Bauplan]")) {
            isCurrentlyHovering = false;
            currentAspectName = "";
            currentAspectDescription = "";
            currentItemName = "";
            return;
        }
        
        // Extract the item name (everything before "[Bauplan]")
        String cleanItemName = itemName.substring(0, itemName.indexOf("[Bauplan]")).trim();
        
        // Remove leading dash/minus if present
        if (cleanItemName.startsWith("-")) {
            cleanItemName = cleanItemName.substring(1).trim();
        }
        
        // Remove trailing dash/minus if present
        if (cleanItemName.endsWith("-")) {
            cleanItemName = cleanItemName.substring(0, cleanItemName.length() - 1).trim();
        }
        
        // Remove Minecraft formatting codes and Unicode characters
        cleanItemName = cleanItemName.replaceAll("§[0-9a-fk-or]", "");
        cleanItemName = cleanItemName.replaceAll("[\\u3400-\\u4DBF]", "");
        cleanItemName = cleanItemName.replaceAll("[^a-zA-ZäöüßÄÖÜ\\s-]", "").trim();
        
        // Look for this item in the aspects database
        AspectInfo aspectInfo = aspectsDatabase.get(cleanItemName);
        if (aspectInfo != null) {
            currentAspectName = aspectInfo.aspectName;
            currentAspectDescription = aspectInfo.aspectDescription;
            currentItemName = cleanItemName;
            isCurrentlyHovering = true; // Set to true when hovering over a valid blueprint item
            lastTooltipUpdateTime = -1; // Use -1 to distinguish blueprint items from chat items (0) and star items (>0)
        } else {
            isCurrentlyHovering = false;
            currentAspectName = "";
            currentAspectDescription = "";
            currentItemName = "";
            lastTooltipUpdateTime = 0;
        }
    }
    
    /**
     * Hides the overlay
     */
    public static void hideOverlay() {
        isCurrentlyHovering = false;
        currentAspectName = "";
        currentAspectDescription = "";
        currentItemName = "";
    }
    
    /**
     * Updates aspect info from blueprint name
     * Can be called for chat messages OR star items (⭐)
     * For chat: sets isChatHovering = true, lastTooltipUpdateTime = 0
     * For star items: sets isCurrentlyHovering = true, lastTooltipUpdateTime = currentTime
     */
    public static void updateAspectInfoFromName(String cleanItemName, net.felix.utilities.Overall.InformationenUtility.AspectInfo aspectInfo) {
        if (cleanItemName == null || cleanItemName.isEmpty() || aspectInfo == null) {
            isChatHovering = false;
            isCurrentlyHovering = false;
            currentAspectName = "";
            currentAspectDescription = "";
            currentItemName = "";
            lastTooltipUpdateTime = 0;
            return;
        }
        
        currentAspectName = aspectInfo.aspectName;
        currentAspectDescription = aspectInfo.aspectDescription;
        currentItemName = cleanItemName;
        
        // Check if this is being called from chat (isChatHovering will be set separately)
        // or from star items (isCurrentlyHovering will be set)
        // We can't distinguish here, so we set both flags and let the caller determine which one
        // Actually, we need to check: if lastTooltipUpdateTime was 0 before, this might be a chat item
        // But we can't know for sure. Let's use a different approach:
        // Star items should set lastTooltipUpdateTime > 0, chat items should set it to 0
        
        // For now, assume this is a star item (most common case)
        // Chat items will explicitly set isChatHovering separately
        isCurrentlyHovering = true;
        lastTooltipUpdateTime = System.currentTimeMillis(); // Star items use timeout
    }
    
    /**
     * Updates aspect info from blueprint name for chat messages only
     */
    public static void updateAspectInfoFromNameForChat(String cleanItemName, net.felix.utilities.Overall.InformationenUtility.AspectInfo aspectInfo) {
        if (cleanItemName == null || cleanItemName.isEmpty() || aspectInfo == null) {
            isChatHovering = false;
            currentAspectName = "";
            currentAspectDescription = "";
            currentItemName = "";
            lastTooltipUpdateTime = 0;
            return;
        }
        
        currentAspectName = aspectInfo.aspectName;
        currentAspectDescription = aspectInfo.aspectDescription;
        currentItemName = cleanItemName;
        isChatHovering = true;
        lastTooltipUpdateTime = 0; // Chat items don't use timeout
    }
    
    /**
     * Called when hovering stops (no item being hovered)
     * For chat overlays: only clears chat data
     * For blueprint item overlays: clears blueprint item data
     * For star item overlays: clears star item data
     */
    public static void onHoverStopped() {
        // If lastTooltipUpdateTime == 0, this is a chat item
        // If lastTooltipUpdateTime == -1, this is a blueprint item
        // If lastTooltipUpdateTime > 0, this is a star item
        if (lastTooltipUpdateTime == 0) {
            // This is a chat item, clear chat data
            isChatHovering = false;
            currentAspectName = "";
            currentAspectDescription = "";
            currentItemName = "";
        } else if (lastTooltipUpdateTime == -1) {
            // This is a blueprint item, clear blueprint item data
            isCurrentlyHovering = false;
            currentAspectName = "";
            currentAspectDescription = "";
            currentItemName = "";
            lastTooltipUpdateTime = 0;
        } else {
            // This is a star item, clear star item data
            isCurrentlyHovering = false;
            currentAspectName = "";
            currentAspectDescription = "";
            currentItemName = "";
            lastTooltipUpdateTime = 0;
        }
    }
    
    /**
     * Checks if we're still actively hovering (tooltip was updated recently)
     * For blueprint items (lastTooltipUpdateTime == -1), we just check isCurrentlyHovering
     * For chat items (lastTooltipUpdateTime == 0), we check isChatHovering
     * For "⭐" items (lastTooltipUpdateTime > 0), we check if the tooltip was updated recently
     */
    private static boolean isActivelyHovering() {
        // For chat items (lastTooltipUpdateTime == 0), check isChatHovering
        if (lastTooltipUpdateTime == 0) {
            return isChatHovering;
        }
        
        // For blueprint items (lastTooltipUpdateTime == -1), check isCurrentlyHovering
        if (lastTooltipUpdateTime == -1) {
            return isCurrentlyHovering;
        }
        
        // For "⭐" items (lastTooltipUpdateTime > 0), check if tooltip was updated recently
        if (!isCurrentlyHovering) {
            return false;
        }
        
        long currentTime = System.currentTimeMillis();
        long timeSinceLastUpdate = currentTime - lastTooltipUpdateTime;
        
        if (timeSinceLastUpdate > TOOLTIP_TIMEOUT_MS) {
            // Tooltip hasn't been updated recently - we're no longer hovering
            isCurrentlyHovering = false;
            currentAspectName = "";
            currentAspectDescription = "";
            currentItemName = "";
            lastTooltipUpdateTime = 0;
            return false;
        }
        
        return true;
    }
    
    /**
     * Returns whether we're currently hovering over a valid blueprint item
     */
    public static boolean isCurrentlyHovering() {
        return isActivelyHovering();
    }
    
    /**
     * Returns whether we're currently hovering over a star item (⭐)
     * Star items have lastTooltipUpdateTime > 0
     */
    public static boolean isStarItemHovering() {
        return lastTooltipUpdateTime > 0 && isCurrentlyHovering;
    }
    
    /**
     * Checks if we're currently in a blueprint inventory
     */
    private static boolean isInBlueprintInventory(MinecraftClient client) {
        if (client == null || client.currentScreen == null) {
            return false;
        }
        
        // Check if the current screen is a HandledScreen (inventory-like screen)
        if (!(client.currentScreen instanceof net.minecraft.client.gui.screen.ingame.HandledScreen)) {
            return false;
        }
        
        // Get the screen title to check if it's a blueprint inventory
        net.minecraft.client.gui.screen.ingame.HandledScreen<?> handledScreen = 
            (net.minecraft.client.gui.screen.ingame.HandledScreen<?>) client.currentScreen;
        
        String title = handledScreen.getTitle().getString();
        
        // Remove Minecraft formatting codes and Unicode characters for comparison
        String cleanTitle = title.replaceAll("§[0-9a-fk-or]", "")
                               .replaceAll("[\\u3400-\\u4DBF]", "");
        
        // Check if the clean title contains any of the blueprint inventory names
        return cleanTitle.contains("Baupläne [Waffen]") ||
               cleanTitle.contains("Baupläne [Rüstung]") ||
               cleanTitle.contains("Baupläne [Werkzeuge]") ||
               cleanTitle.contains("Bauplan [Shop]") ||
               cleanTitle.contains("Favorisierte [Rüstungsbaupläne]") ||
               cleanTitle.contains("Favorisierte [Waffenbaupläne]") ||
               cleanTitle.contains("Favorisierte [Werkzeugbaupläne]") ||
               cleanTitle.contains("CACTUS_CLICKER.blueprints.favorites.title.tools");
    }
    
    /**
     * Gets the current aspect name
     */
    public static String getCurrentAspectName() {
        return currentAspectName;
    }
    
    /**
     * Sets the tooltip bounds for collision detection
     */
    public static void setTooltipBounds(int x, int y, int width, int height) {
        tooltipX = x;
        tooltipY = y;
        tooltipWidth = width;
        tooltipHeight = height;
        tooltipActive = true;
    }
    
    /**
     * Clears the tooltip bounds
     */
    public static void clearTooltipBounds() {
        tooltipActive = false;
        tooltipX = -1;
        tooltipY = -1;
        tooltipWidth = 0;
        tooltipHeight = 0;
    }
    
    /**
     * Checks if two rectangles overlap
     */
    private static boolean rectanglesOverlap(int x1, int y1, int w1, int h1, int x2, int y2, int w2, int h2) {
        return x1 < x2 + w2 && x1 + w1 > x2 && y1 < y2 + h2 && y1 + h1 > y2;
    }
    
    
    private static String getItemName(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) {
            return null;
        }
        
        // Get the display name
        Text displayName = itemStack.getName();
        if (displayName != null) {
            return displayName.getString();
        }
        
        return null;
    }
    
    public static void render(DrawContext context) {
        // Check if we have aspect information and shift is pressed
        if (currentAspectName.isEmpty() || currentItemName.isEmpty()) {
            return;
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.textRenderer == null) {
            return;
        }
        
        // Hide overlay if F1 menu (debug screen) is open
        if (client.options.hudHidden) {
            return;
        }
        
        // Check if shift is pressed
        boolean isShiftPressed = InputUtil.isKeyPressed(client.getWindow().getHandle(), 
                                                       InputUtil.GLFW_KEY_LEFT_SHIFT) || 
                                InputUtil.isKeyPressed(client.getWindow().getHandle(), 
                                                       InputUtil.GLFW_KEY_RIGHT_SHIFT);
        
        if (!isShiftPressed) {
            return;
        }
        
        // Position on the left side of the screen
        int screenHeight = client.getWindow().getScaledHeight();
        
        // Calculate overlay dimensions and position
        int overlayWidth = 300;
        int overlayHeight = 150;
        int overlayX = 15;
        int overlayY = (screenHeight - overlayHeight) / 2;
        
        // Draw background with better transparency
        context.fill(overlayX, overlayY, overlayX + overlayWidth, overlayY + overlayHeight, 0xCC000000);
        
        // Draw border with better visibility
        context.fill(overlayX, overlayY, overlayX + 2, overlayY + overlayHeight, 0xFFFFFF00); // Left border
        context.fill(overlayX + overlayWidth - 2, overlayY, overlayX + overlayWidth, overlayY + overlayHeight, 0xFFFFFF00); // Right border
        context.fill(overlayX, overlayY, overlayX + overlayWidth, overlayY + 2, 0xFFFFFF00); // Top border
        context.fill(overlayX, overlayY + overlayHeight - 2, overlayX + overlayWidth, overlayY + overlayHeight, 0xFFFFFF00); // Bottom border
        
        // Draw header background with better color
        context.fill(overlayX, overlayY, overlayX + overlayWidth, overlayY + 30, 0xCCFFD700);
        
        // Try different text rendering methods with very high contrast colors
        try {
            // Method 1: Using drawText with Text objects and very high contrast colors
            Text titleText = Text.literal("Aspekt Information");
            Text itemText = Text.literal("Item: " + currentItemName);
            Text aspectText = Text.literal("Aspekt: " + currentAspectName);
            Text instructionText = Text.literal("Halte SHIFT gedrückt");
            
            // Use very high contrast colors - pure white on black background
            context.drawText(client.textRenderer, titleText, overlayX + 10, overlayY + 8, 0xFFFFFFFF, false);
            
            context.drawText(client.textRenderer, itemText, overlayX + 10, overlayY + 40, 0xFFFFFFFF, false);
            
            context.drawText(client.textRenderer, aspectText, overlayX + 10, overlayY + 60, 0x00FF00FF, false);
            
            // Draw aspect description (wrapped to fit) with very high contrast
            String[] descriptionLines = wrapText(currentAspectDescription, 40);
            for (int i = 0; i < descriptionLines.length; i++) {
                Text descText = Text.literal(descriptionLines[i]);
                context.drawText(client.textRenderer, descText, overlayX + 10, overlayY + 85 + (i * 14), 0xFFFFFFFF, false);
            }
            
            context.drawText(client.textRenderer, instructionText, overlayX + 10, overlayY + overlayHeight - 20, 0xFFFFFFFF, false);
            
        } catch (Exception e) {
            // Ignore rendering errors
        }
    }
    
    /**
     * Renders the overlay in the foreground for chat messages (top-left, only when Shift is pressed)
     * This method should be called from ChatHudHoverMixin when hovering over chat messages with Shift pressed
     * @param context The DrawContext for rendering
     */
    public static void renderForegroundForChat(DrawContext context) {
        // Check if chat aspect overlay is enabled in config
        boolean aspectOverlayEnabled = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().aspectOverlayEnabled;
        boolean showAspectOverlay = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showAspectOverlay;
        boolean chatAspectOverlayEnabled = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().chatAspectOverlayEnabled;
        
        if (!aspectOverlayEnabled || !showAspectOverlay || !chatAspectOverlayEnabled) {
            return;
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.textRenderer == null) {
            return;
        }
        
        // Check if Shift is pressed
        boolean isShiftPressed = InputUtil.isKeyPressed(client.getWindow().getHandle(), 
                                                       InputUtil.GLFW_KEY_LEFT_SHIFT) || 
                                InputUtil.isKeyPressed(client.getWindow().getHandle(), 
                                                       InputUtil.GLFW_KEY_RIGHT_SHIFT);
        
        if (!isShiftPressed) {
            // Shift not pressed, don't render overlay
            return;
        }
        
        // Check if we have aspect information and are currently hovering (chat hover)
        if (currentAspectName.isEmpty() || currentItemName.isEmpty() || !isChatHovering) {
            return;
        }
        
        // Check if we're in a world (important for multiplayer servers)
        if (client.world == null) {
            return;
        }
        
        // Hide overlay if F1 menu (debug screen) is open
        if (client.options.hudHidden) {
            return;
        }
        
        // Get configurable position and scale from config
        boolean showBackground = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().aspectOverlayShowBackground;
        float overlayScale = 1.0f; // Default scale
        
        // Ensure scale is valid
        if (overlayScale <= 0) overlayScale = 1.0f;
        
        // Position using absolute coordinates from screen edges
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        
        // Calculate base overlay dimensions dynamically based on content
        int baseOverlayWidth = 210; // Base width in pixels
        
        // Calculate required height based on description lines
        String[] descriptionLines = wrapText(currentAspectDescription, 30);
        int descriptionHeight = descriptionLines.length * 12; // 12 pixels per line
        
        // Base height: 10 (top margin) + 20 (aspect name) + 15 (bottom margin) + description height
        int baseHeight = 45;
        int baseOverlayHeight = baseHeight + descriptionHeight;
        
        // Ensure minimum height
        baseOverlayHeight = Math.max(baseOverlayHeight, 110);
        
        // Use base dimensions for positioning (scale will be applied via matrix)
        int overlayWidth = baseOverlayWidth;
        int overlayHeight = baseOverlayHeight;
        
        // Position overlay using fixed position from config (same as drag overlay)
        // Use config X and Y values directly
        int overlayX = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().chatAspectOverlayX;
        int overlayY = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().chatAspectOverlayY;
        
        // Ensure overlay doesn't go off-screen horizontally
        if (overlayX < 0) {
            overlayX = 0;
        }
        if (overlayX + overlayWidth > screenWidth) {
            overlayX = screenWidth - overlayWidth;
        }
        // Ensure overlay doesn't go off-screen vertically
        if (overlayY + overlayHeight > screenHeight) {
            overlayY = screenHeight - overlayHeight;
        }
        if (overlayY < 0) {
            overlayY = 0;
        }
        
        // Calculate scaled dimensions and offsets for centered scaling
        int scaledWidth = (int) (overlayWidth * overlayScale);
        int scaledHeight = (int) (overlayHeight * overlayScale);
        int offsetX = (scaledWidth - overlayWidth) / 2;
        int offsetY = (scaledHeight - overlayHeight) / 2;
        
        // Draw background only if enabled in config (scaled)
        if (showBackground) {
            int bgX1 = overlayX - offsetX;
            int bgY1 = overlayY - offsetY;
            int bgX2 = overlayX - offsetX + scaledWidth;
            int bgY2 = overlayY - offsetY + scaledHeight;
            context.fill(bgX1, bgY1, bgX2, bgY2, 0xCC000000);
        }
        
        // Draw simple border (scaled)
        int borderX1 = overlayX - offsetX;
        int borderY1 = overlayY - offsetY;
        int borderX2 = overlayX - offsetX + scaledWidth;
        int borderY2 = overlayY - offsetY + scaledHeight;
        
        context.fill(borderX1, borderY1, borderX1 + 1, borderY2, 0xFFFFFFFF); // Left border
        context.fill(borderX2 - 1, borderY1, borderX2, borderY2, 0xFFFFFFFF); // Right border
        context.fill(borderX1, borderY1, borderX2, borderY1 + 1, 0xFFFFFFFF); // Top border
        context.fill(borderX1, borderY2 - 1, borderX2, borderY2, 0xFFFFFFFF); // Bottom border
        
        // Apply scaling for text rendering
        Matrix3x2fStack matrices = context.getMatrices();
        matrices.pushMatrix();
        
        // Translate to scaled overlay position and apply scaling
        int textX = overlayX - offsetX;
        int textY = overlayY - offsetY;
        
        matrices.translate(textX, textY);
        matrices.scale(overlayScale, overlayScale);
        
        // Draw aspect name on its own line with custom color #FCA800
        context.drawText(client.textRenderer, currentAspectName, 10, 10, 0xFFFCA800, false);
        
        // Draw aspect description (wrapped to fit) with colored text - numbers and special characters in light green
        for (int i = 0; i < descriptionLines.length; i++) {
            drawColoredText(context, client.textRenderer, descriptionLines[i], 10, 35 + (i * 12));
        }
        
        // Restore matrices
        matrices.popMatrix();
    }
    
    /**
     * Renders the overlay in the foreground - this method should be called from a Mixin
     * to ensure it renders over all GUI elements
     * This version requires Shift to be pressed (for inventory items)
     */
    public static void renderForeground(DrawContext context) {
        // Check if aspect overlay is enabled in config
        if (!net.felix.CCLiveUtilitiesConfig.HANDLER.instance().aspectOverlayEnabled || 
            !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showAspectOverlay) {
            return;
        }
        
        // Check if we're actively hovering (tooltip was updated recently)
        if (!isActivelyHovering()) {
            return;
        }
        
        // Check if we have aspect information
        if (currentAspectName.isEmpty() || currentItemName.isEmpty()) {
            return;
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.textRenderer == null) {
            return;
        }
        
        // Check if we're in a world (important for multiplayer servers)
        if (client.world == null) {
            return;
        }
        
        // Hide overlay if F1 menu (debug screen) is open
        if (client.options.hudHidden) {
            return;
        }
        
        // Check if shift is pressed (redundant check, but kept for safety)
        long windowHandle = client.getWindow().getHandle();
        boolean leftShift = InputUtil.isKeyPressed(windowHandle, InputUtil.GLFW_KEY_LEFT_SHIFT);
        boolean rightShift = InputUtil.isKeyPressed(windowHandle, InputUtil.GLFW_KEY_RIGHT_SHIFT);
        boolean isShiftPressed = leftShift || rightShift;
        
        if (!isShiftPressed) {
            return;
        }
        
        // Additional safety check: ensure we're still hovering over a valid item
        if (!isCurrentlyHovering) {
            return;
        }
        
        // Check if we're in a blueprint inventory AND this is a "⭐" item (not a blueprint item)
        // If so, don't render this overlay (the blueprint Aspect Overlay is handled separately)
        // For blueprint items (lastTooltipUpdateTime == 0), we should render in blueprint inventories
        // For "⭐" items (lastTooltipUpdateTime > 0), we should NOT render in blueprint inventories
        if (isInBlueprintInventory(client) && lastTooltipUpdateTime > 0) {
            return;
        }
        
        // Get configurable position and scale from config
        // Use different positions for blueprint items vs "⭐" items
        boolean isStarItem = (lastTooltipUpdateTime > 0);
        int configX, configY;
        if (isStarItem) {
            // For "⭐" items, use starAspectOverlayX/Y (from left/top)
            configX = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().starAspectOverlayX;
            configY = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().starAspectOverlayY;
        } else {
            // For blueprint items, use aspectOverlayX/Y (from right/top)
            configX = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().aspectOverlayX;
            configY = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().aspectOverlayY;
        }
        boolean showBackground = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().aspectOverlayShowBackground;
        float overlayScale = 1.0f; // Default scale - add aspectOverlayScale to config later

        
        // Ensure scale is valid
        if (overlayScale <= 0) overlayScale = 1.0f;
        
        // Position using absolute coordinates from screen edges
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        
        // Calculate base overlay dimensions dynamically based on content
        int baseOverlayWidth = 210; // Base width in pixels
        
        // Calculate required height based on description lines
        String[] descriptionLines = wrapText(currentAspectDescription, 30); // Reduced from 35 to 30 due to smaller width
        int descriptionHeight = descriptionLines.length * 12; // 12 pixels per line
        
        // Base height: 10 (top margin) + 20 (aspect name) + 15 (bottom margin) + description height
        int baseHeight = 45;
        int baseOverlayHeight = baseHeight + descriptionHeight;
        
        // Ensure minimum height
        baseOverlayHeight = Math.max(baseOverlayHeight, 110);
        
        // Use base dimensions for positioning (scale will be applied via matrix)
        int overlayWidth = baseOverlayWidth;
        int overlayHeight = baseOverlayHeight;
        
        // Position overlay using config values
        int overlayX, overlayY;
        if (isStarItem) {
            // For "⭐" items: X is absolute position from left, Y is absolute position from top
            overlayX = configX;
            overlayY = configY;
        } else {
            // For blueprint items: X is from right edge, Y is from top
            overlayX = screenWidth - configX - overlayWidth;
            overlayY = configY;
        }
        
        // Check for collision with tooltip and adjust position if needed
        if (tooltipActive && tooltipWidth > 0 && tooltipHeight > 0) {
            // Calculate actual overlay bounds (with scaling)
            int scaledWidth = (int) (overlayWidth * overlayScale);
            int scaledHeight = (int) (overlayHeight * overlayScale);
            int offsetX = (scaledWidth - overlayWidth) / 2;
            int offsetY = (scaledHeight - overlayHeight) / 2;
            
            int overlayActualX = overlayX - offsetX;
            int overlayActualY = overlayY - offsetY;
            int overlayActualWidth = scaledWidth;
            int overlayActualHeight = scaledHeight;
            
            // Check if tooltip overlaps with overlay
            if (rectanglesOverlap(overlayActualX, overlayActualY, overlayActualWidth, overlayActualHeight,
                                 tooltipX, tooltipY, tooltipWidth, tooltipHeight)) {
                // Move overlay to the right side if it overlaps with tooltip
                overlayX = screenWidth - overlayWidth - 10; // 10px margin from right edge
            }
        }
        
        // Ensure overlay doesn't go off-screen
        if (overlayX < 0) {
            overlayX = 0;
        }
        if (overlayX + overlayWidth > screenWidth) {
            overlayX = screenWidth - overlayWidth;
        }
        if (overlayY < 0) {
            overlayY = 0;
        }
        if (overlayY + overlayHeight > screenHeight) {
            overlayY = screenHeight - overlayHeight;
        }
        
        // Calculate scaled dimensions and offsets for centered scaling
        int scaledWidth = (int) (overlayWidth * overlayScale);
        int scaledHeight = (int) (overlayHeight * overlayScale);
        int offsetX = (scaledWidth - overlayWidth) / 2;
        int offsetY = (scaledHeight - overlayHeight) / 2;
        
        // Draw background only if enabled in config (scaled)
        if (showBackground) {
            context.fill(overlayX - offsetX, overlayY - offsetY, overlayX - offsetX + scaledWidth, overlayY - offsetY + scaledHeight, 0xCC000000);
        }
        
        // Draw simple border (scaled)
        context.fill(overlayX - offsetX, overlayY - offsetY, overlayX - offsetX + 1, overlayY - offsetY + scaledHeight, 0xFFFFFFFF); // Left border
        context.fill(overlayX - offsetX + scaledWidth - 1, overlayY - offsetY, overlayX - offsetX + scaledWidth, overlayY - offsetY + scaledHeight, 0xFFFFFFFF); // Right border
        context.fill(overlayX - offsetX, overlayY - offsetY, overlayX - offsetX + scaledWidth, overlayY - offsetY + 1, 0xFFFFFFFF); // Top border
        context.fill(overlayX - offsetX, overlayY - offsetY + scaledHeight - 1, overlayX - offsetX + scaledWidth, overlayY - offsetY + scaledHeight, 0xFFFFFFFF); // Bottom border
        
        // Apply scaling for text rendering
        Matrix3x2fStack matrices = context.getMatrices();
        matrices.pushMatrix();
        
        // Translate to scaled overlay position and apply scaling
        matrices.translate(overlayX - offsetX, overlayY - offsetY);
        matrices.scale(overlayScale, overlayScale);
        
        // Draw aspect name on its own line with custom color #FCA800
        context.drawText(client.textRenderer, currentAspectName, 10, 10, 0xFFFCA800, false);
        
        // Draw aspect description (wrapped to fit) with colored text - numbers and special characters in light green
        for (int i = 0; i < descriptionLines.length; i++) {
            drawColoredText(context, client.textRenderer, descriptionLines[i], 10, 35 + (i * 12));
        }
        
        // Draw instruction text with simple gray text
        context.drawText(client.textRenderer, "Halte SHIFT gedrückt", 10, overlayHeight - 15, 0xCCCCCC, false);
        
        // Restore matrices
        matrices.popMatrix();
    }
    
    private static String[] wrapText(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return new String[]{text};
        }
        
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        java.util.List<String> lines = new java.util.ArrayList<>();
        
        for (String word : words) {
            // If adding this word would exceed the line length
            if (currentLine.length() + word.length() + 1 > maxLength) {
                // If current line has content, add it to lines
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString().trim());
                    currentLine = new StringBuilder(word);
                } else {
                    // If current line is empty but word is too long, split the word
                    if (word.length() > maxLength) {
                        // Split long words
                        int remainingLength = maxLength;
                        while (remainingLength < word.length()) {
                            lines.add(word.substring(remainingLength - maxLength, remainingLength));
                            remainingLength += maxLength;
                        }
                        if (remainingLength - maxLength < word.length()) {
                            currentLine = new StringBuilder(word.substring(remainingLength - maxLength));
                        }
                    } else {
                        lines.add(word);
                    }
                }
            } else {
                // Add word to current line
                if (currentLine.length() > 0) {
                    currentLine.append(" ");
                }
                currentLine.append(word);
            }
        }
        
        // Add the last line if it has content
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString().trim());
        }
        
        return lines.toArray(new String[0]);
    }
    
    /**
     * Loads the aspects database from Aspekte.json
     */
    private static void loadAspectsDatabase() {
        try {
            // Load from mod resources
            var resource = net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer("cclive-utilities")
                .orElseThrow(() -> new RuntimeException("Mod container not found"))
                .findPath(ASPECTS_CONFIG_FILE)
                .orElseThrow(() -> new RuntimeException("Aspects config file not found"));
            
            try (var inputStream = java.nio.file.Files.newInputStream(resource)) {
                try (var reader = new java.io.InputStreamReader(inputStream, java.nio.charset.StandardCharsets.UTF_8)) {
                    com.google.gson.JsonObject json = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
                    
                    for (String itemName : json.keySet()) {
                        try {
                            com.google.gson.JsonObject itemData = json.getAsJsonObject(itemName);
                            String aspectName = itemData.get("aspect_name").getAsString();
                            String aspectDescription = itemData.get("aspect_description").getAsString();
                            
                            // Only store items that have both aspect name and description
                            if (!aspectName.isEmpty() && !aspectDescription.isEmpty()) {
                                aspectsDatabase.put(itemName, new AspectInfo(aspectName, aspectDescription));
                            }
                        } catch (Exception e) {
                            // Ignore parse errors
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignore load errors
        }
    }
    
    /**
     * Draws text with colored text and white numbers/special characters/specific words
     */
    private static void drawColoredText(DrawContext context, net.minecraft.client.font.TextRenderer textRenderer, String text, int x, int y) {
        int currentX = x;
        int mainColor = 0xFF54FC54; // Main text color #54FC54
        int whiteColor = 0xFFFFFFFF; // White color for numbers, special characters, and specific words
        
        // Words that should be white
        String[] whiteWords = {"Block", "Blöcke", "Blöcken", "Sekunde", "Sekunden"};
        
        // Process text word by word
        String[] words = text.split(" ");
        
        for (String word : words) {
            // Check if this word should be white
            boolean isWhiteWord = false;
            for (String whiteWord : whiteWords) {
                if (word.equals(whiteWord)) {
                    isWhiteWord = true;
                    break;
                }
            }
            
            if (isWhiteWord) {
                // Render white word
                context.drawText(textRenderer, word, currentX, y, whiteColor, false);
                currentX += textRenderer.getWidth(word + " ");
            } else {
                // Render word character by character to handle numbers and special characters
                for (int i = 0; i < word.length(); i++) {
                    char c = word.charAt(i);
                    String charStr = String.valueOf(c);
                    
                    // Check if character is a number or special character
                    boolean isSpecial = Character.isDigit(c) || c == '%' || c == '+' || c == '-' || c == '.' || c == ',' || c == ':' || c == '[' || c == ']';
                    
                    int charColor = isSpecial ? whiteColor : mainColor;
                    context.drawText(textRenderer, charStr, currentX, y, charColor, false);
                    currentX += textRenderer.getWidth(charStr);
                }
                // Add space after word
                currentX += textRenderer.getWidth(" ");
            }
        }
    }
    
    /**
     * Data class to store aspect information
     */
    private static class AspectInfo {
        public final String aspectName;
        public final String aspectDescription;
        
        public AspectInfo(String aspectName, String aspectDescription) {
            this.aspectName = aspectName;
            this.aspectDescription = aspectDescription;
        }
    }
}
