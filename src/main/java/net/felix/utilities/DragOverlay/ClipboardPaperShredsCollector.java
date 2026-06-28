package net.felix.utilities.DragOverlay;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sammelt Pergamentfetzen für die Pinnwand:
 * <ul>
 *   <li>Sektion {@code other} in {@code collected_materials-ressources.json}</li>
 * </ul>
 */
public class ClipboardPaperShredsCollector {
    static final String STORAGE_NAME = "Pergamentfetzen";
    
    // Pattern für die Pergamentfetzen-Zeile im Tooltip: "780,080 / 186 Pergamentfetzen"
    // Format: "vorhandene Anzahl / benötigte Anzahl Pergamentfetzen"
    // Unterstützt Kommas als Tausendertrennzeichen (z.B. 1,000, 1,000,000)
    // Extrahiert die erste Zahl (vorhandene Anzahl) vor dem "/"
    private static final Pattern PAPER_SHREDS_PATTERN = Pattern.compile("(\\d{1,3}(?:,\\d{3})*|\\d+)\\s*/\\s*\\d+(?:[.,]\\d+)*\\s+Pergamentfetzen", Pattern.CASE_INSENSITIVE);

    /** Chat: {@code [Legend] Du erhältst 186 Pergamentfetzen.} */
    private static final Pattern PAPER_SHREDS_CHAT_PATTERN = Pattern.compile(
            "Du erhältst\\s+(\\d{1,3}(?:,\\d{3})*|\\d+(?:[.,]\\d+)*)\\s+Pergamentfetzen\\.?",
            Pattern.CASE_INSENSITIVE);
    
    /**
     * Initialisiert den ClipboardPaperShredsCollector
     */
    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(ClipboardPaperShredsCollector::onClientTick);
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (message != null) {
                processChatMessage(message.getString());
            }
        });
    }

    /**
     * Verarbeitet Server-Chat: {@code [Legend] Du erhältst ANZAHL Pergamentfetzen.}
     */
    public static void processChatMessage(String messageText) {
        if (messageText == null || messageText.isEmpty() || !shouldCollectPaperShreds()) {
            return;
        }
        String clean = stripFormatting(messageText);
        Matcher matcher = PAPER_SHREDS_CHAT_PATTERN.matcher(clean);
        if (!matcher.find()) {
            return;
        }
        long amount = parseAmount(matcher.group(1));
        if (amount > 0) {
            addPaperShreds(amount);
        }
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
            
            // Hole Tooltip über Data Component API (wie in ItemInfoUtility)
            List<Text> tooltip = new ArrayList<>();
            try {
                // Add item name
                tooltip.add(stack.getName());
                
                // Read lore from Data Component API (1.21.7) - wie in ItemInfoUtility
                var loreComponent = stack.get(net.minecraft.component.DataComponentTypes.LORE);
                if (loreComponent != null) {
                    tooltip.addAll(loreComponent.lines());
                }
            } catch (Exception e) {
                // Silent error handling
            }
            
            if (tooltip == null || tooltip.isEmpty()) {
                return;
            }
            
            // Parse Tooltip nach Pergamentfetzen-Anzahl
            // Durchsuche ALLE Zeilen des Tooltips (nicht nur die erste)
            for (int i = 0; i < tooltip.size(); i++) {
                Text line = tooltip.get(i);
                String lineText = line.getString();
                // Entferne Formatierungs-Codes und Unicode-Zeichen für bessere Erkennung
                String cleanLineText = lineText.replaceAll("§[0-9a-fk-or]", "")
                                               .replaceAll("[\\u3400-\\u4DBF]", "")
                                               .trim();
                
                Matcher matcher = PAPER_SHREDS_PATTERN.matcher(cleanLineText);
                if (matcher.find()) {
                    try {
                        // Entferne Tausendertrennzeichen (Kommas - Tooltip verwendet Kommas, nicht Punkte)
                        // Format: "1,000" oder "1,000,000" -> entferne alle Kommas
                        String amountStr = matcher.group(1).replace(",", "");
                        long amount = Long.parseLong(amountStr);
                        
                        setPaperShreds(amount);
                        break; // Gefunden, keine weiteren Zeilen prüfen
                    } catch (NumberFormatException e) {
                        // Silent error handling
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
     * Addiert erhaltene Pergamentfetzen zur aktuellen Anzahl (z. B. aus Server-Chat).
     */
    public static void addPaperShreds(long amount) {
        if (amount <= 0) {
            return;
        }
        CollectedMaterialsResourcesStorage.addOther(STORAGE_NAME, amount);
        ClipboardDraggableOverlay.invalidateRenderSnapshot();
    }

    /**
     * Gibt die aktuellen Pergamentfetzen zurück (Sektion {@code other}).
     */
    public static long getCurrentPaperShreds() {
        return CollectedMaterialsResourcesStorage.getOtherAmount(STORAGE_NAME);
    }
    
    /**
     * Setzt die Pergamentfetzen (Shop-Tooltip oder Testing).
     */
    public static void setPaperShreds(long amount) {
        CollectedMaterialsResourcesStorage.updateOther(STORAGE_NAME, Math.max(0L, amount));
        ClipboardDraggableOverlay.invalidateRenderSnapshot();
    }

    private static String stripFormatting(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("§[0-9a-fk-or]", "")
                .replaceAll("[\\u3400-\\u4DBF\\u4E00-\\u9FFF]", "")
                .trim();
    }

    private static long parseAmount(String amountStr) {
        if (amountStr == null || amountStr.isEmpty()) {
            return 0L;
        }
        try {
            String normalized = amountStr.replace(".", "").replace(",", "").trim();
            return Long.parseLong(normalized);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
