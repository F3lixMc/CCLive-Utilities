package net.felix.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseClickMixin {
    
    /**
     * Injiziert in onMouseButton, um Clipboard-Button-Klicks im HUD zu behandeln
     * (wenn kein Screen offen ist)
     */
    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void onMouseButton(long window, MouseButtonInfo buttonInfo, int action, CallbackInfo ci) {
        int button = buttonInfo.button();
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.screen != null) {
            return; // Nur behandeln wenn kein Screen offen ist
        }
        
        // Nur Linksklick behandeln (button 0, action 1 = press)
        if (button == 0 && action == 1) {
            // Hole aktuelle Mausposition und skaliere auf Screen-Koordinaten
            if (client.getWindow() != null) {
                int windowWidth = client.getWindow().getScreenWidth();
                int windowHeight = client.getWindow().getScreenHeight();
                int mouseX = (int) (client.mouseHandler.xpos() * (double) client.getWindow().getGuiScaledWidth() / (double) windowWidth);
                int mouseY = (int) (client.mouseHandler.ypos() * (double) client.getWindow().getGuiScaledHeight() / (double) windowHeight);
                
                // Handle clicks on Clipboard Overlay buttons
                if (net.felix.utilities.DragOverlay.Overall.ClipboardDraggableOverlay.handleButtonClick(mouseX, mouseY)) {
                    // Button wurde geklickt - verhindere weitere Verarbeitung
                    ci.cancel();
                }
            }
        }
    }
}


