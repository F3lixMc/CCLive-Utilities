package net.felix.mixin;

import net.felix.utilities.Other.PlayericonUtility.PlayerIconUtility;
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

/**
 * Mixin to add the CCLive-Utilities icon next to player names in chat messages.
 */
@Mixin(ChatHud.class)
public abstract class ChatHudRenderMixin {
    
    @Shadow(remap = true)
    private List<ChatHudLine.Visible> visibleMessages;
    
    @Shadow(remap = true)
    private List<ChatHudLine> messages;
    
    // Pattern to match player names in chat (e.g., "<PlayerName> message" or "PlayerName: message")
    // Also matches common chat formats like "[Server] PlayerName: message" or "PlayerName > message"
    private static final Pattern PLAYER_NAME_PATTERN = Pattern.compile(
        "<([^>]+)>|" +  // <PlayerName> format
        "\\[([^\\]]+)\\]|" +  // [PlayerName] format
        "^([^:<>\\[\\]]+?):|" +  // PlayerName: format (non-greedy)
        "^([^:<>\\[\\]]+?)\\s+>"  // PlayerName > format
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
                    
                    // Calculate position for icon
                    // Chat messages are rendered from bottom to top
                    // visibleMessages are ordered from newest (index 0) at bottom to oldest at top
                    int reversedIndex = visibleMessages.size() - 1 - i;
                    int lineY = chatBottom - (reversedIndex * chatLineHeight) - chatLineHeight;
                    
                    // Make sure the icon is within screen bounds
                    if (lineY < 0 || lineY > screenHeight) {
                        continue;
                    }
                    
                    // Render icon to the left of the chat area
                    // Position it slightly above the text line for better alignment
                    int iconX = chatLeft;
                    int iconY = lineY - 2; // Slight offset for alignment
                    
                    // Render the icon
                    PlayerIconUtility.renderIcon(context, iconX, iconY, iconSize);
                } catch (Exception e) {
                    // Silently skip this message if there's an error
                    continue;
                }
            }
        } catch (Exception e) {
            // Silently fail if there's any error
        }
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

