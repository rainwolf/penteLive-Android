package be.submanifold.pentelive.liveGameRoom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Spec C3: marks follow the latest dsgArenaMyRequestsEvent; taps wait for it (R11). */
public class ArenaMyRequestsTest {

    private final ArenaMyRequests my = new ArenaMyRequests();

    private static Set<Integer> tables(Integer... t) {
        return new HashSet<>(Arrays.asList(t));
    }

    @Test
    public void tapOnAnUnmarkedTable_requestsOnce_thenIgnoresTapsUntilTheAnswer() {
        assertEquals(ArenaMyRequests.Tap.REQUEST, my.onTap(5));
        assertEquals(ArenaMyRequests.Tap.IGNORE, my.onTap(5)); // a double-tap never sends twice
        my.onMyRequests(tables(5));
        assertTrue(my.isRequested(5));
        assertEquals(ArenaMyRequests.Tap.CONFIRM_WITHDRAW, my.onTap(5));
    }

    @Test
    public void anAnswerThatLeavesTheSetUnchanged_stillClearsInFlight() {
        assertEquals(ArenaMyRequests.Tap.REQUEST, my.onTap(5));
        my.onMyRequests(tables()); // refused, e.g. BLOCKED
        assertFalse(my.isRequested(5));
        assertEquals(ArenaMyRequests.Tap.REQUEST, my.onTap(5));
    }

    @Test
    public void otherTablesStayTappableWhileOneIsInFlight() {
        my.onTap(5);
        assertEquals(ArenaMyRequests.Tap.REQUEST, my.onTap(6));
    }

    @Test
    public void confirmWithdraw_sendsOnce_thenWaitsForTheAnswer() {
        my.onMyRequests(tables(5));
        assertTrue(my.confirmWithdraw(5));
        assertFalse(my.confirmWithdraw(5));
        assertEquals(ArenaMyRequests.Tap.IGNORE, my.onTap(5));
        my.onMyRequests(tables());
        assertFalse(my.isRequested(5));
    }

    @Test
    public void confirmWithdraw_afterTheRequestAlreadyEnded_sendsNothing() {
        my.onMyRequests(tables(5));
        // The confirm dialog is open; meanwhile the owner declines and the set drops table 5.
        my.onMyRequests(tables());
        assertFalse(my.confirmWithdraw(5));
    }

    @Test
    public void myRequestsReplacesTheWholeSet() {
        my.onMyRequests(tables(1, 2));
        my.onMyRequests(tables(2));
        assertFalse(my.isRequested(1));
        assertTrue(my.isRequested(2));
    }

    @Test
    public void laterChangesToTheCallersSetDoNotLeakIn() {
        Set<Integer> in = tables(3);
        my.onMyRequests(in);
        in.clear();
        assertTrue(my.isRequested(3));
    }
}
