package net.felix.Community.profile;

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

class PlayerHoverRankParseTest {

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void dateHoverBeforeTheNameDoesNotHideRanksWithoutColon() {
        Component ranks = Component.literal("[Kaktusrang] 377")
            .append(Component.literal("\n"))
            .append(Component.literal("[Seelenrang] 323"));
        Component date = Component.empty().setStyle(Style.EMPTY.withHoverEvent(showText(Component.literal("11/19/2026"))));
        Component nameLine = Component.empty().setStyle(Style.EMPTY.withHoverEvent(showText(ranks)));
        Component message = Component.empty().append(date).append(nameLine);

        assertEquals(377, PlayerHoverStatsUtility.extractKaktusrang(message));
        assertEquals(323, PlayerHoverStatsUtility.extractSeelenrang(message));
    }

    @Test
    void colonAndThousandsSeparatorStillParse() {
        Component ranks = Component.literal("[Kaktusrang]: 1,234\n[Seelenrang]: 5,678");
        Component message = Component.literal("Name").setStyle(Style.EMPTY.withHoverEvent(showText(ranks)));

        assertEquals(1234, PlayerHoverStatsUtility.extractKaktusrang(message));
        assertEquals(5678, PlayerHoverStatsUtility.extractSeelenrang(message));
    }

    @Test
    void statsHoverStopsAtTheArrow() {
        HoverEvent nameHover = showText(Component.literal("alte stats"));
        HoverEvent stats = showText(Component.literal("=== Spieler Stats ==="));
        Component line = Component.empty()
                .append(Component.literal("firestarter03").setStyle(Style.EMPTY.withHoverEvent(nameHover)))
                .append(Component.literal(" >> test Coin Kit").setStyle(Style.EMPTY.withHoverEvent(nameHover)));

        Component rewritten = PlayerHoverStatsUtility.applyStatsHoverUntilArrow(line, stats);

        assertEquals(line.getString(), rewritten.getString());
        assertEquals(stats, hoverOn(rewritten, "firestarter03"));
        assertNull(hoverOn(rewritten, " test Coin Kit"));
        assertNull(hoverOn(rewritten, "Coin Kit"));
    }

    @Test
    void statsHoverOnTheParentStopsAtTheArrowAndKeepsTheDate() {
        HoverEvent dateHover = showText(Component.literal("11/20/2026"));
        HoverEvent nameHover = showText(Component.literal("alte stats"));
        HoverEvent stats = showText(Component.literal("=== Spieler Stats ==="));
        Component date = Component.empty().setStyle(Style.EMPTY
                .withHoverEvent(dateHover)
                .withInsertion("1795140927972"));
        Component wrapper = Component.empty().setStyle(Style.EMPTY.withHoverEvent(nameHover))
                .append(Component.literal("fire2003"))
                .append(Component.literal(" "))
                .append(Component.literal("ѻ"))
                .append(Component.literal(">>"))
                .append(Component.literal(" "))
                .append(Component.literal("test Holzschwert end"));
        Component line = Component.empty().append(date).append(wrapper);

        Component rewritten = PlayerHoverStatsUtility.applyStatsHoverUntilArrow(line, stats);

        assertEquals(line.getString(), rewritten.getString());
        assertEquals(dateHover, hoverWithInsertion(rewritten, "1795140927972"));
        assertEquals(stats, hoverOn(rewritten, "fire2003"));
        assertNull(hoverOn(rewritten, "test Holzschwert end"));
        assertFalse(statsCoverTextAfterArrow(rewritten, stats));
    }

    private static boolean statsCoverTextAfterArrow(Component component, HoverEvent stats) {
        if (component == null) {
            return false;
        }
        boolean here = component.getStyle() != null && stats.equals(component.getStyle().getHoverEvent());
        if (here) {
            String plain = component.getString();
            int arrow = plain == null ? -1 : plain.indexOf(">>");
            if (arrow >= 0 && arrow + 2 < plain.length()) {
                return true;
            }
            if (arrow < 0 && plain != null && plain.contains("Holzschwert")) {
                return true;
            }
        }
        for (Component sibling : component.getSiblings()) {
            if (statsCoverTextAfterArrow(sibling, stats)) {
                return true;
            }
        }
        return false;
    }

    private static HoverEvent hoverWithInsertion(Component component, String insertion) {
        if (component == null) {
            return null;
        }
        if (component.getStyle() != null && insertion.equals(component.getStyle().getInsertion())) {
            return component.getStyle().getHoverEvent();
        }
        for (Component sibling : component.getSiblings()) {
            HoverEvent nested = hoverWithInsertion(sibling, insertion);
            if (nested != null) {
                return nested;
            }
        }
        return null;
    }

    private static HoverEvent hoverOn(Component component, String text) {
        if (component == null) {
            return null;
        }
        if (text.equals(own(component)) && component.getStyle() != null) {
            return component.getStyle().getHoverEvent();
        }
        for (Component sibling : component.getSiblings()) {
            HoverEvent nested = hoverOn(sibling, text);
            if (nested != null || text.equals(own(sibling))) {
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
        assertNotNull(null, "Show-Text-Hover konnte nicht erzeugt werden");
        return null;
    }
}
