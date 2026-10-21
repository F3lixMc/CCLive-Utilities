package net.felix.chat;

import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.ItemViewer.ItemData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.List;

/**
 * Spalten-Übersicht für Build-Share-Chat-Hover.
 * Misst natürliche Größe und skaliert bei Engpass gleichmäßig (kein Clip).
 * Vanilla-SHOW_TEXT ist nur Hit-Test-Marker — echtes Panel zeichnet {@link #tryRenderAtMouse}.
 */
public final class BuildShareOverviewLayout {
    private static final int PAD = 6;
    private static final int COL_GAP = 10;
    private static final int LINE = 10;
    private static final int MAX_CARD_COL = 10; // bis 20 Karten in 2 Spalten
    /** Erkennbar für DrawContextMixin: Vanilla-Tooltip unterdrücken. */
    public static final String VANILLA_HOVER_MARKER = "\u200B\u200BCCLIVE_BUILD_PANEL";
    private static String lastLoggedHoverId;

    private BuildShareOverviewLayout() {
    }

    public static final class LineEntry {
        public final Component text;
        public final int width;

        LineEntry(Component text, int width) {
            this.text = text;
            this.width = width;
        }
    }

    public static final class Column {
        public final List<LineEntry> lines = new ArrayList<>();
        public int width;
    }

    public static final class Layout {
        public Component title;
        public int titleWidth;
        public Column left;   // Rüstung
        public Column mid;    // Waffen + Werkzeuge
        public Column right;  // Module
        public Column cardsLeft;
        public Column cardsRight;
        public int naturalWidth;
        public int naturalHeight;
        public float scale = 1f;
        public int drawWidth;
        public int drawHeight;
    }

    public static Layout build(BuildShareManager.BuildSnapshot data, Font font) {
        Layout layout = new Layout();
        if (data == null || font == null) {
            layout.naturalWidth = 40;
            layout.naturalHeight = 20;
            layout.drawWidth = 40;
            layout.drawHeight = 20;
            return layout;
        }

        String titleStr = data.kitName != null && !data.kitName.isEmpty()
            ? ShareTextCleaner.cleanDisplayName(data.kitName) : "Build";
        layout.title = Component.literal(titleStr).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);
        layout.titleWidth = font.width(layout.title);

        layout.left = buildEquipmentColumn(font, "Rüstung",
            data.equipmentRecognized ? data.equipment.get("armor") : null,
            data.equipmentRecognized, true);
        layout.mid = new Column();
        Column weapons = buildEquipmentColumn(font, "Waffen",
            data.equipmentRecognized ? data.equipment.get("weapons") : null,
            data.equipmentRecognized, true);
        Column tools = buildEquipmentColumn(font, "Werkzeuge",
            data.equipmentRecognized ? data.equipment.get("tools") : null,
            data.equipmentRecognized, true);
        layout.mid.lines.addAll(weapons.lines);
        if (!tools.lines.isEmpty()) {
            if (!layout.mid.lines.isEmpty()) {
                layout.mid.lines.add(new LineEntry(Component.literal(" "), 4));
            }
            layout.mid.lines.addAll(tools.lines);
        }
        layout.mid.width = Math.max(weapons.width, tools.width);
        layout.right = buildEquipmentColumn(font, "Module",
            data.equipment != null ? data.equipment.get("modules") : null,
            data.modulesScanned, true);

        List<Component> filledCards = collectFilledCardNames(data);
        layout.cardsLeft = new Column();
        layout.cardsRight = new Column();
        String cardsHeader = data.cardsRecognized && data.cards != null
            ? ("Karten " + data.cards.filled + "/" + data.cards.total)
            : (data.cardsRecognized ? "Karten" : "Karten: nicht erfasst");
        Component headerComp = Component.literal(cardsHeader).withStyle(
            data.cardsRecognized ? ChatFormatting.YELLOW : ChatFormatting.GRAY);
        int headerW = font.width(headerComp);
        layout.cardsLeft.lines.add(new LineEntry(headerComp, headerW));
        layout.cardsLeft.width = headerW;
        layout.cardsRight.lines.add(new LineEntry(Component.literal(" "), 4));
        layout.cardsRight.width = 4;

