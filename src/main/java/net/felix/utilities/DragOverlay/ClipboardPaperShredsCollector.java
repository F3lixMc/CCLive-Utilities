package net.felix.utilities.DragOverlay;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sammelt Pergamentfetzen-Anzahl aus Slot 10 im "Bauplan [Shop]" Menü
 * Liest die Anzahl aus dem Tooltip des Items in Slot 10
 */
public class ClipboardPaperShredsCollector {
    private static long currentPaperShreds = 0;
    
    // Pattern für die Pergamentfetzen-Zeile im Tooltip: "100 / 186 Pergamentfetzen" oder "1,000 / 1,260,636 Pergamentfetzen"
    // Unterstützt Kommas als Tausendertrennzeichen (z.B. 1,000, 1,000,000)
    private static final Pattern PAPER_SHREDS_PATTERN = Pattern.compile("(\\d{1,3}(?:,\\d{3})*|\\d+)\\s*/\\s*\\d+(?:[.,]\\d+)*\\s+Pergamentfetzen", Pattern.CASE_INSENSITIVE);
    
    /**
     * Initialisiert den ClipboardPaperShredsCollector
     */
    public static void initialize() {
        // Registriere Tick-Event
        ClientTickEvents.END_CLIENT_TICK.register(ClipboardPaperShredsCollector::onClientTick);
    }
    
    /**
     * Wird jeden Tick aufgerufen
     * Läuft nur wenn alle Bedingungen erfüllt sind:
     * - Clipboard aktiviert und Baupläne vorhanden
     * - Inventar geöffnet
     * - "Bauplan [Shop]" Menü
     */
    private static void onClientTick(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return;
        }
        
        // Prüfe zuerst ob Clipboard aktiv ist und Baupläne vorhanden sind
        if (!shouldCollectPaperShreds()) {
            return;
        }
        
        // Prüfe ob ein Inventar geöffnet ist
        if (!(client.currentScreen instanceof HandledScreen<?> handledScreen)) {
            return;
        }
        
        // Prüfe ob wir im "Bauplan [Shop]" Menü sind
        net.minecraft.text.Text titleText = handledScreen.getTitle();
        String screenTitle = titleText != null ? titleText.getString() : "";
        String cleanTitle = screenTitle.replaceAll("§[0-9a-fk-or]", "").trim();
        
        if (!cleanTitle.contains("Bauplan [Shop]") && !cleanTitle.contains("Blueprint Store")) {
            return;
        }
        
        // Lese Slot 10 aus
        try {
            Slot slot = handledScreen.getScreenHandler().slots.get(10);
            if (slot == null) {
                return;
            }
            
            ItemStack stack = slot.getStack();
            if (stack.isEmpty()) {
                return;
            }
            
            // Hole Tooltip über Reflection (wie in MenuHoverCollector - probiere verschiedene Signaturen)
            List<Text> tooltip = null;
            try {
                java.lang.reflect.Method[] methods = HandledScreen.class.getDeclaredMethods();
                for (java.lang.reflect.Method method : methods) {
                    if (!"getTooltipFromItem".equals(method.getName())) {
                        continue;
                    }
                    
                    Class<?>[] params = method.getParameterTypes();
                    method.setAccessible(true);
                    
                    try {
                        // Signatur: getTooltipFromItem(MinecraftClient, ItemStack)
                        if (params.length == 2 &&
                            net.minecraft.client.MinecraftClient.class.isAssignableFrom(params[0]) &&
                            ItemStack.class.isAssignableFrom(params[1])) {
                            @SuppressWarnings("unchecked")
                            List<Text> result = (List<Text>) method.invoke(handledScreen, client, stack);
                            tooltip = result;
                            break;
                        }
                        
                        // Signatur: getTooltipFromItem(ItemStack)
                        if (params.length == 1 &&
                            ItemStack.class.isAssignableFrom(params[0])) {
                            @SuppressWarnings("unchecked")
                            List<Text> result = (List<Text>) method.invoke(handledScreen, stack);
                            tooltip = result;
                            break;
                        }
                    } catch (Exception e) {
                        // Versuche nächste Signatur
                        continue;
                    }
                }
                
            } catch (Exception e) {
                // Fehler beim Suchen der Methode - ignoriere
            }
            
            if (tooltip != null && !tooltip.isEmpty()) {
                // Parse Tooltip nach Pergamentfetzen-Anzahl
                for (Text line : tooltip) {
                    String lineText = line.getString();
                    Matcher matcher = PAPER_SHREDS_PATTERN.matcher(lineText);
                    if (matcher.find()) {
                        try {
                            // Entferne Tausendertrennzeichen (Kommas - Tooltip verwendet Kommas, nicht Punkte)
                            // Format: "1,000" oder "1,000,000" -> entferne alle Kommas
                            String amountStr = matcher.group(1).replace(",", "");
                            long amount = Long.parseLong(amountStr);
                            currentPaperShreds = amount;
                            break; // Gefunden, keine weiteren Zeilen prüfen
                        } catch (NumberFormatException e) {
                            // Fehler beim Parsen - ignoriere
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Fehler beim Zugriff auf Slot 10 - ignoriere
        }
    }
    
    /**
     * Prüft ob Pergamentfetzen gesammelt werden sollen
     * Nur wenn Clipboard aktiviert ist und Baupläne vorhanden sind
     */
    private static boolean shouldCollectPaperShreds() {
        // Prüfe ob Clipboard aktiviert ist
        boolean clipboardEnabled = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().clipboardEnabled;
        boolean showClipboard = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showClipboard;
        
        if (!clipboardEnabled || !showClipboard) {
            return false;
        }
        
        // Prüfe ob Baupläne vorhanden sind
        List<ClipboardUtility.ClipboardEntry> entries = ClipboardUtility.getEntries();
        return entries != null && !entries.isEmpty();
    }
    
    /**
     * Gibt die aktuellen Pergamentfetzen zurück
     */
    public static long getCurrentPaperShreds() {
        return currentPaperShreds;
    }
    
    /**
     * Setzt die Pergamentfetzen manuell (für Testing)
     */
    public static void setPaperShreds(long amount) {
        currentPaperShreds = amount;
    }
}
