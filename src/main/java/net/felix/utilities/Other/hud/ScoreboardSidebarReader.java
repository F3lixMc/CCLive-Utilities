package net.felix.utilities.Other.hud;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import net.felix.utilities.Aincraft.cointracker.CoinTrackerCustomSidebar;
import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

/** Liest Sidebar-Zeilen vom Server-Scoreboard (sortiert wie Vanilla-HUD). */
public final class ScoreboardSidebarReader {

    private static final Comparator<PlayerScoreEntry> ENTRY_COMPARATOR = resolveComparator();

    private ScoreboardSidebarReader() {
    }

    /** Einmal aufgelöst. Fehlende Felder bleiben der Wert-Fallback, ohne erneute Reflection. */
    public static Comparator<PlayerScoreEntry> entryComparator() {
        return ENTRY_COMPARATOR;
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

        List<PlayerScoreEntry> entries = rawEntries.stream()
                .filter(entry -> !entry.isHidden())
                .sorted(ENTRY_COMPARATOR)
                .limit(CoinTrackerCustomSidebar.VANILLA_MAX_LINES)
                .toList();

        for (PlayerScoreEntry entry : entries) {
            rows.add(Row.nameOnly(resolveDisplayName(scoreboard, entry)));
        }
        return rows;
    }

    private static Comparator<PlayerScoreEntry> resolveComparator() {
        Comparator<PlayerScoreEntry> named = readComparatorField("SCOREBOARD_ENTRY_COMPARATOR");
        return named != null ? named : valueComparator();
    }

    private static Comparator<PlayerScoreEntry> readComparatorField(String name) {
        try {
            return readComparatorField(Gui.class.getDeclaredField(name));
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static Comparator<PlayerScoreEntry> readComparatorField(Field field) {
        try {
            field.setAccessible(true);
            Object value = field.get(null);
            if (value instanceof Comparator<?> comparator) {
                return (Comparator<PlayerScoreEntry>) comparator;
            }
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
        return null;
    }

    private static Comparator<PlayerScoreEntry> valueComparator() {
        return (a, b) -> Integer.compare(b.value(), a.value());
    }

    private static Component resolveDisplayName(Scoreboard scoreboard, PlayerScoreEntry entry) {
        Component lineText = entry.ownerName();
        String owner = entry.owner() == null ? "" : entry.owner();
        String lineString = lineText == null ? "" : lineText.getString();
        // Leeres display() (26.3 oft gesetzt, Text steht im Team-Prefix) nicht als Zeile verwenden.
        if (!lineString.isBlank() && !lineString.equals(owner)) {
            return lineText;
        }

        PlayerTeam team = scoreboard.getPlayersTeam(owner);
        Component base = Component.literal(owner);
        Component formatted = team != null ? PlayerTeam.formatNameForTeam(team, base) : base;
        String formattedString = formatted.getString();
        if (!formattedString.isBlank()) {
            return formatted;
        }
        return lineText != null ? lineText : base;
    }
}
