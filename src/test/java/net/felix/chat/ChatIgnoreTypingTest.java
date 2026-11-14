package net.felix.chat;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatIgnoreTypingTest {
    private static final List<ChatIgnoreRules.Match> MATCHES = List.of(
        new ChatIgnoreRules.Match("Steve", 1L, false),
        new ChatIgnoreRules.Match("Steven", 2L, false)
    );

    @Test
    void fastTypingKeepsTheFieldAndDropsSlowOlderAnswers() {
        ChatIgnoreTyping typing = new ChatIgnoreTyping();
        Object field = typing.field();
        typing.replaceText("S", 1, 1);
        long first = typing.generation();
        typing.replaceText("St", 2, 2);
        long second = typing.generation();
        typing.replaceText("Ste", 3, 1);
        long third = typing.generation();

        assertFalse(typing.offer(first, "S", MATCHES, null));
        assertFalse(typing.offer(second, "St", MATCHES, null));
        assertFalse(ChatPlayerSearch.isCurrent(first, "S", third, "Ste"));
        assertTrue(ChatPlayerSearch.isCurrent(third, "Ste", third, "Ste"));
        assertTrue(typing.offer(third, "Ste", MATCHES, null));

        assertSame(field, typing.field());
        assertEquals("Ste", typing.text());
        assertEquals(3, typing.cursor());
        assertEquals(1, typing.selectionAnchor());
        assertEquals(List.of("Steve", "Steven"), typing.suggestions().stream().map(ChatIgnoreRules.Match::name).toList());
    }

    @Test
    void selectionSurvivesSuggestionsBeforeAndAfterTheyArrive() {
        ChatIgnoreTyping typing = new ChatIgnoreTyping();
        typing.replaceText("Steve", 5, 5);
        long before = typing.generation();
        assertFalse(typing.offer(before, "Stev", MATCHES, null));

        typing.replaceText("Steve", 0, 5);
        Object field = typing.field();
        long selected = typing.generation();
        assertTrue(typing.offer(selected, "Steve", MATCHES, null));
        assertSame(field, typing.field());
        assertEquals(0, typing.cursor());
        assertEquals(5, typing.selectionAnchor());

        typing.replaceText("Steven", 6, 6);
        assertFalse(typing.offer(selected, "Steve", MATCHES, null));
        assertEquals("Steven", typing.text());
        assertEquals(6, typing.cursor());
        assertEquals(6, typing.selectionAnchor());
    }

    @Test
    void enterAndButtonConfirmTheSameAccountAndAClickOnlyFillsTheField() {
        ChatIgnoreTyping typing = new ChatIgnoreTyping();
        typing.replaceText("stev", 4, 4);
        typing.pick(MATCHES.get(1));
        assertEquals("Steven", typing.text());
        assertEquals(0, typing.submits());

        assertNull(typing.submit(MATCHES, "Nora", 9L));
        typing.replaceText("Steve", 5, 5);
        assertEquals(ChatIgnoreRules.SELF, typing.submit(MATCHES, "steve", 9L));
        typing.replaceText("Niemand", 8, 8);
        assertEquals(ChatIgnoreRules.UNKNOWN, typing.submit(MATCHES, "Nora", 9L));
        assertEquals(3, typing.submits());
        assertEquals(ChatPlayerSearch.DELAY_MS, 200L);
    }
}
