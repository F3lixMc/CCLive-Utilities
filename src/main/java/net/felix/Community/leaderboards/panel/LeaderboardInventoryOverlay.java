package net.felix.Community.leaderboards.panel;

import java.util.List;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.felix.CCLiveUtilitiesConfig;
import net.felix.Community.leaderboards.LeaderboardManager;
import net.felix.Community.leaderboards.LeaderboardSnapshot;
import net.felix.Community.leaderboards.LeaderboardSync;
import net.felix.Community.leaderboards.LeaderboardSyncSchedule;
import net.felix.utilities.Farmworld.farmzone.FarmzoneScoreboard;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2fStack;

/**
 * Rangliste links neben dem eigenen Inventar.
 * Die Seiten stehen lokal. Der Sync holt den Stand im Servertakt.
 */
public final class LeaderboardInventoryOverlay {
    public static final float DEFAULT_SCALE = 0.85f;
    static final float MIN_SCALE = 0.5f;
    static final float MAX_SCALE = 2.0f;

    private static int anchorLeft;
    private static int anchorTop;
    private static int anchorHeight;
    private static boolean anchorReady;

    private static List<LeaderboardPlaces.Entry> rotation = List.of();
    private static int typeIndex;
    private static int page;
    private static int seenTick = Integer.MIN_VALUE;
    private static String placeKey = "";
    private static boolean manual;
    private static String manualKey;
    private static int manualPage;
    private static boolean hasLastPos;
    private static int lastMoveTick = Integer.MIN_VALUE;
    private static double lastX;
    private static double lastY;
    private static double lastZ;

    static {
        ClientTickEvents.END_CLIENT_TICK.register(LeaderboardInventoryOverlay::onClientTick);
    }

    private LeaderboardInventoryOverlay() {
    }

    public static void render(GuiGraphicsExtractor context, int mouseX, int mouseY,
                              int leftPos, int topPos, int imageHeight) {
        rememberAnchor(leftPos, topPos, imageHeight);
        if (!visible()) {
            return;
        }
        float scale = scale();
        int originX = panelX(leftPos, scaledWidth(scale));
        int originY = panelY(topPos, imageHeight, scaledHeight(scale));
        int localX = textureX(mouseX, originX, scale);
        int localY = textureY(mouseY, originY, scale);

        openIfNeeded();
        LeaderboardPlaces.Entry entry = current();
        List<LeaderboardSnapshot.Row> allRows = rowsOf(entry);
        page = LeaderboardSyncSchedule.clampPage(page, allRows.size());
        List<LeaderboardSnapshot.Row> shownRows = LeaderboardSnapshot.pageRows(allRows, page);
        LeaderboardSnapshot.Row self = selfOf(entry);

        Matrix3x2fStack matrices = context.pose();
        matrices.pushMatrix();
        matrices.translate(originX, originY);
        matrices.scale(scale, scale);
        context.blit(
            RenderPipelines.GUI_TEXTURED,
            LeaderboardPanelLayout.TEXTURE,
            0, 0, 0f, 0f,
            LeaderboardPanelLayout.TEX_W, LeaderboardPanelLayout.TEX_H,
            LeaderboardPanelLayout.TEX_W, LeaderboardPanelLayout.TEX_H);
        highlight(context, LeaderboardPanelLayout.TYPE_PREV, localX, localY);
        highlight(context, LeaderboardPanelLayout.TYPE_NEXT, localX, localY);
        highlight(context, LeaderboardPanelLayout.PAGE_PREV, localX, localY);
        highlight(context, LeaderboardPanelLayout.PAGE_NEXT, localX, localY);
        matrices.popMatrix();

        PixelFont57.ScreenGrid grid = PixelFont57.ScreenGrid.choose(guiScale(), scale);
        LeaderboardTextLayer.blit(
            context, originX, originY, scale, grid,
            entry, shownRows, self, localName(), page);
        hoverText(context, mouseX, mouseY, localX, localY);
    }

