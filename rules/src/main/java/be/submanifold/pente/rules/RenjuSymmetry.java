package be.submanifold.pente.rules;

import java.util.LinkedHashSet;
import java.util.Set;

/** D4 (dihedral-8) symmetry helpers for Renju Branch-B offer dedup. 15x15, center (7,7). */
public final class RenjuSymmetry {

    private static final int N = 15;
    private static final int C = 7;

    private RenjuSymmetry() {}

    /** The up-to-8 D4 images of a move index (in-bounds only, deduped). */
    public static int[] d4Images(int move) {
        int x = move % N, y = move / N;
        int dx = x - C, dy = y - C;
        int[][] orbit = {
                {dx, dy}, {-dy, dx}, {-dx, -dy}, {dy, -dx},
                {-dx, dy}, {dx, -dy}, {dy, dx}, {-dy, -dx}
        };
        Set<Integer> out = new LinkedHashSet<>();
        for (int[] o : orbit) {
            int tx = o[0] + C, ty = o[1] + C;
            if (tx >= 0 && tx < N && ty >= 0 && ty < N) {
                out.add(tx + ty * N);
            }
        }
        int[] arr = new int[out.size()];
        int i = 0;
        for (int v : out) arr[i++] = v;
        return arr;
    }

    /** True if `move` (any of its 8 images) collides with an already-accepted offer. */
    public static boolean isSymmetricDup(int move, int[] accepted) {
        for (int img : d4Images(move)) {
            for (int a : accepted) {
                if (img == a) return true;
            }
        }
        return false;
    }

    /** True if no two offers in the set are D4-symmetric duplicates. */
    public static boolean isValidOfferSet(int[] offers) {
        for (int i = 0; i < offers.length; i++) {
            int[] prior = new int[i];
            System.arraycopy(offers, 0, prior, 0, i);
            if (isSymmetricDup(offers[i], prior)) return false;
        }
        return true;
    }

    private static final int[] ROTX = {1, 1, 1, 1, -1, -1, -1, -1};
    private static final int[] ROTY = {1, 1, -1, -1, -1, -1, 1, 1};
    private static final int[] ROTF = {0, 1, 0, 1, 0, 1, 0, 1};

    /** Image of `move` under D4 op `rot` (0..7) about centre (7,7). */
    public static int rotate(int move, int rot) {
        int x = (move % N) - C, y = (move / N) - C;
        int x1 = x * ROTX[rot], y1 = y * ROTY[rot];
        if (ROTF[rot] != 0) { int t = x1; x1 = y1; y1 = t; }
        return (x1 + C) + (y1 + C) * N;
    }

    /** The ops (0..7) that map the coloured board (0=empty) onto itself — the position's stabilizer. */
    public static int[] stabilizer(byte[] board) {
        Set<Integer> stab = new LinkedHashSet<>();
        for (int r = 0; r < 8; r++) {
            boolean invariant = true;
            for (int m = 0; m < N * N && invariant; m++) {
                byte v = board[m];
                if (v > 0 && board[rotate(m, r)] != v) invariant = false;
            }
            if (invariant) stab.add(r);
        }
        int[] arr = new int[stab.size()];
        int i = 0;
        for (int v : stab) arr[i++] = v;
        return arr;
    }

    /** True if `move` maps onto an already-offered point under any op in the precomputed stabilizer. */
    public static boolean isOfferDup(int move, int[] offers, int[] stab) {
        for (int r : stab) {
            int img = rotate(move, r);
            for (int o : offers) if (img == o) return true;
        }
        return false;
    }

    /** Convenience: dedup `move` against `accepted` using the stabilizer of `board`. */
    public static boolean isSymmetricDup(int move, int[] accepted, byte[] board) {
        return isOfferDup(move, accepted, stabilizer(board));
    }
}
