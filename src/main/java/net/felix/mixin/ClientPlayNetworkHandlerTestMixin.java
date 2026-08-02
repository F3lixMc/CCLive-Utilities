package net.felix.mixin;

import net.felix.utilities.Overall.InformationenUtility;
import net.felix.utilities.Town.StarForgedSoundUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to intercept GameMessageS2CPacket before it reaches the chat.
 * This allows us to modify messages with hover events and prevent the original from showing.
 */
@Mixin(ClientPacketListener.class)
public abstract class ClientPlayNetworkHandlerTestMixin {
    
    /**
     * Intercepts onGameMessage to modify messages with hover events.
     * Cancels the original packet and sends a modified version instead.
     */
    @Inject(
        method = "handleSystemChat",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onGameMessage(ClientboundSystemChatPacket packet, CallbackInfo ci) {
        if (packet != null) {
            StarForgedSoundUtility.handleIncomingMessage(packet.content(), packet.overlay());
        }

        if (packet == null) {
            return;
        }
        
        Component originalMessage = packet.content();
        if (originalMessage == null) {
            return;
        }
        
        // Check if message contains [Bauplan] or combo-related keywords
        String messageText = originalMessage.getString();
        if (messageText != null && (messageText.contains("[Bauplan]") || messageText.contains("Kombo") || messageText.contains("Belohnungen"))) {
            // Check for blueprint BEFORE modifying the message
            net.felix.utilities.Aincraft.BPViewerUtility bpInstance = net.felix.utilities.Aincraft.BPViewerUtility.getInstance();
            if (bpInstance != null) {
                bpInstance.checkForBlueprint(originalMessage, messageText);
            }
        }
        
        // Check if message has a hover event (only needed for modification)
        boolean hasHover = false;
        if (originalMessage.getStyle() != null && originalMessage.getStyle().getHoverEvent() != null) {
            hasHover = true;
        }
        for (Component sibling : originalMessage.getSiblings()) {
            if (sibling.getStyle() != null && sibling.getStyle().getHoverEvent() != null) {
                hasHover = true;
                break;
            }
        }
        
        if (!hasHover) {
            // No hover event, let it pass through normally (but we already checked for blueprints above)
            return;
        }
        
        // Check if message contains [Bauplan] - only modify blueprint messages
        if (messageText == null || !messageText.contains("[Bauplan]")) {
            // Not a blueprint message, let it pass through normally
            return;
        }
        
        // Try to modify the message with aspect info
        Component modified = InformationenUtility.modifyChatMessageForAspectInfo(originalMessage);
        
        if (modified == null) {
            return;
        }
        
        if (modified == originalMessage) {
            return;
        }
        
        // Cancel the original packet FIRST to prevent it from being processed
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

