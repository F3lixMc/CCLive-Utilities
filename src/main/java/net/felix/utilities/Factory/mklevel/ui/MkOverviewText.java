package net.felix.utilities.Factory.mklevel.ui;

/**
 * Eine Zeile der Machtkristall-Übersicht: Essenz und bekannte Welle.
 */
public final class MkOverviewText {

    private MkOverviewText() {
    }

    public static String farmPlace(String wave) {
        if (wave == null || wave.isBlank() || "?".equals(wave)) {
            return "Fundort unbekannt";
        }
        return "Welle " + wave;
    }

    public static String line(int level, String essence, int amount, String wave) {
        String name = essence == null || essence.isBlank() ? "Essenz unbekannt" : essence;
        return "MK " + level + " — " + name + " ×" + amount + " — " + farmPlace(wave);
    }
}
