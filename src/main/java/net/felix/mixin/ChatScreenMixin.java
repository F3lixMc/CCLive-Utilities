package net.felix.mixin;

import net.felix.utilities.DragOverlay.OverlayEditorUtility;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.felix.chat.BuildShareManager;
import net.felix.chat.BuildShareViewScreen;
import net.felix.chat.ChatManager;
import net.felix.chat.ItemShareManager;
import net.felix.profile.ProfileStatsManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {
    
    @Shadow
    protected EditBox input;
    
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
    private void onKeyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        // Handle F6 key for overlay editor (works in chat)
        if (OverlayEditorUtility.handleKeyPress(event.key())) {
            cir.setReturnValue(true);
        }
    }

    /** Linksklick auf Build-Share-Link öffnet die PNG-Container-Vorschau (kein SuggestCommand). */
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(MouseButtonEvent event, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (event.button() != 0) {
            return;
        }
        if (BuildShareViewScreen.isActive()) {
            cir.setReturnValue(true);
            return;
        }
        // Auch Insertion/Suggest von Vanilla abfangen, falls noch alte Nachrichten im Chat sind
        if (BuildShareManager.getInstance().tryOpenFromChatClick(event.x(), event.y())) {
            cir.setReturnValue(true);
            return;
        }
        // Alte SuggestCommand-Links (cclive-build:…) nicht in den Chat schreiben
        net.minecraft.network.chat.Style style =
            net.felix.chat.ChatStyleHelper.getHoveredChatStyle(event.x(), event.y());
        if (style != null && style.getClickEvent() != null) {
            String clickVal = net.felix.chat.ChatStyleHelper.clickEventValue(style.getClickEvent());
            if (clickVal != null && clickVal.startsWith(BuildShareManager.BUILD_INSERTION_PREFIX)) {
                String id = clickVal.substring(BuildShareManager.BUILD_INSERTION_PREFIX.length());
                BuildShareManager.getInstance().openBuildScreen(id);
                cir.setReturnValue(true);
            }
        }
    }
    
    /**
     * Interceptiert Chat-Nachrichten und prüft auf @cclive, @global, @lp und !item
     */
    @Inject(method = "handleChatInput", at = @At("HEAD"), cancellable = true)
    private void onSendMessage(String message, boolean addToHistory, CallbackInfo ci) {
        if (message == null || message.trim().isEmpty()) {
            return;
        }
        
        String trimmedMessage = message.trim();
        ChatManager chatManager = ChatManager.getInstance();
        ChatManager.ChatMode writeMode = chatManager.getEffectiveWriteMode();
        
        // @lp: immer in den globalen Minecraft-Chat; !item → xD
        if (trimmedMessage.startsWith("@lp")) {
            String withXd = ItemShareManager.replaceItemTokenWithXd(trimmedMessage);
            if (!withXd.equals(trimmedMessage)) {
                ci.cancel();
                sendMinecraftChat(withXd);
            }
            trackMessageSent();
            return;
        }
        
        // @cclive: nur senden wenn CCLive-Chat an ist
        if (trimmedMessage.startsWith("@cclive")) {
            ci.cancel();

            if (!chatManager.isShowCCLiveChat()) {
                scheduleChatError("§cCCLive-Chat ist aus — Nachricht nicht gesendet.");
                return;
            }

            String chatMessage = trimmedMessage.substring("@cclive".length()).trim();
            if (chatMessage.isEmpty()) {
                return;
            }

            chatManager.sendMessage(chatMessage).thenAccept(error -> {
                if (error != null) {
                    scheduleChatError("§cChat: " + error);
                }
            });
            trackMessageSent();
            return;
        }
        
        // @global: nur senden wenn Default-Chat an ist; !item → xD
        if (trimmedMessage.startsWith("@global")) {
            ci.cancel();

            if (!chatManager.isShowDefaultChat()) {
                scheduleChatError("§cDefault-Chat ist aus — @global nicht verfügbar.");
                return;
            }

            String globalMessage = trimmedMessage.substring("@global".length()).trim();
            if (globalMessage.isEmpty()) {
                return;
            }
            globalMessage = ItemShareManager.replaceItemTokenWithXd(globalMessage);

            sendMinecraftChat(globalMessage);
            if (!globalMessage.startsWith("/")) {
                trackMessageSent();
            }
            return;
        }
        
        // CCLIVE-Schreibziel: normale Nachrichten → CCLive (!item → Share-Ref in sendMessage)
        if (writeMode == ChatManager.ChatMode.CCLIVE) {
            if (trimmedMessage.startsWith("/")) {
                return;
            }

            ci.cancel();
            
            chatManager.sendMessage(trimmedMessage).thenAccept(error -> {
                if (error != null) {
                    scheduleChatError("§cChat: " + error);
                }
            });
            trackMessageSent();
            return;
        }

        // Default-Schreibziel: !item → xD
        if (!chatManager.isShowDefaultChat() && !trimmedMessage.startsWith("/")) {
            scheduleChatError("§cDefault-Chat ist aus — Nachricht nicht gesendet.");
            ci.cancel();
            return;
        }

        String defaultOut = ItemShareManager.replaceItemTokenWithXd(trimmedMessage);
        if (!defaultOut.equals(trimmedMessage) && !trimmedMessage.startsWith("/")) {
            ci.cancel();
            sendMinecraftChat(defaultOut);
            trackMessageSent();
            return;
        }
        
        if (!trimmedMessage.startsWith("/")) {
            trackMessageSent();
        }
    }

    private static void sendMinecraftChat(String text) {
        net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
        if (client.player != null && client.getConnection() != null && text != null && !text.isEmpty()) {
            client.getConnection().sendChat(text);
        }
    }

    private static void scheduleChatError(String message) {
        net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
        if (client == null) {
            return;
        }
        client.execute(() -> {
            if (client.player != null) {
                client.player.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal(message)
                );
            }
        });
    }
}
