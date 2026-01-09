package net.felix.mixin;

import net.felix.utilities.ItemViewer.ItemViewerUtility;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Separater Mixin für das Hilfe-Overlay in Screen, damit es wirklich über allem liegt
 * Wird mit RETURN injiziert, um sicherzustellen, dass es nach allem anderen gerendert wird
 */
@Mixin(Screen.class)
public abstract class HelpOverlayScreenMixin {
    
    /**
     * Injiziert am RETURN-Punkt der render-Methode, um sicherzustellen, dass das Hilfe-Overlay
     * wirklich nach allem anderen gerendert wird (auch nach Items)
     */
    @Inject(method = "render", at = @At(value = "RETURN"))
    private void renderHelpOverlayAtReturn(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        // Rendere Help-Overlay ganz am Ende, damit es über allem liegt
        ItemViewerUtility.renderHelpOverlay(context);
    }
}

