package be.submanifold.pentelive;

import static be.submanifold.pentelive.BoardGesture.Action.CANCEL;
import static be.submanifold.pentelive.BoardGesture.Action.MOVE_LOUPE;
import static be.submanifold.pentelive.BoardGesture.Action.NONE;
import static be.submanifold.pentelive.BoardGesture.Action.PLACE;
import static be.submanifold.pentelive.BoardGesture.Action.SHOW_LOUPE;
import static be.submanifold.pentelive.BoardGesture.Action.SWIPE_LEFT;
import static be.submanifold.pentelive.BoardGesture.Action.SWIPE_RIGHT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** JVM tests for the pure-Java swipe / placement classifier. Density 1 so px == dp. */
public class BoardGestureTest {

    private static BoardGesture g() {
        return new BoardGesture(1f, true);
    }

    @Test
    public void quickTapPlaces() {
        BoardGesture g = g();
        assertEquals(NONE, g.down(500, 500, 0));
        assertEquals(PLACE, g.up(502, 501, 60));
    }

    @Test
    public void holdStillShowsLoupeViaTick() {
        BoardGesture g = g();
        g.down(500, 500, 0);
        assertEquals(NONE, g.tick(100));
        assertEquals(SHOW_LOUPE, g.tick(150));
        assertTrue(g.loupeAllowed());
        assertEquals(PLACE, g.up(500, 500, 400));
    }

    @Test
    public void fastSwipeLeft() {
        BoardGesture g = g();
        g.down(500, 500, 0);
        assertEquals(NONE, g.move(470, 505, 30));
        assertTrue(g.swipeLatched());
        assertEquals(NONE, g.move(300, 510, 60));
        assertEquals(SWIPE_LEFT, g.up(300, 510, 70));
    }

    @Test
    public void slowSwipeRightWithLongRestBeforeLift() {
        BoardGesture g = g();
        g.down(500, 500, 0);
        assertEquals(NONE, g.move(530, 500, 100));
        assertEquals(NONE, g.tick(300));
        assertEquals(NONE, g.move(700, 505, 900));
        assertEquals(SWIPE_RIGHT, g.up(700, 505, 3000));
    }

    @Test
    public void swipeNeverShowsLoupeEvenAfterHoldTime() {
        BoardGesture g = g();
        g.down(500, 500, 0);
        g.move(530, 500, 50);
        assertEquals(NONE, g.tick(400));
        assertEquals(NONE, g.move(560, 502, 500));
        assertFalse(g.loupeAllowed());
    }

    @Test
    public void shortLatchedSwipeCancelsNotPlaces() {
        BoardGesture g = g();
        g.down(500, 500, 0);
        g.move(515, 500, 30);
        assertEquals(CANCEL, g.up(530, 500, 60));
    }

    @Test
    public void reversedSwipeCancels() {
        BoardGesture g = g();
        g.down(500, 500, 0);
        g.move(520, 500, 30);
        g.move(440, 500, 80);
        assertEquals(CANCEL, g.up(440, 500, 100));
    }

    @Test
    public void swipeThatTurnsVerticalCancels() {
        BoardGesture g = g();
        g.down(500, 500, 0);
        g.move(520, 500, 30);
        g.move(550, 700, 80);
        assertEquals(CANCEL, g.up(550, 700, 100));
    }

    @Test
    public void verticalMoveArmsLoupe() {
        BoardGesture g = g();
        g.down(500, 500, 0);
        assertEquals(SHOW_LOUPE, g.move(505, 540, 40));
        assertEquals(MOVE_LOUPE, g.move(505, 560, 60));
        assertEquals(PLACE, g.up(505, 560, 80));
    }

    @Test
    public void diagonalBelowRatioArmsLoupe() {
        BoardGesture g = g();
        g.down(500, 500, 0);
        assertEquals(SHOW_LOUPE, g.move(530, 525, 40));
    }

