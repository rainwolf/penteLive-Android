package be.submanifold.pentelive.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SelectionFlashTest {
    @Test
    public void colourAtEndsAreThePureColours() {
        assertEquals(0xFFFFFFFF, SelectionFlash.colourAt(0f, 0xFFFFFFFF, 0xFFFF0000));
        assertEquals(0xFFFF0000, SelectionFlash.colourAt(1f, 0xFFFFFFFF, 0xFFFF0000));
    }

    @Test
    public void colourAtMidpointBlends() {
        assertEquals(0xFF808000, SelectionFlash.colourAt(0.5f, 0xFF000000, 0xFFFFFF00));
    }

    @Test
    public void colourAtClampsPhase() {
        assertEquals(0xFF000000, SelectionFlash.colourAt(-3f, 0xFF000000, 0xFFFFFFFF));
        assertEquals(0xFFFFFFFF, SelectionFlash.colourAt(9f, 0xFF000000, 0xFFFFFFFF));
    }

    @Test
    public void zeroAnimatorScaleMeansReducedMotion() {
        assertFalse(SelectionFlash.animationsEnabled(0f));
        assertTrue(SelectionFlash.animationsEnabled(1f));
        assertTrue(SelectionFlash.animationsEnabled(0.5f));
    }

    @Test
    public void sideToMoveStone() {
        assertEquals(SelectionFlash.WHITE, SelectionFlash.sideToMove(false, 0));
        assertEquals(SelectionFlash.BLACK, SelectionFlash.sideToMove(false, 1));
        assertEquals(SelectionFlash.WHITE, SelectionFlash.sideToMove(false, 2));
        // Connect6: stones 0, 3, 4, 7 are white's.
        assertEquals(SelectionFlash.WHITE, SelectionFlash.sideToMove(true, 0));
        assertEquals(SelectionFlash.BLACK, SelectionFlash.sideToMove(true, 1));
        assertEquals(SelectionFlash.BLACK, SelectionFlash.sideToMove(true, 2));
        assertEquals(SelectionFlash.WHITE, SelectionFlash.sideToMove(true, 3));
    }
}
