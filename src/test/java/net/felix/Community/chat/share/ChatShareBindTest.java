package net.felix.Community.chat.share;

import java.util.List;
import java.util.ArrayList;
import net.felix.Community.chat.share.ChatShareCatalog.Bind;
import net.felix.Community.chat.share.ChatShareInbox.Line;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import net.felix.Community.chat.ChatLineIcons;

class ChatShareBindTest {

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void separateClientFindsPublishedBindingWithoutTheDateInsertion() {
        String body = "test Coin Kit ende";
        Component incoming = Component.empty()
                .append(Component.literal("⸩ "))
                .append(coloredName("firestarter03", showText(Component.literal("Server stats"))).component)
                .append(Component.literal(" >> ")).append(Component.literal(body));

        ChatShareLine.Parsed parsed = ChatShareLine.parse(incoming);
        assertTrue(parsed.complete());
        assertNull(parsed.timestamp());
        ChatShareInbox recipient = new ChatShareInbox();
        long received = 10_000L;
        Line line = new Line(parsed.sender(), parsed.body(), received);
        recipient.accept(line);
        ChatShareClock.Sample clock = new ChatShareClock.Sample(0, 0, received);
        assertTrue(recipient.applyBundle(List.of(), received + 1, clock).isEmpty());
        assertEquals(1, recipient.waiting(received + 1));
        List<ChatShareInbox.Hit> linked = recipient.applyBundle(
                List.of(bind(parsed.sender(), body, received, 5, "Coin Kit")),
                received + ChatShareClock.WINDOW_MS + 1,
                clock);
        assertEquals(1, linked.size());
        assertEquals(line, linked.get(0).line());
        Bind receivedBind = linked.get(0).bind();
        Component decorated = ChatShareLine.decorate(incoming, receivedBind.nameStart(), receivedBind.nameLength(),
                Style.EMPTY.withInsertion("cclive-build:" + receivedBind.shareId()));
        assertEquals("Coin Kit", textWithInsertion(decorated, "cclive-build:"));
    }

    @Test
    void iconStaysInTheColoredChatLineAndPreservesInheritedStyles() {
        HoverEvent hover = showText(Component.literal("Server stats"));
        Component colored = coloredName("firestarter03", hover).component;
        Component date = Component.empty().setStyle(Style.EMPTY.withInsertion("1795097873483"));
        Component incoming = Component.empty().append(date).append(
                Component.empty().setStyle(Style.EMPTY.withColor(0x7FE4CA).withHoverEvent(hover))
                        .append(Component.literal("⸩ ")).append(colored)
                        .append(Component.literal(" >> test Coin Kit")));
        Component withIcon = ChatLineIcons.add(incoming, "firestarter03");
        assertEquals("⸩ firestarter03 Ѳ >> test Coin Kit", withIcon.getString());
        assertEquals("1795097873483", ChatShareLine.dateInsertion(withIcon));
        assertEquals("firestarter03", ChatShareLine.parse(withIcon).sender());
        assertEquals("test Coin Kit", ChatShareLine.parse(withIcon).body());
        assertNotNull(hoverOnText(withIcon, "f"));
        assertNotNull(hoverOnText(withIcon, " >> test Coin Kit"));
        assertEquals(withIcon.getString(), ChatLineIcons.add(withIcon, "firestarter03").getString());

        Component shared = ChatShareLine.decorate(withIcon, 5, "Coin Kit".length(),
                Style.EMPTY.withInsertion("cclive-build:BUILD-abc123"));
        assertEquals("Coin Kit", textWithInsertion(shared, "cclive-build:"));
        assertEquals(withIcon.getString(), shared.getString());
        assertEquals("1795097873483", ChatShareLine.dateInsertion(shared));
    }

    @Test
    void replacesTheSinglePlaceholderAndKeepsTheNameSpan() {
        ChatShareTexts.Replacement replacement = ChatShareTexts.replace(
                "testnachricht !build für einen test", null, "Coin Kit");

        assertTrue(replacement.share());
        assertEquals("testnachricht Coin Kit für einen test", replacement.text());
        assertEquals(14, replacement.nameStart());
        assertEquals("Coin Kit", replacement.text().substring(replacement.nameStart(), replacement.nameStart() + replacement.nameLength()));
    }

