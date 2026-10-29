package be.submanifold.pentelive.db;

import be.submanifold.pentelive.db.DbDtos.NextMove;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Wilson lower bound ranking of next moves (spec 3.6). */
public final class MoveRanking {
    private static final double Z = 1.96;

    private MoveRanking() {}

    public static double wilsonLowerBound(int wins, int games) {
        if (games <= 0 || wins <= 0) return 0; // exact: p=0 gives 0 mathematically; avoids +-1e-17 noise
        double n = games;
        double p = Math.min(1.0, Math.max(0.0, (double) wins / n));
        double z2 = Z * Z;
        return Math.max(0.0, (p + z2 / (2 * n) - Z * Math.sqrt(p * (1 - p) / n + z2 / (4 * n * n))) / (1 + z2 / n));
    }

    public static List<NextMove> sorted(List<NextMove> moves) {
        List<NextMove> out = new ArrayList<>(moves);
        Collections.sort(out, new Comparator<NextMove>() {
            @Override
            public int compare(NextMove a, NextMove b) {
                boolean ag = a.games >= 5;
                boolean bg = b.games >= 5;
                if (ag != bg) return ag ? -1 : 1;
                int c = Double.compare(wilsonLowerBound(b.wins, b.games), wilsonLowerBound(a.wins, a.games));
                if (c != 0) return c;
                return Integer.compare(b.games, a.games);
            }
        });
        return out;
    }

    /** First n rows; n &lt;= 0 means all. */
    public static List<NextMove> top(List<NextMove> sorted, int n) {
        if (n <= 0 || n >= sorted.size()) return new ArrayList<>(sorted);
        return new ArrayList<>(sorted.subList(0, n));
    }
}
