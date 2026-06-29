package be.submanifold.pente.rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

/**
 * Canonical spec vectors for the Renju 5th-move offer symmetry (Branch B).
 * See scratchpad/renju_symmetry_spec.md — single source of truth.
 *
 * Index convention: idx = x + y*15, x = idx % 15, y = idx / 15.
 * Colours: BLACK = 2, WHITE = 1 (labelling is symmetric for the stabilizer).
 */
public class RenjuSymmetryTest {

    private static final int N = 15;
    private static final byte BLACK = 2, WHITE = 1;

    private static int idx(int x, int y) { return x + y * N; }

    /** The regression position: m1..m4 = (7,7)B, (7,6)W, (6,7)B, (6,8)W. */
    private static byte[] regressionBoard() {
        byte[] b = new byte[N * N];
        b[idx(7, 7)] = BLACK;
        b[idx(6, 7)] = BLACK;
        b[idx(7, 6)] = WHITE;
        b[idx(6, 8)] = WHITE;
        return b;
    }

    private static Set<String> stabSet(int[][] stab) {
        Set<String> s = new HashSet<>();
        for (int[] t : stab) s.add(t[0] + "," + t[1] + "," + t[2]);
        return s;
    }

    /** Stabilizer is exactly {identity, 180-deg about (6.5,7)} = {(0,0,0),(4,13,14)}. */
    @Test
    public void stabilizerIsIdentityPlusOffCentreHalfTurn() {
        int[][] stab = RenjuSymmetry.stabilizer(regressionBoard());
        Set<String> got = stabSet(stab);
        Set<String> want = new HashSet<>();
        want.add("0,0,0");
        want.add("4,13,14");
        assertEquals(want, got);
    }

    /** 1. Dedup now fires: each mirror pair's second candidate is a duplicate. */
    @Test
    public void mirrorPairsAreDuplicates() {
        int[][] stab = RenjuSymmetry.stabilizer(regressionBoard());
        int[][] pairs = {
                {idx(8, 4), idx(5, 10)},   // 68 <-> 155
                {idx(6, 5), idx(7, 9)},    // 81 <-> 142
                {idx(5, 6), idx(8, 8)},    // 95 <-> 128
                {idx(9, 9), idx(4, 5)},    // 144 <-> 79
        };
        for (int[] p : pairs) {
            assertTrue("expected " + p[1] + " dup of " + p[0],
                    RenjuSymmetry.isOfferDup(p[1], new int[]{p[0]}, stab));
            assertTrue("expected " + p[0] + " dup of " + p[1] + " (group symmetric)",
                    RenjuSymmetry.isOfferDup(p[0], new int[]{p[1]}, stab));
        }
    }

    /** Sanity: the published indices match the (x,y) intent. */
    @Test
    public void publishedIndicesMatch() {
        assertEquals(68, idx(8, 4));
        assertEquals(155, idx(5, 10));
        assertEquals(81, idx(6, 5));
        assertEquals(142, idx(7, 9));
        assertEquals(95, idx(5, 6));
        assertEquals(128, idx(8, 8));
        assertEquals(144, idx(9, 9));
        assertEquals(79, idx(4, 5));
    }

    /** 2. No over-collapse: replace m4 with (5,8)W -> asymmetric -> stabilizer = {identity}. */
    @Test
    public void asymmetricControlOnlyRejectsExactRepeat() {
        byte[] b = new byte[N * N];
        b[idx(7, 7)] = BLACK;
        b[idx(6, 7)] = BLACK;
        b[idx(7, 6)] = WHITE;
        b[idx(5, 8)] = WHITE; // asymmetry-breaking
        int[][] stab = RenjuSymmetry.stabilizer(b);
        assertEquals(1, stab.length);
        assertEquals("0,0,0", stab[0][0] + "," + stab[0][1] + "," + stab[0][2]);

        // (8,4) then (5,10): NOT a duplicate now (both accepted).
        assertFalse(RenjuSymmetry.isOfferDup(idx(5, 10), new int[]{idx(8, 4)}, stab));
        // exact repeat is still rejected.
        assertTrue(RenjuSymmetry.isOfferDup(idx(8, 4), new int[]{idx(8, 4)}, stab));
    }

    /** 3. Bounds guard: a 180-image that leaves the board must not flag a false dup. */
    @Test
    public void boundsGuardPreventsWraparound() {
        int[][] stab = RenjuSymmetry.stabilizer(regressionBoard());
        // (14,14) -> lin r=4 -> (-14,-14) + (13,14) = (-1,0): X=-1 off-board -> sentinel -1.
        assertEquals(-1, RenjuSymmetry.applyTransform(idx(14, 14), new int[]{4, 13, 14}));
        // Offering (14,14) after an unrelated offer (1,1): both accepted.
        assertFalse(RenjuSymmetry.isOfferDup(idx(14, 14), new int[]{idx(1, 1)}, stab));
        assertFalse(RenjuSymmetry.isOfferDup(idx(1, 1), new int[]{idx(14, 14)}, stab));
    }

    /**
     * 3b. TRUE row-wraparound: the per-axis X/Y guard (not just the {@code image < 0}
     * sentinel) is the assertion that must hold. Offer (14,13)=209; candidate (14,0)=14.
     * Under the non-identity op (4,13,14): lin_r4(14,0)=(-14,0); +(13,14)=(-1,14).
     * X=-1 is off-board, so the per-axis guard returns the sentinel -1 and (14,0) is NOT a
     * duplicate -> ACCEPTED. WITHOUT the per-axis guard, the naive flat index would be
     * (-1)+(14*15)=209 = the prior-row offer -> (14,0) would be FALSELY rejected.
     * This test fails if the {@code 0<=X<N && 0<=Y<N} guard in applyTransform is removed.
     */
    @Test
    public void boundsGuardPreventsRowWraparound() {
        int[][] stab = RenjuSymmetry.stabilizer(regressionBoard());
        // The non-identity op sends (14,0) to an off-board column -> sentinel -1, not 209.
        assertEquals(-1, RenjuSymmetry.applyTransform(idx(14, 0), new int[]{4, 13, 14}));
        // Offering 209 then 14: 14 must be ACCEPTED (not a symmetric duplicate).
        assertFalse(RenjuSymmetry.isOfferDup(idx(14, 0), new int[]{idx(14, 13)}, stab));
        // Spelled out per the spec assertion: isOfferDup(14, {209}, stab) == false.
        assertFalse(RenjuSymmetry.isOfferDup(14, new int[]{209}, stab));
    }

    /** 4. A valid 10-offer set with no two related by any stabilizer transform is fully accepted. */
    @Test
    public void validFullOfferSetAccepted() {
        int[][] stab = RenjuSymmetry.stabilizer(regressionBoard());
        // 10 points each in a distinct orbit (none is the (13-x,14-y) image of another).
        int[] offers = {
                idx(8, 4),   // 68
                idx(6, 5),   // 81
                idx(5, 6),   // 95
                idx(9, 9),   // 144
                idx(0, 0),   // 0
                idx(1, 0),   // 1
                idx(2, 0),   // 2
                idx(3, 0),   // 3
                idx(4, 0),   // 4
                idx(0, 1),   // 15
        };
        assertTrue(RenjuSymmetry.isValidOfferSet(offers, stab));
    }
}
