package net.felix.mixin;

import net.felix.utilities.ItemViewer.ItemViewerUtility;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.lwjgl.glfw.GLFW;
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
        // Prüfe zuerst ESC für Hilfe-Overlay (höchste Priorität)
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (ItemViewerUtility.handleHelpOverlayEscape()) {
                cir.setReturnValue(true);
                return;
            }
        }
        
        // Prüfe dann ob Text-Input für Suchfeld behandelt werden soll (höhere Priorität)
        if (ItemViewerUtility.handleSearchFieldKeyPress(keyCode, scanCode, modifiers)) {
            cir.setReturnValue(true);
            return;
        }
        // Prüfe dann ob unser Keybind gedrückt wurde
        if (ItemViewerUtility.handleKeyPress(keyCode, scanCode, modifiers)) {
            // Keybind wurde behandelt, verhindere weitere Verarbeitung
            cir.setReturnValue(true);
            return;
        }
        
        // Handle keyboard input for Clipboard quantity text field (nur in HandledScreens)
        Screen screen = (Screen) (Object) this;
        if (screen instanceof HandledScreen) {
            if (net.felix.utilities.DragOverlay.ClipboardDraggableOverlay.handleQuantityTextFieldKeyPress(keyCode, scanCode, modifiers)) {
                cir.setReturnValue(true);
            }
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
        // Verwende instanceof für robustere Erkennung (funktioniert auch bei obfuscated class names)
        if (screen instanceof net.minecraft.client.gui.screen.ingame.InventoryScreen && screen instanceof HandledScreen<?>) {
            // Prüfe ob das Menü eines der Special Menus No JEI Zeichen enthält
            HandledScreen<?> handledScreen = (HandledScreen<?>) screen;
            net.minecraft.text.Text titleText = handledScreen.getTitle();
            String titleWithUnicode = titleText.getString(); // Behält Unicode-Zeichen
            if (net.felix.utilities.Overall.ZeichenUtility.containsSpecialMenusNoJei(titleWithUnicode)) {
                return; // KEINE JEI UI in diesen speziellen Menüs
            }
            
            // DEBUG: Logge nur wenn sich der Screen ändert
            if (!screenClassName.equals(lastRenderedScreen)) {
                System.out.println("[ItemViewer] Inventar erkannt: Spielerinventar - " + screenClassName);
                lastRenderedScreen = screenClassName;
            }
            
            MinecraftClient client = MinecraftClient.getInstance();
            // Update mouse position
            ItemViewerUtility.updateMousePosition(mouseX, mouseY);
            
            // Render Clipboard Overlay (wenn aktiviert)
            net.felix.utilities.DragOverlay.ClipboardDraggableOverlay.renderInGame(context, mouseX, mouseY, delta);
            
            // Render Item Viewer (als HandledScreen behandeln)
            ItemViewerUtility.renderItemViewerInScreen(context, client, handledScreen, mouseX, mouseY);
            
            // Rendere AspectOverlay NACH dem ItemViewer, damit es über allen Items liegt
            // (wird nur gerendert wenn Shift gedrückt und ItemViewer aktiv ist)
            if (ItemViewerUtility.isVisible()) {
                net.felix.utilities.Overall.Aspekte.AspectOverlay.renderForeground(context);
            }
            
            // Rendere Help-Overlay ganz am Ende, damit es über allem liegt (wie im Blueprint Shop)
            if (ItemViewerUtility.isHelpOverlayOpen()) {
                ItemViewerUtility.renderHelpOverlay(context);
            }
            return;
        }
        
        // Alle anderen Screens (inkl. andere HandledScreens) werden nicht hier behandelt
        // HandledScreens (Kisten, etc.) werden im HandledScreenMixin behandelt
    }
    
    /**
     * Injiziert in removed, um das Hilfe-Overlay zu schließen, wenn der Screen geschlossen wird
     */
    @Inject(method = "removed", at = @At("HEAD"))
    private void onRemoved(CallbackInfo ci) {
        // Schließe Hilfe-Overlay wenn Screen geschlossen wird
        ItemViewerUtility.closeHelpOverlay();
    }
}

