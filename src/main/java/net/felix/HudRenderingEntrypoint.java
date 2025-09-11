package net.felix;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

public class HudRenderingEntrypoint implements ClientModInitializer {
    private static final Identifier TEST_TEXTURE = Identifier.of("cclive-utilities", "textures/test/testbild.png");

    @Override
    public void onInitializeClient() {
        // HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
        //     MinecraftClient client = MinecraftClient.getInstance();
        //     if (client.player == null) return;

        //     int scaledWidth = client.getWindow().getScaledWidth();
        //     int scaledHeight = client.getWindow().getScaledHeight();

        //     int textureWidth = 32; // Assuming testbild.png is 32x32
        //     int textureHeight = 32;

        //     int x = (scaledWidth - textureWidth) / 2;
        //     int y = (scaledHeight - textureHeight) / 2;

        //     // Use drawTexturedQuad which doesn't require RenderPipeline!
        //     try {
        //         // drawTexturedQuad(Identifier sprite, int x1, int y1, int x2, int y2, float u1, float u2, float v1, float v2)
        //         drawContext.drawTexturedQuad(TEST_TEXTURE, x, y, x + textureWidth, y + textureHeight, 0.0f, 1.0f, 0.0f, 1.0f);
        //     } catch (Exception e) {
        //         // Fallback to green box if texture rendering fails
        //         drawContext.fill(x, y, x + textureWidth, y + textureHeight, 0xFF00FF00);
        //     }
        // });
        
        // TODO: Hier kannst du später deine HUD-Rendering-Features implementieren
    }
}
