package net.felix.chat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ChatNameColorsTest {
    @Test
    void extractSolidNameBeforeArrow() {
        String plain = "firestarter03 >> hallo";
        int[] rgb = fill(plain, 0x7FE4CA);
        ChatNameColors.Sample sample = ChatNameColors.extract(plain, rgb);
        assertNotNull(sample);
        assertEquals("firestarter03", sample.name);
        assertEquals(13, sample.colors.length);
        for (int color : sample.colors) {
            assertEquals(0x7FE4CA, color);
        }
    }

    @Test
    void extractPerLetterGradient() {
        String name = "Abc";
        String plain = name + " >> x";
        int[] rgb = new int[plain.length()];
        rgb[0] = 0xFF0000;
        rgb[1] = 0x00FF00;
        rgb[2] = 0x0000FF;
        for (int i = 3; i < rgb.length; i++) {
            rgb[i] = 0xFFFFFF;
        }
        ChatNameColors.Sample sample = ChatNameColors.extract(plain, rgb);
        assertNotNull(sample);
        assertEquals("Abc", sample.name);
        assertArrayEquals(new int[] { 0xFF0000, 0x00FF00, 0x0000FF }, sample.colors);
    }

    @Test
    void extractSkipsAdminIconAndSpacesBeforeArrow() {
        String plain = "Steve Ѳ >> hi";
        int[] rgb = fill(plain, 0xABCDEF);
        ChatNameColors.Sample sample = ChatNameColors.extract(plain, rgb);
        assertNotNull(sample);
        assertEquals("Steve", sample.name);
    }

    @Test
    void extractSkipsIconAndSpacesBeforeArrow() {
        String plain = "Steve ѳ >> hi";
        int[] rgb = fill(plain, 0xABCDEF);
        ChatNameColors.Sample sample = ChatNameColors.extract(plain, rgb);
        assertNotNull(sample);
        assertEquals("Steve", sample.name);
        assertEquals(5, sample.colors.length);
        assertEquals(0xABCDEF, sample.colors[0]);
    }

    @Test
    void extractIgnoresCcliveLines() {
        String plain = "[CCLive] Steve >> hi";
        assertNull(ChatNameColors.extract(plain, fill(plain, 0x123456)));
    }

    @Test
    void parseAndEncodeRoundtrip() {
        int[] colors = { 0xFF0000, 0x00AABB };
        String encoded = ChatNameColors.encode(colors);
        assertEquals("#FF0000,#00AABB", encoded);
        assertArrayEquals(colors, ChatNameColors.parse(encoded));
        assertEquals(0xFF0000, ChatNameColors.parseSolid("#ff0000"));
    }

    private static int[] fill(String plain, int color) {
        int[] rgb = new int[plain.length()];
        for (int i = 0; i < rgb.length; i++) {
            rgb[i] = color;
        }
        return rgb;
    }
}
