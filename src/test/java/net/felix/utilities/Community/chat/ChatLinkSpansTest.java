package net.felix.utilities.Community.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatLinkSpansTest {
    private static final List<String> DOMAINS = List.of("youtube.com", "twitch.tv", "cclive-utilities.net");
    private static final Style BASE = Style.EMPTY.withColor(ItemShareManager.CCLIVE_MESSAGE_COLOR);

    @Test
    void approvedHttpLinkIsTheOnlyClickablePart() {
        List<Piece> pieces = pieces(ChatLinkSpans.render(
            "schau https://www.youtube.com/watch?v=abc", BASE, DOMAINS));
        Piece link = piece(pieces, "https://www.youtube.com/watch?v=abc");
        ClickEvent.OpenUrl open = assertInstanceOf(ClickEvent.OpenUrl.class, link.style.getClickEvent());
        assertEquals("https://www.youtube.com/watch?v=abc", open.uri().toString());
        assertTrue(link.style.isUnderlined());
        assertNull(piece(pieces, "schau ").style.getClickEvent());
    }

    @Test
    void bareDomainGetsHttpsTarget() {
        List<Piece> pieces = pieces(ChatLinkSpans.render("siehe youtube.com bitte", BASE, DOMAINS));
        ClickEvent.OpenUrl open = assertInstanceOf(
            ClickEvent.OpenUrl.class, piece(pieces, "youtube.com").style.getClickEvent());
        assertEquals("https://youtube.com", open.uri().toString());
        assertNull(piece(pieces, "siehe ").style.getClickEvent());
        assertNull(piece(pieces, " bitte").style.getClickEvent());
    }

    @Test
    void severalLinksStaySeparate() {
        List<ChatLinkSpans.Link> links = ChatLinkSpans.find(
            "https://youtube.com/a und twitch.tv und www.cclive-utilities.net/x", DOMAINS);
        assertEquals(3, links.size());
        assertEquals("https://youtube.com/a", links.get(0).target().toString());
        assertEquals("https://twitch.tv", links.get(1).target().toString());
        assertEquals("https://www.cclive-utilities.net/x", links.get(2).target().toString());
    }

    @Test
    void trailingPunctuationStaysOutsideTheLink() {
        List<Piece> pieces = pieces(ChatLinkSpans.render(
            "Hier (https://youtube.com/watch?v=abc).", BASE, DOMAINS));
        ClickEvent.OpenUrl open = assertInstanceOf(
            ClickEvent.OpenUrl.class, piece(pieces, "https://youtube.com/watch?v=abc").style.getClickEvent());
        assertEquals("https://youtube.com/watch?v=abc", open.uri().toString());
        assertNull(piece(pieces, "Hier (").style.getClickEvent());
        assertNull(piece(pieces, ").").style.getClickEvent());
    }

    @Test
    void blockedDomainAndShareTokensAreNotClickable() {
        assertTrue(ChatLinkSpans.find("https://evil.example/phish", DOMAINS).isEmpty());
        assertTrue(ChatLinkSpans.find("https://playlegend.net@evil.example", DOMAINS).isEmpty());
        assertTrue(ChatLinkSpans.find("Version 1.20.1 ist da", DOMAINS).isEmpty());
        List<ChatLinkSpans.Link> links = ChatLinkSpans.find(
            "Item #ITEM:ab-cd und #BUILD:xy-z auf youtube.com", DOMAINS);
        assertEquals(1, links.size());
        assertEquals("https://youtube.com", links.get(0).target().toString());
        List<Piece> pieces = pieces(ChatLinkSpans.render(
            "Item #ITEM:ab-cd auf youtube.com", BASE, DOMAINS));
        assertNull(piece(pieces, "Item #ITEM:ab-cd auf ").style.getClickEvent());
    }

    @Test
    void linkDoesNotReplaceNameHoverAndDeletionDropsTheTarget() {
        Component body = ChatLinkSpans.render("schau https://youtube.com.", BASE, DOMAINS);
        MutableComponent line = ChatLineTarget.compose(
            "Steve", 4L, new int[] {0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF}, null, null, body);
        Piece link = pieces(line).stream()
            .filter(piece -> piece.text.startsWith("https://youtube.com"))
            .findFirst()
            .orElseThrow();
        assertInstanceOf(ClickEvent.OpenUrl.class, link.style.getClickEvent());
        assertNull(ChatLineTarget.resolve(line, link.style));
        assertNull(ChatLineTarget.nameStyle(line, 0).getClickEvent());
        assertNull(ChatLineTarget.prefixStyle(line).getClickEvent());

        Component replaced = ChatHudMessageIds.replaceMessageBody(line, ChatManager.PLACEHOLDER);
        assertTrue(replaced.getString().contains(ChatManager.PLACEHOLDER));
        assertTrue(pieces(replaced).stream().noneMatch(piece -> piece.style.getClickEvent() instanceof ClickEvent.OpenUrl));
        assertNull(ChatManager.deletedPlaceholderBody().getStyle().getClickEvent());
    }

    private static Piece piece(List<Piece> pieces, String text) {
        return pieces.stream().filter(piece -> piece.text.equals(text)).findFirst().orElseThrow();
    }

    private static List<Piece> pieces(Component component) {
        List<Piece> found = new ArrayList<>();
        component.visit((style, text) -> {
            found.add(new Piece(text, style));
            return Optional.empty();
        }, Style.EMPTY);
        return found;
    }

    private record Piece(String text, Style style) {
    }
}
