package net.felix.utilities.Overall.Aspekte;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;

public class AspectInfoRenderer {
    
    public static void initialize() {
        // Register world render callback to draw the aspect GUI
        LevelRenderEvents.END_MAIN.register(context -> {
            Minecraft client = Minecraft.getInstance();
            if (client.screen != null) {
                // Render the aspect GUI as an overlay
                AspectInfoGUI.getInstance().renderOverlay();
            }
        });
    }
}
