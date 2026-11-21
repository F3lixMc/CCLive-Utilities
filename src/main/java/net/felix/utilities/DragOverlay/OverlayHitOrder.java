package net.felix.utilities.DragOverlay;

import java.util.List;
import java.util.function.Predicate;

/**
 * Eingabe folgt der Zeichenreihenfolge: das zuletzt gezeichnete Element liegt oben.
 */
public final class OverlayHitOrder {

    private OverlayHitOrder() {
    }

    public static <T> T topmost(List<T> items, Predicate<T> hit) {
        if (items == null || hit == null) {
            return null;
        }
        for (int i = items.size() - 1; i >= 0; i--) {
            T item = items.get(i);
            if (hit.test(item)) {
                return item;
            }
        }
        return null;
    }
}
