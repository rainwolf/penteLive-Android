package be.submanifold.pentelive;

import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Characterization ("golden") tests for the pure board-logic workers in Game.
 *
 * These tests lock the CURRENT behaviour of the rules engine before extraction
 * into the :rules module (Task 4). Any future drift in the old Game code will
 * cause a test failure here, and the matching :rules test will prove the new
 * engine produces identical output.
 *
 * <h3>Approach</h3>
 * <ol>
 *   <li>Construct {@link Game} via its Android-free 11-arg string constructor.</li>
 *   <li>Inject a recorded move list via reflection (no public setter exists).</li>
 *   <li>Call the variant-appropriate private {@code replay*Game(int)} worker
 *       directly via reflection — this bypasses the {@code BoardView} wrapper
 *       entirely.</li>
 *   <li>Serialise {@code abstractBoard}, {@code whiteCaptures}, and
 *       {@code blackCaptures} and assert equality against committed golden
 *       files in {@code rules/src/test/resources/golden/}.</li>
 * </ol>
 *
 * <h3>Variant → worker dispatch</h3>
 * (Mirrored from {@code Game.replayGameUntilMove}, which owns the authoritative
 * mapping.)
 * <pre>
 *   Gomoku / Speed Gomoku                  → replayGomokuGame(until)
 *   Pente / Boat-Pente / D-Pente
 *     / Speed Pente / Swap2-Pente          → replayPenteGame(until)
 *   Keryo-Pente / DK-Pente
 *     / Speed Keryo-Pente / Swap2-Keryo    → replayKeryoPenteGame(until)
 *   Connect6 / Speed Connect6             → replayConnect6Game(until)
 *   G-Pente / Speed G-Pente               → replayGPenteGame(until)
 *   Poof-Pente / Speed Poof-Pente         → replayPoofPenteGame(until)
 *   O-Pente (contains)                    → replayOPenteGame(until)
 * </pre>
 *
 * <h3>Move encoding</h3>
 * {@code index = row * 19 + col} for all non-Go variants on a 19×19 board.
 */
public class GameCharacterizationTest {

    // ─── scenario tests ──────────────────────────────────────────────────────

    /**
     * Pente pair capture: Black flanks exactly 2 White stones horizontally.
     *
     * <pre>
     * moves = [180, 0, 1, 19, 2, 3]
     *   i=0 W → (9, 9)  centre placeholder
     *   i=1 B → (0, 0)  B1 anchor
     *   i=2 W → (0, 1)  W1 victim
     *   i=3 B → (1, 0)  placeholder
     *   i=4 W → (0, 2)  W2 victim
     *   i=5 B → (0, 3)  B2 trigger: j−3=0=B, j−1=2=W, j−2=1=W → CAPTURE
     * </pre>
     * Expected: blackCaptures=2, cells (0,1) and (0,2) zeroed.
     */
    @Test
    public void pentePairCapture() throws Exception {
        Game g = makeGame("Pente");
        setMoves(g, 180, 0, 1, 19, 2, 3);
        callReplay(g, "replayPenteGame", 6);
        assertMatchesGolden("pente_pair_capture", g);
    }

    /**
     * Gomoku: no capture mechanic, pure stone placement.
     *
     * <pre>
     * moves = [180, 171, 181, 172, 182]
     *   W at (9,9),(9,10),(9,11);  B at (9,0),(9,1)
     * </pre>
     * Expected: all 5 stones present, whiteCaptures=0, blackCaptures=0.
     */
    @Test
    public void gomokuNoCapture() throws Exception {
        Game g = makeGame("Gomoku");
        setMoves(g, 180, 171, 181, 172, 182);
        callReplay(g, "replayGomokuGame", 5);
        assertMatchesGolden("gomoku_no_capture", g);
    }

    /**
     * Keryo-Pente trio capture: Black flanks exactly 3 White stones horizontally.
     *
     * <pre>
     * moves = [180, 177, 178, 0, 179, 181]
     *   W at (9,9),(9,7),(9,8);  B anchor at (9,6) and placeholder at (0,0)
     *   i=5 B → (9,10): j−4=6=B, j−1=9=W, j−2=8=W, j−3=7=W → TRIO CAPTURE
     * </pre>
     * Expected: blackCaptures=3, cells (9,7)(9,8)(9,9) zeroed.
     */
    @Test
    public void keryoTrioCapture() throws Exception {
        Game g = makeGame("Keryo-Pente");
        setMoves(g, 180, 177, 178, 0, 179, 181);
        callReplay(g, "replayKeryoPenteGame", 6);
        assertMatchesGolden("keryo_trio_capture", g);
    }

    /**
     * Poof-Pente poof: a pair of White stones is removed when sandwiched by Black.
     *
     * <pre>
     * moves = [179, 178, 176, 181, 180]
     *   W at (9,8);  B at (9,7) and (9,10);  W placeholder at (9,5)
     *   i=4 W → (9,9): detectPoof sees B at j−2=7 and j+1=10 flanking
     *                   the pair W(j−1=8) + new W(j=9) → POOF
     * </pre>
     * Expected: whiteCaptures=1, cells (9,8) and (9,9) zeroed.
     */
    @Test
    public void poofCapture() throws Exception {
        Game g = makeGame("Poof-Pente");
        setMoves(g, 179, 178, 176, 181, 180);
        callReplay(g, "replayPoofPenteGame", 5);
        assertMatchesGolden("poof_poof_capture", g);
    }

