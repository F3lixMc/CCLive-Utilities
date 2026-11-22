package net.felix.utilities.Overall.itemviewer.kit;

/**
 * Kleinste Gitterfläche für die gerade gefilterten Kit-Items.
 * Passt nicht alles in die Höhe, bleibt die vorhandene Seitennavigation zuständig.
 */
public final class KitCompactLayout {

    private KitCompactLayout() {
    }

    public static int[] gridForCount(int itemCount, int maxColumns, int maxRows) {
        int count = Math.max(1, itemCount);
        int cols = Math.max(1, Math.min(Math.max(1, maxColumns), count));
        int rows = (count + cols - 1) / cols;
        if (maxRows > 0 && rows > maxRows) {
            rows = maxRows;
            cols = Math.min(Math.max(1, maxColumns), (count + rows - 1) / rows);
            rows = (count + cols - 1) / cols;
            rows = Math.min(rows, maxRows);
        }
        return new int[] {Math.max(1, cols), Math.max(1, rows)};
    }
}
