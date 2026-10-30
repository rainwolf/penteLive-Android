package be.submanifold.pente.rules;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/**
 * Go replay through the rules seam ({@link DefaultPenteRules#replay} dispatches Go to
 * {@link GoReplay}). Cells are row-major ({@code row * n + col}); colours 1 white, 2 black;
 * black moves first.
 */
public class GoReplayTest {
    private final PenteRules rules = new DefaultPenteRules();

    private BoardState replay(Variant v, int... mv) {
        List<Integer> l = new ArrayList<>();
        for (int m : mv) l.add(m);
        return rules.replay(l, v, l.size());
    }

    @Test
    public void boardIsSizedPerVariant() {
        assertEquals(9, replay(Variant.GO_9).gridSize);
        assertEquals(9, replay(Variant.GO_9).board.length);
        assertEquals(13, replay(Variant.GO_13).board.length);
        assertEquals(19, replay(Variant.GO_19).board.length);
    }

    @Test
    public void firstStoneIsBlack() {
        BoardState s = replay(Variant.GO_9, 40);
        assertEquals(2, s.cell(4, 4));
        assertEquals(40, s.lastMove);
    }

    @Test
    public void edgeCaptureOn9x9() {
        // B A9, W B9, B C9, W E5, B B8: the white B9 stone loses its last liberty.
        BoardState s = replay(Variant.GO_9, 0, 1, 2, 40, 10);
        assertEquals(0, s.cell(0, 1));
        assertEquals(2, s.cell(0, 0));
        assertEquals(1, s.whiteCaptures);
        assertEquals(0, s.blackCaptures);
        assertEquals(1, s.cell(4, 4));
    }

    @Test
    public void passKeepsColourParity() {
        BoardState s = replay(Variant.GO_9, 40, 81, 41);
        assertEquals(2, s.cell(4, 4));
        assertEquals(2, s.cell(4, 5));
    }

    // Classic ko: black (1,0),(0,1),(2,1); white (0,2),(2,2),(1,3); white (1,1); black (1,2) takes it.
    private static final int[] KO = {9, 2, 1, 20, 19, 12, 80, 10, 11};

    @Test
    public void koMarksTheRetakeCell() {
        BoardState s = replay(Variant.GO_9, KO);
        assertEquals(10, s.koMove);
        assertEquals(1, s.whiteCaptures);
        assertEquals(0, s.cell(1, 1));
        assertEquals(2, s.cell(1, 2));
    }

    @Test
    public void nonCapturingMoveResetsKo() {
        int[] mv = append(KO, 70);
        assertEquals(-1, replay(Variant.GO_9, mv).koMove);
    }

    @Test
    public void passDoesNotResetKo() {
        // Live Game quirk, kept on purpose: only a placed stone resets the ko.
        int[] mv = append(KO, 81);
        assertEquals(10, replay(Variant.GO_9, mv).koMove);
    }

    @Test
    public void groupCaptureCountsAllStones() {
        // 13x13: white pair at (0,0),(0,1) is surrounded by black (1,0),(1,1),(0,2).
        BoardState s = replay(Variant.GO_13, 13, 0, 14, 1, 2);
        assertEquals(2, s.whiteCaptures);
        assertEquals(0, s.cell(0, 0));
        assertEquals(0, s.cell(0, 1));
        assertEquals(-1, s.koMove);
    }

    @Test
    public void blackGroupCaptureCountsBlack() {
        // 19x19: white surrounds the black corner stone at (0,0).
        BoardState s = replay(Variant.GO_19, 0, 1, 200, 19);
        assertEquals(1, s.blackCaptures);
        assertEquals(0, s.cell(0, 0));
    }

    @Test
    public void deadStonesAfterDoublePassAreRemoved() {
        BoardState s = replay(Variant.GO_9, 40, 41, 81, 81, 41);
        assertEquals(0, s.cell(4, 5));
        assertEquals(2, s.cell(4, 4));
        assertEquals(0, s.whiteCaptures);
        assertEquals(0, s.blackCaptures);
    }

    @Test
    public void junkMovesAreSkippedButKeepParity() {
        BoardState s = replay(Variant.GO_9, 180, 40);
        assertEquals(1, s.cell(4, 4));
        BoardState t = replay(Variant.GO_9, -5, 40);
        assertEquals(1, t.cell(4, 4));
    }

    @Test
    public void untilMoveStopsEarly() {
        List<Integer> l = new ArrayList<>();
        for (int m : new int[]{0, 1, 2, 40, 10}) l.add(m);
        BoardState s = rules.replay(l, Variant.GO_9, 4);
        assertEquals(1, s.cell(0, 1));
        assertEquals(0, s.whiteCaptures);
    }

    @Test
    public void suicideStoneStays() {
        // Live Game quirk, kept on purpose: suicide is not removed.
        // Black (0,1),(1,0); white plays the corner (0,0) with no liberties.
        BoardState s = replay(Variant.GO_9, 1, 80, 9, 0);
        assertEquals(1, s.cell(0, 0));
        assertEquals(0, s.whiteCaptures);
    }

    private static int[] append(int[] a, int x) {
        int[] r = new int[a.length + 1];
        System.arraycopy(a, 0, r, 0, a.length);
        r[a.length] = x;
        return r;
    }
}