    public static boolean mouseClicked(double mouseX, double mouseY, int button,
                                       int leftPos, int topPos, int imageHeight) {
        if (button != 0 || !visible()) {
            return false;
        }
        float scale = scale();
        int originX = panelX(leftPos, scaledWidth(scale));
        int originY = panelY(topPos, imageHeight, scaledHeight(scale));
        if (!contains(mouseX, mouseY, originX, originY, scaledWidth(scale), scaledHeight(scale))) {
            return false;
        }
        openIfNeeded();
        int localX = textureX(mouseX, originX, scale);
        int localY = textureY(mouseY, originY, scale);
        if (LeaderboardPanelLayout.TYPE_PREV.contains(localX, localY)) {
            typeIndex = Math.floorMod(typeIndex - 1, rotation.size());
            page = 0;
            remember();
            return true;
        }
        if (LeaderboardPanelLayout.TYPE_NEXT.contains(localX, localY)) {
            typeIndex = Math.floorMod(typeIndex + 1, rotation.size());
            page = 0;
            remember();
            return true;
        }
        if (LeaderboardPanelLayout.PAGE_PREV.contains(localX, localY)) {
            turnPage(-1);
            remember();
            return true;
        }
        if (LeaderboardPanelLayout.PAGE_NEXT.contains(localX, localY)) {
            turnPage(1);
            remember();
            return true;
        }
        return false;
    }

    /**
     * Seitenwechsel nur über der skalierten Rangliste. Der Stand bleibt lokal.
     * Nach unten ist die nächste Seite, nach oben die vorherige, wie bei den Pfeilen.
     */
    public static boolean mouseScrolled(double mouseX, double mouseY, double vertical,
                                        int leftPos, int topPos, int imageHeight) {
        if (!scrollHitsBoard(visible(), contains(mouseX, mouseY, leftPos, topPos, imageHeight, scale()), vertical)) {
            return false;
        }
        openIfNeeded();
        turnPage(scrollDirection(vertical));
        remember();
        return true;
    }

    /** true, wenn das Mausrad nicht an den Item-Viewer weitergereicht werden darf. */
    public static boolean coversScroll(double mouseX, double mouseY) {
        if (!visible() || !anchorReady) {
            return false;
        }
        return contains(mouseX, mouseY, anchorLeft, anchorTop, anchorHeight, scale());
    }

