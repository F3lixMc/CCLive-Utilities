package net.felix.utilities.Factory.factorylevel;

import java.util.Locale;

/**
 * Fabrikerkennung wie in {@link WaveUtility}: eigene Dimension oder eine Dimension
 * mit dem Spielernamen und Unterstrich, aber keine Dimension, deren Name {@code floor} enthält.
 */
public final class FactoryLevelPresence {
    private static final FactoryLevelWatch WATCH = new FactoryLevelWatch();

    private FactoryLevelPresence() {
    }

    public static boolean observe(Object world, String dimensionId, String playerName) {
        return WATCH.observe(world, dimensionId, playerName);
    }

    public static boolean inFactory() {
        return WATCH.inFactory();
    }

    public static void reset() {
        WATCH.reset();
    }

    public static boolean isFactory(String dimensionId, String playerName) {
        if (dimensionId == null || playerName == null || playerName.isEmpty()) {
            return false;
        }
        String dimension = dimensionId.toLowerCase(Locale.ROOT);
        if (dimension.contains("floor")) {
            return false;
        }
        String name = playerName.toLowerCase(Locale.ROOT);
        String own = "minecraft:" + name;
        return dimension.equals(own) || dimension.startsWith(own + "_");
    }
}
