package net.felix.utilities.Other.playericon;

import java.util.List;
import net.felix.Community.playericon.PlayerIconUtility;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerIconUtilityTest {
    @Test
    void adminNamesUseAdminIconAndGlyph() {
        assertTrue(PlayerIconUtility.usesAdminIcon("firestarter03"));
        assertTrue(PlayerIconUtility.usesAdminIcon("FireStarter03"));
        assertTrue(PlayerIconUtility.usesAdminIcon("F3lixMc_"));
        assertTrue(PlayerIconUtility.usesAdminIcon("f3lixmc_"));
        assertFalse(PlayerIconUtility.usesAdminIcon("Steve"));
        assertFalse(PlayerIconUtility.usesAdminIcon(null));
        assertEquals(PlayerIconUtility.ADMIN_CHAT_ICON_MARKER, PlayerIconUtility.chatIconMarker("firestarter03"));
        assertEquals(PlayerIconUtility.CHAT_ICON_MARKER, PlayerIconUtility.chatIconMarker("Steve"));
        assertTrue(PlayerIconUtility.iconTextureForPlayer("F3lixMc_").getPath().endsWith("admin_icon.png"));
        assertTrue(PlayerIconUtility.iconTextureForPlayer("Steve").getPath().endsWith("icon.png"));
    }

    @Test
    void moderatorNamesUseModIconUnlessTheyAreAdmins() {
        try {
            PlayerIconUtility.replaceModeratorNames(List.of("ModUser", "firestarter03"));
            assertTrue(PlayerIconUtility.usesModIcon("ModUser"));
            assertTrue(PlayerIconUtility.usesModIcon("moduser"));
            assertFalse(PlayerIconUtility.usesModIcon("firestarter03"));
            assertFalse(PlayerIconUtility.usesModIcon("Steve"));
            assertFalse(PlayerIconUtility.usesModIcon(null));
            assertEquals(PlayerIconUtility.MOD_CHAT_ICON_MARKER, PlayerIconUtility.chatIconMarker("ModUser"));
            assertEquals(PlayerIconUtility.ADMIN_CHAT_ICON_MARKER, PlayerIconUtility.chatIconMarker("firestarter03"));
            assertTrue(PlayerIconUtility.iconTextureForPlayer("ModUser").getPath().endsWith("mod_icon.png"));
            assertTrue(PlayerIconUtility.containsChatIconMarker("Hallo ѻ"));
        } finally {
            PlayerIconUtility.replaceModeratorNames(List.of());
        }
    }
}
