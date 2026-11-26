package net.felix.commands;

import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.felix.CCLiveUtilitiesConfig;
import net.felix.Community.chat.ChatManager;
import net.felix.Community.identity.WebsiteProfileLogin;
import net.felix.Community.leaderboards.LeaderboardManager;
import net.felix.Community.staff.StaffClientAccess;
import net.felix.Community.staff.StaffModerationCommands;
import net.felix.Community.staff.StaffNameSuggestions;
import net.felix.Community.staff.StaffPermsCommands;
import net.felix.utilities.Aincraft.blueprints.viewer.BPViewerUtility;
import net.felix.utilities.Other.devtools.DevToolsAccess;
import net.felix.utilities.Other.devtools.DevToolsScreen;
import net.felix.utilities.Other.glyphs.ZeichenUtility;
import net.felix.utilities.Overall.clipboard.collector.ClipboardAmbossRessourceCollector;
import net.felix.utilities.Overall.clipboard.overlay.ClipboardDraggableOverlay;
import net.felix.utilities.Overall.clipboard.storage.CollectedMaterialsResourcesStorage;
import net.felix.utilities.Overall.eventsound.sound.EventSoundPlayer;
import net.felix.utilities.Overall.eventsound.ui.EventSoundListScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.*;

/**
 * Zentrale Command-Klasse für alle /cclive Commands
 * Vereint Blueprint- und Leaderboard-Commands unter einem Hauptcommand
 */
public class CCLiveCommands {

    private static final int MAX_FLOOR = 100;

    private static boolean wasInClassSelectionInventory = false;