    @Test
    void countsTheNameInUtf16AndRejectsMoreThan256Characters() {
        ChatShareTexts.Replacement unicode = ChatShareTexts.replace("!item", "😀 Kit", null);
        assertEquals(0, unicode.nameStart());
        assertEquals("😀 Kit".length(), unicode.nameLength());
        assertEquals("😀 Kit", unicode.text());

        String longName = "N".repeat(250);
        ChatShareTexts.Replacement tooLong = ChatShareTexts.replace("prefix !build", null, longName);
        assertEquals(ChatShareTexts.TOO_LONG, tooLong.error());
        assertEquals("N".repeat(256), ChatShareTexts.replace("!build", null, "N".repeat(256)).text());
    }

    @Test
    void stripsExactlyOneServerSpaceAfterTheArrow() {
        assertEquals(" hello", ChatShareTexts.bodyAfterArrow("Name >>  hello"));
        assertEquals("hello", ChatShareTexts.bodyAfterArrow("Name >> hello"));
        assertEquals("TyraxHD", ChatShareTexts.senderName("⸭ ⶐ TyraxHD >> Nachricht"));
        assertEquals("firestarter03", ChatShareTexts.senderName("firestarter03 Ѳ >> ."));
    }

    @Test
    void twoWaitingSharesAndALaterPlainCopyPublishNothing() {
        ChatShareAttempts attempts = new ChatShareAttempts();
        long now = 1_000L;
        attempts.addShare("gleich", "BUILD", "BUILD-abc123", "Kit", 0, 3, now + 1);
        assertNull(attempts.confirmEcho("gleich", now + 2));
        assertEquals(2, attempts.waitingCount());

        ChatShareAttempts mixed = new ChatShareAttempts();
        mixed.addShare("gleich", "ITEM", "ITEM-abc123", "Schwert", 0, 7, now);
        mixed.addPlain("gleich", now + 5);
        assertNull(mixed.confirmEcho("gleich", now + 6));
        assertEquals(2, mixed.waitingCount());

        ChatShareAttempts retry = new ChatShareAttempts();
        ChatShareAttempts.Attempt once = retry.addShare("einzig", "ITEM", "ITEM-abc123", "Schwert", 0, 7, now);
        ChatShareAttempts.Attempt confirmed = retry.confirmEcho("einzig", now + 1);
        assertEquals(once.attemptId(), confirmed.attemptId());
        assertNull(retry.confirmEcho("einzig", now + 2));

        ChatShareAttempts silent = new ChatShareAttempts();
        silent.addShare("weg", "ITEM", "ITEM-abc123", "Schwert", 0, 7, now);
        assertNull(silent.confirmEcho("weg", now + ChatShareAttempts.LOCAL_TTL_MS + 1));
        assertEquals(0, silent.waitingCount());
    }

    @Test
    void spanHashAndDuplicateBindsStayPlain() {
        String body = "testnachricht Coin Kit für einen test";
        ChatShareClock.Sample clock = new ChatShareClock.Sample(0, 0, 1_000L);
        Bind bind = bind("TyraxHD", body, 1_000L, 14, "Coin Kit");
        ChatShareCatalog catalog = new ChatShareCatalog();
        catalog.put(bind);
        assertEquals(1, catalog.matching("TyraxHD", body, 1_000L, clock).size());
        assertEquals(0, catalog.matching("TyraxHD", body + " ", 1_000L, clock).size());
        assertEquals(0, catalog.matching("TyraxHD", body, 1_000L, null).size());
        assertEquals(0, catalog.matching("andere", body, 1_000L, clock).size());

        catalog.put(bind("TyraxHD", body, 1_000L, 14, "Coin Kit"));
        assertEquals(2, catalog.matching("TyraxHD", body, 1_000L, clock).size());
        assertFalse(ChatShareTexts.spanMatches(body, 14, "Anderes".length(), "Anderes"));
    }

    @Test
    void oneBundleCoversManyLinesAndAnEmptyBundleKeepsTheRing() {
        ChatShareInbox inbox = new ChatShareInbox();
        long now = 5_000L;
        ChatShareClock.Sample clock = new ChatShareClock.Sample(0, 0, now);
        Line first = new Line("TyraxHD", "eins Coin Kit", now);
        Line second = new Line("TyraxHD", "zwei Coin Kit", now);
        Line ordinary = new Line("TyraxHD", "nur text", now);
        inbox.accept(first);
        inbox.accept(second);
        inbox.accept(ordinary);
        assertEquals(3, inbox.waiting(now));

        assertTrue(inbox.applyBundle(List.of(), now + 1, clock).isEmpty());
        assertEquals(3, inbox.waiting(now + 1));

        List<ChatShareInbox.Hit> linked = inbox.applyBundle(List.of(
                bind("TyraxHD", "eins Coin Kit", now, 5, "Coin Kit"),
                bind("TyraxHD", "zwei Coin Kit", now, 5, "Coin Kit")), now + 2, clock);
        assertEquals(1, inbox.waiting(now + 2));
        assertEquals(2, linked.size());
        assertEquals(1, inbox.waiting(now + ChatShareClock.WINDOW_MS + 1));
        assertEquals(0, inbox.waiting(now + ChatShareRing.HOLD_MS + 3));
    }

