package net.felix.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public class HandledScreenMixin {
    
    /**
     * Injects into the drawForeground method to render the aspect overlay
     * This ensures the overlay is rendered over all GUI elements but under tooltips
     */
    @Inject(method = "drawForeground", at = @At("TAIL"))
    private void onDrawForeground(DrawContext context, int mouseX, int mouseY, CallbackInfo ci) {
        System.out.println("DEBUG: HandledScreenMixin.onDrawForeground called!");
        
        // Render the aspect overlay in the foreground
        try {
            net.felix.utilities.AspectOverlay.renderForeground(context);
            System.out.println("DEBUG: AspectOverlay.renderForeground completed successfully");
        } catch (Exception e) {
            System.err.println("Error in HandledScreenMixin.drawForeground: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 