    public static void register() {
        registerClassSelectionInventoryReset();
        StaffClientAccess.register();
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            registerCCLiveCommands(dispatcher);
        });
    }

    private static void registerClassSelectionInventoryReset() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.gui.screen() instanceof AbstractContainerScreen<?> handledScreen) {
                String title = handledScreen.getTitle().getString();
                boolean inClassSelection = ZeichenUtility.containsClassSelectionUi(title);
                if (inClassSelection && !wasInClassSelectionInventory) {
                    resetBlueprintsAndMaterialsProgress();
                }
                wasInClassSelectionInventory = inClassSelection;
            } else {
                wasInClassSelectionInventory = false;
            }
        });
    }

    public static void resetFoundBlueprintsProgress() {
        BPViewerUtility.getInstance().resetFoundBlueprints();
    }

    public static void resetMaterialsAndResourcesProgress() {
        CollectedMaterialsResourcesStorage.resetAll();
        ClipboardDraggableOverlay.resetCollectedMaterials();
        ClipboardAmbossRessourceCollector.resetCollectedResources();
    }

    public static void resetBlueprintsAndMaterialsProgress() {
        resetFoundBlueprintsProgress();
        resetMaterialsAndResourcesProgress();
    }
    
    static void registerCCLiveCommands(CommandDispatcher<FabricClientCommandSource> dispatcher) {
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
                .executes(CCLiveCommands::showLeaderboardHint)
                .then(leaderboardResetCommand())
            )

            .then(literal("connection")
                .then(literal("status")
                    .executes(CCLiveCommands::showConnectionStatus))
                .then(literal("refresh")
                    .executes(CCLiveCommands::refreshConnection))
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
                        .executes(context -> toggleChat(context, StringArgumentType.getString(context, "type")))))
                .then(literal("export-glyphs")
                    .executes(CCLiveCommands::exportBlockedGlyphs))
                .then(literal("ignore")
                    .then(literal("list")
                        .executes(CCLiveCommands::openIgnoreList))))
            .then(literal("chatrules")
                .executes(CCLiveCommands::openChatRules))
            .then(literal("MKs")
                .executes(CCLiveCommands::openMkOverview))
            .then(literal("report")
                .then(argument("player", StringArgumentType.word())
                    .executes(context -> submitReport(context, ""))
                    .then(argument("reason", StringArgumentType.greedyString())
                        .executes(context -> submitReport(
                            context, StringArgumentType.getString(context, "reason"))))))
            
            // Materials/Resources Commands
            .then(literal("materials_resources")
                .then(literal("reset")
                    .executes(CCLiveCommands::resetMaterialsResources)))
            
            .then(literal("eventsound")
                .then(literal("open")
                    .executes(CCLiveCommands::openEventSounds))
                .then(literal("stop")
                    .executes(CCLiveCommands::stopEventSounds)))

            // Fragment drop animation test
            .then(literal("texture_debug")
                .executes(CCLiveCommands::debugCustomTexture))
            .then(literal("fragment_animation_test")
                .executes(context -> testFragmentAnimation(context, null))
                .then(argument("charm", StringArgumentType.word())
                    .suggests((context, builder) -> {
                        for (net.felix.utilities.Overall.customtextures.CharmCatalog.Charm charm
                                : net.felix.utilities.Overall.customtextures.CharmCatalog.all()) {
                            builder.suggest(charm.key());
                        }
                        return builder.buildFuture();
                    })
                    .executes(context -> testFragmentAnimation(context, StringArgumentType.getString(context, "charm")))))
            
            .then(literal("mod")
                .requires(source -> StaffClientAccess.isStaffCommandVisible())
                .executes(StaffModerationCommands::openScreen)
                .then(literal("delete")
                    .then(argument("id", StringArgumentType.word())
                        .then(argument("reason", StringArgumentType.greedyString())
                            .executes(context -> StaffModerationCommands.delete(
                                context,
                                parseMessageId(StringArgumentType.getString(context, "id")),
                                StringArgumentType.getString(context, "reason"))))))
                .then(literal("mute")
                    .then(sanctionName("mute", true)))
                .then(literal("ban")
                    .then(sanctionName("ban", false)))
                .then(literal("unmute")
                    .then(argument("name", StringArgumentType.word())
                        .suggests(StaffNameSuggestions.activeSanctions("mute"))
                        .then(argument("reason", StringArgumentType.greedyString())
                            .executes(context -> StaffModerationCommands.unmute(
                                context,
                                StringArgumentType.getString(context, "name"),
                                StringArgumentType.getString(context, "reason"))))))
                .then(literal("unban")
                    .then(argument("name", StringArgumentType.word())
                        .suggests(StaffNameSuggestions.activeSanctions("ban"))
                        .then(argument("reason", StringArgumentType.greedyString())
                            .executes(context -> StaffModerationCommands.unban(
                                context,
                                StringArgumentType.getString(context, "name"),
                                StringArgumentType.getString(context, "reason")))))))

            .then(literal("perms")
                .requires(source -> StaffClientAccess.isAdminCommandVisible())
                .then(literal("user")
                    .then(literal("list")
                        .executes(StaffPermsCommands::list))
                    .then(literal("add")
                        .then(argument("name", StringArgumentType.word())
                            .suggests(StaffNameSuggestions.verifiedPlayers())
                            .executes(context -> StaffPermsCommands.add(context, StringArgumentType.getString(context, "name")))))
                    .then(literal("remove")
                        .then(argument("name", StringArgumentType.word())
                            .suggests(StaffNameSuggestions.currentModerators())
                            .executes(context -> StaffPermsCommands.remove(context, StringArgumentType.getString(context, "name")))))))

            // DevTools (Allowlist / Fabric-Dev)
            .then(literal("devtools")
                .executes(CCLiveCommands::openDevTools))
            
            // Config
            .then(literal("config")
                .then(literal("reset")
                    .executes(CCLiveCommands::resetConfig)))

            // Allgemeine Commands
            .then(literal("help")
                .executes(CCLiveCommands::showHelp))
            .then(literal("profil")
                .then(literal("login")
                    .then(argument("code", StringArgumentType.word())
                        .executes(CCLiveCommands::profileLogin))))
        );
        
        // /chat Command (separat)
        dispatcher.register(literal("chat")
            .then(argument("mode", StringArgumentType.string())
                .suggests(chatModeSuggestions())
                .executes(context -> setChatMode(context, StringArgumentType.getString(context, "mode"))))
        );
    }
    
    private static int profileLogin(CommandContext<FabricClientCommandSource> context) {
        String code = StringArgumentType.getString(context, "code");
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            context.getSource().sendError(Component.literal("§cSpieler nicht verfügbar."));
            return 0;
        }
        context.getSource().sendFeedback(Component.literal("§eBestätigung läuft…"));
        WebsiteProfileLogin.start(client, code);
        return 1;
    }

    private static int resetConfig(CommandContext<FabricClientCommandSource> context) {
        try {
            if (CCLiveUtilitiesConfig.resetToDefaults()) {
                context.getSource().sendFeedback(Component.literal("§aDie Config wurde gelöscht und mit Standardwerten neu erstellt."));
                return 1;
            }
            context.getSource().sendError(Component.literal("§cDie Config konnte nicht zurückgesetzt werden."));
            return 0;
        } catch (Exception e) {
            context.getSource().sendError(Component.literal("§cFehler beim Zurücksetzen der Config: " + e.getMessage()));
            return 0;
        }
    }

    // =================== BLUEPRINT COMMANDS ===================
    
    /**
     * Resettet alle gefundenen Baupläne
     */
    private static int resetBlueprints(CommandContext<FabricClientCommandSource> context) {
        try {
            resetFoundBlueprintsProgress();
            context.getSource().sendFeedback(Component.literal("§aAlle gefundenen Baupläne wurden zurückgesetzt!"));
            return 1;
        } catch (Exception e) {
            context.getSource().sendError(Component.literal("§cFehler beim Zurücksetzen der Baupläne: " + e.getMessage()));
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
            context.getSource().sendFeedback(Component.literal("§aFloor manuell auf " + floor + " gesetzt!"));
            return 1;
        } catch (Exception e) {
            context.getSource().sendError(Component.literal("§cFehler beim Setzen des Floors: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Zeigt Blueprint-System Status
     */
    private static int showLeaderboardHint(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Component.literal("§7Die Rangliste steht links im eigenen Inventar."));
        return 1;
    }

    private static int showBlueprintStatus(CommandContext<FabricClientCommandSource> context) {
        // Prüfe ob Debug-Modus aktiviert ist
        if (!CCLiveUtilitiesConfig.HANDLER.instance().blueprintDebugging) {
            context.getSource().sendError(Component.literal("§cDieser Command ist nur im Debug-Modus verfügbar!"));
            context.getSource().sendError(Component.literal("§7Aktiviere 'Blueprint Debugging' in der Config."));
            return 0;
        }
        
        try {
            BPViewerUtility instance = BPViewerUtility.getInstance();
            String currentFloor = instance.getActiveFloor();
            
            context.getSource().sendFeedback(Component.literal("§6=== Blueprint System Status ==="));
            context.getSource().sendFeedback(Component.literal("§7Aktueller Floor: " + (currentFloor != null ? "§a" + currentFloor : "§cUnbekannt")));
            context.getSource().sendFeedback(Component.literal("§7Sichtbar: " + (BPViewerUtility.isVisible() ? "§aJa" : "§cNein")));
            
            return 1;
        } catch (Exception e) {
            context.getSource().sendError(Component.literal("§cFehler beim Abrufen des Blueprint-Status: " + e.getMessage()));
            return 0;
        }
    }

    // =================== MATERIALS/RESOURCES COMMANDS ===================
    
    private static int resetMaterialsResources(CommandContext<FabricClientCommandSource> context) {
        try {
            resetMaterialsAndResourcesProgress();
            context.getSource().sendFeedback(Component.literal("§aMaterialien und Ressourcen erfolgreich gelöscht"));
            return 1;
        } catch (Exception e) {
            context.getSource().sendError(Component.literal("§cFehler beim Zurücksetzen: " + e.getMessage()));
            return 0;
        }
    }

    // =================== FRAGMENT DROP ANIMATION ===================

    /** Explains why the held item does or does not get a custom texture. */
    private static int debugCustomTexture(CommandContext<FabricClientCommandSource> context) {
        net.minecraft.world.entity.player.Player player = context.getSource().getPlayer();
        String info = net.felix.utilities.Overall.customtextures.CustomItemModelUtility.describe(player.getMainHandItem());
        context.getSource().sendFeedback(Component.literal("§7[Textur-Debug] §f" + info));
        return 1;
    }

    private static int testFragmentAnimation(CommandContext<FabricClientCommandSource> context, String charmKey) {
        // Without an argument a random charm is played.
        String played = net.felix.utilities.Overall.fragmentdrop.FragmentDropAnimationUtility.playTestAnimation(charmKey);
        if (played == null) {
            context.getSource().sendError(Component.literal("§cUnbekannter Anhänger: " + charmKey));
            return 0;
        }
        context.getSource().sendFeedback(Component.literal("§aTalismanfragment-Animation abgespielt: §f" + played));
        return 1;
    }


    // =================== LEADERBOARD COMMANDS ===================
    
    /**
     * Resettet alle Leaderboard-Scores
     */
    private static int resetAllScores(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Component.literal("§6Lösche alle Leaderboard-Scores..."));
        
        LeaderboardManager manager = LeaderboardManager.getInstance();
        if (!manager.isEnabled() || !manager.isRegistered()) {
            context.getSource().sendError(Component.literal("§cLeaderboard-System nicht verfügbar!"));
            return 0;
        }
        
        // Asynchroner Server-API-Aufruf
        CompletableFuture.runAsync(() -> {
            try {
                JsonObject response = manager.getHttpClient().deleteWithToken("/remove/all", manager.getPlayerToken());
                if (response != null && response.has("success") && response.get("success").getAsBoolean()) {
                    context.getSource().sendFeedback(Component.literal("§aAlle Leaderboard-Scores wurden gelöscht!"));
                    if (response.has("deleted")) {
                        int deleted = response.get("deleted").getAsInt();
                        context.getSource().sendFeedback(Component.literal("§7" + deleted + " Einträge entfernt"));
                    }
                } else {
                    String message = response != null && response.has("message") ? 
                        response.get("message").getAsString() : "Unbekannter Fehler";
                    context.getSource().sendError(Component.literal("§cFehler: " + message));
                }
            } catch (Exception e) {
                context.getSource().sendError(Component.literal("§cVerbindungsfehler: " + e.getMessage()));
            }
        });
        
        return 1;
    }
    
    /**
     * Resettet den Score eines bestimmten Leaderboards
     */
    private static int resetBoardScore(CommandContext<FabricClientCommandSource> context, String boardName) {
        context.getSource().sendFeedback(Component.literal("§6Lösche Score für " + boardName + "..."));
        
        LeaderboardManager manager = LeaderboardManager.getInstance();
        if (!manager.isEnabled() || !manager.isRegistered()) {
            context.getSource().sendError(Component.literal("§cLeaderboard-System nicht verfügbar!"));
            return 0;
        }
        
        // Asynchroner Server-API-Aufruf
        CompletableFuture.runAsync(() -> {
            try {
                JsonObject response = manager.getHttpClient().deleteWithToken("/remove/" + boardName, manager.getPlayerToken());
                if (response != null && response.has("success") && response.get("success").getAsBoolean()) {
                    context.getSource().sendFeedback(Component.literal("§aScore für " + boardName + " wurde gelöscht!"));
                } else {
                    String message = response != null && response.has("message") ? 
                        response.get("message").getAsString() : "Unbekannter Fehler";
                    context.getSource().sendError(Component.literal("§cFehler: " + message));
                }
            } catch (Exception e) {
                context.getSource().sendError(Component.literal("§cVerbindungsfehler: " + e.getMessage()));
            }
        });
        
        return 1;
    }
    
    /**
     * Resettet die Player-Stats des aktuellen Spielers (nur aktuelle Season)
     */
    private static int resetPlayerStats(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Component.literal("§6Lösche Player-Stats..."));
        
        LeaderboardManager manager = LeaderboardManager.getInstance();
        if (!manager.isEnabled() || !manager.isRegistered()) {
            context.getSource().sendError(Component.literal("§cStats-/Leaderboard-System nicht verfügbar!"));
            return 0;
        }
        
        // Asynchroner Server-API-Aufruf
        CompletableFuture.runAsync(() -> {
            try {
                JsonObject response = manager.getHttpClient().deleteWithToken("/profile/reset", manager.getPlayerToken());
                if (response != null && response.has("success") && response.get("success").getAsBoolean()) {
                    context.getSource().sendFeedback(Component.literal("§aPlayer-Stats wurden gelöscht!"));
                    if (response.has("deleted")) {
                        int deleted = response.get("deleted").getAsInt();
                        context.getSource().sendFeedback(Component.literal("§7" + deleted + " Einträge entfernt"));
                    }
                } else {
                    String message = response != null && response.has("message") ?
                        response.get("message").getAsString() : "Unbekannter Fehler";
                    context.getSource().sendError(Component.literal("§cFehler: " + message));
                }
            } catch (Exception e) {
                context.getSource().sendError(Component.literal("§cVerbindungsfehler: " + e.getMessage()));
            }
        });
        
        return 1;
    }
    
    /**
     * Zeigt den gemeinsamen Backend-/Authentifizierungszustand. Keine Tokenwerte.
     */
    private static int showConnectionStatus(CommandContext<FabricClientCommandSource> context) {
        LeaderboardManager manager = LeaderboardManager.getInstance();
        manager.printDiagnostics();
        var status = manager.connectionStatus();
        context.getSource().sendFeedback(Component.literal("§6=== CCLive-Verbindung ==="));
        context.getSource().sendFeedback(Component.literal("§7Verbindung: " + (status.trackingEnabled() ? "§abereit" : "§cnicht bereit")));
        context.getSource().sendFeedback(Component.literal("§7Registrierung: " + (status.registrationRunning() ? "§eläuft" : "§7inaktiv")));
        context.getSource().sendFeedback(Component.literal("§7Spieler: " + (status.playerName() != null ? "§a" + status.playerName() : "§cunbekannt")));
        context.getSource().sendFeedback(Component.literal("§7Identität: " + (status.playerUuid() != null ? "§a" + status.playerUuid() : "§cfehlt")));
        context.getSource().sendFeedback(Component.literal("§7Token: " + (status.hasToken() ? "§avorhanden" : "§cfehlt")));
        context.getSource().sendFeedback(Component.literal("§7WebSocket: " +
            (ChatManager.getInstance().isWebSocketConnected() ? "§averbunden" : "§cgetrennt")));
        context.getSource().sendFeedback(Component.literal("§7Nächste Wiederholung: §f" + LeaderboardManager.formatRetry(status.retryAfterMs())));
        return 1;
    }

    /**
     * Erneuert die Mod-Registrierung über die zentrale Operation.
     */
    private static int refreshConnection(CommandContext<FabricClientCommandSource> context) {
        if (!CCLiveUtilitiesConfig.isDataTrackingEnabled()) {
            context.getSource().sendError(Component.literal("§cDatentracking ist aus. CCLive-Verbindung wird nicht erneuert."));
            return 0;
        }
        var outcome = LeaderboardManager.getInstance().refreshRegistration();
        if (outcome.kind() == net.felix.Community.identity.PlayerRegistrationService.RefreshKind.ATTACHED) {
            context.getSource().sendFeedback(Component.literal("§eCCLive-Verbindung: Registrierung läuft bereits. Kein zweiter Versuch gestartet."));
            return 1;
        }
        if (outcome.kind() == net.felix.Community.identity.PlayerRegistrationService.RefreshKind.RATE_LIMITED) {
            context.getSource().sendFeedback(Component.literal("§eCCLive-Verbindung: Wartezeit nach Rate-Limit. Nächste Wiederholung "
                    + LeaderboardManager.formatRetry(outcome.retryAfterMs()) + "."));
            return 1;
        }
        context.getSource().sendFeedback(Component.literal("§6CCLive-Verbindung wird erneuert…"));
        return 1;
    }
    
    
    // =================== HELP COMMAND ===================
    
    // =================== DEVTOOLS ===================

    private static int openDevTools(CommandContext<FabricClientCommandSource> context) {
        if (!DevToolsAccess.isAllowed()) {
            context.getSource().sendError(Component.literal("§cKeine Berechtigung"));
            return 0;
        }
        net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
        if (client == null) {
            return 0;
        }
        client.execute(DevToolsScreen::open);
        return 1;
    }

    private static int openEventSounds(CommandContext<FabricClientCommandSource> context) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return 0;
        }
        client.execute(() -> client.gui.setScreen(new EventSoundListScreen(null)));
        return 1;
    }

    private static int stopEventSounds(CommandContext<FabricClientCommandSource> context) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return 0;
        }
        client.execute(EventSoundPlayer::stopAll);
        context.getSource().sendFeedback(Component.literal("§aLaufende Event-Sounds gestoppt."));
        return 1;
    }
    
    // =================== CHAT COMMANDS ===================
    
    /**
     * Setzt das Schreibziel (default oder cclive). Wird persistiert.
     * Bei beiden Chats an kann frei zwischen Default und CCLive gewechselt werden.
     */
    private static int setChatMode(CommandContext<FabricClientCommandSource> context, String mode) {
        ChatManager chatManager = ChatManager.getInstance();
        
        if (mode.equalsIgnoreCase("cclive")) {
            ChatManager.showDisabledNotice();
            return 0;
        } else if (mode.equalsIgnoreCase("default")) {
            if (!chatManager.isShowDefaultChat()) {
                context.getSource().sendError(Component.literal("§cDefault-Chat ist aus — erst mit /cclive chat toggle default aktivieren."));
                return 0;
            }
            chatManager.setChatMode(ChatManager.ChatMode.DEFAULT);
            context.getSource().sendFeedback(Component.literal("§aSchreiben: Default"));
        } else {
            context.getSource().sendError(Component.literal("§cUngültiger Modus! Verwende 'cclive' oder 'default'."));
            return 0;
        }
        
        return 1;
    }
    
    private static int submitReport(CommandContext<FabricClientCommandSource> context, String reason) {
        String player = StringArgumentType.getString(context, "player");
        net.felix.Community.chat.ChatReport.request(player, null, reason);
        return 1;
    }

    private static int openIgnoreList(CommandContext<FabricClientCommandSource> context) {
        net.felix.Community.chat.ignore.ChatIgnoreScreen.open();
        return 1;
    }

    private static int openChatRules(CommandContext<FabricClientCommandSource> context) {
        net.felix.Community.chat.rules.ChatRulesScreen.open();
        return 1;
    }

    private static int openMkOverview(CommandContext<FabricClientCommandSource> context) {
        net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
        if (client != null) {
            client.execute(() -> client.gui.setScreen(new net.felix.utilities.Factory.mklevel.ui.MkOverviewScreen()));
        }
        return 1;
    }

    /**
     * Togglet die Sichtbarkeit eines Chat-Kanals
     */
    private static int exportBlockedGlyphs(CommandContext<FabricClientCommandSource> context) {
        java.nio.file.Path path = net.felix.Community.chat.ChatBlockedGlyphs.writeExportFile();
        if (path == null) {
            context.getSource().sendError(Component.literal("§cGlyph-Liste konnte nicht geschrieben werden."));
            return 0;
        }
        context.getSource().sendFeedback(Component.literal(
            "§aGlyph-Sperrliste exportiert: §f" + path.toAbsolutePath()
                + " §7— Datei auf den Server als blocked-glyphs.json legen."));
        return 1;
    }

    private static int toggleChat(CommandContext<FabricClientCommandSource> context, String chatType) {
        ChatManager chatManager = ChatManager.getInstance();
        
        if (chatType.equalsIgnoreCase("cclive")) {
            ChatManager.showDisabledNotice();
            return 0;
        }
        if (!chatType.equalsIgnoreCase("default")) {
            context.getSource().sendError(Component.literal("§cUngültiger Chat-Typ! Verwende 'default'."));
            return 0;
        }
        
        chatManager.toggleChat(chatType);
        sendChatVisibilityOverview(context.getSource());
        return 1;
    }

    /** Übersicht über die Sichtbarkeit beider Chats. */
    private static void sendChatVisibilityOverview(FabricClientCommandSource source) {
        ChatManager chat = ChatManager.getInstance();
        String ccliveState = chat.isShowCCLiveChat() ? "§a§lAN" : "§c§lAUS";
        String defaultState = chat.isShowDefaultChat() ? "§a§lAN" : "§c§lAUS";
        String writeMode = chat.getEffectiveWriteMode() == ChatManager.ChatMode.CCLIVE ? "§dCCLive" : "§fDefault";

        source.sendFeedback(Component.literal("§6== Chats =="));
        source.sendFeedback(Component.literal("§7CCLive  " + ccliveState));
        source.sendFeedback(Component.literal("§7Default " + defaultState));
        source.sendFeedback(Component.literal("§7Schreiben: " + writeMode));
        source.sendFeedback(Component.literal("§6=========="));
    }
    
    // =================== HELP COMMAND ===================
    
    /**
     * Zeigt alle verfügbaren Commands
     */
    private static int showHelp(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Component.literal("§6=== CCLive-Utilities Commands ==="));
        context.getSource().sendFeedback(Component.literal(""));
        context.getSource().sendFeedback(Component.literal("§e§lBlueprint Commands:"));
        context.getSource().sendFeedback(Component.literal("§7/cclive bp reset §f- Alle Baupläne zurücksetzen"));
        context.getSource().sendFeedback(Component.literal("§7/cclive bp set <floor> §f- Floor manuell setzen"));
        context.getSource().sendFeedback(Component.literal(""));
        context.getSource().sendFeedback(Component.literal("§e§lCCLive-Verbindung:"));
        context.getSource().sendFeedback(Component.literal("§7/cclive connection status §f- Verbindungs- und Authentifizierungszustand"));
        context.getSource().sendFeedback(Component.literal("§7/cclive connection refresh §f- Registrierung erneuern"));
        context.getSource().sendFeedback(Component.literal(""));
        context.getSource().sendFeedback(Component.literal("§e§lLeaderboard Commands:"));
        context.getSource().sendFeedback(Component.literal("§7/cclive lb §f- Rangliste steht links im eigenen Inventar"));
        context.getSource().sendFeedback(Component.literal("§7/cclive lb reset all §f- Alle Scores löschen"));
        context.getSource().sendFeedback(Component.literal("§7/cclive lb reset <board> §f- Spezifischen Score löschen"));
        context.getSource().sendFeedback(Component.literal("§7  Verfügbare Boards: current_coins, alltime_kills, playtime,"));
        context.getSource().sendFeedback(Component.literal("§7  all_collections, *_collection (oak, sulfur, mushroom, etc.),"));
        context.getSource().sendFeedback(Component.literal("§7  floor_1 bis floor_" + MAX_FLOOR));
        context.getSource().sendFeedback(Component.literal("§7  Tab-Completion verfügbar!"));
        context.getSource().sendFeedback(Component.literal(""));
        context.getSource().sendFeedback(Component.literal("§e§lChat Commands:"));
        context.getSource().sendFeedback(Component.literal("§c" + ChatManager.DISABLED_LINE));
        context.getSource().sendFeedback(Component.literal("§e" + ChatManager.DISABLED_FOLLOWUP));
        context.getSource().sendFeedback(Component.literal("§7@global <nachricht> §f- Direkt in den Minecraft-Chat schreiben"));
        context.getSource().sendFeedback(Component.literal("§7@lp … §f- Immer in den globalen Minecraft-Chat"));
        context.getSource().sendFeedback(Component.literal(""));
        if (StaffClientAccess.isStaffCommandVisible()) {
            context.getSource().sendFeedback(Component.literal("§e§lModerationstest (Etappe 3, temporär):"));
            context.getSource().sendFeedback(Component.literal("§7/cclive mod §f- Testfenster"));
            context.getSource().sendFeedback(Component.literal("§7/cclive mod delete <id> <grund>"));
            context.getSource().sendFeedback(Component.literal("§7/cclive mod mute|ban <name> <dauer> <grund>"));
            context.getSource().sendFeedback(Component.literal("§7Dauer z. B. 10m, 5m, 30m, 2h, 14d. Höchstens 366 Tage."));
            context.getSource().sendFeedback(Component.literal("§7Permanent: /cclive mod mute|ban <name> permanent confirm <grund>"));
            context.getSource().sendFeedback(Component.literal("§7/cclive mod unmute|unban <name> <grund>"));
            context.getSource().sendFeedback(Component.literal(""));
        }
        if (StaffClientAccess.isAdminCommandVisible()) {
            context.getSource().sendFeedback(Component.literal("§e§lBerechtigungen:"));
            context.getSource().sendFeedback(Component.literal("§7/cclive perms user list §f- Moderatorliste öffnen"));
            context.getSource().sendFeedback(Component.literal("§7/cclive perms user add <name> §f- Moderator hinzufügen"));
            context.getSource().sendFeedback(Component.literal("§7/cclive perms user remove <name> §f- Moderator entfernen"));
            context.getSource().sendFeedback(Component.literal(""));
        }

        context.getSource().sendFeedback(Component.literal("§e§lSchmied:"));
        context.getSource().sendFeedback(Component.literal("§7/cclive eventsound open §f- Event-Sound-Menü öffnen"));
        context.getSource().sendFeedback(Component.literal("§7/cclive eventsound stop §f- Laufende Event-Sounds stoppen"));
        context.getSource().sendFeedback(Component.literal("§7/cclive fragment_animation_test [anhänger] §f- Talismanfragment-Animation testen"));
        context.getSource().sendFeedback(Component.literal(""));
        
        // Debug Commands nur anzeigen wenn Debug aktiviert ist
        if (CCLiveUtilitiesConfig.HANDLER.instance().blueprintDebugging) {
            context.getSource().sendFeedback(Component.literal("§e§lBlueprint Debug Commands:"));
            context.getSource().sendFeedback(Component.literal("§7/cclive bp status §f- Blueprint-System Status"));
            context.getSource().sendFeedback(Component.literal(""));
        }
        
        if (CCLiveUtilitiesConfig.HANDLER.instance().leaderboardDebugging) {
            context.getSource().sendFeedback(Component.literal("§e§lLeaderboard Debug:"));
            context.getSource().sendFeedback(Component.literal("§7Zusätzliche Debug-Logs in der Konsole"));
            context.getSource().sendFeedback(Component.literal(""));
        }
        
        context.getSource().sendFeedback(Component.literal("§7/cclive config reset §f- Config löschen und mit Standardwerten neu erstellen"));
        context.getSource().sendFeedback(Component.literal("§7/cclive profil login <CODE> §f- Website-Profil bestätigen"));
        context.getSource().sendFeedback(Component.literal("§7/cclive help §f- Diese Hilfe anzeigen"));
        context.getSource().sendFeedback(Component.literal(""));
        context.getSource().sendFeedback(Component.literal("§7Debug-Commands sind nur sichtbar wenn Debug-Modi in der Config aktiviert sind."));
        
        return 1;
    }
    
    // =================== TAB COMPLETION ===================
    
    /**
     * Suggestion Provider für Chat-Modi (default/cclive)
     */
    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<FabricClientCommandSource, String> sanctionName(String action, boolean mute) {
        return argument("name", StringArgumentType.word())
            .suggests(StaffNameSuggestions.verifiedPlayers())
            .then(literal("permanent")
                .executes(context -> {
                    context.getSource().sendError(Component.literal(
                        "§cPermanent nur so: /cclive mod " + action + " <name> permanent confirm <grund>"));
                    return 0;
                })
                .then(literal("confirm")
                    .then(argument("reason", StringArgumentType.greedyString())
                        .executes(context -> runPermanentSanction(context, mute)))))
            .then(argument("duration", TimedDurationArgument.timedDuration())
                .suggests(durationSuggestions())
                .then(argument("reason", StringArgumentType.greedyString())
                    .executes(context -> runSanction(context, mute))));
    }

    private static int runPermanentSanction(CommandContext<FabricClientCommandSource> context, boolean mute) {
        String name = StringArgumentType.getString(context, "name");
        String reason = StringArgumentType.getString(context, "reason");
        String trimmed = reason == null ? "" : reason.strip();
        if (trimmed.length() < 3) {
            context.getSource().sendError(Component.literal("§cBegründung ist Pflicht."));
            return 0;
        }
        return mute
            ? StaffModerationCommands.mute(context, name, "permanent", true, trimmed)
            : StaffModerationCommands.ban(context, name, "permanent", true, trimmed);
    }

    private static int runSanction(CommandContext<FabricClientCommandSource> context, boolean mute) {
        String name = StringArgumentType.getString(context, "name");
        String duration = StringArgumentType.getString(context, "duration");
        String reason = StringArgumentType.getString(context, "reason");
        String trimmed = reason == null ? "" : reason.strip();
        boolean permanent = "permanent".equalsIgnoreCase(duration);
        String action = mute ? "mute" : "ban";
        if (startsWithToken(trimmed, "confirm")) {
            if (!permanent) {
                context.getSource().sendError(Component.literal("§cconfirm ist nur bei einer permanenten Sanktion nötig."));
                return 0;
            }
            String rest = trimmed.substring(7).strip();
            if (rest.length() < 3) {
                context.getSource().sendError(Component.literal("§cBegründung ist Pflicht."));
                return 0;
            }
            return mute
                ? StaffModerationCommands.mute(context, name, duration, true, rest)
                : StaffModerationCommands.ban(context, name, duration, true, rest);
        }
        if (permanent) {
            context.getSource().sendError(Component.literal(
                "§cPermanent nur so: /cclive mod " + action + " <name> permanent confirm <grund>"));
            return 0;
        }
        if (trimmed.length() < 3) {
            context.getSource().sendError(Component.literal("§cBegründung ist Pflicht."));
            return 0;
        }
        return mute
            ? StaffModerationCommands.mute(context, name, duration, false, trimmed)
            : StaffModerationCommands.ban(context, name, duration, false, trimmed);
    }

    private static boolean startsWithToken(String value, String token) {
        if (value.length() < token.length()) {
            return false;
        }
        if (!value.regionMatches(true, 0, token, 0, token.length())) {
            return false;
        }
        return value.length() == token.length() || Character.isWhitespace(value.charAt(token.length()));
    }

    private static int parseMessageId(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (Exception e) {
            return 0;
        }
    }

    private static SuggestionProvider<FabricClientCommandSource> durationSuggestions() {
        return (context, builder) -> {
            String input = builder.getRemaining().toLowerCase();
            for (String value : new String[] { "10m", "1h", "24h", "7d" }) {
                if (value.startsWith(input)) {
                    builder.suggest(value);
                }
            }
            return builder.buildFuture();
        };
    }

    static final class TimedDurationArgument implements ArgumentType<String> {
        private TimedDurationArgument() {
        }

        static TimedDurationArgument timedDuration() {
            return new TimedDurationArgument();
        }

        @Override
        public String parse(StringReader reader) throws CommandSyntaxException {
            int start = reader.getCursor();
            String word = reader.readUnquotedString();
            if (word.equalsIgnoreCase("permanent")) {
                reader.setCursor(start);
                throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.literalIncorrect().createWithContext(reader, "permanent");
            }
            if (word.isEmpty()) {
                throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerExpectedInt().createWithContext(reader);
            }
            return word;
        }

        @Override
        public java.util.Collection<String> getExamples() {
            return java.util.List.of("10m", "5m", "2h", "14d");
        }
    }

    private static SuggestionProvider<FabricClientCommandSource> chatModeSuggestions() {
        return (context, builder) -> {
            String input = builder.getRemaining().toLowerCase();
            if ("default".startsWith(input)) {
                builder.suggest("default");
            }
            return builder.buildFuture();
        };
    }
    
    private static SuggestionProvider<FabricClientCommandSource> chatTypeSuggestions() {
        return chatModeSuggestions();
    }
    
    private static LiteralArgumentBuilder<FabricClientCommandSource> leaderboardResetCommand() {
        // Nur ein String-Argument + Suggestions – keine parallelen Literale (sonst Brigadier-Ambiguity-Spam)
        return literal("reset")
                .then(argument("board", StringArgumentType.word())
                        .suggests(leaderboardSuggestions())
                        .executes(context -> {
                            String board = StringArgumentType.getString(context, "board");
                            if ("all".equalsIgnoreCase(board)) {
                                return resetAllScores(context);
                            }
                            return resetBoardScore(context, board);
                        }));
    }

    /**
     * Suggestion Provider für Leaderboard-Namen
     */
    private static SuggestionProvider<FabricClientCommandSource> leaderboardSuggestions() {
        return (context, builder) -> {
            String input = builder.getRemaining().toLowerCase();
            
            String[] leaderboards = {
                "all",
                "current_coins",
                "alltime_kills",
                "playtime",
                "all_collections",
                "oak_collection",
                "sulfur_collection",
                "mushroom_collection",
                "spruce_collection",
                "dark_oak_collection",
                "mangrove_collection",
                "jungle_collection",
                "bamboo_collection",
                "raw_gold_collection",
                "coal_collection",
                "crimson_collection",
                "raw_copper_collection",
                "quartz_collection",
                "warped_collection",
                "diamond_collection",
                "ancient_debris_collection",
                "echo_collection",
                "raw_iron_collection",
                "obsidian_collection",
            };
            
            for (String board : leaderboards) {
                if (board.toLowerCase().startsWith(input)) {
                    builder.suggest(board);
                }
            }

            for (int floor = 1; floor <= MAX_FLOOR; floor++) {
                String board = "floor_" + floor;
                if (board.startsWith(input)) {
                    builder.suggest(board);
                }
            }
            
            return builder.buildFuture();
        };
    }
}
