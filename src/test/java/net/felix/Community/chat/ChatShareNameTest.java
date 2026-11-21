package net.felix.Community.chat;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatShareNameTest {
    @Test
    void itemChatNameDropsLevelAndForgingOnly() {
        assertEquals("Phantomfluch", ChatShareNames.itemChatName("+15 Phantomfluch [Frostgeschmiedet]"));
        assertEquals("Phantomfluch", ChatShareNames.itemChatName("[+15] Phantomfluch [Frostgeschmiedet]"));
        assertEquals("Phantomfluch [Modul]", ChatShareNames.itemChatName("Phantomfluch [Modul]"));
        assertEquals("Klinge +15", ChatShareNames.itemChatName("Klinge +15"));
    }

    @Test
    void cleanedNameFeedsHashSpanAndLength() {
        String name = ChatShareNames.itemChatName("+15 Phantomfluch [Frostgeschmiedet]");
        ChatShareTexts.Replacement replacement = ChatShareTexts.replace("vor !item nach", name, null);

        assertEquals("vor Phantomfluch nach", replacement.text());
        assertEquals(4, replacement.nameStart());
        assertEquals(name.length(), replacement.nameLength());
        assertEquals(name, replacement.text().substring(replacement.nameStart(),
                replacement.nameStart() + replacement.nameLength()));
        assertEquals(ChatShareTexts.sha256("vor Phantomfluch nach"), ChatShareTexts.sha256(replacement.text()));
    }

    @Test
    void nameColorComesFromTheItemNameSpan() {
        List<ChatShareNames.Piece> pieces = List.of(
                new ChatShareNames.Piece("+15 ", 0xFFAA00),
                new ChatShareNames.Piece("Phantomfluch", 0x55FF55),
                new ChatShareNames.Piece(" [Frostgeschmiedet]", 0x5555FF));

        assertEquals(0x55FF55, ChatShareNames.colorOfItemName(pieces, "Phantomfluch"));
        assertNull(ChatShareNames.colorOfItemName(List.of(), "Phantomfluch"));
        assertNull(ChatShareNames.chatColor("ITEM", null));
        assertNull(ChatShareNames.chatColor("ITEM", -1));
        assertNull(ChatShareNames.chatColor("ITEM", 0x1FFFFFF));
        assertEquals(ChatShareNames.BUILD_COLOR, ChatShareNames.chatColor("BUILD", null));
        assertEquals(0x55FFFF, ChatShareNames.BUILD_COLOR);
    }

    @Test
    void onlyTheShareNameIsRecoloredAndTheTooltipReloadKeepsIt() {
        HoverEvent hover = new HoverEvent.ShowText(Component.literal("+15 Phantomfluch [Frostgeschmiedet]\nSchaden"));
        Style link = Style.EMPTY
                .withColor(0x55FF55)
                .withUnderlined(true)
                .withInsertion("cclive-share:ITEM-abc123")
                .withHoverEvent(hover);
        Component line = Component.literal("vor ").setStyle(Style.EMPTY.withColor(0xFFFFFF))
                .append(Component.literal("Phantomfluch").setStyle(Style.EMPTY.withColor(0xAAAAAA)))
                .append(Component.literal(" nach").setStyle(Style.EMPTY.withColor(0xFFFFFF)));

        Component decorated = ChatShareLine.decorateRange(line, 4, "Phantomfluch".length(), link);
        assertEquals("vor Phantomfluch nach", decorated.getString());
        assertEquals(0xFFFFFF, colorOf(decorated, "vor "));
        assertEquals(0x55FF55, colorOf(decorated, "Phantomfluch"));
        assertEquals(0xFFFFFF, colorOf(decorated, " nach"));
        assertEquals("cclive-share:ITEM-abc123", insertionOf(decorated, "Phantomfluch"));
        assertNull(insertionOf(decorated, "vor "));
        assertTrue(hoverOf(decorated, "Phantomfluch"));
        assertTrue(underlined(decorated, "Phantomfluch"));

        Style reloaded = link.withHoverEvent(new HoverEvent.ShowText(Component.literal("voller Tooltip")));
        Component again = ChatShareLine.decorateRange(decorated, 4, "Phantomfluch".length(), reloaded);
        assertEquals(0x55FF55, colorOf(again, "Phantomfluch"));
        assertEquals(0xFFFFFF, colorOf(again, " nach"));
        assertTrue(hoverOf(again, "Phantomfluch"));
        assertEquals("cclive-share:ITEM-abc123", insertionOf(again, "Phantomfluch"));
    }

    @Test
    void buildColorIsAquaWithoutATransmittedField() {
        Style link = Style.EMPTY.withColor(ChatShareNames.chatColor("BUILD", null)).withUnderlined(true)
                .withInsertion("cclive-build:BUILD-abc123");
        Component line = Component.literal("vor Build nach");
        Component decorated = ChatShareLine.decorateRange(line, 4, "Build".length(), link);
        assertEquals(0x55FFFF, colorOf(decorated, "Build"));
        assertNull(colorOf(decorated, "vor "));
        assertNull(colorOf(decorated, " nach"));
    }

    private static Integer colorOf(Component root, String wanted) {
        Integer[] found = {null};
        walk(root, (text, style) -> {
            if (wanted.equals(text) && style.getColor() != null) {
                found[0] = style.getColor().getValue() & 0xFFFFFF;
            } else if (wanted.equals(text)) {
                found[0] = null;
            }
        });
        return found[0];
    }

    private static String insertionOf(Component root, String wanted) {
        String[] found = {null};
        walk(root, (text, style) -> {
            if (wanted.equals(text)) {
                found[0] = style.getInsertion();
            }
        });
        return found[0];
    }

    private static boolean hoverOf(Component root, String wanted) {
        boolean[] found = {false};
        walk(root, (text, style) -> {
            if (wanted.equals(text)) {
                found[0] = style.getHoverEvent() != null;
            }
        });
        return found[0];
    }

    private static boolean underlined(Component root, String wanted) {
        boolean[] found = {false};
        walk(root, (text, style) -> {
            if (wanted.equals(text)) {
                found[0] = Boolean.TRUE.equals(style.isUnderlined());
            }
        });
        return found[0];
    }

    private static void walk(Component component, java.util.function.BiConsumer<String, Style> visit) {
        if (component == null) {
            return;
        }
        String own = component.getContents() instanceof net.minecraft.network.chat.contents.PlainTextContents plain
                ? plain.text() : "";
        if (own != null && !own.isEmpty()) {
            visit.accept(own, component.getStyle() == null ? Style.EMPTY : component.getStyle());
        }
        for (Component sibling : component.getSiblings()) {
            walk(sibling, visit);
        }
    }
}
