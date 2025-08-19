package net.felix.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DrawContext.class)
public class DrawContextMixin {

    @Inject(method = "fillGradient", at = @At("HEAD"), cancellable = true)
    private void disableInventoryDarkOverlay(
        int x1, int y1, int x2, int y2, int colorTop, int colorBottom, CallbackInfo ci
    ) {
        // Prüfen, ob es der dunkle Inventar-Overlay ist (die typischen Farben)
        if (colorTop == 0xC0101010 && colorBottom == 0xD0101010) {
            // Prüfen, ob wir in einem "Ausrüstung" Inventar sind
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.currentScreen instanceof HandledScreen<?> handledScreen) {
                String title = handledScreen.getTitle().getString();
                if (title.contains("㬨") || title.contains("㬥") || title.contains("㬦") || title.contains("㬧")) {
                    ci.cancel(); // Rendering abbrechen → Overlay wird nicht gezeichnet
                }
            }
        }
    }
} 
