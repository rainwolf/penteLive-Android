package be.submanifold.pentelive.db;

import be.submanifold.pente.rules.Variant;
import be.submanifold.pente.rules.Variants;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Capture counts and occupied cells from a :rules replay (spec 3.4). Pure Java. */
public final class CaptureIcons {
    public static final int P1_FILL = 0xFFFFFFFF;
    public static final int P2_FILL = 0xFF16181D;

    public static final class Counts {
        public final boolean visible;
        public final int p1Count;
        public final int p1Fill;
        public final int p2Count;
        public final int p2Fill;

        Counts(boolean visible, int p1Count, int p1Fill, int p2Count, int p2Fill) {
            this.visible = visible;
            this.p1Count = p1Count;
            this.p1Fill = p1Fill;
            this.p2Count = p2Count;
            this.p2Fill = p2Fill;
        }
    }

    private CaptureIcons() {
    }

    public static Counts of(int gameId, List<Integer> moves) {
        Variant v = Variants.fromGameId(gameId);
        boolean visible = v != null && !v.isGo() && !v.isRenju() && !v.isGomoku() && !v.isConnect6();
        if (!visible) {
            return new Counts(false, 0, P1_FILL, 0, P2_FILL);
        }
        BoardReplay.Result r = BoardReplay.replay(gameId, moves);
        // Spec 3.4 read literally: P1 slot = white stones removed, P2 slot = black stones removed.
        return new Counts(true, r.whiteCaptures, P1_FILL, r.blackCaptures, P2_FILL);
    }

    public static String text(Counts c) {
        if (!c.visible) {
            return "";
        }
        return "● × " + c.p1Count + "  ● × " + c.p2Count;
    }

    public static Set<Integer> occupied(int gameId, List<Integer> moves) {
        BoardReplay.Result r = BoardReplay.replay(gameId, moves);
        int n = r.boardXY.length;
        Set<Integer> out = new HashSet<>();
        for (int x = 0; x < n; x++) {
            for (int y = 0; y < n; y++) {
                if (r.boardXY[x][y] != 0) {
                    out.add(y * n + x);
                }
            }
        }
        return out;
    }
}
