package net.felix.Community.chat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.felix.Community.playericon.PlayerIconUtility;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatContextTargetTest {
    private static final int[] GRADIENT = {0x112233, 0x445566, 0x778899, 0x99AABB, 0xCCDDEE};

    @Test
    void nameAndPrefixResolveTheSameLineAmongTwoMessages() {
        HoverEvent hover = showText(Component.literal("Stats"));
        Component first = line("Steve", 10L, Component.literal("eins"));
        Component second = line("Steve", 11L, Component.literal("zwei"));
        List<Component> lines = List.of(first, second);

        ChatLineTarget.Hit prefix = ChatLineTarget.resolveAmong(lines, ChatLineTarget.prefixStyle(first));
        ChatLineTarget.Hit name = ChatLineTarget.resolveAmong(lines, ChatLineTarget.nameStyle(first, 1));
        ChatLineTarget.Hit otherPrefix = ChatLineTarget.resolveAmong(lines, ChatLineTarget.prefixStyle(second));
        ChatLineTarget.Hit otherName = ChatLineTarget.resolveAmong(lines, ChatLineTarget.nameStyle(second, 0));

        assertEquals("Steve", prefix.player());
        assertEquals(10L, prefix.messageId());
        assertEquals(10L, name.messageId());
        assertEquals("Steve", name.player());
        assertEquals(11L, otherPrefix.messageId());
        assertEquals(11L, otherName.messageId());
        assertEquals("Steve", otherName.player());

        Style letter = ChatLineTarget.nameStyle(first, 1);
        assertNull(letter.getClickEvent());
        assertTrue(letter.getInsertion() == null || letter.getInsertion().isEmpty());
        if (hover != null) {
            assertEquals(hover, letter.getHoverEvent());
        }
    }

    @Test
    void wrappedPieceAndDeletedBodyKeepTheSameId() {
        Component original = line("Steve", 10L, Component.literal("lang"));
        Style wrappedLetter = ChatLineTarget.nameStyle(original, 3);
        assertEquals(10L, ChatLineTarget.resolve(original, wrappedLetter).messageId());

        Component replaced = ChatHudMessageIds.replaceMessageBody(original, "Nachricht von Moderatoren gelöscht!");
        ChatLineTarget.Hit prefix = ChatLineTarget.resolve(replaced, ChatLineTarget.prefixStyle(replaced));
        ChatLineTarget.Hit name = ChatLineTarget.resolve(replaced, ChatLineTarget.nameStyle(replaced, 2));
        assertEquals(10L, prefix.messageId());
        assertEquals("Steve", prefix.player());
        assertEquals(10L, name.messageId());
        assertEquals("Steve", name.player());
    }

    @Test
    void oldLineWithoutIdKeepsIgnoreAndHidesDelete() {
        Component old = ChatLineTarget.compose("Steve", null, GRADIENT, null, null, Component.literal("alt"));
        ChatLineTarget.Hit hit = ChatLineTarget.resolve(old, ChatLineTarget.nameStyle(old, 0));
        assertEquals("Steve", hit.player());
        assertNull(hit.messageId());
        assertFalse(hit.canDelete());

        List<ChatContextMenu.Entry> staff = ChatContextMenu.entries(true, false, hit.messageId());
        assertTrue(staff.stream().anyMatch(entry -> entry.kind() == ChatContextMenu.Kind.IGNORE));
        assertFalse(staff.stream().anyMatch(entry -> entry.kind() == ChatContextMenu.Kind.DELETE));
        assertTrue(staff.stream().anyMatch(entry -> entry.kind() == ChatContextMenu.Kind.MUTE));
    }

    @Test
    void sharesIconsAndBodyAreNotMenuTargets() {
        Style iconStyle = Style.EMPTY.withColor(0xFFFFFF);
        Component icon = Component.literal(String.valueOf(PlayerIconUtility.CHAT_ICON_MARKER)).setStyle(iconStyle);
        Style share = Style.EMPTY.withInsertion("cclive-share:ITEM-1");
        Component line = ChatLineTarget.compose(
            "Steve", 10L, GRADIENT, null, icon, Component.literal("item").setStyle(share));
        assertTrue(ChatLineTarget.isShare(share));
        assertNull(ChatLineTarget.resolve(line, share));
        assertNull(ChatLineTarget.resolve(line, iconStyle));
        Style body = Style.EMPTY.withColor(0xFBA9FC);
        Component plain = ChatLineTarget.compose("Steve", 10L, GRADIENT, null, null, Component.literal("hallo").setStyle(body));
        assertNull(ChatLineTarget.resolve(plain, body));
    }

    @Test
    void menuShowsStaffActionsOnlyForStaffAndReportDoesNothing() {
        List<ChatContextMenu.Entry> player = ChatContextMenu.entries(false, false, 10L);
        assertEquals(ChatContextMenu.Kind.IGNORE, player.get(0).kind());
        assertEquals(ChatContextMenu.Kind.REPORT, player.get(1).kind());
        assertEquals("Melden", player.get(1).label());
        assertEquals(2, player.size());

        List<ChatContextMenu.Entry> staff = ChatContextMenu.entries(true, true, 10L);
        assertTrue(staff.stream().anyMatch(entry -> entry.kind() == ChatContextMenu.Kind.UNIGNORE));
        assertTrue(staff.stream().anyMatch(entry -> entry.kind() == ChatContextMenu.Kind.DELETE));
        assertTrue(staff.stream().anyMatch(entry -> entry.kind() == ChatContextMenu.Kind.BAN));
    }

    @Test
    void ignoreIsSeparatePerAccountAndCheckedBeforeInsert() throws Exception {
        Path dir = Files.createTempDirectory("cclive-ignore");
        ChatIgnoreList list = ChatIgnoreList.load(dir.resolve("chat-ignore.json"));
        list.ignore("account-a", "Steve");
        list.ignore("account-b", "Alex");
        ChatIgnoreList again = ChatIgnoreList.load(dir.resolve("chat-ignore.json"));
        assertTrue(again.ignores("account-a", "steve"));
        assertFalse(again.ignores("account-a", "Alex"));
        assertTrue(again.ignores("account-b", "Alex"));
        assertEquals(List.of("Steve"), again.names("account-a"));

        assertTrue(ChatManager.shouldInsertPreparedLine("Alex", again, "account-a"));
        again.ignore("account-a", "Alex");
        assertFalse(ChatManager.shouldInsertPreparedLine("Alex", again, "account-a"));
        assertTrue(ChatManager.shouldInsertPreparedLine("Nora", again, "account-a"));
    }

    @Test
    void ignoringRemovesOnlyThatPlayersVisibleLines() {
        List<GuiMessage> messages = new ArrayList<>();
        messages.add(message(line("Steve", 10L, Component.literal("eins"))));
        messages.add(message(line("Nora", 11L, Component.literal("zwei"))));
        messages.add(message(line("Steve", 12L, Component.literal("drei"))));
        assertEquals(2, ChatHudMessageIds.removeByPlayer(messages, "steve"));
        assertEquals(1, messages.size());
        assertEquals("Nora", ChatLineTarget.playerOf(messages.get(0).content()));
    }

    @Test
    void ignoreRemovesOnlyTheCcliveLineNotAServerLineInTheSameFormat() {
        Component cclive = ChatLineTarget.compose("Steve", null, GRADIENT, null, null, Component.literal("hallo"));
        Component server = Component.empty()
            .append(Component.literal("Steve"))
            .append(Component.literal(">> "))
            .append(Component.literal("hallo"));
        assertEquals("Steve", ChatLineTarget.playerOf(cclive));
        assertNull(ChatLineTarget.playerOf(server));
        assertNull(ChatLineTarget.resolve(server, ChatLineTarget.nameStyle(server, 0)));
        ChatLineTarget.Hit oldLine = ChatLineTarget.resolve(cclive, ChatLineTarget.nameStyle(cclive, 0));
        assertEquals("Steve", oldLine.player());
        assertNull(oldLine.messageId());

        List<GuiMessage> messages = new ArrayList<>();
        messages.add(message(cclive));
        messages.add(message(server));
        assertEquals(1, ChatHudMessageIds.removeByPlayer(messages, "Steve"));
        assertEquals(1, messages.size());
        assertEquals("Steve>> hallo", messages.get(0).content().getString());
    }

    @Test
    void renderedPrefixNameAndArrowResolveTheClickedLine() {
        HoverEvent hover = showText(Component.literal("Stats"));
        Style iconStyle = Style.EMPTY.withColor(0xFFFFFF);
        Component icon = Component.literal(String.valueOf(PlayerIconUtility.CHAT_ICON_MARKER)).setStyle(iconStyle);
        Style share = Style.EMPTY.withInsertion("cclive-share:ITEM-1");
        Component first = ChatLineTarget.compose(
            "Steve", 10L, GRADIENT, hover, icon, Component.literal("item").setStyle(share));
        Component second = ChatLineTarget.compose(
            "Steve", 11L, GRADIENT, hover, null, Component.literal("zwei"));
        assertLineGlyphs(first, 10L, hover);
        assertLineGlyphs(second, 11L, hover);

        Style secondName = glyphStyles(second).stream()
            .filter(style -> ChatLineTarget.resolve(second, style) != null
                && !ChatLineTarget.clickFinderAccepts(style))
            .findFirst()
            .orElseThrow();
        ChatLineTarget.Hit among = ChatLineTarget.resolveAmong(List.of(first, second), secondName);
        assertEquals(11L, among.messageId());
        assertEquals("Steve", among.player());
        assertNull(ChatLineTarget.resolveAmong(List.of(first, second), share));
        assertNull(ChatLineTarget.resolve(first, iconStyle));
    }

    @Test
    void ownAccountIsExcludedAndDoesNotHideOwnLines() throws Exception {
        List<ChatIgnoreRules.Match> matches = List.of(
            new ChatIgnoreRules.Match("Steve", 1L, false),
            new ChatIgnoreRules.Match("Steven", 2L, false),
            new ChatIgnoreRules.Match("Plain", 3L, true)
        );
        assertEquals(List.of("Steven"), ChatIgnoreRules.suggestions(matches, "steve", 1L).stream()
            .map(ChatIgnoreRules.Match::name).toList());
        assertEquals(ChatIgnoreRules.SELF, ChatIgnoreRules.rejection("STEVE", matches, "steve", 1L));
        assertEquals(ChatIgnoreRules.UNKNOWN, ChatIgnoreRules.rejection("Nora", matches, "steve", null));
        assertEquals("Steven", ChatIgnoreRules.confirmed("steven", matches, "steve", 1L).name());

        List<ChatContextMenu.Entry> own = ChatContextMenu.entries(true, false, 10L, true);
        assertFalse(own.stream().anyMatch(entry -> entry.kind() == ChatContextMenu.Kind.IGNORE));
        assertTrue(own.stream().anyMatch(entry -> entry.kind() == ChatContextMenu.Kind.DELETE));

        Path dir = Files.createTempDirectory("cclive-ignore-self");
        ChatIgnoreList list = ChatIgnoreList.load(dir.resolve("chat-ignore.json"));
        list.ignore("account-a", "Steve");
        list.ignore("account-a", "OldName", 7L);
        ChatIgnoreList again = ChatIgnoreList.load(dir.resolve("chat-ignore.json"));
        assertEquals(7L, again.playerId("account-a", "oldname"));
        assertTrue(ChatManager.shouldInsertPreparedLine("steve", again, "account-a", "Steve", null));
        assertTrue(ChatManager.shouldInsertPreparedLine("OldName", again, "account-a", "Renamed", 7L));
        again.ignore("account-a", "Alex");
        assertFalse(ChatManager.shouldInsertPreparedLine("Alex", again, "account-a", "Steve", 7L));
    }

    @Test
    void directDeleteUsesTheLineIdWithoutReasonAndIgnoresASecondClick() {
        com.google.gson.JsonObject body = ChatMessageDelete.body(11L);
        assertEquals(11, body.get("messageId").getAsInt());
        assertEquals("", body.get("reason").getAsString());
        assertTrue(ChatMessageDelete.begin(11L));
        assertFalse(ChatMessageDelete.begin(11L));
        ChatMessageDelete.finish(11L);
        assertTrue(ChatMessageDelete.begin(11L));
        ChatMessageDelete.finish(11L);
        assertEquals("Nachricht gelöscht.", ChatMessageDelete.feedback(200, null, false));
        assertEquals("Diese Nachricht ist bereits gelöscht.", ChatMessageDelete.feedback(200, null, true));
        assertEquals(
            "Moderatoren können keine Aktionen gegen Teammitglieder ausführen.",
            ChatMessageDelete.feedback(403, "Moderatoren können keine Aktionen gegen Teammitglieder ausführen.", false));
    }

    @Test
    void contextMenuClosesWhenItsScreenGoesAwayAndStaysClosed() {
        Object chat = new Object();
        Object reopened = new Object();
        ChatContextMenu.rememberOpenOn(chat);
        assertTrue(ChatContextMenu.isOpen());
        assertTrue(ChatContextMenu.hidesPlayerStatsHover());
        ChatContextMenu.close();
        assertFalse(ChatContextMenu.hidesPlayerStatsHover());
        ChatContextMenu.rememberOpenOn(chat);
        assertTrue(ChatContextMenu.hidesPlayerStatsHover());
        ChatContextMenu.dismissWithScreen(new Object());
        assertTrue(ChatContextMenu.isOpen());
        assertTrue(ChatContextMenu.hidesPlayerStatsHover());
        ChatContextMenu.dismissWithScreen(chat);
        assertFalse(ChatContextMenu.isOpen());
        assertFalse(ChatContextMenu.hidesPlayerStatsHover());
        assertFalse(ChatContextMenu.isOpen());
        ChatContextMenu.dismissWithScreen(reopened);
        assertFalse(ChatContextMenu.isOpen());
    }

    private static void assertLineGlyphs(Component line, long id, HoverEvent hover) {
        StringBuilder plain = new StringBuilder();
        List<Style> styles = new ArrayList<>();
        line.getVisualOrderText().accept((index, style, codePoint) -> {
            plain.appendCodePoint(codePoint);
            styles.add(style);
            return true;
        });
        int arrow = plain.indexOf(">>");
        assertTrue(arrow > 0);
        for (int i = 0; i < arrow + 2; i++) {
            if (PlayerIconUtility.isChatIconMarker(plain.charAt(i))) {
                assertNull(ChatLineTarget.resolve(line, styles.get(i)));
                continue;
            }
            if (Character.isWhitespace(plain.charAt(i))) {
                ChatLineTarget.Hit gap = ChatLineTarget.resolve(line, styles.get(i));
                if (gap != null) {
                    assertEquals(id, gap.messageId());
                }
                continue;
            }
            ChatLineTarget.Hit hit = ChatLineTarget.resolve(line, styles.get(i));
            assertEquals("Steve", hit.player());
            assertEquals(id, hit.messageId());
            Style style = styles.get(i);
            assertNull(style.getClickEvent());
            boolean nameOrArrow = plain.charAt(i) == '>' || (Character.isLetter(plain.charAt(i)) && i > plain.indexOf("]"));
            if (nameOrArrow) {
                assertFalse(ChatLineTarget.clickFinderAccepts(style));
            }
            if (hover != null) {
                assertEquals(hover, style.getHoverEvent());
            }
        }
        int body = arrow + 3;
        assertTrue(body < styles.size());
        assertNull(ChatLineTarget.resolve(line, styles.get(body)));
        assertEquals(id, ChatLineTarget.resolve(line, ChatLineTarget.arrowStyle(line)).messageId());
    }

    private static List<Style> glyphStyles(Component line) {
        List<Style> styles = new ArrayList<>();
        line.getVisualOrderText().accept((index, style, codePoint) -> {
            styles.add(style);
            return true;
        });
        return styles;
    }

    private static Component line(String player, long id, Component body) {
        HoverEvent hover = showText(Component.literal("Stats"));
        return ChatLineTarget.compose(player, id, GRADIENT, hover, null, body);
    }

    private static HoverEvent showText(Component text) {
        try {
            for (Class<?> type : HoverEvent.class.getDeclaredClasses()) {
                for (java.lang.reflect.Constructor<?> constructor : type.getDeclaredConstructors()) {
                    Class<?>[] params = constructor.getParameterTypes();
                    if (params.length == 1 && params[0] == Component.class) {
                        constructor.setAccessible(true);
                        Object value = constructor.newInstance(text);
                        if (value instanceof HoverEvent event) {
                            return event;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static GuiMessage message(Component content) {
        return new GuiMessage(1, content, null, GuiMessageSource.SYSTEM_CLIENT, GuiMessageTag.system());
    }
}
