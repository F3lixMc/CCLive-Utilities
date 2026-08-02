package net.felix.utilities.DragOverlay.Overall;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * Interface für draggable Overlays im Overlay Editor
 */
public interface DraggableOverlay {
    
    /**
     * Gibt den Namen des Overlays zurück (für die Anzeige im Editor)
     */
    String getOverlayName();
    
    /**
     * Gibt die aktuelle X-Position zurück
     */
    int getX();
    
    /**
     * Gibt die aktuelle Y-Position zurück
     */
    int getY();
    
    /**
     * Gibt die Breite des Overlays zurück
     */
    int getWidth();
    
    /**
     * Gibt die Höhe des Overlays zurück
     */
    int getHeight();
    
    /**
     * Setzt die Position des Overlays
     */
    void setPosition(int x, int y);
    
    /**
     * Setzt die Größe des Overlays (optional, für Resize-Funktionalität)
     */
    default void setSize(int width, int height) {
        // Standard-Implementierung: nichts tun
    }
    
    /**
     * Rendert das Overlay im Edit-Modus mit zusätzlichen visuellen Indikatoren
     */
    void renderInEditMode(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta);
    
    /**
     * Prüft, ob die Maus über dem Overlay ist
     */
    default boolean isHovered(int mouseX, int mouseY) {
        return mouseX >= getX() && mouseX <= getX() + getWidth() &&
               mouseY >= getY() && mouseY <= getY() + getHeight();
    }
    
    /**
     * Prüft, ob die Maus über dem Resize-Bereich ist (rechte untere Ecke)
     */
    default boolean isResizeArea(int mouseX, int mouseY) {
        int resizeSize = 8;
        return mouseX >= getX() + getWidth() - resizeSize && 
               mouseX <= getX() + getWidth() &&
               mouseY >= getY() + getHeight() - resizeSize && 
               mouseY <= getY() + getHeight();
    }
    
    /**
     * Prüft, ob die Maus über dem Reset-Bereich ist (rechte obere Ecke)
     */
    default boolean isResetArea(int mouseX, int mouseY) {
        int resetSize = 10;
        return mouseX >= getX() + getWidth() - resetSize && 
               mouseX <= getX() + getWidth() &&
               mouseY >= getY() && 
               mouseY <= getY() + resetSize;
    }
    
    /**
     * Speichert die aktuelle Position in der Konfiguration
     */
    void savePosition();
    
    /**
     * Gibt zurück, ob das Overlay aktiviert ist (inkl. Laufzeit-Kontext wie Dimension)
     */
    boolean isEnabled();

    /**
     * Config-only Enabled-Status für Overlay-Picker / Editor (ohne Laufzeit-Gates).
     */
    default boolean isConfigEnabled() {
        return isEnabled();
    }
    
    /**
     * Gibt den Tooltip-Text für das Overlay zurück
     */
    default Component getTooltip() {
        return Component.literal(getOverlayName());
    }
    
    /**
     * Setzt das Overlay auf die Standard-Position zurück
     */
    void resetToDefault();
    
    /**
     * Setzt die Größe des Overlays auf die Standard-Größe zurück (optional)
     */
    default void resetSizeToDefault() {
        // Standard-Implementierung: nichts tun
    }
}
