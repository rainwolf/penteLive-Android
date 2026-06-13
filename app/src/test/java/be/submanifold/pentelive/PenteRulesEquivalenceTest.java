package be.submanifold.pentelive;

import be.submanifold.pente.rules.BoardState;
import be.submanifold.pente.rules.DefaultPenteRules;
import be.submanifold.pente.rules.Variant;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.fail;

/**
 * Differential equivalence test: compares {@link DefaultPenteRules} (new engine) against
 * the legacy {@link Game} replay workers over 500 randomly-generated games per variant.
 *
 * <p>A variant whose test passes is <em>safe to delegate</em> to the new engine — its
 * board state and capture counts are bit-identical to the legacy worker on every game.
 * A failing test reports the variant, game index, and the minimal move list so the
 * divergence can be diagnosed without re-running the full suite.
 *
 * <h3>Variant → legacy worker mapping</h3>
 * <pre>
 *   PENTE        → replayPenteGame
 *   KERYO_PENTE  → replayKeryoPenteGame
 *   POOF_PENTE   → replayPoofPenteGame
 *   G_PENTE      → replayGPenteGame
 *   O_PENTE      → replayOPenteGame
 *   GOMOKU       → replayGomokuGame
 *   CONNECT6     → replayConnect6Game
 * </pre>
 *
 * <h3>Random game generation</h3>
 * Each game is a sequence of DISTINCT cell indices in [0, 361) of length drawn
 * uniformly from [1, 60]. The seed is fixed per-variant so runs are reproducible.
 */
public class PenteRulesEquivalenceTest {

    private static final int GAMES_PER_VARIANT = 500;
    private static final int MIN_MOVES = 1;
    private static final int MAX_MOVES = 60;

    // Fixed per-variant seeds — change these only if you need a different random corpus.
    private static final long SEED_PENTE       = 0xBEEF0001L;
    private static final long SEED_KERYO_PENTE = 0xBEEF0002L;
    private static final long SEED_POOF_PENTE  = 0xBEEF0003L;
    private static final long SEED_G_PENTE     = 0xBEEF0004L;
    private static final long SEED_O_PENTE     = 0xBEEF0005L;
    private static final long SEED_GOMOKU      = 0xBEEF0006L;
    private static final long SEED_CONNECT6    = 0xBEEF0007L;

    // ─── one @Test per variant (independent pass/fail) ────────────────────────

    @Test
    public void pente() throws Exception {
        runVariant(Variant.PENTE, "Pente", "replayPenteGame", SEED_PENTE);
    }

    @Test
    public void keryoPente() throws Exception {
        runVariant(Variant.KERYO_PENTE, "Keryo-Pente", "replayKeryoPenteGame", SEED_KERYO_PENTE);
    }

    @Test
    public void poofPente() throws Exception {
        runVariant(Variant.POOF_PENTE, "Poof-Pente", "replayPoofPenteGame", SEED_POOF_PENTE);
    }

    // G_PENTE diverges from the legacy worker: replayGPenteGame marks a move-2 cross-restriction
    // (-1 cells) that DefaultPenteRules does not model. G_PENTE is therefore EXCLUDED from the
    // delegation allowlist (Game keeps the legacy path for it). Ignored so the suite stays green
    // while documenting the known, intentional divergence — see CONTEXT.md "Known legacy quirks".
    @Test
    @org.junit.Ignore("G_PENTE diverges (move-2 cross-restriction); excluded from delegation — see CONTEXT.md")
    public void gPente() throws Exception {
        runVariant(Variant.G_PENTE, "G-Pente", "replayGPenteGame", SEED_G_PENTE);
    }

    @Test
    public void oPente() throws Exception {
        runVariant(Variant.O_PENTE, "O-Pente", "replayOPenteGame", SEED_O_PENTE);
    }

    @Test
    public void gomoku() throws Exception {
        runVariant(Variant.GOMOKU, "Gomoku", "replayGomokuGame", SEED_GOMOKU);
    }

    @Test
    public void connect6() throws Exception {
        runVariant(Variant.CONNECT6, "Connect6", "replayConnect6Game", SEED_CONNECT6);
    }

    // ─── core differential loop ───────────────────────────────────────────────

