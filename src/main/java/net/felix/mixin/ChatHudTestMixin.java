package net.felix.mixin;

import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Mixin to intercept chat messages with hover events and modify them.
 * Modifies the message variable directly to avoid infinite loops.
 * 
 * NOTE: This mixin is currently disabled in mixins.json.
 * To enable, add "ChatHudTestMixin" to the mixins array after verifying the method signature.
 */
@Mixin(ChatHud.class)
public abstract class ChatHudTestMixin {
    
    /**
     * Modifies the message variable at the start of the method.
     * Tries multiple method signatures to find the correct one.
     */
    @ModifyVariable(
        method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignature;ILnet/minecraft/client/gui/hud/MessageIndicator;Z)V",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0,
        remap = true
    )
    private Text modifyMessageVariableFull(Text originalMessage) {
        return modifyMessage(originalMessage);
    }
    
    /**
     * Fallback for simple addMessage signature if it exists.
     */
    @ModifyVariable(
        method = "addMessage(Lnet/minecraft/text/Text;)V",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0,
        remap = true
    )
    private Text modifyMessageVariableSimple(Text originalMessage) {
        return modifyMessage(originalMessage);
    }
    
    /**
     * Common modification logic.
     */
    private Text modifyMessage(Text originalMessage) {
        if (originalMessage == null) {
            return originalMessage;
        }
        
        try {
            // Debug: Check if message has hover event
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
            
            if (hasHover) {
                System.out.println("[ChatHudTestMixin] Found message with hover event: " + originalMessage.getString());
            }
            
            // TestUtility is disabled - return original message
            // Text modified = TestUtility.modifyChatMessageWithHoverEvent(originalMessage);
            
            // Return original message (TestUtility disabled)
            return originalMessage;
        } catch (Exception e) {
            // If anything goes wrong, return original message to prevent crashes
            return originalMessage;
        }
    }
}

