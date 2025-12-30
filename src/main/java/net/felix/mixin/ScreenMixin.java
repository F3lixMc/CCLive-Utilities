package net.felix.mixin;

import net.felix.utilities.ItemViewer.ItemViewerUtility;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin für Screen, um Keybind-Handling und Text-Input auch in geöffneten Screens zu ermöglichen
 */
@Mixin(Screen.class)
public class ScreenMixin {
    
    /**
     * Injiziert in keyPressed, um unseren Item-Viewer-Keybind auch in Screens zu behandeln
     */
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        // Prüfe zuerst ob Text-Input für Suchfeld behandelt werden soll (höhere Priorität)
        if (ItemViewerUtility.handleSearchFieldKeyPress(keyCode, scanCode, modifiers)) {
            cir.setReturnValue(true);
            return;
        }
        // Prüfe dann ob unser Keybind gedrückt wurde
        if (ItemViewerUtility.handleKeyPress(keyCode, scanCode, modifiers)) {
            // Keybind wurde behandelt, verhindere weitere Verarbeitung
            cir.setReturnValue(true);
        }
    }
    
    /**
     * Injiziert in render, um Item Viewer auch in nicht-HandledScreen Screens zu rendern (z.B. Spielerinventar)
     */
    // Debug: Letzter gerenderter Screen (um Logs zu reduzieren)
    private static String lastRenderedScreen = "";
    
    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        Screen screen = (Screen) (Object) this;
        String screenClassName = screen.getClass().getName();
        
        // Prüfe zuerst ob es ein InventoryScreen ist (Spielerinventar)
        // InventoryScreen ist ein HandledScreen, wird aber hier explizit behandelt
        if (screenClassName.contains("InventoryScreen") && screen instanceof HandledScreen<?>) {
            // DEBUG: Logge nur wenn sich der Screen ändert
            if (!screenClassName.equals(lastRenderedScreen)) {
                System.out.println("[ItemViewer] Inventar erkannt: Spielerinventar - " + screenClassName);
                lastRenderedScreen = screenClassName;
            }
            
            MinecraftClient client = MinecraftClient.getInstance();
            // Update mouse position
            ItemViewerUtility.updateMousePosition(mouseX, mouseY);
            // Render Item Viewer (als HandledScreen behandeln)
            ItemViewerUtility.renderItemViewerInScreen(context, client, (HandledScreen<?>) screen, mouseX, mouseY);
            return;
        }
        
        // Alle anderen Screens (inkl. andere HandledScreens) werden nicht hier behandelt
        // HandledScreens (Kisten, etc.) werden im HandledScreenMixin behandelt
    }
}

