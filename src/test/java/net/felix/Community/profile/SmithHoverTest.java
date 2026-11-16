package net.felix.Community.profile;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SmithHoverTest {
    @Test
    void settingsOfferSmithLevelAndTheHoverUsesTheHammerOrPlaytime() {
        assertEquals("Schmiedelevel", PlayerHoverStatsUtility.HoverStatsType.SMITH_LEVEL.getDisplayName());
        assertEquals("smith_level", PlayerHoverStatsUtility.HoverStatsType.SMITH_LEVEL.getValue());
        assertEquals(
            PlayerHoverStatsUtility.HoverStatsType.SMITH_LEVEL,
            PlayerHoverStatsUtility.HoverStatsType.fromString("smith_level"));

        JsonObject captured = new JsonObject();
        captured.addProperty("smith_level", "10");
        assertEquals("§f⚒ §f10", PlayerHoverStatsUtility.hoverStatLine(captured, "Spielzeit"));

        JsonObject zero = new JsonObject();
        zero.addProperty("smith_level", "0");
        assertEquals("§f⚒ §f0", PlayerHoverStatsUtility.smithLevelLine(zero));

        assertNull(PlayerHoverStatsUtility.smithLevelLine(new JsonObject()));
        JsonObject missing = new JsonObject();
        missing.add("smith_level", JsonNull.INSTANCE);
        assertEquals("Spielzeit", PlayerHoverStatsUtility.hoverStatLine(missing, "Spielzeit"));
    }

    @Test
    void missingRanksStayAtZeroUntilALocalCacheValueExists() {
        assertArrayEquals(new int[]{0, 0}, PlayerHoverStatsUtility.getCachedRanks("RankFallbackHero"));
        PlayerHoverStatsUtility.cacheRanks("RankFallbackHero", 3, 4);
        assertArrayEquals(new int[]{3, 4}, PlayerHoverStatsUtility.getCachedRanks("rankfallbackhero"));
    }
}
