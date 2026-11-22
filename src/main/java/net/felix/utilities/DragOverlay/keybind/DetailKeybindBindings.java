package net.felix.utilities.DragOverlay.keybind;

import java.util.ArrayList;
import net.felix.utilities.DragOverlay.DraggableOverlay;
import java.util.List;
import net.felix.utilities.Aincraft.blueprints.viewer.BPViewerUtility;
import net.felix.utilities.Aincraft.cardsstatues.overlay.CardsStatuesUtility;
import net.felix.utilities.Aincraft.kills.KillsUtility;
import net.felix.utilities.Aincraft.materials.tracker.MaterialTrackerUtility;
import net.felix.utilities.Overall.clipboard.ClipboardUtility;
import net.felix.utilities.Overall.itemviewer.ItemViewerHotkeys;
import net.minecraft.client.KeyMapping;

/**
 * Dieselben KeyMapping-Instanzen, die unter Controls liegen. Keine neuen Belegungen.
 */
public final class DetailKeybindBindings {

    private DetailKeybindBindings() {
    }

    public static DetailKeybindCatalog.Panel panelFor(DraggableOverlay overlay) {
        if (overlay instanceof net.felix.utilities.Aincraft.blueprints.viewer.BlueprintViewerDraggableOverlay) {
            return DetailKeybindCatalog.Panel.BLUEPRINT;
        }
        if (overlay instanceof net.felix.utilities.Aincraft.cardsstatues.overlay.CardsDraggableOverlay) {
            return DetailKeybindCatalog.Panel.CARDS;
        }
        if (overlay instanceof net.felix.utilities.Aincraft.cardsstatues.overlay.StatuesDraggableOverlay) {
            return DetailKeybindCatalog.Panel.STATUES;
        }
        if (overlay instanceof net.felix.utilities.Overall.clipboard.overlay.ClipboardDraggableOverlay) {
            return DetailKeybindCatalog.Panel.CLIPBOARD;
        }
        if (overlay instanceof net.felix.utilities.Aincraft.kills.KillsUtilityDraggableOverlay) {
            return DetailKeybindCatalog.Panel.KILLS;
        }
        if (overlay instanceof net.felix.utilities.Aincraft.materials.tracker.MaterialTrackerDraggableOverlay) {
            return DetailKeybindCatalog.Panel.MATERIAL;
        }
        return DetailKeybindCatalog.Panel.NONE;
    }

    public static List<KeyMapping> mappings(DetailKeybindCatalog.Panel panel) {
        List<KeyMapping> mappings = new ArrayList<>();
        switch (panel) {
            case BLUEPRINT -> {
                mappings.add(BPViewerUtility.toggleKey());
                mappings.add(BPViewerUtility.missingModeKey());
                mappings.add(BPViewerUtility.nextPageKey());
                mappings.add(BPViewerUtility.previousPageKey());
            }
            case CARDS, STATUES -> mappings.add(CardsStatuesUtility.toggleKey());
            case CLIPBOARD -> {
                mappings.add(ClipboardUtility.toggleKey());
                mappings.add(ItemViewerHotkeys.clipboardPinKey());
                mappings.add(ClipboardUtility.previousPageKey());
                mappings.add(ClipboardUtility.nextPageKey());
            }
            case KILLS -> {
                mappings.add(KillsUtility.toggleKey());
                mappings.add(KillsUtility.resetKey());
            }
            case MATERIAL -> {
                mappings.add(MaterialTrackerUtility.toggleKey());
                mappings.add(MaterialTrackerUtility.resetRateKey());
            }
            case NONE -> {
            }
        }
        return mappings;
    }
}
