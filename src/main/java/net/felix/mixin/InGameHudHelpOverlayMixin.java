package net.felix.mixin;

import net.felix.utilities.ItemViewer.ItemViewerUtility;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin für InGameHud, um das Hilfe-Overlay nach allen Screen-Render-Layern zu rendern
 * Wird am RETURN-Punkt injiziert, um sicherzustellen, dass es wirklich über allen Items liegt
 */
@Mixin(InGameHud.class)
public abstract class InGameHudHelpOverlayMixin {
    
    /**
     * Injiziert am RETURN-Punkt der render-Methode, um sicherzustellen, dass das Hilfe-Overlay
     * wirklich nach allen Screen-Render-Layern gerendert wird (auch nach Items)
     */
    @Inject(method = "render", at = @At(value = "RETURN"))
    private void renderHelpOverlayAtReturn(DrawContext context, float tickDelta, CallbackInfo ci) {
        // Rendere Help-Overlay nur wenn ein Screen offen ist und das Overlay geöffnet ist
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client != null && client.currentScreen != null && ItemViewerUtility.isHelpOverlayOpen()) {
            System.out.println("[DEBUG InGameHudHelpOverlayMixin] renderHelpOverlayAtReturn aufgerufen (InGameHud RETURN) - helpScreenOpen=" + ItemViewerUtility.isHelpOverlayOpen());
            ItemViewerUtility.renderHelpOverlay(context);
        }
    }
}

