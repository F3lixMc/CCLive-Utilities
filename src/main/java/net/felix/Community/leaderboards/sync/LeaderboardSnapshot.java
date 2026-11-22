package net.felix.Community.leaderboards.sync;

import java.util.List;
import java.util.Map;

/** Unveränderlicher Ranglistenstand. Die GUI schneidet daraus Seiten zu je zehn Zeilen. */
public final class LeaderboardSnapshot {
    public static final LeaderboardSnapshot EMPTY = new LeaderboardSnapshot(Map.of());

    private final Map<String, Board> boards;

    public LeaderboardSnapshot(Map<String, Board> boards) {
        this.boards = boards == null ? Map.of() : Map.copyOf(boards);
    }

    public Board board(String boardId) {
        if (boardId == null) {
            return null;
        }
        return boards.get(boardId);
    }

    public static List<Row> pageRows(List<Row> rows, int page) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        int start = Math.max(0, page) * 10;
        if (start >= rows.size()) {
            return List.of();
        }
        int end = Math.min(rows.size(), start + 10);
        return List.copyOf(rows.subList(start, end));
    }

    public record Row(int rank, String name, long score, Integer smithLevel, Long smithXp) {
        public Row(int rank, String name, long score) {
            this(rank, name, score, null, null);
        }
    }

    public record Board(List<Row> rows, Row self) {
        public Board {
            rows = rows == null ? List.of() : List.copyOf(rows);
        }
    }
}