        // Dynamisch: gerade → halb/halb; ungerade → links eine mehr. Cap 10+10.
        int n = Math.min(filledCards.size(), MAX_CARD_COL * 2);
        int leftCount = (n + 1) / 2;
        for (int i = 0; i < n; i++) {
            Component name = filledCards.get(i);
            int w = font.width(name);
            LineEntry entry = new LineEntry(name, w);
            if (i < leftCount) {
                layout.cardsLeft.lines.add(entry);
                layout.cardsLeft.width = Math.max(layout.cardsLeft.width, w);
            } else {
                layout.cardsRight.lines.add(entry);
                layout.cardsRight.width = Math.max(layout.cardsRight.width, w);
            }
        }

        int topColsW = layout.left.width + layout.mid.width + layout.right.width + 2 * COL_GAP;
        int cardsW = layout.cardsLeft.width + layout.cardsRight.width + COL_GAP;
        layout.naturalWidth = Math.max(layout.titleWidth, Math.max(topColsW, cardsW)) + 2 * PAD;

        int topH = LINE * Math.max(layout.left.lines.size(),
            Math.max(layout.mid.lines.size(), layout.right.lines.size()));
        int cardsH = LINE * Math.max(layout.cardsLeft.lines.size(), layout.cardsRight.lines.size());
        layout.naturalHeight = PAD + LINE + 4 + topH + 6 + cardsH + PAD + LINE;