    /**
     * Pente capture-win: Black completes 5 separate pair captures → blackCaptures=10.
     *
     * <p>22-move sequence interleaves 5 independent rows (rows 0–4), each row
     * following the pattern B at col 0 (anchor), W at col 1, W at col 2,
     * B at col 3 (trigger). W placeholder at (9,9); B placeholder at (5,0).
     *
     * <pre>
     * moves = [180,0,1,19,2,3, 20,38,21,22, 39,57,40,41, 58,76,59,60, 77,95,78,79]
     * </pre>
     * Expected: blackCaptures=10, whiteCaptures=0.
     */
    @Test
    public void penteCaptureWin() throws Exception {
        Game g = makeGame("Pente");
        setMoves(g,
                180,  0,  1, 19,  2,  3,
                 20, 38, 21, 22,
                 39, 57, 40, 41,
                 58, 76, 59, 60,
                 77, 95, 78, 79);
        callReplay(g, "replayPenteGame", 22);
        assertMatchesGolden("pente_capture_win", g);
    }

    /**
     * Connect6 colour assignment: i%4 ∈ {0,3} → White(1), else Black(2).
     *
     * <pre>
     * moves = [180, 100, 101, 181, 182]
     *   i=0 W→(9,9);  i=1 B→(5,5);  i=2 B→(5,6);
     *   i=3 W→(9,10); i=4 W→(9,11)
     * </pre>
     * Expected: 5 stones on board, 0 captures each.
     */
    @Test
    public void connect6Placement() throws Exception {
        Game g = makeGame("Connect6");
        setMoves(g, 180, 100, 101, 181, 182);
        callReplay(g, "replayConnect6Game", 5);
        assertMatchesGolden("connect6_placement", g);
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    /**
     * Constructs a Game with the Android-free 11-arg string constructor.
     * Passing "Not Rated" ensures {@code rated()} returns {@code false},
     * suppressing the centre-restriction mark in the replay workers.
     * Passing "opponent" (not "computer") suppresses the computer-win check.
     */
    private static Game makeGame(String gameType) {
        return new Game(
                "test-id",    // gameID
                "set-id",     // setID
                gameType,     // gameType
                "opponent",   // opponentName  — must NOT be "computer"
                "1500",       // opponentRating
                "white",      // myColor
                "5",          // remainingTime
                "Not Rated",  // ratedNot  — keeps rated()==false
                "false",      // privateGame
                "1",          // nameColor
                "0"           // crown
        );
    }

    /** Injects a move list into the private {@code mMovesList} field. */
    private static void setMoves(Game game, Integer... moves) throws Exception {
        Field f = Game.class.getDeclaredField("mMovesList");
        f.setAccessible(true);
        f.set(game, new ArrayList<>(Arrays.asList(moves)));
    }

    /** Reflectively invokes a private {@code replay*Game(int)} worker. */
    private static void callReplay(Game game, String methodName, int until) throws Exception {
        Method m = Game.class.getDeclaredMethod(methodName, int.class);
        m.setAccessible(true);
        m.invoke(game, until);
    }

    /**
     * Serialises the post-replay game state into the golden file format:
     * 19 lines of space-separated cell values followed by the capture counts.
     * Cell values: 0 = empty, 1 = White, 2 = Black, −1 = restricted.
     */
    private static String serialise(Game game) throws Exception {
        byte[][] board = readBoard(game);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 19; i++) {
            for (int j = 0; j < 19; j++) {
                if (j > 0) sb.append(' ');
                sb.append(board[i][j]);
            }
            sb.append('\n');
        }
        sb.append("whiteCaptures=").append(readInt(game, "whiteCaptures")).append('\n');
        sb.append("blackCaptures=").append(readInt(game, "blackCaptures")).append('\n');
        return sb.toString();
    }

    /** Reflectively reads a now-private int field (capture counts) from {@link Game}. */
    private static int readInt(Game game, String field) throws Exception {
        Field f = Game.class.getDeclaredField(field);
        f.setAccessible(true);
        return f.getInt(game);
    }

    /** Reflectively reads the now-private {@code abstractBoard} field from {@link Game}. */
    private static byte[][] readBoard(Game game) throws Exception {
        Field f = Game.class.getDeclaredField("abstractBoard");
        f.setAccessible(true);
        return (byte[][]) f.get(game);
    }

    /**
     * Locates the {@code rules/src/test/resources/golden/} directory by
     * walking up from the test working directory.  This handles both the
     * module-root ({@code …/app/}) and project-root ({@code …/penteLive-Android/})
     * working-directory conventions across different Gradle / IDE environments.
     */
    private static Path findGoldenDir() {
        Path cwd = Paths.get("").toAbsolutePath();
        for (Path p = cwd; p != null; p = p.getParent()) {
            Path candidate = p.resolve("rules/src/test/resources/golden");
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
        }
        fail("Cannot locate rules/src/test/resources/golden/ — searched from " + cwd);
        throw new AssertionError("unreachable");
    }

    private static String readGolden(String name) throws IOException {
        Path file = findGoldenDir().resolve(name + ".txt");
        if (!Files.exists(file)) {
            fail("Golden file not found: " + file.toAbsolutePath()
                    + "\nRun the test once with golden generation enabled to create it.");
        }
        return new String(Files.readAllBytes(file));
    }

    private static void assertMatchesGolden(String name, Game game) throws Exception {
        String actual = serialise(game);
        String expected = readGolden(name);
        assertEquals("Golden mismatch for '" + name + "'", expected, actual);
    }
}
