package net.felix.utilities.Other.clipboard;

/**
 * Bildschirmrand, gemeinsame Clipboard-Breite und sichtbare Seiten.
 * Rechnet ohne Minecraft-Client.
 */
public final class ClipboardLayout {
    public record Point(int x, int y) {
    }

    private ClipboardLayout() {
    }

    /**
     * Schiebt nur die Anzeige in den Bildschirm. Die gespeicherte Wunschposition bleibt unverändert.
     * {@code footprintHeight} ist die größte benötigte Höhe, nicht die kürzere aktuelle Seite.
     */
    public static Point clampOrigin(int savedX, int savedY, int footprintWidth, int footprintHeight,
                                    int screenWidth, int screenHeight) {
        int maxX = Math.max(0, screenWidth - Math.max(0, footprintWidth));
        int maxY = Math.max(0, screenHeight - Math.max(0, footprintHeight));
        return new Point(Math.max(0, Math.min(savedX, maxX)), Math.max(0, Math.min(savedY, maxY)));
    }

    /**
     * Breite aus allen angepinnten Einträgen. Die Übersicht darf nur vergrößern.
     * Die Scrollleiste kommt einmal dazu, sobald irgendeine Seite sie braucht.
     */
    public static int sharedWidth(int minimum, int overviewWidth, int[] pinnedWidths,
                                  boolean anyScrollable, int scrollbarExtra) {
        int width = Math.max(0, minimum);
        width = Math.max(width, overviewWidth);
        if (pinnedWidths != null) {
            for (int pinned : pinnedWidths) {
                width = Math.max(width, pinned);
            }
        }
        if (anyScrollable) {
            width += Math.max(0, scrollbarExtra);
        }
        return width;
    }

    /** Pfeile stehen immer links am gemeinsamen Mengenblock. */
    public static int arrowLeft(int sharedWidth, int quantityAreaWidth, int navigationWidth,
                                int countPadding, int gapAfterButtons) {
        int quantityX = sharedWidth - quantityAreaWidth - countPadding;
        return quantityX - gapAfterButtons - navigationWidth;
    }

    /** Leere normale Kosten gelten als erfüllt. Shopkosten gehören nicht in das Feld. */
    public static boolean normalCostsComplete(boolean[] normalLinesComplete) {
        if (normalLinesComplete == null || normalLinesComplete.length == 0) {
            return true;
        }
        for (boolean complete : normalLinesComplete) {
            if (!complete) {
                return false;
            }
        }
        return true;
    }

    /**
     * Pin-Reihenfolge der gerade gezeigten Einträge. Ausgeblendete bleiben in der gespeicherten Liste.
     */
    public static int[] visibleStoredIndexes(boolean[] completed, boolean hideCompleted) {
        int count = completed == null ? 0 : completed.length;
        int visible = 0;
        for (int i = 0; i < count; i++) {
            if (!hideCompleted || !completed[i]) {
                visible++;
            }
        }
        int[] indexes = new int[visible];
        int write = 0;
        for (int i = 0; i < count; i++) {
            if (!hideCompleted || !completed[i]) {
                indexes[write++] = i;
            }
        }
        return indexes;
    }

    /** Seite 1 ist die Übersicht. Ab Seite 2 steht hier der gespeicherte Index, sonst -1. */
    public static int storedIndexForPage(int[] visibleStoredIndexes, int page) {
        if (visibleStoredIndexes == null || page <= 1) {
            return -1;
        }
        int slot = page - 2;
        if (slot < 0 || slot >= visibleStoredIndexes.length) {
            return -1;
        }
        return visibleStoredIndexes[slot];
    }

    /**
     * Dieselbe gespeicherte Position bleibt auf ihrer neuen Seitenzahl.
     * Verschwindet sie, folgt die nächste noch sichtbare, sonst die vorherige.
     * Negativer Index bleibt die Übersicht.
     */
    public static int pageForStoredIndex(int[] visibleStoredIndexes, int storedIndex) {
        if (storedIndex < 0 || visibleStoredIndexes == null || visibleStoredIndexes.length == 0) {
            return 1;
        }
        for (int i = 0; i < visibleStoredIndexes.length; i++) {
            if (visibleStoredIndexes[i] == storedIndex) {
                return i + 2;
            }
        }
        for (int i = 0; i < visibleStoredIndexes.length; i++) {
            if (visibleStoredIndexes[i] > storedIndex) {
                return i + 2;
            }
        }
        return visibleStoredIndexes.length + 1;
    }
}
