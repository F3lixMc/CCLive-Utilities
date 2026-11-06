package net.felix.chat;

import net.felix.mixin.ChatHudMessagesAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Vanilla fasst nur identische sichtbare Texte zusammen. CCLive-Zeilen tragen
 * die Server-ID in der Insertion, daher werden verschiedene IDs nicht gestapelt.
 * Nach {@code chat.deleted} wird nur die passende Zeile durch den Platzhalter ersetzt.
 */
public final class ChatHudMessageIds {
    private ChatHudMessageIds() {
    }

    public static void replacePlaceholder(Minecraft client, long messageId, String placeholder) {
        if (client == null || client.gui == null || client.gui.hud == null) {
            return;
        }
        ChatComponent chat = client.gui.hud.getChat();
        if (!(chat instanceof ChatHudMessagesAccess access)) {
            return;
        }
        List<GuiMessage> messages = access.cclive$allMessages();
        if (replaceMatching(messages, messageId, placeholder) > 0) {
            access.cclive$refreshTrimmedMessages();
        }
    }

    static int replaceMatching(List<GuiMessage> messages, long messageId, String placeholder) {
        if (messages == null || messageId <= 0L) {
            return 0;
        }
        String needle = ChatManager.MESSAGE_ID_PREFIX + messageId;
        int changed = 0;
        for (int i = 0; i < messages.size(); i++) {
            GuiMessage current = messages.get(i);
            if (current == null || !hasInsertion(current.content(), needle)) {
                continue;
            }
            Component replaced = replaceMessageBody(current.content(), placeholder);
            if (replaced == null) {
                continue;
            }
            messages.set(i, new GuiMessage(
                current.addedTime(),
                replaced,
                current.signature(),
                current.source(),
                current.tag()
            ));
            changed++;
        }
        return changed;
    }

    static Component replaceMessageBody(Component line, String placeholder) {
        if (!(line instanceof MutableComponent mutable) || placeholder == null) {
            return null;
        }
        List<Component> siblings = new ArrayList<>(mutable.getSiblings());
        int arrow = -1;
        for (int i = 0; i < siblings.size(); i++) {
            Component sibling = siblings.get(i);
            if (sibling != null && ">> ".equals(sibling.getString())) {
                arrow = i;
            }
        }
        if (arrow < 0) {
            return null;
        }
        MutableComponent rebuilt = Component.empty();
        if (mutable.getStyle() != null) {
            rebuilt.setStyle(mutable.getStyle());
        }
        for (int i = 0; i <= arrow; i++) {
            rebuilt.append(siblings.get(i).copy());
        }
        rebuilt.append(Component.literal(placeholder).setStyle(Style.EMPTY.withColor(ChatManager.DELETED_BODY_COLOR)));
        return rebuilt;
    }

    static boolean hasInsertion(Component content, String needle) {
        if (content == null || needle == null) {
            return false;
        }
        Boolean found = content.visit((style, text) -> {
            if (style != null && needle.equals(style.getInsertion())) {
                return Optional.of(Boolean.TRUE);
            }
            return Optional.empty();
        }, Style.EMPTY).orElse(Boolean.FALSE);
        return found;
    }

}
