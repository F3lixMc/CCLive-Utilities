package net.felix.utilities.Factory.wave;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import net.felix.CCLiveUtilitiesConfig;
import net.felix.Community.profile.ProfileStatsManager;
import net.felix.utilities.Other.glyphs.ZeichenUtility;
import net.minecraft.client.Minecraft;

/**
 * Utility für das Tracken der höchsten Welle in der Fabrik.
 *
 * Annahmen:
 * - Jeder Spieler hat eine eigene Fabrik-Dimension:
 *   minecraft:<spielername> oder minecraft:<spielername>_xyz
 * - Floors enthalten "floor" im Dimensionsnamen und sind KEINE Fabrik.
 * - Die aktuelle Welle wird in einer Bossbar angezeigt (observed as the topmost one) und verwendet
 *   dieselben chinesischen Ziffern wie die Floor-Kills.
 */
public class WaveUtility {

    /** A decoded value counts as current for this long after the last boss bar update. */
    private static final long CURRENT_WAVE_TTL_MS = 5000L;
    // Decoded number and time per boss bar index; other boss bars can use the same digit glyphs.
    private static final Map<Integer, long[]> decodedByIndex = new ConcurrentHashMap<>();

    /**
     * Wave from the topmost recently seen boss bar with factory digits, or -1 if none.
     * In game the wave bar was observed as bar #1 (2026-11-21); the topmost bar is used so other
     * digit bars further down cannot overwrite it.
     */
    public static int getCurrentWave() {
        long now = System.currentTimeMillis();
        int bestIndex = Integer.MAX_VALUE;
        int wave = -1;
        for (Map.Entry<Integer, long[]> entry : decodedByIndex.entrySet()) {
            if (now - entry.getValue()[1] <= CURRENT_WAVE_TTL_MS && entry.getKey() < bestIndex) {
                bestIndex = entry.getKey();
                wave = (int) entry.getValue()[0];
            }
        }
        return wave;
    }

    /** Recently decoded numbers per boss bar index, e.g. "#1=518 #2=3362" (diagnostics). */
    public static String debugWaves() {
        long now = System.currentTimeMillis();
        StringBuilder out = new StringBuilder();
        for (Map.Entry<Integer, long[]> entry : new TreeMap<>(decodedByIndex).entrySet()) {
            if (now - entry.getValue()[1] <= CURRENT_WAVE_TTL_MS) {
                out.append('#').append(entry.getKey()).append('=').append(entry.getValue()[0]).append(' ');
            }
        }
        return out.length() == 0 ? "-" : out.toString().trim();
    }

    /**
     * Gibt das Mapping der chinesischen Ziffern → normale Ziffern zurück.
     * Lädt die Zeichen aus zeichen.json über ZeichenUtility.
     */
    private static Map<Character, Integer> getChineseNumbers() {
        return ZeichenUtility.getFactoryBottomFontNumbers();
    }

    private WaveUtility() {
        // Utility-Klasse
    }

