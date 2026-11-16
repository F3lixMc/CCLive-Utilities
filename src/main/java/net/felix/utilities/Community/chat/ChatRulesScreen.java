package net.felix.utilities.Community.chat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.felix.utilities.Community.leaderboards.LeaderboardManager;
import net.felix.utilities.Community.leaderboards.SeasonChangeGate;
import net.felix.utilities.Community.leaderboards.http.HttpClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ChatRulesScreen extends Screen {
    private static final String TITLE = "CCLive Chatregeln";
    private static final String[] RULES = {
        "Keine harten Beleidigungen, Diskriminierung, Mobbing oder anzüglichen Bemerkungen gegenüber anderen Spielern. Wenn du das nicht akzeptieren kannst, beleidige dich selbst oder nutz unseren Chat nicht. ;D",
        "Kein unnötiger Spam. Geh anderen Spielern nicht absichtlich auf den Sack.",
        "Keine sexuell expliziten, pornografischen oder sonstigen jugendgefährdenden Inhalte.",
        "Bestimmte Ausdrücke werden vom Chatfilter automatisch blockiert.",
        "Links sind nur zu freigegebenen Domains erlaubt."
    };

    private final boolean accepting;
    private final List<String> domains = new ArrayList<>();
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private Button actionButton;
    private String notice = "Lade Chatregeln …";
    private String status = "";
    private boolean rulesLoaded;
    private boolean rulesFailed;
    private int scroll;
    private int contentHeight;
    private int viewTop;
    private int viewHeight;

    public ChatRulesScreen() {
        this(false);
    }

    private ChatRulesScreen(boolean accepting) {
        super(Component.literal(TITLE));
        this.accepting = accepting;
    }

    public static void open() {
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            client.execute(() -> client.gui.setScreen(new ChatRulesScreen()));
        }
    }

    public static ChatRulesScreen forAcceptance() {
        return new ChatRulesScreen(true);
    }

    @Override
    protected void init() {
        super.init();
        actionButton = Button.builder(Component.literal(actionLabel()), button -> onAction())
                .bounds(this.width / 2 - 60, this.height - 28, 120, 20)
                .build();
        addRenderableWidget(actionButton);
        refresh();
    }

    @Override
    public void onClose() {
        closed.set(true);
        super.onClose();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        int max = Math.max(0, contentHeight - viewHeight);
        if (max == 0) return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
        scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(vertical) * 16));
        return true;
    }

    private boolean stillOpen() {
        if (closed.get()) return false;
        Minecraft client = Minecraft.getInstance();
        return client != null && client.gui.screen() == this;
    }

    private String actionLabel() {
        if (!accepting) return "Schließen";
        return rulesFailed ? "Erneut laden" : "Akzeptieren";
    }

    private void syncAction() {
        if (actionButton != null) {
            actionButton.setMessage(Component.literal(actionLabel()));
        }
    }

    private void onAction() {
        if (!accepting) {
            onClose();
            return;
        }
        if (rulesFailed || !rulesLoaded) {
            if (rulesFailed) refresh();
            return;
        }
        String accountId = ChatIgnoreList.localAccountId();
        Integer seasonId = SeasonChangeGate.confirmedSeasonId();
        ChatRulesAcceptance store = ChatRulesAcceptance.shared();
        if (!store.accept(accountId, seasonId, true)) {
            status = seasonId == null
                ? "Die aktuelle Season ist noch nicht bestätigt."
                : "Die Bestätigung konnte nicht gespeichert werden.";
            return;
        }
        String draft = store.draft() == null ? "" : store.draft();
        store.clearDraft();
        onClose();
        Minecraft client = Minecraft.getInstance();
        if (client == null) return;
        client.execute(() -> {
            if (client.gui != null && client.gui.hud != null && client.gui.hud.getChat() != null) {
                client.gui.hud.getChat().saveAsDraft(draft);
            }
            client.gui.setScreen(new ChatScreen(draft, true));
        });
    }

    private void refresh() {
        rulesLoaded = false;
        status = "";
        LeaderboardManager manager = LeaderboardManager.getInstance();
        HttpClient http = manager == null ? null : manager.getHttpClient();
        String token = manager == null ? null : manager.getPlayerToken();
        if (http == null || token == null || token.isBlank()) {
            fail("Keine Verbindung zum CCLive-Server.");
            return;
        }
        notice = "Lade Chatregeln …";
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                HttpClient.JsonResponse response = http.getResultWithToken("/chat/rules", token);
                Minecraft client = Minecraft.getInstance();
                if (client == null) return;
                client.execute(() -> apply(response));
            } catch (Exception e) {
                Minecraft client = Minecraft.getInstance();
                if (client == null) return;
                client.execute(() -> {
                    if (!stillOpen()) return;
                    fail("Regeln konnten nicht geladen werden.");
                });
            }
        });
    }

    private void fail(String message) {
        rulesLoaded = false;
        rulesFailed = true;
        notice = message;
        domains.clear();
        syncAction();
    }

    private void apply(HttpClient.JsonResponse response) {
        if (!stillOpen()) return;
        domains.clear();
        if (response.status() != 200 || response.body() == null) {
            fail("Regeln konnten nicht geladen werden.");
            return;
        }
        JsonArray rows = response.body().has("domains") && response.body().get("domains").isJsonArray()
            ? response.body().getAsJsonArray("domains") : new JsonArray();
        for (JsonElement element : rows) {
            if (element.isJsonPrimitive()) domains.add(element.getAsString());
        }
        rulesLoaded = true;
        rulesFailed = false;
        notice = "";
        scroll = 0;
        syncAction();
    }

    private void wrap(List<String> lines, String text, int maxWidth) {
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String next = line.length() == 0 ? word : line + " " + word;
            if (this.font.width(next) > maxWidth && line.length() > 0) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                if (line.length() > 0) line.append(' ');
                line.append(word);
            }
        }
        if (line.length() > 0) lines.add(line.toString());
    }

    private List<String> bodyLines(int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (!notice.isEmpty()) {
            wrap(lines, notice, maxWidth);
            return lines;
        }
        for (int i = 0; i < RULES.length; i++) {
            wrap(lines, RULES[i], maxWidth);
            if (i < RULES.length - 1) lines.add("");
        }
        lines.add("");
        lines.add("Freigegebene Domains");
        if (domains.isEmpty()) {
            lines.add("Keine Domain freigegeben.");
        } else {
            for (String domain : domains) wrap(lines, domain, maxWidth);
        }
        if (!status.isEmpty()) {
            lines.add("");
            wrap(lines, status, maxWidth);
        }
        return lines;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xC0101010);
        int boxWidth = Math.min(520, Math.max(180, this.width - 24));
        int boxHeight = Math.max(120, this.height - 44);
        int x = (this.width - boxWidth) / 2;
        int y = 8;
        context.fill(x, y, x + boxWidth, y + boxHeight, 0xFF111111);
        context.outline(x, y, boxWidth, boxHeight, 0xFFFFFFFF);
        int titleWidth = this.font.width(TITLE);
        context.text(this.font, TITLE, x + (boxWidth - titleWidth) / 2, y + 10, 0xFFFFFF00, false);
        viewTop = y + 32;
        viewHeight = boxHeight - 44;
        int textWidth = boxWidth - 36;
        List<String> lines = bodyLines(textWidth);
        contentHeight = lines.size() * 12;
        int maxScroll = Math.max(0, contentHeight - viewHeight);
        if (scroll > maxScroll) scroll = maxScroll;
        int lineY = viewTop - scroll;
        boolean headingNext = false;
        for (String line : lines) {
            if (line.equals("Freigegebene Domains")) headingNext = true;
            int bottom = lineY + 12;
            if (bottom > viewTop && lineY < viewTop + viewHeight) {
                int color = headingNext ? 0xFFFFFF55 : 0xFFFFFFFF;
                context.text(this.font, line, x + 12, lineY, color, false);
            }
            headingNext = false;
            lineY += 12;
        }
        if (maxScroll > 0) {
            int trackX = x + boxWidth - 8;
            int trackHeight = viewHeight;
            int thumbHeight = Math.max(16, trackHeight * viewHeight / contentHeight);
            int thumbY = viewTop + (trackHeight - thumbHeight) * scroll / maxScroll;
            context.fill(trackX, viewTop, trackX + 3, viewTop + trackHeight, 0xFF333333);
            context.fill(trackX, thumbY, trackX + 3, thumbY + thumbHeight, 0xFFDDDDDD);
        }
        super.extractRenderState(context, mouseX, mouseY, delta);
    }
}
