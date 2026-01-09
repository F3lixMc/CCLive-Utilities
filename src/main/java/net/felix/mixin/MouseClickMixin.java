package net.felix.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseClickMixin {
    
    /**
     * Injiziert in onMouseButton, um Clipboard-Button-Klicks im HUD zu behandeln
     * (wenn kein Screen offen ist)
     */
    @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
    private void onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.currentScreen != null) {
            return; // Nur behandeln wenn kein Screen offen ist
        }
        
        // Nur Linksklick behandeln (button 0, action 1 = press)
        if (button == 0 && action == 1) {
            // Hole aktuelle Mausposition und skaliere auf Screen-Koordinaten
            if (client.getWindow() != null) {
                int windowWidth = client.getWindow().getWidth();
                int windowHeight = client.getWindow().getHeight();
                int mouseX = (int) (client.mouse.getX() * (double) client.getWindow().getScaledWidth() / (double) windowWidth);
                int mouseY = (int) (client.mouse.getY() * (double) client.getWindow().getScaledHeight() / (double) windowHeight);
                
                // Handle clicks on Clipboard Overlay buttons
                if (net.felix.utilities.DragOverlay.ClipboardDraggableOverlay.handleButtonClick(mouseX, mouseY)) {
                    // Button wurde geklickt - verhindere weitere Verarbeitung
                    ci.cancel();
                }
            }
        }
    }
}


