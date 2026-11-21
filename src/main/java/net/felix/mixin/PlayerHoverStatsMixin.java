package net.felix.mixin;

import net.felix.Community.chat.ChatShareClient;
import net.felix.Community.chat.ChatShareLine;
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

    private ChatShareLine.Parsed cclive$shareHold;

    /**
     * Fängt GameMessageS2CPacket ab, bevor es den Chat erreicht.
     * Stats und die Share-Spanne laufen hier zusammen, damit sich die beiden Umbauten nicht verwerfen.
     */
    @Inject(
        method = "handleSystemChat",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onGameMessage(ClientboundSystemChatPacket packet, CallbackInfo ci) {
        if (!Minecraft.getInstance().isSameThread()) {
            return;
        }
        cclive$shareHold = null;
        if (packet == null || packet.overlay()) {
            return;
        }

        Component originalMessage = packet.content();
        if (originalMessage == null) {
            return;
        }

        net.felix.Community.chat.ChatNameColorTracker.getInstance().observeDefaultChatMessage(originalMessage);

        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return;
        }

        ChatShareLine.Parsed parsed = ChatShareLine.parse(originalMessage);

        Component modified = null;
        java.util.concurrent.CompletableFuture<?> profile = null;
        if (PlayerHoverStatsUtility.isInitialized()) {
            modified = PlayerHoverStatsUtility.processChatMessage(originalMessage);
            if (modified == originalMessage) {
                modified = null;
            }
            profile = PlayerHoverStatsUtility.pendingChatProfile(originalMessage);
        }

        Component base = modified == null ? originalMessage : modified;
        // Verknüpfung erst im Render-Thread: dort ist der lokale Spielername zuverlässig da.
        if (modified == null && profile == null) {
            cclive$shareHold = parsed;
            return;
        }

        ci.cancel();
        java.util.concurrent.CompletableFuture<?> pendingProfile = profile;
        client.execute(() -> {
            if (client.getConnection() != (Object) this || client.gui == null || client.gui.hud == null) {
                return;
            }
            ChatShareClient.observeEcho(parsed);
            Component displayed = ChatShareClient.decorate(base, parsed);
            if (displayed == null) {
                displayed = base;
            }
            var chat = client.gui.hud.getChat();
            chat.addClientSystemMessage(displayed);
            if (!(chat instanceof ChatHudMessagesAccess access)) {
                return;
            }
            var messages = access.cclive$allMessages();
            GuiMessage added = messages.isEmpty() ? null : messages.getFirst();
            if (added == null || added.content() != displayed) {
                return;
            }
            if (displayed == base && parsed.complete()) {
                ChatShareClient.hold(added, parsed);
            }
            if (pendingProfile == null) {
                return;
            }
            pendingProfile.thenAccept(stats -> {
                if (stats == null) {
                    return;
                }
                client.execute(() -> {
                    if (client.getConnection() != (Object) this) {
                        return;
                    }
                    for (int i = 0; i < messages.size(); i++) {
                        if (messages.get(i) != added) {
                            continue;
                        }
                        Component updated = PlayerHoverStatsUtility.processChatMessage(originalMessage);
                        if (updated == null) {
                            updated = originalMessage;
                        }
                        Component shared = ChatShareClient.decorate(updated, parsed);
                        if (shared != null && shared != added.content()) {
                            GuiMessage neu = new GuiMessage(added.addedTime(), shared,
                                    added.signature(), added.source(), added.tag());
                            messages.set(i, neu);
                            ChatShareClient.retarget(added, neu);
                            access.cclive$refreshTrimmedMessages();
                        }
                        break;
                    }
                });
            });
        });
    }

    @Inject(method = "handleSystemChat", at = @At("TAIL"))
    private void cclive$holdPlainShareLine(ClientboundSystemChatPacket packet, CallbackInfo ci) {
        ChatShareLine.Parsed parsed = cclive$shareHold;
        cclive$shareHold = null;
        if (parsed == null || packet == null || packet.overlay()) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return;
        }
        client.execute(() -> {
            if (client.gui == null || client.gui.hud == null || client.gui.hud.getChat() == null) {
                return;
            }
            var chat = client.gui.hud.getChat();
            if (!(chat instanceof ChatHudMessagesAccess access)) {
                return;
            }
            var messages = access.cclive$allMessages();
            if (messages.isEmpty()) {
                return;
            }
            GuiMessage added = messages.getFirst();
            if (added.content() == null || packet.content() == null) {
                return;
            }
            if (!added.content().getString().equals(packet.content().getString())) {
                return;
            }
            ChatShareClient.observeEcho(parsed);
            Component decorated = ChatShareClient.decorate(added.content(), parsed);
            if (decorated != null && decorated != added.content()) {
                GuiMessage neu = new GuiMessage(added.addedTime(), decorated,
                        added.signature(), added.source(), added.tag());
                messages.set(0, neu);
                access.cclive$refreshTrimmedMessages();
                return;
            }
            if (parsed.complete()) {
                ChatShareClient.hold(added, parsed);
            }
        });
    }
}
