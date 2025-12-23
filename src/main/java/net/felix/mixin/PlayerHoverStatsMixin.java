package net.felix.mixin;

import net.felix.profile.PlayerHoverStatsUtility;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin zum Abfangen von Chat-Nachrichten und Hinzufügen von Player-Stats zum Hover-Event
 */
@Mixin(ClientPlayNetworkHandler.class)
public abstract class PlayerHoverStatsMixin {
    
    /**
     * Fängt GameMessageS2CPacket ab, bevor es den Chat erreicht
     * Modifiziert die Nachricht mit Player-Stats im Hover-Event, falls verfügbar
     */
    @Inject(
        method = "onGameMessage",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onGameMessage(GameMessageS2CPacket packet, CallbackInfo ci) {
        if (packet == null) {
            return;
        }
        
        Text originalMessage = packet.content();
        if (originalMessage == null) {
            return;
        }
        
        // Prüfe ob PlayerHoverStatsUtility initialisiert ist
        if (!PlayerHoverStatsUtility.isInitialized()) {
            return;
        }
        
        // Versuche die Nachricht mit Player-Stats zu modifizieren
        Text modified = PlayerHoverStatsUtility.processChatMessage(originalMessage);
        
        if (modified == null || modified == originalMessage) {
            // Keine Modifikation nötig oder möglich → Nachricht normal durchlassen
            return;
        }
        
        // Modifizierte Nachricht gefunden → Original abbrechen und modifizierte Version hinzufügen
        ci.cancel();
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.inGameHud != null && client.inGameHud.getChatHud() != null) {
            client.inGameHud.getChatHud().addMessage(modified);
        }
    }
}

