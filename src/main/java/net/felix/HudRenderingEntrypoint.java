package net.felix;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

public class HudRenderingEntrypoint implements ClientModInitializer {
    private static final Identifier FONT_IDENTIFIER = Identifier.of("cclive-utilities", "default");

    @Override
    public void onInitializeClient() {
        // Verifiziere, dass die Font-Definition geladen wird
        // Die Font wird automatisch aus assets/cclive-utilities/font/default.json geladen
        // Silent error handling("✅ [CCLive-Utilities] Client initialisiert - Font sollte automatisch geladen werden");
        
        // Registriere Resource-Reload-Listener, um zu prüfen, ob die Font geladen wird
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(
            new SimpleSynchronousResourceReloadListener() {
                @Override
                public Identifier getFabricId() {
                    return Identifier.of("cclive-utilities", "font_verification");
                }
                
                @Override
                public void reload(ResourceManager manager) {
                    // Prüfe, ob die Font-Definition existiert
                    var fontResource = manager.getResource(FONT_IDENTIFIER.withPath("font/default.json"));
                    if (fontResource.isPresent()) {
                        // Silent error handling("✅ [CCLive-Utilities] Font-Definition gefunden: " + FONT_IDENTIFIER);
                    } else {
                        // Silent error handling("❌ [CCLive-Utilities] Font-Definition NICHT gefunden: " + FONT_IDENTIFIER);
                    }
                    
                    // Prüfe, ob die Icon-Textur existiert
                    var iconResource = manager.getResource(Identifier.of("cclive-utilities", "textures/8_chat_icon.png"));
                    if (iconResource.isPresent()) {
                        // Silent error handling("✅ [CCLive-Utilities] Icon-Textur gefunden: textures/8_chat_icon.png");
                    } else {
                        // Silent error handling("❌ [CCLive-Utilities] Icon-Textur NICHT gefunden: textures/8_chat_icon.png");
                    }
                }
            }
        );
        
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
