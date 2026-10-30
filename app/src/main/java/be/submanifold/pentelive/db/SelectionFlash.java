package be.submanifold.pentelive.db;

import be.submanifold.pente.rules.Variant;
import be.submanifold.pente.rules.Variants;

/** Pure helpers for the selected-move flash on the board. */
public final class SelectionFlash {
    public static final int WHITE = 1;
    public static final int BLACK = 2;

    private SelectionFlash() {
    }

    /** Stone colour of the side to move after moveCount moves (matches DBBoardView's placement colour). */
    public static int sideToMove(boolean connect6, int moveCount) {
        if (connect6) {
            int r = moveCount % 4;
            return r == 0 || r == 3 ? WHITE : BLACK;
        }
        return 1 + (moveCount % 2);
    }

    /** Side to move in DB game {@code gameId}: Connect6 pattern, black first for Go/Renju, else white first. */
    public static int sideToMove(int gameId, int moveCount) {
        Variant v = Variants.fromGameId(gameId);
        if (v != null && v.isConnect6()) return sideToMove(true, moveCount);
        if (DbGames.blackMovesFirst(gameId)) return BLACK - (moveCount % 2);
        return sideToMove(false, moveCount);
    }

    /** ARGB fill of the side-to-move stone. */
    public static int stoneArgb(int side) {
        return side == WHITE ? CaptureIcons.P1_FILL : CaptureIcons.P2_FILL;
    }

    /** Linear blend of from to to at phase t (clamped to 0..1), per channel, rounded. */
    public static int colourAt(float t, int from, int to) {
        float p = Math.max(0f, Math.min(1f, t));
        int out = 0;
        for (int shift = 24; shift >= 0; shift -= 8) {
            int a = (from >>> shift) & 0xFF;
            int b = (to >>> shift) & 0xFF;
            out |= Math.round(a + (b - a) * p) << shift;
        }
        return out;
    }

    /** False when the system animator duration scale is 0 (animations off): show a static ring instead. */
    public static boolean animationsEnabled(float animatorDurationScale) {
        return animatorDurationScale != 0f;
    }
}
