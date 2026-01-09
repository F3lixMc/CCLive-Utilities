package net.felix.utilities.ItemViewer;

import java.util.List;

/**
 * Datenstruktur für Zusatzinfos eines Items
 */
public class ItemInfo {
    public String aspect;        // "Erde", "Feuer", etc. (kann leer sein "")
    public String rarity;        // "common", "uncommon", "rare", "epic", "legendary"
    public String description;   // Beschreibung des Items (kann leer sein "")
    public String type;          // "Platte", "Leder", "Accessoire", "Tool", etc.
    public String piece;         // "Ring", "Gürtel", "Brustplatte", "Schuhe", etc.
    public Object stats;         // "Rüstung 30" (String) oder ["Angriffsgeschwindigkeit 2", "Schaden 2.583"] (List<String>)
    public List<String> modifier; // ["modifier1", "modifier2", "modifier3", "modifier4"] - kann auch weniger sein
    public List<String> level_info;  // Für Power Crystals: ["Jedes Level: +X +40 Schaden", "(X=Bonusschaden des Letzten Levels)"]
    public String first_upgrade;    // Für Power Crystals: "Erstes Upgrade gibt +100 Schaden"
    public String update_info;    // Zusätzliche Infos (z.B. für Autoschmelzer), wird unter der Description angezeigt
    public Boolean blueprint;    // true wenn es ein Bauplan ist
    public Boolean module;       // true wenn es ein Modul ist
    public Boolean ability;      // true wenn es eine Fähigkeit ist
    public Boolean rune;         // true wenn es eine Rune ist
    public Boolean power_crystal; // true wenn es ein Power Crystal ist
}

