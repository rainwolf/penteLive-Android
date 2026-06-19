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

    @Test public void branch_phase_after_move4_window_resolved_without_branch() {
        RenjuLiveState r = new RenjuLiveState();
        r.awaitingSwap = false; // move-4 window resolved (e.g. take-over)
        r.swapTaken = true;
        assertEquals(RenjuLiveState.Phase.BRANCH, r.phase(4));
        assertEquals(1, r.openingPlayer(4)); // black chooses branch
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

    // ---- take-over at window 4 -> BRANCH (no swap button) ----
    @Test public void takeover_at_window4_yields_branch() {
        RenjuLiveState r = new RenjuLiveState();
        for (int n = 1; n <= 3; n++) { r.advanceAfterMove(n, false); r.applySwap(false, n); }
        r.advanceAfterMove(4, false); assertEquals(RenjuLiveState.Phase.SWAP, r.phase(4));
        r.applySwapSeats(); // take-over
        assertFalse(r.awaitingSwap); assertTrue(r.swapTaken); assertFalse(r.branchChosen);
        assertEquals(RenjuLiveState.Phase.BRANCH, r.phase(4));
        assertFalse(r.showSwap(4));         // no swap button post take-over
        assertTrue(r.showDeclinePlace(4));
        assertTrue(r.showOffer10(4));
    }

    // ---- rejoin: bulk replay must not reopen a resolved window ----
    @Test public void rejoin_does_not_reopen_resolved_window() {
        RenjuLiveState r = new RenjuLiveState();
        r.applyRejoinSignal(RenjuLiveState.RejoinKind.SILENT_SWAP, 4); // a window resolved before join
        r.advanceAfterMove(4, true); // bulk replay of 4 moves, isRejoin
        assertNotEquals(RenjuLiveState.Phase.SWAP, r.phase(4)); // no spurious swap modal
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
}
