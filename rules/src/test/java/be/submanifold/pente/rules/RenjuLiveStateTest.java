package be.submanifold.pente.rules;

import static org.junit.Assert.*;
import org.junit.Test;

public class RenjuLiveStateTest {

    // ---- phase classifier (openingPhase.test.js) ----
    @Test public void fresh_state_is_move_then_swap_after_first_stone() {
        RenjuLiveState r = new RenjuLiveState();
        assertEquals(RenjuLiveState.Phase.MOVE, r.phase(0));
        r.advanceAfterMove(1, false);
        assertEquals(RenjuLiveState.Phase.SWAP, r.phase(1));
        assertEquals(2, r.openingPlayer(1)); // white decides window 1
    }

    @Test public void selection_phase_after_offer10() {
        RenjuLiveState r = new RenjuLiveState();
        r.applyOffer10(new int[]{98,99,100,113,114,115,128,129,130,131});
        assertEquals(RenjuLiveState.Phase.SELECTION, r.phase(4));
        assertEquals(2, r.openingPlayer(4)); // white selecting
    }

    // ---- Branch A full sequence (01 spec §18) ----
    @Test public void branch_A_full_sequence() {
        RenjuLiveState r = new RenjuLiveState();
        // M1 black centre
        r.advanceAfterMove(1, false); assertEquals(RenjuLiveState.Phase.SWAP, r.phase(1));
        // win1 white decline+place 113
        r.applySwap(false, 1);        assertEquals(RenjuLiveState.Phase.MOVE, r.phase(1));
        r.advanceAfterMove(2, false); assertEquals(RenjuLiveState.Phase.SWAP, r.phase(2));
        // win2 black decline+place
        r.applySwap(false, 2);        r.advanceAfterMove(3, false); assertEquals(RenjuLiveState.Phase.SWAP, r.phase(3));
        // win3 white decline+place
        r.applySwap(false, 3);        r.advanceAfterMove(4, false); assertEquals(RenjuLiveState.Phase.SWAP, r.phase(4));
        // win4 black "Place 5th move" => Branch A
        r.applySwap(false, 4);
        assertTrue(r.branchChosen); assertFalse(r.tenOffer);
        // M5 black lands => window 5 opens
        r.advanceAfterMove(5, false); assertEquals(RenjuLiveState.Phase.SWAP, r.phase(5));
        assertEquals(2, r.openingPlayer(5)); // window 5: white decides (black placed move 5)
        // win5 white bare decline
        r.applySwap(false, 5);        assertEquals(RenjuLiveState.Phase.MOVE, r.phase(5));
        // M6 white anywhere => complete
        r.advanceAfterMove(6, false); assertEquals(RenjuLiveState.Phase.COMPLETE, r.phase(6));
        assertTrue(r.complete);
    }

    // ---- LIVE take-over at window 4 must auto-commit Branch A (Taraguchi-10 regression) ----
    // The live server delivers a move-4 take-over as a SEAT-SWAP event (DSGSwapSeatsTableEvent ->
    // applySwapSeats), NOT as a renju swap decision-echo. Pre-fix applySwapSeats left branchChosen
    // false, so phase(4) fell back to BRANCH and re-presented the 3-way choice (incl. Offer-10 /
    // Branch B) to the swapped-in player. A seat swap only happens on a take-over, so numMoves==4
    // here means "took over move 4 -> Branch A": seats flip, play continues, Offer-10 unreachable.
    @Test public void takeover_at_move4_commits_branch_A() {
        RenjuLiveState r = new RenjuLiveState();
        // reach the open window at move 4 via three declines
        for (int n = 1; n <= 3; n++) { r.advanceAfterMove(n, false); r.applySwap(false, n); }
        r.advanceAfterMove(4, false);
        assertEquals(RenjuLiveState.Phase.SWAP, r.phase(4)); // window 4 open: swap / decline+place / offer-10
        // the REAL live take-over signal: a seat-swap event at move 4 => Branch A auto-committed
        r.applySwapSeats(4);
        assertTrue("take-over@move4 must commit Branch A", r.branchChosen);
        assertFalse("take-over@move4 must not be Branch B (tenOffer)", r.tenOffer);
        assertTrue(r.swapTaken);
        assertFalse(r.awaitingSwap);
        assertEquals(RenjuLiveState.Phase.MOVE, r.phase(4)); // MOVE, not BRANCH
        assertFalse("swap button gone after take-over", r.showSwap(4));
        assertFalse("Offer-10 unreachable after take-over@move4", r.showOffer10(4));
    }

    // ---- DECLINE path is UNCHANGED by the swap fix: move-4 decline still => Branch A ----
    @Test public void decline_at_move4_still_commits_branch_A() {
        RenjuLiveState r = new RenjuLiveState();
        for (int n = 1; n <= 3; n++) { r.advanceAfterMove(n, false); r.applySwap(false, n); }
        r.advanceAfterMove(4, false);
        r.applySwap(false, 4);
        assertTrue(r.branchChosen);
        assertFalse(r.tenOffer);
        assertEquals(RenjuLiveState.Phase.MOVE, r.phase(4));
    }

