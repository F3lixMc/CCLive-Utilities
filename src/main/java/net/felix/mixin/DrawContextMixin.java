package net.felix.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;
import net.felix.utilities.Overall.ZeichenUtility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Mixin for DrawContext to disable the dark inventory overlay for Equipment Display screens.
 */
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
                if (ZeichenUtility.containsEquipmentDisplay(title)) { //Equipment Display
                    ci.cancel(); // Rendering abbrechen → Overlay wird nicht gezeichnet
                }
            }
        }
    }
    
    /**
     * Blockiert Item-Tooltips wenn das Hilfe-Overlay vom ItemViewer offen ist
     * Blockiert alle Tooltips außer bekannten Button-Tooltips (z.B. "Hilfs Übersicht", "Favoriten", etc.)
     * Funktioniert für alle Screens, einschließlich InventoryScreen (Spieler-Inventar)
     */
    @Inject(method = "drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;II)V", at = @At("HEAD"), cancellable = true)
    private void blockItemTooltipsWhenHelpOverlayOpen(TextRenderer textRenderer, List<Text> lines, int x, int y, CallbackInfo ci) {
        // Blockiere nur wenn Hilfe-Overlay offen ist
        if (net.felix.utilities.ItemViewer.ItemViewerUtility.isHelpOverlayOpen()) {
            if (lines == null || lines.isEmpty()) {
                return;
            }
            
            // Liste bekannter Button-Tooltip-Texte, die NICHT blockiert werden sollen
            java.util.Set<String> allowedButtonTooltips = java.util.Set.of(
                "Hilfs Übersicht",
                "Sonderzeichen",
                "Favoriten",
                "Linksklick: Nach Kit Suchen",
                "Rechtsklick: Kit auswählen",
                "ItemViewer einklappen",
                "ItemViewer ausklappen"
            );
            
            // Prüfe ob es ein bekannter Button-Tooltip ist
            boolean isAllowedTooltip = false;
            for (Text line : lines) {
                if (line != null) {
                    String text = line.getString().trim();
                    // Entferne Formatierungs-Codes für Vergleich
                    String cleanText = text.replaceAll("§[0-9a-fk-or]", "");
                    if (allowedButtonTooltips.contains(cleanText)) {
                        isAllowedTooltip = true;
                        break;
                    }
                    // Prüfe auch auf Hotkey-Zeilen (z.B. "(Hotkey: I)")
                    if (cleanText.startsWith("(Hotkey:")) {
                        isAllowedTooltip = true;
                        break;
                    }
                }
            }
            
            // Blockiere alle Tooltips außer den erlaubten Button-Tooltips
            // Item-Tooltips haben normalerweise mehrere Zeilen oder enthalten Item-Informationen
            // Button-Tooltips haben normalerweise nur 1-2 Zeilen und sind in der erlaubten Liste
            // Diese Blockierung funktioniert für alle Screens, einschließlich InventoryScreen
            if (!isAllowedTooltip) {
                ci.cancel();
            }
        }
    }
}

