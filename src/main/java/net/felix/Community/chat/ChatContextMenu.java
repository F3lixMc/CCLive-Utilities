package net.felix.Community.chat;

import java.util.ArrayList;
import java.util.List;
import net.felix.Community.staff.StaffActionScreen;
import net.felix.Community.staff.StaffClientAccess;
import net.felix.mixin.ChatHudMessagesAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

/**
 * Rechtsklick-Menü im offenen Chat. Es schließt den Chat nicht.
 * „Nachricht löschen“ sendet die Löschung dieser Zeile direkt.
 */
public final class ChatContextMenu {
    public enum Kind {
        IGNORE,
        UNIGNORE,
        REPORT,
        DELETE,
        MUTE,
        BAN
    }

    public record Entry(Kind kind, String label) {
    }

    private static final int ROW = 14;
    private static boolean open;
    private static Object owner;
    private static int x;
    private static int y;
    private static int width;
    private static int height;
    private static String player = "";
    private static Long messageId;
    private static List<Entry> entries = List.of();

    private ChatContextMenu() {
    }

    public static List<Entry> entries(boolean staff, boolean ignored, Long messageId) {
        return entries(staff, ignored, messageId, false);
    }

    public static List<Entry> entries(boolean staff, boolean ignored, Long messageId, boolean self) {
        List<Entry> list = new ArrayList<>();
        if (self) {
            if (ignored) {
                list.add(new Entry(Kind.UNIGNORE, "Nicht mehr ignorieren"));
            }
        } else if (ignored) {
            list.add(new Entry(Kind.UNIGNORE, "Nicht mehr ignorieren"));
        } else {
            list.add(new Entry(Kind.IGNORE, "Spieler ignorieren"));
        }
        list.add(new Entry(Kind.REPORT, "Melden"));
        if (staff) {
            if (messageId != null && messageId > 0L) {
                list.add(new Entry(Kind.DELETE, "Nachricht löschen"));
            }
            list.add(new Entry(Kind.MUTE, "Mute"));
            list.add(new Entry(Kind.BAN, "Chatbann"));
        }
        return List.copyOf(list);
    }

    public static boolean tryOpen(double mouseX, double mouseY) {
        Style style = ChatStyleHelper.getRenderedChatStyle(mouseX, mouseY);
        ChatLineTarget.Hit hit = ChatLineTarget.resolveAmong(visibleLines(), style);
        if (hit == null || hit.player() == null || hit.player().isEmpty()) {
            return false;
        }
        String account = ChatIgnoreList.localAccountId();
        boolean self = ChatIgnoreList.isOwn(
            hit.player(),
            ChatIgnoreList.localPlayerName(),
            account == null ? null : ChatIgnoreList.shared().playerId(account, hit.player()),
            ChatPlayerSearch.selfId());
        boolean ignored = account != null && ChatIgnoreList.shared().ignores(account, hit.player());
        entries = entries(StaffClientAccess.isStaffCommandVisible(), ignored, hit.messageId(), self);
        player = hit.player();
        messageId = hit.messageId();
        Minecraft client = Minecraft.getInstance();
        owner = client == null ? null : client.gui.screen();
        Font font = client == null ? null : client.font;
        int textWidth = 0;
        if (font != null) {
            for (Entry entry : entries) {
                textWidth = Math.max(textWidth, font.width(entry.label()));
            }
        } else {
            textWidth = 180;
        }
        width = textWidth + 12;
        height = entries.size() * ROW + 6;
        int screenW = client != null && client.getWindow() != null ? client.getWindow().getGuiScaledWidth() : width;
        int screenH = client != null && client.getWindow() != null ? client.getWindow().getGuiScaledHeight() : height;
        x = (int) mouseX;
        y = (int) mouseY;
        if (x + width > screenW) {
            x = Math.max(0, screenW - width);
        }
        if (y + height > screenH) {
            y = Math.max(0, screenH - height);
        }
        open = true;
        return true;
    }

    public static void close() {
        open = false;
        owner = null;
    }

    static void rememberOpenOn(Object screen) {
        owner = screen;
        open = true;
    }

    public static void dismissWithScreen(Object screen) {
        if (owner == screen) {
            close();
        }
    }

    static boolean isOpen() {
        return open;
    }

    /** Solange das Menü offen ist, bleibt der Playerstat-Hover aus. Nach dem Schließen gilt wieder der normale Hover. */
    public static boolean hidesPlayerStatsHover() {
        return open;
    }

    public static boolean handleClick(double mouseX, double mouseY, int legacyButton) {
        if (!open || legacyButton != 0) {
            return false;
        }
        int row = rowAt(mouseX, mouseY);
        if (row < 0) {
            close();
            return false;
        }
        Entry entry = entries.get(row);
        if (entry.kind() == Kind.REPORT) {
            ChatReport.request(player, messageId, "");
            close();
            return true;
        }
        String account = ChatIgnoreList.localAccountId();
        if (entry.kind() == Kind.IGNORE && account != null) {
            if (!ChatIgnoreList.isOwn(player, ChatIgnoreList.localPlayerName(), null, ChatPlayerSearch.selfId())) {
                ChatIgnoreList.shared().ignore(account, player, null);
                ChatHudMessageIds.removeByPlayer(Minecraft.getInstance(), player);
            }
        } else if (entry.kind() == Kind.UNIGNORE && account != null) {
            ChatIgnoreList.shared().unignore(account, player);
        } else if (entry.kind() == Kind.DELETE) {
            ChatMessageDelete.request(messageId);
        } else if (entry.kind() == Kind.MUTE || entry.kind() == Kind.BAN) {
            StaffActionScreen.open(player, messageId);
        }
        close();
        return true;
    }

    public static void render(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        if (!open || context == null) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.font == null || client.gui.screen() != owner) {
            close();
            return;
        }
        context.fill(x, y, x + width, y + height, 0xF0101410);
        context.outline(x, y, width, height, 0xFF8AA88A);
        int textY = y + 4;
        for (Entry entry : entries) {
            int color = 0xFFF4F7F4;
            context.text(client.font, Component.literal(entry.label()), x + 6, textY, color, false);
            textY += ROW;
        }
    }

    private static int rowAt(double mouseX, double mouseY) {
        if (mouseX < x || mouseX >= x + width || mouseY < y || mouseY >= y + height) {
            return -1;
        }
        int row = (int) ((mouseY - y - 3) / ROW);
        if (row < 0 || row >= entries.size()) {
            return -1;
        }
        return row;
    }

    private static List<Component> visibleLines() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.gui == null || client.gui.hud == null) {
            return List.of();
        }
        ChatComponent chat = client.gui.hud.getChat();
        if (!(chat instanceof ChatHudMessagesAccess access)) {
            return List.of();
        }
        List<GuiMessage> messages = access.cclive$allMessages();
        if (messages == null) {
            return List.of();
        }
        List<Component> lines = new ArrayList<>();
        for (GuiMessage message : messages) {
            if (message != null && message.content() != null) {
                lines.add(message.content());
            }
        }
        return lines;
    }
}
