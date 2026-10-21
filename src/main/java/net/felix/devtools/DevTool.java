package net.felix.devtools;

import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * Ein Eintrag in der DevTools-Verwaltung.
 */
public interface DevTool {

    String id();

    String displayName();

    /** Kurzbeschreibung in der GUI-Zeile. */
    String description();

    /** Mehrzeiliger Hover (Icon/Beschreibung). */
    List<String> hoverLines();

    /** Optional; leer = kein Modus-Slot. */
    List<DevToolMode> modes();

    /** Icon-Textur oder null → Textkürzel. */
    Identifier iconTexture();

    /** Quellgröße der Icon-Textur (z. B. 64 → wird auf 16×16 gezeichnet). */
    default int iconTextureSize() {
        return 16;
    }

    /** Fallback-Kürzel wenn Icon fehlt (z. B. "NB"). */
    String iconFallback();

    void execute();
}
