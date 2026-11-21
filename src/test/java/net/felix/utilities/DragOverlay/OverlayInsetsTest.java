package net.felix.utilities.DragOverlay;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OverlayInsetsTest {

    @Test
    void contentScrollAndFrameShareTheSameInset() {
        int inset = 2;
        int contentTop = 20;
        int box = 166;
        int span = OverlayInsets.contentSpan(box, contentTop, inset);
        assertEquals(box - inset, contentTop + span);
        assertEquals(inset, box - (contentTop + span));
    }

    @Test
    void lastLineDoesNotKeepAFullSlot() {
        int padding = 5;
        int slot = 12;
        int font = 9;
        int height = OverlayInsets.heightForLines(padding, slot, font, 5);
        int lastGlyphBottom = padding + 4 * slot + font;
        assertEquals(padding, height - lastGlyphBottom);
        assertEquals(padding, lastGlyphBottom - (4 * slot + font));
    }
}
