package net.felix.chat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatBlockedGlyphsTest {
    @AfterEach
    void resetPack() {
        ChatBlockedGlyphs.replacePackCodePointsForTest(Set.of());
        ChatBlockedGlyphs.replaceServerCodePoints(Set.of());
    }

    @Test
    void germanTextAndAllowedSymbolsPass() {
        assertFalse(ChatBlockedGlyphs.containsBlockedGlyph("Hallo äöüß 123! ?:)"));
        assertFalse(ChatBlockedGlyphs.containsBlockedGlyph("Item #ITEM:abc42 und 😀"));
    }

    @Test
    void ownIconsAreAlwaysBlocked() {
        assertTrue(ChatBlockedGlyphs.containsBlockedGlyph("ѳ"));
        assertTrue(ChatBlockedGlyphs.containsBlockedGlyph("Ѳstart"));
        assertTrue(ChatBlockedGlyphs.containsBlockedGlyph("mit ѳ mitte"));
        assertTrue(ChatBlockedGlyphs.containsBlockedGlyph("ende Ѳ"));
    }

    @Test
    void packGlyphBlocksWholeMessage() {
        ChatBlockedGlyphs.replacePackCodePointsForTest(Set.of(0xE001));
        String glyph = new String(Character.toChars(0xE001));
        assertTrue(ChatBlockedGlyphs.containsBlockedGlyph(glyph + " hallo"));
        assertTrue(ChatBlockedGlyphs.containsBlockedGlyph("ha " + glyph + " llo"));
        assertTrue(ChatBlockedGlyphs.containsBlockedGlyph("hallo " + glyph));
        assertFalse(ChatBlockedGlyphs.containsBlockedGlyph("hallo"));
    }

    @Test
    void severalBlockedGlyphsStillOneDetection() {
        assertTrue(ChatBlockedGlyphs.containsBlockedGlyph("ѳ und Ѳ"));
        assertEquals(
            ChatBlockedGlyphs.ERROR_MESSAGE,
            ChatBlockedGlyphs.clientErrorMessage(ChatBlockedGlyphs.ERROR_CODE));
    }

    @Test
    void failedPackReloadDoesNotClearPreviousList() {
        ChatBlockedGlyphs.replacePackCodePointsForTest(Set.of(0xE050));
        String glyph = new String(Character.toChars(0xE050));
        assertTrue(ChatBlockedGlyphs.containsBlockedGlyph(glyph));
        ChatBlockedGlyphs.replacePackCodePointsForTest(Set.of(0xE050));
        assertTrue(ChatBlockedGlyphs.containsBlockedGlyph(glyph));
    }

    @Test
    void supplementaryPlaneIsCheckedWithoutBanningAllAstralChars() {
        ChatBlockedGlyphs.replacePackCodePointsForTest(Set.of(0x1F000));
        assertTrue(ChatBlockedGlyphs.containsBlockedGlyph(new String(Character.toChars(0x1F000))));
        assertFalse(ChatBlockedGlyphs.containsBlockedGlyph("😀"));
    }

    @Test
    void exportRoundtripKeepsCoreIcons() {
        String json = ChatBlockedGlyphs.toExportJson();
        Set<Integer> parsed = ChatBlockedGlyphs.parseCodepointsJson(json);
        assertTrue(parsed.contains(0x0473));
        assertTrue(parsed.contains(0x0472));
        assertEquals(0x0473, ChatBlockedGlyphs.parseUPlus("U+0473"));
        assertEquals(0x1F000, ChatBlockedGlyphs.parseUPlus("U+1F000"));
    }
}
