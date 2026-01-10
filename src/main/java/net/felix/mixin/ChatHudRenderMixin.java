package net.felix.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.felix.utilities.Other.PlayericonUtility.PlayerIconUtility;

/**
 * Mixin for ChatHud rendering functionality.
 */
@Mixin(ChatHud.class)
public abstract class ChatHudRenderMixin {
    
    @Shadow(remap = true)
    private List<ChatHudLine.Visible> visibleMessages;
    
    @Shadow(remap = true)
    private List<ChatHudLine> messages;
    
    // Pattern to match player names in chat messages (before >>)
    private static final Pattern PLAYER_NAME_PATTERN = Pattern.compile(
        "([A-Za-z0-9_]{2,20})\\s*>>",  // Player name followed by >>
        Pattern.CASE_INSENSITIVE
    );
    
    @Inject(
        method = "render",
        at = @At("TAIL"),
        cancellable = false
    )
    private void renderIconsInChat(
        DrawContext context,
        int currentTick,
        int mouseX,
        int mouseY,
        boolean bl,
        CallbackInfo ci
    ) {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.textRenderer == null || client.getWindow() == null) {
                return;
            }
            
            // Prüfe ob Chat-Icon aktiviert ist
            if (!net.felix.CCLiveUtilitiesConfig.HANDLER.instance().chatIconEnabled) {
                return;
            }
            
            // Don't render if world is not loaded (prevents issues during startup)
            if (client.world == null) {
                return;
            }
            
            // Safely access visibleMessages
            if (visibleMessages == null || visibleMessages.isEmpty()) {
                return;
            }
            
            int screenHeight = client.getWindow().getScaledHeight();
            int chatLineHeight = client.textRenderer.fontHeight;
            int chatBottom = screenHeight - 40;
            int chatLeft = 2; // Left margin of chat
            int iconSize = 6; // Smaller icon for chat
            
