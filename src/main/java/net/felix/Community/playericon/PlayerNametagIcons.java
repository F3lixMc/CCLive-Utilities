package net.felix.Community.playericon;

import java.util.regex.Pattern;
import net.felix.CCLiveUtilities;
import net.felix.CCLiveUtilitiesConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class PlayerNametagIcons {
    private static final FontDescription.Resource ICON_FONT = new FontDescription.Resource(
            Identifier.fromNamespaceAndPath(CCLiveUtilities.MOD_ID, "default"));
    private static final Pattern PLAYER_NAME_TOKEN = Pattern.compile("[A-Za-z0-9_]+");

    private PlayerNametagIcons() {
    }

    public static boolean enabled(Minecraft client) {
        return client.level != null && !client.gui.hud.isHidden()
                && CCLiveUtilitiesConfig.HANDLER.instance().showPlayerNametagIcon
                && CCLiveUtilitiesConfig.isDataTrackingEnabled();
    }

    public static Component suffix(Minecraft client, Player player) {
        if (player == null || player == client.player || player.isInvisible()
                || !PlayerIconUtility.hasMod(player.getUUID())) {
            return null;
        }

        String marker = String.valueOf(PlayerIconUtility.chatIconMarker(player.getGameProfile().name()));
        return Component.literal(" ").append(Component.literal(marker).withStyle(style -> style.withFont(ICON_FONT)));
    }

    public static Player findOwner(Entity display, Component label, Minecraft client, float partialTick) {
        String text = label.getString();
        // Prefer the server's passenger relationship when it is present on the client.
        for (Entity vehicle = display.getVehicle(); vehicle != null; vehicle = vehicle.getVehicle()) {
            if (vehicle instanceof Player player) {
                return hasPlayerName(text, player) ? player : null;
            }
        }

        // Some servers move the hologram separately. Require both name and proximity.
        var displayPosition = display.getPosition(partialTick);
        Player owner = null;
        for (Player candidate : client.level.players()) {
            var offset = displayPosition.subtract(candidate.getPosition(partialTick));
            if (offset.y < -0.5 || offset.y > 3.25 || offset.horizontalDistanceSqr() > 0.36
                    || !hasPlayerName(text, candidate)) {
                continue;
            }
            if (owner != null) {
                return null;
            }
            owner = candidate;
        }
        return owner;
    }

    private static boolean hasPlayerName(String text, Player player) {
        String name = player.getGameProfile().name();
        var tokens = PLAYER_NAME_TOKEN.matcher(text);
        while (tokens.find()) {
            if (tokens.end() - tokens.start() == name.length() && text.regionMatches(true, tokens.start(), name, 0, name.length())) {
                return true;
            }
        }
        return false;
    }
}
