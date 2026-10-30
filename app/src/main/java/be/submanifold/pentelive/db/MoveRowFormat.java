package be.submanifold.pentelive.db;

import java.text.NumberFormat;
import java.util.Locale;

/** Text for one row of the Moves sheet. */
public final class MoveRowFormat {
    private MoveRowFormat() {}

    public static String pct(double winPct) {
        return Math.round(winPct) + "%";
    }

    public static String games(int n, Locale l) {
        return NumberFormat.getIntegerInstance(l).format(n);
    }
}