    // ---- DECLINE path can still reach Branch B via the separate offer10 echo (unchanged) ----
    @Test public void decline_path_can_still_reach_branch_B_via_offer10() {
        RenjuLiveState r = new RenjuLiveState();
        for (int n = 1; n <= 3; n++) { r.advanceAfterMove(n, false); r.applySwap(false, n); }
        r.advanceAfterMove(4, false);
        assertEquals(RenjuLiveState.Phase.SWAP, r.phase(4));
        r.applyOffer10(new int[]{98,99,100,113,114,115,128,129,130,131}); // decline+offer-10 => Branch B
        assertTrue(r.branchChosen);
        assertTrue(r.tenOffer);
        assertEquals(RenjuLiveState.Phase.SELECTION, r.phase(4));
    }

    // ---- Branch B full sequence ----
    @Test public void branch_B_full_sequence() {
        RenjuLiveState r = new RenjuLiveState();
        r.advanceAfterMove(1, false);
        r.applySwap(false, 1); r.advanceAfterMove(2, false);
        r.applySwap(false, 2); r.advanceAfterMove(3, false);
        r.applySwap(false, 3); r.advanceAfterMove(4, false);
        assertEquals(RenjuLiveState.Phase.SWAP, r.phase(4));
        // win4 black "Offer 10" => Branch B, SELECTION
        r.applyOffer10(new int[]{98,99,100,113,114,115,128,129,130,131});
        assertEquals(RenjuLiveState.Phase.SELECTION, r.phase(4));
        // white selects one
        r.applySelect1(114);
        assertEquals(Integer.valueOf(114), r.selected);
        assertEquals(RenjuLiveState.Phase.MOVE, r.phase(4)); // gap: board inert
        // server places move 5 (tenOffer => no window 5) => complete
        r.advanceAfterMove(5, false);
        assertEquals(RenjuLiveState.Phase.COMPLETE, r.phase(5));
        assertTrue(r.complete);
    }

    // ---- take-over at a NON-move-4 window just resolves the window (-> MOVE), never Branch A ----
    // The Branch-A commit is keyed strictly on numMoves==4; a take-over at window 1..3 or 5 must
    // leave branchChosen false and yield MOVE (guards against over-broadening the fix).
    @Test public void takeover_at_nonfour_window_resolves_to_move() {
        RenjuLiveState r = new RenjuLiveState();
        r.advanceAfterMove(1, false); assertEquals(RenjuLiveState.Phase.SWAP, r.phase(1));
        r.applySwapSeats(1); // take-over at window 1 (not move 4)
        assertFalse(r.awaitingSwap); assertTrue(r.swapTaken);
        assertFalse("non-move-4 take-over must NOT commit a branch", r.branchChosen);
        assertFalse(r.tenOffer);
        assertEquals(RenjuLiveState.Phase.MOVE, r.phase(1));
        assertFalse(r.showSwap(1)); // no swap button post take-over
    }

    // ---- phase classifier: a resolved-but-unchosen move-4 window classifies as BRANCH ----
    // Guards the phase()/openingPlayer() BRANCH branch. In the healthy flow every move-4
    // resolution commits a branch (decline/take-over -> Branch A, offer10 -> Branch B), so this
    // state should not arise in production; but if a resolved window ever lacks a branch commit,
    // phase() must report BRANCH with black (1) to choose -- not silently fall through to MOVE.
    @Test public void phase_branch_when_move4_window_resolved_without_branch() {
        RenjuLiveState r = new RenjuLiveState();
        // awaitingSwap=false (resolved), branchChosen=false, tenOffer=false: the pending-branch state.
        assertEquals(RenjuLiveState.Phase.BRANCH, r.phase(4));
        assertEquals(1, r.openingPlayer(4)); // black chooses the branch
    }

    // ---- rejoin/state-sync into a move-4 take-over must reconstruct Branch A (MOVE), not BRANCH ----
    // These drive the REAL production rejoin path (LiveGameRoomActivity: applySwapSeats(getMoves()
    // .size()) for the silent seat-swap marker + advanceAfterMove for the bulk move replay), NOT the
    // unused applyRejoinSignal helper. Both event orderings must reconstruct Branch A.
    @Test public void rejoin_takeover_at_move4_marker_before_moves_yields_branch_A() {
        // Marker-first ordering: the silent seat-swap marker arrives before the moves, so
        // applySwapSeats runs with 0 moves and cannot commit Branch A yet; the commit lands
        // once the 4th stone replays (advanceAfterMove).
        RenjuLiveState r = new RenjuLiveState();
        r.applySwapSeats(0);
        assertTrue(r.swapTaken);
        assertFalse("cannot commit Branch A at 0 moves", r.branchChosen);
        for (int n = 1; n <= 4; n++) r.advanceAfterMove(n, true); // bulk replay, isRejoin
        assertTrue("rejoin take-over@move4 must commit Branch A", r.branchChosen);
        assertFalse(r.tenOffer);
        assertFalse("resolved window must not reopen", r.awaitingSwap);
        assertEquals(RenjuLiveState.Phase.MOVE, r.phase(4)); // Branch A: MOVE, not BRANCH
        assertFalse("Offer-10 unreachable on rejoin take-over", r.showOffer10(4));
    }

