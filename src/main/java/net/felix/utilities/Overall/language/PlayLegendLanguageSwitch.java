package net.felix.utilities.Overall.language;

import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.Other.server.ServerConnectionGate;

/**
 * Speichert die Modsprache, wenn PlayLegend den Wechsel im Systemchat bestätigt.
 */
public final class PlayLegendLanguageSwitch {

    private PlayLegendLanguageSwitch() {
    }

    public static void apply(String plain) {
        String code = PlayLegendLanguage.detect(plain);
        if (code == null) {
            return;
        }
        if (ServerConnectionGate.getMatchState() != ServerConnectionGate.MatchState.ALLOWED) {
            return;
        }
        try {
            CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
            if (code.equals(config.modLanguage)) {
                return;
            }
            config.modLanguage = code;
            CCLiveUtilitiesConfig.HANDLER.save();
        } catch (Exception ignored) {
        }
    }
}
