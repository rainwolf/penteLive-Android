package be.submanifold.pente.rules;

/**
 * Immutable snapshot of a Pente-family board position.
 *
 * Cell encoding in {@link #board}: 0 empty, 1 white, 2 black, -1 forbidden.
 * Pente-only by design; Go positions are out of scope for v1.
 * // TODO(rules): GoRules seam — Go captures/territory are not modeled here.
 */
public final class BoardState {

    public final byte[][] board;
    public final int whiteCaptures;
    public final int blackCaptures;
    public final int gridSize;
    public final int lastMove;
    /** Winning color (1 white, 2 black) or {@code null} when there is no winner yet. */
    public final Integer winner;
    public final boolean swap2DecisionPoint;
    public final boolean dPenteDecisionPoint;
    public final int koMove;

    public BoardState(byte[][] board,
                      int whiteCaptures,
                      int blackCaptures,
                      int gridSize,
                      int lastMove,
                      Integer winner,
                      boolean swap2DecisionPoint,
                      boolean dPenteDecisionPoint,
                      int koMove) {
        this.board = deepCopy(board);
        this.whiteCaptures = whiteCaptures;
        this.blackCaptures = blackCaptures;
        this.gridSize = gridSize;
        this.lastMove = lastMove;
        this.winner = winner;
        this.swap2DecisionPoint = swap2DecisionPoint;
        this.dPenteDecisionPoint = dPenteDecisionPoint;
        this.koMove = koMove;
    }

    /** Value at row {@code i}, column {@code j}. */
    public byte cell(int i, int j) {
        return board[i][j];
    }

    private static byte[][] deepCopy(byte[][] src) {
        byte[][] copy = new byte[src.length][];
        for (int i = 0; i < src.length; i++) {
            byte[] row = src[i];
            copy[i] = new byte[row.length];
            System.arraycopy(row, 0, copy[i], 0, row.length);
        }
        return copy;
    }
}
