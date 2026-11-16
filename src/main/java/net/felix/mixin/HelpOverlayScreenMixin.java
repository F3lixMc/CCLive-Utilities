package net.felix.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.felix.features.itemviewer.ui.FilterOverlayControls;
import net.felix.features.itemviewer.ui.HelpOverlay;

/**
 * Separater Mixin für das Hilfe-Overlay in Screen, damit es wirklich über allem liegt
 * Wird mit RETURN injiziert, um sicherzustellen, dass es nach allem anderen gerendert wird
 */
@SuppressWarnings("unused")
@Mixin(Screen.class)
public abstract class HelpOverlayScreenMixin {
    
    /**
     * Injiziert am RETURN-Punkt der render-Methode, um sicherzustellen, dass das Hilfe-Overlay
     * wirklich nach allem anderen gerendert wird (auch nach Items)
     */
    @Inject(method = "extractRenderState", at = @At(value = "RETURN"))
    private void renderHelpOverlayAtReturn(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        // Im Spielerinventar zeichnet InventoryScreenMixin die Hilfe nach der Rangliste.
        if ((Object) this instanceof InventoryScreen) {
            return;
        }
        HelpOverlay.renderHelpOverlay(context);
        FilterOverlayControls.renderFilterOverlay(context);
    }
}

