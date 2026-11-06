package net.felix.staff;

import com.google.gson.JsonObject;
import net.felix.leaderboards.http.HttpClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.concurrent.atomic.AtomicBoolean;

public class StaffActionScreen extends Screen {
    private final long capturedEpoch;
    private final String capturedToken;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private EditBox messageId;
    private EditBox playerName;
    private EditBox duration;
    private EditBox reason;
    private Button permanentButton;
    private boolean confirmPermanent;
    private String status = "Etappe-3-Test. Kein Rechtsklick-Menü.";

    public StaffActionScreen() {
        super(Component.literal("CCLive Moderation"));
        this.capturedEpoch = StaffClientAccess.epoch();
        this.capturedToken = StaffClientAccess.token();
    }

    public static void open() {
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            client.execute(() -> client.gui.setScreen(new StaffActionScreen()));
        }
    }

    @Override
    protected void init() {
        super.init();
        int x = this.width / 2 - 140;
        int y = 40;
        messageId = field(x, y, "Nachrichten-ID");
        playerName = field(x, y + 24, "Spieler");
        duration = field(x, y + 48, "Dauer z. B. 5m, 2h, 14d, permanent");
        reason = field(x, y + 72, "Begründung");
        addRenderableWidget(Button.builder(Component.literal("Löschen"), b -> post(
                "/moderation/actions/message/delete", deleteBody()))
                .bounds(x, y + 104, 90, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Mute"), b -> post(
                "/moderation/actions/mute", sanctionBody()))
                .bounds(x + 96, y + 104, 90, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Bann"), b -> post(
                "/moderation/actions/ban", sanctionBody()))
                .bounds(x + 192, y + 104, 90, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Entmuten"), b -> post(
                "/moderation/actions/unmute", liftBody()))
                .bounds(x, y + 128, 90, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Entbannen"), b -> post(
                "/moderation/actions/unban", liftBody()))
                .bounds(x + 96, y + 128, 90, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Schließen"), b -> onClose())
                .bounds(x + 192, y + 128, 90, 20).build());
        permanentButton = Button.builder(Component.literal(permanentLabel()), b -> {
            confirmPermanent = !confirmPermanent;
            b.setMessage(Component.literal(permanentLabel()));
        }).bounds(x, y + 152, 136, 20).build();
        addRenderableWidget(permanentButton);
        addRenderableWidget(Button.builder(Component.literal("Leeren"), b -> clearInputs())
                .bounds(x + 144, y + 152, 136, 20).build());
    }

    private String permanentLabel() {
        return confirmPermanent ? "Permanent: an" : "Permanent: aus";
    }

    private void clearInputs() {
        if (messageId != null) messageId.setValue("");
        if (playerName != null) playerName.setValue("");
        if (duration != null) duration.setValue("");
        if (reason != null) reason.setValue("");
        confirmPermanent = false;
        if (permanentButton != null) {
            permanentButton.setMessage(Component.literal(permanentLabel()));
        }
    }

    private EditBox field(int x, int y, String hint) {
        EditBox box = new EditBox(this.font, x, y, 280, 20, Component.literal(hint));
        box.setHint(Component.literal(hint));
        box.setMaxLength(400);
        addRenderableWidget(box);
        return box;
    }

    private JsonObject deleteBody() {
        JsonObject body = new JsonObject();
        try {
            body.addProperty("messageId", Integer.parseInt(messageId.getValue().trim()));
        } catch (Exception e) {
            body.addProperty("messageId", 0);
        }
        body.addProperty("reason", reason.getValue());
        return body;
    }

    private JsonObject sanctionBody() {
        JsonObject body = liftBody();
        String value = duration.getValue().trim();
        body.addProperty("duration", value);
        body.addProperty("confirm", confirmPermanent);
        String message = messageId.getValue() == null ? "" : messageId.getValue().trim();
        if (!message.isEmpty()) {
            try {
                body.addProperty("messageId", Integer.parseInt(message));
            } catch (NumberFormatException ignored) {
                body.addProperty("messageId", -1);
            }
        }
        return body;
    }

    private JsonObject liftBody() {
        JsonObject body = new JsonObject();
        body.addProperty("player", playerName.getValue().trim());
        body.addProperty("reason", reason.getValue());
        return body;
    }

    private void post(String path, JsonObject body) {
        status = "Sende …";
        HttpClient http = StaffClientAccess.http();
        String token = capturedToken;
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                if (http == null || token == null) {
                    setStatus("Keine gültige Mod-Verbindung.");
                    return;
                }
                HttpClient.JsonResponse response = http.postResultWithToken(path, body, token);
                Minecraft client = Minecraft.getInstance();
                if (client == null) return;
                client.execute(() -> apply(response));
            } catch (Exception e) {
                setStatus("Server nicht erreichbar.");
            }
        });
    }

    private void apply(HttpClient.JsonResponse response) {
        if (closed.get() || !StaffClientAccess.isCurrent(capturedEpoch, capturedToken)) return;
        if (response.status() == 401 || response.status() == 403) {
            StaffClientAccess.invalidate();
            status = StaffClientAccess.publicError(response, "Keine Berechtigung.");
            return;
        }
        if (response.status() != 200) {
            status = StaffClientAccess.publicError(response, "Aktion fehlgeschlagen.");
            return;
        }
        clearInputs();
        StaffNameSuggestions.invalidateSanctions();
        if (StaffClientAccess.sanctionStoredWithoutLiveUpdate(response.body())) {
            status = StaffClientAccess.sanctionStoredWarning(response.body());
            return;
        }
        status = "Aktion übernommen.";
    }

    private void setStatus(String text) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return;
        client.execute(() -> {
            if (!closed.get()) status = text;
        });
    }

    @Override
    public void onClose() {
        closed.set(true);
        super.onClose();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xC0101010);
        context.text(this.font, this.status, this.width / 2 - 140, 20, 0xFFFFFF00, false);
        super.extractRenderState(context, mouseX, mouseY, delta);
    }
}