    private static int guiScale() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.getWindow() == null || client.getWindow().getGuiScale() <= 0) {
            return 1;
        }
        return client.getWindow().getGuiScale();
    }

    /** Serverwechsel verwirft die manuelle Auswahl. Das nächste Öffnen nimmt wieder den Standard. */
    public static void onServerChanged() {
        manual = false;
        manualKey = null;
        manualPage = 0;
        placeKey = "";
        seenTick = Integer.MIN_VALUE;
        hasLastPos = false;
        LeaderboardTextLayer.invalidate();
    }

    /**
     * Zonenwechsel in der Farmwelt ist ein Teleport auf demselben Server.
     * 48 Blöcke in einem Tick sind kein Laufweg. Danach gilt wieder die aktuelle Zone.
     */
    private static void onClientTick(Minecraft client) {
        if (client == null || client.player == null || client.level == null) {
            hasLastPos = false;
            return;
        }
        int tick = client.player.tickCount;
        double x = client.player.getX();
        double y = client.player.getY();
        double z = client.player.getZ();
        if (hasLastPos) {
            double dx = x - lastX;
            double dy = y - lastY;
            double dz = z - lastZ;
            boolean inFarm = "farm".equals(placeKey) || FarmzoneScoreboard.isInFarmzone(client);
            if (isFarmZoneTeleport(dx * dx + dy * dy + dz * dz, tick - lastMoveTick == 1, inFarm)) {
                manual = false;
                manualKey = null;
                manualPage = 0;
                seenTick = Integer.MIN_VALUE;
            }
        }
        lastX = x;
        lastY = y;
        lastZ = z;
        lastMoveTick = tick;
        hasLastPos = true;
    }

    static boolean isFarmZoneTeleport(double distanceSquared, boolean oneTick, boolean inFarm) {
        return inFarm && oneTick && distanceSquared >= 48.0 * 48.0;
    }

    private static void openIfNeeded() {
        Minecraft client = Minecraft.getInstance();
        int tick = client.player != null ? client.player.tickCount : 0;
        if (seenTick == Integer.MIN_VALUE || tick - seenTick > 2) {
            LeaderboardPlaces.Rotation picked = LeaderboardPlaces.forPlayer(client);
            boolean placeChanged = !picked.placeKey().equals(placeKey);
            placeKey = picked.placeKey();
            rotation = picked.entries();
            if (placeChanged || !manual) {
                if (placeChanged) {
                    manual = false;
                    manualKey = null;
                }
                typeIndex = picked.defaultIndex();
                page = 0;
            } else {
                int kept = indexOfKey(manualKey);
                if (kept >= 0) {
                    typeIndex = kept;
                    page = manualPage;
                } else {
                    manual = false;
                    manualKey = null;
                    typeIndex = picked.defaultIndex();
                    page = 0;
                }
            }
        }
        seenTick = tick;
    }

    private static void remember() {
        if (rotation.isEmpty()) {
            return;
        }
        manual = true;
        manualKey = key(current());
        manualPage = page;
    }

    private static int indexOfKey(String wanted) {
        if (wanted == null) {
            return -1;
        }
        for (int i = 0; i < rotation.size(); i++) {
            if (wanted.equals(key(rotation.get(i)))) {
                return i;
            }
        }
        return -1;
    }

    private static String key(LeaderboardPlaces.Entry entry) {
        String board = entry.boardId() == null ? "" : entry.boardId();
        return board + "\n" + entry.title();
    }

    private static LeaderboardPlaces.Entry current() {
        if (rotation.isEmpty()) {
            openIfNeeded();
        }
        return rotation.get(Math.floorMod(typeIndex, rotation.size()));
    }

    private static void turnPage(int direction) {
        int next = pageAfter(page, rowsOf(current()).size(), direction);
        if (next == page) {
            return;
        }
        page = next;
    }

    private static List<LeaderboardSnapshot.Row> rowsOf(LeaderboardPlaces.Entry entry) {
        LeaderboardSnapshot.Board board = boardOf(entry);
        return board == null ? List.of() : board.rows();
    }

    private static LeaderboardSnapshot.Row selfOf(LeaderboardPlaces.Entry entry) {
        LeaderboardSnapshot.Board board = boardOf(entry);
        return board == null ? null : board.self();
    }

    private static LeaderboardSnapshot.Board boardOf(LeaderboardPlaces.Entry entry) {
        if (entry.boardId() == null) {
            return null;
        }
        return LeaderboardSync.getInstance().snapshot().board(entry.boardId());
    }

    /** Spielername für die eigene Zeile, solange der Server keinen Eintrag hat. */
    private static String localName() {
        String registered = LeaderboardManager.getInstance().getPlayerName();
        if (registered != null && !registered.isBlank()) {
            return registered;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            String name = client.player.getName().getString();
            if (name != null && !name.isBlank()) {
                return name;
            }
        }
        return "MEINNAME";
    }

    private static void highlight(GuiGraphicsExtractor context, LeaderboardPanelLayout.Hit hit,
                                  int localX, int localY) {
        if (!hit.contains(localX, localY)) {
            return;
        }
        context.fill(hit.x1(), hit.y1(), hit.x2() + 1, hit.y2() + 1, LeaderboardPanelLayout.HOVER_FILL);
    }

    private static void hoverText(GuiGraphicsExtractor context, int mouseX, int mouseY, int localX, int localY) {
        String text = null;
        if (LeaderboardPanelLayout.TYPE_PREV.contains(localX, localY)) {
            text = entryAt(typeIndex - 1).title();
        } else if (LeaderboardPanelLayout.TYPE_NEXT.contains(localX, localY)) {
            text = entryAt(typeIndex + 1).title();
        } else if (LeaderboardPanelLayout.PAGE_PREV.contains(localX, localY)) {
            text = pageHover(page - 1);
        } else if (LeaderboardPanelLayout.PAGE_NEXT.contains(localX, localY)) {
            text = pageHover(page + 1);
        }
        if (text != null) {
            context.setTooltipForNextFrame(Minecraft.getInstance().font, Component.literal(text), mouseX, mouseY);
        }
    }

    private static LeaderboardPlaces.Entry entryAt(int index) {
        return rotation.get(Math.floorMod(index, rotation.size()));
    }

    private static String pageHover(int pageIndex) {
        int shown = Math.floorMod(pageIndex, LeaderboardSyncSchedule.pageCount(rowsOf(current()).size()));
        int start = shown * 10 + 1;
        return "Rangliste: " + start + "-" + (start + 9);
    }

    static int pageAfter(int page, int rowCount, int direction) {
        int current = LeaderboardSyncSchedule.clampPage(page, rowCount);
        int count = LeaderboardSyncSchedule.pageCount(rowCount);
        int next = Math.floorMod(current + direction, count);
        return next == current ? current : next;
    }

    /** Positiv ist nach oben, negativ nach unten. 0 bleibt stehen. */
    static int scrollDirection(double vertical) {
        if (vertical < 0.0) {
            return 1;
        }
        if (vertical > 0.0) {
            return -1;
        }
        return 0;
    }

    static boolean scrollHitsBoard(boolean shown, boolean inside, double vertical) {
        return shown && inside && scrollDirection(vertical) != 0;
    }

    static float sanitizeScale(float scale) {
        if (Float.isNaN(scale) || scale <= 0f) {
            return DEFAULT_SCALE;
        }
        return Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale));
    }

    public static float scaleFromSize(int width, int height) {
        float scaleX = width / (float) LeaderboardPanelLayout.TEX_W;
        float scaleY = height / (float) LeaderboardPanelLayout.TEX_H;
        return sanitizeScale((scaleX + scaleY) / 2f);
    }

    public static int scaledWidth(float scale) {
        return Math.max(1, Math.round(LeaderboardPanelLayout.TEX_W * sanitizeScale(scale)));
    }

    public static int scaledHeight(float scale) {
        return Math.max(1, Math.round(LeaderboardPanelLayout.TEX_H * sanitizeScale(scale)));
    }

    /** Rechte Kante liegt genau ein GUI-Pixel links vom Inventar. */
    static int panelX(int leftPos, int scaledWidth) {
        return leftPos - 1 - scaledWidth;
    }

    /** Vertikal an der Inventarmitte, gemessen an der skalierten Höhe. */
    static int panelY(int topPos, int imageHeight, int scaledHeight) {
        return topPos + (imageHeight - scaledHeight) / 2;
    }

    static boolean contains(double mouseX, double mouseY, int leftPos, int topPos, int imageHeight, float scale) {
        int width = scaledWidth(scale);
        int height = scaledHeight(scale);
        return contains(mouseX, mouseY, panelX(leftPos, width), panelY(topPos, imageHeight, height), width, height);
    }

    static boolean contains(double mouseX, double mouseY, int originX, int originY, int width, int height) {
        return mouseX >= originX && mouseX < originX + width && mouseY >= originY && mouseY < originY + height;
    }

    static int textureX(double mouseX, int originX, float scale) {
        return (int) Math.floor((mouseX - originX) / sanitizeScale(scale));
    }

    static int textureY(double mouseY, int originY, float scale) {
        return (int) Math.floor((mouseY - originY) / sanitizeScale(scale));
    }

    static LeaderboardPanelLayout.Hit hitAt(double mouseX, double mouseY, int originX, int originY, float scale) {
        int localX = textureX(mouseX, originX, scale);
        int localY = textureY(mouseY, originY, scale);
        if (LeaderboardPanelLayout.TYPE_PREV.contains(localX, localY)) {
            return LeaderboardPanelLayout.TYPE_PREV;
        }
        if (LeaderboardPanelLayout.TYPE_NEXT.contains(localX, localY)) {
            return LeaderboardPanelLayout.TYPE_NEXT;
        }
        if (LeaderboardPanelLayout.PAGE_PREV.contains(localX, localY)) {
            return LeaderboardPanelLayout.PAGE_PREV;
        }
        if (LeaderboardPanelLayout.PAGE_NEXT.contains(localX, localY)) {
            return LeaderboardPanelLayout.PAGE_NEXT;
        }
        return null;
    }

    static boolean visible() {
        return CCLiveUtilitiesConfig.HANDLER.instance().leaderboardVisible;
    }

    public static float scale() {
        return sanitizeScale(CCLiveUtilitiesConfig.HANDLER.instance().leaderboardScale);
    }

    public static void applyScale(float scale) {
        CCLiveUtilitiesConfig.HANDLER.instance().leaderboardScale = sanitizeScale(scale);
    }

    public static boolean hasAnchor() {
        return anchorReady;
    }

    public static int anchorLeft() {
        return anchorLeft;
    }

    public static int anchorTop() {
        return anchorTop;
    }

    public static int anchorHeight() {
        return anchorHeight;
    }

    public static int editorX() {
        return panelX(anchorLeft, scaledWidth(scale()));
    }

    public static int editorY() {
        return panelY(anchorTop, anchorHeight, scaledHeight(scale()));
    }

    private static void rememberAnchor(int leftPos, int topPos, int imageHeight) {
        anchorLeft = leftPos;
        anchorTop = topPos;
        anchorHeight = imageHeight;
        anchorReady = true;
    }
}
