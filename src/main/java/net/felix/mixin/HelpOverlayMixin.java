package net.felix.mixin;

import net.felix.utilities.Aincraft.itemviewer.ui.FilterOverlayControls;
import net.felix.utilities.Aincraft.itemviewer.ui.HelpOverlay;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Separater Mixin für das Hilfe-Overlay, damit es wirklich über allem liegt
 * Wird mit RETURN injiziert, um sicherzustellen, dass es nach allem anderen gerendert wird
 */
@SuppressWarnings("unused")
@Mixin(AbstractContainerScreen.class)
public abstract class HelpOverlayMixin {
    
    /**
     * Injiziert am RETURN-Punkt der render-Methode, um sicherzustellen, dass das Hilfe-Overlay
     * wirklich nach allem anderen gerendert wird (auch nach Items)
     */
    /**
     * Injiziert am RETURN-Punkt der render-Methode, um sicherzustellen, dass das Hilfe-Overlay
     * wirklich nach allem anderen gerendert wird (auch nach Items)
     * Wird zusätzlich zum TAIL-Inject verwendet, um sicherzustellen, dass es wirklich zuletzt gerendert wird
     */
    @Inject(method = "extractRenderState", at = @At(value = "RETURN"))
    private void renderHelpOverlayAtReturn(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        // Rendere Help-Overlay ganz am Ende, damit es über allem liegt
        HelpOverlay.renderHelpOverlay(context);
        FilterOverlayControls.renderFilterOverlay(context);
    }
}

