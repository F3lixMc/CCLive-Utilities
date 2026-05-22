package net.felix.utilities.Overall;

import net.minecraft.client.MinecraftClient;

/**
 * Hilfen für die aktuelle Client-Welt / Dimension.
 */
public final class DimensionUtility {

    private DimensionUtility() {
    }

    /**
     * @return true wenn der Spieler in {@code minecraft:general_lobby} ist (Pfad {@code general_lobby})
     */
    public static boolean isInGeneralLobby(MinecraftClient client) {
        if (client == null || client.world == null) {
            return false;
        }
        return "general_lobby".equals(client.world.getRegistryKey().getValue().getPath());
    }
}
