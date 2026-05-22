package net.felix.leaderboards.collectors;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.felix.leaderboards.LeaderboardManager;
import net.felix.utilities.DebugUtility;
import net.felix.utilities.Overall.DimensionUtility;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sammelt Current Coins durch zufällige /cc coins Aufrufe
 * Führt den Command in zufälligen Intervallen aus um Server-Erkennung zu vermeiden
 */
public class CoinCollector implements DataCollector {
    private boolean isActive = false;
    private int tickCounter = 0;
    private long nextCommandTime = 0; // Absoluter Zeitpunkt in Millisekunden
    private final Random random = new Random();
    
    // Mindest- und Maximalabstand in Ticks (20 ticks = 1 Sekunde)
    private static final int MIN_INTERVAL = 20 * 60 * 30; // 30 Minuten
    private static final int MAX_INTERVAL = 20 * 60 * 60; // 60 Minuten
    private static final int MIN_DISTANCE = 20 * 60 * 5;  // 5 Minuten Mindestabstand
    
    // Pattern für die Coin-Antwort: [Legend] Du besitzt aktuell 56,989,382,785 Coins
    private static final Pattern COIN_PATTERN = Pattern.compile(".*\\[.*?\\].*Du besitzt aktuell ([0-9,]+) Coins.*", Pattern.CASE_INSENSITIVE);
    
    private long lastCoins = 0;
    private long lastCommandTime = 0;
    private boolean waitingForResponse = false;
    private boolean pendingSuccessFeedback = false;
    private long pendingCoins = 0;
    
    @Override
    public void initialize() {
        if (isActive) return;
        
        // Berechne ersten Command-Zeitpunkt (10-15 Minuten für schnelleren Start)
        scheduleFirstCommand();
        
        // Registriere Tick-Event
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        
        isActive = true;
        long timeUntilFirst = (nextCommandTime - System.currentTimeMillis()) / 60000; // Millisekunden zu Minuten
        // Silent error handling("✅ CoinCollector initialisiert (erster Command in " + timeUntilFirst + " Minuten)");
    }
    
    private void onClientTick(MinecraftClient client) {
        if (!isActive || client.player == null || client.world == null) {
            return;
        }
        
        // Prüfe ob ClipboardCoinCollector aktiv ist - wenn ja, deaktiviere diesen Collector
        if (net.felix.utilities.DragOverlay.ClipboardCoinCollector.isCollecting()) {
            return; // ClipboardCoinCollector läuft, dieser Collector soll nicht laufen
        }
        
        // Prüfe ob es Zeit für den nächsten Command ist
        long currentTime = System.currentTimeMillis();
        if (currentTime >= nextCommandTime && nextCommandTime > 0) {
            // In der Lobby kein /cc coins; Timer neu planen statt zu senden
            if (DimensionUtility.isInGeneralLobby(client)) {
                scheduleNextCommand();
                return;
            }
            // Silent error handling("💰 [CoinCollector] Automatischer /cc coins Command wird ausgeführt (waitingForResponse wird auf true gesetzt)");
            executeCoinsCommand(client);
            scheduleNextCommand();
        }
    }
    
    /**
     * Führt den /cc coins Command aus
     */
    private void executeCoinsCommand(MinecraftClient client) {
        if (DimensionUtility.isInGeneralLobby(client)) {
            return;
        }
        try {
            long currentTime = System.currentTimeMillis();
            
            // Prüfe Mindestabstand
            if (currentTime - lastCommandTime < MIN_DISTANCE * 50) { // 50ms pro tick
                // Silent error handling("⚠️ CoinCollector: Mindestabstand noch nicht erreicht, Command übersprungen");
                return;
            }
            
            // Debug-Nachricht vor Command (nur Console)
            DebugUtility.debugLeaderboard("Coins werden aufgerufen");
            
            // Sende Command
            client.player.networkHandler.sendChatCommand("cc coins");
            lastCommandTime = currentTime;
            waitingForResponse = true;
            
            // Silent error handling("💰 CoinCollector: /cc coins Command ausgeführt");
            
        } catch (Exception e) {
            System.err.println("❌ Fehler beim Ausführen von /cc coins: " + e.getMessage());
        }
    }
    
    /**
     * Plant den nächsten Command-Zeitpunkt
     */
    /**
     * Berechnet den ersten Command-Zeitpunkt (10-15 Minuten für schnelleren Start)
     */
    private void scheduleFirstCommand() {
        int firstInterval = 12000 + random.nextInt(6000); // 10-15 Minuten in Ticks (1200 Ticks-Einheiten = 1 Minute)
        nextCommandTime = System.currentTimeMillis() + (firstInterval * 50); // 50ms pro tick
        
        int minutesUntilNext = firstInterval / 1200;
        // Silent error handling("💰 CoinCollector: Erster Command in " + minutesUntilNext + " Minuten (dann normale 30-60min Intervalle)");
    }
    
