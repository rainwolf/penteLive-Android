package be.submanifold.pentelive;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;

import be.submanifold.pentelive.liveGameRoom.Table;

/**
 * Task 1 (Android renju-pass-draw plan): a renju live pass arrives as move 225
 * (== gridSize*gridSize) over a {@code dsgMoveTableEvent}, which the production
 * dispatcher ({@code LiveGameRoomActivity#updateTableMove}) routes through the
 * single-move path ({@code move != 0 -> Table.addMove}) since 225 != 0.
 *
 * <p>{@code Table.abstractBoard} is a fixed 19x19 array regardless of the game's
 * logical grid size. Renju plays on a 15x15 grid, so an unguarded write for move
 * 225 computes move_i=15, move_j=0 -- IN BOUNDS of the 19x19 array. That does not
 * crash; it silently places a phantom stone at abstractBoard[15][0]. This test
 * therefore asserts the *entire* board is byte-for-byte unchanged after a pass
 * (not merely "no exception"), plus move-list growth, {@code isPass}, and turn
 * parity (which {@code RenjuLiveState.advanceAfterMove(numMoves, ...)} and
 * {@code Table.currentColor()} both derive from {@code moves.size()}).
 */
public class LiveTablePassTest {

    /** A renju table (game id 31) past the opening: 6 stones placed via a bulk replay. */
    private Table renjuTable() {
        Table t = new Table();
        t.setGame(31); // Renju; Table.setGame must size gridSize=15 (Variant.RENJU) for isPass to work.
        t.addMoves(Arrays.asList(112, 113, 114, 115, 116, 117)); // bulk replay: 6-stone opening done
        return t;
    }

    private static byte[][] snapshotBoard(Table t) {
        byte[][] src = t.abstractBoard;
        byte[][] copy = new byte[src.length][];
        for (int i = 0; i < src.length; i++) {
            copy[i] = src[i].clone();
        }
        return copy;
    }

    @Test
    public void passMoveDoesNotTouchBoardAndKeepsParity() {
        Table t = renjuTable();
        assertEquals(15, t.getGridSize());
        assertTrue("225 must be recognized as the renju pass sentinel (15*15)", t.isPass(225));

        byte[][] before = snapshotBoard(t);
        int movesBefore = t.getMoves().size();
        int colorBefore = t.currentColor();

        // Live single-move path (mirrors LiveGameRoomActivity#updateTableMove: move != 0 -> addMove).
        t.addMove(225); // pass -- must not throw, and must not touch the board

        assertEquals("pass must enter the move list", movesBefore + 1, t.getMoves().size());
        assertTrue(t.isPass(t.getMoves().get(t.getMoves().size() - 1)));

        byte[][] after = snapshotBoard(t);
        for (int i = 0; i < before.length; i++) {
            assertArrayEquals("row " + i + " must be unchanged by a pass move", before[i], after[i]);
        }

        // Turn parity: a pass still consumes a moves-list slot, so whose turn it is must flip,
        // exactly as it would after any ordinary move.
        int expectedAfter = (colorBefore == 1) ? 2 : 1;
        assertEquals(expectedAfter, t.currentColor());
    }

    @Test
    public void nonRenjuMove225IsAnOrdinaryBoardCell() {
        // Regression guard: isPass must be variant-aware, not a hardcoded "225". For a 19x19
        // game, move 225 is a legitimate in-bounds cell (row 11, col 16), not a pass -- it must
        // still place a stone rather than being silently swallowed as a pass.
        Table t = new Table();
        t.setGame(1); // Pente, 19x19
        assertEquals(19, t.getGridSize());
        assertTrue("move 225 is a real cell on a 19-wide board, not a pass", !t.isPass(225));

        t.addMove(225);
        assertTrue("move 225 must place an ordinary stone on a 19-wide board",
                t.abstractBoard[225 / 19][225 % 19] != 0);
    }
}
