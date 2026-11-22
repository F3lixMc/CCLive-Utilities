package net.felix.Community.chat;

import net.felix.Community.playericon.PlayerIconUtility;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.felix.Community.chat.share.ChatShareLine;
import net.felix.Community.chat.share.ChatShareTexts;

/** Inserts chat icons as font glyphs so wrapping, scaling and fading follow the message. */
public final class ChatLineIcons {
    private static final FontDescription.Resource FONT = new FontDescription.Resource(
            Identifier.fromNamespaceAndPath("cclive-utilities", "default"));

    private ChatLineIcons() {
    }

    public static Component add(Component message, String playerName) {
        if (message == null || playerName == null) {
            return message;
        }
        String text = message.getString();
        if (!playerName.equals(ChatShareTexts.senderName(text))) {
            return message;
        }
        int end = text.indexOf(">>");
        while (end > 0 && Character.isWhitespace(text.charAt(end - 1))) {
            end--;
        }
        if (end > 0 && PlayerIconUtility.isChatIconMarker(text.charAt(end - 1))) {
            return message;
        }
        Component icon = Component.literal(" ").append(
                Component.literal(String.valueOf(PlayerIconUtility.chatIconMarker(playerName)))
                        .setStyle(Style.EMPTY.withFont(FONT).withColor(0xFFFFFF)));
        return ChatShareLine.insertAt(message, end, icon);
    }
}
