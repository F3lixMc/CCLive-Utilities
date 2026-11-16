package net.felix.Community.leaderboards.panel;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeaderboardPlacesTest {
    @Test
    void schmiedNametagIsTheCityNpcAndNothingBroader() {
        assertTrue(LeaderboardPlaces.isSchmiedName(Component.literal("Schmied")));
        assertTrue(LeaderboardPlaces.isSchmiedName(Component.literal("§6Schmied")));
        assertFalse(LeaderboardPlaces.isSchmiedName(Component.literal("Schmiede Skill")));
        assertFalse(LeaderboardPlaces.isSchmiedName(Component.literal("Schmiedeskill")));
        assertFalse(LeaderboardPlaces.isSchmiedName(null));
    }

    @Test
    void factoryDefaultsToFactoryLevelAndTheSyncSetKeepsTheOldBoards() {
        LeaderboardPlaces.Rotation rotation = LeaderboardPlaces.forPlayer(null);
        LeaderboardPlaces.Entry selected = rotation.entries().get(rotation.defaultIndex());
        assertEquals("factory_level", selected.boardId());
        assertEquals("Fabrik Level", selected.title());
        assertEquals("Level", selected.header());
        assertEquals(LeaderboardPlaces.Style.FACTORY, selected.style());
        assertEquals("factory", rotation.placeKey());
        var ids = LeaderboardPlaces.syncBoardIds();
        assertEquals(126, ids.size());
        assertTrue(ids.contains("factory_level"));
        assertTrue(ids.contains("playtime"));
        assertTrue(ids.contains("smith_skill"));
        int width = PixelFont57.width("2000");
        assertEquals(23, width);
        int left = LeaderboardPanelLayout.VALUE_TEXT_RIGHT - width + 1;
        assertTrue(left >= LeaderboardPanelLayout.VALUE_TEXT_X);
    }
}
