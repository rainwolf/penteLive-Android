package be.submanifold.pentelive;

import be.submanifold.pente.rules.BoardState;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;

/**
 * Regression test for the Go dead-stone snapshot-aliasing bug (PR #7 review, Finding 1).
 *
 * <p>Since the rules-engine refactor, {@link BoardView} reads {@code game.getState().board}
 * — a cached <em>deep copy</em> — instead of aliasing the live {@code abstractBoard}.
 * {@link Game#processDeadStone(int)} mutates {@code abstractBoard} in place during the Go
 * scoring "mark dead stones" phase but used to leave the cached {@link BoardState} stale,
 * so toggling a dead stone produced no visual change on the next {@code invalidate()}.
 *
 * <p>This test asserts the invariant that {@code getState()} reflects an in-place
 * {@code processDeadStone()} mutation. It fails before the fix (stale snapshot) and
 * passes after {@code processDeadStone()} refreshes {@code state}.
 */
public class GoDeadStoneSnapshotTest {

    private static final int GRID = 19;

    @Test
    public void markingStoneDead_refreshesGetStateSnapshot() throws Exception {
        Game game = makeGoGame();
        invokePrivate(game, "initGo");
        invokePrivate(game, "resetAbstractBoard");

        // Place a black stone (encoding 2) and prime the getState() cache with it present.
        int i = 3, j = 4, move = i * GRID + j;
        byte[][] board = abstractBoard(game);
        board[i][j] = 2;
        assertEquals("precondition: primed snapshot holds the live stone",
                2, game.getState().cell(i, j));

        // Mark it dead: processDeadStone() must clear the live board AND refresh the snapshot.
        game.processDeadStone(move);

        assertEquals("live board cleared by processDeadStone", 0, board[i][j]);
        assertEquals("getState() snapshot must reflect the dead-stone mutation",
                0, game.getState().cell(i, j));
    }

    @Test
    public void restoringStoneDead_refreshesGetStateSnapshot() throws Exception {
        Game game = makeGoGame();
        invokePrivate(game, "initGo");
        invokePrivate(game, "resetAbstractBoard");

        // An empty cell that the opponent (player 2) previously marked dead; restoring it
        // puts a white stone (encoding 1) back. The snapshot must show the restored stone.
        int i = 5, j = 6, move = i * GRID + j;
        game.getGoDeadStonesByPlayer().get(2).add(move);
        game.getState(); // prime cache with the empty cell

        game.processDeadStone(move);

        byte[][] board = abstractBoard(game);
        assertEquals("live board restored to white", 1, board[i][j]);
        assertEquals("getState() snapshot must reflect the restored stone",
                1, game.getState().cell(i, j));
    }

    // ─── helpers (mirror PenteRulesEquivalenceTest reflection harness) ─────────

    private static Game makeGoGame() {
        return new Game(
                "go-test-id", "go-set-id", "Go", "opponent", "1500",
                "white", "5", "Not Rated", "false", "1", "0");
    }

    private static void invokePrivate(Game game, String name) throws Exception {
        Method m = Game.class.getDeclaredMethod(name);
        m.setAccessible(true);
        m.invoke(game);
    }

    private static byte[][] abstractBoard(Game game) throws Exception {
        Field f = Game.class.getDeclaredField("abstractBoard");
        f.setAccessible(true);
        return (byte[][]) f.get(game);
    }
}
