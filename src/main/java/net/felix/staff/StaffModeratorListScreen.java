package net.felix.staff;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.felix.leaderboards.http.HttpClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class StaffModeratorListScreen extends Screen {
    private final List<String> lines = new ArrayList<>();
    private final long capturedEpoch;
    private final String capturedToken;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private String status = "Lade Moderatoren …";

    public StaffModeratorListScreen() {
        super(Component.literal("Moderatoren"));
        this.capturedEpoch = StaffClientAccess.epoch();
        this.capturedToken = StaffClientAccess.token();
    }

    public static void open() {
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            client.execute(() -> client.gui.setScreen(new StaffModeratorListScreen()));
        }
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("Schließen"), button -> onClose())
                .bounds(this.width / 2 - 50, this.height - 36, 100, 20)
                .build());
        refresh();
    }

    @Override
    public void onClose() {
        closed.set(true);
        super.onClose();
    }

    private boolean stillOpen() {
        if (closed.get()) return false;
        if (!StaffClientAccess.isCurrent(capturedEpoch, capturedToken)) return false;
        Minecraft client = Minecraft.getInstance();
        return client != null && client.gui.screen() == this;
    }

    private void refresh() {
        HttpClient http = StaffClientAccess.http();
        String token = capturedToken;
        if (http == null || token == null || token.isBlank()) {
            status = "Keine gültige Mod-Verbindung.";
            return;
        }
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                HttpClient.JsonResponse response = http.getResultWithToken("/moderation/staff/moderators", token);
                Minecraft client = Minecraft.getInstance();
                if (client == null) return;
                client.execute(() -> apply(response));
            } catch (Exception e) {
                Minecraft client = Minecraft.getInstance();
                if (client == null) return;
                client.execute(() -> {
                    if (!stillOpen()) return;
                    status = "Liste konnte nicht geladen werden.";
                    lines.clear();
                });
            }
        });
    }

    private void apply(HttpClient.JsonResponse response) {
        if (!stillOpen()) return;
        if (response.status() == 401 || response.status() == 403) {
            StaffClientAccess.invalidate();
            status = StaffClientAccess.publicError(response, "Keine Berechtigung.");
            Minecraft client = Minecraft.getInstance();
            if (client != null && client.gui.screen() == this) {
                client.gui.setScreen(null);
            }
            return;
        }
        if (response.status() != 200 || response.body() == null) {
            status = StaffClientAccess.publicError(response, "Liste konnte nicht geladen werden.");
            return;
        }
        lines.clear();
        JsonArray rows = response.body().getAsJsonArray("moderators");
        int online = response.body().has("online") ? response.body().get("online").getAsInt() : 0;
        int offline = response.body().has("offline") ? response.body().get("offline").getAsInt() : 0;
        status = "Moderatoren · online " + online + " · offline " + offline;
        if (rows != null) {
            for (JsonElement element : rows) {
                if (!element.isJsonObject()) continue;
                JsonObject row = element.getAsJsonObject();
                String name = row.has("player") ? row.get("player").getAsString() : "?";
                boolean isOnline = row.has("online") && row.get("online").getAsBoolean();
                lines.add(name + "  " + (isOnline ? "online" : "offline"));
            }
        }
        if (lines.isEmpty()) {
            lines.add("Keine Moderatoren eingetragen.");
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xC0101010);
        int boxWidth = Math.min(420, this.width - 40);
        int boxHeight = Math.min(280, this.height - 80);
        int x = (this.width - boxWidth) / 2;
        int y = (this.height - boxHeight) / 2;
        context.fill(x, y, x + boxWidth, y + boxHeight, 0xFF111111);
        context.outline(x, y, boxWidth, boxHeight, 0xFFFFFFFF);
        context.text(this.font, this.status, x + 12, y + 12, 0xFFFFFF00, false);
        int lineY = y + 36;
        for (String line : lines) {
            if (lineY > y + boxHeight - 20) break;
            context.text(this.font, line, x + 12, lineY, 0xFFFFFFFF, false);
            lineY += 12;
        }
        super.extractRenderState(context, mouseX, mouseY, delta);
    }
}
