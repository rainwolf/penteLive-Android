package be.submanifold.pentelive.liveGameRoom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Spec C2: freeze 400 ms, minimum interactive window 1000 ms, no optimistic removal. */
public class ArenaRequestListModelTest {

    private FakeArenaScheduler scheduler;
    private int changes;
    private ArenaRequestListModel model;

    @Before
    public void setUp() {
        scheduler = new FakeArenaScheduler();
        changes = 0;
        model = new ArenaRequestListModel(scheduler, () -> changes++);
    }

    private static ArenaJoinRequest req(String player, long seq) {
        return new ArenaJoinRequest(player, seq);
    }

    private static List<ArenaJoinRequest> snap(ArenaJoinRequest... rs) {
        return Arrays.asList(rs);
    }

    private List<String> shown() {
        List<String> out = new ArrayList<>();
        for (ArenaJoinRequest r : model.rows()) out.add(r.player);
        return out;
    }

    /** With no dialog showing a snapshot applies at once; then the dialog opens over those rows.
     *  The list is visible, idle and interactive. */
    private void showRows(ArenaJoinRequest... rs) {
        model.onSnapshot(snap(rs));
        model.onShown();
    }

    @Test
    public void snapshotWhileHidden_appliesAtOnceWithoutFreeze() {
        // No dialog is showing, so nothing is under the owner's finger.
        model.onSnapshot(snap(req("bob", 1)));
        assertEquals(Collections.singletonList("bob"), shown());
        assertFalse(model.isFrozen());
        assertTrue(model.isInteractive());
        assertEquals(0, scheduler.pendingCount());
    }

    @Test
    public void snapshotWhileVisible_freezes400msThenApplies() {
        showRows(req("bob", 1));
        model.onSnapshot(snap(req("bob", 1), req("carol", 2)));
        assertTrue(model.isFrozen());
        assertFalse(model.isInteractive());
        assertEquals(Collections.singletonList("bob"), shown());
        scheduler.advance(399);
        assertTrue(model.isFrozen());
        assertEquals(Collections.singletonList("bob"), shown());
        scheduler.advance(1);
        assertFalse(model.isFrozen());
        assertTrue(model.isInteractive());
        assertEquals(Arrays.asList("bob", "carol"), shown());
    }

    @Test
    public void snapshotsDuringFreeze_onlyTheLastIsApplied() {
        showRows(req("bob", 1));
        model.onSnapshot(snap(req("bob", 1), req("carol", 2)));
        scheduler.advance(100);
        model.onSnapshot(snap(req("carol", 2)));
        scheduler.advance(100);
        model.onSnapshot(snap(req("carol", 2), req("dave", 3)));
        scheduler.advance(199);
        assertEquals(Collections.singletonList("bob"), shown());
        scheduler.advance(1); // t=400: the freeze that started at t=0 ends; it is not restarted
        assertEquals(Arrays.asList("carol", "dave"), shown());
        assertFalse(model.isFrozen());
    }

    @Test
    public void snapshotDuringMinWindow_isHeldThenTriggersTheNextFreeze() {
        showRows(req("bob", 1));
        model.onSnapshot(snap(req("bob", 1), req("carol", 2)));
        scheduler.advance(400); // unfreeze at t=400; window runs to t=1400
        model.onSnapshot(snap(req("carol", 2)));
        assertFalse(model.isFrozen());
        assertTrue(model.isInteractive());
        assertEquals(Arrays.asList("bob", "carol"), shown());
        scheduler.advance(999); // t=1399
        assertFalse(model.isFrozen());
        assertEquals(Arrays.asList("bob", "carol"), shown());
        scheduler.advance(1); // t=1400: window ends, the held snapshot starts a freeze
        assertTrue(model.isFrozen());
        scheduler.advance(400); // t=1800
        assertEquals(Collections.singletonList("carol"), shown());
        assertFalse(model.isFrozen());
    }

