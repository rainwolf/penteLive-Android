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
        /** Remove the staged stone, stay on the latest position. */
        CLEAR_PLACED_STONE,
        /** Step one move back (two for Connect6). */
        STEP_BACK
    }

    private BackNavigation() {}

    /**
     * @param swap2Pending   swap2 opening picks in progress (legacy behaviour kept: no clearing)
     * @param renjuPicking   Renju offer collection or SELECTION in progress (legacy behaviour kept)
     */
    public static Action decide(boolean connect6, int connect6Move1, boolean dPente,
                                boolean movesListEmpty, int playedMove,
                                boolean swap2Pending, boolean renjuPicking) {
        if (connect6 && connect6Move1 > -1) return Action.CLEAR_CONNECT6;
        if (dPente && movesListEmpty) return Action.CLEAR_DPENTE_PICK;
        if (playedMove > -1 && !swap2Pending && !renjuPicking) return Action.CLEAR_PLACED_STONE;
        return Action.STEP_BACK;
    }
}
