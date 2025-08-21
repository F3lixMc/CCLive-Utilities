package net.felix.utilities;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class AspectOverlayRenderer {
    
    public static void initialize() {
        // Note: We now use a Mixin approach for foreground rendering
        // The old HUD renderer is disabled since it renders in the background
        System.out.println("DEBUG: AspectOverlayRenderer initialized - using Mixin for foreground rendering");
        
        // Uncomment the following lines if you want to fall back to HUD rendering
        /*
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            // Render the aspect overlay in all situations with high priority
            AspectOverlay.render(drawContext);
        });
        */
    }
}