    @Test
    public void ratioBoundary() {
        BoardGesture a = g();
        a.down(500, 500, 0);
        assertEquals(NONE, a.move(530, 520, 20)); // dx 30 = 1.5 * dy 20 -> latched
        assertTrue(a.swipeLatched());

        BoardGesture b = g();
        b.down(500, 500, 0);
        assertEquals(SHOW_LOUPE, b.move(529, 520, 20)); // dx 29 < 30 -> aiming
        assertTrue(b.loupeAllowed());
    }

    @Test
    public void aimingHorizontalDragStillPlaces() {
        BoardGesture g = g();
        g.down(500, 500, 0);
        assertEquals(SHOW_LOUPE, g.tick(150));
        assertEquals(MOVE_LOUPE, g.move(620, 500, 300));
        assertEquals(PLACE, g.up(620, 500, 320));
    }

    @Test
    public void swipeDistanceBoundary() {
        BoardGesture a = g();
        a.down(500, 500, 0);
        a.move(520, 500, 10);
        assertEquals(SWIPE_RIGHT, a.up(540, 500, 20)); // exactly 40

        BoardGesture b = g();
        b.down(500, 500, 0);
        b.move(520, 500, 10);
        assertEquals(CANCEL, b.up(539.9f, 500, 20));
    }

    @Test
    public void cancelResets() {
        BoardGesture g = g();
        g.down(500, 500, 0);
        g.move(530, 500, 20);
        assertEquals(CANCEL, g.cancel());
        assertEquals(NONE, g.up(600, 500, 40));
        assertEquals(NONE, g.down(500, 500, 100));
        assertEquals(PLACE, g.up(500, 500, 120));
    }

    @Test
    public void swipesDisabledZoomsImmediately() {
        BoardGesture g = new BoardGesture(2.75f, false);
        assertEquals(SHOW_LOUPE, g.down(500, 500, 0));
        assertEquals(MOVE_LOUPE, g.move(900, 500, 20));
        assertEquals(PLACE, g.up(900, 500, 40));
    }

    @Test
    public void densityScales() {
        // 15 px of travel: past the 10 dp slop at density 1, but within it at density 3.
        BoardGesture lo = new BoardGesture(1f, true);
        lo.down(500, 500, 0);
        assertEquals(NONE, lo.move(515, 500, 10));
        assertTrue(lo.swipeLatched());

        BoardGesture hi = new BoardGesture(3f, true);
        hi.down(500, 500, 0);
        assertEquals(NONE, hi.move(515, 500, 10));
        assertFalse(hi.swipeLatched());

        // 100 px at density 3 is only 33 dp -> below SWIPE_MIN_DP
        hi.move(560, 500, 20);
        assertTrue(hi.swipeLatched());
        assertEquals(CANCEL, hi.up(600, 500, 30));
        // 130 px at density 3 is 43 dp -> swipe
        BoardGesture hi2 = new BoardGesture(3f, true);
        hi2.down(500, 500, 0);
        hi2.move(560, 500, 20);
        assertEquals(SWIPE_RIGHT, hi2.up(630, 500, 30));
    }

    @Test
    public void oneSwipePerGesture() {
        BoardGesture g = g();
        g.down(500, 500, 0);
        g.move(560, 500, 20);
        assertEquals(SWIPE_RIGHT, g.up(600, 500, 40));
        assertEquals(NONE, g.up(600, 500, 50));
        assertEquals(NONE, g.move(700, 500, 60));
    }

    @Test
    public void moveWithinSlopAfterHoldArmsLoupe() {
        BoardGesture g = g();
        g.down(500, 500, 0);
        assertEquals(SHOW_LOUPE, g.move(503, 502, 200));
    }

    @Test
    public void secondPointerCancelDuringAimingCancels() {
        BoardGesture g = g();
        g.down(500, 500, 0);
        g.tick(150);
        assertEquals(CANCEL, g.cancel());
        assertEquals(NONE, g.up(500, 500, 300));
    }
}
