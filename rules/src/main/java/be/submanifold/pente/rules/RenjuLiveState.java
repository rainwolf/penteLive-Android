package be.submanifold.pente.rules;

import java.util.Arrays;

/**
 * Client-side Renju (Taraguchi-10) opening state machine for LIVE play.
 * Pure mirror of the server RenjuState.getOpeningPhase/getCurrentPlayer and the React reducer
 * (docs/superpowers/renju-live/01-react-frontend-spec.md). No Android deps — JVM unit-tested.
 * The live client derives the phase from stone moves + 3 decision-echo events (no server phase field).
 */
public final class RenjuLiveState {

    public enum Phase { SWAP, BRANCH, SELECTION, MOVE, COMPLETE }
    public enum RejoinKind { NONE, SILENT_SWAP, OFFERS, SELECT1 }

    public boolean complete;
    public boolean awaitingSwap;
    public boolean branchChosen;
    public boolean tenOffer;
    public boolean swapTaken;
    public int[] offered = new int[0];
    public Integer selected = null;

    public void reset() {
        complete = false; awaitingSwap = false; branchChosen = false; tenOffer = false;
        swapTaken = false; offered = new int[0]; selected = null;
    }

    /** §3.1 advanceRenjuTrackingAfterMove — runs after every stone lands. */
    public void advanceAfterMove(int numMoves, boolean isRejoin) {
        if (!isRejoin) swapTaken = false; // a fresh incremental move opens a new window
        // Rejoin ordering: the silent seat-swap marker arrives BEFORE the bulk move
        // replay, so applySwapSeats ran with 0 moves and could not commit Branch A.
        // Once the 4th stone lands, a seen take-over marker (swapTaken) at move 4 means
        // Branch A -- commit it so the swapped-in player is NOT re-shown Branch B on
        // reconnect (mirrors React advanceRenjuTrackingAfterMove and iOS
        // advanceRenjuTracking). Gated on swapTaken, so a decline rejoin is unaffected;
        // the moves-first ordering is already handled by applySwapSeats(4) itself.
        if (isRejoin && swapTaken && numMoves == 4) {
            branchChosen = true;
            tenOffer = false;
        }
        boolean windowResolved = swapTaken
                || (numMoves == 4 && (branchChosen || tenOffer || selected != null));
        // A window follows a placed stone: never open one before move 1 (numMoves==0), which would
        // make openingPlayer(0) compute a negative remainder and return an invalid seat.
        boolean windowOpens = !windowResolved
                && ((numMoves >= 1 && numMoves <= 4) || (numMoves == 5 && !tenOffer));
        awaitingSwap = windowOpens;
        complete = !windowOpens && numMoves >= 5;
    }

    /** §3.3 renjuSwap echo. */
    public void applySwap(boolean swap, int numMoves) {
        awaitingSwap = false;                 // ALWAYS clears the window
        if (!swap && numMoves == 4) {         // move-4 decline = Branch A
            branchChosen = true;
            tenOffer = false;
        }
    }

    /** §3.4 renjuOffer10 echo — Branch B. */
    public void applyOffer10(int[] moves) {
        branchChosen = true;
        tenOffer = true;
        offered = Arrays.copyOf(moves, moves.length);
        awaitingSwap = false;
    }

    /** §3.5 renjuSelect1 echo.
     * Selecting only occurs in Branch B, so mirror the server: set Branch-B flags even when
     * arriving without a prior offer10 (SELECT1 rejoin path). This prevents advanceAfterMove(5)
     * from computing windowOpens=true and opening a spurious SWAP phase for Branch-B games. */
    public void applySelect1(int move) {
        branchChosen = true;   // selecting only happens in Branch B
        tenOffer = true;       // mirror the server: Branch B implies tenOffer (needed for SELECT1 rejoin)
        selected = move;
    }

    /** §3.6 swapSeats (renju part): the LIVE take-over signal. The server delivers a move-4
     * take-over as a SEAT-SWAP event (DSGSwapSeatsTableEvent); the renju swap event stays a
     * decision-only echo. A seat swap only ever happens on a TAKE-OVER (a decline changes no
     * seats), so numMoves==4 here unambiguously means "took over move 4 -> Branch A": seats flip,
     * play continues, and Offer-10 / Branch B are unreachable — mirroring backend
     * renjuSwapDecisionMade(true) and RenjuRejoin.decode returning MOVE. Take-overs at any other
     * window merely resolve the window (-> MOVE). Also used as the silent rejoin marker. */
    public void applySwapSeats(int numMoves) {
        awaitingSwap = false;
        swapTaken = true;
        if (numMoves == 4) {          // move-4 take-over = Branch A (seats flip, play continues)
            branchChosen = true;
            tenOffer = false;
        }
    }

    /** §3.4 rejoin: apply the single phase signal BEFORE the bulk move replay. */
    public void applyRejoinSignal(RejoinKind kind, int numMoves) {
        switch (kind) {
            case SILENT_SWAP: applySwapSeats(numMoves); break;  // n==4 take-over => Branch A (MOVE); else MOVE
            case OFFERS:                                    // offered[] arrives on the re-sent offer10 echo
            case SELECT1:                                   // selected arrives on the re-sent select1 echo
            case NONE:
            default: break;
        }
    }

    /** §2 renjuPhase. */
    public Phase phase(int numMoves) {
        if (complete) return Phase.COMPLETE;
        if (awaitingSwap) return Phase.SWAP;
        if (numMoves == 4 && !branchChosen) return Phase.BRANCH;
        if (numMoves == 4 && branchChosen && tenOffer && offered.length == 10 && selected == null) {
            return Phase.SELECTION;
        }
        return Phase.MOVE;
    }

    /** renjuOpeningPlayer — returns 1 (black) or 2 (white) during the opening, else 0 (caller: parity). */
    public int openingPlayer(int numMoves) {
        if (complete) return 0;
        int n = numMoves;
        if (awaitingSwap) {
            int lastColor = ((n - 1) % 2) + 1;
            return 3 - lastColor;
        }
        if (branchChosen && tenOffer && n == 4) {
            if (offered.length < 10) return 1; // black offering
            if (selected == null) return 2;    // white selecting
        }
        if (n == 4 && !branchChosen) return 1; // black chooses branch / plays move 5
        return (n % 2) + 1;
    }

    public int boxRadius(int numMoves) {
        return (numMoves >= 1 && numMoves <= 4) ? numMoves : 0;
    }

    public boolean isSwapChoice(int n)   { return phase(n) == Phase.SWAP; }
    public boolean isBranchChoice(int n) { return phase(n) == Phase.BRANCH; }
    public boolean isSelection(int n)    { return phase(n) == Phase.SELECTION; }

    // renjuModalButtons
    public boolean showSwap(int n)         { return isSwapChoice(n); }
    public boolean showDeclinePlace(int n) { return isSwapChoice(n) || isBranchChoice(n); }
    public boolean showOffer10(int n)      { return isBranchChoice(n) || (isSwapChoice(n) && n == 4); }
}
