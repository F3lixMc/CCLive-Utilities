package net.felix.chat;

import net.felix.utilities.ItemViewer.ItemData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MagnetModuleNamesTest {
    @Test
    void romanTiersMapToJeiNames() {
        assertEquals("Kupfermagnet", MagnetModuleNames.jeiName("Magnet [Modul]", tooltip("Stufe: [I]")));
        assertEquals("Eisenmagnet", MagnetModuleNames.jeiName("Magnet", tooltip("Stufe: [II]")));
        assertEquals("Goldmagnet", MagnetModuleNames.jeiName("Magnet [Modul]", tooltip("Stufe: [III]")));
        assertEquals("Diamantmagnet", MagnetModuleNames.jeiName("Magnet [Modul]", tooltip("Stufe: [IV]")));
    }

    @Test
    void unknownTierAndOtherModulesStayUnchanged() {
        assertNull(MagnetModuleNames.jeiName("Magnet [Modul]", tooltip("Stufe: [V]")));
        assertNull(MagnetModuleNames.jeiName("Magnet [Modul]", tooltip("Zieht Gegner an")));
        assertNull(MagnetModuleNames.jeiName("Erzbrecher [Modul]", tooltip("Stufe: [I]")));
    }

    @Test
    void applyRewritesTheNameAndKeepsTheFirstLineColor() {
        ItemData data = new ItemData();
        data.name = "Magnet [Modul]";
        ChatTooltipLine name = line("Magnet [Modul]", 0x55FF55);
        ChatTooltipLine tier = line("Stufe: [IV]", 0xAAAAAA);
        data.chatTooltip = List.of(name, tier);

        MagnetModuleNames.apply(data);

        assertEquals("Diamantmagnet", data.name);
        assertEquals("Diamantmagnet", name.parts.get(0).text);
        assertEquals(0x55FF55, name.parts.get(0).color);
        assertEquals("Stufe: [IV]", tier.plainText());
    }

    private static List<ChatTooltipLine> tooltip(String stufe) {
        return List.of(line("Magnet [Modul]", null), line(stufe, null));
    }

    private static ChatTooltipLine line(String text, Integer color) {
        ChatTooltipLine line = new ChatTooltipLine();
        ChatTooltipLine.ChatTooltipPart part = new ChatTooltipLine.ChatTooltipPart();
        part.text = text;
        part.color = color;
        line.parts.add(part);
        return line;
    }
}
