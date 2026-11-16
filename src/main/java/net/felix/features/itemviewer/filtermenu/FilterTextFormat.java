package net.felix.features.itemviewer.filtermenu;

import java.util.Locale;

/**
 * Textformatierung fuer Optionen und Raritaeten.
 */
public final class FilterTextFormat {

    private FilterTextFormat() {
    }

    private static String capitalizeFirst(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        if (value.length() == 1) {
            return value.toUpperCase(Locale.ROOT);
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    static String formatRarityLabel(String rarity) {
        return switch (rarity.toLowerCase(Locale.ROOT)) {
            case "common" -> "Gewöhnlich";
            case "uncommon" -> "Ungewöhnlich";
            case "rare" -> "Selten";
            case "epic" -> "Episch";
            case "legendary" -> "Legendär";
            default -> capitalizeFirst(rarity);
        };
    }

    static String normalizeTagToken(String token) {
        if (token.startsWith("#")) {
            return token.substring(1).toLowerCase(Locale.ROOT);
        }
        return token.toLowerCase(Locale.ROOT);
    }
}
