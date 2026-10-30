package be.submanifold.pente.rules;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/** Renju replay: 15x15, black first, no captures; off-board moves are skipped but keep parity. */
public class RenjuReplayRulesTest {
    private final PenteRules rules = new DefaultPenteRules();

    private BoardState replay(int... mv) {
        List<Integer> l = new ArrayList<>();
        for (int m : mv) l.add(m);
        return rules.replay(l, Variant.RENJU, l.size());
    }

    @Test
    public void blackMovesFirst() {
        BoardState s = replay(112, 113);
        assertEquals(15, s.gridSize);
        assertEquals(2, s.cell(7, 7));
        assertEquals(1, s.cell(7, 8));
        assertEquals(113, s.lastMove);
    }

    @Test
    public void passKeepsParityAndDoesNotThrow() {
        BoardState s = replay(112, 225, 113);
        assertEquals(2, s.cell(7, 7));
        assertEquals(2, s.cell(7, 8));
        assertEquals(113, s.lastMove);
    }

    @Test
    public void negativeMoveIsSkipped() {
        BoardState s = replay(-1, 112);
        assertEquals(1, s.cell(7, 7));
    }
}
