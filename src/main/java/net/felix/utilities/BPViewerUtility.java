package net.felix.utilities;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.client.gl.RenderPipelines;
import net.felix.CCLiveUtilitiesConfig;
import net.felix.OverlayType;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import org.joml.Matrix3x2fStack;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BPViewerUtility {
    
    private static boolean isInitialized = false;
    private static boolean isVisible = true;
    private static boolean showOverlays = true; // Neue Variable für Overlay-Sichtbarkeit
    private static BPViewerUtility INSTANCE;
    
    // Blueprint tracking variables
    private String currentRarity = "common";
    private static String manualFloor = null;
    private final Map<String, String> foundBlueprints = new HashMap<>();
    private final Map<String, Set<String>> floorProgress = new HashMap<>();
    
    // Constants
    private static final String[] RARITY_ORDER = {"common", "uncommon", "rare", "epic", "legendary"};
    private static final int HUD_WIDTH = 200;
    private static final int HUD_HEIGHT = 100; // Base height, will be adjusted dynamically
    private static final int RIGHT_MARGIN_PERCENT = 1;
    private static final int TOP_MARGIN_PERCENT = 2; // Original small top margin
    // Background texture for blueprint display
    private static final Identifier BLUEPRINT_BACKGROUND_TEXTURE = Identifier.of("cclive-utilities", "textures/gui/blueprint_background.png");
    
    // Patterns for blueprint detection
    private static final Pattern BLUEPRINT_PATTERN = Pattern.compile("§f\\[§6Legend§f\\] Du erhältst (.+?) §f- §3\\[Bauplan\\]");
    private static final Pattern BLUEPRINT_PATTERN_ALT = Pattern.compile("§a\\+ 1x (.+?) §f- §3\\[Bauplan\\]");
    private static final Pattern BLUEPRINT_PATTERN_ALT2 = Pattern.compile("§f\\[§6Legend§f\\] Du erhältst (.+?) - §3\\[Bauplan\\]");
    private static final Pattern BLUEPRINT_PATTERN_ALT3 = Pattern.compile("§a\\+ 1x (.+?) - §3\\[Bauplan\\]");
    // Additional pattern for combo chest rewards and edge cases
    private static final Pattern BLUEPRINT_PATTERN_COMBO = Pattern.compile("§a\\+ 1x (.+?) §f- §3\\[Bauplan\\]");
    // More flexible pattern that handles various spacing and formatting
    private static final Pattern BLUEPRINT_PATTERN_FLEXIBLE = Pattern.compile(".*?\\+ 1x (.+?) .*?\\[Bauplan\\]");
    // Pattern specifically for combo chest rewards
    private static final Pattern BLUEPRINT_PATTERN_COMBO_CHEST = Pattern.compile(".*?\\+ 1x (.+?) .*?\\[Bauplan\\].*?\\(\\d+\\)");
    
    // File handling
    private static final String SAVE_FILE_NAME = "found_blueprints.json";
    private static final String PROGRESS_FILE_NAME = "blueprint_progress.json";
    private final File saveFile;
    private final File progressFile;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    // Key bindings
    private static KeyBinding toggleKeyBinding;
    private static KeyBinding nextRarityKeyBinding;
    private static KeyBinding previousRarityKeyBinding;
    
    // Blueprint configuration
    private final BlueprintConfig config = new BlueprintConfig();
    private static final String BLUEPRINTS_CONFIG_FILE = "assets/cclive-utilities/blueprints.json";
    
    // Debug tracking
    private static String lastDebugFloor = null;
    private static boolean debugPrinted = false;
    
    // Inventory monitoring
    private static boolean lastWasBlueprintShop = false;
    private static final int[] BLUEPRINT_SHOP_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
    
    public BPViewerUtility() {
        INSTANCE = this;
        this.saveFile = new File("config", SAVE_FILE_NAME);
        this.progressFile = new File("config", PROGRESS_FILE_NAME);
        loadFoundBlueprints();
        loadProgress();
        loadBlueprintConfig();
    }
    
    public static BPViewerUtility getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("BPViewerUtility instance is null!");
        }
        return INSTANCE;
    }
    
    public static void initialize() {
        if (isInitialized) {
            return;
        }
        
        BPViewerUtility instance = new BPViewerUtility();
        
        // Register key bindings
        registerKeyBindings();
        
        // Register HUD render callback
        HudRenderCallback.EVENT.register((context, tickCounter) -> {
            if (isVisible && instance.getActiveFloor() != null && showOverlays && !EquipmentDisplayUtility.isEquipmentOverlayActive()) {
                instance.onHudRender(context, tickCounter);
            }
        });
        
                       // Register client tick events
               ClientTickEvents.END_CLIENT_TICK.register(client -> {
                   if (client.world != null) {
                       String dimensionId = client.world.getRegistryKey().getValue().toString();
                       String previousFloor = getCurrentFloor();
                       String newFloor = getCurrentFloor();

                       // Handle key bindings
                       if (toggleKeyBinding.wasPressed()) {
                           toggleVisibility();
                       }

                       if (nextRarityKeyBinding.wasPressed()) {
                           instance.nextRarity();
                       }

                       if (previousRarityKeyBinding.wasPressed()) {
                           instance.previousRarity();
                       }
                       
                       // Check for blueprint shop inventory
                       instance.checkBlueprintShopInventory(client);
                       
                       // Check Tab key for overlay visibility
                       checkTabKey();
                   }
               });
        
        // Register chat message listener for game messages
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, isOverlay) -> {
            String messageText = message.getString();
            instance.checkForBlueprint(messageText);
            return true;
        });
        
        // Also register for all message types to catch combo chest rewards
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, isOverlay) -> {
            String messageText = message.getString();
            // Debug: Log all messages that contain [Bauplan] or combo-related keywords
            if (messageText.contains("[Bauplan]") || messageText.contains("Kombo") || messageText.contains("Belohnungen")) {
                System.out.println("[BPViewer] Received message: " + messageText);
            }
            instance.checkForBlueprint(messageText);
            return true;
        });
        
        // Register tooltip render event to show checkmarks for found blueprints
        net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback.EVENT.register((stack, context, tooltipType, lines) -> {
            // Only process tooltips if blueprint viewer is enabled in config
            if (!CCLiveUtilitiesConfig.HANDLER.instance().blueprintViewerEnabled) {
                return;
            }
            
            // Check if we're hovering over a name_tag item (blueprint items)
            if (stack != null && stack.getItem().toString().contains("name_tag")) {
                // Check if we're in the special inventory (the one with 㬨)
                MinecraftClient mcClient = MinecraftClient.getInstance();
                boolean isSpecialInventory = false;
                if (mcClient != null && mcClient.currentScreen != null) {
                    String screenTitle = mcClient.currentScreen.getTitle().getString();
                    if (screenTitle.contains("㬪")) {
                        isSpecialInventory = true;
                    }
                }
                
                // Only process blueprints in the special inventory (㬨)
                if (!isSpecialInventory) {
                    return;
                }
            } else {
                // Not a name_tag item, skip
                return;
            }
            
            // Check if we're in the blueprint shop - if so, don't show checkmarks for any items
            MinecraftClient mcClient = MinecraftClient.getInstance();
            boolean isInShop = false;
            if (mcClient != null && mcClient.currentScreen instanceof HandledScreen) {
                HandledScreen<?> screen = (HandledScreen<?>) mcClient.currentScreen;
                String title = screen.getTitle().getString();
                String cleanTitle = title.replaceAll("[\\u3400-\\u4DBF\\u4E00-\\u9FFF]", "");
                cleanTitle = cleanTitle.replaceAll("§[0-9a-fk-or]", "");
                
                // More specific check for blueprint shop - look for exact patterns
                isInShop = cleanTitle.contains("Bauplan [Shop]") || 
                          cleanTitle.contains("Bauplan Shop") ||
                          (cleanTitle.contains("Bauplan") && cleanTitle.contains("Shop"));
                
                // If we're in the shop, don't show checkmarks for any items
                if (isInShop) {
                    return;
                }
            }
            
            // Process blueprint tooltips (we already confirmed it's a name_tag item)
            // Look through the existing tooltip lines to find blueprint names
            for (net.minecraft.text.Text line : lines) {
                String lineText = line.getString();
                
                // Check if this line contains a blueprint name - handle both original and modified formats
                // Original format: "Item Name - [Bauplan]"
                // Modified format (after SchmiedTrackerUtility): "Item Name [Ebene X] - [Bauplan]"
                if (lineText.contains(" - [Bauplan]")) {
                    String blueprintName = lineText.replace(" - [Bauplan]", "");
                    
                    // Remove Unicode formatting characters (㔺㔸 etc.)
                    blueprintName = blueprintName.replaceAll("[\\u3400-\\u4DBF\\u4E00-\\u9FFF]", "");
                    
                    // Remove percentage and parentheses
                    blueprintName = blueprintName.replaceAll("\\([^)]*\\)", "").trim();
                    
                    // Remove level information added by SchmiedTrackerUtility (e.g., "[Ebene 1]")
                    blueprintName = blueprintName.replaceAll("\\[Ebene \\d+\\]", "").trim();
                    
                    // Check if this blueprint name matches any blueprint we've found
                    if (instance.foundBlueprints.containsKey(blueprintName)) {
                        // Replace the current line with the blueprint name + checkmark, preserving original formatting
                        int lineIndex = lines.indexOf(line);
                        if (lineIndex != -1) {
                            // Create a mutable copy of the original text to preserve formatting
                            net.minecraft.text.MutableText newText = line.copy();
                            
                            // Simple approach: just add the green checkmark to the original text
                            net.minecraft.text.MutableText coloredText = line.copy();
                            coloredText.append(net.minecraft.text.Text.literal(" ✓").formatted(net.minecraft.util.Formatting.GREEN));
                            
                            lines.set(lineIndex, coloredText);
                        }
                        // Continue checking other lines (don't break)
                    } else {
                        // Only auto-mark blueprints if we're not in the shop
                        if (!isInShop) {
                            // Check if we're in the blueprint shop and auto-mark found blueprints
                            MinecraftClient client = MinecraftClient.getInstance();
                            if (client != null && client.currentScreen instanceof HandledScreen) {
                                HandledScreen<?> screen = (HandledScreen<?>) client.currentScreen;
                                String title = screen.getTitle().getString();
                                String cleanTitle = title.replaceAll("[\\u3400-\\u4DBF\\u4E00-\\u9FFF]", "");
                                cleanTitle = cleanTitle.replaceAll("§[0-9a-fk-or]", "");
                                
                                // Check if we're in the blueprint shop (multiple ways to detect)
                                boolean isInShopCheck = cleanTitle.contains("Bauplan [Shop]") || 
                                                      cleanTitle.contains("Bauplan Shop") ||
                                                      (cleanTitle.contains("Bauplan") && cleanTitle.contains("Shop"));
                                
                                if (isInShopCheck) {
                                    // Since we're in the shop (not on a floor), search all floors
                                    boolean foundInAnyFloor = false;
                                    
                                    for (Map.Entry<String, BlueprintConfig.FloorData> floorEntry : instance.config.floors.entrySet()) {
                                        BlueprintConfig.FloorData floor = floorEntry.getValue();
                                        if (floor != null && floor.blueprints != null) {
                                            for (Map.Entry<String, BlueprintConfig.RarityData> rarityEntry : floor.blueprints.entrySet()) {
                                                if (rarityEntry.getValue().items.contains(blueprintName)) {
                                                    instance.markBlueprintAsFound(blueprintName, rarityEntry.getKey());
                                                    foundInAnyFloor = true;
                                                    break;
                                                }
                                            }
                                            if (foundInAnyFloor) break;
                                        }
                                    }
                                    
                                    // If not found in any floor, mark as "unknown" rarity
                                    if (!foundInAnyFloor && !instance.foundBlueprints.containsKey(blueprintName)) {
                                        instance.markBlueprintAsFound(blueprintName, "unknown");
                                    }
                                    
                                    // Also trigger slot scanning for immediate detection
                                    instance.scanBlueprintShopSlots(screen);
                                } else {
                                    // Even if shop detection fails, try slot scanning if we're hovering over a name tag with blueprints
                                    if (client.currentScreen instanceof HandledScreen) {
                                        instance.scanBlueprintShopSlots((HandledScreen<?>) client.currentScreen);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        });

               // Register commands
               registerCommands();

               isInitialized = true;
    }
    
    private static void registerKeyBindings() {
        toggleKeyBinding = new KeyBinding(
            "key.cclive-utilities.toggle-blueprint-viewer",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            "key.categories.cclive-utilities.blueprints"
        );
        
        nextRarityKeyBinding = new KeyBinding(
            "key.cclive-utilities.next-blueprint-rarity",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT,
            "key.categories.cclive-utilities.blueprints"
        );
        
        previousRarityKeyBinding = new KeyBinding(
            "key.cclive-utilities.previous-blueprint-rarity",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT,
            "key.categories.cclive-utilities.blueprints"
        );
        
        // Register key bindings
        KeyBindingHelper.registerKeyBinding(toggleKeyBinding);
        KeyBindingHelper.registerKeyBinding(nextRarityKeyBinding);
        KeyBindingHelper.registerKeyBinding(previousRarityKeyBinding);
    }
           
           private static void registerCommands() {
               ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
                   dispatcher.register(ClientCommandManager.literal("bp")
                       .then(ClientCommandManager.literal("reset")
                           .executes(context -> {
                               BPViewerUtility instance = getInstance();
                               instance.resetFoundBlueprints();
                               context.getSource().sendFeedback(Text.literal("§aAlle gefundenen Baupläne wurden zurückgesetzt!"));
                               return 1;
                           })
                       )
                       .then(ClientCommandManager.literal("set")
                           .then(ClientCommandManager.literal("floor")
                               .then(ClientCommandManager.argument("floorNumber", StringArgumentType.string())
                                   .executes(context -> {
                                       String floorNumber = StringArgumentType.getString(context, "floorNumber");
                                       BPViewerUtility instance = getInstance();
                                       instance.setManualFloor(floorNumber);
                                       instance.setVisibility(true, floorNumber);
                                       context.getSource().sendFeedback(Text.literal("§aBauplan-Anzeige für Ebene " + floorNumber + " aktiviert!"));
                                       return 1;
                                   })
                               )
                           )
                       )
                   );
               });
           }
    
    private void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null) return;
            
            // Check if blueprint viewer is enabled in config
            if (!CCLiveUtilitiesConfig.HANDLER.instance().blueprintViewerEnabled || 
                !CCLiveUtilitiesConfig.HANDLER.instance().showBlueprintViewer) {
                return;
            }
            
            int screenWidth = client.getWindow().getScaledWidth();
            int screenHeight = client.getWindow().getScaledHeight();
            
            // Use config values for positioning
            int x = screenWidth - HUD_WIDTH - CCLiveUtilitiesConfig.HANDLER.instance().blueprintViewerX;
            int y = screenHeight * CCLiveUtilitiesConfig.HANDLER.instance().blueprintViewerY / 100;
            
                           // Debug: Show HUD position (only once per frame)
               if (!debugPrinted) {
                   
               }
            

            

            
            // Get floor data and render blueprints
            String activeFloor = getActiveFloor();
            BlueprintConfig.FloorData floorData = config.getFloorData(activeFloor);
            
            // Debug info (only print once per floor change)
            if (!activeFloor.equals(lastDebugFloor)) {
                
                if (floorData != null) {
                    
                }
                lastDebugFloor = activeFloor;
            }
            
            if (floorData != null && floorData.blueprints != null && floorData.blueprints.containsKey(currentRarity)) {
                BlueprintConfig.RarityData rarityData = floorData.blueprints.get(currentRarity);
                
                // Calculate dynamic size - 15px smaller from right
                int dynamicWidth = calculateRequiredWidth(rarityData.items) - 15;
                int dynamicHeight = calculateRequiredHeight(rarityData.items);
                
                // Recalculate position with dynamic width using config
                int dynamicX = screenWidth - dynamicWidth - CCLiveUtilitiesConfig.HANDLER.instance().blueprintViewerX;
                
                // Verwende Matrix-Transformationen für das gesamte Overlay
                Matrix3x2fStack matrices = context.getMatrices();
                matrices.pushMatrix();
                
                // Skaliere basierend auf der Config
                float scale = CCLiveUtilitiesConfig.HANDLER.instance().blueprintViewerScale;
                if (scale <= 0) scale = 1.0f; // Sicherheitscheck
                
                // Übersetze zur Position und skaliere von dort aus
                matrices.translate(dynamicX, y);
                matrices.scale(scale, scale);
                
                        // Render complete background (skaliert) basierend auf Overlay-Typ
        OverlayType overlayType = CCLiveUtilitiesConfig.HANDLER.instance().blueprintViewerOverlayType;
        if (overlayType == OverlayType.CUSTOM) {
            // Bild-Overlay mit blueprint_background.png
            context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                BLUEPRINT_BACKGROUND_TEXTURE,
                0, 0, // Position (0-basiert, da wir bereits übersetzt haben)
                0.0f, 0.0f, // UV-Koordinaten
                dynamicWidth, dynamicHeight, // Größe
                dynamicWidth, dynamicHeight // Textur-Größe
            );
        } else if (overlayType == OverlayType.BLACK) {
            // Schwarzes halbtransparentes Overlay
            context.fill(0, 0, dynamicWidth, dynamicHeight, 0x80000000); // Schwarzer Hintergrund mit 50% Transparenz
        }
        // Bei OverlayType.NONE wird kein Hintergrund gezeichnet
                
                // Matrix-Transformationen wiederherstellen
                matrices.popMatrix();
                
                // Only print debug once per frame
                if (!debugPrinted) {
                    
                    debugPrinted = true;
                }
                
                // Render title and blueprint list together in the scaled area
                renderTitleAndBlueprintList(context, dynamicX + 10, y + 2, rarityData.items, rarityData.color, dynamicWidth);
            } else {
                // Show error message if no data found
                context.drawText(client.textRenderer, "No data for floor: " + activeFloor, x + 10, y + 30, 0xFFFF0000, false);
                context.drawText(client.textRenderer, "Rarity: " + currentRarity, x + 10, y + 45, 0xFFFFFF00, false);
                if (floorData != null && floorData.blueprints != null) {
                    context.drawText(client.textRenderer, "Available: " + floorData.blueprints.keySet(), x + 10, y + 60, 0xFFFFFF00, false);
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Reset debug flag for next frame
        debugPrinted = false;
    }
    
    private int calculateRequiredHeight(List<String> blueprints) {
        // Base height for title and padding - reduced for smaller overlay
        int baseHeight = 20;
        // Height per blueprint (12px spacing)
        int blueprintHeight = blueprints.size() * 12;
        // Add some padding at the bottom - reduced
        int bottomPadding = 5;
        return baseHeight + blueprintHeight + bottomPadding;
    }
    
    private int calculateRequiredWidth(List<String> blueprints) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return HUD_WIDTH;
        
        int maxWidth = HUD_WIDTH;
        for (String blueprint : blueprints) {
            String displayText = blueprint.startsWith("- ") ? blueprint.substring(2) : blueprint;
            int textWidth = client.textRenderer.getWidth(displayText);
            // Add padding for the text (left margin + right margin) - increased for longer German text
            textWidth += 45;
            if (textWidth > maxWidth) {
                maxWidth = textWidth;
            }
        }
        // Ensure minimum width
        return Math.max(maxWidth, HUD_WIDTH);
    }
    
    /**
     * Rendert den Titel (Rarity) ohne Skalierung
     */
    private void renderTitle(DrawContext context, int x, int y, String rarityColor, int overlayWidth) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        
        // Render rarity title with bright color for visibility (unskaliert)
        String rarityDisplay = currentRarity.toUpperCase();
        
        // Zentriere die Überschrift in der Mitte des Overlays
        // x ist bereits dynamicX + 10, also müssen wir das berücksichtigen
        int titleX = (x - 10) + (overlayWidth - client.textRenderer.getWidth(rarityDisplay)) / 2;
        
        // Get the appropriate color for the rarity - using bright colors
        int titleColor;
        switch (currentRarity.toLowerCase()) {
            case "common": titleColor = 0xFFFFFFFF; break;      // Bright white
            case "uncommon": titleColor = 0xFF00FF00; break;    // Bright green
            case "rare": titleColor = 0xFF0000FF; break;        // Bright blue
            case "epic": titleColor = 0xFFFF00FF; break;        // Bright magenta
            case "legendary": titleColor = 0xFFFFFF00; break;   // Bright yellow
            default: titleColor = 0xFFFFFFFF; break;            // Bright white as default
        }
        context.drawText(client.textRenderer, rarityDisplay, titleX, y + 5, titleColor, true);
    }
    
    /**
     * Rendert den Titel und die Bauplan-Liste zusammen im skalierten Bereich
     */
    private void renderTitleAndBlueprintList(DrawContext context, int x, int y, List<String> blueprints, String rarityColor, int overlayWidth) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        
        // Verwende Matrix-Transformationen für Skalierung
        Matrix3x2fStack matrices = context.getMatrices();
        matrices.pushMatrix();
        
        // Skaliere basierend auf der Config
        float scale = CCLiveUtilitiesConfig.HANDLER.instance().blueprintViewerScale;
        if (scale <= 0) scale = 1.0f; // Sicherheitscheck
        
        // Übersetze zur Position und skaliere von dort aus
        matrices.translate(x, y);
        matrices.scale(scale, scale);
        
        // Render title (skaliert) - über dem Hintergrund
        String rarityDisplay = currentRarity.toUpperCase();
        
        // Überschrift perfekt mittig positionieren
        int textWidth = client.textRenderer.getWidth(rarityDisplay);
        int titleX = -9 + (overlayWidth - textWidth) / 2;
        
        // Get the appropriate color for the rarity - using bright colors
        int titleColor;
        switch (currentRarity.toLowerCase()) {
            case "common": titleColor = 0xFFFFFFFF; break;      // Bright white
            case "uncommon": titleColor = 0xFF00FF00; break;    // Bright green
            case "rare": titleColor = 0xFF0000FF; break;        // Bright blue
            case "epic": titleColor = 0xFFFF00FF; break;        // Bright magenta
            case "legendary": titleColor = 0xFFFFFF00; break;   // Bright yellow
            default: titleColor = 0xFFFFFFFF; break;            // Bright white as default
        }
        context.drawText(client.textRenderer, rarityDisplay, titleX, 5, titleColor, true);
        
        // Render blueprint list - start below the title (moved 3 pixels up)
        int blueprintY = 17; // Start below the title
        
        for (String blueprint : blueprints) {
            String displayText = blueprint.startsWith("- ") ? blueprint.substring(2) : blueprint;
            boolean isFound = foundBlueprints.containsKey(displayText) || isBlueprintFound(getActiveFloor(), displayText);
            
            // Draw the text with appropriate color based on found status (skaliert)
            int textColor = isFound ? 0xFFFFFFFF : 0xFF888888; // White if found, gray if not found
            context.drawText(client.textRenderer, displayText, 5, blueprintY, textColor, true);
            
            blueprintY += 12; // Increased spacing for better readability (skaliert)
        }
        
        // Matrix-Transformationen wiederherstellen
        matrices.popMatrix();
    }
    
    private void renderBlueprintList(DrawContext context, int x, int y, List<String> blueprints, String rarityColor) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        
        // Verwende Matrix-Transformationen für Skalierung
        Matrix3x2fStack matrices = context.getMatrices();
        matrices.pushMatrix();
        
        // Skaliere basierend auf der Config
        float scale = CCLiveUtilitiesConfig.HANDLER.instance().blueprintViewerScale;
        if (scale <= 0) scale = 1.0f; // Sicherheitscheck
        
        // Übersetze zur Position und skaliere von dort aus
        matrices.translate(x, y);
        matrices.scale(scale, scale);
        
        // Debug: Show that we're in renderBlueprintList (only once per frame)
        if (!debugPrinted) {
        
        }
        
        // Render blueprint list - start from the beginning since title is rendered separately (moved 3 pixels up)
        int blueprintY = -3; // Start from the beginning of the scalable area
        
        
        
        for (String blueprint : blueprints) {
            String displayText = blueprint.startsWith("- ") ? blueprint.substring(2) : blueprint;
            boolean isFound = foundBlueprints.containsKey(displayText) || isBlueprintFound(getActiveFloor(), displayText);
            
            // Use normal colors for blueprints
            int color;
            if (isFound) {
                color = getColorFromString(rarityColor); // Use rarity color for found
 
            } else {
                color = 0x888888; // Gray for unfound
 
            }
            
            // Get the actual text width for this blueprint
            int textWidth = client.textRenderer.getWidth(displayText);
            

            
            // Draw the text with appropriate color based on found status (skaliert)
            int textColor = isFound ? 0xFFFFFFFF : 0xFF888888; // White if found, gray if not found
            context.drawText(client.textRenderer, displayText, 5, blueprintY, textColor, true);
            if (!debugPrinted) {
        
            }
            
            			blueprintY += 12; // Increased spacing for better readability (skaliert)
        }
        
        // Matrix-Transformationen wiederherstellen
        matrices.popMatrix();
    }
    
               	private static void checkTabKey() {
		// Check if player list key is pressed (respects custom key bindings)
		if (KeyBindingUtility.isPlayerListKeyPressed()) {
			showOverlays = false; // Hide overlays when player list key is pressed
		} else {
			showOverlays = true; // Show overlays when player list key is released
		}
	}
           
           public void checkForBlueprint(String messageText) {
               // Debug logging to see what messages are being processed
               if (messageText.contains("[Bauplan]") || messageText.contains("Kombo") || messageText.contains("Belohnungen")) {
                   System.out.println("[BPViewer] Processing message: " + messageText);
               }
               
               Matcher matcher = BLUEPRINT_PATTERN.matcher(messageText);
               if (!matcher.matches()) {
                   matcher = BLUEPRINT_PATTERN_ALT.matcher(messageText);
               }
               if (!matcher.matches()) {
                   matcher = BLUEPRINT_PATTERN_ALT2.matcher(messageText);
               }
               if (!matcher.matches()) {
                   matcher = BLUEPRINT_PATTERN_ALT3.matcher(messageText);
               }
               if (!matcher.matches()) {
                   matcher = BLUEPRINT_PATTERN_COMBO.matcher(messageText);
               }
               if (!matcher.matches()) {
                   matcher = BLUEPRINT_PATTERN_FLEXIBLE.matcher(messageText);
               }
               if (!matcher.matches()) {
                   matcher = BLUEPRINT_PATTERN_COMBO_CHEST.matcher(messageText);
               }

               if (matcher.matches()) {
                   String blueprintName = matcher.group(1).trim();
                   System.out.println("[BPViewer] Found blueprint: " + blueprintName);

                   if (foundBlueprints.containsKey(blueprintName)) {
                       System.out.println("[BPViewer] Blueprint already found: " + blueprintName);
                       return; // Already found
                   }

                   String activeFloor = getActiveFloor();
                   System.out.println("[BPViewer] Active floor: " + activeFloor);
                   
                   BlueprintConfig.FloorData floorData = config.getFloorData(activeFloor);
                   if (floorData != null && floorData.blueprints != null) {
                       boolean foundInFloor = false;
                       for (Map.Entry<String, BlueprintConfig.RarityData> entry : floorData.blueprints.entrySet()) {
                           if (entry.getValue().items.contains(blueprintName)) {
                               System.out.println("[BPViewer] Marking blueprint as found: " + blueprintName + " (rarity: " + entry.getKey() + ")");
                               markBlueprintAsFound(blueprintName, entry.getKey());
                               foundInFloor = true;
                               break;
                           }
                       }
                       if (!foundInFloor) {
                           System.out.println("[BPViewer] Blueprint not found in current floor: " + blueprintName);
                           // Try to find it in other floors
                           for (Map.Entry<String, BlueprintConfig.FloorData> floorEntry : config.floors.entrySet()) {
                               if (floorEntry.getValue().blueprints != null) {
                                   for (Map.Entry<String, BlueprintConfig.RarityData> rarityEntry : floorEntry.getValue().blueprints.entrySet()) {
                                       if (rarityEntry.getValue().items.contains(blueprintName)) {
                                           System.out.println("[BPViewer] Found blueprint in floor " + floorEntry.getKey() + ": " + blueprintName + " (rarity: " + rarityEntry.getKey() + ")");
                                           markBlueprintAsFound(blueprintName, rarityEntry.getKey());
                                           break;
                                       }
                                   }
                               }
                           }
                       }
                   } else {
                       System.out.println("[BPViewer] No floor data found for current floor: " + activeFloor);
                   }
               } else if (messageText.contains("[Bauplan]")) {
                   System.out.println("[BPViewer] Message contains [Bauplan] but no pattern matched: " + messageText);
               }
           }
           
           private void checkBlueprintShopInventory(MinecraftClient client) {
               if (client.currentScreen instanceof HandledScreen) {
                   HandledScreen<?> screen = (HandledScreen<?>) client.currentScreen;
                   String title = screen.getTitle().getString();
                   

                   
                   // Remove Unicode formatting characters and Minecraft formatting codes from title
                   String cleanTitle = title.replaceAll("[\\u3400-\\u4DBF\\u4E00-\\u9FFF]", "");
                   cleanTitle = cleanTitle.replaceAll("§[0-9a-fk-or]", "");

                   
                   // Check if this is the blueprint shop (multiple ways to detect)
                   boolean isInShop = cleanTitle.contains("Bauplan [Shop]") || 
                                     cleanTitle.contains("Bauplan Shop") ||
                                     (cleanTitle.contains("Bauplan") && cleanTitle.contains("Shop"));
                   
                   if (isInShop) {
                       if (!lastWasBlueprintShop) {
                           lastWasBlueprintShop = true;
                       }
                       
                       // Scan the specified slots for blueprints
                       scanBlueprintShopSlots(screen);
                   } else {
                       lastWasBlueprintShop = false;
                   }
               } else {
                   lastWasBlueprintShop = false;
               }
           }
           
           private void scanBlueprintShopSlots(HandledScreen<?> screen) {
               if (screen.getScreenHandler() == null) {
                   return;
               }
               

               
               int itemsFound = 0;
               for (int slotIndex : BLUEPRINT_SHOP_SLOTS) {
                   if (slotIndex < screen.getScreenHandler().slots.size()) {
                       Slot slot = screen.getScreenHandler().slots.get(slotIndex);
                       ItemStack itemStack = slot.getStack();
                       
                       if (!itemStack.isEmpty()) {
                           itemsFound++;
                           // Get the item name and check if it's a blueprint
                           String itemName = itemStack.getName().getString();

                           
                           // Check if the item name contains [Bauplan]
                           if (itemName.contains("[Bauplan]")) {
                               // Extract the blueprint name (remove [Bauplan] and any formatting)
                               String blueprintName = itemName.replaceAll("\\[Bauplan\\]", "").trim();
                               // Remove Unicode formatting characters
                               blueprintName = blueprintName.replaceAll("[\\u3400-\\u4DBF\\u4E00-\\u9FFF]", "");
                               // Remove any remaining formatting codes
                               blueprintName = blueprintName.replaceAll("§[0-9a-fk-or]", "");
                               // Remove trailing spaces, dashes, and other common suffixes
                               blueprintName = blueprintName.replaceAll("\\s*-\\s*$", "").trim();
                               

                               
                               // Search all floors for this blueprint
                               boolean foundInAnyFloor = false;
                               for (Map.Entry<String, BlueprintConfig.FloorData> floorEntry : config.floors.entrySet()) {
                                   BlueprintConfig.FloorData floor = floorEntry.getValue();
                                   if (floor != null && floor.blueprints != null) {
                                       for (Map.Entry<String, BlueprintConfig.RarityData> rarityEntry : floor.blueprints.entrySet()) {
                                           if (rarityEntry.getValue().items.contains(blueprintName)) {
                                               if (!foundBlueprints.containsKey(blueprintName)) {
                                                   markBlueprintAsFound(blueprintName, rarityEntry.getKey());
                                               }
                                               foundInAnyFloor = true;
                                               break;
                                           }
                                       }
                                       if (foundInAnyFloor) break;
                                   }
                               }
                               
                               if (!foundInAnyFloor && !foundBlueprints.containsKey(blueprintName)) {
                                   markBlueprintAsFound(blueprintName, "unknown");
                               }
                                                          }
                       }
                   } else {
    
                   }
               }
               

           }
    
    private void markBlueprintAsFound(String blueprintName, String rarity) {
        System.out.println("[BPViewer] markBlueprintAsFound called with: " + blueprintName + ", rarity: " + rarity);
        if (!foundBlueprints.containsKey(blueprintName)) {
            foundBlueprints.put(blueprintName, rarity);
            markBlueprintFound(getActiveFloor(), blueprintName);
            saveFoundBlueprints();
            System.out.println("[BPViewer] Successfully marked blueprint as found: " + blueprintName);
        } else {
            System.out.println("[BPViewer] Blueprint already in foundBlueprints: " + blueprintName);
        }
    }
    
    private void nextRarity() {
        int currentIndex = getCurrentRarityIndex();
        if (currentIndex < RARITY_ORDER.length - 1) {
            currentRarity = RARITY_ORDER[currentIndex + 1];
        }
    }
    
    private void previousRarity() {
        int currentIndex = getCurrentRarityIndex();
        if (currentIndex > 0) {
            currentRarity = RARITY_ORDER[currentIndex - 1];
        }
    }
    
    private int getCurrentRarityIndex() {
        for (int i = 0; i < RARITY_ORDER.length; i++) {
            if (RARITY_ORDER[i].equals(currentRarity)) {
                return i;
            }
        }
        return 0;
    }
    
    public static void toggleVisibility() {
        isVisible = !isVisible;
        if (!isVisible) {
            manualFloor = null;
        }
    }
    
    public static void setVisibility(boolean visible, String floor) {
        isVisible = visible;
        if (!visible) {
            manualFloor = null;
        } else if (floor != null) {
            getInstance().setManualFloor(floor);
        }
    }
    
    public static boolean isVisible() {
        return isVisible;
    }
    
    public String getActiveFloor() {
        return manualFloor != null ? manualFloor : getCurrentFloor();
    }
    
    public void setManualFloor(String floor) {
        if (floor != null && !floor.startsWith("floor_")) {
            floor = "floor_" + floor;
        }
        manualFloor = floor;
    }
    
    private static String getCurrentFloor() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.world != null) {
            String dimensionId = client.world.getRegistryKey().getValue().toString();
            
            if (dimensionId.startsWith("minecraft:floor_")) {
                String floorPart = dimensionId.substring("minecraft:floor_".length());
                String[] parts = floorPart.split("_");
                if (parts.length >= 1) {
                    String floorNumber = parts[0];
                    String floorKey = "floor_" + floorNumber;
                    return floorKey;
                }
            }
        }
        return null;
    }
    
    private void loadFoundBlueprints() {
        if (saveFile.exists()) {
            try (FileReader reader = new FileReader(saveFile)) {
                Type type = new TypeToken<HashMap<String, String>>(){}.getType();
                Map<String, String> loaded = gson.fromJson(reader, type);
                if (loaded != null) {
                    foundBlueprints.clear();
                    foundBlueprints.putAll(loaded);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void saveFoundBlueprints() {
        try (FileWriter writer = new FileWriter(saveFile)) {
            gson.toJson(foundBlueprints, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void loadProgress() {
        if (progressFile.exists()) {
            try (FileReader reader = new FileReader(progressFile)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                JsonObject progress = json.getAsJsonObject("progress");
                
                for (String floor : progress.keySet()) {
                    Set<String> floorBlueprints = new HashSet<>();
                    progress.getAsJsonArray(floor).forEach(element -> 
                        floorBlueprints.add(element.getAsString()));
                    floorProgress.put(floor, floorBlueprints);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void saveProgress() {
        try {
            progressFile.getParentFile().mkdirs();
            JsonObject json = new JsonObject();
            json.addProperty("modVersion", "1.0.0");
            
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.player != null) {
                json.addProperty("username", client.player.getName().getString());
            } else {
                json.addProperty("username", "unknown");
            }
            
            JsonObject progress = new JsonObject();
            for (Map.Entry<String, Set<String>> entry : floorProgress.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    progress.add(entry.getKey(), gson.toJsonTree(entry.getValue()));
                }
            }
            json.add("progress", progress);
            
            try (FileWriter writer = new FileWriter(progressFile)) {
                gson.toJson(json, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void markBlueprintFound(String floor, String blueprintName) {
        if (floor != null && blueprintName != null) {
            floorProgress.computeIfAbsent(floor, k -> new HashSet<>()).add(blueprintName);
            saveProgress();
        }
    }
    
    public boolean isBlueprintFound(String floor, String blueprintName) {
        Set<String> floorBlueprints = floorProgress.get(floor);
        return floorBlueprints != null && floorBlueprints.contains(blueprintName);
    }
    
    public void resetFoundBlueprints() {
 
        
        // Clear found blueprints
        foundBlueprints.clear();
        
        // Clear floor progress
        floorProgress.clear();
        
        // Save empty data to files
        saveFoundBlueprints();
        saveProgress();
        
 
    }
    
    private int getColorFromString(String colorString) {
        switch (colorString.toUpperCase()) {
            case "WHITE": return 0xFFFFFF;
            case "GREEN": return 0x55FF55;
            case "BLUE": return 0x5555FF;
            case "PURPLE": return 0xAA00AA;
            case "GOLD": return 0xFFAA00;
            default: return 0xFFFFFF;
        }
    }
    
    private void loadBlueprintConfig() {
        try {
            // Load from mod resources
            var resource = FabricLoader.getInstance().getModContainer("cclive-utilities")
                .orElseThrow(() -> new RuntimeException("Mod container not found"))
                .findPath(BLUEPRINTS_CONFIG_FILE)
                .orElseThrow(() -> new RuntimeException("Blueprint config file not found"));
            
     
            
            try (var inputStream = java.nio.file.Files.newInputStream(resource)) {
                try (var reader = new java.io.InputStreamReader(inputStream)) {
                    JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                    JsonObject floors = json.getAsJsonObject("floors");
                    
             
                    
                    for (String floorKey : floors.keySet()) {
                        JsonObject floorData = floors.getAsJsonObject(floorKey);
                        JsonObject blueprints = floorData.getAsJsonObject("blueprints");
                        
                        BlueprintConfig.FloorData floor = new BlueprintConfig.FloorData();
                        
             
                        
                        for (String rarityKey : blueprints.keySet()) {
                            JsonObject rarityData = blueprints.getAsJsonObject(rarityKey);
                            String color = rarityData.get("color").getAsString();
                            com.google.gson.JsonArray itemsArray = rarityData.getAsJsonArray("items");
                            
                            List<String> items = new java.util.ArrayList<>();
                            itemsArray.forEach(element -> items.add(element.getAsString()));
                            
                            floor.blueprints.put(rarityKey, new BlueprintConfig.RarityData(items, color));
                            
                        }
                        
                        config.floors.put(floorKey, floor);
                    }
                    
             
                    
                    // Debug: Show first few floors
                    int count = 0;
                    for (String floorKey : config.floors.keySet()) {
                        if (count < 3) {
                            BlueprintConfig.FloorData floor = config.floors.get(floorKey);
                            
                            count++;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load blueprint config: " + e.getMessage());
            e.printStackTrace();
            // Fallback to hardcoded config
            initializeFallbackFloors();
        }
    }
    
    private void initializeFallbackFloors() {
        // Fallback configuration if JSON loading fails
        BlueprintConfig.FloorData floor1 = new BlueprintConfig.FloorData();
        floor1.blueprints.put("common", new BlueprintConfig.RarityData(List.of(
            "Anfänger Hacke", "Anfänger Axt", "Anfänger Spitzhacke", "Gebeulte Handgelenkmanschetten", "Kopfgeldjägerring"
        ), "WHITE"));
        floor1.blueprints.put("uncommon", new BlueprintConfig.RarityData(List.of(
            "Erzbrecherballiste", "Ergiebiger Langbogen", "Pelzgewand des Eiswanderers"
        ), "GREEN"));
        floor1.blueprints.put("rare", new BlueprintConfig.RarityData(List.of(
            "Halskette des Bezwingers", "Eroberers Schulterpanzer", "Schädelspalter"
        ), "BLUE"));
        config.floors.put("floor_1", floor1);
 
    }
    
    // Blueprint configuration classes
    public static class BlueprintConfig {
        public final Map<String, FloorData> floors = new HashMap<>();
        
        public BlueprintConfig() {
            // Configuration is now loaded from JSON file
        }
        
        public FloorData getFloorData(String floor) {
            return floors.get(floor);
        }
        
        public static class FloorData {
            public Map<String, RarityData> blueprints = new HashMap<>();
        }
        
        public static class RarityData {
            public List<String> items;
            public String color;
            
            public RarityData(List<String> items, String color) {
                this.items = items;
                this.color = color;
            }
        }
    }
} 