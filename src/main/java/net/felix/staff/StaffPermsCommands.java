package net.felix.staff;

import com.google.gson.JsonObject;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.felix.leaderboards.http.HttpClient;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public final class StaffPermsCommands {
    private static final AtomicBoolean inFlight = new AtomicBoolean(false);

    private StaffPermsCommands() {
    }

    public static int list(CommandContext<FabricClientCommandSource> context) {
        return runStaffRequest(context, null, (http, token, reply) -> {
            Minecraft client = Minecraft.getInstance();
            if (client == null) return;
            client.execute(() -> {
                if (!reply.stillCurrent()) return;
                StaffModeratorListScreen.open();
            });
        });
    }

    public static int add(CommandContext<FabricClientCommandSource> context, String name) {
        return runStaffRequest(context, "Moderator wird hinzugefügt …", (http, token, reply) -> {
            JsonObject body = new JsonObject();
            body.addProperty("player", name);
            HttpClient.JsonResponse response = http.postResultWithToken("/moderation/staff/moderators", body, token);
            reply.onClient(() -> applyMutation(response, reply.source(), true));
        });
    }

    public static int remove(CommandContext<FabricClientCommandSource> context, String name) {
        return runStaffRequest(context, "Moderator wird entfernt …", (http, token, reply) -> {
            String path = "/moderation/staff/moderators?player="
                    + java.net.URLEncoder.encode(name, java.nio.charset.StandardCharsets.UTF_8);
            HttpClient.JsonResponse response = http.deleteResultWithToken(path, token);
            reply.onClient(() -> applyMutation(response, reply.source(), false));
        });
    }

    private static int runStaffRequest(
            CommandContext<FabricClientCommandSource> context,
            String started,
            StaffWork work
    ) {
        if (!inFlight.compareAndSet(false, true)) {
            context.getSource().sendError(Component.literal("§cBitte warte, bis die vorherige Anfrage fertig ist."));
            return 0;
        }
        FabricClientCommandSource source = context.getSource();
        if (started != null) {
            source.sendFeedback(Component.literal("§e" + started));
        }
        long capturedEpoch = StaffClientAccess.epoch();
        String capturedToken = StaffClientAccess.token();
        Reply reply = new Reply(source, capturedEpoch, capturedToken);
        CompletableFuture.runAsync(() -> {
            try {
                if (!StaffClientAccess.isAdminCommandVisible()) {
                    StaffClientAccess.refresh();
                }
                if (!reply.stillCurrent()) return;
                if (!StaffClientAccess.isAdminCommandVisible()) {
                    reply.onClient(() -> source.sendError(Component.literal("§cKeine Berechtigung.")));
                    return;
                }
                HttpClient http = StaffClientAccess.http();
                String token = StaffClientAccess.token();
                if (http == null || token == null || token.isBlank()) {
                    reply.onClient(() -> source.sendError(Component.literal("§cKeine gültige Mod-Verbindung.")));
                    return;
                }
                if (!reply.stillCurrent()) return;
                work.run(http, token, reply);
            } catch (Exception e) {
                reply.onClient(() -> source.sendError(Component.literal("§cServer nicht erreichbar.")));
            } finally {
                Minecraft client = Minecraft.getInstance();
                if (client != null) client.execute(() -> inFlight.set(false));
                else inFlight.set(false);
            }
        });
        return 1;
    }

    private static void applyMutation(HttpClient.JsonResponse response, FabricClientCommandSource source, boolean adding) {
        if (response.status() == 401 || response.status() == 403) {
            StaffClientAccess.invalidate();
            source.sendError(Component.literal("§c" + StaffClientAccess.publicError(response, "Keine Berechtigung.")));
            return;
        }
        if (response.status() != 200) {
            source.sendError(Component.literal("§c" + StaffClientAccess.publicError(
                    response, adding ? "Hinzufügen fehlgeschlagen." : "Entfernen fehlgeschlagen.")));
            return;
        }
        String player = StaffClientAccess.playerFrom(response.body());
        boolean already = response.body() != null && response.body().has("already") && response.body().get("already").getAsBoolean();
        if (adding) {
            source.sendFeedback(Component.literal(already
                    ? "§e" + player + " ist bereits Moderator."
                    : "§a" + player + " ist jetzt Moderator."));
        } else {
            source.sendFeedback(Component.literal(already
                    ? "§e" + player + " war kein Moderator."
                    : "§a" + player + " ist kein Moderator mehr."));
        }
    }

    @FunctionalInterface
    private interface StaffWork {
        void run(HttpClient http, String token, Reply reply) throws Exception;
    }

    private record Reply(FabricClientCommandSource source, long epoch, String token) {
        boolean stillCurrent() {
            return StaffClientAccess.isCurrent(epoch, token);
        }

        void onClient(Runnable action) {
            Minecraft client = Minecraft.getInstance();
            if (client == null) return;
            client.execute(() -> {
                if (!stillCurrent()) return;
                action.run();
            });
        }
    }
}
