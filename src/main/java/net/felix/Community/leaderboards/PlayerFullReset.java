package net.felix.Community.leaderboards;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.felix.CCLiveUtilities;
import net.felix.CCLiveUtilitiesConfig;
import net.felix.Community.chat.ChatIgnoreList;
import net.felix.Community.leaderboards.collectors.FloorKillsCollector;
import net.felix.Community.profile.ProfileStatsManager;
import net.felix.utilities.Farmworld.fishing.found.LocalSeasonProgressReset;
import net.felix.utilities.Other.glyphs.ZeichenUtility;
import net.minecraft.client.Minecraft;

/**
 * Klassenwahl setzt den lokalen Season-Fortschritt zurück, ohne die globale Season zu ändern.
 * Der Serverauftrag wird vor dem Löschen gesichert und bis zur Bestätigung wiederholt.
 */
public final class PlayerFullReset {
    private static final Gson GSON = new Gson();
    private static final PlayerFullResetLedger LEDGER = new PlayerFullResetLedger();
    private static final AtomicBoolean SENDING = new AtomicBoolean(false);
    private static int retryTicks;
    private static boolean initialized;

    private PlayerFullReset() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        load();
        for (PlayerFullResetLedger.Order order : LEDGER.pendingLocal()) {
            clearLocal(order.account(), order.seasonId(), order.preseason());
            LEDGER.markLocalCleared(order.id());
            save();
        }
        ClientTickEvents.END_CLIENT_TICK.register(PlayerFullReset::onTick);
    }

    public static boolean blocks(String account, int seasonId, boolean preseason) {
        return account != null && LEDGER.blocks(account, seasonId, preseason);
    }

    public static boolean blocksCurrentProgress() {
        String account = ChatIgnoreList.localAccountId();
        Integer seasonId = SeasonChangeGate.confirmedSeasonId();
        if (account == null || seasonId == null) {
            return false;
        }
        return blocks(account, seasonId, CCLiveUtilitiesConfig.HANDLER.instance().lastProcessedPreseason);
    }

    public static String currentUploadResetId() {
        String account = ChatIgnoreList.localAccountId();
        Integer seasonId = SeasonChangeGate.confirmedSeasonId();
        if (account == null || seasonId == null) {
            return null;
        }
        return LEDGER.uploadId(account, seasonId, CCLiveUtilitiesConfig.HANDLER.instance().lastProcessedPreseason);
    }

    public static void attachResetId(JsonObject body, int seasonId, boolean preseason) {
        if (body == null) {
            return;
        }
        String account = ChatIgnoreList.localAccountId();
        String resetId = account == null ? null : LEDGER.uploadId(account, seasonId, preseason);
        if (resetId != null) {
            body.addProperty("resetId", resetId);
        }
    }

    private static void onTick(Minecraft client) {
        String title = "";
        if (client != null && client.gui.screen() != null && client.gui.screen().getTitle() != null) {
            title = client.gui.screen().getTitle().getString();
        }
        if (LEDGER.opening(ZeichenUtility.containsClassSelectionUi(title))) {
            begin();
        }
        retryTicks++;
        if (retryTicks >= 100) {
            retryTicks = 0;
            dispatch();
        }
    }

    private static void begin() {
        String account = ChatIgnoreList.localAccountId();
        Integer seasonId = SeasonChangeGate.confirmedSeasonId();
        boolean preseason = CCLiveUtilitiesConfig.HANDLER.instance().lastProcessedPreseason;
        PlayerFullResetLedger.Order order = LEDGER.stage(UUID.randomUUID().toString(), account, seasonId, preseason);
        if (order != null && !save()) {
            LEDGER.drop(order.id());
            LEDGER.reopen();
            return;
        }
        clearLocal(account, seasonId, preseason);
        if (order != null) {
            LEDGER.markLocalCleared(order.id());
            save();
        }
        retryTicks = 100;
    }

    private static void clearLocal(String account, Integer seasonId, boolean preseason) {
        LocalSeasonProgressReset.resetAllSixProgressFiles();
        LeaderboardManager.getInstance().discardSeasonComparisonState();
        ProfileStatsManager.getInstance().discardLocalHighsAfterPlayerReset();
        if (account == null || seasonId == null || seasonId < 0) {
            return;
        }
        SmithSkillUpload.getInstance().forget(account, seasonId, preseason);
        FactoryLevelUpload.getInstance().forget(account, seasonId, preseason);
        FloorKillsCollector.dropStoredPhase(account, seasonId, preseason);
    }

    private static void dispatch() {
        if (!SENDING.compareAndSet(false, true)) {
            return;
        }
        var pending = LEDGER.pendingServer();
        if (pending.isEmpty()) {
            SENDING.set(false);
            return;
        }
        LeaderboardManager manager = LeaderboardManager.getInstance();
        if (!manager.isRegistered() || manager.getPlayerToken() == null || !CCLiveUtilitiesConfig.isDataTrackingEnabled()) {
            SENDING.set(false);
            return;
        }
        PlayerFullResetLedger.Order order = pending.get(0);
        String token = manager.getPlayerToken();
        CompletableFuture.runAsync(() -> {
            try {
                JsonObject body = new JsonObject();
                body.addProperty("resetId", order.id());
                body.addProperty("seasonId", order.seasonId());
                body.addProperty("isPreseason", order.preseason());
                JsonObject response = manager.getHttpClient().postWithToken("/profile/player-reset", body, token);
                if (response != null && response.has("success") && response.get("success").getAsBoolean()) {
                    Minecraft.getInstance().execute(() -> {
                        LEDGER.markServerApplied(order.id());
                        save();
                    });
                }
            } catch (Exception ignored) {
            } finally {
                SENDING.set(false);
            }
        });
    }

    private static void load() {
        Path file = file();
        if (!Files.isRegularFile(file)) {
            return;
        }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            LEDGER.copyFrom(PlayerFullResetLedger.fromJson(root));
        } catch (Exception ignored) {
        }
    }

    private static boolean save() {
        try {
            Path file = file();
            Files.createDirectories(file.getParent());
            Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(temporary, GSON.toJson(LEDGER.toJson()), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomic) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static Path file() {
        return CCLiveUtilities.getConfigDir().resolve("cclive-utilities").resolve("player-full-reset.json");
    }
}
