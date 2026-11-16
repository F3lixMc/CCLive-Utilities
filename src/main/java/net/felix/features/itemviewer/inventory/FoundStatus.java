package net.felix.features.itemviewer.inventory;

import com.google.gson.JsonObject;
import net.felix.features.itemviewer.data.ItemData;
import net.felix.features.itemviewer.tooltip.TooltipCache;
import net.felix.utilities.Aincraft.blueprints.BPViewerUtility;
import net.minecraft.client.Minecraft;

import static net.felix.features.itemviewer.search.ItemFilterPipeline.applyFilters;
import static net.felix.features.itemviewer.search.ItemFilterPipeline.currentSortMode;

/**
 * Gefunden-Status von Bauplaenen und Items samt Anzeigetext und Farbe.
 */
public final class FoundStatus {

    private FoundStatus() {
    }

    // Map von Blueprint-Namen zu vollständigen JSON-Objekten (für Favoriten)
    public static java.util.Map<String, JsonObject> blueprintJsonMap = new java.util.HashMap<>();

    /**
     * Aktualisiert den Item Viewer, wenn sich der Gefunden-Status ändert
     * (z. B. „Nicht Gefunden“-Filter sofort anwenden).
     * Läuft immer auf dem Client-Thread (auch bei Chat-/Netzwerk-Callbacks).
     */
    public static void onFoundStatusChanged() {
        Runnable refresh = () -> {
            net.felix.utilities.Other.clipboard.overlay.ClipboardDraggableOverlay.invalidateRenderSnapshot();
            if (currentSortMode.filtersNotFoundOnly()) {
                applyFilters();
                TooltipCache.invalidateTooltipCache();
            }
        };
        Minecraft client = Minecraft.getInstance();
        if (client != null && client.isSameThread()) {
            refresh.run();
        } else if (client != null) {
            client.execute(refresh);
        } else {
            refresh.run();
        }
    }

    /**
     * Prüft, ob ein Blueprint gefunden wurde (ähnlich wie im Moblexicon)
     * @param blueprintName Der Name des Blueprints (z.B. "Abgenutze Plattenstampfer")
     * @return true wenn gefunden, false wenn nicht gefunden
     */
    public static boolean isBlueprintFound(String blueprintName) {
        if (blueprintName == null || blueprintName.isEmpty()) {
            return false;
        }
        
        try {
            BPViewerUtility bpViewer = BPViewerUtility.getInstance();
            
            // Prüfe foundBlueprints (ähnlich wie in BPViewerUtility.isBlueprintFoundAnywhere)
            // Für Drachenzahn: prüfe beide Rarities separat
            if (blueprintName.equals("Drachenzahn")) {
                return bpViewer.isBlueprintFoundAnywhere(blueprintName + ":epic") ||
                       bpViewer.isBlueprintFoundAnywhere(blueprintName + ":legendary");
            } else {
                // Für andere Blueprints: prüfe mit und ohne Rarity-Suffix
                return bpViewer.isBlueprintFoundAnywhere(blueprintName) ||
                       bpViewer.isBlueprintFoundAnywhere(blueprintName + ":epic") ||
                       bpViewer.isBlueprintFoundAnywhere(blueprintName + ":legendary");
            }
        } catch (Exception e) {
            // Falls BPViewerUtility nicht verfügbar ist
            return false;
        }
    }

    /**
     * Gibt den gefundenen-Status als Text zurück (für Anzeige)
     * @param item Das Item, das geprüft werden soll
     * @return "[Gefunden]" in grün oder "[Nicht Gefunden]" in rot
     */
    public static String getFoundStatusText(ItemData item) {
        if (item.info == null || !Boolean.TRUE.equals(item.info.blueprint)) {
            return null; // Kein Bauplan, kein Status
        }
        
        boolean isFound = isItemFound(item);
        return isFound ? "[Gefunden]" : "[Nicht Gefunden]";
    }

    /**
     * Gibt die Farbe für den gefundenen-Status zurück
     * @param item Das Item, das geprüft werden soll
     * @return 0xFF00FF00 (grün) wenn gefunden, 0xFFFF0000 (rot) wenn nicht gefunden, 0xFFFFFFFF (weiß) wenn kein Bauplan
     */
    public static int getFoundStatusColor(ItemData item) {
        if (item.info == null || !Boolean.TRUE.equals(item.info.blueprint)) {
            return 0xFFFFFFFF; // Kein Bauplan, weiß
        }
        
        boolean isFound = isItemFound(item);
        return isFound ? 0xFF00FF00 : 0xFFFF0000; // Grün wenn gefunden, rot wenn nicht
    }

    public static boolean isItemFound(ItemData item) {
        if (item == null || item.name == null || item.name.isEmpty()) {
            return false;
        }
        if ("fishing_components".equals(item.category)) {
            return net.felix.utilities.Aincraft.fishing.FishingComponentFoundUtility.isFound(item.name);
        }
        if ("fish_traps".equals(item.category)) {
            return net.felix.utilities.Aincraft.fishing.FishTrapFoundUtility.isFound(item.name);
        }
        return isBlueprintFound(item.name);
    }
}
