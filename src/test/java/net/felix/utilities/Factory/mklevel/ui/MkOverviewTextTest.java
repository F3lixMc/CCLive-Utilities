package net.felix.utilities.Factory.mklevel.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MkOverviewTextTest {
    @Test
    void lineShowsEssenceAndWaveOrSaysThePlaceIsUnknown() {
        assertEquals("MK 3 — Pferd T1 ×12 — Welle 4", MkOverviewText.line(3, "Pferd T1", 12, "4"));
        assertTrue(MkOverviewText.line(1, "Unbekannt T1", 1, "?").contains("Fundort unbekannt"));
        assertTrue(MkOverviewText.line(1, "Unbekannt T1", 1, null).contains("Fundort unbekannt"));
        assertTrue(MkOverviewText.line(2, " ", 1, "2").contains("Essenz unbekannt"));
    }
}
