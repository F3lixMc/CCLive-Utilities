package net.felix.utilities.Overall.Aspekte;

public class AspectOverlayRenderer {
    
    public static void initialize() {
        // Note: We now use a Mixin approach for foreground rendering
        // The old HUD renderer is disabled since it renders in the background
        
        // Uncomment the following lines if you want to fall back to HUD rendering
        /*
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            // Render the aspect overlay in all situations with high priority
            AspectOverlay.render(drawContext);
        });
        */
    }
}