    /**
     * Berechnet den nächsten zufälligen Command-Zeitpunkt (30-60 Minuten)
     */
    private void scheduleNextCommand() {
        int interval = MIN_INTERVAL + random.nextInt(MAX_INTERVAL - MIN_INTERVAL);
        nextCommandTime = System.currentTimeMillis() + (interval * 50); // 50ms pro tick
        
        int minutesUntilNext = interval / 1200; // 1200 ticks = 1 Minute
        // Silent error handling("💰 CoinCollector: Nächster Command in " + minutesUntilNext + " Minuten");
    }
    
    /**
     * Verarbeitet eine Chat-Nachricht auf Coin-Informationen
     * Diese Methode wird vom LeaderboardManager aufgerufen wenn eine Chat-Nachricht empfangen wird
     * @return true wenn die Nachricht unterdrückt werden soll, false wenn sie angezeigt werden soll
     */
    public boolean processChatMessage(String message) {
        if (!isActive) return false;
        
        // Prüfe ob ClipboardCoinCollector aktiv ist - wenn ja, lasse diesen Collector die Nachricht nicht verarbeiten
        if (net.felix.utilities.DragOverlay.ClipboardCoinCollector.isCollecting()) {
            return false; // ClipboardCoinCollector verarbeitet die Nachricht
        }
        
        // Debug: Alle Chat-Nachrichten loggen
        if (DebugUtility.isLeaderboardDebuggingEnabled()) {
            DebugUtility.debugLeaderboard("Chat-Nachricht empfangen: " + message);
        }
        
        Matcher matcher = COIN_PATTERN.matcher(message);
        if (matcher.find()) {
            try {
                String coinStr = matcher.group(1).replace(",", ""); // Entferne Kommas: 14,350,869,677 -> 14350869677
                long coins = Long.parseLong(coinStr);
                
                // Debug-Nachrichten
                if (DebugUtility.isLeaderboardDebuggingEnabled()) {
                    if (waitingForResponse) {
                        // Zeige Original-Nachricht im Debug-Modus (nur Console bei automatischem Command)
                        DebugUtility.debugLeaderboard("Original-Nachricht: " + message);
                    } else {
                        // Bei manuellem Command: Start-Separator + Debug-Nachrichten
                        sendDebugSeparator("§6=== Coin Collector ===");
                        DebugUtility.debugLeaderboard("Manueller /cc coins erkannt");
                        sendFeedbackMessage("§aManueller §e/cc coins §aerkannt");
                    }
                    DebugUtility.debugLeaderboard("Coinwert ausgelesen: " + matcher.group(1));
                    DebugUtility.debugLeaderboard("Plainwert: " + coins);
                    
                    // Nur bei manuellem Command Chat-Nachrichten senden
                    if (!waitingForResponse) {
                        sendFeedbackMessage("§aCoinwert ausgelesen: §e" + matcher.group(1));
                        sendFeedbackMessage("§aPlainwert: §e" + coins);
                    }
                }
                
                if (coins != lastCoins) {
                    lastCoins = coins;
                    
                    // Setze Pending-Flag für manuellen Command (immer bei manuellen Commands)
                    if (!waitingForResponse) {
                        pendingSuccessFeedback = true;
                        pendingCoins = coins;
                    }
                    
                    // Silent error handling("💰 Coins aktualisiert: " + coins + " (formatiert: " + matcher.group(1) + ")");
                }
                
                // Bestimme ob Nachricht unterdrückt werden soll
                // MANUELL (!waitingForResponse): Minecraft-Server Feedback IMMER zeigen (nicht unterdrücken)
                // AUTOMATISCH (waitingForResponse): Minecraft-Server Feedback unterdrücken
                boolean shouldSuppress = waitingForResponse;
                boolean wasManual = !waitingForResponse;  // Merke ob es manuell war
                // Silent error handling("💰 [CoinCollector] Chat-Nachricht verarbeitet - waitingForResponse=" + waitingForResponse + ", wasManual=" + wasManual);
                waitingForResponse = false;
                
                // Server-Update NACH dem Return - damit Server-Feedback zuerst kommt!
                if (coins > 0) {
                    final long finalCoins = coins;
                    // Verzögerter Aufruf nach Server-Feedback
                    CompletableFuture.runAsync(() -> {
                        try {
                            Thread.sleep(50); // Kurz warten damit Server-Feedback zuerst kommt
                            if (wasManual) {
                                // Manueller /cc coins - OHNE Cooldown
                                LeaderboardManager.getInstance().updateScoreManual("current_coins", finalCoins);
                            } else {
                                // Automatischer Call - MIT Cooldown
                                LeaderboardManager.getInstance().updateScore("current_coins", finalCoins);
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    });
                }
                
                return shouldSuppress;
                
            } catch (NumberFormatException e) {
                System.err.println("❌ Fehler beim Parsen der Coins: " + e.getMessage() + " (Input: " + matcher.group(1) + ")");
                waitingForResponse = false;
            }
        }
        
        return false;
    }
    
    /**
     * Setzt die Coins manuell (für Testing)
     */
    public void setCoins(long coins) {
        this.lastCoins = coins;
        LeaderboardManager.getInstance().updateScore("current_coins", coins);
    }
    
    /**
     * Gibt die aktuellen Coins zurück
     */
    public long getCoins() {
        return lastCoins;
    }
    
    /**
     * Führt sofort einen Coins-Command aus (für Testing)
     */
    public void forceCoinsCommand() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.player != null) {
            executeCoinsCommand(client);
        }
    }
    
