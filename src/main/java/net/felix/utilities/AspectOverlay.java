package net.felix.utilities;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;

public class AspectOverlay {
    
    private static boolean shouldShow = false;
    private static boolean isCurrentlyHovering = false; // Track if currently hovering over a valid blueprint item
    private static String currentAspectName = "";
    private static String currentAspectDescription = "";
    private static String currentItemName = "";
    
    // Aspects database
    private static Map<String, AspectInfo> aspectsDatabase = new HashMap<>();
    private static final String ASPECTS_CONFIG_FILE = "assets/cclive-utilities/Aspekte.json";
    
    public static void initialize() {
        loadAspectsDatabase();
    }
    
    public static void updateAspectInfo(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) {
            shouldShow = false;
            isCurrentlyHovering = false;
            currentAspectName = "";
            currentAspectDescription = "";
            currentItemName = "";
            return;
        }
        
        // Get the item name
        String itemName = getItemName(itemStack);
        if (itemName == null || !itemName.contains("[Bauplan]")) {
            shouldShow = false;
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
        cleanItemName = cleanItemName.replaceAll("[^a-zA-ZäöüßÄÖÜ\\s]", "").trim();
        
        // Look for this item in the aspects database
        AspectInfo aspectInfo = aspectsDatabase.get(cleanItemName);
        if (aspectInfo != null && !aspectInfo.aspectName.isEmpty()) {
            currentAspectName = aspectInfo.aspectName;
            currentAspectDescription = aspectInfo.aspectDescription;
            currentItemName = cleanItemName;
            isCurrentlyHovering = true; // Set to true when hovering over a valid blueprint item
            // Don't set shouldShow to true here - it will be controlled by Shift key in render method
        } else {
            shouldShow = false;
            isCurrentlyHovering = false;
            currentAspectName = "";
            currentAspectDescription = "";
            currentItemName = "";
        }
    }
    
    /**
     * Hides the overlay
     */
    public static void hideOverlay() {
        shouldShow = false;
        isCurrentlyHovering = false;
        currentAspectName = "";
        currentAspectDescription = "";
        currentItemName = "";
    }
    
    /**
     * Called when hovering stops (no item being hovered)
     */
    public static void onHoverStopped() {
        isCurrentlyHovering = false;
    }
    
    /**
     * Updates the overlay with aspect information from a blueprint item name
     */
    public static void updateFromBlueprintName(String blueprintName) {
        if (blueprintName == null || !blueprintName.contains("[Bauplan]")) {
            shouldShow = false;
            isCurrentlyHovering = false;
            currentAspectName = "";
            currentAspectDescription = "";
            currentItemName = "";
            return;
        }
        
        // Extract the item name (everything before "[Bauplan]")
        String cleanItemName = blueprintName.substring(0, blueprintName.indexOf("[Bauplan]")).trim();
        
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
        cleanItemName = cleanItemName.replaceAll("[^a-zA-ZäöüßÄÖÜ\\s]", "").trim();
        
        // Look for this item in the aspects database
        AspectInfo aspectInfo = aspectsDatabase.get(cleanItemName);
        if (aspectInfo != null && !aspectInfo.aspectName.isEmpty()) {
            currentAspectName = aspectInfo.aspectName;
            currentAspectDescription = aspectInfo.aspectDescription;
            currentItemName = cleanItemName;
            isCurrentlyHovering = true; // Set to true when hovering over a valid blueprint item
            // Don't set shouldShow to true here - it will be controlled by Shift key in render method
        } else {
            shouldShow = false;
            isCurrentlyHovering = false;
            currentAspectName = "";
            currentAspectDescription = "";
            currentItemName = "";
        }
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
        
        // Check if shift is pressed
        boolean isShiftPressed = InputUtil.isKeyPressed(client.getWindow().getHandle(), 
                                                       InputUtil.GLFW_KEY_LEFT_SHIFT) || 
                                InputUtil.isKeyPressed(client.getWindow().getHandle(), 
                                                       InputUtil.GLFW_KEY_RIGHT_SHIFT);
        
        if (!isShiftPressed) {
            return;
        }
        
        // Position on the left side of the screen
        int screenWidth = client.getWindow().getScaledWidth();
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
            System.err.println("Error rendering text: " + e.getMessage());
            e.printStackTrace();
            
            // Fallback: Try simple string rendering with very high contrast colors
            try {
                context.drawText(client.textRenderer, "Aspekt Information", overlayX + 10, overlayY + 8, 0xFFFFFFFF, false);
                context.drawText(client.textRenderer, "Item: " + currentItemName, overlayX + 10, overlayY + 40, 0xFFFFFFFF, false);
                context.drawText(client.textRenderer, "Aspekt: " + currentAspectName, overlayX + 10, overlayY + 60, 0x00FF00FF, false);
            } catch (Exception e2) {
                System.err.println("Fallback text rendering also failed: " + e2.getMessage());
            }
        }
        
