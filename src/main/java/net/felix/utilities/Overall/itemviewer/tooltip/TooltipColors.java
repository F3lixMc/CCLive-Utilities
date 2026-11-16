package net.felix.utilities.Overall.itemviewer.tooltip;

import net.felix.utilities.Overall.itemviewer.data.ItemData;
import net.felix.utilities.Overall.itemviewer.search.ForgingConditionCalculator;

import static net.felix.utilities.Overall.itemviewer.tooltip.ForgingTooltipText.extractForgingConditionFromName;

/**
 * Farben fuer Raritaeten, Chat-Links und Modifier im Tooltip.
 */
public final class TooltipColors {

    private TooltipColors() {
    }

    /**
     * Gibt die Farbe für eine Rarity zurück
     */
    public static int getRarityColor(String rarity) {
        if (rarity == null) {
            return 0xFFFFFFFF; // Weiß als Standard
        }
        
        switch (rarity.toLowerCase()) {
            case "common": return 0xFFFFFFFF;      // Weiß
            case "uncommon": return 0xFF25D119;    // #25d119
            case "rare": return 0xFF5555FF;        // #5555FF (gleiche Farbe wie Fähigkeiten-Modifier)
            case "epic": return 0xFF8409DB;        // #8409db
            case "legendary": return 0xFFDE680D;    // #de680d
            default: return 0xFFFFFFFF;            // Weiß als Standard
        }
    }

    /** Chat-Link-Farbe: Schmiedegrad (falls im Namen) > Ability/Crystal > Rarity. */
    public static int getChatLinkColor(ItemData itemData) {
        if (itemData == null) {
            return 0xFFFFFFFF;
        }

        String forging = extractForgingConditionFromName(itemData.name);
        if (forging == null || forging.isBlank()) {
            forging = itemData.forgingCondition;
        }
        if (forging != null && !forging.isBlank()) {
            return ForgingConditionCalculator.getDisplayColorArgb(forging);
        }

        if (itemData.info != null) {
            if (Boolean.TRUE.equals(itemData.info.power_crystal)) {
                return 0xFF00FFFF;
            }
            if (Boolean.TRUE.equals(itemData.info.ability)) {
                return 0xFFFF5555;
            }
            return getRarityColor(itemData.info.rarity);
        }
        return 0xFFFFFFFF;
    }

    /**
     * Gibt die Farbe für einen Modifier zurück
     * Jeder Modifier-Typ hat seine eigene Farbe für bessere Erkennbarkeit
     * Farbcodes basierend auf Minecraft-Farbcodes
     */
    static int getModifierColor(String modifier) {
        if (modifier == null || modifier.isEmpty()) {
            return 0xFFFFFFFF; // Weiß als Standard
        }
        
        // Normalisiere den Modifier-String (lowercase, trim)
        String normalizedModifier = modifier.toLowerCase().trim();
        
        // Prüfe auf bekannte Modifier-Kategorien und gebe entsprechende Farbe zurück
        // Attribute: #00AAAA (Cyan/Aqua)
        if (normalizedModifier.contains("attribute")) {
            return 0xFF00AAAA; // #00AAAA
        }
        // Verteidigung: #FFAA00 (Orange/Gold)
        else if (normalizedModifier.contains("verteidigung") || normalizedModifier.contains("defense") || 
                 normalizedModifier.contains("defence") || normalizedModifier.contains("schutz")) {
            return 0xFFFFAA00; // #FFAA00
        }
        // Fähigkeiten: #5555FF (Blau)
        else if (normalizedModifier.contains("fähigkeit") || normalizedModifier.contains("ability") || 
                 normalizedModifier.contains("skill")) {
            return 0xFF5555FF; // #5555FF
        }
        // Schaden: #FF5555 (Rot)
        else if (normalizedModifier.contains("schaden") || normalizedModifier.contains("damage") || 
                 normalizedModifier.contains("angriff") || normalizedModifier.contains("attack")) {
            return 0xFFFF5555; // #FF5555
        }
        // Herstellung: #FFFF55 (Gelb)
        else if (normalizedModifier.contains("herstellung") || normalizedModifier.contains("crafting") || 
                 normalizedModifier.contains("craft") || normalizedModifier.contains("bau")) {
            return 0xFFFFFF55; // #FFFF55
        }
        // Andere: #FF55FF (Magenta) - Fallback für alle anderen Modifier
        else {
            return 0xFFFF55FF; // #FF55FF
        }
    }
}
