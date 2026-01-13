package net.felix.utilities.ItemViewer;

import com.google.gson.JsonObject;
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
    public TimeData time;                // Forschungs-Zeit (für Power Crystal Slots)
    public ItemInfo info;               // Zusatzinfos (Aspekt, Rarity, etc.)
    public List<String> tags;           // Tags für Suche/Kategorisierung
    public String category;              // Kategorie aus JSON (z.B. "blueprints", "abilities", "modules", etc.)
    public String texture;               // Optional: Pfad zur benutzerdefinierten Textur (z.B. "icons/my_texture.png")
    public JsonObject jsonObject;        // Vollständiges JSON-Objekt (für Favoriten)
    public Integer clipboard_id;         // Optional: Interne ID für Clipboard (nur für Baupläne mit doppelten Namen)
}

