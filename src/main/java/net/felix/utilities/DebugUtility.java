package net.felix.utilities;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.felix.CCLiveUtilitiesConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Utility-Klasse für einheitliche Debug-Nachrichten und Item-Logging
 */
public class DebugUtility {
    
    private static final String DEBUG_PREFIX = "§7[§bCCLive-Debug§7]§r ";
    
    // Item Hover Logger fields
    private static boolean isItemLoggerInitialized = false;
    
    // Store last mouse position for reliable hover detection
    private static double lastMouseX = -1;
    private static double lastMouseY = -1;
    
    // Store the currently hovered item from ItemTooltipCallback
    private static ItemStack lastHoveredItem = ItemStack.EMPTY;
    private static long lastHoveredItemTime = 0;
    private static final long HOVER_ITEM_TIMEOUT_MS = 200; // Consider item still hovered if tooltip was shown within 200ms
    
    // Track key press states to prevent multiple triggers
    private static boolean f8Pressed = false;
    private static boolean f9Pressed = false;
    private static boolean f10Pressed = false;
    private static boolean f12Pressed = false;
    
    /**
     * Sendet eine Blueprint-Debug-Nachricht
     */
    public static void debugBlueprint(String message) {
        if (CCLiveUtilitiesConfig.HANDLER.instance().blueprintDebugging) {
            sendDebugMessage(message);
        }
    }
    
    /**
     * Sendet eine Leaderboard-Debug-Nachricht
     */
    public static void debugLeaderboard(String message) {
        if (CCLiveUtilitiesConfig.HANDLER.instance().leaderboardDebugging) {
            sendDebugMessage(message);
        }
    }
    
    /**
     * Sendet eine Debug-Nachricht nur an die Console (nicht in den Chat um Endlosschleifen zu vermeiden)
     */
    private static void sendDebugMessage(String message) {
        // Debug-Logs entfernt
    }
    
    /**
     * Prüft ob Blueprint-Debugging aktiviert ist
     */
    public static boolean isBlueprintDebuggingEnabled() {
        return CCLiveUtilitiesConfig.HANDLER.instance().blueprintDebugging;
    }
    
    /**
     * Prüft ob Leaderboard-Debugging aktiviert ist
     */
    public static boolean isLeaderboardDebuggingEnabled() {
        return CCLiveUtilitiesConfig.HANDLER.instance().leaderboardDebugging;
    }
    
    // ========== Item Hover Logger Functions ==========
    
    /**
     * Initializes the item hover logger functionality
     */
    public static void initializeItemLogger() {
        if (isItemLoggerInitialized) {
            return;
        }
        
        try {
            // Register ItemTooltipCallback to track currently hovered item
            // NOTE: This only stores the item, it does NOT log anything
            // Logging only happens when the hotkey is pressed
            ItemTooltipCallback.EVENT.register((stack, context, tooltipType, lines) -> {
                if (stack != null && !stack.isEmpty()) {
                    lastHoveredItem = stack;
                    lastHoveredItemTime = System.currentTimeMillis();
                }
            });
            
            // Register client tick event
            ClientTickEvents.END_CLIENT_TICK.register(DebugUtility::onItemLoggerClientTick);
            
            isItemLoggerInitialized = true;
        } catch (Exception e) {
            // Silent error handling
        }
    }
    
    private static void onItemLoggerClientTick(MinecraftClient client) {
        // Only check keys if debug functions are enabled
        if (!CCLiveUtilitiesConfig.HANDLER.instance().debugFunctionsEnabled) {
            return;
        }
        
        // Check hardcoded keys: F8, F9, F10, F12
        if (client.getWindow() != null) {
            long windowHandle = client.getWindow().getHandle();
            
            // F8 - ItemHoverLogger
            boolean f8CurrentlyPressed = InputUtil.isKeyPressed(windowHandle, GLFW.GLFW_KEY_F8);
            if (f8CurrentlyPressed && !f8Pressed) {
                logHoveredItemInfo(client);
            }
            f8Pressed = f8CurrentlyPressed;
            
            // F9 - InventoryNameLogger
            boolean f9CurrentlyPressed = InputUtil.isKeyPressed(windowHandle, GLFW.GLFW_KEY_F9);
            if (f9CurrentlyPressed && !f9Pressed) {
                logInventoryName(client);
            }
            f9Pressed = f9CurrentlyPressed;
            
            // F10 - ScoreboardLogger
            boolean f10CurrentlyPressed = InputUtil.isKeyPressed(windowHandle, GLFW.GLFW_KEY_F10);
            if (f10CurrentlyPressed && !f10Pressed) {
                logScoreboard(client);
            }
            f10Pressed = f10CurrentlyPressed;
            
            // F12 - BossBarLogger
            boolean f12CurrentlyPressed = InputUtil.isKeyPressed(windowHandle, GLFW.GLFW_KEY_F12);
            if (f12CurrentlyPressed && !f12Pressed) {
                logBossBars(client);
            }
            f12Pressed = f12CurrentlyPressed;
        }
    }
    
