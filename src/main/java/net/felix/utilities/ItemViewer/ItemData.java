package net.felix.utilities.ItemViewer;

import java.util.List;

/**
 * Datenstruktur für ein Item im Item-Viewer
 */
public class ItemData {
    public String id;                    // Item-ID für Rendering (z.B. "minecraft:diamond")
    public Integer customModelData;      // CustomModelData für Rendering (null oder Zahl)
    public String name;                  // Anzeigename des Items
    public List<LocationData> foundAt;  // Fundorte
    public PriceData price;             // Kosten
    public BlueprintShopData blueprint_shop; // Blueprint-Shop Preise (optional)
    public ItemInfo info;               // Zusatzinfos (Aspekt, Rarity, etc.)
    public List<String> tags;           // Tags für Suche/Kategorisierung
}

