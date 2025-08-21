package net.felix.utilities;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class AspectInfoGUI extends Screen {
    
    private static AspectInfoGUI instance;
    private static boolean shouldShow = false;
    private static String currentAspectName = "";
    private static String currentAspectDescription = "";
    private static int x, y;
    
    // Aspects database
    private static Map<String, AspectInfo> aspectsDatabase = new HashMap<>();
    private static final String ASPECTS_CONFIG_FILE = "assets/cclive-utilities/Aspekte.json";
    
    public AspectInfoGUI() {
        super(Text.literal("Aspect Info"));
    }
    
    public static void initialize() {
        if (instance == null) {
            instance = new AspectInfoGUI();
        }
        loadAspectsDatabase();
    }
    
    public static AspectInfoGUI getInstance() {
        return instance;
    }
    
    public static void showAspectInfo(int mouseX, int mouseY) {
        if (instance == null) {
            initialize();
        }
        
        // Check if shift is pressed
        MinecraftClient client = MinecraftClient.getInstance();
        boolean isShiftPressed = InputUtil.isKeyPressed(client.getWindow().getHandle(), 
                                                       InputUtil.GLFW_KEY_LEFT_SHIFT) || 
                                InputUtil.isKeyPressed(client.getWindow().getHandle(), 
                                                       InputUtil.GLFW_KEY_RIGHT_SHIFT);
        
        if (!isShiftPressed) {
            shouldShow = false;
            return;
        }
        
        // Get the item under the mouse cursor
        ItemStack hoveredItem = getHoveredItem(client, mouseX, mouseY);
        if (hoveredItem == null || hoveredItem.isEmpty()) {
            shouldShow = false;
            return;
        }
        
        // Check if this is a blueprint item
        String itemName = getItemName(hoveredItem);
        if (itemName == null || !itemName.contains("[Bauplan]")) {
            shouldShow = false;
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
            shouldShow = true;
            x = mouseX + 10;
            y = mouseY - 10;
        } else {
            shouldShow = false;
        }
    }
    
    private static ItemStack getHoveredItem(MinecraftClient client, int mouseX, int mouseY) {
        // This is a simplified approach - in a real implementation you'd need to
        // check the actual GUI elements and their item stacks
        // For now, we'll return null to indicate no item found
        return null;
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
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!shouldShow) {
            return;
        }
        
        // Set position relative to mouse
        int guiX = x;
        int guiY = y;
        
        // Ensure GUI doesn't go off screen
        int screenWidth = MinecraftClient.getInstance().getWindow().getScaledWidth();
        int screenHeight = MinecraftClient.getInstance().getWindow().getScaledHeight();
        
        if (guiX + 200 > screenWidth) {
            guiX = mouseX - 210;
        }
        if (guiY + 80 > screenHeight) {
            guiY = mouseY - 90;
        }
        
        // Draw background
        context.fill(guiX, guiY, guiX + 200, guiY + 80, 0x88000000);
        context.fill(guiX, guiY, guiX + 200, guiY + 20, 0x88FFD700); // Gold header
        
        // Draw border
        context.fill(guiX, guiY, guiX + 1, guiY + 80, 0xFFFFFFFF);
        context.fill(guiX + 199, guiY, guiX + 200, guiY + 80, 0xFFFFFFFF);
        context.fill(guiX, guiY, guiX + 200, guiY + 1, 0xFFFFFFFF);
        context.fill(guiX, guiY + 79, guiX + 200, guiY + 80, 0xFFFFFFFF);
        
        // Draw title
        context.drawTextWithShadow(textRenderer, "Aspekt Information", guiX + 5, guiY + 5, 0x000000);
        
        // Draw aspect name
        context.drawTextWithShadow(textRenderer, "Aspekt: " + currentAspectName, guiX + 5, guiY + 25, 0x00FF00);
        
        // Draw aspect description (wrapped to fit)
        String[] descriptionLines = wrapText(currentAspectDescription, 30);
        for (int i = 0; i < descriptionLines.length; i++) {
            context.drawTextWithShadow(textRenderer, descriptionLines[i], guiX + 5, guiY + 45 + (i * 12), 0xFFFFFF);
        }
    }
    
    /**
     * Renders the aspect GUI as an overlay
     */
    public void renderOverlay() {
        if (!shouldShow) {
            return;
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.textRenderer == null) {
            return;
        }
        
        // Get current mouse position
        double mouseX = client.mouse.getX() * client.getWindow().getScaledWidth() / client.getWindow().getWidth();
        double mouseY = client.mouse.getY() * client.getWindow().getScaledHeight() / client.getWindow().getHeight();
        
        // Set position relative to mouse
        int guiX = x;
        int guiY = y;
        
        // Ensure GUI doesn't go off screen
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        
        if (guiX + 200 > screenWidth) {
            guiX = (int) mouseX - 210;
        }
        if (guiY + 80 > screenHeight) {
            guiY = (int) mouseY - 90;
        }
        
        // Create a temporary DrawContext for rendering
        // Note: This is a simplified approach - in a real implementation you'd need to
        // properly integrate with the rendering pipeline
        System.out.println("DEBUG: Rendering aspect GUI at position: " + guiX + ", " + guiY);
        System.out.println("DEBUG: Aspect: " + currentAspectName);
        System.out.println("DEBUG: Description: " + currentAspectDescription);
    }
    
    private String[] wrapText(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return new String[]{text};
        }
        
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        java.util.List<String> lines = new java.util.ArrayList<>();
        
        for (String word : words) {
            if (currentLine.length() + word.length() + 1 <= maxLength) {
                if (currentLine.length() > 0) {
                    currentLine.append(" ");
                }
                currentLine.append(word);
            } else {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    lines.add(word);
                }
            }
        }
        
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        
        return lines.toArray(new String[0]);
    }
    
    @Override
    public boolean shouldPause() {
        return false;
    }
    
    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
    
    /**
     * Loads the aspects database from Aspekte.json
     */
    private static void loadAspectsDatabase() {
        try {
            System.out.println("DEBUG: Loading aspects database for GUI...");
            
            // Load from mod resources
            var resource = net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer("cclive-utilities")
                .orElseThrow(() -> new RuntimeException("Mod container not found"))
                .findPath(ASPECTS_CONFIG_FILE)
                .orElseThrow(() -> new RuntimeException("Aspects config file not found"));
            
            try (var inputStream = java.nio.file.Files.newInputStream(resource)) {
                try (var reader = new java.io.InputStreamReader(inputStream)) {
                    com.google.gson.JsonObject json = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
                    
                    for (String itemName : json.keySet()) {
                        com.google.gson.JsonObject itemData = json.getAsJsonObject(itemName);
                        String aspectName = itemData.get("aspect_name").getAsString();
                        String aspectDescription = itemData.get("aspect_description").getAsString();
                        
                        // Only store items that have aspect information
                        if (!aspectName.isEmpty() || !aspectDescription.isEmpty()) {
                            aspectsDatabase.put(itemName, new AspectInfo(aspectName, aspectDescription));
                        }
                    }
                    
                    System.out.println("DEBUG: GUI Aspects database loaded with " + aspectsDatabase.size() + " items");
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load aspects database for GUI: " + e.getMessage());
            e.printStackTrace();
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