    /**
     * Gibt den Zeitpunkt des nächsten Commands zurück (für Debug)
     */
    public long getNextCommandTime() {
        return nextCommandTime;
    }
    
    @Override
    public void shutdown() {
        isActive = false;
        // Silent error handling("🛑 CoinCollector gestoppt");
    }
    
    @Override
    public String getName() {
        return "CoinCollector";
    }
    
    @Override
    public boolean isActive() {
        return isActive;
    }
    
    /**
     * Sendet eine Feedback-Nachricht an den Spieler (aber nur wenn es keine CCLive-Debug Nachricht ist)
     */
    private void sendFeedbackMessage(String message) {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.player != null) {
                // Verwende einen speziellen Prefix um Endlosschleifen zu vermeiden
                String prefixedMessage = "§7[§bCCLive-Debug§7]§r " + message;
                client.player.sendMessage(Text.literal(prefixedMessage), false);
            }
        } catch (Exception e) {
            // Ignoriere Fehler bei Chat-Ausgabe
        }
    }
    
    /**
     * Sendet eine Debug-Abgrenzung ohne CCLive-Debug Prefix
     */
    private void sendDebugSeparator(String separator) {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.player != null) {
                client.player.sendMessage(Text.literal(separator), false);
            }
        } catch (Exception e) {
            // Ignoriere Fehler bei Chat-Ausgabe
        }
    }
    
    /**
     * Formatiert eine Zahl mit Tausendertrennzeichen
     */
    private String formatNumber(long number) {
        return String.format("%,d", number).replace('.', ',');
    }
    
    
    /**
     * Sendet Fehler-Feedback bei Server-Problemen
     */
    public void sendErrorFeedback() {
        if (DebugUtility.isLeaderboardDebuggingEnabled()) {
            sendFeedbackMessage("§cFehler beim Senden an Server!");
            sendDebugSeparator("§6=== Coin Collector Ende ===");
        }
    }
    
    /**
     * Callback-Methode für erfolgreiche Server-Updates
     */
    public void onServerUpdateSuccess(long coins) {
        // Silent error handling("🔥 DEBUG: onServerUpdateSuccess - pendingSuccessFeedback=" + pendingSuccessFeedback + ", debugEnabled=" + DebugUtility.isLeaderboardDebuggingEnabled());
        
        if (pendingSuccessFeedback && DebugUtility.isLeaderboardDebuggingEnabled()) {
            // Silent error handling("🔥 DEBUG: Sende Success-Feedback...");
            // Chat-Nachrichten müssen im Main-Thread gesendet werden
            MinecraftClient.getInstance().execute(() -> {
                // Leaderboard-Feedback NUR bei Debug-Modus
                sendFeedbackMessage("§eErfolgreich §aan Server gesendet");
                sendFeedbackMessage("§aCoins aktualisiert: §e" + formatNumber(coins));
                sendDebugSeparator("§6=== Coin Collector Ende ===");
            });
        } else {
            // Silent error handling("🔥 DEBUG: KEIN Success-Feedback - Bedingung nicht erfüllt");
        }
        pendingSuccessFeedback = false;
        pendingCoins = 0;
    }
    
    /**
     * Callback-Methode für fehlgeschlagene Server-Updates
     */
    public void onServerUpdateFailure() {
        if (pendingSuccessFeedback && DebugUtility.isLeaderboardDebuggingEnabled()) {
            // Chat-Nachrichten müssen im Main-Thread gesendet werden
            MinecraftClient.getInstance().execute(() -> {
                sendErrorFeedback();
            });
        }
        pendingSuccessFeedback = false;
        pendingCoins = 0;
    }
    
}
