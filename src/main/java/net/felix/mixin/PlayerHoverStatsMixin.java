package net.felix.mixin;

import net.felix.profile.PlayerHoverStatsUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin zum Abfangen von Chat-Nachrichten und Hinzufügen von Player-Stats zum Hover-Event
 */
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
        if (packet == null) {
            return;
        }
        
        Component originalMessage = packet.content();
        if (originalMessage == null) {
            return;
        }
        
        // Prüfe ob PlayerHoverStatsUtility initialisiert ist
        if (!PlayerHoverStatsUtility.isInitialized()) {
            return;
        }
        
        // Versuche die Nachricht mit Player-Stats zu modifizieren
        Component modified = PlayerHoverStatsUtility.processChatMessage(originalMessage);
        
        if (modified == null || modified == originalMessage) {
            // Keine Modifikation nötig oder möglich → Nachricht normal durchlassen
            return;
        }
        
        // Modifizierte Nachricht gefunden → Original abbrechen und modifizierte Version hinzufügen
        ci.cancel();

        // handleSystemChat kann auf dem Netty-Thread laufen – Chat/GUI nur auf dem Render-Thread anfassen
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return;
        }
        Component messageToAdd = modified;
        client.execute(() -> {
            if (client.gui != null && client.gui.getChat() != null) {
                client.gui.getChat().addClientSystemMessage(messageToAdd);
            }
        });
    }
}

