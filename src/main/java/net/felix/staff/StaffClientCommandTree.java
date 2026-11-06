package net.felix.staff;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.impl.command.client.ClientCommandInternals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import net.minecraft.server.RunningOnDifferentThreadException;
import net.felix.mixin.ChatScreenSuggestionsAccess;

/**
 * Chat-Färbung und Autocomplete nutzen den Vanilla-Suggestion-Dispatcher.
 * Fabric kopiert Client-Befehle dorthin nur einmal beim Command-Tree-Paket
 * und filtert dabei {@code canUse}. Die Ausführung läuft getrennt über den
 * Live-Dispatcher. Nach Rollenwechsel muss der Suggestion-Baum neu gebaut werden.
 */
final class StaffClientCommandTree {
    private StaffClientCommandTree() {
    }

    static void applyVisibility() {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return;
        if (!client.isSameThread()) {
            client.execute(StaffClientCommandTree::applyVisibility);
            return;
        }
        rebuildSuggestionDispatcher(client);
        refreshOpenChatParse(client);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void rebuildSuggestionDispatcher(Minecraft client) {
        ClientPacketListener connection = client.getConnection();
        if (connection == null) return;

        if (connection instanceof ClientCommandInternals.LastReceivedCommandsPacketAccessor accessor) {
            ClientboundCommandsPacket packet = accessor.fabric_api$getLastReceivedCommandsPacket();
            if (packet != null) {
                try {
                    connection.handleCommands(packet);
                } catch (RunningOnDifferentThreadException ignored) {
                    client.execute(StaffClientCommandTree::applyVisibility);
                }
                return;
            }
        }

        CommandDispatcher dispatcher = connection.getCommands();
        if (dispatcher != null && connection.getSuggestionsProvider() instanceof FabricClientCommandSource source) {
            ClientCommandInternals.addCommands(dispatcher, source);
        }
    }

    private static void refreshOpenChatParse(Minecraft client) {
        if (!(client.gui.screen() instanceof ChatScreen screen)) return;
        if (!(screen instanceof ChatScreenSuggestionsAccess access)) return;
        CommandSuggestions suggestions = access.cclive$commandSuggestions();
        if (suggestions != null) {
            suggestions.updateCommandInfo();
        }
    }
}
