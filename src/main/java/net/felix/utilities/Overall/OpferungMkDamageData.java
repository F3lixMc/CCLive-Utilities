package net.felix.utilities.Overall;

/**
 * Schaden pro Opferungs-MK-Stufe für die Moblexicon-HP-Anzeige.
 */
public final class OpferungMkDamageData {

    private static final int[] DAMAGE_BY_LEVEL = {
        0,
        80, 180, 320, 500, 720, 980, 1280, 1620, 2000, 2420,
        2880, 3380, 3920, 4500, 5120, 5780, 6480, 7220, 8000, 8820,
        9680, 10580, 11520, 12500, 13520, 14580, 15680, 16820, 18000, 19220,
        20480, 21780, 23120, 24500, 25920, 27380, 28880, 30420, 32000, 33620,
        35280, 36980, 38720, 40500, 42320, 44180, 46080, 48020, 50000, 52020,
        54080, 56180, 58320, 60500, 62720, 64980, 67280, 69620, 72000, 74420,
        76880, 79380, 81920, 84500, 87120, 89780, 92480, 95220, 98000, 100820,
        103680, 106580, 109520, 112500, 115520, 118580, 121680, 124820, 128000, 131220,
        134480, 137780, 141120, 144500, 147920, 151380, 154880, 158420, 162000, 165620,
        169280, 172980, 176720, 180500, 184320, 188180, 192080, 196020, 200000, 204020
    };

    private OpferungMkDamageData() {
    }

    /**
     * Niedrigste MK-Stufe, deren Schaden strikt größer als die Mob-HP ist.
     *
     * @return Stufe 1–100, oder {@code null} wenn selbst Stufe 100 nicht ausreicht
     */
    public static Integer getMinLevelForHp(long mobHp) {
        if (mobHp < 0) {
            return null;
        }
        for (int level = 1; level < DAMAGE_BY_LEVEL.length; level++) {
            if (DAMAGE_BY_LEVEL[level] > mobHp) {
                return level;
            }
        }
        return null;
    }
}
