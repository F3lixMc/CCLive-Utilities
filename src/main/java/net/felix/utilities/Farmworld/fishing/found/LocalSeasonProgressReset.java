package net.felix.utilities.Farmworld.fishing.found;

import net.felix.Community.profile.ProfileStatsManager;
import net.felix.utilities.Aincraft.blueprints.viewer.BPViewerUtility;
import net.felix.utilities.Other.clipboard.collector.ClipboardAmbossRessourceCollector;
import net.felix.utilities.Other.clipboard.overlay.ClipboardDraggableOverlay;
import net.felix.utilities.Other.clipboard.storage.CollectedMaterialsResourcesStorage;

/**
 * Rein lokaler Season-Progress-Reset der sechs Fortschrittsdateien.
 * Keine Backend-Reset-/Löschaufrufe.
 */
public final class LocalSeasonProgressReset {

    private LocalSeasonProgressReset() {
    }

    /**
     * Setzt die sechs saisonalen Fortschrittsdateien + Memory/Caches zurück und speichert synchron.
     * @return true nur wenn alle sechs Persistenzschritte erfolgreich waren
     */
    public static boolean resetAllSixProgressFiles() {
        boolean blueprintsOk = BPViewerUtility.getInstance().resetLocalBlueprintProgressFiles();
        boolean fishingOk = FishingComponentFoundUtility.resetReturningSuccess();
        boolean trapsOk = FishTrapFoundUtility.resetReturningSuccess();
        boolean materialsOk = CollectedMaterialsResourcesStorage.resetAllAndFlushSync();
        try {
            ClipboardDraggableOverlay.resetCollectedMaterials();
            ClipboardAmbossRessourceCollector.resetCollectedResources();
        } catch (Exception ignored) {
        }
        boolean cardsOk = ProfileStatsManager.getInstance().resetCardsStatuesLevelsLocalSync();

        return blueprintsOk && fishingOk && trapsOk && materialsOk && cardsOk;
    }
}
