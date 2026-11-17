package net.felix.Community.leaderboards.panel;

import com.google.gson.JsonObject;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmithSkillTest {
    @Test
    void widgetLineKeepsCurrentXpAndLevelAndDropsTheTarget() {
        SmithSkill.Stand stand = SmithSkill.read(List.of("[Schmiede Skill]", "10 / 20 XP [3]"));
        assertEquals(3, stand.level());
        assertEquals(10L, stand.xp());
        assertEquals("3/10", SmithSkill.display(stand));
        assertEquals("Schmiedeskill gelesen: Level 3, XP 10",
            SmithSkill.diagnose(List.of("[Schmiede Skill]", "10 / 20 XP [3]")));

        SmithSkill.Stand suffixed = SmithSkill.read(List.of("[Schmiede Skill]", "1.000M / 2.000M XP [8]"));
        assertEquals(8, suffixed.level());
        assertEquals(1_000_000L, suffixed.xp());
        assertEquals("8/1.000M", SmithSkill.display(suffixed));
    }

    @Test
    void commaThousandsSeparatorParsesAsWholeNumber() {
        SmithSkill.Stand stand = SmithSkill.read(List.of("[Schmiede Skill]", "878 / 1,310 XP [21]"));
        assertEquals(21, stand.level());
        assertEquals(878L, stand.xp());
        assertEquals("21/878", SmithSkill.display(stand));
        assertEquals(1_310L, LeaderboardNumbers.parseMagnitude("1,310"));
    }

    @Test
    void oneMillionSuffixParsesAndFormatsBack() {
        SmithSkill.Stand stand = SmithSkill.read(List.of("[Schmiede Skill]", "1.000M / 2.500M [8]"));
        assertEquals(8, stand.level());
        assertEquals(1_000_000L, stand.xp());
        assertEquals("1.000M", LeaderboardNumbers.magnitude(stand.xp()));
        assertEquals("8/1.000M", SmithSkill.display(stand));
    }

    @Test
    void levelOneHundredWithTheWidestXpFitsTheValueColumn() {
        long xp = 999_999_000_000_000_000L;
        assertEquals("100/999.999Q", SmithSkill.display(100, xp));
        assertTrue(PixelFont57.width("100/999.999Q") <= PixelFont57.ScreenGrid.VALUE_BOX);
    }

    @Test
    void zeroXpWithALevelIsARealStand() {
        SmithSkill.Stand stand = SmithSkill.read(List.of("Name", "[Schmiede Skill]", "", "§a0 / 100 [6]"));
        assertEquals(6, stand.level());
        assertEquals(0L, stand.xp());
        assertEquals("6/0", SmithSkill.display(stand));
    }

    @Test
    void missingLineOrForeignWidgetCreatesNothing() {
        assertNull(SmithSkill.read(List.of("[Schmiede Skill]")));
        assertNull(SmithSkill.read(List.of("[Schmiede Skill]", "[Forschung]", "1 / 2 [4]")));
        assertNull(SmithSkill.read(List.of("[Forschung]", "1.000M / 2.000M [3]")));
        assertNull(SmithSkill.parseValue("9223.373Q / 1 [1]"));
        assertNull(SmithSkill.parseValue("1 / 1 [2147483648]"));
        assertEquals("Schmiedeskill: nächste Zeile ist ein anderes Widget: [Forschung]",
            SmithSkill.diagnose(List.of("[Schmiede Skill]", "[Forschung]", "1 / 2 [4]")));
        assertEquals("Schmiedeskill: Wertezeile ungültig: Ada",
            SmithSkill.diagnose(List.of("[Schmiede Skill]", "Ada")));
    }

    @Test
    void levelTenWithOneXpStaysTheSameDecimalStrings() {
        SmithSkill.Stand stand = SmithSkill.read(List.of("[Schmiede Skill]", "1 / 9 [10]"));
        assertEquals("10", SmithSkill.levelText(stand.level()));
        assertEquals("1", SmithSkill.xpText(stand.xp()));
        JsonObject body = new JsonObject();
        body.addProperty("board", SmithSkill.BOARD);
        body.addProperty("level", SmithSkill.levelText(stand.level()));
        body.addProperty("xp", SmithSkill.xpText(stand.xp()));
        assertEquals("10", body.get("level").getAsString());
        assertEquals("1", body.get("xp").getAsString());
        assertTrue(body.get("xp").toString().startsWith("\""));
        assertEquals(Long.MAX_VALUE, LeaderboardNumbers.parseMagnitude(Long.toString(Long.MAX_VALUE)));
    }
}
