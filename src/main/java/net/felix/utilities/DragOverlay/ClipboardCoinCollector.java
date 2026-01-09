package net.felix.utilities.DragOverlay;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.felix.CCLiveUtilitiesConfig;

import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sammelt Current Coins durch zufällige /cc coins Aufrufe für das Clipboard
 * Führt den Command in zufälligen Intervallen aus (2-10 Minuten)
 * Nur aktiv wenn Clipboard angezeigt wird und Baupläne vorhanden sind
 */
public class ClipboardCoinCollector {
    private static boolean isActive = false;
    private static long nextCommandTime = 0; // Absoluter Zeitpunkt in Millisekunden
    private static final Random random = new Random();
    
    // Intervall: 2-10 Minuten in Millisekunden
    private static final long MIN_INTERVAL_MS = 2 * 60 * 1000; // 2 Minuten
    private static final long MAX_INTERVAL_MS = 10 * 60 * 1000; // 10 Minuten
    
    // Pattern für die Coin-Antwort: [Legend] Du besitzt aktuell 56,989,382,785 Coins
    private static final Pattern COIN_PATTERN = Pattern.compile(".*\\[.*?\\].*Du besitzt aktuell ([0-9,]+) Coins.*", Pattern.CASE_INSENSITIVE);
    
    private static long currentCoins = 0;
    private static boolean waitingForResponse = false;
    
    /**
     * Initialisiert den ClipboardCoinCollector
     */
    public static void initialize() {
        if (isActive) return;
        
        // Berechne ersten Command-Zeitpunkt (zufällig zwischen 2-10 Minuten)
        scheduleNextCommand();
        
        // Registriere Tick-Event
        ClientTickEvents.END_CLIENT_TICK.register(ClipboardCoinCollector::onClientTick);
        
        isActive = true;
    }
    
    /**
     * Wird jeden Tick aufgerufen
     */
    private static void onClientTick(MinecraftClient client) {
        if (!isActive || client.player == null || client.world == null) {
            return;
        }
        
        // Prüfe ob Clipboard aktiv ist und Baupläne vorhanden sind
        if (!shouldCollectCoins()) {
            // Pausiere: Setze nextCommandTime zurück, damit Timer neu startet wenn Bedingung wieder erfüllt ist
            if (nextCommandTime > 0) {
                nextCommandTime = 0;
            }
            return;
        }
        
        // Wenn Bedingung erfüllt ist und noch kein Timer läuft, starte neuen Timer
        if (nextCommandTime == 0) {
            scheduleNextCommand();
        }
        
        // Prüfe ob es Zeit für den nächsten Command ist
        long currentTime = System.currentTimeMillis();
        if (currentTime >= nextCommandTime && nextCommandTime > 0) {
            executeCoinsCommand(client);
            scheduleNextCommand();
        }
    }
    
    /**
     * Prüft ob Coins gesammelt werden sollen
     * Nur wenn Clipboard angezeigt wird und Baupläne vorhanden sind
     */
    private static boolean shouldCollectCoins() {
        // Prüfe ob Clipboard aktiviert ist
        if (!CCLiveUtilitiesConfig.HANDLER.instance().clipboardEnabled || 
            !CCLiveUtilitiesConfig.HANDLER.instance().showClipboard) {
            return false;
        }
        
        // Prüfe ob Baupläne vorhanden sind
        List<ClipboardUtility.ClipboardEntry> entries = ClipboardUtility.getEntries();
        return entries != null && !entries.isEmpty();
    }
    
    /**
     * Führt den /cc coins Command aus
     */
    private static void executeCoinsCommand(MinecraftClient client) {
        try {
            // Sende Command
            client.player.networkHandler.sendChatCommand("cc coins");
            waitingForResponse = true;
        } catch (Exception e) {
            // Silent error handling
        }
    }
    
    /**
     * Berechnet den nächsten zufälligen Command-Zeitpunkt (2-10 Minuten)
     * Verwendet Millisekunden für präzise Zeitpunkte (auch z.B. 2min 30.6 Sekunden)
     */
    private static void scheduleNextCommand() {
        // Zufälliges Intervall zwischen MIN und MAX in Millisekunden
        long intervalMs = MIN_INTERVAL_MS + (long)(random.nextDouble() * (MAX_INTERVAL_MS - MIN_INTERVAL_MS));
        nextCommandTime = System.currentTimeMillis() + intervalMs;
    }
    
    /**
     * Verarbeitet eine Chat-Nachricht auf Coin-Informationen
     * Diese Methode wird vom ChatMixin aufgerufen wenn eine Chat-Nachricht empfangen wird
     * @return true wenn die Nachricht unterdrückt werden soll, false wenn sie angezeigt werden soll
     */
    public static boolean processChatMessage(String message) {
        if (!isActive) {
            return false;
        }
        
        Matcher matcher = COIN_PATTERN.matcher(message);
        if (matcher.find()) {
            try {
                String coinStr = matcher.group(1).replace(",", ""); // Entferne Kommas
                long coins = Long.parseLong(coinStr);
                
                // Unterscheide zwischen automatischem und manuellem Command (vor dem Setzen von waitingForResponse)
                boolean wasAutomatic = waitingForResponse;
                boolean wasManual = !waitingForResponse;
                
                if (coins > 0) {
                    currentCoins = coins;
                    
                    // Sende auch Update an das Leaderboard (wenn aktiviert)
                    // Sende Update an Leaderboard (wie im Leaderboard CoinCollector)
                    if (wasManual) {
                        // Manueller /cc coins - OHNE Cooldown
                        net.felix.leaderboards.LeaderboardManager.getInstance().updateScoreManual("current_coins", coins);
                    } else {
                        // Automatischer Call - MIT Cooldown
                        net.felix.leaderboards.LeaderboardManager.getInstance().updateScore("current_coins", coins);
                    }
                }
                
                waitingForResponse = false;
                
                // Unterdrücke die Nachricht nur bei automatischen Commands
                // Bei manuellen Commands (waitingForResponse war false) wird die Nachricht angezeigt
                return wasAutomatic;
                
            } catch (NumberFormatException e) {
                waitingForResponse = false;
            }
        }
        
        return false;
    }
    
    /**
     * Gibt die aktuellen Coins zurück
     */
    public static long getCurrentCoins() {
        return currentCoins;
    }
    
    /**
     * Setzt die Coins manuell (für Testing)
     */
    public static void setCoins(long coins) {
        currentCoins = coins;
    }
    
    /**
     * Gibt den Zeitpunkt des nächsten Commands zurück (für Debug)
     */
    public static long getNextCommandTime() {
        return nextCommandTime;
    }
    
    /**
     * Stoppt den Collector
     */
    public static void shutdown() {
        isActive = false;
    }
    
    /**
     * Prüft ob der Collector aktiv ist
     */
    public static boolean isActive() {
        return isActive;
    }
    
    /**
     * Prüft ob der ClipboardCoinCollector tatsächlich läuft (aktiviert UND Bedingungen erfüllt)
     * Wird vom Leaderboard CoinCollector verwendet, um zu prüfen ob er deaktiviert werden soll
     */
    public static boolean isCollecting() {
        return isActive && shouldCollectCoins();
    }
}
