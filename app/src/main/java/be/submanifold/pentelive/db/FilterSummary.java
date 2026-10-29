package be.submanifold.pentelive.db;

import java.util.ArrayList;
import java.util.List;

/** Chip-row text for the applied filters, e.g. "Pente · alice 1700+ · either seat". */
public final class FilterSummary {
    private FilterSummary() {}

    public static String text(DbFilters f) {
        return text(f, DbCapabilities.SERVER_FILTERS_B3_B4);
    }

    /** {@code serverFiltersB3B4} false leaves out exclude-timeouts and live/turn-based, which are then not sent. */
    public static String text(DbFilters f, boolean serverFiltersB3B4) {
        List<String> parts = new ArrayList<>();
        String name = DbGames.baseName(f.baseGameId);
        parts.add(f.gameId() != f.baseGameId ? "Speed " + name : name);
        addPlayer(parts, f.player1Name, f.player1MinRating, "P1");
        addPlayer(parts, f.player2Name, f.player2MinRating, "P2");
        if (f.eitherSeatEffective()) parts.add("either seat");
        if (f.winner == 1) parts.add("P1 won");
        if (f.winner == 2) parts.add("P2 won");
        if (serverFiltersB3B4) {
            if (f.excludeTimeouts) parts.add("no timeouts");
            if (DbFilters.LIVE_ONLY.equals(f.liveOrTurnBased)) parts.add("live only");
            if (DbFilters.TURN_BASED_ONLY.equals(f.liveOrTurnBased)) parts.add("turn-based only");
        }
        if (f.afterDate != null) parts.add("after " + f.afterDate);
        if (f.beforeDate != null) parts.add("before " + f.beforeDate);
        if (DbFilters.SCOPE_MINE.equals(f.scope)) parts.add("Mine");
        if (DbFilters.SCOPE_BOTH.equals(f.scope)) parts.add("Archive + Mine");
        return String.join(" · ", parts);
    }

    private static void addPlayer(List<String> parts, String name, int minRating, String seatLabel) {
        if (name == null && minRating <= 0) return;
        StringBuilder sb = new StringBuilder(name != null ? name : seatLabel);
        if (minRating > 0) sb.append(' ').append(minRating).append('+');
        parts.add(sb.toString());
    }
}
