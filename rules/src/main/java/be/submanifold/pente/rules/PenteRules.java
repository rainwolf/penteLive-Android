package be.submanifold.pente.rules;

import java.util.List;

/** Deep, pure rules interface — the implementation hides all capture/win detection. */
public interface PenteRules {
    /** Recompute the board from scratch by replaying moves[0..untilMove). */
    BoardState replay(List<Integer> moves, Variant v, int untilMove);

    /** True if {@code color} (1=white, 2=black) has won given the last move. Caller decides WHEN to evaluate. */
    boolean isWin(BoardState s, int color, int lastMove);
}
