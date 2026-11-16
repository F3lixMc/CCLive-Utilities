package net.felix.utilities.Community.chat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatIgnoreLayoutTest {
    private static final int WIDTH = 400;
    private static final int HEIGHT = 240;
    private static final List<String> NAMES = names(40);
    private static final List<ChatIgnoreRules.Match> SUGGESTIONS = suggestions(20);

    @Test
    void listStartsUnderTheSearchFieldAndBothLongListsScroll() {
        ChatIgnoreLayout layout = new ChatIgnoreLayout();
        assertEquals(ChatIgnoreLayout.FIELD_Y + ChatIgnoreLayout.FIELD_HEIGHT + ChatIgnoreLayout.LIST_GAP, layout.listTop());
        assertTrue(layout.listTop() < ChatIgnoreLayout.DROPDOWN_TOP + ChatIgnoreLayout.VISIBLE_SUGGESTIONS * ChatIgnoreLayout.DROPDOWN_ROW);

        int fieldY = layout.fieldY();
        int closeY = layout.closeY(HEIGHT);
        int onSuggestions = ChatIgnoreLayout.fieldX(WIDTH) + 8;
        int onRemove = ChatIgnoreLayout.fieldX(WIDTH) + ChatIgnoreLayout.NAME_WIDTH + ChatIgnoreLayout.BUTTON_GAP + 4;
        int belowDropdown = layout.listRowY(5) + 4;

        for (int step = 0; step < 14; step++) {
            assertTrue(layout.scroll(onSuggestions, ChatIgnoreLayout.DROPDOWN_TOP + 2, -1, WIDTH, HEIGHT, SUGGESTIONS.size(), NAMES.size()));
        }
        assertEquals(14, layout.suggestionScroll());
        assertEquals(0, layout.listScroll());
        assertEquals(19, layout.suggestionIndex(onSuggestions, ChatIgnoreLayout.DROPDOWN_TOP + 5 * ChatIgnoreLayout.DROPDOWN_ROW + 2,
            WIDTH, SUGGESTIONS.size(), HEIGHT));
        assertTrue(layout.scroll(onSuggestions, ChatIgnoreLayout.DROPDOWN_TOP + 2, -1, WIDTH, HEIGHT, SUGGESTIONS.size(), NAMES.size()));
        assertEquals(14, layout.suggestionScroll());

        for (int step = 0; step < 10; step++) {
            assertTrue(layout.scroll(onRemove, belowDropdown, -1, WIDTH, HEIGHT, SUGGESTIONS.size(), NAMES.size()));
        }
        assertEquals(10, layout.listScroll());
        assertEquals(14, layout.suggestionScroll());
        assertEquals(15, layout.listIndex(onRemove, belowDropdown, WIDTH, NAMES.size(), HEIGHT));
        assertEquals("P15", NAMES.get(layout.listIndex(onRemove, belowDropdown, WIDTH, NAMES.size(), HEIGHT)));
        assertEquals(-1, layout.suggestionIndex(onRemove, belowDropdown, WIDTH, SUGGESTIONS.size(), HEIGHT));

        int coveredClick = layout.suggestionIndex(onRemove, layout.listRowY(0) + 4, WIDTH, SUGGESTIONS.size(), HEIGHT);
        assertEquals(14, coveredClick);
        assertTrue(layout.listSlotCovered(0, SUGGESTIONS.size(), HEIGHT));
        assertFalse(layout.listSlotCovered(5, SUGGESTIONS.size(), HEIGHT));

        assertEquals(fieldY, layout.fieldY());
        assertEquals(closeY, layout.closeY(HEIGHT));
        assertFalse(layout.scroll(ChatIgnoreLayout.fieldX(WIDTH) + 8, layout.fieldY() + 4, -1, WIDTH, HEIGHT, SUGGESTIONS.size(), NAMES.size()));
    }

    @Test
    void scrollingLeavesTheSearchFieldAndEnterPathUntouched() {
        ChatIgnoreTyping typing = new ChatIgnoreTyping();
        typing.replaceText("Steve", 0, 5);
        Object field = typing.field();
        ChatIgnoreLayout layout = new ChatIgnoreLayout();
        int x = ChatIgnoreLayout.fieldX(WIDTH) + 8;
        for (int step = 0; step < 8; step++) {
            layout.scroll(x, ChatIgnoreLayout.DROPDOWN_TOP + 2, -1, WIDTH, HEIGHT, SUGGESTIONS.size(), NAMES.size());
            layout.scroll(x, layout.listRowY(5) + 4, -1, WIDTH, HEIGHT, 0, NAMES.size());
        }

        assertSame(field, typing.field());
        assertEquals("Steve", typing.text());
        assertEquals(0, typing.cursor());
        assertEquals(5, typing.selectionAnchor());

        typing.replaceText("Steven", 6, 6);
        assertEquals(6, typing.cursor());
        assertEquals(6, typing.selectionAnchor());
        List<ChatIgnoreRules.Match> confirmed = List.of(new ChatIgnoreRules.Match("Steven", 2L, false));
        assertNull(typing.submit(confirmed, "Nora", 9L));
        assertEquals(ChatIgnoreRules.SELF, typing.submit(confirmed, "steven", 9L));
    }

    private static List<String> names(int count) {
        List<String> names = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            names.add("P" + i);
        }
        return List.copyOf(names);
    }

    private static List<ChatIgnoreRules.Match> suggestions(int count) {
        List<ChatIgnoreRules.Match> matches = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            matches.add(new ChatIgnoreRules.Match("T" + i, i + 1L, false));
        }
        return List.copyOf(matches);
    }
}
