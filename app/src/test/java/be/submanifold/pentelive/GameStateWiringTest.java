package be.submanifold.pentelive;

import be.submanifold.pente.rules.BoardState;
import be.submanifold.pente.rules.DefaultPenteRules;
import be.submanifold.pente.rules.Variant;

import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Verifies the {@code Game.getState()} wiring added in Task 6.
 *
 * <p>{@code replayGameUntilMove} cannot run in a plain JVM test (it dereferences
 * an Android {@code BoardView}), so this test drives the headless seam
 * {@code finishBoardState(int, Variant, boolean)} that it delegates to — exactly
 * the code path the production replay takes — plus the private legacy workers,
 * via reflection (the same technique as {@link GameCharacterizationTest}).
 *
 * <p>Both paths must leave {@link Game#getState()} non-null with a board and
 * capture counts that match the public {@code abstractBoard} /
 * {@code whiteCaptures} / {@code blackCaptures} fields.
 */
public class GameStateWiringTest {

    /**
     * Delegated variant (PENTE, not rated): {@code finishBoardState(.., true)}
     * recomputes the board via the rules engine and mirrors it into the public
     * fields. {@code getState()} must equal both the engine output and the fields.
     *
     * <p>Move list is the {@code pente_pair_capture} scenario (blackCaptures=2).
     */
    @Test
    public void delegatedPenteMirrorsEngineAndFields() throws Exception {
        Game g = makeGame("Pente");
        Integer[] moves = {180, 0, 1, 19, 2, 3};
        setMoves(g, moves);

        callFinishBoardState(g, moves.length, Variant.PENTE, true);

        BoardState s = g.getState();
        assertNotNull("getState() must be populated after a delegated replay", s);

        // getState() mirrors the (now-private) fields exactly.
        assertEquals(readInt(g, "whiteCaptures"), s.whiteCaptures);
        assertEquals(readInt(g, "blackCaptures"), s.blackCaptures);
        byte[][] board = readBoard(g);
        for (int i = 0; i < 19; i++) {
            assertArrayEquals("row " + i, board[i], s.board[i]);
        }

        // The delegated path genuinely used the engine: its result matches a
        // fresh engine replay of the same moves.
        BoardState expected = new DefaultPenteRules().replay(toList(moves), Variant.PENTE, moves.length);
        assertEquals(expected.whiteCaptures, s.whiteCaptures);
        assertEquals(expected.blackCaptures, s.blackCaptures);
        // pente_pair_capture scenario: Black flanks 2 White → 2 White stones removed.
        assertEquals(2, s.whiteCaptures);
        assertEquals(0, s.blackCaptures);
        for (int i = 0; i < 19; i++) {
            assertArrayEquals("row " + i, expected.board[i], s.board[i]);
        }
    }

    /**
     * Non-delegated variant (G-Pente): the legacy worker fills the public fields,
     * then {@code finishBoardState(.., false)} snapshots them into {@code state}.
     * {@code getState()} must equal the public fields.
     */
    @Test
    public void legacyGPenteSnapshotsFields() throws Exception {
        Game g = makeGame("G-Pente");
        Integer[] moves = {180, 0, 1, 19, 2, 3};
        setMoves(g, moves);

        callReplay(g, "replayGPenteGame", moves.length); // legacy worker fills the fields
        callFinishBoardState(g, moves.length, Variant.G_PENTE, false);

        assertStateMirrorsFields(g);
    }

    /**
     * Non-delegated because rated (a rated PENTE game): the legacy worker runs
     * (including the move-2 forbidden-cell marking the engine does not model),
     * then {@code finishBoardState(.., false)} snapshots the fields.
     */
    @Test
    public void legacyRatedPenteSnapshotsFields() throws Exception {
        Game g = makeGame("Pente", "Rated");
        Integer[] moves = {180, 0}; // move 2 triggers the rated forbidden-cell mark
        setMoves(g, moves);

        callReplay(g, "replayPenteGame", moves.length);
        callFinishBoardState(g, moves.length, Variant.PENTE, false);

        assertStateMirrorsFields(g);
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private static void assertStateMirrorsFields(Game g) throws Exception {
        BoardState s = g.getState();
        assertNotNull("getState() must be populated after a legacy replay", s);
        assertEquals(readInt(g, "whiteCaptures"), s.whiteCaptures);
        assertEquals(readInt(g, "blackCaptures"), s.blackCaptures);
        byte[][] board = readBoard(g);
        for (int i = 0; i < 19; i++) {
            assertArrayEquals("row " + i, board[i], s.board[i]);
        }
    }

    /** Reflectively reads a now-private int field (capture counts) from {@link Game}. */
    private static int readInt(Game g, String field) throws Exception {
        Field f = Game.class.getDeclaredField(field);
        f.setAccessible(true);
        return f.getInt(g);
    }

    /** Reflectively reads the now-private {@code abstractBoard} field from {@link Game}. */
    private static byte[][] readBoard(Game g) throws Exception {
        Field f = Game.class.getDeclaredField("abstractBoard");
        f.setAccessible(true);
        return (byte[][]) f.get(g);
    }

    private static Game makeGame(String gameType) {
        return makeGame(gameType, "Not Rated");
    }

    private static Game makeGame(String gameType, String ratedNot) {
        return new Game(
                "test-id",   // gameID
                "set-id",    // setID
                gameType,    // gameType
                "opponent",  // opponentName — must NOT be "computer"
                "1500",      // opponentRating
                "white",     // myColor
                "5",         // remainingTime
                ratedNot,    // ratedNot
                "false",     // privateGame
                "1",         // nameColor
                "0"          // crown
        );
    }

    private static List<Integer> toList(Integer[] moves) {
        return new ArrayList<>(Arrays.asList(moves));
    }

    private static void setMoves(Game game, Integer... moves) throws Exception {
        Field f = Game.class.getDeclaredField("mMovesList");
        f.setAccessible(true);
        f.set(game, new ArrayList<>(Arrays.asList(moves)));
    }

    private static void callReplay(Game game, String methodName, int until) throws Exception {
        Method m = Game.class.getDeclaredMethod(methodName, int.class);
        m.setAccessible(true);
        m.invoke(game, until);
    }

    private static void callFinishBoardState(Game game, int until, Variant v, boolean delegable)
            throws Exception {
        Method m = Game.class.getDeclaredMethod("finishBoardState", int.class, Variant.class, boolean.class);
        m.setAccessible(true);
        m.invoke(game, until, v, delegable);
    }
}