    @Test
    void vanillaLineGetsTheHoverOnTheNameWithoutAnArrow() {
        Component line = Component.literal("<Dev> test Coin Kit");
        Style link = Style.EMPTY.withInsertion("cclive-share:ITEM-abc123")
                .withHoverEvent(showText(Component.literal("Coin Kit\nSelten")));
        Component decorated = ChatShareLine.decorateRange(line, "<Dev> test ".length(), "Coin Kit".length(), link);

        assertEquals(line.getString(), decorated.getString());
        assertEquals("Coin Kit", textWithInsertion(decorated, "cclive-share:"));
        assertNotNull(hoverOnText(decorated, "Coin Kit"));
    }

    @Test
    void onlyTheEmptyDateBeforeTheArrowIsTheTimestamp() {
        Component date = Component.empty().setStyle(Style.EMPTY
                .withInsertion("1795083778607")
                .withHoverEvent(showText(Component.literal("11/19/2026"))));
        Component bodyInsertion = Component.literal("danach").setStyle(Style.EMPTY.withInsertion("1795097873483"));
        Component line = Component.empty().append(date).append(Component.literal("TyraxHD >> ")).append(bodyInsertion);

        assertEquals("1795083778607", ChatShareLine.dateInsertion(line));

        Component secondDate = Component.empty().setStyle(Style.EMPTY.withInsertion("1795083778608"));
        Component doubled = Component.empty().append(date).append(secondDate).append(Component.literal("TyraxHD >> hallo"));
        assertNull(ChatShareLine.dateInsertion(doubled));
        assertNull(ChatShareLine.dateInsertion(Component.literal("TyraxHD >> hallo")));
    }

    @Test
    void tyraxLineKeepsDatePrefixAndNameHover() {
        HoverEvent dateHover = showText(Component.literal("11/19/2026"));
        HoverEvent nameHover = showText(Component.literal("[Kaktusrang] 377"));
        Component date = Component.empty().setStyle(Style.EMPTY.withInsertion("1795083778607").withHoverEvent(dateHover));
        Component name = Component.literal("TyraxHD").setStyle(Style.EMPTY.withHoverEvent(nameHover));
        String body = "testnachricht Coin Kit für einen test";
        Component line = Component.empty()
                .append(date)
                .append(Component.literal("⸭"))
                .append(Component.literal(" "))
                .append(Component.literal("ⶐ"))
                .append(Component.literal(" "))
                .append(name)
                .append(Component.literal(" >> "))
                .append(Component.literal(body));

        ChatShareLine.Parsed parsed = ChatShareLine.parse(line);
        assertEquals("TyraxHD", parsed.sender());
        assertEquals(body, parsed.body());
        assertEquals("1795083778607", parsed.timestamp());

        Style link = Style.EMPTY.withInsertion("cclive-build:BUILD-abc123").withHoverEvent(showText(Component.literal("Coin Kit")));
        Component decorated = ChatShareLine.decorate(line, 14, "Coin Kit".length(), link);

        assertEquals(line.getString(), decorated.getString());
        assertEquals("Coin Kit", textWithInsertion(decorated, "cclive-build:"));
        assertEquals("1795083778607", insertionOnEmpty(decorated));
        assertNotNull(hoverOnText(decorated, "TyraxHD"));
        assertEquals("", insertionOnText(decorated, "TyraxHD"));
        assertEquals("", insertionOnText(decorated, "⸭"));
        assertEquals("", insertionOnText(decorated, "ⶐ"));
    }