        // Alternative method: Try rendering with different approach
        try {
            // Render text with different color format
            context.drawText(client.textRenderer, "TEST TEXT VISIBLE", overlayX + 10, overlayY + 100, 0xFF0000, false);
        } catch (Exception e) {
            System.err.println("Alternative text rendering failed: " + e.getMessage());
        }
    }
    
    /**
     * Renders the overlay in the foreground - this method should be called from a Mixin
     * to ensure it renders over all GUI elements
     */
    public static void renderForeground(DrawContext context) {
        // Check if aspect overlay is enabled in config
        if (!net.felix.CCLiveUtilitiesConfig.HANDLER.instance().aspectOverlayEnabled || 
            !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showAspectOverlay) {
            return;
        }
        
        // Check if we have aspect information, are currently hovering, and shift is pressed
        if (currentAspectName.isEmpty() || currentItemName.isEmpty() || !isCurrentlyHovering) {
            return;
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.textRenderer == null) {
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
        
        // Additional safety check: ensure we're still hovering over a valid item
        if (!isCurrentlyHovering) {
            return;
        }
        
        // Get configurable position from config
        int configX = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().aspectOverlayX;
        int configY = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().aspectOverlayY;
        boolean showBackground = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().aspectOverlayShowBackground;
        
        // Position using absolute coordinates from screen edges
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        
        // Calculate overlay dimensions dynamically based on content
        int overlayWidth = 210; // Reduced from 250 to 210 pixels
        
        // Calculate required height based on description lines
        String[] descriptionLines = wrapText(currentAspectDescription, 30); // Reduced from 35 to 30 due to smaller width
        int descriptionHeight = descriptionLines.length * 12; // 12 pixels per line
        
        // Base height: 10 (top margin) + 20 (aspect name) + 15 (bottom margin) + description height
        int baseHeight = 45;
        int overlayHeight = baseHeight + descriptionHeight;
        
        // Ensure minimum height
        overlayHeight = Math.max(overlayHeight, 110);
        
        // Position overlay from right and top edges
        int overlayX = screenWidth - configX - overlayWidth; // X position: configX pixels from right edge of overlay to right edge of screen
        int overlayY = configY; // Y position from top edge
        
        // Special handling for right edge positioning
        if (configX == 0) {
            // When X = 0, position overlay exactly at right edge of screen
            overlayX = screenWidth - overlayWidth;
        } else if (configX > 0) {
            // When X > 0, ensure overlay is positioned correctly from right edge
            overlayX = screenWidth - configX - overlayWidth;
        }
        
        // Ensure overlay doesn't go off-screen (only check right and bottom edges)
        if (overlayX + overlayWidth > screenWidth) {
            overlayX = screenWidth - overlayWidth; // Ensure right edge doesn't go off screen
        }
        if (overlayY < 0) {
            overlayY = 0; // Allow overlay to go to very top
        }
        if (overlayY + overlayHeight > screenHeight) {
            overlayY = screenHeight - overlayHeight; // Ensure bottom edge doesn't go off screen
        }
        
        // Draw background only if enabled in config
        if (showBackground) {
            context.fill(overlayX, overlayY, overlayX + overlayWidth, overlayY + overlayHeight, 0xCC000000);
        }
        
        // Draw simple border
        context.fill(overlayX, overlayY, overlayX + 1, overlayY + overlayHeight, 0xFFFFFFFF); // Left border
        context.fill(overlayX + overlayWidth - 1, overlayY, overlayX + overlayWidth, overlayY + overlayHeight, 0xFFFFFFFF); // Right border
        context.fill(overlayX, overlayY, overlayX + overlayWidth, overlayY + 1, 0xFFFFFFFF); // Top border
        context.fill(overlayX, overlayY + overlayHeight - 1, overlayX + overlayWidth, overlayY + overlayHeight, 0xFFFFFFFF); // Bottom border
        
        // Draw aspect name on its own line with custom color #FCA800
        context.drawText(client.textRenderer, currentAspectName, overlayX + 10, overlayY + 10, 0xFFFCA800, false);
        
        // Draw aspect description (wrapped to fit) with colored text - numbers and special characters in light green
        for (int i = 0; i < descriptionLines.length; i++) {
            drawColoredText(context, client.textRenderer, descriptionLines[i], overlayX + 10, overlayY + 35 + (i * 12));
        }
        
        // Draw instruction text with simple gray text
        context.drawText(client.textRenderer, "Halte SHIFT gedrückt", overlayX + 10, overlayY + overlayHeight - 15, 0xCCCCCC, false);
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
                    
                    int loadedCount = 0;
                    for (String itemName : json.keySet()) {
                        try {
                            com.google.gson.JsonObject itemData = json.getAsJsonObject(itemName);
                            String aspectName = itemData.get("aspect_name").getAsString();
                            String aspectDescription = itemData.get("aspect_description").getAsString();
                            
                            // Only store items that have both aspect name and description
                            if (!aspectName.isEmpty() && !aspectDescription.isEmpty()) {
                                aspectsDatabase.put(itemName, new AspectInfo(aspectName, aspectDescription));
                                loadedCount++;
                            }
                        } catch (Exception e) {
                            System.err.println("Failed to parse aspect data for item: " + itemName + " - " + e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load aspects database for overlay: " + e.getMessage());
            e.printStackTrace();
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
