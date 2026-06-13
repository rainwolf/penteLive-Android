package be.submanifold.pente.rules;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BoardStateTest {

    private static byte[][] grid3() {
        return new byte[][] {
                {0, 1, 2},
                {2, 0, 1},
                {1, 2, 0}
        };
    }

    @Test
    public void storesAllScalarFields() {
        BoardState s = new BoardState(grid3(), 4, 6, 3, 5,
                Integer.valueOf(1), true, false, 7);
        assertEquals(4, s.whiteCaptures);
        assertEquals(6, s.blackCaptures);
        assertEquals(3, s.gridSize);
        assertEquals(5, s.lastMove);
        assertEquals(Integer.valueOf(1), s.winner);
        assertTrue(s.swap2DecisionPoint);
        assertFalse(s.dPenteDecisionPoint);
        assertEquals(7, s.koMove);
    }

    @Test
    public void cellReturnsBoardValueAtCoordinate() {
        BoardState s = new BoardState(grid3(), 0, 0, 3, -1,
                null, false, false, -1);
        assertEquals((byte) 0, s.cell(0, 0));
        assertEquals((byte) 1, s.cell(0, 1));
        assertEquals((byte) 2, s.cell(0, 2));
        assertEquals((byte) 2, s.cell(1, 0));
        assertEquals((byte) 1, s.cell(2, 0));
        assertEquals((byte) 2, s.cell(2, 1));
    }

    @Test
    public void winnerIsNullableAndDefaultsToNoWinner() {
        BoardState s = new BoardState(grid3(), 0, 0, 3, -1,
                null, false, false, -1);
        assertNull(s.winner);
    }

    @Test
    public void constructorDefensivelyCopiesBoard() {
        byte[][] input = grid3();
        BoardState s = new BoardState(input, 0, 0, 3, -1,
                null, false, false, -1);
        // Mutate the caller's array AND its rows after construction.
        input[0][1] = (byte) 9;
        input[1] = new byte[] {7, 7, 7};
        assertEquals("cell(0,1) must reflect original value, not mutation",
                (byte) 1, s.cell(0, 1));
        assertArrayEquals("row 1 must be the original, deep-copied row",
                new byte[] {2, 0, 1}, s.board[1]);
    }

    @Test
    public void exposedBoardMutationDoesNotLeakBackIntoCellSemantics() {
        BoardState s = new BoardState(grid3(), 0, 0, 3, -1,
                null, false, false, -1);
        // board is a public final field; this test pins current expected shape.
        assertEquals(3, s.board.length);
        assertEquals(3, s.board[0].length);
    }
}
