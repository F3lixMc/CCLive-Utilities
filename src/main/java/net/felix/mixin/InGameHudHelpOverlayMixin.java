package net.felix.mixin;

import net.felix.utilities.ItemViewer.ItemViewerUtility;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin für Gui, um das Hilfe-Overlay nach allen Screen-Render-Layern zu rendern
 * Wird am RETURN-Punkt injiziert, um sicherzustellen, dass es wirklich über allen Items liegt
 */
@Mixin(Gui.class)
public abstract class InGameHudHelpOverlayMixin {
    
    /**
     * Injiziert am RETURN-Punkt der render-Methode, um sicherzustellen, dass das Hilfe-Overlay
     * wirklich nach allen Screen-Render-Layern gerendert wird (auch nach Items)
     */
    @Inject(method = "extractRenderState", at = @At(value = "RETURN"))
    private void renderHelpOverlayAtReturn(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        // Rendere Help-Overlay nur wenn ein Screen offen ist und das Overlay geöffnet ist
        net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
        if (client != null && client.screen != null && ItemViewerUtility.isOverlayOpen()) {
            ItemViewerUtility.renderHelpOverlay(context);
            ItemViewerUtility.renderFilterOverlay(context);
        }
    }
}

