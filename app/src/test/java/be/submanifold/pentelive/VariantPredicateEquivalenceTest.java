package be.submanifold.pentelive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import be.submanifold.pente.rules.Variant;
import be.submanifold.pente.rules.Variants;

import org.junit.Test;

/**
 * Guard test for Task 8 (Variants-routing refactor).
 *
 * <p>Pins the equivalence between legacy predicate logic and the Variants-based
 * routing BEFORE any code is changed. This test must pass identically before and
 * after the rerouting — that is the proof that no behaviour changed.
 *
 * <h3>Game.java predicates</h3>
 * <ul>
 *   <li>{@code isConnect6()} — equivalent; rerouted through Variants.</li>
 *   <li>{@code isGomoku()} — equivalent; rerouted through Variants.</li>
 *   <li>{@code isDPente()} — equivalent; rerouted through Variants.</li>
 *   <li>{@code isSwap2()} — DIVERGES on "Speed Swap2-*" strings; NOT rerouted
 *       (see {@link #gameIsSwap2_speedVariantsDiverge()}).</li>
 *   <li>{@code isGo()} — returns a boolean flag, not a string predicate;
 *       left unchanged.</li>
 * </ul>
 *
 * <h3>Table.java predicates</h3>
 * <p>Table cannot be instantiated in plain JVM unit tests because its field
 * initialisers call {@code Color.parseColor()}, an Android method that throws in
 * the stub environment. The legacy predicate LOGIC is therefore reproduced inline —
 * an exact transcription of each method body. All three id-based predicates are
 * equivalent to Variants and are rerouted.
 */
public class VariantPredicateEquivalenceTest {

    // Full canonical + Speed set, mirroring VariantsTest.CASES.
    private static final String[] ALL_GAME_TYPES = {
            "Pente",        "Speed Pente",
            "Keryo-Pente",  "Speed Keryo-Pente",
            "Gomoku",       "Speed Gomoku",
            "D-Pente",      "Speed D-Pente",
            "G-Pente",      "Speed G-Pente",
            "Poof-Pente",   "Speed Poof-Pente",
            "Connect6",     "Speed Connect6",
            "Boat-Pente",   "Speed Boat-Pente",
            "DK-Pente",     "Speed DK-Pente",
            "Go",           "Speed Go",
            "Go (9x9)",     "Speed Go (9x9)",
            "Go (13x13)",   "Speed Go (13x13)",
            "O-Pente",      "Speed O-Pente",
            "Swap2-Pente",  "Speed Swap2-Pente",
            "Swap2-Keryo",  "Speed Swap2-Keryo",
    };

    // ─── Game predicate equivalence ──────────────────────────────────────────

    @Test
    public void gameIsConnect6EquivalentToVariants() {
        for (String gt : ALL_GAME_TYPES) {
            Game g = makeGame(gt);
            boolean legacy = g.isConnect6();
            Variant v = Variants.fromGameType(gt);
            boolean routed = v != null && v.isConnect6();
            assertEquals("isConnect6 mismatch for '" + gt + "'", legacy, routed);
        }
    }

    @Test
    public void gameIsGomokuEquivalentToVariants() {
        for (String gt : ALL_GAME_TYPES) {
            Game g = makeGame(gt);
            boolean legacy = g.isGomoku();
            Variant v = Variants.fromGameType(gt);
            boolean routed = v != null && v.isGomoku();
            assertEquals("isGomoku mismatch for '" + gt + "'", legacy, routed);
        }
    }

    @Test
    public void gameIsDPenteEquivalentToVariants() {
        for (String gt : ALL_GAME_TYPES) {
            Game g = makeGame(gt);
            boolean legacy = g.isDPente();
            Variant v = Variants.fromGameType(gt);
            boolean routed = v != null && v.isDPente();
            assertEquals("isDPente mismatch for '" + gt + "'", legacy, routed);
        }
    }

    /**
     * Documents the KNOWN DIVERGENCE in {@code Game.isSwap2()}.
     *
     * <p>The legacy body is {@code mGameType.startsWith("Swap2")}, which returns
     * {@code false} for "Speed Swap2-*" strings. {@link Variants#fromGameType}
     * uses {@code contains("Swap2-")} and correctly returns {@code true}.
     * Because the results differ, {@code isSwap2()} in Game is NOT rerouted —
     * changing it would alter observable behaviour.
     */
    @Test
    public void gameIsSwap2_speedVariantsDiverge() {
        for (String gt : new String[]{"Speed Swap2-Pente", "Speed Swap2-Keryo"}) {
            Game g = makeGame(gt);
            boolean legacy = g.isSwap2();
            Variant v = Variants.fromGameType(gt);
            boolean routed = v != null && v.isSwap2();
            assertFalse(
                    "Legacy Game.isSwap2() must be false for '" + gt + "' (startsWith(\"Swap2\") fails on \"Speed \" prefix)",
                    legacy);
            assertTrue(
                    "Variants.isSwap2() must be true for '" + gt + "' (contains(\"Swap2-\") matches)",
                    routed);
            // They differ — this proves why isSwap2() in Game must NOT be rerouted.
        }
    }

    // ─── Table predicate equivalence (logic inlined — Table has Android deps) ─

    /**
     * Legacy body: {@code game > 18 && game < 25} (Table.java line 186).
     * Rerouted body: {@code Variants.fromGameId(game).isGo()}.
     */
    @Test
    public void tableIsGoEquivalentToVariants() {
        for (int id = 1; id <= 30; id++) {
            boolean legacy = id > 18 && id < 25;
            Variant v = Variants.fromGameId(id);
            boolean routed = v != null && v.isGo();
            assertEquals("Table.isGo mismatch for gameId=" + id, legacy, routed);
        }
    }

    /**
     * Legacy body: {@code game == 7 || game == 8 || game == 17 || game == 18}
     * (Table.java line 267). Rerouted body: {@code Variants.fromGameId(game).isDPente()}.
     */
    @Test
    public void tableIsDPenteEquivalentToVariants() {
        for (int id = 1; id <= 30; id++) {
            boolean legacy = (id == 7 || id == 8 || id == 17 || id == 18);
            Variant v = Variants.fromGameId(id);
            boolean routed = v != null && v.isDPente();
            assertEquals("Table.isDPente mismatch for gameId=" + id, legacy, routed);
        }
    }

    /**
     * Legacy body: {@code game == 27 || game == 28 || game == 29 || game == 30}
     * (Table.java line 271). Rerouted body: {@code Variants.fromGameId(game).isSwap2()}.
     */
    @Test
    public void tableIsSwap2EquivalentToVariants() {
        for (int id = 1; id <= 30; id++) {
            boolean legacy = (id == 27 || id == 28 || id == 29 || id == 30);
            Variant v = Variants.fromGameId(id);
            boolean routed = v != null && v.isSwap2();
            assertEquals("Table.isSwap2 mismatch for gameId=" + id, legacy, routed);
        }
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private static Game makeGame(String gameType) {
        return new Game(
                "test-id",   // gameID
                "set-id",    // setID
                gameType,    // gameType
                "opponent",  // opponentName
                "1500",      // opponentRating
                "white",     // myColor
                "5",         // remainingTime
                "Not Rated", // ratedNot
                "false",     // privateGame
                "1",         // nameColor
                "0"          // crown
        );
    }
}
