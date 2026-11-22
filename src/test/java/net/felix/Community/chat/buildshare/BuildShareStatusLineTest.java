package net.felix.Community.chat.buildshare;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildShareStatusLineTest {
    @Test
    void equipmentLineEndsAtTheCapacity() {
        String line = BuildShareManager.equipmentRecognizedLine(4);
        assertEquals("Ausrüstung erkannt - 4/10", line);
        assertTrue(line.endsWith("/10"));
        assertFalse(line.contains("belegt"));
        assertFalse(line.contains("..."));
        assertFalse(line.contains("…"));
        assertEquals("Ausrüstung erkannt - 0/10", BuildShareManager.equipmentRecognizedLine(0));
        assertEquals("Ausrüstung erkannt - 10/10", BuildShareManager.equipmentRecognizedLine(10));
    }
}