    @Test
    void coloredNameWithIconKeepsTheIconOutsideTheSpan() {
        HoverEvent nameHover = showText(Component.literal("Stats"));
        Component date = Component.empty().setStyle(Style.EMPTY.withInsertion("1795084068210"));
        MutableName name = coloredName("firestarter03", nameHover);
        Component line = Component.empty()
                .append(date)
                .append(name.component)
                .append(Component.literal(" "))
                .append(Component.literal("Ѳ"))
                .append(Component.literal(">>"))
                .append(Component.literal(" "))
                .append(Component.literal("test Coin Kit"));

        ChatShareLine.Parsed parsed = ChatShareLine.parse(line);
        assertEquals("firestarter03", parsed.sender());
        assertEquals("test Coin Kit", parsed.body());
        assertEquals("1795084068210", parsed.timestamp());

        Style link = Style.EMPTY.withInsertion("cclive-share:ITEM-abc123").withHoverEvent(showText(Component.literal("Coin Kit")));
        Component decorated = ChatShareLine.decorate(line, 5, "Coin Kit".length(), link);
        assertEquals("Coin Kit", textWithInsertion(decorated, "cclive-share:"));
        assertEquals(nameHover, hoverOnText(decorated, "f"));
        assertEquals("", insertionOnText(decorated, "Ѳ"));
    }

    private static int attemptSeq;

    private static Bind bind(String player, String body, long acceptedAt, int start, String name) {
        return new Bind(++attemptSeq, "attempt-" + attemptSeq, player, ChatShareTexts.sha256(body),
                name, acceptedAt, "BUILD", "BUILD-abc123", start, name.length(), null);
    }

    private record MutableName(Component component) {
    }

    private static MutableName coloredName(String name, HoverEvent hover) {
        Component out = Component.empty();
        for (int i = 0; i < name.length(); i++) {
            out = ((net.minecraft.network.chat.MutableComponent) out)
                    .append(Component.literal(name.substring(i, i + 1)).setStyle(Style.EMPTY.withHoverEvent(hover)));
        }
        return new MutableName(out);
    }

    private static String textWithInsertion(Component root, String prefix) {
        StringBuilder text = new StringBuilder();
        collectInsertion(root, prefix, text);
        return text.toString();
    }

    private static void collectInsertion(Component component, String prefix, StringBuilder text) {
        if (component == null) {
            return;
        }
        String insertion = component.getStyle() == null ? null : component.getStyle().getInsertion();
        if (insertion != null && insertion.startsWith(prefix)) {
            String own = own(component);
            if (!own.isEmpty()) {
                text.append(own);
            }
        }
        for (Component sibling : component.getSiblings()) {
            collectInsertion(sibling, prefix, text);
        }
    }

    private static String insertionOnEmpty(Component root) {
        List<String> found = new ArrayList<>();
        walkEmpty(root, found);
        return found.size() == 1 ? found.get(0) : null;
    }

    private static void walkEmpty(Component component, List<String> found) {
        if (component == null) {
            return;
        }
        String insertion = component.getStyle() == null ? null : component.getStyle().getInsertion();
        if (own(component).isEmpty() && insertion != null && insertion.length() == 13) {
            found.add(insertion);
        }
        for (Component sibling : component.getSiblings()) {
            walkEmpty(sibling, found);
        }
    }

    private static HoverEvent hoverOnText(Component root, String text) {
        return hover(root, text);
    }

    private static HoverEvent hover(Component component, String text) {
        if (component == null) {
            return null;
        }
        if (text.equals(own(component)) && component.getStyle() != null && component.getStyle().getHoverEvent() != null) {
            return component.getStyle().getHoverEvent();
        }
        for (Component sibling : component.getSiblings()) {
            HoverEvent nested = hover(sibling, text);
            if (nested != null) {
                return nested;
            }
        }
        return null;
    }

    private static String insertionOnText(Component root, String text) {
        return insertion(root, text);
    }

    private static String insertion(Component component, String text) {
        if (component == null) {
            return null;
        }
        if (text.equals(own(component))) {
            String insertion = component.getStyle() == null ? null : component.getStyle().getInsertion();
            return insertion == null ? "" : insertion;
        }
        for (Component sibling : component.getSiblings()) {
            String nested = insertion(sibling, text);
            if (nested != null) {
                return nested;
            }
        }
        return null;
    }

    private static String own(Component component) {
        String full = component.getString();
        if (full == null) {
            return "";
        }
        StringBuilder siblings = new StringBuilder();
        for (Component sibling : component.getSiblings()) {
            String part = sibling.getString();
            if (part != null) {
                siblings.append(part);
            }
        }
        String suffix = siblings.toString();
        if (!suffix.isEmpty() && full.endsWith(suffix)) {
            return full.substring(0, full.length() - suffix.length());
        }
        return component.getSiblings().isEmpty() ? full : "";
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
        throw new AssertionError("Show-Text-Hover konnte nicht erzeugt werden");
    }
}
