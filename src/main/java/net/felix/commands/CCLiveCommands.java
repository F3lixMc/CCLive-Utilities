package net.felix.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.felix.leaderboards.LeaderboardManager;
import net.felix.leaderboards.collectors.CoinCollector;
import net.felix.utilities.BPViewerUtility;
import net.felix.utilities.DebugUtility;
import net.felix.CCLiveUtilitiesConfig;
import net.minecraft.text.Text;

import java.util.concurrent.CompletableFuture;
import com.google.gson.JsonObject;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;

/**
 * Zentrale Command-Klasse für alle /cclive Commands
 * Vereint Blueprint- und Leaderboard-Commands unter einem Hauptcommand
 */
public class CCLiveCommands {
    
    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            registerCCLiveCommands(dispatcher);
        });
    }
    
    private static void registerCCLiveCommands(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(literal("cclive")
            // Blueprint Commands
            .then(literal("bp")
                .then(literal("reset")
                    .executes(CCLiveCommands::resetBlueprints))
                .then(literal("set")
                    .then(argument("floor", StringArgumentType.string())
                        .executes(context -> setBlueprintFloor(context, StringArgumentType.getString(context, "floor")))))
                .then(literal("status")
                    .executes(CCLiveCommands::showBlueprintStatus)))
            
            // Leaderboard Commands  
            .then(literal("lb")
                .then(literal("reset")
                    .then(literal("all")
                        .executes(CCLiveCommands::resetAllScores))
                    .then(argument("board", StringArgumentType.word())
                        .executes(context -> resetBoardScore(context, StringArgumentType.getString(context, "board")))))
                .then(literal("status")
                    .executes(CCLiveCommands::showLeaderboardStatus))
                .then(literal("refresh")
                    .executes(CCLiveCommands::refreshRegistration))
)
            
            // Debug Commands
            .then(literal("debug")
                .then(literal("coin_collector")
                    .executes(CCLiveCommands::debugCoinCollector)))
            
            // Allgemeine Commands
            .then(literal("help")
                .executes(CCLiveCommands::showHelp))
        );
    }
    
    // =================== BLUEPRINT COMMANDS ===================
    
    /**
     * Resettet alle gefundenen Baupläne
     */
    private static int resetBlueprints(CommandContext<FabricClientCommandSource> context) {
        try {
            BPViewerUtility instance = BPViewerUtility.getInstance();
            instance.resetFoundBlueprints();
            context.getSource().sendFeedback(Text.literal("§aAlle gefundenen Baupläne wurden zurückgesetzt!"));
            return 1;
        } catch (Exception e) {
            context.getSource().sendError(Text.literal("§cFehler beim Zurücksetzen der Baupläne: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Setzt den manuellen Floor für Blueprint-Viewer
     */
    private static int setBlueprintFloor(CommandContext<FabricClientCommandSource> context, String floor) {
        try {
            BPViewerUtility instance = BPViewerUtility.getInstance();
            instance.setManualFloor(floor);
            context.getSource().sendFeedback(Text.literal("§aFloor manuell auf " + floor + " gesetzt!"));
            return 1;
        } catch (Exception e) {
            context.getSource().sendError(Text.literal("§cFehler beim Setzen des Floors: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Zeigt Blueprint-System Status
     */
    private static int showBlueprintStatus(CommandContext<FabricClientCommandSource> context) {
        // Prüfe ob Debug-Modus aktiviert ist
        if (!CCLiveUtilitiesConfig.HANDLER.instance().blueprintDebugging) {
            context.getSource().sendError(Text.literal("§cDieser Command ist nur im Debug-Modus verfügbar!"));
            context.getSource().sendError(Text.literal("§7Aktiviere 'Blueprint Debugging' in der Config."));
            return 0;
        }
        
        try {
            BPViewerUtility instance = BPViewerUtility.getInstance();
            String currentFloor = instance.getActiveFloor();
            
            context.getSource().sendFeedback(Text.literal("§6=== Blueprint System Status ==="));
            context.getSource().sendFeedback(Text.literal("§7Aktueller Floor: " + (currentFloor != null ? "§a" + currentFloor : "§cUnbekannt")));
            context.getSource().sendFeedback(Text.literal("§7Sichtbar: " + (BPViewerUtility.isVisible() ? "§aJa" : "§cNein")));
            
            return 1;
        } catch (Exception e) {
            context.getSource().sendError(Text.literal("§cFehler beim Abrufen des Blueprint-Status: " + e.getMessage()));
            return 0;
        }
    }
    
    // =================== LEADERBOARD COMMANDS ===================
    
    /**
     * Resettet alle Leaderboard-Scores
     */
    private static int resetAllScores(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Text.literal("§6Lösche alle Leaderboard-Scores..."));
        
        LeaderboardManager manager = LeaderboardManager.getInstance();
        if (!manager.isEnabled() || !manager.isRegistered()) {
            context.getSource().sendError(Text.literal("§cLeaderboard-System nicht verfügbar!"));
            return 0;
        }
        
        // Asynchroner Server-API-Aufruf
        CompletableFuture.runAsync(() -> {
            try {
                JsonObject response = manager.getHttpClient().deleteWithToken("/remove/all", manager.getPlayerToken());
                if (response != null && response.has("success") && response.get("success").getAsBoolean()) {
                    context.getSource().sendFeedback(Text.literal("§aAlle Leaderboard-Scores wurden gelöscht!"));
                    if (response.has("deleted")) {
                        int deleted = response.get("deleted").getAsInt();
                        context.getSource().sendFeedback(Text.literal("§7" + deleted + " Einträge entfernt"));
                    }
                } else {
                    String message = response != null && response.has("message") ? 
                        response.get("message").getAsString() : "Unbekannter Fehler";
                    context.getSource().sendError(Text.literal("§cFehler: " + message));
                }
            } catch (Exception e) {
                context.getSource().sendError(Text.literal("§cVerbindungsfehler: " + e.getMessage()));
            }
        });
        
        return 1;
    }
    
    /**
     * Resettet den Score eines bestimmten Leaderboards
     */
    private static int resetBoardScore(CommandContext<FabricClientCommandSource> context, String boardName) {
        context.getSource().sendFeedback(Text.literal("§6Lösche Score für " + boardName + "..."));
        
        LeaderboardManager manager = LeaderboardManager.getInstance();
        if (!manager.isEnabled() || !manager.isRegistered()) {
            context.getSource().sendError(Text.literal("§cLeaderboard-System nicht verfügbar!"));
            return 0;
        }
        
        // Asynchroner Server-API-Aufruf
        CompletableFuture.runAsync(() -> {
            try {
                JsonObject response = manager.getHttpClient().deleteWithToken("/remove/" + boardName, manager.getPlayerToken());
                if (response != null && response.has("success") && response.get("success").getAsBoolean()) {
                    context.getSource().sendFeedback(Text.literal("§aScore für " + boardName + " wurde gelöscht!"));
                } else {
                    String message = response != null && response.has("message") ? 
                        response.get("message").getAsString() : "Unbekannter Fehler";
                    context.getSource().sendError(Text.literal("§cFehler: " + message));
                }
            } catch (Exception e) {
                context.getSource().sendError(Text.literal("§cVerbindungsfehler: " + e.getMessage()));
            }
        });
        
        return 1;
    }
    
    /**
     * Zeigt den Status des Leaderboard-Systems
     */
    private static int showLeaderboardStatus(CommandContext<FabricClientCommandSource> context) {
        // Prüfe ob Debug-Modus aktiviert ist
        if (!CCLiveUtilitiesConfig.HANDLER.instance().leaderboardDebugging) {
            context.getSource().sendError(Text.literal("§cDieser Command ist nur im Debug-Modus verfügbar!"));
            context.getSource().sendError(Text.literal("§7Aktiviere 'Leaderboard Debugging' in der Config."));
            return 0;
        }
        
        LeaderboardManager manager = LeaderboardManager.getInstance();
        
        context.getSource().sendFeedback(Text.literal("§6=== Leaderboard System Status ==="));
        context.getSource().sendFeedback(Text.literal("§7Aktiviert: " + (manager.isEnabled() ? "§aJa" : "§cNein")));
        context.getSource().sendFeedback(Text.literal("§7Registriert: " + (manager.isRegistered() ? "§aJa" : "§cNein")));
        context.getSource().sendFeedback(Text.literal("§7Spieler: " + (manager.getPlayerName() != null ? "§a" + manager.getPlayerName() : "§cUnbekannt")));
        
        return 1;
    }
    
    /**
     * Erneuert die Registrierung
     */
    private static int refreshRegistration(CommandContext<FabricClientCommandSource> context) {
        // Prüfe ob Debug-Modus aktiviert ist
        if (!CCLiveUtilitiesConfig.HANDLER.instance().leaderboardDebugging) {
            context.getSource().sendError(Text.literal("§cDieser Command ist nur im Debug-Modus verfügbar!"));
            context.getSource().sendError(Text.literal("§7Aktiviere 'Leaderboard Debugging' in der Config."));
            return 0;
        }
        
        context.getSource().sendFeedback(Text.literal("§6Erneuere Leaderboard-Registrierung..."));
        
        LeaderboardManager.getInstance().refreshRegistration();
        
        return 1;
    }
    
    
    // =================== HELP COMMAND ===================
    
    // =================== DEBUG COMMANDS ===================
    
    /**
     * Debug-Info für CoinCollector
     */
    private static int debugCoinCollector(CommandContext<FabricClientCommandSource> context) {
        // Prüfe ob Debug-Modus aktiviert ist
        if (!CCLiveUtilitiesConfig.HANDLER.instance().leaderboardDebugging) {
            context.getSource().sendError(Text.literal("§cDieser Command ist nur im Debug-Modus verfügbar!"));
            context.getSource().sendError(Text.literal("§7Aktiviere 'Leaderboard Debugging' in der Config."));
            return 0;
        }
        
        try {
            LeaderboardManager manager = LeaderboardManager.getInstance();
            CoinCollector coinCollector = (CoinCollector) manager.getCollector("coins");
            
            if (coinCollector == null || !coinCollector.isActive()) {
                context.getSource().sendError(Text.literal("§cCoinCollector ist nicht aktiv!"));
                return 0;
            }
            
            // Hole nächsten Command-Zeitpunkt
            long nextCommandTime = coinCollector.getNextCommandTime();
            long currentTime = System.currentTimeMillis();
            long timeUntilNext = Math.max(0, nextCommandTime - currentTime);
            
            context.getSource().sendFeedback(Text.literal("§6=== CoinCollector Debug ==="));
            context.getSource().sendFeedback(Text.literal("§7Status: " + (coinCollector.isActive() ? "§aAktiv" : "§cInaktiv")));
            context.getSource().sendFeedback(Text.literal("§7Aktuelle Coins: §a" + String.format("%,d", coinCollector.getCoins()).replace('.', ',')));
            context.getSource().sendFeedback(Text.literal("§7Nächster /cc coins Command: §a" + formatTime(timeUntilNext)));
            
            return 1;
        } catch (Exception e) {
            context.getSource().sendError(Text.literal("§cFehler beim Abrufen der CoinCollector-Debug-Info: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Formatiert Zeit in Millisekunden zu lesbarem Format
     */
    private static String formatTime(long milliseconds) {
        if (milliseconds <= 0) {
            return "Jetzt";
        }
        
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        
        if (minutes > 0) {
            return minutes + "m " + seconds + "s";
        } else {
            return seconds + "s";
        }
    }
    
    // =================== HELP COMMAND ===================
    
    /**
     * Zeigt alle verfügbaren Commands
     */
    private static int showHelp(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Text.literal("§6=== CCLive-Utilities Commands ==="));
        context.getSource().sendFeedback(Text.literal(""));
        context.getSource().sendFeedback(Text.literal("§e§lBlueprint Commands:"));
        context.getSource().sendFeedback(Text.literal("§7/cclive bp reset §f- Alle Baupläne zurücksetzen"));
        context.getSource().sendFeedback(Text.literal("§7/cclive bp set <floor> §f- Floor manuell setzen"));
        context.getSource().sendFeedback(Text.literal(""));
        context.getSource().sendFeedback(Text.literal("§e§lLeaderboard Commands:"));
        context.getSource().sendFeedback(Text.literal("§7/cclive lb reset all §f- Alle Scores löschen"));
        context.getSource().sendFeedback(Text.literal("§7/cclive lb reset <board> §f- Spezifischen Score löschen"));
        context.getSource().sendFeedback(Text.literal(""));
        
        // Debug Commands nur anzeigen wenn Debug aktiviert ist
        if (CCLiveUtilitiesConfig.HANDLER.instance().blueprintDebugging) {
            context.getSource().sendFeedback(Text.literal("§e§lBlueprint Debug Commands:"));
            context.getSource().sendFeedback(Text.literal("§7/cclive bp status §f- Blueprint-System Status"));
            context.getSource().sendFeedback(Text.literal(""));
        }
        
        if (CCLiveUtilitiesConfig.HANDLER.instance().leaderboardDebugging) {
            context.getSource().sendFeedback(Text.literal("§e§lLeaderboard Debug Commands:"));
            context.getSource().sendFeedback(Text.literal("§7/cclive lb status §f- Leaderboard-System Status"));
            context.getSource().sendFeedback(Text.literal("§7/cclive lb refresh §f- Registrierung erneuern"));
            context.getSource().sendFeedback(Text.literal("§7/cclive debug coin_collector §f- CoinCollector Debug-Info"));
            context.getSource().sendFeedback(Text.literal(""));
        }
        
        context.getSource().sendFeedback(Text.literal("§7/cclive help §f- Diese Hilfe anzeigen"));
        context.getSource().sendFeedback(Text.literal(""));
        context.getSource().sendFeedback(Text.literal("§7Debug-Commands sind nur sichtbar wenn Debug-Modi in der Config aktiviert sind."));
        
        return 1;
    }
    
    /**
     * Formatiert eine Zahl mit Tausendertrennzeichen (deutsche Formatierung)
     */
    private static String formatNumber(long number) {
        return String.format("%,d", number).replace(",", ".");
    }
}
