package net.felix.mixin;

import net.felix.utilities.DragOverlay.OverlayEditorUtility;
import net.felix.chat.ChatManager;
import net.felix.profile.ProfileStatsManager;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {
    
    @Shadow
    protected TextFieldWidget chatField;
    
    /**
     * Zählt gesendete Chat-Nachrichten für Profile-Stats.
     */
    private void trackMessageSent() {
        try {
            ProfileStatsManager.getInstance().onMessageSent();
        } catch (Exception e) {
            // Fehler beim Tracking ignorieren, damit der Chat nicht crasht
        }
    }
    
    /**
     * Handles F6 key press in chat screen to open overlay editor
     */
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        // Handle F6 key for overlay editor (works in chat)
        if (OverlayEditorUtility.handleKeyPress(keyCode)) {
            cir.setReturnValue(true);
        }
    }
    
    /**
     * Interceptiert Chat-Nachrichten und prüft auf @cclive, @default und @lp
     */
    @Inject(method = "sendMessage", at = @At("HEAD"), cancellable = true)
    private void onSendMessage(String message, boolean addToHistory, CallbackInfo ci) {
        if (message == null || message.trim().isEmpty()) {
            return;
        }
        
        String trimmedMessage = message.trim();
        ChatManager chatManager = ChatManager.getInstance();
        ChatManager.ChatMode currentMode = chatManager.getCurrentChatMode();
        
        // @lp sollte IMMER in den Default-Chat gehen (normale Nachricht, nicht cancel)
        if (trimmedMessage.startsWith("@lp")) {
            // Wird ganz normal als Chat-Nachricht gesendet → mitzählen
            trackMessageSent();
            return; // Lass normale Nachricht durchgehen (geht an Default-Chat)
        }
        
        // @cclive: Nur wenn Chat-Modus DEFAULT ist (sonst wäre es doppelt)
        if (trimmedMessage.startsWith("@cclive")) {
            if (currentMode == ChatManager.ChatMode.DEFAULT) {
                ci.cancel(); // Verhindere normale Nachricht
                
                // Extrahiere die eigentliche Nachricht (nach @cclive)
                String chatMessage = trimmedMessage.substring("@cclive".length()).trim();
                
                if (chatMessage.isEmpty()) {
                    return;
                }
                
                // Sende an CCLive Chat
                chatManager.sendMessage(chatMessage).thenAccept(success -> {
                    if (!success) {
                        net.minecraft.client.MinecraftClient.getInstance().player.sendMessage(
                            net.minecraft.text.Text.literal("§cFehler beim Senden der Chat-Nachricht!"), false
                        );
                    }
                });
            } else {
                // Wenn Chat-Modus CCLIVE ist, sende die Nachricht mit @cclive Prefix an CCLIVE Chat
                // (damit der User merkt, dass er es falsch gemacht hat, aber bleibt im CCLIVE Chat)
                ci.cancel(); // Verhindere normale Nachricht (die würde in Default-Chat gehen)
                
                // Sende die komplette Nachricht (mit @cclive) an CCLIVE Chat
                chatManager.sendMessage(trimmedMessage).thenAccept(success -> {
                    if (!success) {
                        net.minecraft.client.MinecraftClient.getInstance().player.sendMessage(
                            net.minecraft.text.Text.literal("§cFehler beim Senden der Chat-Nachricht!"), false
                        );
                    }
                });
            }
            // Jede @cclive-Nachricht ist eine gesendete Chatnachricht
            trackMessageSent();
            return;
        }
        
        // @default: Nur wenn Chat-Modus CCLIVE ist (um in Default-Chat zu schreiben)
        if (trimmedMessage.startsWith("@default")) {
            if (currentMode == ChatManager.ChatMode.CCLIVE) {
                // Entferne @default Prefix und sende Nachricht manuell in den Default-Chat
                String defaultMessage = trimmedMessage.substring("@default".length()).trim();
                if (!defaultMessage.isEmpty()) {
                    ci.cancel();
                    // Sende die Nachricht ohne @default direkt an den Server (normale Chat-Nachricht)
                    net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
                    if (client.player != null && client.getNetworkHandler() != null) {
                        client.getNetworkHandler().sendChatMessage(defaultMessage);
                    }
                    
                    // Nur echte Chat-Nachrichten (keine Commands) mitzählen
                    if (!defaultMessage.startsWith("/")) {
                        trackMessageSent();
                    }
                }
            } else {
                // Wenn Chat-Modus DEFAULT ist, wird @default ignoriert und die Nachricht normal gesendet
                // → immer eine Chat-Nachricht (kein führendes /), also mitzählen
                trackMessageSent();
            }
            return;
        }
        
        // Prüfe ob der aktuelle Chat-Modus deaktiviert ist
        if (currentMode == ChatManager.ChatMode.CCLIVE && !chatManager.isShowCCLiveChat()) {
            // CCLIVE Chat ist deaktiviert, wechsle automatisch zu DEFAULT
            chatManager.setChatMode(ChatManager.ChatMode.DEFAULT);
            currentMode = ChatManager.ChatMode.DEFAULT;
        } else if (currentMode == ChatManager.ChatMode.DEFAULT && !chatManager.isShowDefaultChat()) {
            // Default Chat ist deaktiviert, wechsle automatisch zu CCLIVE
            chatManager.setChatMode(ChatManager.ChatMode.CCLIVE);
            currentMode = ChatManager.ChatMode.CCLIVE;
        }
        
        // Wenn Chat-Modus auf CCLIVE gesetzt ist, sende alle Nachrichten an CCLive Chat
        // ABER: Commands (beginnend mit /) sollten weiterhin normal funktionieren
        if (currentMode == ChatManager.ChatMode.CCLIVE) {
            if (trimmedMessage.startsWith("/")) {
                // Command → nicht als Chatnachricht zählen
                return; // Lass Commands normal funktionieren
            }
            
            ci.cancel(); // Verhindere normale Nachricht
            
            chatManager.sendMessage(trimmedMessage).thenAccept(success -> {
                if (!success) {
                    net.minecraft.client.MinecraftClient.getInstance().player.sendMessage(
                        net.minecraft.text.Text.literal("§cFehler beim Senden der Chat-Nachricht!"), false
                    );
                }
            });
            // Kein return; wir wollen unten noch trackMessageSent() aufrufen
        }
        
        // Standard-Fall: alle Nachrichten, die bis hierher durchkommen
        // und keine Commands sind, als gesendete Chat-Nachricht zählen
        if (!trimmedMessage.startsWith("/")) {
            trackMessageSent();
        }
    }
}

