package net.felix.mixin;

import net.felix.Community.chat.BuildShareManager;
import net.felix.Community.chat.BuildShareViewScreen;
import net.felix.Community.chat.ChatManager;
import net.felix.Community.chat.ItemShareManager;
import net.felix.Community.profile.ProfileStatsManager;
import net.felix.Community.staff.StaffClientAccess;
import net.felix.utilities.DragOverlay.OverlayEditorUtility;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SuppressWarnings("unused")
@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {

    @Shadow
    protected EditBox input;

    private boolean cclive$openRulesAfterClose;
    private String cclive$rulesDraft;
    
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
    @Inject(method = "init", at = @At("TAIL"))
    private void cclive$onChatOpened(CallbackInfo ci) {
        StaffClientAccess.refreshAsync();
        StaffClientAccess.applyCommandVisibility();
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        // Handle F6 key for overlay editor (works in chat)
        if (OverlayEditorUtility.handleKeyPress(event.key())) {
            cir.setReturnValue(true);
            return;
        }
        if (!isChatConfirmKey(event) || this.input == null) {
            return;
        }
        if (blockCcliveGlyphsIfNeeded()) {
            cir.setReturnValue(true);
            return;
        }
        String typed = this.input.getValue();
        if (typed != null && holdForChatRules(typed.trim())) {
            cclive$openRulesAfterClose = false;
            net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
            if (client != null && client.gui != null) {
                client.gui.setScreen(net.felix.Community.chat.ChatRulesScreen.forAcceptance((net.minecraft.client.gui.screens.Screen) (Object) this));
            }
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "keyPressed", at = @At("RETURN"))
    private void cclive$openRulesAfterClose(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (!cclive$openRulesAfterClose) {
            return;
        }
        cclive$openRulesAfterClose = false;
        String draft = cclive$rulesDraft;
        net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
        if (client == null || client.gui == null || draft == null) {
            return;
        }
        if (client.gui.hud != null && client.gui.hud.getChat() != null) {
            client.gui.hud.getChat().saveAsDraft(draft);
        }
        if (!(client.gui.screen() instanceof net.felix.Community.chat.ChatRulesScreen)) {
            client.gui.setScreen(net.felix.Community.chat.ChatRulesScreen.forAcceptance());
        }
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void cclive$closeContextMenu(CallbackInfo ci) {
        net.felix.Community.chat.ChatContextMenu.dismissWithScreen(this);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(MouseButtonEvent event, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        int button = net.felix.utilities.Other.keybinding.KeyBindingUtility.legacyMouseButton(event.button());
        if (button == 1) {
            if (net.felix.Community.chat.ChatContextMenu.tryOpen(event.x(), event.y())) {
                cir.setReturnValue(true);
            } else {
                net.felix.Community.chat.ChatContextMenu.close();
            }
            return;
        }
        if (button != 0) {
            return;
        }
        if (net.felix.Community.chat.ChatContextMenu.handleClick(event.x(), event.y(), button)) {
            cir.setReturnValue(true);
            return;
        }
        // 26.3/SDL: Linksklick = 1 → auf Legacy-0 abbilden. Dieser Pfad bleibt der bisherige Linksklick.
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
            net.felix.Community.chat.ChatStyleHelper.getHoveredChatStyle(event.x(), event.y());
        if (style != null && style.getClickEvent() != null) {
            String clickVal = net.felix.Community.chat.ChatStyleHelper.clickEventValue(style.getClickEvent());
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
        
        // @lp bleibt im Text. !item und !build werden zum Anzeigenamen, bevor PlayLegend sie sieht.
        if (trimmedMessage.startsWith("@lp")) {
            ShareSend send = dispatchShareChat(message, ci, false);
            if (send == ShareSend.BLOCKED) {
                return;
            }
            if (send == ShareSend.VANILLA) {
                net.felix.Community.chat.ChatShareClient.notePlain(message);
            }
            trackMessageSent();
            return;
        }
        
        // @cclive: nur senden wenn CCLive-Chat an ist
        if (trimmedMessage.startsWith("@cclive")) {
            ci.cancel();

            if (!chatManager.isShowCCLiveChat()) {
                ChatManager.showDisabledNotice();
                return;
            }

            String chatMessage = trimmedMessage.substring("@cclive".length()).trim();
            if (chatMessage.isEmpty()) {
                return;
            }
            if (rejectBlockedGlyphs(chatMessage)) {
                return;
            }
            if (holdForChatRules(trimmedMessage)) {
                return;
            }

            sendCclive(chatMessage);
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
            ShareSend send = dispatchShareChat(globalMessage, ci, true);
            if (send == ShareSend.SENT && !globalMessage.startsWith("/")) {
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
            if (rejectBlockedGlyphs(trimmedMessage)) {
                return;
            }
            if (holdForChatRules(trimmedMessage)) {
                return;
            }
            sendCclive(trimmedMessage);
            return;
        }

        // Default-Schreibziel: !item und !build werden zum Anzeigenamen.
        if (!chatManager.isShowDefaultChat() && !trimmedMessage.startsWith("/")) {
            scheduleChatError("§cDefault-Chat ist aus — Nachricht nicht gesendet.");
            ci.cancel();
            return;
        }

        if (!trimmedMessage.startsWith("/")) {
            ShareSend send = dispatchShareChat(message, ci, false);
            if (send == ShareSend.BLOCKED) {
                return;
            }
            if (send == ShareSend.VANILLA) {
                net.felix.Community.chat.ChatShareClient.notePlain(message);
            }
            trackMessageSent();
        }
    }

    private enum ShareSend { VANILLA, SENT, BLOCKED }

    private static ShareSend dispatchShareChat(String text, CallbackInfo ci, boolean alwaysSend) {
        net.felix.Community.chat.ChatShareOutgoing.Draft draft =
                net.felix.Community.chat.ChatShareOutgoing.prepare(text);
        net.felix.Community.chat.ChatShareTexts.Replacement replacement = draft.replacement();
        if (replacement.error() != null) {
            ci.cancel();
            scheduleChatError("§c" + replacement.error());
            return ShareSend.BLOCKED;
        }
        String outgoing = replacement.text() == null ? text : replacement.text();
        boolean changed = !outgoing.equals(text);
        if (!changed && !alwaysSend) {
            return ShareSend.VANILLA;
        }
        if (replacement.share()) {
            net.felix.Community.chat.ChatShareClient.noteShare(replacement, draft.shareId(), draft.nameColor());
        } else if (changed || alwaysSend) {
            net.felix.Community.chat.ChatShareClient.notePlain(outgoing);
        }
        ci.cancel();
        if (!sendMinecraftChat(outgoing)) {
            net.felix.Community.chat.ChatShareClient.dropNoted();
            return ShareSend.BLOCKED;
        }
        return ShareSend.SENT;
    }

    private boolean blockCcliveGlyphsIfNeeded() {
        if (this.input == null) {
            return false;
        }
        String message = this.input.getValue();
        if (message == null || message.trim().isEmpty()) {
            return false;
        }
        String trimmed = message.trim();
        ChatManager chatManager = ChatManager.getInstance();
        if (!isCcliveOutgoing(trimmed, chatManager)) {
            return false;
        }
        String body = trimmed.startsWith("@cclive")
            ? trimmed.substring("@cclive".length()).trim()
            : trimmed;
        if (body.isEmpty()) {
            return false;
        }
        return rejectBlockedGlyphs(body);
    }

    private static boolean isCcliveOutgoing(String trimmed, ChatManager chatManager) {
        if (trimmed.startsWith("@lp") || trimmed.startsWith("@global") || trimmed.startsWith("/")) {
            return false;
        }
        if (trimmed.startsWith("@cclive")) {
            return true;
        }
        return chatManager.getEffectiveWriteMode() == ChatManager.ChatMode.CCLIVE;
    }

    private static boolean rejectBlockedGlyphs(String userText) {
        if (!net.felix.Community.chat.ChatBlockedGlyphs.containsBlockedGlyph(userText)) {
            return false;
        }
        scheduleChatError("§c" + net.felix.Community.chat.ChatBlockedGlyphs.ERROR_MESSAGE);
        return true;
    }

    private boolean holdForChatRules(String rawInput) {
        if (!net.felix.Community.chat.ChatRulesAcceptance.isRulesGatedSend(
                rawInput, ChatManager.getInstance().getEffectiveWriteMode() == ChatManager.ChatMode.CCLIVE)) {
            return false;
        }
        net.felix.Community.chat.ChatRulesAcceptance store = net.felix.Community.chat.ChatRulesAcceptance.shared();
        String accountId = net.felix.Community.chat.ChatIgnoreList.localAccountId();
        Integer seasonId = net.felix.Community.leaderboards.SeasonChangeGate.confirmedSeasonId();
        if (!store.shouldHold(accountId, seasonId)) {
            return false;
        }
        store.hold(rawInput);
        cclive$rulesDraft = rawInput;
        cclive$openRulesAfterClose = true;
        return true;
    }

    private void sendCclive(String chatMessage) {
        ChatManager.getInstance().sendMessage(chatMessage).thenAccept(error -> {
            if (error != null) {
                if (net.felix.Community.chat.ChatBlockedGlyphs.isBlockedError(error)) {
                    scheduleChatError("§c" + net.felix.Community.chat.ChatBlockedGlyphs.ERROR_MESSAGE);
                } else {
                    scheduleChatError("§cChat: " + error);
                }
            }
        });
        trackMessageSent();
    }

    private static boolean isChatConfirmKey(KeyEvent event) {
        int key = event.key();
        return key == com.mojang.blaze3d.platform.InputConstants.KEY_RETURN
            || key == com.mojang.blaze3d.platform.InputConstants.KEY_NUMPADENTER;
    }

    private static boolean sendMinecraftChat(String text) {
        net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
        if (client.player == null || client.getConnection() == null || text == null || text.isEmpty()) {
            return false;
        }
        client.getConnection().sendChat(text);
        return true;
    }

    private static long lastGlyphErrorAtMs;

    private static void scheduleChatError(String message) {
        if (message != null && message.contains(net.felix.Community.chat.ChatBlockedGlyphs.ERROR_MESSAGE)) {
            long now = System.currentTimeMillis();
            if (now - lastGlyphErrorAtMs < 500L) {
                return;
            }
            lastGlyphErrorAtMs = now;
        }
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
