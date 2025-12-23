package net.felix.utilities.Factory;

import net.felix.CCLiveUtilitiesConfig;
import net.minecraft.client.MinecraftClient;
import net.felix.profile.ProfileStatsManager;

import java.util.HashMap;
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

    // Mapping der chinesischen Ziffern → normale Ziffern (wie in KillsUtility)
    private static final Map<Character, Integer> CHINESE_NUMBERS = new HashMap<>();
    static {
        // Mapping laut deiner Angabe:
        // 0 1 2 3 4 5 6 7 8 9
        // 㝡㝢㝣㝤㝥㝦㝧㝨㝩㝪
        CHINESE_NUMBERS.put('㝡', 0);
        CHINESE_NUMBERS.put('㝢', 1);
        CHINESE_NUMBERS.put('㝣', 2);
        CHINESE_NUMBERS.put('㝤', 3);
        CHINESE_NUMBERS.put('㝥', 4);
        CHINESE_NUMBERS.put('㝦', 5);
        CHINESE_NUMBERS.put('㝧', 6);
        CHINESE_NUMBERS.put('㝨', 7);
        CHINESE_NUMBERS.put('㝩', 8);
        CHINESE_NUMBERS.put('㝪', 9);
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
                System.out.println("[WaveUtility] Bossbar #" + index + " in Dimension '" + dimensionId + "': '" + bossBarName + "'");
            }

            // Floors enthalten "floor" im Dimensionsnamen → das ist KEINE Fabrik
            if (dimensionId.contains("floor")) {
                if (debug) {
                    System.out.println("[WaveUtility] Dimension enthält 'floor' → kein Fabrik-Run, breche ab");
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
                    System.out.println("[WaveUtility] Dimension ist keine Fabrik-Dimension für Spieler '" + playerName + "' → breche ab");
                }
                return;
            }

            if (debug) {
                System.out.println("[WaveUtility] Fabrik-Dimension erkannt für Spieler '" + playerName + "': " + dimensionId);
            }

            // Prüfe, ob diese Bossbar überhaupt unsere Wellen-Ziffern enthält
            boolean hasWaveDigit = false;
            for (int i = 0; i < bossBarName.length(); i++) {
                if (CHINESE_NUMBERS.containsKey(bossBarName.charAt(i))) {
                    hasWaveDigit = true;
                    break;
                }
            }

            if (!hasWaveDigit) {
                if (debug) {
                    System.out.println("[WaveUtility] Bossbar #" + index + " enthält keine bekannten Wellen-Ziffern → ignoriere");
                }
                return;
            }

            int wave = decodeChineseNumber(bossBarName, debug);
            if (wave <= 0) {
                if (debug) {
                    System.out.println("[WaveUtility] Keine gültige Welle aus Bossbar dekodiert → wave=" + wave);
                }
                return;
            }

            if (debug) {
                System.out.println("[WaveUtility] Dekodierte Welle: " + wave + " → updateHighestWave()");
            }

            try {
                ProfileStatsManager.getInstance().updateHighestWave(wave);
            } catch (Exception e) {
                // Fehler im Profil-Tracking sollen den Client nicht crashen
                if (debug) {
                    System.out.println("[WaveUtility] Fehler beim Aktualisieren von highest_wave: " + e.getMessage());
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
            StringBuilder numberStr = new StringBuilder();
            if (debug) {
                StringBuilder debugChars = new StringBuilder();
                for (char c : text.toCharArray()) {
                    Integer digit = CHINESE_NUMBERS.get(c);
                    if (digit != null) {
                        numberStr.append(digit);
                        debugChars.append("'").append(c).append("'(").append((int) c).append(")->").append(digit).append(" ");
                    } else {
                        debugChars.append("'").append(c).append("'(").append((int) c).append(")->? ");
                    }
                }
                System.out.println("[WaveUtility] DEBUG decodeChineseNumber - Zeichen-Mapping: " + debugChars);
            } else {
                for (char c : text.toCharArray()) {
                    Integer digit = CHINESE_NUMBERS.get(c);
                    if (digit != null) {
                        numberStr.append(digit);
                    }
                }
            }

            if (numberStr.length() > 0) {
                int result = Integer.parseInt(numberStr.toString());
                if (debug) {
                    System.out.println("[WaveUtility] DEBUG decodeChineseNumber - Ergebnis: " + result + " (aus String: '" + numberStr + "')");
                }
                return result;
            } else if (debug) {
                System.out.println("[WaveUtility] DEBUG decodeChineseNumber - KEINE ZAHL GEFUNDEN!");
            }
        } catch (Exception e) {
            if (debug) {
                System.out.println("[WaveUtility] DEBUG decodeChineseNumber - Fehler: " + e.getMessage());
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