    /**
     * Logs the hover info of the currently hovered item
     */
    private static void logHoveredItemInfo(MinecraftClient client) {
        // Check if debug functions are enabled
        if (!CCLiveUtilitiesConfig.HANDLER.instance().debugFunctionsEnabled) {
            return;
        }
        
        if (client == null || client.player == null) {
            System.out.println("[ItemHoverLogger] Kein Spieler gefunden!");
            return;
        }
        
        ItemStack hoveredItem = getHoveredItem(client);
        
        if (hoveredItem == null || hoveredItem.isEmpty()) {
            System.out.println("[ItemHoverLogger] Kein Item gefunden!");
            return;
        }
        
        // Get tooltip
        List<Text> tooltip = getItemTooltip(hoveredItem, client.player);
        
        if (tooltip == null || tooltip.isEmpty()) {
            System.out.println("[ItemHoverLogger] Kein Tooltip gefunden!");
            return;
        }
        
        // Create both raw and clean versions
        List<String> rawLines = new ArrayList<>();
        List<String> cleanLines = new ArrayList<>();
        for (Text line : tooltip) {
            String rawLine = getTextWithFormatting(line);
            // Clean version: remove all formatting codes (including hex colors) and Chinese characters
            String cleanLine = rawLine
                .replaceAll("§[0-9a-fk-or]", "")  // Remove standard formatting codes
                .replaceAll("§#[0-9a-fA-F]{6}", "")  // Remove hex color codes like §#FF8000
                .replaceAll("§x(§[0-9a-fA-F]){6}", "")  // Remove hex color codes like §x§f§f§8§0§0§0
                .replaceAll("[\\u3400-\\u4DBF]", "")  // Remove Chinese characters
                .trim();
            rawLines.add(rawLine);
            cleanLines.add(cleanLine);
        }
        
        // Log item info - Raw version first
        System.out.println("========================================");
        System.out.println("[ItemHoverLogger] Item Tooltip (RAW - mit Formatierung):");
        System.out.println("========================================");
        System.out.println("Anzahl Zeilen: " + tooltip.size());
        System.out.println("----------------------------------------");
        
        for (int i = 0; i < rawLines.size(); i++) {
            String rawLine = rawLines.get(i);
            System.out.println("[" + (i + 1) + "] " + rawLine);
        }
        
        System.out.println("========================================");
        System.out.println();
        
        // Log item info - Clean version
        System.out.println("========================================");
        System.out.println("[ItemHoverLogger] Item Tooltip (CLEAN - ohne Formatierung):");
        System.out.println("========================================");
        System.out.println("Anzahl Zeilen: " + cleanLines.size());
        System.out.println("----------------------------------------");
        
        for (int i = 0; i < cleanLines.size(); i++) {
            String cleanLine = cleanLines.get(i);
            System.out.println("[" + (i + 1) + "] " + cleanLine);
        }
        
        System.out.println("========================================");
        
        // Also send a message to the player
        if (client.player != null) {
            client.player.sendMessage(Text.literal("§a[ItemHoverLogger] §fTooltip wurde in die Konsole geloggt!"), false);
        }
    }
    
