package net.felix.utilities.Overall;

import net.felix.CCLiveUtilitiesConfig;
import net.felix.CoinTrackerDisplayMode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Ersetzt die Vanilla-Sidebar: Server-Zeilen auslesen, CPM einfügen, im Vanilla-Stil rendern.
 */
public final class CoinTrackerCustomSidebar {

    /** Vanilla-Sidebar: maximal 15 Einträge vom Server. */
    public static final int VANILLA_MAX_LINES = 15;
    /** Custom-Sidebar mit CPM-Block: bis zu 4 zusätzliche Zeilen. */
    public static final int CUSTOM_MAX_LINES = VANILLA_MAX_LINES + 4;

    private CoinTrackerCustomSidebar() {
    }

    public static boolean shouldReplaceVanillaSidebar() {
        CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
        return config.enableMod
                && config.coinTrackerEnabled
                && config.coinTrackerDisplayMode == CoinTrackerDisplayMode.SCOREBOARD;
    }

    public static void render(
            DrawContext context,
            ScoreboardObjective objective,
            TextRenderer textRenderer,
            MinecraftClient client) {
        if (client.world == null || objective == null) {
            return;
        }

        Scoreboard scoreboard = client.world.getScoreboard();
        List<ScoreboardSidebarReader.Row> rows = new ArrayList<>(
                ScoreboardSidebarReader.readRows(scoreboard, objective));

        boolean cpmInserted = false;
        if (shouldInsertCpmBlock()) {
            insertCpmBeforeMaterialien(rows);
            cpmInserted = true;
        }

        trimExcessLines(rows, cpmInserted ? CUSTOM_MAX_LINES : VANILLA_MAX_LINES);
        if (rows.isEmpty()) {
            return;
        }

        Text title = objective.getDisplayName();
        int maxWidth = textRenderer.getWidth(title);
        for (ScoreboardSidebarReader.Row row : rows) {
            maxWidth = Math.max(maxWidth, textRenderer.getWidth(row.name()));
        }

        // Vanilla renderScoreboardSidebar positioning (hardcoded line height 9)
        int lineHeight = 9;
        int entryCount = rows.size();
        int scaledHeight = context.getScaledWindowHeight();
        int scaledWidth = context.getScaledWindowWidth();
        int bottomY = scaledHeight / 2 + entryCount * lineHeight / 3;
        int leftX = scaledWidth - maxWidth - 3;
        int rightX = scaledWidth - 3 + 2;
        int titleRowY = bottomY - entryCount * lineHeight;

        context.fill(
                leftX - 2,
                titleRowY - lineHeight - 1,
                rightX,
                titleRowY - 1,
                client.options.getTextBackgroundColor(0.4F));
        context.fill(
                leftX - 2,
                titleRowY - 1,
                rightX,
                bottomY - 1,
                client.options.getTextBackgroundColor(0.3F));

        int titleX = leftX + maxWidth / 2 - textRenderer.getWidth(title) / 2;
        context.drawText(textRenderer, title, titleX, titleRowY - lineHeight - 1, 0xFFFFFFFF, false);

        for (int i = 0; i < entryCount; i++) {
            int y = bottomY - (entryCount - i) * lineHeight;
            ScoreboardSidebarReader.Row row = rows.get(i);
            context.drawText(textRenderer, row.name(), leftX, y, 0xFFFFFFFF, false);
        }
    }

    private static boolean shouldInsertCpmBlock() {
        CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
        return config.enableMod
                && config.coinTrackerEnabled
                && BossBarHudValueDecoder.isFloorDimension();
    }

    /**
     * Layout vor Materialien:
     * (leer), ▌ Coins/min, ➥ CPM, (leer), ▌ Materialien, …
     */
    private static void insertCpmBeforeMaterialien(List<ScoreboardSidebarReader.Row> rows) {
        int materialienIndex = findMaterialienIndex(rows);
        if (materialienIndex < 0) {
            return;
        }

        if (materialienIndex == 0 || !isEmptyLine(rows.get(materialienIndex - 1))) {
            rows.add(materialienIndex, emptyRow());
            materialienIndex++;
        }

        rows.add(materialienIndex, cpmHeaderRow());
        materialienIndex++;

        rows.add(materialienIndex, cpmValueRow());
        materialienIndex++;

        if (materialienIndex >= rows.size() || !isEmptyLine(rows.get(materialienIndex))) {
            rows.add(materialienIndex, emptyRow());
        }
    }

    /** Überschuss von oben entfernen, damit untere Zeilen (Materialien, Footer) erhalten bleiben. */
    private static void trimExcessLines(List<ScoreboardSidebarReader.Row> rows, int maxLines) {
        while (rows.size() > maxLines && removeFirstEmptyLine(rows)) {
        }
        while (rows.size() > maxLines && !rows.isEmpty()) {
            rows.remove(0);
        }
    }

    private static boolean removeFirstEmptyLine(List<ScoreboardSidebarReader.Row> rows) {
        for (int i = 0; i < rows.size(); i++) {
            if (isEmptyLine(rows.get(i))) {
                rows.remove(i);
                return true;
            }
        }
        return false;
    }

    private static int findMaterialienIndex(List<ScoreboardSidebarReader.Row> rows) {
        for (int i = 0; i < rows.size(); i++) {
            if (isMaterialienHeader(cleanLine(rows.get(i).name().getString()))) {
                return i;
            }
        }
        return -1;
    }

    private static final int CPM_BAR_COLOR = 0x545454;
    private static final int CPM_HEADER_COLOR = 0xFCFC54;
    private static final int CPM_VALUE_COLOR = 0x54FCFC;

    private static ScoreboardSidebarReader.Row cpmHeaderRow() {
        MutableText text = Text.empty()
                .append(Text.literal("▌").setStyle(Style.EMPTY.withColor(CPM_BAR_COLOR)))
                .append(Text.literal(" Coins/min").setStyle(Style.EMPTY.withColor(CPM_HEADER_COLOR)));
        return ScoreboardSidebarReader.Row.nameOnly(text);
    }

    private static ScoreboardSidebarReader.Row cpmValueRow() {
        MutableText text = Text.literal("  ➥ " + CoinTrackerUtility.formatCpmForScoreboard())
                .setStyle(Style.EMPTY.withColor(CPM_VALUE_COLOR));
        return ScoreboardSidebarReader.Row.nameOnly(text);
    }

    private static ScoreboardSidebarReader.Row emptyRow() {
        return ScoreboardSidebarReader.Row.nameOnly(Text.literal(""));
    }

    private static boolean isEmptyLine(ScoreboardSidebarReader.Row row) {
        return cleanLine(row.name().getString()).isEmpty();
    }

    private static boolean isMaterialienHeader(String cleanLine) {
        if (!cleanLine.toLowerCase().contains("materialien") || isCpmValueLine(cleanLine)) {
            return false;
        }
        return cleanLine.contains("▌") || cleanLine.contains("|") || cleanLine.startsWith("Materialien");
    }

    private static boolean isCpmValueLine(String cleanLine) {
        String trimmed = cleanLine.stripLeading();
        return trimmed.startsWith("➥") || trimmed.startsWith("->");
    }

    private static String cleanLine(String line) {
        if (line == null || line.isEmpty()) {
            return "";
        }
        return line.replaceAll("§[0-9a-fk-or]", "")
                .replaceAll("[\\u3400-\\u4DBF]", "")
                .trim();
    }
}
