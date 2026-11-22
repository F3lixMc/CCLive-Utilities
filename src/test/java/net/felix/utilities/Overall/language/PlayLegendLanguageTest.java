package net.felix.utilities.Overall.language;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PlayLegendLanguageTest {

    @Test
    void detectsTheEnglishAndGermanConfirmations() {
        assertEquals("en", PlayLegendLanguage.detect(PlayLegendLanguage.ENGLISH));
        assertEquals("de", PlayLegendLanguage.detect(PlayLegendLanguage.GERMAN));
    }

    @Test
    void ignoresColorCodesAndSurroundingSpace() {
        assertEquals("en", PlayLegendLanguage.detect(" §6[Legend]§r You set your language to English. "));
        assertEquals("de", PlayLegendLanguage.detect("§e[Legend]§f Du hast deine Sprache zu Deutsch geändert."));
    }

    @Test
    void ignoresQuotedOrPartialLines() {
        assertNull(PlayLegendLanguage.detect("Player: [Legend] You set your language to English."));
        assertNull(PlayLegendLanguage.detect("[Legend] You set your language to English"));
        assertNull(PlayLegendLanguage.detect(null));
        assertNull(PlayLegendLanguage.detect("Willkommen auf PlayLegend"));
    }
}
