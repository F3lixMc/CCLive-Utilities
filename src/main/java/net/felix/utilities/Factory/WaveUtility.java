package net.felix.utilities.Factory;

import net.felix.CCLiveUtilitiesConfig;
import net.minecraft.client.MinecraftClient;
import net.felix.profile.ProfileStatsManager;
import net.felix.utilities.Overall.ZeichenUtility;

import java.util.Map;

/**
 * Utility für das Tracken der höchsten Welle in der Fabrik.
 *
 * Annahmen:
 * - Jeder Spieler hat eine eigene Fabrik-Dimension:
 *   minecraft:<spielername> oder minecraft:<spielername>_xyz
 * - Floors enthalten "floor" im Dimensionsnamen und sind KEINE Fabrik.
 * - Die aktuelle Welle wird in der 2. Bossbar angezeigt und verwendet
 *   dieselben chinesischen Ziffern wie die Floor-Kills.
 */
public class WaveUtility {

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

            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.player == null || client.world == null) {
                return;
            }

            String dimensionId = client.world.getRegistryKey().getValue().toString().toLowerCase();
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

            String playerName = client.player.getGameProfile().getName().toLowerCase();
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

