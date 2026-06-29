package be.submanifold.pente.rules;

import java.util.ArrayList;
import java.util.List;

/**
 * D4 (dihedral-8) symmetry helpers for Renju Branch-B offer dedup, 15x15.
 *
 * <p>Symmetries are computed about the placed-stone SHAPE, not the fixed board centre:
 * a symmetry is an affine map {@code g(p) = lin(p, r) + (tx, ty)} solved from the placed
 * set, so off-centre point/axis symmetries are found. The earlier centre-only version
 * missed those and offered mirror-equivalent candidates as distinct.
 */
public final class RenjuSymmetry {

    private static final int N = 15;

    private RenjuSymmetry() {}

    // Linear D4 parts, applied to ABSOLUTE coordinates (no +/-centre offset).
    private static final int[] ROTX = {1, 1, 1, 1, -1, -1, -1, -1};
    private static final int[] ROTY = {1, 1, -1, -1, -1, -1, 1, 1};
    private static final int[] ROTF = {0, 1, 0, 1, 0, 1, 0, 1};

    /** Linear D4 part of op {@code r} on absolute coords; returns {x1, y1}. */
    private static int[] lin(int x, int y, int r) {
        int x1 = x * ROTX[r], y1 = y * ROTY[r];
        if (ROTF[r] != 0) { int t = x1; x1 = y1; y1 = t; }
        return new int[]{x1, y1};
    }

    /**
     * Image of {@code move} under affine transform {@code t = (r, tx, ty)}.
     * Bounds guard: returns the sentinel {@code -1} when the image leaves the board
     * (prevents row wraparound from producing a false duplicate).
     */
    public static int applyTransform(int move, int[] t) {
        int x = move % N, y = move / N;
        int[] l = lin(x, y, t[0]);
        int X = l[0] + t[1], Y = l[1] + t[2];
        if (X >= 0 && X < N && Y >= 0 && Y < N) return X + Y * N;
        return -1;
    }

    /**
     * The position's stabilizer: every affine transform {@code (r, tx, ty)} that maps the
     * coloured placed set onto itself (a colour-preserving bijection). Always contains the
     * identity {@code (0,0,0)}; for an asymmetric shape that is the only member.
     *
     * @param board flat 15x15, 0 = empty, positive = stone colour
     */
    public static int[][] stabilizer(byte[] board) {
        List<int[]> result = new ArrayList<>();

        // Collect the placed stones ONCE as {x, y, colour}; drive the r/q loops and the
        // verification off this small list instead of re-scanning all N*N cells per candidate.
        List<int[]> stones = new ArrayList<>();
        for (int m = 0; m < N * N; m++) {
            byte v = board[m];
            if (v > 0) stones.add(new int[]{m % N, m / N, v});
        }

        if (stones.isEmpty()) { // empty board: identity only
            result.add(new int[]{0, 0, 0});
            return result.toArray(new int[0][]);
        }

        int[] p0 = stones.get(0);
        int p0x = p0[0], p0y = p0[1];
        int c0 = p0[2];

        for (int r = 0; r < 8; r++) {
            int[] lp0 = lin(p0x, p0y, r);
            for (int[] q : stones) {
                if (q[2] != c0) continue;
                int tx = q[0] - lp0[0];
                int ty = q[1] - lp0[1];
                if (isStabilizer(board, stones, r, tx, ty)) {
                    addUnique(result, new int[]{r, tx, ty});
                }
            }
        }
        addUnique(result, new int[]{0, 0, 0}); // identity guaranteed
        return result.toArray(new int[0][]);
    }

    /** True iff (r, tx, ty) maps every placed stone onto a placed stone of the same colour. */
    private static boolean isStabilizer(byte[] board, List<int[]> stones, int r, int tx, int ty) {
        for (int[] s : stones) {
            int[] l = lin(s[0], s[1], r);
            int X = l[0] + tx, Y = l[1] + ty;
            if (X < 0 || X >= N || Y < 0 || Y >= N) return false;
            if (board[X + Y * N] != s[2]) return false;
        }
        return true;
    }

    private static void addUnique(List<int[]> list, int[] t) {
        for (int[] e : list) {
            if (e[0] == t[0] && e[1] == t[1] && e[2] == t[2]) return;
        }
        list.add(t);
    }

    /**
     * True iff {@code move} maps onto an already-offered index under some stabilizer
     * transform. The stabilizer is a group (closed, has inverses), so this one-directional
     * check is complete.
     */
    public static boolean isOfferDup(int move, int[] offers, int[][] stab) {
        for (int[] t : stab) {
            int img = applyTransform(move, t);
            if (img < 0) continue;
            for (int o : offers) {
                if (img == o) return true;
            }
        }
        return false;
    }

    /** True iff no two offers in the set are stabilizer-symmetric duplicates. */
    public static boolean isValidOfferSet(int[] offers, int[][] stab) {
        for (int i = 0; i < offers.length; i++) {
            int[] prior = new int[i];
            System.arraycopy(offers, 0, prior, 0, i);
            if (isOfferDup(offers[i], prior, stab)) return false;
        }
        return true;
    }

    /** Convenience: dedup {@code move} against {@code accepted} using the stabilizer of {@code board}. */
    public static boolean isSymmetricDup(int move, int[] accepted, byte[] board) {
        return isOfferDup(move, accepted, stabilizer(board));
    }
}
