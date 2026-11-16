package net.felix.features.itemviewer.inventory;

import net.felix.features.itemviewer.data.ItemData;
import net.felix.utilities.Aincraft.blueprints.BPViewerUtility;
import net.felix.utilities.Aincraft.fishing.FishTrapFoundUtility;
import net.felix.utilities.Aincraft.fishing.FishingComponentFoundUtility;

/**
 * Prüft, ob ein Item-Viewer-Eintrag bereits gefunden wurde.
 */
public final class FoundItemChecker {

    private FoundItemChecker() {
    }

    public static boolean isFound(ItemData item) {
        if (item == null || item.name == null || item.name.isEmpty()) {
            return false;
        }
        if ("fishing_components".equals(item.category)) {
            return FishingComponentFoundUtility.isFound(item.name);
        }
        if ("fish_traps".equals(item.category)) {
            return FishTrapFoundUtility.isFound(item.name);
        }
        return isBlueprintFound(item.name);
    }

    public static boolean isBlueprintFound(String blueprintName) {
        try {
            BPViewerUtility bpViewer = BPViewerUtility.getInstance();
            if (blueprintName.equals("Drachenzahn")) {
                return bpViewer.isBlueprintFoundAnywhere(blueprintName + ":epic")
                        || bpViewer.isBlueprintFoundAnywhere(blueprintName + ":legendary");
            }
            return bpViewer.isBlueprintFoundAnywhere(blueprintName)
                    || bpViewer.isBlueprintFoundAnywhere(blueprintName + ":epic")
                    || bpViewer.isBlueprintFoundAnywhere(blueprintName + ":legendary");
        } catch (Exception e) {
            return false;
        }
    }
}
