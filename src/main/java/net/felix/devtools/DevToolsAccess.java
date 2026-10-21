package net.felix.devtools;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Allowlist + Fabric-Dev-Umgebung für DevTools.
 * Unabhängig von ServerConnectionGate.
 */
public final class DevToolsAccess {

    private static final Set<String> ALLOWED_NAMES = Set.of(
        "firestarter03",
        "f3lixmc_",
        "fire2003"
    );

    private static UUID cachedPlayerId;
    private static Boolean cachedAllowed;

    private DevToolsAccess() {
    }

    public static void invalidate() {
        cachedPlayerId = null;
        cachedAllowed = null;
    }

    public static boolean isAllowed() {
        if (isDevelopmentEnvironment()) {
            return true;
        }

        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) {
            return false;
        }

        LocalPlayer player = client.player;
        UUID id = player.getUUID();
        if (cachedAllowed != null && id.equals(cachedPlayerId)) {
            return cachedAllowed;
        }

        boolean allowed = isNameAllowed(resolvePlayerName(player));
        cachedPlayerId = id;
        cachedAllowed = allowed;
        return allowed;
    }

    public static boolean isDevelopmentEnvironment() {
        try {
            return FabricLoader.getInstance().isDevelopmentEnvironment();
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isNameAllowed(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        return ALLOWED_NAMES.contains(name.toLowerCase(Locale.ROOT).trim());
    }

    private static String resolvePlayerName(LocalPlayer player) {
        try {
            String name = player.getGameProfile().name();
            if (name != null && !name.isEmpty()) {
                return name;
            }
        } catch (Exception ignored) {
        }
        try {
            return player.getName().getString();
        } catch (Exception e) {
            return null;
        }
    }
}
