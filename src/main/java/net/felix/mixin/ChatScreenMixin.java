package net.felix.mixin;

import net.felix.utilities.DragOverlay.OverlayEditorUtility;
import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {
    
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
}

