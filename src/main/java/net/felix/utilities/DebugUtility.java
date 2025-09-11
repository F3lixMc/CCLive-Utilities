package net.felix.utilities;

import net.felix.CCLiveUtilitiesConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

/**
 * Utility-Klasse für einheitliche Debug-Nachrichten
 */
public class DebugUtility {
    
    private static final String DEBUG_PREFIX = "§7[§bCCLive-Debug§7]§r ";
    
    /**
     * Sendet eine Blueprint-Debug-Nachricht
     */
    public static void debugBlueprint(String message) {
        if (CCLiveUtilitiesConfig.HANDLER.instance().blueprintDebugging) {
            sendDebugMessage(message);
        }
    }
    
    /**
     * Sendet eine Leaderboard-Debug-Nachricht
     */
    public static void debugLeaderboard(String message) {
        if (CCLiveUtilitiesConfig.HANDLER.instance().leaderboardDebugging) {
            sendDebugMessage(message);
        }
    }
    
    /**
     * Sendet eine Debug-Nachricht nur an die Console (nicht in den Chat um Endlosschleifen zu vermeiden)
     */
    private static void sendDebugMessage(String message) {
        // Nur Console-Ausgabe - Chat-Ausgabe würde Endlosschleifen verursachen
        System.out.println("[CCLive-Debug] " + message);
    }
    
    /**
     * Prüft ob Blueprint-Debugging aktiviert ist
     */
    public static boolean isBlueprintDebuggingEnabled() {
        return CCLiveUtilitiesConfig.HANDLER.instance().blueprintDebugging;
    }
    
    /**
     * Prüft ob Leaderboard-Debugging aktiviert ist
     */
    public static boolean isLeaderboardDebuggingEnabled() {
        return CCLiveUtilitiesConfig.HANDLER.instance().leaderboardDebugging;
    }
}
