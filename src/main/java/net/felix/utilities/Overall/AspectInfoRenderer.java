package net.felix.utilities.Overall;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;

public class AspectInfoRenderer {
    
    public static void initialize() {
        // Register world render callback to draw the aspect GUI
        WorldRenderEvents.END.register(context -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.currentScreen != null) {
                // Render the aspect GUI as an overlay
                AspectInfoGUI.getInstance().renderOverlay();
            }
        });
    }
}
