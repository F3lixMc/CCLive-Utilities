package net.felix.utilities.Overall.eventsound.ui;

import java.util.function.Consumer;
import net.felix.utilities.Overall.itemviewer.data.ItemData;
import net.felix.utilities.Overall.itemviewer.search.ItemFilterPipeline;
import net.felix.utilities.Overall.itemviewer.ui.ViewerState;

/**
 * Öffnet den ItemViewer so, dass nur Aincraft-Baupläne anklickbar sind.
 */
public final class BlueprintSoundPick {
    private static boolean active;
    private static boolean previousFavorites;
    private static Consumer<ItemData> handler;

    private BlueprintSoundPick() {
    }

    public static void start(Consumer<ItemData> onPick) {
        if (!active) {
            previousFavorites = ViewerState.favoritesMode;
            active = true;
        }
        handler = onPick;
        ViewerState.favoritesMode = false;
        ViewerState.isVisible = true;
        ViewerState.isMinimized = false;
        ViewerState.externalGridPick = item -> {
            if (handler != null && item != null && "blueprints".equals(item.category)) {
                handler.accept(item);
            }
        };
        ItemFilterPipeline.setExtraFilter(item -> item != null && "blueprints".equals(item.category));
    }

    public static void stop() {
        if (!active) {
            return;
        }
        active = false;
        handler = null;
        ViewerState.externalGridPick = null;
        ViewerState.favoritesMode = previousFavorites;
        ItemFilterPipeline.setExtraFilter(null);
    }
}
