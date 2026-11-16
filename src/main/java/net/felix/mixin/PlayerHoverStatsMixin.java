package net.felix.mixin;

import net.felix.Community.profile.PlayerHoverStatsUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin zum Abfangen von Chat-Nachrichten und Hinzufügen von Player-Stats zum Hover-Event
 */
@SuppressWarnings("unused")
@Mixin(ClientPacketListener.class)
public abstract class PlayerHoverStatsMixin {
    
    /**
     * Fängt GameMessageS2CPacket ab, bevor es den Chat erreicht
     * Modifiziert die Nachricht mit Player-Stats im Hover-Event, falls verfügbar
     */
    @Inject(
        method = "handleSystemChat",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onGameMessage(ClientboundSystemChatPacket packet, CallbackInfo ci) {
        if (packet == null || packet.overlay()) {
            return;
        }
        
        Component originalMessage = packet.content();
        if (originalMessage == null) {
            return;
        }

        // Namensfarbe aus Original-Default-Chat (vor Cancel/Umbau durch Hover-Stats)
        net.felix.Community.chat.ChatNameColorTracker.getInstance().observeDefaultChatMessage(originalMessage);
        
        // Prüfe ob PlayerHoverStatsUtility initialisiert ist
        if (!PlayerHoverStatsUtility.isInitialized()) {
            return;
        }
        
        Minecraft client = Minecraft.getInstance();
        if (client == null) return;

        Component modified = PlayerHoverStatsUtility.processChatMessage(originalMessage);
        if (modified == originalMessage) modified = null;
        var profile = PlayerHoverStatsUtility.pendingChatProfile(originalMessage);
        if (modified == null && profile == null) return;

        Component displayed = modified == null ? originalMessage : modified;
        ci.cancel();
        client.execute(() -> {
            if (client.getConnection() != (Object) this || client.gui == null || client.gui.hud == null) return;
            var chat = client.gui.hud.getChat();
            chat.addClientSystemMessage(displayed);
            if (profile == null || !(chat instanceof ChatHudMessagesAccess access)) return;
            var messages = access.cclive$allMessages();
            GuiMessage added = messages.isEmpty() ? null : messages.getFirst();
            if (added == null || added.content() != displayed) return;

            profile.thenAccept(stats -> {
                if (stats == null) return;
                client.execute(() -> {
                    if (client.getConnection() != (Object) this) return;
                    for (int i = 0; i < messages.size(); i++) {
                        if (messages.get(i) != added) continue;
                        Component updated = PlayerHoverStatsUtility.processChatMessage(originalMessage);
                        if (updated != null && !updated.equals(displayed)) {
                            messages.set(i, new GuiMessage(added.addedTime(), updated,
                                    added.signature(), added.source(), added.tag()));
                            access.cclive$refreshTrimmedMessages();
                        }
                        break;
                    }
                });
            });
        });
    }
}
