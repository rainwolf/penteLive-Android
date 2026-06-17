package be.submanifold.pente.rules;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

public class RenjuSymmetryTest {

    @Test
    public void eightImagesOfOffsetOneZero() {
        int center = 112; // (7,7)
        int east = center + 1;      // (8,7) dx=1,dy=0
        int[] imgs = RenjuSymmetry.d4Images(east);
        assertTrue(contains(imgs, center - 1));   // (6,7)
        assertTrue(contains(imgs, center - 15));  // (7,6)
        assertTrue(contains(imgs, center + 15));  // (7,8)
    }

    @Test
    public void detectsSymmetricDuplicateAcross() {
        int east = 113; // (8,7)
        int west = 111; // (6,7)
        assertTrue(RenjuSymmetry.isSymmetricDup(west, new int[]{east}));
        assertFalse(RenjuSymmetry.isSymmetricDup(113 + 30, new int[]{east}));
    }

    @Test
    public void validOfferSetOfTenHasNoSymmetricDup() {
        int[] offers = {113,114,115,116,128,129,130,131,144,145};
        assertTrue(RenjuSymmetry.isValidOfferSet(offers));
    }

    @Test
    public void offerSetWithSymmetricPairIsInvalid() {
        int[] offers = {113, 111}; // east + west = symmetric
        assertFalse(RenjuSymmetry.isValidOfferSet(offers));
    }

    private static boolean contains(int[] a, int v) {
        return Arrays.stream(a).anyMatch(x -> x == v);
    }
}
