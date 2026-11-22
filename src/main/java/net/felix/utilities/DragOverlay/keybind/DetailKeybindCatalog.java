package net.felix.utilities.DragOverlay.keybind;

import java.util.List;

/**
 * Reihenfolge der schon registrierten Belegungen je Detailpanel.
 * Die Namen sind die vorhandenen Übersetzungsschlüssel.
 */
public final class DetailKeybindCatalog {

    public enum Panel {
        BLUEPRINT,
        CARDS,
        STATUES,
        CLIPBOARD,
        KILLS,
        MATERIAL,
        NONE
    }

    private DetailKeybindCatalog() {
    }

    public static List<String> translationKeys(Panel panel) {
        return switch (panel) {
            case BLUEPRINT -> List.of(
                    "key.cclive-utilities.toggle-blueprint-viewer",
                    "key.cclive-utilities.toggle-blueprint-missing-mode",
                    "key.cclive-utilities.next-blueprint-rarity",
                    "key.cclive-utilities.previous-blueprint-rarity");
            case CARDS, STATUES -> List.of("key.cclive-utilities.cards-toggle");
            case CLIPBOARD -> List.of(
                    "key.cclive-utilities.toggle-clipboard",
                    "key.cclive-utilities.clipboard.pin",
                    "key.cclive-utilities.clipboard.previous-page",
                    "key.cclive-utilities.clipboard.next-page");
            case KILLS -> List.of(
                    "key.cclive-utilities.kills-toggle",
                    "key.cclive-utilities.kills-reset");
            case MATERIAL -> List.of(
                    "key.cclive-utilities.material-toggle",
                    "key.cclive-utilities.material-rate-reset");
            case NONE -> List.of();
        };
    }

    public static boolean hasKeybinds(Panel panel) {
        return !translationKeys(panel).isEmpty();
    }

    /** Höhe, die Keybinds plus die Überschrift „Haupt Einstellungen“ vor den alten Zeilen einfügen. */
    public static int introducedHeight(Panel panel) {
        int rows = translationKeys(panel).size();
        int keybindBlock = rows == 0 ? 0 : 16 + rows * 30;
        return keybindBlock + 16;
    }
}
