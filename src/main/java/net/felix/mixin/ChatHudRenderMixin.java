package net.felix.mixin;

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
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.util.Mth;

/**
 * Fallback-Icon-Rendering im Chat, falls keine Icon-Glyphe (ѳ) in der Nachricht steht.
 * Primär rendert die Custom-Font das Icon bereits an der richtigen Stelle.
 */
@Mixin(ChatComponent.class)
public abstract class ChatHudRenderMixin {
    
    @Shadow(remap = true)
    private List<GuiMessage.Line> trimmedMessages;
    
    @Shadow(remap = true)
    private List<GuiMessage> allMessages;

    @Shadow
    private int chatScrollbarPos;

    @Shadow
    public abstract int getLinesPerPage();
    
    private static final Pattern PLAYER_NAME_PATTERN = Pattern.compile(
        "([A-Za-z0-9_]{2,20})\\s*>>",
        Pattern.CASE_INSENSITIVE
    );

    private static final char ICON_MARKER = 'ѳ';
    
    @Inject(
        method = "extractRenderState",
        at = @At("TAIL"),
        cancellable = false
    )
    private void renderIconsInChat(
        GuiGraphicsExtractor context,
        Font font,
        int ticks,
        int mouseX,
        int mouseY,
        ChatComponent.DisplayMode displayMode,
        boolean changeCursorOnInsertions,
        CallbackInfo ci
    ) {
        try {
            Minecraft client = Minecraft.getInstance();
            if (client == null || client.font == null) {
                return;
            }
            
            if (!net.felix.CCLiveUtilitiesConfig.HANDLER.instance().chatIconEnabled) {
                return;
            }
            
            if (client.level == null || trimmedMessages == null || trimmedMessages.isEmpty()) {
                return;
            }

            float scale = client.options.chatScale().get().floatValue();
            if (scale <= 0.0f) {
                return;
            }

            int screenHeight = context.guiHeight();
            int chatBottom = Mth.floor((screenHeight - 40) / scale);
            double chatLineSpacing = client.options.chatLineSpacing().get();
            int messageHeight = 9;
            int entryHeight = (int) (messageHeight * (chatLineSpacing + 1.0));
            int entryBottomToMessageY = (int) Math.round(8.0 * (chatLineSpacing + 1.0) - 4.0 * chatLineSpacing);
            int iconSize = 6;
            int perPage = this.getLinesPerPage();
            int visibleEnd = Math.min(trimmedMessages.size() - chatScrollbarPos, perPage);

            // Gleiche Pose wie ChatComponent.extractRenderState
            context.pose().pushMatrix();
            context.pose().scale(scale, scale);
            context.pose().translate(4.0F, 0.0F);

            try {
                // Wie forEachLine: lineIndex 0 = unterste Zeile = trimmedMessages[chatScrollbarPos]
                for (int lineIndex = visibleEnd - 1; lineIndex >= 0; lineIndex--) {
                    try {
                        int messageIndex = lineIndex + chatScrollbarPos;
                        if (messageIndex < 0 || messageIndex >= trimmedMessages.size()) {
                            continue;
                        }

                        GuiMessage.Line visibleLine = trimmedMessages.get(messageIndex);
                        if (visibleLine == null) {
                            continue;
                        }

                        StringBuilder visibleText = new StringBuilder();
                        visibleLine.content().accept((index, style, codePoint) -> {
                            visibleText.appendCodePoint(codePoint);
                            return true;
                        });
                        String visibleString = visibleText.toString();

                        // Glyph-Marker → Font hat das Icon bereits korrekt gerendert
                        if (visibleString.indexOf(ICON_MARKER) >= 0) {
                            continue;
                        }

                        String messageString = visibleString;
                        if ((messageString == null || messageString.isEmpty()) && allMessages != null && messageIndex < allMessages.size()) {
                            GuiMessage originalLine = allMessages.get(messageIndex);
                            if (originalLine != null && originalLine.content() != null) {
                                messageString = originalLine.content().getString();
                            }
                        }

                        if (messageString == null || messageString.isEmpty()) {
                            continue;
                        }

                        if (messageString.indexOf(ICON_MARKER) >= 0) {
                            continue;
                        }

                        int entryBottom = chatBottom - lineIndex * entryHeight;
                        int textTop = entryBottom - entryBottomToMessageY;

                        String cleanMessage = messageString.replaceAll("§[0-9a-fk-or]", "");
                        boolean isCCLiveMessage = cleanMessage.contains("[CCLive]");

                        if (isCCLiveMessage) {
                            net.minecraft.network.chat.Component cclivePrefix = net.minecraft.network.chat.Component.literal("[CCLive] ")
                                .setStyle(net.minecraft.network.chat.Style.EMPTY.withColor(0xD478F0));
                            int cclivePrefixWidth = client.font.width(cclivePrefix);
                            PlayerIconUtility.renderIcon(context, cclivePrefixWidth, textTop - 1, iconSize + 2);
                            continue;
                        }

                        String playerName = extractPlayerName(messageString);
                        if (playerName == null || playerName.isEmpty()) {
                            continue;
                        }

                        UUID playerUuid = findPlayerUuidByName(playerName, client);
                        if (playerUuid == null || !PlayerIconUtility.hasMod(playerUuid)) {
                            continue;
                        }

                        int namePosition = findPlayerNamePosition(messageString, playerName, client);
                        if (namePosition >= 0) {
                            int nameWidth = client.font.width(playerName);
                            int spaceWidth = client.font.width(" ");
                            int iconX = namePosition + nameWidth + spaceWidth;
                            PlayerIconUtility.renderIcon(context, iconX, textTop - 1, iconSize);
                        }
                    } catch (Exception e) {
                        continue;
                    }
                }
            } finally {
                context.pose().popMatrix();
            }
        } catch (Exception e) {
            // Silently fail
        }
    }
    
