package net.felix.mixin;

import net.minecraft.client.gui.hud.ChatHud;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Mixin to modify chat messages before they are added to the chat.
 * This allows us to add aspect information to hover events.
 * 
 * NOTE: This mixin is currently disabled in mixins.json due to method signature changes in Minecraft 1.21.7.
 * To re-enable, find the correct addMessage method signature and update the @Inject annotation.
 */
@Mixin(ChatHud.class)
public abstract class ChatHudAddMessageMixin {
    
    /**
     * Modifies chat messages before they are added to the chat
     * This allows us to add aspect information to hover events
     * 
     * TODO: Update method signature to match Minecraft 1.21.7
     * The old signature was: addMessage(Text, MessageSignature, int, MessageIndicator, boolean)
     * Need to find the new signature using MixinExtras or by examining the class at runtime
     */
    // @Inject(method = "addMessage", at = @At("HEAD"))
    // private void onAddMessage(Text message, CallbackInfo ci) {
    //     if (message == null) {
    //         return;
    //     }
    //     
    //     // Modify the message to add aspect information
    //     Text modified = InformationenUtility.modifyChatMessageForAspectInfo(message);
    //     if (modified != message) {
    //         // If we need to replace the message, we'd need to use a different approach
    //         // For now, this is just a placeholder
    //     }
    // }
}