    /**
     * Runs {@value #GAMES_PER_VARIANT} randomly generated games for the given variant,
     * comparing legacy {@link Game} output against {@link DefaultPenteRules} output.
     * Fails fast on the first mismatch with a detailed diagnostic message.
     */
    private static void runVariant(Variant variant, String gameType,
                                   String workerName, long seed) throws Exception {
        DefaultPenteRules engine = new DefaultPenteRules();
        Random rng = new Random(seed);

        for (int gameIdx = 0; gameIdx < GAMES_PER_VARIANT; gameIdx++) {
            List<Integer> moves = randomMoves(rng);

            // ── Legacy (OLD) via reflection ───────────────────────────────────
            Game legacyGame = makeGame(gameType);
            setMoves(legacyGame, moves);
            resetAbstractBoard(legacyGame);
            callReplay(legacyGame, workerName, moves.size());

            // ── New engine ────────────────────────────────────────────────────
            BoardState newState = engine.replay(moves, variant, moves.size());

            // ── Compare captures first (cheap), then the full board ───────────
            assertEquivalent(variant, workerName, gameIdx, moves, legacyGame, newState);
        }
    }

    // ─── assertion ────────────────────────────────────────────────────────────

    private static void assertEquivalent(Variant variant, String worker,
                                         int gameIdx, List<Integer> moves,
                                         Game legacy, BoardState newState) {
        // 1. Capture counts
        if (legacy.whiteCaptures != newState.whiteCaptures
                || legacy.blackCaptures != newState.blackCaptures) {
            fail(String.format(
                    "DIVERGENCE [%s / %s] game #%d:%n"
                            + "  captures — legacy: white=%d black=%d  |  new: white=%d black=%d%n"
                            + "  moves = %s",
                    variant, worker, gameIdx,
                    legacy.whiteCaptures, legacy.blackCaptures,
                    newState.whiteCaptures, newState.blackCaptures,
                    moves));
        }

        // 2. Board — cell for cell
        for (int i = 0; i < 19; i++) {
            for (int j = 0; j < 19; j++) {
                int legacyCell = legacy.abstractBoard[i][j];
                int newCell    = newState.cell(i, j);
                if (legacyCell != newCell) {
                    fail(String.format(
                            "DIVERGENCE [%s / %s] game #%d:%n"
                                    + "  board[%d][%d] — legacy=%d  new=%d%n"
                                    + "  moves = %s",
                            variant, worker, gameIdx,
                            i, j, legacyCell, newCell,
                            moves));
                }
            }
        }
    }

    // ─── helpers (mirrors GameCharacterizationTest) ───────────────────────────

    /**
     * Constructs a {@link Game} with the Android-free 11-arg string constructor.
     * "Not Rated" keeps {@code rated()==false} (suppresses centre-restriction marks).
     * "opponent" (not "computer") suppresses computer-win checks in the workers.
     */
    private static Game makeGame(String gameType) {
        return new Game(
                "eq-test-id", // gameID
                "eq-set-id",  // setID
                gameType,     // gameType
                "opponent",   // opponentName — must NOT be "computer"
                "1500",       // opponentRating
                "white",      // myColor
                "5",          // remainingTime
                "Not Rated",  // ratedNot — keeps rated()==false
                "false",      // privateGame
                "1",          // nameColor
                "0"           // crown
        );
    }

    /** Injects a move list into the private {@code mMovesList} field. */
    private static void setMoves(Game game, List<Integer> moves) throws Exception {
        Field f = Game.class.getDeclaredField("mMovesList");
        f.setAccessible(true);
        f.set(game, new ArrayList<>(moves));
    }

    /** Calls the package-private/private {@code resetAbstractBoard()} to zero the board. */
    private static void resetAbstractBoard(Game game) throws Exception {
        Method m = Game.class.getDeclaredMethod("resetAbstractBoard");
        m.setAccessible(true);
        m.invoke(game);
    }

    /** Reflectively invokes a {@code replay*Game(int)} worker. */
    private static void callReplay(Game game, String methodName, int until) throws Exception {
        Method m = Game.class.getDeclaredMethod(methodName, int.class);
        m.setAccessible(true);
        m.invoke(game, until);
    }

    /**
     * Generates a list of {@code len} distinct move indices in [0, 361) using the
     * Fisher-Yates shuffle on the full board, then taking the first {@code len}.
     * Length is uniform in [{@value #MIN_MOVES}, {@value #MAX_MOVES}].
     */
    private static List<Integer> randomMoves(Random rng) {
        int len = MIN_MOVES + rng.nextInt(MAX_MOVES - MIN_MOVES + 1);
        List<Integer> pool = new ArrayList<>(361);
        for (int i = 0; i < 361; i++) pool.add(i);
        Collections.shuffle(pool, rng);
        return new ArrayList<>(pool.subList(0, len));
    }
}
