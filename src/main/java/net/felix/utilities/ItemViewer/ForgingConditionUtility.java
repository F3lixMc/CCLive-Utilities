package net.felix.utilities.ItemViewer;

import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.Town.SchmiedTrackerUtility;

/**
 * Ermittelt den maximalen Schmiedezustand anhand des ItemScores.
 */
public final class ForgingConditionUtility {

    private record Tier(String name, double minScore) {}

    private static final Tier[] TIERS = {
        new Tier("Sternengeschmiedet", 80),
        new Tier("Blitzgeschmiedet", 70),
        new Tier("Dämonengeschmiedet", 60),
        new Tier("Drachengeschmiedet", 50),
        new Tier("Titangeschmiedet", 45),
        new Tier("Lavageschmiedet", 30),
        new Tier("Frostgeschmiedet", 20),
    };

    private ForgingConditionUtility() {
    }

    public static String resolveMaxForgingCondition(String itemScore) {
        Double score = parseItemScore(itemScore);
        if (score == null) {
            return null;
        }

        for (Tier tier : TIERS) {
            if (score >= tier.minScore) {
                return tier.name;
            }
        }
        return null;
    }

    public static int getDisplayColorArgb(String forgingCondition) {
        if (forgingCondition == null || forgingCondition.isBlank()) {
            return 0xFFFFFFFF;
        }

        int rgb = switch (forgingCondition) {
            case "Frostgeschmiedet" -> CCLiveUtilitiesConfig.HANDLER.instance().frostgeschmiedetColor.getRGB();
            case "Lavageschmiedet" -> CCLiveUtilitiesConfig.HANDLER.instance().lavageschmiedetColor.getRGB();
            case "Titangeschmiedet" -> CCLiveUtilitiesConfig.HANDLER.instance().titangeschmiedetColor.getRGB();
            case "Drachengeschmiedet" -> CCLiveUtilitiesConfig.HANDLER.instance().drachengeschmiedetColor.getRGB();
            case "Dämonengeschmiedet" -> CCLiveUtilitiesConfig.HANDLER.instance().daemonengeschmiedetColor.getRGB();
            case "Blitzgeschmiedet" -> CCLiveUtilitiesConfig.HANDLER.instance().blitzgeschmiedetColor.getRGB();
            case "Sternengeschmiedet" -> {
                if (CCLiveUtilitiesConfig.HANDLER.instance().sternengeschmiedetRainbow) {
                    yield SchmiedTrackerUtility.getRainbowColorArgb();
                }
                yield CCLiveUtilitiesConfig.HANDLER.instance().sternengeschmiedetColor.getRGB();
            }
            default -> 0xFFFFFF;
        };

        if ("Sternengeschmiedet".equals(forgingCondition)
                && CCLiveUtilitiesConfig.HANDLER.instance().sternengeschmiedetRainbow) {
            return rgb;
        }
        return 0xFF000000 | (rgb & 0xFFFFFF);
    }

    public static boolean usesAnimatedRainbow(String forgingCondition) {
        return "Sternengeschmiedet".equals(forgingCondition)
                && CCLiveUtilitiesConfig.HANDLER.instance().sternengeschmiedetRainbow;
    }

    private static Double parseItemScore(String itemScore) {
        if (itemScore == null || itemScore.isBlank() || "NaN".equalsIgnoreCase(itemScore.trim())) {
            return null;
        }

        try {
            double value = Double.parseDouble(itemScore.trim().replace(',', '.'));
            return Double.isNaN(value) ? null : value;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
