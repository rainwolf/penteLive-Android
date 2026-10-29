package be.submanifold.pentelive.db;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Pure text and colour formatting for a games-list row. */
public final class GameRowFormat {
    private static final int INK = 0xFF16181D;

    private GameRowFormat() {
    }

    public static String score(int winner) {
        if (winner == 1) return "1 – 0";
        if (winner == 2) return "0 – 1";
        return "–";
    }

    /** "" for null or unparsable input; "MMM d", plus ", yyyy" when the year differs from now's. */
    public static String date(String isoUtcOrNull, ZonedDateTime now, Locale l) {
        if (isoUtcOrNull == null) return "";
        ZonedDateTime when;
        try {
            when = Instant.parse(isoUtcOrNull).atZone(now.getZone());
        } catch (RuntimeException e) {
            return "";
        }
        String pattern = when.getYear() == now.getYear() ? "MMM d" : "MMM d, yyyy";
        return DateTimeFormatter.ofPattern(pattern, l).format(when);
    }

    public static String listHeader(String scope, int movesLen) {
        if ("mine".equals(scope)) return "My games (position and filters not applied)";
        String archive = movesLen > 0
                ? "Games through this position (" + movesLen + " moves)"
                : "All matching games";
        if ("both".equals(scope)) {
            return archive + ", plus all your games (personal games not position-filtered)";
        }
        return archive;
    }

    public static int nameArgb(int nameColor) {
        return nameColor == 0 ? INK : 0xFF000000 | (nameColor & 0xFFFFFF);
    }

    public static boolean nameBold(int nameColor) {
        return nameColor != 0;
    }
}
