package net.felix.mixin;

import com.mojang.renderpearl.api.commands.RenderPass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.state.WindowRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Minecraft 26.3 wirft, wenn ein Scissor nach dem GUI-Scale die Breite oder Höhe 0 hat
 * (laufende Button-Beschriftung außerhalb des Fensters, MC-310211). Der Draw bleibt,
 * nur der leere Schnitt wird verworfen.
 */
@SuppressWarnings("unused")
@Mixin(GuiRenderer.class)
public class GuiRendererScissorMixin {

    @Inject(method = "enableScissor", at = @At("HEAD"), cancellable = true)
    private void cclive$skipNonPositiveScissor(ScreenRectangle rectangle, RenderPass pass, CallbackInfo ci) {
        if (rectangle == null || pass == null) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return;
        }
        WindowRenderState window = client.gameRenderer.gameRenderState().windowRenderState;
        int scale = Math.max(1, window.guiScale);
        int left = rectangle.left() * scale;
        int top = rectangle.top() * scale;
        int right = Math.min(rectangle.right() * scale, window.width);
        int bottom = Math.min(rectangle.bottom() * scale, window.height);
        if (Math.max(0, right - left) > 0 && Math.max(0, bottom - top) > 0) {
            return;
        }
        pass.disableScissor();
        ci.cancel();
    }
}