    /**
     * Updates the stored mouse position (called from mixin)
     */
    public static void updateMousePosition(double mouseX, double mouseY) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;
    }
    
    /**
     * Gets the currently hovered item
     * Works both in inventory screens and in the world
     * Uses ItemTooltipCallback as primary method for reliability
     */
    private static ItemStack getHoveredItem(MinecraftClient client) {
        // First, try to use the item from ItemTooltipCallback (most reliable)
        long currentTime = System.currentTimeMillis();
        if (lastHoveredItem != null && !lastHoveredItem.isEmpty()) {
            long timeSinceLastHover = currentTime - lastHoveredItemTime;
            if (timeSinceLastHover < HOVER_ITEM_TIMEOUT_MS) {
                // Item was hovered recently, use it
                return lastHoveredItem;
            }
        }
        
        // Fallback: Try to find item using mouse position (for cases where tooltip wasn't shown yet)
        if (client.currentScreen != null && client.currentScreen instanceof HandledScreen) {
            HandledScreen<?> screen = (HandledScreen<?>) client.currentScreen;
            
            // Get mouse position - use stored position if available, otherwise calculate
            double mouseX;
            double mouseY;
            if (lastMouseX >= 0 && lastMouseY >= 0) {
                mouseX = lastMouseX;
                mouseY = lastMouseY;
            } else {
                // Fallback: calculate from window mouse position
                mouseX = client.mouse.getX() * client.getWindow().getScaledWidth() / client.getWindow().getWidth();
                mouseY = client.mouse.getY() * client.getWindow().getScaledHeight() / client.getWindow().getHeight();
            }
            
            // Try to get screen position using reflection (similar to HandledScreenMixin)
            int screenX = 0;
            int screenY = 0;
            
            try {
                java.lang.reflect.Field xField = HandledScreen.class.getDeclaredField("x");
                java.lang.reflect.Field yField = HandledScreen.class.getDeclaredField("y");
                xField.setAccessible(true);
                yField.setAccessible(true);
                screenX = xField.getInt(screen);
                screenY = yField.getInt(screen);
            } catch (Exception e) {
                // If reflection fails, try alternative approach
                try {
                    java.lang.reflect.Field[] fields = HandledScreen.class.getDeclaredFields();
                    for (java.lang.reflect.Field field : fields) {
                        if (field.getType() == int.class) {
                            field.setAccessible(true);
                            int value = field.getInt(screen);
                            // Heuristic: x and y are usually small positive values
                            if (value > 0 && value < 1000) {
                                if (screenX == 0) {
                                    screenX = value;
                                } else if (screenY == 0) {
                                    screenY = value;
                                    break;
                                }
                            }
                        }
                    }
                } catch (Exception e2) {
                    // Ignore
                }
            }
            
            // Find the hovered slot - Minecraft slots are 18x18 pixels
            if (screenX > 0 || screenY > 0) {
                for (Slot slot : screen.getScreenHandler().slots) {
                    // Check if mouse is over this slot (slots are 18x18 pixels)
                    int slotLeft = slot.x + screenX;
                    int slotRight = slotLeft + 18;
                    int slotTop = slot.y + screenY;
                    int slotBottom = slotTop + 18;
                    
                    if (slotLeft <= mouseX && mouseX < slotRight &&
                        slotTop <= mouseY && mouseY < slotBottom) {
                        if (slot.hasStack()) {
                            return slot.getStack();
                        }
                    }
                }
            }
        }
        
        // Last fallback: check the item in the player's hand (main hand)
        // Only use this if we're not in an inventory, to avoid logging wrong items
        if (client.currentScreen == null && client.player != null) {
            ItemStack mainHandItem = client.player.getMainHandStack();
            if (mainHandItem != null && !mainHandItem.isEmpty()) {
                return mainHandItem;
            }
            
            // If main hand is empty, check off hand
            ItemStack offHandItem = client.player.getOffHandStack();
            if (offHandItem != null && !offHandItem.isEmpty()) {
                return offHandItem;
            }
        }
        
        return ItemStack.EMPTY;
    }
    
    /**
     * Gets the tooltip for an item stack
     */
    private static List<Text> getItemTooltip(ItemStack itemStack, PlayerEntity player) {
        List<Text> tooltip = new ArrayList<>();
        
        // Add item name
        tooltip.add(itemStack.getName());
        
        // Read lore from Data Component API (1.21.7)
        var loreComponent = itemStack.get(DataComponentTypes.LORE);
        if (loreComponent != null) {
            tooltip.addAll(loreComponent.lines());
        }
        
        return tooltip;
    }
    
    /**
     * Extracts text with all formatting codes from a Text object
     * Uses OrderedText to preserve all formatting codes
     */
    private static String getTextWithFormatting(Text text) {
        if (text == null) {
            return "";
        }
        
        // First check if getString() already contains formatting codes
        String stringValue = text.getString();
        if (stringValue != null && stringValue.contains("§")) {
            // getString() already contains formatting codes, use it
            return stringValue;
        }
        
        // Otherwise, extract formatting from style using OrderedText
        StringBuilder result = new StringBuilder();
        net.minecraft.text.OrderedText orderedText = text.asOrderedText();
        
        final net.minecraft.text.Style[] lastStyle = {null};
        
        // Process each character with its style
        orderedText.accept((index, style, codePoint) -> {
            // Check if style changed
            if (lastStyle[0] == null || !lastStyle[0].equals(style)) {
                // Style changed, apply formatting codes
                if (style != null) {
                    // Add color code if present
                    if (style.getColor() != null) {
                        net.minecraft.text.TextColor textColor = style.getColor();
                        try {
                            // Try to get as Formatting first (named colors)
                            String colorName = textColor.getName();
                            if (colorName != null) {
                                net.minecraft.util.Formatting formatting = net.minecraft.util.Formatting.byName(colorName);
                                if (formatting != null) {
                                    result.append("§").append(formatting.getCode());
                                } else {
                                    // Try to get RGB color
                                    Integer rgb = textColor.getRgb();
                                    if (rgb != null) {
                                        // Use hex color format §#RRGGBB
                                        String hex = String.format("#%06X", rgb & 0xFFFFFF);
                                        result.append("§").append(hex);
                                    }
                                }
                            } else {
                                // Try to get RGB color directly
                                Integer rgb = textColor.getRgb();
                                if (rgb != null) {
                                    String hex = String.format("#%06X", rgb & 0xFFFFFF);
                                    result.append("§").append(hex);
                                }
                            }
                        } catch (Exception e) {
                            // Ignore
                        }
                    }
                    
                    // Add formatting codes (bold, italic, etc.)
                    if (style.isBold()) result.append("§l");
                    if (style.isItalic()) result.append("§o");
                    if (style.isStrikethrough()) result.append("§m");
                    if (style.isUnderlined()) result.append("§n");
                    if (style.isObfuscated()) result.append("§k");
                }
                lastStyle[0] = style;
            }
            
            // Append the character
            result.appendCodePoint(codePoint);
            
            return true;
        });
        
        return result.toString();
    }
    
    /**
     * Logs the name of the currently open inventory
     */
    private static void logInventoryName(MinecraftClient client) {
        // Check if debug functions are enabled
        if (!CCLiveUtilitiesConfig.HANDLER.instance().debugFunctionsEnabled) {
            return;
        }
        
        if (client == null || client.currentScreen == null) {
            System.out.println("[InventoryNameLogger] Kein Inventar geöffnet!");
            if (client != null && client.player != null) {
                client.player.sendMessage(Text.literal("§c[InventoryNameLogger] §fKein Inventar geöffnet!"), false);
            }
            return;
        }
        
        // Check if it's a HandledScreen (inventory-like screen)
        if (!(client.currentScreen instanceof HandledScreen)) {
            String screenType = client.currentScreen.getClass().getSimpleName();
            System.out.println("[InventoryNameLogger] Aktueller Screen ist kein Inventar: " + screenType);
            if (client.player != null) {
                client.player.sendMessage(Text.literal("§c[InventoryNameLogger] §fAktueller Screen ist kein Inventar: §7" + screenType), false);
            }
            return;
        }
        
        HandledScreen<?> screen = (HandledScreen<?>) client.currentScreen;
        Text titleText = screen.getTitle();
        
        if (titleText == null) {
            System.out.println("[InventoryNameLogger] Kein Titel gefunden!");
            if (client.player != null) {
                client.player.sendMessage(Text.literal("§c[InventoryNameLogger] §fKein Titel gefunden!"), false);
            }
            return;
        }
        
        // Get title as string (with formatting codes)
        String titleWithFormatting = titleText.getString();
        
        // Get clean title (without formatting codes, but keep Chinese characters)
        String cleanTitle = titleWithFormatting
            .replaceAll("§[0-9a-fk-or]", "")  // Remove standard formatting codes
            .replaceAll("§#[0-9a-fA-F]{6}", "")  // Remove hex color codes like §#FF8000
            .replaceAll("§x(§[0-9a-fA-F]){6}", "")  // Remove hex color codes like §x§f§f§8§0§0§0
            .trim();
        
        // Log inventory name - Raw version first
        System.out.println("========================================");
        System.out.println("[InventoryNameLogger] Inventar Name (RAW - mit Formatierung):");
        System.out.println("========================================");
        System.out.println("Titel: " + titleWithFormatting);
        System.out.println("========================================");
        System.out.println();
        
        // Log inventory name - Clean version
        System.out.println("========================================");
        System.out.println("[InventoryNameLogger] Inventar Name (CLEAN - ohne Formatierung):");
        System.out.println("========================================");
        System.out.println("Titel: " + cleanTitle);
        System.out.println("========================================");
        
        // Also send a message to the player
        if (client.player != null) {
            client.player.sendMessage(Text.literal("§a[InventoryNameLogger] §fInventarname wurde in die Konsole geloggt!"), false);
            client.player.sendMessage(Text.literal("§7Original: §f" + titleWithFormatting), false);
            client.player.sendMessage(Text.literal("§7Bereinigt: §f" + cleanTitle), false);
        }
    }
    
    /**
     * Logs all currently active bossbars
     */
    private static void logBossBars(MinecraftClient client) {
        // Check if debug functions are enabled
        if (!CCLiveUtilitiesConfig.HANDLER.instance().debugFunctionsEnabled) {
            return;
        }
        
        if (client == null || client.inGameHud == null) {
            System.out.println("[BossBarLogger] Kein Client oder InGameHud gefunden!");
            if (client != null && client.player != null) {
                client.player.sendMessage(Text.literal("§c[BossBarLogger] §fKein Client oder InGameHud gefunden!"), false);
            }
            return;
        }
        
        BossBarHud bossBarHud = client.inGameHud.getBossBarHud();
        if (bossBarHud == null) {
            System.out.println("[BossBarLogger] Kein BossBarHud gefunden!");
            if (client.player != null) {
                client.player.sendMessage(Text.literal("§c[BossBarLogger] §fKein BossBarHud gefunden!"), false);
            }
            return;
        }
        
        // Get bossbars using the same method as BossBarMixin
        Map<UUID, ClientBossBar> bossBars = null;
        String[] possibleFieldNames = {
            "field_2060", "field_2061", "field_2062", // Common obfuscated field names
            "bossBars", "bossbars", "bossBars", "bars", "bossBarMap",
            "clientBossBars", "bossBarEntries", "entries", "bossBarList"
        };
        
        // First try to find the field by type
        java.lang.reflect.Field[] fields = bossBarHud.getClass().getDeclaredFields();
        for (java.lang.reflect.Field field : fields) {
            if (Map.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    Object fieldValue = field.get(bossBarHud);
                    if (fieldValue instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<UUID, ClientBossBar> tempBars = (Map<UUID, ClientBossBar>) fieldValue;
                        if (tempBars != null) {
                            bossBars = tempBars;
                            break;
                        }
                    }
                } catch (Exception e) {
                    // Continue trying other fields
                }
            }
        }
        
        // If we didn't find it by type, try the known field names
        if (bossBars == null) {
            for (String fieldName : possibleFieldNames) {
                try {
                    java.lang.reflect.Field bossBarsField = bossBarHud.getClass().getDeclaredField(fieldName);
                    bossBarsField.setAccessible(true);
                    Object fieldValue = bossBarsField.get(bossBarHud);
                    
                    if (fieldValue instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<UUID, ClientBossBar> tempBars = (Map<UUID, ClientBossBar>) fieldValue;
                        if (tempBars != null) {
                            bossBars = tempBars;
                            break;
                        }
                    }
                } catch (Exception e) {
                    // Silent error handling
                }
            }
        }
        
        if (bossBars == null || bossBars.isEmpty()) {
            System.out.println("[BossBarLogger] Keine Bossbars gefunden!");
            if (client.player != null) {
                client.player.sendMessage(Text.literal("§e[BossBarLogger] §fKeine Bossbars gefunden!"), false);
            }
            return;
        }
        
        // Prepare data for both versions
        List<BossBarData> bossBarDataList = new ArrayList<>();
        for (Map.Entry<UUID, ClientBossBar> entry : bossBars.entrySet()) {
            UUID uuid = entry.getKey();
            ClientBossBar bossBar = entry.getValue();
            String name = bossBar.getName().getString();
            String cleanName = name.replaceAll("§[0-9a-fk-or]", "");
            float percent = bossBar.getPercent();
            int color = bossBar.getColor().ordinal();
            int style = bossBar.getStyle().ordinal();
            bossBarDataList.add(new BossBarData(uuid, name, cleanName, percent, color, style));
        }
        
        // Log all bossbars - Raw version first
        System.out.println("========================================");
        System.out.println("[BossBarLogger] Aktive Bossbars (RAW - mit Formatierung) (" + bossBars.size() + "):");
        System.out.println("========================================");
        
        for (int i = 0; i < bossBarDataList.size(); i++) {
            BossBarData data = bossBarDataList.get(i);
            System.out.println("--- Bossbar #" + (i + 1) + " ---");
            System.out.println("UUID: " + data.uuid.toString());
            System.out.println("Name: " + data.name);
            System.out.println("Prozent: " + (data.percent * 100) + "%");
            System.out.println("Farbe (Index): " + data.color);
            System.out.println("Stil (Index): " + data.style);
            System.out.println();
        }
        
        System.out.println("========================================");
        System.out.println();
        
        // Log all bossbars - Clean version
        System.out.println("========================================");
        System.out.println("[BossBarLogger] Aktive Bossbars (CLEAN - ohne Formatierung) (" + bossBars.size() + "):");
        System.out.println("========================================");
        
        for (int i = 0; i < bossBarDataList.size(); i++) {
            BossBarData data = bossBarDataList.get(i);
            System.out.println("--- Bossbar #" + (i + 1) + " ---");
            System.out.println("UUID: " + data.uuid.toString());
            System.out.println("Name: " + data.cleanName);
            System.out.println("Prozent: " + (data.percent * 100) + "%");
            System.out.println("Farbe (Index): " + data.color);
            System.out.println("Stil (Index): " + data.style);
            System.out.println();
        }
        
        System.out.println("========================================");
        
        // Also send a message to the player
        if (client.player != null) {
            client.player.sendMessage(Text.literal("§a[BossBarLogger] §f" + bossBars.size() + " Bossbar(s) wurden in die Konsole geloggt!"), false);
        }
    }
    
    /**
     * Logs the current scoreboard (sidebar on the right)
     */
    private static void logScoreboard(MinecraftClient client) {
        // Check if debug functions are enabled
        if (!CCLiveUtilitiesConfig.HANDLER.instance().debugFunctionsEnabled) {
            return;
        }
        
        if (client == null || client.world == null) {
            System.out.println("[ScoreboardLogger] Kein Client oder World gefunden!");
            if (client != null && client.player != null) {
                client.player.sendMessage(Text.literal("§c[ScoreboardLogger] §fKein Client oder World gefunden!"), false);
            }
            return;
        }
        
        Scoreboard scoreboard = client.world.getScoreboard();
        if (scoreboard == null) {
            System.out.println("[ScoreboardLogger] Kein Scoreboard gefunden!");
            if (client.player != null) {
                client.player.sendMessage(Text.literal("§c[ScoreboardLogger] §fKein Scoreboard gefunden!"), false);
            }
            return;
        }
        
        // Get sidebar objective
        ScoreboardObjective sidebarObjective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (sidebarObjective == null) {
            System.out.println("[ScoreboardLogger] Kein Sidebar-Objektiv gefunden!");
            if (client.player != null) {
                client.player.sendMessage(Text.literal("§c[ScoreboardLogger] §fKein Sidebar-Objektiv gefunden!"), false);
            }
            return;
        }
        
        // Get title
        String title = sidebarObjective.getDisplayName().getString();
        String cleanTitle = title.replaceAll("§[0-9a-fk-or]", "").trim();
        
        // Read scoreboard lines
        List<String> lines = readScoreboardLines(scoreboard, sidebarObjective);
        
        if (lines.isEmpty()) {
            System.out.println("[ScoreboardLogger] Scoreboard ist leer!");
            if (client.player != null) {
                client.player.sendMessage(Text.literal("§e[ScoreboardLogger] §fScoreboard ist leer!"), false);
            }
            return;
        }
        
        // Create clean version (without formatting)
        List<String> cleanLines = new ArrayList<>();
        for (String line : lines) {
            String cleanLine = line.replaceAll("§[0-9a-fk-or]", "").trim();
            cleanLines.add(cleanLine);
        }
        
        // Log scoreboard - Raw version first
        System.out.println("========================================");
        System.out.println("[ScoreboardLogger] Scoreboard Inhalt (RAW - mit Formatierung):");
        System.out.println("========================================");
        System.out.println("Titel: " + title);
        System.out.println("Anzahl Zeilen: " + lines.size());
        System.out.println("----------------------------------------");
        
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            System.out.println("[" + (i + 1) + "] " + line);
        }
        
        System.out.println("========================================");
        System.out.println();
        
        // Log scoreboard - Clean version
        System.out.println("========================================");
        System.out.println("[ScoreboardLogger] Scoreboard Inhalt (CLEAN - ohne Formatierung):");
        System.out.println("========================================");
        System.out.println("Titel: " + cleanTitle);
        System.out.println("Anzahl Zeilen: " + cleanLines.size());
        System.out.println("----------------------------------------");
        
        for (int i = 0; i < cleanLines.size(); i++) {
            String cleanLine = cleanLines.get(i);
            System.out.println("[" + (i + 1) + "] " + cleanLine);
        }
        
        System.out.println("========================================");
        
        // Also send a message to the player
        if (client.player != null) {
            client.player.sendMessage(Text.literal("§a[ScoreboardLogger] §f" + lines.size() + " Zeile(n) wurden in die Konsole geloggt!"), false);
        }
    }
    
    /**
     * Reads all scoreboard lines from sidebar in order (top to bottom)
     * Uses the official 1.21.x API: getScoreboardEntries(ScoreboardObjective)
     * Based on the implementation in InformationenUtility.java
     */
    private static List<String> readScoreboardLines(Scoreboard scoreboard, ScoreboardObjective sidebarObjective) {
        List<String> lines = new ArrayList<>();
        
        try {
            // Use official 1.21.x API: getScoreboardEntries(ScoreboardObjective)
            Collection<ScoreboardEntry> rawEntries = scoreboard.getScoreboardEntries(sidebarObjective);
            
            if (rawEntries == null || rawEntries.isEmpty()) {
                return lines;
            }
            
            // Filter hidden entries and sort like in HUD
            List<ScoreboardEntry> filteredEntries = rawEntries.stream()
                    .filter(e -> !e.hidden())
                    .toList();
            
            // Try to sort with InGameHud.SCOREBOARD_ENTRY_COMPARATOR (via reflection if needed)
            List<ScoreboardEntry> entries;
            try {
                java.lang.reflect.Field comparatorField = InGameHud.class.getField("SCOREBOARD_ENTRY_COMPARATOR");
                @SuppressWarnings("unchecked")
                java.util.Comparator<ScoreboardEntry> comparator = (java.util.Comparator<ScoreboardEntry>) comparatorField.get(null);
                entries = filteredEntries.stream()
                        .sorted(comparator)
                        .toList();
            } catch (Exception e) {
                // Fallback: sort by score value (descending)
                entries = filteredEntries.stream()
                        .sorted((a, b) -> Integer.compare(b.value(), a.value()))
                        .toList();
            }
            
            // Extract text from each entry (same logic as InformationenUtility.java)
            for (int i = 0; i < entries.size(); i++) {
                ScoreboardEntry entry = entries.get(i);
                
                String owner = entry.owner();
                
                // Get the team for this owner using getScoreHolderTeam
                Team team = null;
                try {
                    java.lang.reflect.Method getScoreHolderTeamMethod = scoreboard.getClass().getMethod("getScoreHolderTeam", String.class);
                    team = (Team) getScoreHolderTeamMethod.invoke(scoreboard, owner);
                } catch (Exception e) {
                    // Try alternative method names
                    try {
                        java.lang.reflect.Method method = scoreboard.getClass().getMethod("method_1164", String.class);
                        team = (Team) method.invoke(scoreboard, owner);
                    } catch (Exception e2) {
                        // Ignore
                    }
                }
                
                // WICHTIG: sichtbarer Text kommt aus entry.name()
                // Aber wenn entry.name() nur den Owner zurückgibt, müssen wir Team.decorateName() manuell verwenden
                Text lineText = entry.name();
                String nameString = lineText != null ? lineText.getString() : "";
                
                // Prüfe ob entry.name() nur den Owner zurückgibt (dann ist Team.decorateName() nötig)
                if (nameString.equals(owner)) {
                    // entry.name() hat nur den Owner zurückgegeben - versuche Teams zu finden
                    
                    // Versuche alle Teams im Scoreboard zu durchsuchen
                    try {
                        java.lang.reflect.Field[] fields = scoreboard.getClass().getDeclaredFields();
                        for (java.lang.reflect.Field field : fields) {
                            if (java.util.Map.class.isAssignableFrom(field.getType())) {
                                field.setAccessible(true);
                                Object fieldValue = field.get(scoreboard);
                                if (fieldValue instanceof java.util.Map) {
                                    @SuppressWarnings("unchecked")
                                    java.util.Map<?, ?> map = (java.util.Map<?, ?>) fieldValue;
                                    // Prüfe ob dies die Teams-Map ist (Map<String, Team>)
                                    if (!map.isEmpty()) {
                                        Object firstKey = map.keySet().iterator().next();
                                        Object firstValue = map.get(firstKey);
                                        if (firstKey instanceof String && firstValue instanceof Team) {
                                            @SuppressWarnings("unchecked")
                                            java.util.Map<String, Team> teamsMap = (java.util.Map<String, Team>) map;
                                            
                                            // Suche nach einem Team, das diesen Owner enthält
                                            for (java.util.Map.Entry<String, Team> teamEntry : teamsMap.entrySet()) {
                                                Team t = teamEntry.getValue();
                                                if (t != null) {
                                                    // Prüfe ob dieses Team den Owner als Mitglied hat
                                                    try {
                                                        java.lang.reflect.Method getMembersMethod = t.getClass().getMethod("getMembers");
                                                        java.util.Collection<?> members = (java.util.Collection<?>) getMembersMethod.invoke(t);
                                                        if (members != null && members.contains(owner)) {
                                                            team = t;
                                                            break;
                                                        }
                                                    } catch (Exception ex) {
                                                        // Ignoriere Fehler
                                                    }
                                                }
                                            }
                                            
                                            // Wenn kein Team über Members gefunden wurde, versuche über Team-Name zu suchen
                                            if (team == null) {
                                                // Versuche Team-Name, der dem Owner entspricht (z.B. Team "§e" für Owner "§e")
                                                Team possibleTeam = teamsMap.get(owner);
                                                if (possibleTeam != null) {
                                                    team = possibleTeam;
                                                }
                                            }
                                            
                                            if (team != null) {
                                                Text base = Text.literal(owner);
                                                lineText = Team.decorateName(team, base);
                                            }
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        // Silent error handling
                    }
                }
                
                if (lineText != null) {
                    String rawText = lineText.getString();   // z.B. "Biom", "-> Kohle", "[Kohle Mine]"
                    
                    // Add raw text (with formatting)
                    lines.add(rawText);
                }
            }
        } catch (Exception e) {
            // Silent error handling
        }
        
        return lines;
    }
    
    /**
     * Handles key press in inventory screens
     * Called from SearchBarInputMixin
     */
    public static boolean handleItemLoggerKeyPress(int keyCode) {
        // Only handle keys if debug functions are enabled
        if (!CCLiveUtilitiesConfig.HANDLER.instance().debugFunctionsEnabled) {
            return false;
        }
        
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null) {
                return false;
            }
            
            // Check hardcoded keys: F8, F9, F10, F12
            if (keyCode == GLFW.GLFW_KEY_F8) {
                logHoveredItemInfo(client);
                return true;
            }
            
            if (keyCode == GLFW.GLFW_KEY_F9) {
                logInventoryName(client);
                return true;
            }
            
            if (keyCode == GLFW.GLFW_KEY_F10) {
                logScoreboard(client);
                return true;
            }
            
            if (keyCode == GLFW.GLFW_KEY_F12) {
                logBossBars(client);
                return true;
            }
        } catch (Exception e) {
            // Silent error handling
        }
        
        return false;
    }
    
    /**
     * Helper class to store bossbar data
     */
    private static class BossBarData {
        UUID uuid;
        String name;
        String cleanName;
        float percent;
        int color;
        int style;
        
        BossBarData(UUID uuid, String name, String cleanName, float percent, int color, int style) {
            this.uuid = uuid;
            this.name = name;
            this.cleanName = cleanName;
            this.percent = percent;
            this.color = color;
            this.style = style;
        }
    }
}
