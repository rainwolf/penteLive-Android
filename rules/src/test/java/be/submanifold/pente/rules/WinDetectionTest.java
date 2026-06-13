package be.submanifold.pente.rules;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class WinDetectionTest {

    private final PenteRules rules = new DefaultPenteRules();

    private static int pos(int row, int col) {
        return row * 19 + col;
    }

    /** Build a BoardState with no captures and no winner. */
    private static BoardState boardWith(byte[][] board, int lastMove) {
        return new BoardState(board, 0, 0, 19, lastMove, null, false, false, -1);
    }

    /** Build a BoardState with specific capture counts and an empty board. */
    private static BoardState captureState(int whiteCaptures, int blackCaptures) {
        return new BoardState(new byte[19][19], whiteCaptures, blackCaptures, 19, -1, null, false, false, -1);
    }

    // ── Five-in-a-row ─────────────────────────────────────────────────────────

    @Test
    public void horizontalExactlyFiveWins() {
        byte[][] b = new byte[19][19];
        for (int c = 5; c <= 9; c++) b[5][c] = 1;
        assertTrue(rules.isWin(boardWith(b, pos(5, 9)), 1, pos(5, 9)));
    }

    @Test
    public void horizontalSixWins() {
        byte[][] b = new byte[19][19];
        for (int c = 5; c <= 10; c++) b[5][c] = 1;
        assertTrue(rules.isWin(boardWith(b, pos(5, 10)), 1, pos(5, 10)));
    }

    @Test
    public void horizontalFourDoesNotWin() {
        byte[][] b = new byte[19][19];
        for (int c = 5; c <= 8; c++) b[5][c] = 1;
        assertFalse(rules.isWin(boardWith(b, pos(5, 8)), 1, pos(5, 8)));
    }

    @Test
    public void verticalFiveWins() {
        byte[][] b = new byte[19][19];
        for (int r = 5; r <= 9; r++) b[r][7] = 2;
        assertTrue(rules.isWin(boardWith(b, pos(9, 7)), 2, pos(9, 7)));
    }

    @Test
    public void diagonalDownRightFiveWins() {
        byte[][] b = new byte[19][19];
        for (int k = 0; k < 5; k++) b[5 + k][5 + k] = 1;
        assertTrue(rules.isWin(boardWith(b, pos(9, 9)), 1, pos(9, 9)));
    }

    @Test
    public void antiDiagonalFiveWinsFromMiddleStone() {
        byte[][] b = new byte[19][19];
        b[5][9] = 1; b[6][8] = 1; b[7][7] = 1; b[8][6] = 1; b[9][5] = 1;
        // lastMove is the middle stone — must count both arms of the line
        assertTrue(rules.isWin(boardWith(b, pos(7, 7)), 1, pos(7, 7)));
    }

    /**
     * Legacy detectPente does NOT detect fives on row 0 — preserved quirk.
     * The walk condition is {@code r > 0 && r < gridSize && c > 0 && c < gridSize};
     * when the placed stone is on row 0, {@code r > 0} is false from the start so
     * the horizontal walk loop never runs, leaving penteCounter == 1.
     */
    @Test
    public void topEdgeRowFiveNotDetectedLegacyQuirk() {
        byte[][] b = new byte[19][19];
        for (int c = 0; c <= 4; c++) b[0][c] = 1;
        assertFalse(rules.isWin(boardWith(b, pos(0, 4)), 1, pos(0, 4)));
    }

    /**
     * Legacy detectPente does NOT detect fives on column 0 — preserved quirk.
     * The walk condition checks {@code c > 0}; when the placed stone is on col 0,
     * this is false from the start so the vertical walk loop never runs.
     */
    @Test
    public void leftEdgeColumnFiveNotDetectedLegacyQuirk() {
        byte[][] b = new byte[19][19];
        for (int r = 0; r <= 4; r++) b[r][0] = 2;
        assertFalse(rules.isWin(boardWith(b, pos(4, 0)), 2, pos(4, 0)));
    }

    /** Legacy detects fives on row 18 — {@code r < gridSize} is true at index 18. */
    @Test
    public void bottomEdgeRowFiveWins() {
        byte[][] b = new byte[19][19];
        for (int c = 14; c <= 18; c++) b[18][c] = 1;
        assertTrue(rules.isWin(boardWith(b, pos(18, 18)), 1, pos(18, 18)));
    }

    /** Legacy detects a diagonal five through the bottom-right corner (18,18). */
    @Test
    public void bottomRightCornerDiagonalFiveWins() {
        byte[][] b = new byte[19][19];
        for (int k = 0; k < 5; k++) b[14 + k][14 + k] = 1;
        assertTrue(rules.isWin(boardWith(b, pos(18, 18)), 1, pos(18, 18)));
    }

    @Test
    public void wrongColorAtLastMoveDoesNotWin() {
        byte[][] b = new byte[19][19];
        for (int c = 5; c <= 9; c++) b[5][c] = 1;
        // Board has white (1) five-in-a-row but we ask about black (2)
        assertFalse(rules.isWin(boardWith(b, pos(5, 9)), 2, pos(5, 9)));
    }

    // ── Capture wins ──────────────────────────────────────────────────────────
    // whiteCaptures = white stones lost → black (2) wins when == 10.
    // blackCaptures = black stones lost → white (1) wins when == 10.
    // Matches Game.java:1252-1259.

    @Test
    public void whiteStonesLostTenMeansBlackWins() {
        BoardState s = captureState(10, 0);
        assertTrue(rules.isWin(s, 2, -1));
        assertFalse(rules.isWin(s, 1, -1));
    }

    @Test
    public void blackStonesLostTenMeansWhiteWins() {
        BoardState s = captureState(0, 10);
        assertTrue(rules.isWin(s, 1, -1));
        assertFalse(rules.isWin(s, 2, -1));
    }

    @Test
    public void eightCapturesIsNotYetAWin() {
        BoardState s = captureState(8, 0);
        assertFalse(rules.isWin(s, 2, -1));
    }
}
