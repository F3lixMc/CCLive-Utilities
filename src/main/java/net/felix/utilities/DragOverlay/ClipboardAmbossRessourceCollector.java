package net.felix.utilities.DragOverlay;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sammelt Amboss- und Ressourcen-Anzahlen aus dem Ressourcen-Bag Inventar (Symbol Ⳅ)
 * Liest die Anzahl aus den Tooltips der Items im Format: "Name\nAnzahl x\nFür Details klicken"
 */
public class ClipboardAmbossRessourceCollector {
    private static final Map<String, Long> ambossItems = new HashMap<>(); // ItemName -> Anzahl
    private static final Map<String, Long> ressourceItems = new HashMap<>(); // ItemName -> Anzahl
    
    // Pattern für die Anzahl-Zeile im Tooltip: "Anzahl" + Leerzeichen + Zahl (z.B. "Anzahl 10000", "Anzahl 1,000")
    // Format: "Anzahl" (gold) + " " + Zahl (white)
    // Matcht: eine oder mehrere Ziffern, gefolgt von optionalen Komma-Gruppen (jeweils 3 Ziffern)
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("Anzahl\\s+(\\d+(?:,\\d{3})*)", Pattern.CASE_INSENSITIVE);
    
    // Cache für den letzten Screen-Titel, um zu erkennen wenn sich das Inventar ändert
    private static String lastScreenTitle = "";
    private static int lastSlotCount = 0;
    
    /**
     * Initialisiert den ClipboardAmbossRessourceCollector
     */
    public static void initialize() {
        // Registriere Tick-Event
        ClientTickEvents.END_CLIENT_TICK.register(ClipboardAmbossRessourceCollector::onClientTick);
    }
    
    /**
     * Wird jeden Tick aufgerufen
     * Läuft nur wenn alle Bedingungen erfüllt sind:
     * - Clipboard aktiviert und Baupläne vorhanden
     * - Inventar geöffnet
     * - Ressourcen-Bag Inventar (Symbol Ⳅ im Titel)
     */
    private static void onClientTick(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return;
        }
        
        // Prüfe zuerst ob Clipboard aktiv ist und Baupläne vorhanden sind
        if (!shouldCollectAmbossRessource()) {
            return;
        }
        
        // Prüfe ob ein Inventar geöffnet ist
        if (!(client.currentScreen instanceof HandledScreen<?> handledScreen)) {
            // Inventar geschlossen - Cache zurücksetzen
            lastScreenTitle = "";
            lastSlotCount = 0;
            return;
        }
        
        // Prüfe ob wir im Ressourcen-Bag Inventar sind (Symbol Ⳅ im Titel)
        net.minecraft.text.Text titleText = handledScreen.getTitle();
        String screenTitle = titleText != null ? titleText.getString() : "";
        
        if (!net.felix.utilities.Overall.ZeichenUtility.containsUiRessourceBag(screenTitle)) {
            // Nicht im Ressourcen-Bag - Cache zurücksetzen
            lastScreenTitle = "";
            lastSlotCount = 0;
            return;
        }
        
        // Prüfe ob sich das Inventar geändert hat (neue Seite oder neu geladen)
        int currentSlotCount = handledScreen.getScreenHandler().slots.size();
        boolean inventoryChanged = !screenTitle.equals(lastScreenTitle) || currentSlotCount != lastSlotCount;
        
        if (inventoryChanged) {
            // Inventar hat sich geändert - aktualisiere Cache, aber lösche NICHT die Werte
            // Die Werte bleiben erhalten, auch wenn wir die Seite wechseln
            lastScreenTitle = screenTitle;
            lastSlotCount = currentSlotCount;
        }
        
