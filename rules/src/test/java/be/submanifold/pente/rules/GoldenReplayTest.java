package be.submanifold.pente.rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/**
 * Cross-module golden equivalence net for Task 4.
 *
 * <p>For each committed golden in {@code rules/src/test/resources/golden/}, this replays the EXACT
 * move-list + variant that produced it in the legacy {@code GameCharacterizationTest} (which drove
 * the old {@code Game.replay*Game} workers) through the new pure engine and asserts the resulting
 * board, whiteCaptures, blackCaptures and winner all match the golden. This proves the new engine
 * reproduces the old rules behaviour exactly. The goldens are the oracle: if one cannot be matched,
 * the engine is wrong — fix the engine, never the golden.
 *
 * <p>Per Task 4 the engine does NOT compute wins eagerly, so {@code winner} is always {@code null}
 * here (the legacy worker also leaves no winner set) — even for {@code pente_capture_win} which
 * reaches whiteCaptures==10.
 */
public class GoldenReplayTest {

    private final PenteRules rules = new DefaultPenteRules();

    /** pente_pair_capture: Black flanks 2 White stones (PENTE). */
    @Test public void goldenPentePairCapture() throws IOException {
        assertMatchesGolden("pente_pair_capture", Variant.PENTE, 180, 0, 1, 19, 2, 3);
    }

    /** gomoku_no_capture: pure placement, no capture mechanic. */
    @Test public void goldenGomokuNoCapture() throws IOException {
        assertMatchesGolden("gomoku_no_capture", Variant.GOMOKU, 180, 171, 181, 172, 182);
    }

    /** keryo_trio_capture: Black flanks 3 White stones (KERYO_PENTE). */
    @Test public void goldenKeryoTrioCapture() throws IOException {
        assertMatchesGolden("keryo_trio_capture", Variant.KERYO_PENTE, 180, 177, 178, 0, 179, 181);
    }

    /** poof_poof_capture: White self-poofs its own flanked pair (POOF_PENTE). whiteCaptures==2 (+1 in-branch +1 bonus). */
    @Test public void goldenPoofPoofCapture() throws IOException {
        assertMatchesGolden("poof_poof_capture", Variant.POOF_PENTE, 179, 178, 176, 181, 180);
    }

    /** pente_capture_win: Black completes 5 pair captures → blackCaptures... actually whiteCaptures==10 (white stones removed). winner stays null. */
    @Test public void goldenPenteCaptureWin() throws IOException {
        BoardState s = assertMatchesGolden("pente_capture_win", Variant.PENTE,
                180, 0, 1, 19, 2, 3,
                20, 38, 21, 22,
                39, 57, 40, 41,
                58, 76, 59, 60,
                77, 95, 78, 79);
        assertEquals("pente_capture_win must reach 10 white stones removed", 10, s.whiteCaptures);
        assertNull("Task 4 replay must not eagerly set a winner", s.winner);
    }

    /** connect6_placement: 2 stones/turn, i%4 ∈ {0,3} → white. No captures. */
    @Test public void goldenConnect6Placement() throws IOException {
        assertMatchesGolden("connect6_placement", Variant.CONNECT6, 180, 100, 101, 181, 182);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private BoardState assertMatchesGolden(String name, Variant v, int... mv) throws IOException {
        Golden golden = readGolden(name);
        List<Integer> moves = new ArrayList<>();
        for (int m : mv) moves.add(m);
        BoardState s = rules.replay(moves, v, moves.size());

        assertEquals("[" + name + "] gridSize", golden.board.length, s.gridSize);
        for (int i = 0; i < golden.board.length; i++) {
            for (int j = 0; j < golden.board[i].length; j++) {
                assertEquals("[" + name + "] cell(" + i + "," + j + ")", golden.board[i][j], s.cell(i, j));
            }
        }
        assertEquals("[" + name + "] whiteCaptures", golden.whiteCaptures, s.whiteCaptures);
        assertEquals("[" + name + "] blackCaptures", golden.blackCaptures, s.blackCaptures);
        assertNull("[" + name + "] winner must be null in Task 4 replay", s.winner);
        return s;
    }

    private static final class Golden {
        final byte[][] board;
        final int whiteCaptures;
        final int blackCaptures;
        Golden(byte[][] board, int whiteCaptures, int blackCaptures) {
            this.board = board;
            this.whiteCaptures = whiteCaptures;
            this.blackCaptures = blackCaptures;
        }
    }

    /** Parses a committed golden file: N lines of N space-separated cell values, then capture counts. */
    private static Golden readGolden(String name) throws IOException {
        String resource = "/golden/" + name + ".txt";
        List<byte[]> rows = new ArrayList<>();
        int white = -1;
        int black = -1;
        try (InputStream in = GoldenReplayTest.class.getResourceAsStream(resource)) {
            assertNotNull("Golden resource not found on classpath: " + resource, in);
            try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    if (line.startsWith("whiteCaptures=")) {
                        white = Integer.parseInt(line.substring("whiteCaptures=".length()).trim());
                    } else if (line.startsWith("blackCaptures=")) {
                        black = Integer.parseInt(line.substring("blackCaptures=".length()).trim());
                    } else {
                        String[] parts = line.split("\\s+");
                        byte[] row = new byte[parts.length];
                        for (int j = 0; j < parts.length; j++) {
                            row[j] = Byte.parseByte(parts[j]);
                        }
                        rows.add(row);
                    }
                }
            }
        }
        assertTrue("[" + name + "] expected whiteCaptures line present", white >= 0);
        assertTrue("[" + name + "] expected blackCaptures line present", black >= 0);
        return new Golden(rows.toArray(new byte[0][]), white, black);
    }
}
