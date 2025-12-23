package net.felix.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.felix.leaderboards.LeaderboardManager;
import net.felix.leaderboards.collectors.CoinCollector;
import net.felix.utilities.Aincraft.BPViewerUtility;
import net.felix.CCLiveUtilitiesConfig;
import net.felix.chat.ChatManager;
import net.felix.utilities.Other.ItemDisplayDebugUtility;
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
            
            // Player-Stats Commands
            .then(literal("ps")
                .then(literal("reset")
                    .executes(CCLiveCommands::resetPlayerStats))
            )
            
            // Chat Commands
            .then(literal("chat")
                .then(literal("toggle")
                    .then(argument("type", StringArgumentType.string())
                        .suggests(chatTypeSuggestions())
                        .executes(context -> toggleChat(context, StringArgumentType.getString(context, "type"))))))
            
            // Debug Commands
            .then(literal("debug")
                .then(literal("coin_collector")
                    .executes(CCLiveCommands::debugCoinCollector))
                .then(literal("itemdisplay")
                    .executes(CCLiveCommands::debugItemDisplay))
                .then(literal("nearby")
                    .executes(CCLiveCommands::debugNearbyDisplays)))
            
            // Allgemeine Commands
            .then(literal("help")
                .executes(CCLiveCommands::showHelp))
        );
        
        // /chat Command (separat)
        dispatcher.register(literal("chat")
            .then(argument("mode", StringArgumentType.string())
                .suggests(chatModeSuggestions())
                .executes(context -> setChatMode(context, StringArgumentType.getString(context, "mode"))))
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
     * Resettet die Player-Stats des aktuellen Spielers (nur aktuelle Season)
     */
    private static int resetPlayerStats(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Text.literal("§6Lösche Player-Stats..."));
        
        LeaderboardManager manager = LeaderboardManager.getInstance();
        if (!manager.isEnabled() || !manager.isRegistered()) {
            context.getSource().sendError(Text.literal("§cStats-/Leaderboard-System nicht verfügbar!"));
            return 0;
        }
        
        // Asynchroner Server-API-Aufruf
        CompletableFuture.runAsync(() -> {
            try {
                JsonObject response = manager.getHttpClient().deleteWithToken("/profile/reset", manager.getPlayerToken());
                if (response != null && response.has("success") && response.get("success").getAsBoolean()) {
                    context.getSource().sendFeedback(Text.literal("§aPlayer-Stats wurden gelöscht!"));
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
        
        // Zeige Diagnose in der Konsole
        manager.printDiagnostics();
        
        // Zeige Status im Chat
        context.getSource().sendFeedback(Text.literal("§6=== Leaderboard System Status ==="));
        context.getSource().sendFeedback(Text.literal("§7Aktiviert: " + (manager.isEnabled() ? "§aJa" : "§cNein")));
        context.getSource().sendFeedback(Text.literal("§7Registriert: " + (manager.isRegistered() ? "§aJa" : "§cNein")));
        context.getSource().sendFeedback(Text.literal("§7Spieler: " + (manager.getPlayerName() != null ? "§a" + manager.getPlayerName() : "§cUnbekannt")));
        context.getSource().sendFeedback(Text.literal("§7Token: " + (manager.getPlayerToken() != null ? "§aVorhanden" : "§cFehlt")));
        context.getSource().sendFeedback(Text.literal("§7Siehe Konsole für vollständige Diagnose"));
        
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
    
    // =================== CHAT COMMANDS ===================
    
    /**
     * Setzt den Chat-Modus (default oder cclive)
     */
    private static int setChatMode(CommandContext<FabricClientCommandSource> context, String mode) {
        ChatManager chatManager = ChatManager.getInstance();
        
        if (mode.equalsIgnoreCase("cclive")) {
            chatManager.setChatMode(ChatManager.ChatMode.CCLIVE);
            context.getSource().sendFeedback(Text.literal("§aChat-Modus auf CCLive gesetzt!"));
        } else if (mode.equalsIgnoreCase("default")) {
            chatManager.setChatMode(ChatManager.ChatMode.DEFAULT);
            context.getSource().sendFeedback(Text.literal("§aChat-Modus auf Default gesetzt!"));
        } else {
            context.getSource().sendError(Text.literal("§cUngültiger Modus! Verwende 'cclive' oder 'default'."));
            return 0;
        }
        
        return 1;
    }
    
    /**
     * Togglet die Sichtbarkeit eines Chat-Kanals
     */
    private static int toggleChat(CommandContext<FabricClientCommandSource> context, String chatType) {
        ChatManager chatManager = ChatManager.getInstance();
        
        if (!chatType.equalsIgnoreCase("default") && !chatType.equalsIgnoreCase("cclive")) {
            context.getSource().sendError(Text.literal("§cUngültiger Chat-Typ! Verwende 'default' oder 'cclive'."));
            return 0;
        }
        
        chatManager.toggleChat(chatType);
        boolean isVisible = chatManager.isChatVisible(chatType);
        
        context.getSource().sendFeedback(Text.literal(
            "§a" + chatType + " Chat ist jetzt " + (isVisible ? "§a§lAN" : "§c§lAUS")
        ));
        
        return 1;
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
        context.getSource().sendFeedback(Text.literal("§e§lChat Commands:"));
        context.getSource().sendFeedback(Text.literal("§7/chat <cclive/default> §f- Chat-Modus wechseln"));
        context.getSource().sendFeedback(Text.literal("§7/cclive chat toggle <default/cclive> §f- Chat-Sichtbarkeit togglen"));
        context.getSource().sendFeedback(Text.literal("§7@cclive <nachricht> §f- Direkt in CCLive Chat schreiben"));
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
    
    // =================== DEBUG: ITEM DISPLAY ===================
    private static int debugItemDisplay(CommandContext<FabricClientCommandSource> context) {
        try {
            int result = ItemDisplayDebugUtility.dump(context.getSource());
            if (result == 0) {
                context.getSource().sendError(Text.literal("§cKein passendes Entity gefunden."));
            }
            return result;
        } catch (Exception e) {
            context.getSource().sendError(Text.literal("§cFehler beim Auslesen des ItemDisplays: " + e.getMessage()));
            return 0;
        }
    }
    
    // =================== DEBUG: NEARBY DISPLAYS ===================
    private static int debugNearbyDisplays(CommandContext<FabricClientCommandSource> context) {
        try {
            // fester Radius 8 Blöcke für den Anfang
            return ItemDisplayDebugUtility.scanNearby(context.getSource(), 8.0);
        } catch (Exception e) {
            context.getSource().sendError(Text.literal("§cFehler beim Nearby-Scan: " + e.getMessage()));
            return 0;
        }
    }
    
    // =================== TAB COMPLETION ===================
    
    /**
     * Suggestion Provider für Chat-Modi (default/cclive)
     */
    private static SuggestionProvider<FabricClientCommandSource> chatModeSuggestions() {
        return (context, builder) -> {
            String input = builder.getRemaining().toLowerCase();
            if ("default".startsWith(input)) {
                builder.suggest("default");
            }
            if ("cclive".startsWith(input)) {
                builder.suggest("cclive");
            }
            return builder.buildFuture();
        };
    }
    
    /**
     * Suggestion Provider für Chat-Typen (default/cclive)
     */
    private static SuggestionProvider<FabricClientCommandSource> chatTypeSuggestions() {
        return (context, builder) -> {
            String input = builder.getRemaining().toLowerCase();
            if ("default".startsWith(input)) {
                builder.suggest("default");
            }
            if ("cclive".startsWith(input)) {
                builder.suggest("cclive");
            }
            return builder.buildFuture();
        };
    }
}