        // Sammle Items aus allen Slots
        // WICHTIG: Wir überschreiben nur die Werte, die wir auf der aktuellen Seite sehen
        // Werte, die nicht auf der aktuellen Seite sind, bleiben erhalten
        try {
            List<Slot> slots = handledScreen.getScreenHandler().slots;
            for (Slot slot : slots) {
                ItemStack stack = slot.getStack();
                if (stack.isEmpty()) {
                    continue;
                }
                
                // Hole Tooltip über Data Component API (wie in ItemInfoUtility)
                List<Text> tooltip = getTooltip(handledScreen, client, stack);
                if (tooltip == null || tooltip.isEmpty()) {
                    continue;
                }
                
                // Parse Tooltip nach Format: "Name\nAnzahl 10000\n\nFür Details klicken"
                // Format: Zeile 0 = Name, Zeile 1 = "Anzahl" + " " + Zahl, Zeile 2 = leer, Zeile 3 = "Für Details klicken"
                String itemName = null;
                long amount = 0;
                
                // Durchsuche alle Tooltip-Zeilen
                for (int i = 0; i < tooltip.size(); i++) {
                    String lineText = tooltip.get(i).getString();
                    // Entferne Formatierungs-Codes und Unicode-Zeichen für bessere Erkennung
                    String cleanLineText = lineText.replaceAll("§[0-9a-fk-or]", "")
                                                   .replaceAll("[\\u3400-\\u4DBF]", "")
                                                   .trim();
                    
                    // Erste Zeile ist der Name
                    if (i == 0) {
                        itemName = cleanLineText;
                        continue;
                    }
                    
                    // Suche nach "Anzahl" + Zahl in jeder Zeile
                    Matcher matcher = AMOUNT_PATTERN.matcher(cleanLineText);
                    if (matcher.find()) {
                        try {
                            // Entferne Tausendertrennzeichen (Kommas)
                            String amountStr = matcher.group(1).replace(",", "");
                            amount = Long.parseLong(amountStr);
                            break; // Gefunden, keine weiteren Zeilen prüfen
                        } catch (NumberFormatException e) {
                            // Silent error handling
                        }
                    }
                }
                
                // Wenn wir Name und Anzahl haben, speichere es
                if (itemName != null && !itemName.isEmpty() && amount > 0) {
                    // Entferne Formatierungs-Codes und Unicode-Zeichen vom Item-Namen für Vergleich
                    String cleanItemName = itemName.replaceAll("§[0-9a-fk-or]", "")
                                                   .replaceAll("[\\u3400-\\u4DBF]", "")
                                                   .trim();
                    
                    // Prüfe ob es ein Amboss- oder Ressource-Item ist
                    // Wir müssen prüfen, ob dieses Item in den Clipboard-Einträgen als Amboss oder Ressource verwendet wird
                    boolean isAmboss = isAmbossItem(cleanItemName);
                    boolean isRessource = isRessourceItem(cleanItemName);
                    
                    if (isAmboss) {
                        ambossItems.put(cleanItemName, amount);
                    } else if (isRessource) {
                        ressourceItems.put(cleanItemName, amount);
                    }
                }
            }
        } catch (Exception e) {
            // Fehler beim Zugriff auf Slots - ignoriere
        }
    }
    
    /**
     * Holt den Tooltip eines Items über Data Component API (wie in ItemInfoUtility)
     */
    private static List<Text> getTooltip(HandledScreen<?> handledScreen, MinecraftClient client, ItemStack stack) {
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
        return tooltip.isEmpty() ? null : tooltip;
    }
    
    /**
     * Prüft ob ein Item ein Amboss-Item ist (basierend auf den Clipboard-Einträgen)
     */
    private static boolean isAmbossItem(String itemName) {
        List<ClipboardUtility.ClipboardEntry> entries = ClipboardUtility.getEntries();
        if (entries == null) {
            return false;
        }
        
        for (ClipboardUtility.ClipboardEntry entry : entries) {
            if (entry.price != null && entry.price.Amboss != null && 
                entry.price.Amboss.itemName != null && entry.price.Amboss.itemName.equals(itemName)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Prüft ob ein Item ein Ressource-Item ist (basierend auf den Clipboard-Einträgen)
     */
    private static boolean isRessourceItem(String itemName) {
        List<ClipboardUtility.ClipboardEntry> entries = ClipboardUtility.getEntries();
        if (entries == null) {
            return false;
        }
        
        for (ClipboardUtility.ClipboardEntry entry : entries) {
            if (entry.price != null && entry.price.Ressource != null && 
                entry.price.Ressource.itemName != null && entry.price.Ressource.itemName.equals(itemName)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Prüft ob Amboss/Ressource gesammelt werden sollen
     * Nur wenn Clipboard aktiviert ist und Baupläne vorhanden sind
     */
    private static boolean shouldCollectAmbossRessource() {
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
     * Gibt die aktuelle Anzahl eines Amboss-Items zurück
     */
    public static long getAmbossAmount(String itemName) {
        return ambossItems.getOrDefault(itemName, 0L);
    }
    
    /**
     * Gibt die aktuelle Anzahl eines Ressource-Items zurück
     */
    public static long getRessourceAmount(String itemName) {
        return ressourceItems.getOrDefault(itemName, 0L);
    }
}