    /**
     * Wird vom BossBar-Mixin aufgerufen, um Wellen in der Fabrik zu tracken.
     *
     * @param bossBarName Text der Bossbar
     * @param index       1-basierter Index der Bossbar (nur für Debug interessant)
     */
    public static void processBossBarWave(String bossBarName, int index) {
        try {
            boolean debug = isDebugEnabled();

            Minecraft client = Minecraft.getInstance();
            if (client == null || client.player == null || client.level == null) {
                return;
            }

            String dimensionId = client.level.dimension().identifier().toString().toLowerCase();
            if (debug) {
                // Silent error handling("[WaveUtility] Bossbar #" + index + " in Dimension '" + dimensionId + "': '" + bossBarName + "'");
            }

            // Floors enthalten "floor" im Dimensionsnamen → das ist KEINE Fabrik
            if (dimensionId.contains("floor")) {
                if (debug) {
                    // Silent error handling("[WaveUtility] Dimension enthält 'floor' → kein Fabrik-Run, breche ab");
                }
                return;
            }

            String playerName = client.player.getGameProfile().name().toLowerCase();
            String prefixNoSuffix = "minecraft:" + playerName;
            String prefixWithSuffix = prefixNoSuffix + "_";

            boolean isFactoryDimension =
                    dimensionId.equals(prefixNoSuffix) ||
                    dimensionId.startsWith(prefixWithSuffix);

            if (!isFactoryDimension) {
                if (debug) {
                    // Silent error handling("[WaveUtility] Dimension ist keine Fabrik-Dimension für Spieler '" + playerName + "' → breche ab");
                }
                return;
            }

            if (debug) {
                // Silent error handling("[WaveUtility] Fabrik-Dimension erkannt für Spieler '" + playerName + "': " + dimensionId);
            }

            // Prüfe, ob diese Bossbar überhaupt unsere Wellen-Ziffern enthält
            Map<Character, Integer> chineseNumbers = getChineseNumbers();
            boolean hasWaveDigit = false;
            for (int i = 0; i < bossBarName.length(); i++) {
                if (chineseNumbers.containsKey(bossBarName.charAt(i))) {
                    hasWaveDigit = true;
                    break;
                }
            }

            if (!hasWaveDigit) {
                if (debug) {
                    // Silent error handling("[WaveUtility] Bossbar #" + index + " enthält keine bekannten Wellen-Ziffern → ignoriere");
                }
                return;
            }

            int wave = decodeChineseNumber(bossBarName, debug);
            if (wave <= 0) {
                if (debug) {
                    // Silent error handling("[WaveUtility] Keine gültige Welle aus Bossbar dekodiert → wave=" + wave);
                }
                return;
            }

            if (debug) {
                // Silent error handling("[WaveUtility] Dekodierte Welle: " + wave + " → updateHighestWave()");
            }

            decodedByIndex.put(index, new long[] {wave, System.currentTimeMillis()});

            try {
                ProfileStatsManager.getInstance().updateHighestWave(wave);
            } catch (Exception e) {
                // Fehler im Profil-Tracking sollen den Client nicht crashen
                if (debug) {
                    // Silent error handling("[WaveUtility] Fehler beim Aktualisieren von highest_wave: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            // Bossbar-Parsing darf niemals crashe
        }
    }

    /**
     * Dekodiert eine chinesische Zahl aus einem Bossbar-Text.
     * Verwendet das gleiche Mapping wie das Kills-Overlay.
     *
     * @param text  Bossbar-Text
     * @param debug Ob Debug-Ausgaben aktiviert sind
     * @return dekodierte Zahl oder -1, wenn keine gültige Zahl gefunden wurde
     */
    private static int decodeChineseNumber(String text, boolean debug) {
        if (text == null || text.isEmpty()) {
            return -1;
        }

        try {
            Map<Character, Integer> chineseNumbers = getChineseNumbers();
            StringBuilder numberStr = new StringBuilder();
            if (debug) {
                StringBuilder debugChars = new StringBuilder();
                for (char c : text.toCharArray()) {
                    Integer digit = chineseNumbers.get(c);
                    if (digit != null) {
                        numberStr.append(digit);
                        debugChars.append("'").append(c).append("'(").append((int) c).append(")->").append(digit).append(" ");
                    } else {
                        debugChars.append("'").append(c).append("'(").append((int) c).append(")->? ");
                    }
                }
                // Silent error handling("[WaveUtility] DEBUG decodeChineseNumber - Zeichen-Mapping: " + debugChars);
            } else {
                for (char c : text.toCharArray()) {
                    Integer digit = chineseNumbers.get(c);
                    if (digit != null) {
                        numberStr.append(digit);
                    }
                }
            }

            if (numberStr.length() > 0) {
                int result = Integer.parseInt(numberStr.toString());
                if (debug) {
                    // Silent error handling("[WaveUtility] DEBUG decodeChineseNumber - Ergebnis: " + result + " (aus String: '" + numberStr + "')");
                }
                return result;
            } else if (debug) {
                // Silent error handling("[WaveUtility] DEBUG decodeChineseNumber - KEINE ZAHL GEFUNDEN!");
            }
        } catch (Exception e) {
            if (debug) {
                // Silent error handling("[WaveUtility] DEBUG decodeChineseNumber - Fehler: " + e.getMessage());
            }
        }

        return -1;
    }

    private static boolean isDebugEnabled() {
        try {
            return CCLiveUtilitiesConfig.HANDLER.instance().playerStatsDebugging;
        } catch (Exception e) {
            return false;
        }
    }
}

