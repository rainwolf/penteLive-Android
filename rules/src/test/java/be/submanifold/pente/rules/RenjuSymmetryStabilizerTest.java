package be.submanifold.pente.rules;

import static org.junit.Assert.*;
import org.junit.Test;

public class RenjuSymmetryStabilizerTest {
    private static byte[] empty() { return new byte[225]; }
    private static byte[] withBlack(int... idx) { byte[] b = empty(); for (int i : idx) b[i] = 2; return b; }

    // Centre is fixed by 180-degree rotation (op 4): 40 <-> 184, 112 fixed.
    @Test public void rotate_180_about_centre() {
        assertEquals(112, RenjuSymmetry.rotate(112, 4));   // centre fixed
        assertEquals(184, RenjuSymmetry.rotate(40, 4));    // (10,2)->(4,12): 10+2*15=40 -> 4+12*15=184
        assertEquals(40, RenjuSymmetry.rotate(184, 4));
    }

    // Lone centre stone is fully symmetric: stabilizer is all 8 ops -> a rotated offer collides.
    @Test public void symmetric_position_rejects_rotations() {
        byte[] board = withBlack(112);
        int[] stab = RenjuSymmetry.stabilizer(board);
        assertEquals(8, stab.length);
        assertTrue(RenjuSymmetry.isSymmetricDup(RenjuSymmetry.rotate(98, 4), new int[]{98}, board));
    }

    // Asymmetric placed position: stabilizer is identity only -> only EXACT dup rejected; rotations legal.
    @Test public void asymmetric_position_allows_rotations() {
        // centre + a stone off any axis breaks all symmetry
        byte[] board = withBlack(112, 99); // 99 = (9,6)
        int[] stab = RenjuSymmetry.stabilizer(board);
        assertEquals(1, stab.length);                       // identity only
        assertTrue(RenjuSymmetry.isSymmetricDup(57, new int[]{57}, board));        // exact dup
        assertFalse(RenjuSymmetry.isSymmetricDup(RenjuSymmetry.rotate(57, 4), new int[]{57}, board)); // rotation OK
    }
}