    @Test
    public void windowEndingWithNothingHeld_staysInteractive_andTheNextSnapshotFreezesAtOnce() {
        showRows(req("bob", 1));
        model.onSnapshot(snap(req("bob", 1), req("carol", 2)));
        scheduler.advance(1400);
        assertTrue(model.isInteractive());
        assertEquals(0, scheduler.pendingCount());
        model.onSnapshot(snap(req("carol", 2)));
        assertTrue(model.isFrozen());
    }

    @Test
    public void burstOfSnapshots_leavesAtLeast1000msInteractiveAfterEveryUnfreeze() {
        showRows(req("p0", 0));
        List<Long> runs = new ArrayList<>();
        long run = -1; // -1: not inside an interactive run that began at an unfreeze
        boolean wasLocked = false;
        for (int t = 1; t <= 6000; t++) {
            if (t % 50 == 0) {
                model.onSnapshot(snap(req("p0", 0), req("p" + t, t)));
            }
            scheduler.advance(1);
            boolean locked = !model.isInteractive();
            if (wasLocked && !locked) run = 0;
            if (!locked && run >= 0) run++;
            if (locked && run >= 0) {
                runs.add(run);
                run = -1;
            }
            wasLocked = locked;
        }
        assertTrue("the burst must cause several freeze cycles, got " + runs.size(), runs.size() >= 3);
        for (long r : runs) {
            assertTrue("interactive run was only " + r + " ms", r >= ArenaRequestListModel.MIN_INTERACTIVE_MS);
        }
    }

    @Test
    public void firstRowsOntoTheOpenEmptyDialog_freezeBeforeTheyAppear() {
        // The owner just created the table: the dialog shows only "Exit table" over an empty list.
        model.onShown();
        model.onSnapshot(snap(req("bob", 1)));
        assertTrue(model.isFrozen());
        assertFalse(model.isInteractive());
        assertTrue(model.rows().isEmpty());
        assertFalse(model.beginAction("bob"));
        scheduler.advance(399);
        assertTrue(model.rows().isEmpty());
        scheduler.advance(1);
        assertEquals(Collections.singletonList("bob"), shown());
        assertTrue(model.isInteractive());
    }

    @Test
    public void dialogPoppedBySnapshot_onShownBeforeApply_opensFrozen() {
        // Controller ruling C1: when a snapshot opens the dialog, the glue calls onShown() first,
        // so rows that just appeared (reopen after a close, or a first request) cannot be tapped
        // under the owner's finger.
        model.onSnapshot(snap(req("bob", 1)));
        model.reset(); // dialog closed
        model.onShown(); // glue pops the dialog before applying the snapshot
        model.onSnapshot(snap(req("bob", 1), req("carol", 2)));
        assertTrue(model.isFrozen());
        assertFalse(model.isInteractive());
        assertTrue(model.rows().isEmpty());
        scheduler.advance(400);
        assertEquals(Arrays.asList("bob", "carol"), shown());
        assertTrue(model.isInteractive());
    }

    @Test
    public void emptySnapshotOntoTheOpenEmptyDialog_changesNothing() {
        model.onShown();
        model.onSnapshot(snap());
        assertFalse(model.isFrozen());
        assertTrue(model.rows().isEmpty());
        assertEquals(0, scheduler.pendingCount());
    }

    @Test
    public void emptySnapshot_hidesTheListAfterTheFreeze_andTheNextRowsFreezeAgain() {
        showRows(req("bob", 1));
        model.onSnapshot(snap());
        assertTrue(model.isFrozen());
        assertEquals(Collections.singletonList("bob"), shown());
        scheduler.advance(400);
        assertTrue(model.rows().isEmpty());
        assertFalse(model.isFrozen());
        assertFalse(model.isInteractive());
        assertEquals(0, scheduler.pendingCount()); // no minimum window once the list is empty
        model.onSnapshot(snap(req("carol", 2))); // the dialog is still open
        assertTrue(model.isFrozen());
        assertTrue(model.rows().isEmpty());
        scheduler.advance(400);
        assertEquals(Collections.singletonList("carol"), shown());
        assertTrue(model.isInteractive());
    }

