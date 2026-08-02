package net.felix.utilities.Overall;

import net.minecraft.client.Minecraft;

/**
 * Hilfen für die aktuelle Client-Welt / Dimension.
 */
public final class DimensionUtility {

    private DimensionUtility() {
    }

    /**
     * @return true wenn der Spieler in {@code minecraft:general_lobby} ist (Pfad {@code general_lobby})
     */
    public static boolean isInGeneralLobby(Minecraft client) {
        if (client == null || client.level == null) {
            return false;
        }
        return "general_lobby".equals(client.level.dimension().identifier().getPath());
    }
}
