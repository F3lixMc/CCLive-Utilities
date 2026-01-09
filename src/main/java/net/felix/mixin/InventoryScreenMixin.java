package net.felix.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for InventoryScreen to render aspect overlay in player inventory (survival mode)
 * This is needed because InventoryScreen extends Screen, not HandledScreen
 */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {
    
    /**
     * Injects at the very end of the render method to ensure our overlays are drawn last
     * This allows the aspect overlay to work in the player inventory (survival mode)
     */
    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        // Show aspect overlay when hovering over items with "⭐" in tooltip
        // This works in the player inventory (survival mode)
        if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().aspectOverlayEnabled) {
            // Render aspect overlay if we're hovering over an item with "⭐" (set up by addAspectNameToTooltip)
            // This allows aspect overlay to work in the player inventory
            boolean shouldRender = net.felix.utilities.Overall.Aspekte.AspectOverlay.isCurrentlyHovering();
            
            if (shouldRender) {
                // Render our aspect overlay AFTER everything else (including buttons and tooltips)
                try {
                    net.felix.utilities.Overall.Aspekte.AspectOverlay.renderForeground(context);
                } catch (Exception e) {
                    // Ignore rendering errors
                }
            }
        }
    }
}