    private int findPlayerNamePosition(String messageString, String playerName, Minecraft client) {
        if (messageString == null || playerName == null || playerName.isEmpty() || client == null || client.font == null) {
            return -1;
        }
        
        try {
            String cleanMessage = messageString.replaceAll("§[0-9a-fk-or]", "");
            int nameIndex = cleanMessage.indexOf(playerName);
            if (nameIndex < 0) {
                return -1;
            }
            
            if (cleanMessage.indexOf(">>", nameIndex) < 0) {
                return -1;
            }
            
            return client.font.width(cleanMessage.substring(0, nameIndex));
        } catch (Exception e) {
            return -1;
        }
    }
    
    private String extractPlayerName(String messageText) {
        if (messageText == null || messageText.isEmpty()) {
            return null;
        }
        
        String cleanText = messageText.replaceAll("§[0-9a-fk-or]", "");
        Matcher matcher = PLAYER_NAME_PATTERN.matcher(cleanText);
        if (matcher.find()) {
            for (int i = 1; i <= matcher.groupCount(); i++) {
                String group = matcher.group(i);
                if (group != null && !group.trim().isEmpty()) {
                    String name = group.trim();
                    if (!name.equalsIgnoreCase("Server") &&
                        !name.equalsIgnoreCase("System") &&
                        !name.equalsIgnoreCase("Console") &&
                        name.length() > 0 && name.length() < 20) {
                        return name;
                    }
                }
            }
        }
        
        return null;
    }
    
    private UUID findPlayerUuidByName(String playerName, Minecraft client) {
        try {
            if (client == null || client.getConnection() == null) {
                return null;
            }
            
            var playerList = client.getConnection().getOnlinePlayers();
            if (playerList == null) {
                return null;
            }
            
            for (var entry : playerList) {
                if (entry == null || entry.getProfile() == null) {
                    continue;
                }
                
                String entryName = entry.getProfile().name();
                if (entryName != null && entryName.equalsIgnoreCase(playerName)) {
                    return entry.getProfile().id();
                }
            }
        } catch (Exception e) {
            // Silently fail
        }
        
        return null;
    }
}
