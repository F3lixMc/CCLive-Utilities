package net.felix.utilities.Overall;

import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/** Liest Sidebar-Zeilen vom Server-Scoreboard (sortiert wie Vanilla-HUD). */
public final class ScoreboardSidebarReader {

    private ScoreboardSidebarReader() {
    }

    public record Row(Text name, Text score, int scoreWidth) {
        public static Row nameOnly(Text name) {
            return new Row(name, Text.empty(), 0);
        }
    }

    public static List<Row> readRows(Scoreboard scoreboard, ScoreboardObjective objective) {
        List<Row> rows = new ArrayList<>();
        if (scoreboard == null || objective == null) {
            return rows;
        }

        Collection<ScoreboardEntry> rawEntries = scoreboard.getScoreboardEntries(objective);
        if (rawEntries == null || rawEntries.isEmpty()) {
            return rows;
        }

        Comparator<ScoreboardEntry> comparator = resolveComparator();
        List<ScoreboardEntry> entries = rawEntries.stream()
                .filter(entry -> !entry.hidden())
                .sorted(comparator)
                .limit(CoinTrackerCustomSidebar.VANILLA_MAX_LINES)
                .toList();

        for (ScoreboardEntry entry : entries) {
            rows.add(Row.nameOnly(resolveDisplayName(scoreboard, entry)));
        }
        return rows;
    }

    private static Comparator<ScoreboardEntry> resolveComparator() {
        try {
            @SuppressWarnings("unchecked")
            Comparator<ScoreboardEntry> comparator =
                    (Comparator<ScoreboardEntry>) InGameHud.class.getField("SCOREBOARD_ENTRY_COMPARATOR").get(null);
            return comparator;
        } catch (ReflectiveOperationException ignored) {
            return (a, b) -> Integer.compare(b.value(), a.value());
        }
    }

    private static Text resolveDisplayName(Scoreboard scoreboard, ScoreboardEntry entry) {
        Text lineText = entry.name();
        String owner = entry.owner();
        if (lineText != null && !lineText.getString().equals(owner)) {
            return lineText;
        }

        Team team = scoreboard.getScoreHolderTeam(owner);
        Text base = Text.literal(owner);
        return team != null ? Team.decorateName(team, base) : base;
    }
}