        layout.drawWidth = layout.naturalWidth;
        layout.drawHeight = layout.naturalHeight;
        return layout;
    }

    /** Gleichmäßige Skalierung an verfügbare Fläche (kein Abschneiden). */
    public static void fitToScreen(Layout layout, int availW, int availH) {
        if (layout == null) {
            return;
        }
        float sx = availW > 0 ? (float) availW / Math.max(1, layout.naturalWidth) : 1f;
        float sy = availH > 0 ? (float) availH / Math.max(1, layout.naturalHeight) : 1f;
        layout.scale = Math.min(1f, Math.min(sx, sy));
        layout.drawWidth = Math.max(1, Math.round(layout.naturalWidth * layout.scale));
        layout.drawHeight = Math.max(1, Math.round(layout.naturalHeight * layout.scale));
    }

    public static void render(GuiGraphicsExtractor context, Font font, Layout layout, int x, int y) {
        if (context == null || layout == null || font == null) {
            return;
        }
        float scale = layout.scale <= 0 ? 1f : layout.scale;
        context.fill(x, y, x + layout.drawWidth, y + layout.drawHeight, 0xF0101010);
        context.fill(x, y, x + layout.drawWidth, y + 1, 0xFF55FFFF);
        context.fill(x, y + layout.drawHeight - 1, x + layout.drawWidth, y + layout.drawHeight, 0xFF55FFFF);

        context.pose().pushMatrix();
        context.pose().translate(x, y);
        context.pose().scale(scale, scale);

        int cx = PAD;
        int cy = PAD;
        context.text(font, layout.title, cx, cy, 0xFFFFFFFF, true);
        cy += LINE + 4;

        int colY = cy;
        int leftX = cx;
        int midX = leftX + layout.left.width + COL_GAP;
        int rightX = midX + layout.mid.width + COL_GAP;
        drawColumn(context, font, layout.left, leftX, colY);
        drawColumn(context, font, layout.mid, midX, colY);
        drawColumn(context, font, layout.right, rightX, colY);

        int topH = LINE * Math.max(layout.left.lines.size(),
            Math.max(layout.mid.lines.size(), layout.right.lines.size()));
        cy = colY + topH + 6;

        int cardsLeftX = cx;
        int cardsRightX = cardsLeftX + layout.cardsLeft.width + COL_GAP;
        drawColumn(context, font, layout.cardsLeft, cardsLeftX, cy);
        drawColumn(context, font, layout.cardsRight, cardsRightX, cy);

        int footerY = layout.naturalHeight - PAD - LINE;
        context.text(font,
            Component.literal("Klicken für mehr Details").withStyle(ChatFormatting.GRAY),
            cx, footerY, 0xFFFFFFFF, true);

        context.pose().popMatrix();
    }

    private static void drawColumn(GuiGraphicsExtractor context, Font font, Column col, int x, int y) {
        if (col == null) {
            return;
        }
        int yy = y;
        for (LineEntry entry : col.lines) {
            if (entry != null && entry.text != null) {
                context.text(font, entry.text, x, yy, 0xFFFFFFFF, true);
            }
            yy += LINE;
        }
    }

    private static Column buildEquipmentColumn(
            Font font,
            String label,
            BuildShareManager.CategoryBucket bucket,
            boolean recognized,
            boolean listSlots) {
        Column col = new Column();
        if (!recognized || bucket == null) {
            Component h = Component.literal(label + ": —").withStyle(ChatFormatting.DARK_GRAY);
            col.lines.add(new LineEntry(h, font.width(h)));
            col.width = font.width(h);
            return col;
        }
        Component header = Component.literal(label + " " + bucket.filled + "/" + bucket.total)
            .withStyle(ChatFormatting.YELLOW);
        col.lines.add(new LineEntry(header, font.width(header)));
        col.width = font.width(header);

        if (!listSlots || bucket.slotStatuses == null || bucket.slotStatuses.isEmpty()) {
            if (bucket.items != null) {
                for (ItemData item : bucket.items) {
                    if (item == null) {
                        continue;
                    }
                    Component name = ItemShareManager.nameFromSnapshot(item);
                    int w = font.width(name);
                    col.lines.add(new LineEntry(name, w));
                    col.width = Math.max(col.width, w);
                }
            }
            return col;
        }

        for (int i = 0; i < bucket.slotStatuses.size(); i++) {
            String status = bucket.slotStatuses.get(i);
            String slotLabel = bucket.slotLabels != null && i < bucket.slotLabels.size()
                ? bucket.slotLabels.get(i) : ("Slot " + (i + 1));
            if ("unknown".equalsIgnoreCase(status)) {
                Component t = Component.literal(slotLabel + " (nicht erfasst)").withStyle(ChatFormatting.DARK_GRAY);
                int w = font.width(t);
                col.lines.add(new LineEntry(t, w));
                col.width = Math.max(col.width, w);
            } else if ("empty".equalsIgnoreCase(status)) {
                Component t = Component.literal(slotLabel + " (leer)").withStyle(ChatFormatting.DARK_GRAY);
                int w = font.width(t);
                col.lines.add(new LineEntry(t, w));
                col.width = Math.max(col.width, w);
            } else if ("locked".equalsIgnoreCase(status)) {
                Component t = Component.literal(slotLabel + " (gesperrt)").withStyle(ChatFormatting.DARK_RED);
                int w = font.width(t);
                col.lines.add(new LineEntry(t, w));
                col.width = Math.max(col.width, w);
            } else if ("filled".equalsIgnoreCase(status)) {
                ItemData item = findItemForStatusIndex(bucket, i);
                Component name = item != null
                    ? ItemShareManager.nameFromSnapshot(item)
                    : Component.literal(slotLabel).withStyle(ChatFormatting.GRAY);
                int w = font.width(name);
                col.lines.add(new LineEntry(name, w));
                col.width = Math.max(col.width, w);
            }
        }
        return col;
    }

    private static List<Component> collectFilledCardNames(BuildShareManager.BuildSnapshot data) {
        List<Component> out = new ArrayList<>();
        if (data == null || data.cards == null || data.cards.items == null) {
            return out;
        }
        // Kompakt: items in Capture-Reihenfolge (bereits nur FILLED)
        for (ItemData item : data.cards.items) {
            if (item != null) {
                out.add(ItemShareManager.nameFromSnapshot(item));
            }
        }
        return out;
    }

    private static ItemData findItemForStatusIndex(BuildShareManager.CategoryBucket bucket, int statusIndex) {
        if (bucket.items == null || bucket.slotIndices == null || bucket.statusesSlotIndices == null) {
            if (bucket.items != null && statusIndex < bucket.items.size()) {
                return bucket.items.get(statusIndex);
            }
            return null;
        }
        if (statusIndex >= bucket.statusesSlotIndices.size()) {
            return null;
        }
        int slot = bucket.statusesSlotIndices.get(statusIndex);
        for (int i = 0; i < bucket.slotIndices.size(); i++) {
            if (bucket.slotIndices.get(i) != null && bucket.slotIndices.get(i) == slot
                    && i < bucket.items.size()) {
                return bucket.items.get(i);
            }
        }
        return null;
    }

    /**
     * HoverEvent-Inhalt nur als Hit-Test-Marker (echtes Panel via {@link #renderHoveredShare}).
     */
    public static Component toHoverComponent(BuildShareManager.BuildSnapshot data) {
        return Component.literal(VANILLA_HOVER_MARKER);
    }

    /**
     * Vom componentHoverEffect-Mixin: skaliertes Spaltenpanel statt Vanilla-Wrap.
     * @return true wenn gezeichnet (Vanilla darf gecancelt werden)
     */
    public static boolean renderHoveredShare(
            GuiGraphicsExtractor context, String shareId, int mouseX, int mouseY) {
        if (context == null || shareId == null || shareId.isEmpty() || BuildShareViewScreen.isActive()) {
            return false;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.font == null) {
            return false;
        }
        BuildShareManager.BuildSnapshot snap = BuildShareManager.getInstance().getResolvedBuild(shareId);
        if (snap == null) {
            // Snapshot fehlt noch — kurzer Hinweis statt leerem Hover
            BuildShareManager.setLastHoveredShareId(shareId);
            Layout loading = new Layout();
            loading.title = Component.literal("Build wird geladen…").withStyle(ChatFormatting.GRAY);
            loading.titleWidth = client.font.width(loading.title);
            loading.left = new Column();
            loading.mid = new Column();
            loading.right = new Column();
            loading.cardsLeft = new Column();
            loading.cardsRight = new Column();
            loading.naturalWidth = loading.titleWidth + 2 * PAD;
            loading.naturalHeight = PAD * 2 + LINE;
            loading.drawWidth = loading.naturalWidth;
            loading.drawHeight = loading.naturalHeight;
            placeAndRender(context, client, loading, mouseX, mouseY);
            return true;
        }

        BuildShareManager.setLastHoveredShareId(shareId);
        if (CCLiveUtilitiesConfig.HANDLER.instance().chatDebugging
                && (lastLoggedHoverId == null || !lastLoggedHoverId.equals(shareId))) {
            lastLoggedHoverId = shareId;
            System.out.println("[CCLive-Chat] Build-Hover shareId=" + shareId);
        }

        Layout layout = build(snap, client.font);
        placeAndRender(context, client, layout, mouseX, mouseY);
        return true;
    }

    private static void placeAndRender(
            GuiGraphicsExtractor context, Minecraft client, Layout layout, int mouseX, int mouseY) {
        int screenW = client.getWindow() != null ? client.getWindow().getGuiScaledWidth() : 400;
        int screenH = client.getWindow() != null ? client.getWindow().getGuiScaledHeight() : 300;
        int margin = 10;
        int availW = Math.max(48, Math.round(screenW * 0.92f) - margin);
        int availH = Math.max(48, Math.round(screenH * 0.92f) - margin);
        fitToScreen(layout, availW, availH);
        int x = mouseX + 12;
        int y = mouseY - 12;
        if (x + layout.drawWidth > screenW - margin) {
            x = Math.max(margin, mouseX - layout.drawWidth - 8);
        }
        if (y + layout.drawHeight > screenH - margin) {
            y = Math.max(margin, screenH - margin - layout.drawHeight);
        }
        if (y < margin) {
            y = margin;
        }
        render(context, client.font, layout, x, y);
    }

    /** Merkt shareId für Klicks (ohne Panel), falls Hover über Style-Lookup läuft. */
    public static void trackHoverAtMouse(int mouseX, int mouseY) {
        if (BuildShareViewScreen.isActive()) {
            BuildShareManager.clearLastHoveredShareId();
            lastLoggedHoverId = null;
            return;
        }
        String shareId = ChatStyleHelper.resolveBuildShareIdAt(mouseX, mouseY);
        if (shareId == null || shareId.isEmpty()) {
            BuildShareManager.clearLastHoveredShareId();
            lastLoggedHoverId = null;
            return;
        }
        if (BuildShareManager.getInstance().getResolvedBuild(shareId) == null) {
            // ID trotzdem merken (Klick kann nachladen)
            BuildShareManager.setLastHoveredShareId(shareId);
            return;
        }
        BuildShareManager.setLastHoveredShareId(shareId);
    }
}