            // Render icons for each visible chat message
            // For testing: render icon for ALL visible messages to verify positioning works
            for (int i = 0; i < visibleMessages.size() && i < 100; i++) { // Limit to 100 messages
                try {
                    ChatHudLine.Visible visibleLine = visibleMessages.get(i);
                    if (visibleLine == null) {
                        continue;
                    }
                    
                    // Get the original message text from the messages list
                    // visibleMessages and messages are aligned by index
                    String messageString = null;
                    if (messages != null && i < messages.size()) {
                        ChatHudLine originalLine = messages.get(i);
                        if (originalLine != null && originalLine.content() != null) {
                            messageString = originalLine.content().getString();
                        }
                    }
                    
                    // Fallback: try to extract from OrderedText if messages list is not available
                    if (messageString == null || messageString.isEmpty()) {
                        // Use OrderedText visitor to build string
                        StringBuilder sb = new StringBuilder();
                        visibleLine.content().accept((index, style, codePoint) -> {
                            sb.appendCodePoint(codePoint);
                            return true;
                        });
                        messageString = sb.toString();
                    }
                    
                    if (messageString == null || messageString.isEmpty()) {
                        continue;
                    }
                    
                    // Check if this is a CCLive message (remove color codes first)
                    String cleanMessage = messageString.replaceAll("§[0-9a-fk-or]", "");
                    boolean isCCLiveMessage = cleanMessage.contains("[CCLive]");
                    
                    if (isCCLiveMessage) {
                        // For CCLive messages, render icon directly after "[CCLive]"
                        // Calculate position for icon
                        int reversedIndex = visibleMessages.size() - 1 - i;
                        int lineY = chatBottom - (reversedIndex * chatLineHeight) - chatLineHeight;
                        
                        // Make sure the icon is within screen bounds
                        if (lineY >= 0 && lineY <= screenHeight) {
                            // Calculate X position: after "[CCLive] " text
                            // Need to measure the actual rendered width of "[CCLive] " with color codes
                            // Create a Text object with the same style to measure accurately
                            net.minecraft.text.Text cclivePrefix = net.minecraft.text.Text.literal("[CCLive] ")
                                .setStyle(net.minecraft.text.Style.EMPTY.withColor(0xD478F0));
                            int cclivePrefixWidth = client.textRenderer.getWidth(cclivePrefix);
                            int iconX = chatLeft + cclivePrefixWidth;
                            int iconY = lineY - 1; // Slight offset for alignment (reduced from -2 to -1)
                            
                            // Render the icon with slightly larger size for better visibility
                            // renderIcon() will ensure the icon is loaded internally
                            PlayerIconUtility.renderIcon(context, iconX, iconY, iconSize + 2);
                        }
                        continue; // Skip normal icon rendering for CCLive messages
                    }
                    
                    String playerName = extractPlayerName(messageString);
                    if (playerName == null || playerName.isEmpty()) {
                        continue;
                    }
                    
                    // Find player UUID by name and check if they have the mod
                    UUID playerUuid = findPlayerUuidByName(playerName, client);
                    if (playerUuid == null) {
                        continue;
                    }
                    
                    // Only show icon for players who have the mod installed
                    boolean shouldShowIcon = PlayerIconUtility.hasMod(playerUuid);
                    
                    if (!shouldShowIcon) {
                        continue;
                    }
                    
                    // Calculate position for icon (zwischen Name und >>)
                    int reversedIndex = visibleMessages.size() - 1 - i;
                    int lineY = chatBottom - (reversedIndex * chatLineHeight) - chatLineHeight;
                    
                    // Make sure the icon is within screen bounds
                    if (lineY < 0 || lineY > screenHeight) {
                        continue;
                    }
                    
                    // HINWEIS: Das Resource Pack ersetzt die Glyphe (ѳ) automatisch durch das Icon
                    // Das separate Rendering hier ist nur noch ein Fallback, falls das Resource Pack nicht geladen wird
                    // Suche nach Icon-Marker im Text (wenn vorhanden)
                    int iconPosition = -1;
                    if (messages != null && i < messages.size()) {
                        ChatHudLine originalLine = messages.get(i);
                        if (originalLine != null && originalLine.content() != null) {
                            iconPosition = findIconMarkerPosition(originalLine.content(), client);
                        }
                    }
                    
                    if (iconPosition >= 0) {
                        // Icon-Marker gefunden - das Resource Pack sollte das Icon bereits rendern
                        // Falls nicht, rendere Icon als Fallback an dieser Position
                        // (Normalerweise sollte dies nicht nötig sein, da das Resource Pack aktiv ist)
                        int iconX = chatLeft + iconPosition;
                        int iconY = lineY - 1; // Slight offset for alignment
                        PlayerIconUtility.renderIcon(context, iconX, iconY, iconSize);
                    } else {
                        // Fallback: Finde Position des Namens und rendere Icon danach
                        int namePosition = -1;
                        if (messages != null && i < messages.size()) {
                            ChatHudLine originalLine = messages.get(i);
                            if (originalLine != null && originalLine.content() != null) {
                                namePosition = findPlayerNamePositionInText(originalLine.content(), playerName, client);
                            }
                        }
                        
                        if (namePosition < 0) {
                            namePosition = findPlayerNamePosition(messageString, playerName, client);
                        }
                        
                        if (namePosition >= 0) {
                            // Berechne Position nach dem Namen (mit Leerzeichen)
                            int nameWidth = client.textRenderer.getWidth(playerName);
                            int spaceWidth = client.textRenderer.getWidth(" ");
                            int iconX = chatLeft + namePosition + nameWidth + spaceWidth;
                            int iconY = lineY - 1;
                            PlayerIconUtility.renderIcon(context, iconX, iconY, iconSize);
                        } else {
                            // Fallback: Render icon to the left of the chat area
                            int iconX = chatLeft;
                            int iconY = lineY - 2;
                            PlayerIconUtility.renderIcon(context, iconX, iconY, iconSize);
                        }
                    }
                } catch (Exception e) {
                    // Silently skip this message if there's an error
                    continue;
                }
            }
        } catch (Exception e) {
            // Silently fail if there's any error
        }
    }
    
    // Glyphe als Marker für das Icon (Cyrillic letter fita - ѳ, muss mit PlayerHoverStatsUtility übereinstimmen)
    private static final char ICON_MARKER = 'ѳ';
    
    /**
     * Findet die X-Position des Icon-Markers im Text
     * @param messageText Die Chat-Nachricht als Text
     * @param client Minecraft Client
     * @return Die X-Position des Markers in Pixeln, oder -1 wenn nicht gefunden
     */
    private int findIconMarkerPosition(net.minecraft.text.Text messageText, MinecraftClient client) {
        if (messageText == null || client == null || client.textRenderer == null) {
            return -1;
        }
        
        try {
            // Prüfe zuerst den Haupttext
            String mainText = messageText.getString();
            if (mainText != null && mainText.indexOf(ICON_MARKER) >= 0) {
                int markerIndex = mainText.indexOf(ICON_MARKER);
                String textBeforeMarker = mainText.substring(0, markerIndex);
                return client.textRenderer.getWidth(textBeforeMarker);
            }
            
            // Durchsuche die Siblings rekursiv nach dem Icon-Marker
            int position = 0;
            List<net.minecraft.text.Text> siblings = messageText.getSiblings();
            
            if (siblings != null && !siblings.isEmpty()) {
                for (int idx = 0; idx < siblings.size(); idx++) {
                    net.minecraft.text.Text sibling = siblings.get(idx);
                    String siblingText = sibling.getString();
                    
                    if (siblingText != null && siblingText.indexOf(ICON_MARKER) >= 0) {
                        // Marker gefunden - berechne Position bis zum Marker
                        int markerIndex = siblingText.indexOf(ICON_MARKER);
                        String textBeforeMarker = siblingText.substring(0, markerIndex);
                        
                        // Berechne Breite des Textes vor dem Marker
                        int textWidth = client.textRenderer.getWidth(textBeforeMarker);
                        return position + textWidth;
                    }
                    
                    // Rekursiv in diesem Sibling suchen
                    int found = findIconMarkerPosition(sibling, client);
                    if (found >= 0) {
                        return position + found;
                    }
                    
                    // Addiere die Breite dieses Siblings zur Position
                    position += client.textRenderer.getWidth(sibling);
                }
            }
        } catch (Exception e) {
            // Silent error handling
        }
        
        return -1;
    }
    
    /**
     * Findet die X-Position des Spielernamens in der Text-Struktur
     * @param messageText Die Chat-Nachricht als Text
     * @param playerName Der Spielername
     * @param client Minecraft Client
     * @return Die X-Position des Namens in Pixeln, oder -1 wenn nicht gefunden
     */
    private int findPlayerNamePositionInText(net.minecraft.text.Text messageText, String playerName, MinecraftClient client) {
        if (messageText == null || playerName == null || playerName.isEmpty() || client == null || client.textRenderer == null) {
            return -1;
        }
        
        try {
            int position = 0;
            List<net.minecraft.text.Text> siblings = messageText.getSiblings();
            
            if (siblings != null && !siblings.isEmpty()) {
                for (int idx = 0; idx < siblings.size(); idx++) {
                    net.minecraft.text.Text sibling = siblings.get(idx);
                    String siblingText = sibling.getString();
                    
                    // Prüfe ob dieser Sibling den Namen enthält
                    if (siblingText != null && siblingText.contains(playerName)) {
                        // Finde die Position des Namens innerhalb dieses Siblings
                        int nameIndex = siblingText.indexOf(playerName);
                        if (nameIndex >= 0) {
                            // Prüfe ob nach dem Namen ">>" kommt (in diesem oder einem nachfolgenden Sibling)
                            String textAfterName = siblingText.substring(nameIndex + playerName.length());
                            boolean hasArrowInSibling = textAfterName.contains(">>");
                            boolean hasArrowInNext = idx + 1 < siblings.size() && 
                                siblings.get(idx + 1).getString() != null && 
                                siblings.get(idx + 1).getString().contains(">>");
                            
                            if (hasArrowInSibling || hasArrowInNext) {
                                // Berechne Position bis zum Namen
                                String textBeforeName = siblingText.substring(0, nameIndex);
                                return position + client.textRenderer.getWidth(textBeforeName);
                            }
                        }
                    }
                    
                    // Rekursiv in diesem Sibling suchen
                    int found = findPlayerNamePositionInText(sibling, playerName, client);
                    if (found >= 0) {
                        return position + found;
                    }
                    
                    // Addiere die Breite dieses Siblings zur Position
                    position += client.textRenderer.getWidth(sibling);
                }
            }
            
            // Prüfe auch den Haupttext
            String mainText = messageText.getString();
            if (mainText != null && mainText.contains(playerName)) {
                int nameIndex = mainText.indexOf(playerName);
                if (nameIndex >= 0 && mainText.indexOf(">>", nameIndex) >= 0) {
                    String textBeforeName = mainText.substring(0, nameIndex);
                    return client.textRenderer.getWidth(textBeforeName);
                }
            }
        } catch (Exception e) {
            // Silent error handling
        }
        
        return -1;
    }
    
    /**
     * Findet die X-Position des Spielernamens in der Nachricht
     * @param messageString Die Chat-Nachricht als String
     * @param playerName Der Spielername
     * @param client Minecraft Client
     * @return Die X-Position des Namens in Pixeln, oder -1 wenn nicht gefunden
     */
    private int findPlayerNamePosition(String messageString, String playerName, MinecraftClient client) {
        if (messageString == null || playerName == null || playerName.isEmpty() || client == null || client.textRenderer == null) {
            return -1;
        }
        
        try {
            // Entferne Farbcodes für die Suche
            String cleanMessage = messageString.replaceAll("§[0-9a-fk-or]", "");
            
            // Suche nach dem Namen vor ">>"
            int nameIndex = cleanMessage.indexOf(playerName);
            if (nameIndex < 0) {
                return -1;
            }
            
            // Prüfe ob nach dem Namen ">>" kommt (mit optionalem Leerzeichen/Icon-Marker dazwischen)
            int arrowIndex = cleanMessage.indexOf(">>", nameIndex);
            if (arrowIndex < 0) {
                return -1;
            }
            
            // Berechne die Position des Namens
            // Wir müssen die Breite des Textes vor dem Namen berechnen
            // Da Farbcodes die Breite nicht ändern, können wir den cleanMessage verwenden
            String textBeforeName = cleanMessage.substring(0, nameIndex);
            
            // Aber für die genaue Breite müssen wir die Original-Nachricht mit Farbcodes verwenden
            // Vereinfachter Ansatz: Berechne Breite basierend auf cleanMessage
            // (Farbcodes ändern die Breite nicht, nur die Darstellung)
            return client.textRenderer.getWidth(textBeforeName);
        } catch (Exception e) {
            // Silent error handling
        }
        
        return -1;
    }
    
    /**
     * Extract player name from chat message text.
     */
    private String extractPlayerName(String messageText) {
        if (messageText == null || messageText.isEmpty()) {
            return null;
        }
        
        // Remove color codes first
        String cleanText = messageText.replaceAll("§[0-9a-fk-or]", "");
        
        Matcher matcher = PLAYER_NAME_PATTERN.matcher(cleanText);
        if (matcher.find()) {
            // Try all groups
            for (int i = 1; i <= matcher.groupCount(); i++) {
                String group = matcher.group(i);
                if (group != null && !group.trim().isEmpty()) {
                    String name = group.trim();
                    // Filter out common non-player prefixes
                    if (!name.equalsIgnoreCase("Server") && 
                        !name.equalsIgnoreCase("System") &&
                        !name.equalsIgnoreCase("Console") &&
                        name.length() > 0 && name.length() < 20) { // Reasonable player name length
                        return name;
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * Find player UUID by name from the player list.
     */
    private UUID findPlayerUuidByName(String playerName, MinecraftClient client) {
        try {
            if (client == null || client.getNetworkHandler() == null) {
                return null;
            }
            
            var playerList = client.getNetworkHandler().getPlayerList();
            if (playerList == null) {
                return null;
            }
            
            for (var entry : playerList) {
                if (entry == null || entry.getProfile() == null) {
                    continue;
                }
                
                String entryName = entry.getProfile().getName();
                if (entryName != null && entryName.equalsIgnoreCase(playerName)) {
                    return entry.getProfile().getId();
                }
            }
        } catch (Exception e) {
            // Silently fail
        }
        
        return null;
    }
}