    @Test
    public void newRequestCue_firesOnlyForAPlayerAbsentFromThePreviousSnapshot() {
        assertFalse(model.onSnapshot(snap()));
        assertTrue(model.onSnapshot(snap(req("bob", 1))));
        assertFalse(model.onSnapshot(snap(req("bob", 1)))); // R11 echo: nothing new
        assertTrue(model.onSnapshot(snap(req("bob", 1), req("carol", 2))));
        assertFalse(model.onSnapshot(snap(req("carol", 2)))); // a removal is not a cue
        assertTrue(model.onSnapshot(snap(req("carol", 2), req("bob", 3)))); // bob is back
    }

    @Test
    public void beginAction_isRefusedWhileFrozenOrForAPlayerNotShown() {
        showRows(req("bob", 1));
        assertFalse(model.beginAction("zed"));
        assertFalse(model.beginAction(null));
        model.onSnapshot(snap(req("bob", 1), req("carol", 2))); // freeze
        assertFalse(model.beginAction("bob"));
        assertNull(model.inFlightPlayer());
    }

    @Test
    public void beginAction_marksTheRowAndBlocksAllTapsUntilTheNextSnapshot() {
        showRows(req("bob", 1), req("carol", 2));
        int before = changes;
        assertTrue(model.beginAction("bob"));
        assertTrue(changes > before);
        assertEquals("bob", model.inFlightPlayer());
        assertFalse(model.isInteractive());
        assertFalse(model.beginAction("carol"));
        assertEquals(Arrays.asList("bob", "carol"), shown()); // no optimistic removal
        model.onSnapshot(snap(req("carol", 2))); // the R11 answer
        assertNull(model.inFlightPlayer());
        assertTrue(model.isFrozen());
        scheduler.advance(400);
        assertEquals(Collections.singletonList("carol"), shown());
        assertTrue(model.isInteractive());
    }

    @Test
    public void answerToAnActionInsideTheMinWindow_freezesAtOnce() {
        showRows(req("bob", 1));
        model.onSnapshot(snap(req("bob", 1), req("carol", 2)));
        scheduler.advance(400); // window runs to t=1400
        assertTrue(model.beginAction("bob"));
        scheduler.advance(100);
        model.onSnapshot(snap(req("carol", 2))); // the answer, at t=500
        assertTrue(model.isFrozen());
        assertFalse(model.beginAction("bob")); // the stale row is not tappable
        scheduler.advance(400); // t=900, inside what was the old window
        assertEquals(Collections.singletonList("carol"), shown());
        assertTrue(model.isInteractive());
    }

    @Test
    public void reset_cancelsTimersAndClearsEverything() {
        showRows(req("bob", 1));
        model.onSnapshot(snap(req("bob", 1), req("carol", 2))); // freeze timer pending
        model.reset();
        assertTrue(model.rows().isEmpty());
        assertFalse(model.isFrozen());
        assertNull(model.inFlightPlayer());
        assertEquals(0, scheduler.pendingCount());
        int before = changes;
        scheduler.advance(5000);
        assertEquals(before, changes);
        assertTrue(model.rows().isEmpty());
        assertTrue(model.onSnapshot(snap(req("bob", 2)))); // cue history is cleared too
        // reset() also hides the list: with no dialog showing, the snapshot applied at once.
        assertEquals(Collections.singletonList("bob"), shown());
        assertFalse(model.isFrozen());
    }

    @Test
    public void rowsAreACopyTheCallerCannotChange() {
        List<ArenaJoinRequest> in = new ArrayList<>(snap(req("bob", 1)));
        model.onSnapshot(in);
        in.clear();
        assertEquals(1, model.rows().size());
    }
}
