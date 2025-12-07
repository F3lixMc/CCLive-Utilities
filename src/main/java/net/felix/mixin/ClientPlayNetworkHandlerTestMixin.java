package net.felix.mixin;

import net.felix.utilities.Overall.InformationenUtility;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to intercept GameMessageS2CPacket before it reaches the chat.
 * This allows us to modify messages with hover events and prevent the original from showing.
 */
@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerTestMixin {
    
    /**
     * Intercepts onGameMessage to modify messages with hover events.
     * Cancels the original packet and sends a modified version instead.
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
        for (Text sibling : originalMessage.getSiblings()) {
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
        Text modified = InformationenUtility.modifyChatMessageForAspectInfo(originalMessage);
        
        if (modified == null) {
            return;
        }
        
        if (modified == originalMessage) {
            return;
        }
        
        // Cancel the original packet FIRST to prevent it from being processed
        ci.cancel();
        
        // Add our modified message to the chat AFTER cancelling
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.inGameHud != null && client.inGameHud.getChatHud() != null) {
            client.inGameHud.getChatHud().addMessage(modified);
        }
    }
}

