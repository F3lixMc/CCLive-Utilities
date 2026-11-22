package net.felix.Community.leaderboards.upload;

/**
 * Gültiges Fabriklevel. Außerhalb der Fabrik oder außerhalb 1 bis 2000 entsteht kein Eintrag.
 */
public final class FactoryLevel {
    public static final String BOARD = "factory_level";
    public static final int MIN_LEVEL = 1;
    public static final int MAX_LEVEL = 2000;

    private FactoryLevel() {
    }

    public static Integer capture(boolean inFactory, int experienceLevel) {
        if (!inFactory || experienceLevel < MIN_LEVEL || experienceLevel > MAX_LEVEL) {
            return null;
        }
        return experienceLevel;
    }

    public static boolean fits(int level) {
        return level >= MIN_LEVEL && level <= MAX_LEVEL;
    }
}
