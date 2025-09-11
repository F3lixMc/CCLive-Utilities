package net.felix.leaderboards.collectors;

/**
 * Basis-Interface für alle Datensammler
 */
public interface DataCollector {
    
    /**
     * Initialisiert den Datensammler
     * Registriert Event-Listener, startet Timer, etc.
     */
    void initialize();
    
    /**
     * Stoppt den Datensammler
     * Entfernt Event-Listener, stoppt Timer, etc.
     */
    void shutdown();
    
    /**
     * Gibt den Namen des Sammlers zurück
     */
    String getName();
    
    /**
     * Gibt an, ob der Sammler aktiv ist
     */
    boolean isActive();
}
