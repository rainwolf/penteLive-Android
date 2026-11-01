package be.submanifold.pentelive;

/**
 * Pure decision for what a "back" (button or swipe) does on the live board. A tentative
 * (placed but not submitted) stone is removed first; only then does back step through moves.
 */
public final class BackNavigation {
    public enum Action {
        /** Clear the Connect6 pending pair (first stone and any staged second stone). */
        CLEAR_CONNECT6,
        /** Clear one dPente opening pick (or nothing if none; never steps back). */
        CLEAR_DPENTE_PICK,
        /** Remove the last swap2 opening pick, stay put. */
        CLEAR_SWAP2_PICK,
        /** Remove the last Renju SELECTION stone (6th, else 5th), stay on the latest position. */
        CLEAR_RENJU_SELECTION_PICK,
        /** Remove the last Renju OFFERS pick, stay on the latest position. */
        CLEAR_RENJU_OFFER_PICK,
        /** Remove the staged stone, stay on the latest position. */
        CLEAR_PLACED_STONE,
        /** Step one move back (two for Connect6). */
        STEP_BACK
    }

    private BackNavigation() {}

    /**
     * Pick counts are negative when that pick phase is not active, and 0 when it is active with
     * nothing picked (legacy behaviour kept: step back, never clear a staged stone).
     * @param swap2Picks swap2 opening picks (empty-list opening, or the 2-stone pass)
     * @param renjuSelectionPicks Renju SELECTION stones picked (5th, 6th)
     * @param renjuOfferPicks Renju OFFERS picks
     */
    public static Action decide(boolean connect6, int connect6Move1, boolean dPente, boolean movesListEmpty,
                                int playedMove, int swap2Picks, int renjuSelectionPicks, int renjuOfferPicks) {
        if (connect6 && connect6Move1 > -1) return Action.CLEAR_CONNECT6;
        if (dPente && movesListEmpty) return Action.CLEAR_DPENTE_PICK;
        if (swap2Picks > 0) return Action.CLEAR_SWAP2_PICK;
        if (renjuSelectionPicks > 0) return Action.CLEAR_RENJU_SELECTION_PICK;
        if (renjuOfferPicks > 0) return Action.CLEAR_RENJU_OFFER_PICK;
        boolean picking = swap2Picks >= 0 || renjuSelectionPicks >= 0 || renjuOfferPicks >= 0;
        if (playedMove > -1 && !picking) return Action.CLEAR_PLACED_STONE;
        return Action.STEP_BACK;
    }
}
