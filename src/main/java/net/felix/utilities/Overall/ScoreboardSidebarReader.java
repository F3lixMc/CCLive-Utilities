package net.felix.utilities.Overall;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

/** Liest Sidebar-Zeilen vom Server-Scoreboard (sortiert wie Vanilla-HUD). */
public final class ScoreboardSidebarReader {

    private ScoreboardSidebarReader() {
    }

    public record Row(Component name, Component score, int scoreWidth) {
        public static Row nameOnly(Component name) {
            return new Row(name, Component.empty(), 0);
        }
    }

    public static List<Row> readRows(Scoreboard scoreboard, Objective objective) {
        List<Row> rows = new ArrayList<>();
        if (scoreboard == null || objective == null) {
            return rows;
        }

        Collection<PlayerScoreEntry> rawEntries = scoreboard.listPlayerScores(objective);
        if (rawEntries == null || rawEntries.isEmpty()) {
            return rows;
        }

        Comparator<PlayerScoreEntry> comparator = resolveComparator();
        List<PlayerScoreEntry> entries = rawEntries.stream()
                .filter(entry -> !entry.isHidden())
                .sorted(comparator)
                .limit(CoinTrackerCustomSidebar.VANILLA_MAX_LINES)
                .toList();

        for (PlayerScoreEntry entry : entries) {
            rows.add(Row.nameOnly(resolveDisplayName(scoreboard, entry)));
        }
        return rows;
    }

    private static Comparator<PlayerScoreEntry> resolveComparator() {
        try {
            @SuppressWarnings("unchecked")
            Comparator<PlayerScoreEntry> comparator =
                    (Comparator<PlayerScoreEntry>) Gui.class.getField("SCOREBOARD_ENTRY_COMPARATOR").get(null);
            return comparator;
        } catch (ReflectiveOperationException ignored) {
            return (a, b) -> Integer.compare(b.value(), a.value());
        }
    }

    private static Component resolveDisplayName(Scoreboard scoreboard, PlayerScoreEntry entry) {
        Component lineText = entry.ownerName();
        String owner = entry.owner();
        if (lineText != null && !lineText.getString().equals(owner)) {
            return lineText;
        }

        PlayerTeam team = scoreboard.getPlayersTeam(owner);
        Component base = Component.literal(owner);
        return team != null ? PlayerTeam.formatNameForTeam(team, base) : base;
    }
}
