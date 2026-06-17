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
}