    @Test public void rejoin_takeover_at_move4_moves_before_marker_yields_branch_A() {
        // Moves-first ordering: the 4 moves replay before the marker, so applySwapSeats(4) sees
        // the board and commits Branch A directly.
        RenjuLiveState r = new RenjuLiveState();
        for (int n = 1; n <= 4; n++) r.advanceAfterMove(n, true);
        r.applySwapSeats(4);
        assertTrue(r.branchChosen);
        assertFalse(r.tenOffer);
        assertFalse(r.awaitingSwap);
        assertEquals(RenjuLiveState.Phase.MOVE, r.phase(4));
        assertFalse(r.showOffer10(4));
    }

    // ---- rejoin into a take-over at a NON-move-4 window reconstructs MOVE (no branch, no modal) ----
    @Test public void rejoin_takeover_at_nonfour_window_yields_move() {
        RenjuLiveState r = new RenjuLiveState();
        r.applySwapSeats(0);                                      // marker first
        for (int n = 1; n <= 2; n++) r.advanceAfterMove(n, true); // took over window 2
        assertFalse("non-move-4 rejoin take-over must not commit a branch", r.branchChosen);
        assertFalse(r.awaitingSwap);
        assertEquals(RenjuLiveState.Phase.MOVE, r.phase(2));
    }

    // ---- reset() clears a fully resolved/completed state (rematch on a reused table) ----
    @Test public void reset_clears_completed_state() {
        RenjuLiveState r = new RenjuLiveState();
        r.complete = true; r.branchChosen = true; r.tenOffer = true; r.selected = 5; r.offered = new int[]{1,2};
        r.reset();
        assertEquals(RenjuLiveState.Phase.MOVE, r.phase(0));
        assertFalse(r.complete); assertFalse(r.branchChosen); assertFalse(r.tenOffer);
        assertNull(r.selected); assertEquals(0, r.offered.length);
    }

    // ---- SELECT1 rejoin: applySelect1 must set Branch-B flags ----
    // applySelect1 implies Branch B (so a SELECT1 rejoin reconstructs correctly).
    @Test public void select1_implies_branch_b_flags() {
        RenjuLiveState r = new RenjuLiveState();
        r.applySelect1(57);
        assertTrue(r.tenOffer);
        assertTrue(r.branchChosen);
        assertEquals(Integer.valueOf(57), r.selected);
    }

    // SELECT1 rejoin: server re-sends ONLY select1 (no offer10), then bulk move replay of 5 moves.
    // Must NOT open a spurious window-5 swap for Branch B.
    @Test public void select1_rejoin_does_not_open_window5() {
        RenjuLiveState r = new RenjuLiveState();
        r.applySelect1(57);             // the re-sent select1 rejoin signal (no prior offer10)
        r.advanceAfterMove(5, true);    // bulk replay of 5 moves, isRejoin
        assertNotEquals(RenjuLiveState.Phase.SWAP, r.phase(5));
        assertEquals(RenjuLiveState.Phase.COMPLETE, r.phase(5));
    }

    // ---- modal buttons by window (renjuModalButtons) ----
    @Test public void modal_buttons_open_move4_window_shows_all_three() {
        RenjuLiveState r = new RenjuLiveState();
        r.awaitingSwap = true;
        assertTrue(r.showSwap(4)); assertTrue(r.showDeclinePlace(4)); assertTrue(r.showOffer10(4));
    }
    @Test public void modal_buttons_window1_no_offer10() {
        RenjuLiveState r = new RenjuLiveState();
        r.awaitingSwap = true;
        assertTrue(r.showSwap(1)); assertTrue(r.showDeclinePlace(1)); assertFalse(r.showOffer10(1));
    }

    // A swap window must not open before move 1 (guards openingPlayer(0) against an invalid seat).
    @Test public void no_window_or_invalid_seat_at_zero_moves() {
        RenjuLiveState r = new RenjuLiveState();
        r.advanceAfterMove(0, true);
        assertFalse(r.awaitingSwap);
        assertEquals(RenjuLiveState.Phase.MOVE, r.phase(0));
        assertEquals(1, r.openingPlayer(0)); // black to play move 1
    }
}
