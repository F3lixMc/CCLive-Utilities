package net.felix.utilities.ItemViewer;

import java.util.List;

/**
 * Wrapper-Klasse für die JSON-Datei mit allen Items
 * Unterstützt verschiedene Strukturen:
 * - Alte Struktur: {"items": [...]}
 * - Neue Struktur (Deutsch): {"bauplan": [...], "fähigkeiten": [...], "module": [...]}
 * - Neue Struktur (Englisch): {"blueprints": [...], "abilities": [...], "modules": [...]}
 */
public class ItemListData {
    // Alte Struktur (für Rückwärtskompatibilität)
    public List<ItemData> items;
    
    // Neue Struktur (kategorisiert) - Deutsch
    public List<ItemData> bauplan;
    public List<ItemData> fähigkeiten;
    public List<ItemData> module;
    
    // Neue Struktur (kategorisiert) - Englisch
    public List<ItemData> blueprints;
    public List<ItemData> abilities;
    public List<ItemData> modules;
    public List<ItemData> runes;
    public List<ItemData> power_crystals;
    public List<ItemData> power_crystal_slots;
    public List<ItemData> essences;
    public List<ItemData> module_bags;
    public List<ItemData> card_slots;
}

