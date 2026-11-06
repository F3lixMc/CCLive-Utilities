package net.felix.staff;

import com.google.gson.JsonObject;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.felix.leaderboards.http.HttpClient;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Temporäre Ingame-Testbefehle für Etappe 3 ({@code /cclive mod ...}).
 * Später durch das Rechtsklick-Kontextmenü (Etappe 4) ablösen.
 */
public final class StaffModerationCommands {
    private static final AtomicBoolean inFlight = new AtomicBoolean(false);

    private StaffModerationCommands() {
    }

    public static int openScreen(CommandContext<FabricClientCommandSource> context) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return 0;
        client.execute(StaffActionScreen::open);
        return 1;
    }

    public static int delete(CommandContext<FabricClientCommandSource> context, int messageId, String reason) {
        JsonObject body = new JsonObject();
        body.addProperty("messageId", messageId);
        body.addProperty("reason", reason);
        return post(context, "/moderation/actions/message/delete", body, "Nachricht wird ersetzt …");
    }

    public static int mute(CommandContext<FabricClientCommandSource> context, String name, String duration, boolean confirm, String reason) {
        return sanction(context, "/moderation/actions/mute", name, duration, confirm, reason);
    }

    public static int ban(CommandContext<FabricClientCommandSource> context, String name, String duration, boolean confirm, String reason) {
        return sanction(context, "/moderation/actions/ban", name, duration, confirm, reason);
    }

    public static int unmute(CommandContext<FabricClientCommandSource> context, String name, String reason) {
        JsonObject body = new JsonObject();
        body.addProperty("player", name);
        body.addProperty("reason", reason);
        return post(context, "/moderation/actions/unmute", body, "Mute wird aufgehoben …");
    }

    public static int unban(CommandContext<FabricClientCommandSource> context, String name, String reason) {
        JsonObject body = new JsonObject();
        body.addProperty("player", name);
        body.addProperty("reason", reason);
        return post(context, "/moderation/actions/unban", body, "Chatbann wird aufgehoben …");
    }

    private static int sanction(
            CommandContext<FabricClientCommandSource> context,
            String path,
            String name,
            String duration,
            boolean confirm,
            String reason
    ) {
        JsonObject body = new JsonObject();
        body.addProperty("player", name);
        body.addProperty("duration", duration);
        body.addProperty("reason", reason);
        body.addProperty("confirm", confirm);
        return post(context, path, body, "Sanktion wird gesetzt …");
    }

    private static int post(
            CommandContext<FabricClientCommandSource> context,
            String path,
            JsonObject body,
            String started
    ) {
        if (!inFlight.compareAndSet(false, true)) {
            context.getSource().sendError(Component.literal("§cBitte warte, bis die vorherige Anfrage fertig ist."));
            return 0;
        }
        FabricClientCommandSource source = context.getSource();
        source.sendFeedback(Component.literal("§e" + started));
        long epoch = StaffClientAccess.epoch();
        String token = StaffClientAccess.token();
        CompletableFuture.runAsync(() -> {
            try {
                StaffClientAccess.refresh();
                if (!StaffClientAccess.isCurrent(epoch, token) || !StaffClientAccess.isStaffCommandVisible()) {
                    reply(source, epoch, token, () -> source.sendError(Component.literal("§cKeine Berechtigung.")));
                    return;
                }
                HttpClient http = StaffClientAccess.http();
                String current = StaffClientAccess.token();
                if (http == null || current == null) {
                    reply(source, epoch, token, () -> source.sendError(Component.literal("§cKeine gültige Mod-Verbindung.")));
                    return;
                }
                HttpClient.JsonResponse response = http.postResultWithToken(path, body, current);
                reply(source, epoch, token, () -> {
                    if (response.status() == 401 || response.status() == 403) {
                        StaffClientAccess.invalidate();
                    }
                    if (response.status() != 200) {
                        source.sendError(Component.literal("§c" + StaffClientAccess.publicError(response, "Aktion fehlgeschlagen.")));
                        return;
                    }
                    if (response.status() == 200) {
                        StaffNameSuggestions.invalidateSanctions();
                    }
                    if (StaffClientAccess.sanctionStoredWithoutLiveUpdate(response.body())) {
                        source.sendFeedback(Component.literal("§e" + StaffClientAccess.sanctionStoredWarning(response.body())));
                        return;
                    }
                    source.sendFeedback(Component.literal("§aAktion übernommen."));
                });
            } catch (Exception e) {
                reply(source, epoch, token, () -> source.sendError(Component.literal("§cServer nicht erreichbar.")));
            } finally {
                Minecraft client = Minecraft.getInstance();
                if (client != null) client.execute(() -> inFlight.set(false));
                else inFlight.set(false);
            }
        });
        return 1;
    }

    private static void reply(FabricClientCommandSource source, long epoch, String token, Runnable action) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return;
        client.execute(() -> {
            if (!StaffClientAccess.isCurrent(epoch, token)) return;
            action.run();
        });
    }
}
