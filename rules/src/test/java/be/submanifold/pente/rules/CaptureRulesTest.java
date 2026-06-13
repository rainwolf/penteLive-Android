package be.submanifold.pente.rules;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/**
 * Per-direction capture tests, 8 per capture rule (32 total). Each crafts a minimal
 * position so the final move triggers exactly one capture in one of the 8 directions,
 * locking the engine's per-direction capture geometry and count semantics.
 */
public class CaptureRulesTest {
    private final PenteRules rules = new DefaultPenteRules();

    private static int idx(int i, int j) { return i * 19 + j; }

    private BoardState replay(Variant v, int... mv) {
        List<Integer> l = new ArrayList<>();
        for (int m : mv) l.add(m);
        return rules.replay(l, v, l.size());
    }

    // ── PENTE_PAIR: my-A at distance 3, two opp between; placing F (white) captures both blacks (+2 black). ──
    @Test public void penteUp()        { BoardState s = replay(Variant.PENTE, idx(6,9), idx(7,9), idx(0,0), idx(8,9), idx(9,9)); assertEquals(2, s.blackCaptures); assertEquals(0, s.whiteCaptures); assertEquals(0, s.cell(7,9)); assertEquals(0, s.cell(8,9)); assertEquals(1, s.cell(6,9)); assertEquals(1, s.cell(9,9)); }
    @Test public void penteUpLeft()    { BoardState s = replay(Variant.PENTE, idx(6,6), idx(7,7), idx(0,0), idx(8,8), idx(9,9)); assertEquals(2, s.blackCaptures); assertEquals(0, s.cell(7,7)); assertEquals(0, s.cell(8,8)); }
    @Test public void penteLeft()      { BoardState s = replay(Variant.PENTE, idx(9,6), idx(9,7), idx(0,0), idx(9,8), idx(9,9)); assertEquals(2, s.blackCaptures); assertEquals(0, s.cell(9,7)); assertEquals(0, s.cell(9,8)); }
    @Test public void penteDownLeft()  { BoardState s = replay(Variant.PENTE, idx(12,6), idx(11,7), idx(0,0), idx(10,8), idx(9,9)); assertEquals(2, s.blackCaptures); assertEquals(0, s.cell(10,8)); assertEquals(0, s.cell(11,7)); }
    @Test public void penteDown()      { BoardState s = replay(Variant.PENTE, idx(12,9), idx(11,9), idx(0,0), idx(10,9), idx(9,9)); assertEquals(2, s.blackCaptures); assertEquals(0, s.cell(10,9)); assertEquals(0, s.cell(11,9)); }
    @Test public void penteDownRight() { BoardState s = replay(Variant.PENTE, idx(12,12), idx(11,11), idx(0,0), idx(10,10), idx(9,9)); assertEquals(2, s.blackCaptures); assertEquals(0, s.cell(10,10)); assertEquals(0, s.cell(11,11)); }
    @Test public void penteRight()     { BoardState s = replay(Variant.PENTE, idx(9,12), idx(9,11), idx(0,0), idx(9,10), idx(9,9)); assertEquals(2, s.blackCaptures); assertEquals(0, s.cell(9,10)); assertEquals(0, s.cell(9,11)); }
    @Test public void penteUpRight()   { BoardState s = replay(Variant.PENTE, idx(6,12), idx(7,11), idx(0,0), idx(8,10), idx(9,9)); assertEquals(2, s.blackCaptures); assertEquals(0, s.cell(8,10)); assertEquals(0, s.cell(7,11)); }

    // ── KERYO_TRIO: my-A at distance 4, three opp between; capturing F is white (+3 black). ──
    @Test public void keryoUp()        { BoardState s = replay(Variant.KERYO_PENTE, idx(5,9), idx(6,9), idx(0,0), idx(7,9), idx(0,18), idx(8,9), idx(9,9)); assertEquals(3, s.blackCaptures); assertEquals(0, s.whiteCaptures); assertEquals(0, s.cell(6,9)); assertEquals(0, s.cell(7,9)); assertEquals(0, s.cell(8,9)); }
    @Test public void keryoUpLeft()    { BoardState s = replay(Variant.KERYO_PENTE, idx(5,5), idx(6,6), idx(0,0), idx(7,7), idx(0,18), idx(8,8), idx(9,9)); assertEquals(3, s.blackCaptures); assertEquals(0, s.cell(6,6)); assertEquals(0, s.cell(7,7)); assertEquals(0, s.cell(8,8)); }
    @Test public void keryoLeft()      { BoardState s = replay(Variant.KERYO_PENTE, idx(9,5), idx(9,6), idx(0,0), idx(9,7), idx(0,18), idx(9,8), idx(9,9)); assertEquals(3, s.blackCaptures); assertEquals(0, s.cell(9,6)); assertEquals(0, s.cell(9,7)); assertEquals(0, s.cell(9,8)); }
    @Test public void keryoDownLeft()  { BoardState s = replay(Variant.KERYO_PENTE, idx(13,5), idx(12,6), idx(0,0), idx(11,7), idx(0,18), idx(10,8), idx(9,9)); assertEquals(3, s.blackCaptures); assertEquals(0, s.cell(10,8)); assertEquals(0, s.cell(11,7)); assertEquals(0, s.cell(12,6)); }
    @Test public void keryoDown()      { BoardState s = replay(Variant.KERYO_PENTE, idx(13,9), idx(12,9), idx(0,0), idx(11,9), idx(0,18), idx(10,9), idx(9,9)); assertEquals(3, s.blackCaptures); assertEquals(0, s.cell(10,9)); assertEquals(0, s.cell(11,9)); assertEquals(0, s.cell(12,9)); }
    @Test public void keryoDownRight() { BoardState s = replay(Variant.KERYO_PENTE, idx(13,13), idx(12,12), idx(0,0), idx(11,11), idx(0,18), idx(10,10), idx(9,9)); assertEquals(3, s.blackCaptures); assertEquals(0, s.cell(10,10)); assertEquals(0, s.cell(11,11)); assertEquals(0, s.cell(12,12)); }
    @Test public void keryoRight()     { BoardState s = replay(Variant.KERYO_PENTE, idx(9,13), idx(9,12), idx(0,0), idx(9,11), idx(0,18), idx(9,10), idx(9,9)); assertEquals(3, s.blackCaptures); assertEquals(0, s.cell(9,10)); assertEquals(0, s.cell(9,11)); assertEquals(0, s.cell(9,12)); }
    @Test public void keryoUpRight()   { BoardState s = replay(Variant.KERYO_PENTE, idx(5,13), idx(6,12), idx(0,0), idx(7,11), idx(0,18), idx(8,10), idx(9,9)); assertEquals(3, s.blackCaptures); assertEquals(0, s.cell(8,10)); assertEquals(0, s.cell(7,11)); assertEquals(0, s.cell(6,12)); }

    // ── POOF: a white pair (my N + placed F) flanked by two blacks self-poofs (+2 white); flanking blacks remain. ──
    @Test public void poofUp()        { BoardState s = replay(Variant.POOF_PENTE, idx(8,9), idx(7,9), idx(0,0), idx(10,9), idx(9,9)); assertEquals(2, s.whiteCaptures); assertEquals(0, s.blackCaptures); assertEquals(0, s.cell(8,9)); assertEquals(0, s.cell(9,9)); assertEquals(2, s.cell(7,9)); assertEquals(2, s.cell(10,9)); }
    @Test public void poofUpLeft()    { BoardState s = replay(Variant.POOF_PENTE, idx(8,8), idx(7,7), idx(0,0), idx(10,10), idx(9,9)); assertEquals(2, s.whiteCaptures); assertEquals(0, s.cell(8,8)); assertEquals(0, s.cell(9,9)); }
    @Test public void poofLeft()      { BoardState s = replay(Variant.POOF_PENTE, idx(9,8), idx(9,7), idx(0,0), idx(9,10), idx(9,9)); assertEquals(2, s.whiteCaptures); assertEquals(0, s.cell(9,8)); assertEquals(0, s.cell(9,9)); }
    @Test public void poofDownLeft()  { BoardState s = replay(Variant.POOF_PENTE, idx(10,8), idx(8,10), idx(0,0), idx(11,7), idx(9,9)); assertEquals(2, s.whiteCaptures); assertEquals(0, s.cell(10,8)); assertEquals(0, s.cell(9,9)); }
    @Test public void poofDown()      { BoardState s = replay(Variant.POOF_PENTE, idx(10,9), idx(11,9), idx(0,0), idx(8,9), idx(9,9)); assertEquals(2, s.whiteCaptures); assertEquals(0, s.cell(10,9)); assertEquals(0, s.cell(9,9)); }
    @Test public void poofDownRight() { BoardState s = replay(Variant.POOF_PENTE, idx(10,10), idx(8,8), idx(0,0), idx(11,11), idx(9,9)); assertEquals(2, s.whiteCaptures); assertEquals(0, s.cell(10,10)); assertEquals(0, s.cell(9,9)); }
    @Test public void poofRight()     { BoardState s = replay(Variant.POOF_PENTE, idx(9,10), idx(9,8), idx(0,0), idx(9,11), idx(9,9)); assertEquals(2, s.whiteCaptures); assertEquals(0, s.cell(9,10)); assertEquals(0, s.cell(9,9)); }
    @Test public void poofUpRight()   { BoardState s = replay(Variant.POOF_PENTE, idx(8,10), idx(10,8), idx(0,0), idx(7,11), idx(9,9)); assertEquals(2, s.whiteCaptures); assertEquals(0, s.cell(8,10)); assertEquals(0, s.cell(9,9)); }

    // ── KERYO_POOF: a white trio (two my M + placed F) flanked by two blacks self-poofs (+3 white); flanking blacks remain. ──
    @Test public void keryoPoofLeft()      { BoardState s = replay(Variant.O_PENTE, idx(8,9), idx(6,9), idx(7,9), idx(10,9), idx(9,9)); assertEquals(3, s.whiteCaptures); assertEquals(0, s.blackCaptures); assertEquals(0, s.cell(7,9)); assertEquals(0, s.cell(8,9)); assertEquals(0, s.cell(9,9)); assertEquals(2, s.cell(6,9)); assertEquals(2, s.cell(10,9)); }
    @Test public void keryoPoofUpLeft()    { BoardState s = replay(Variant.O_PENTE, idx(8,8), idx(6,6), idx(7,7), idx(10,10), idx(9,9)); assertEquals(3, s.whiteCaptures); assertEquals(0, s.cell(7,7)); assertEquals(0, s.cell(8,8)); assertEquals(0, s.cell(9,9)); }
    @Test public void keryoPoofUp()        { BoardState s = replay(Variant.O_PENTE, idx(9,8), idx(9,6), idx(9,7), idx(9,10), idx(9,9)); assertEquals(3, s.whiteCaptures); assertEquals(0, s.cell(9,7)); assertEquals(0, s.cell(9,8)); assertEquals(0, s.cell(9,9)); }
    @Test public void keryoPoofUpRight()   { BoardState s = replay(Variant.O_PENTE, idx(10,8), idx(8,10), idx(11,7), idx(12,6), idx(9,9)); assertEquals(3, s.whiteCaptures); assertEquals(0, s.cell(10,8)); assertEquals(0, s.cell(11,7)); assertEquals(0, s.cell(9,9)); }
    @Test public void keryoPoofRight()     { BoardState s = replay(Variant.O_PENTE, idx(10,9), idx(12,9), idx(11,9), idx(8,9), idx(9,9)); assertEquals(3, s.whiteCaptures); assertEquals(0, s.cell(10,9)); assertEquals(0, s.cell(11,9)); assertEquals(0, s.cell(9,9)); }
    @Test public void keryoPoofDownRight() { BoardState s = replay(Variant.O_PENTE, idx(10,10), idx(8,8), idx(11,11), idx(12,12), idx(9,9)); assertEquals(3, s.whiteCaptures); assertEquals(0, s.cell(10,10)); assertEquals(0, s.cell(11,11)); assertEquals(0, s.cell(9,9)); }
    @Test public void keryoPoofDown()      { BoardState s = replay(Variant.O_PENTE, idx(9,10), idx(9,8), idx(9,11), idx(9,12), idx(9,9)); assertEquals(3, s.whiteCaptures); assertEquals(0, s.cell(9,10)); assertEquals(0, s.cell(9,11)); assertEquals(0, s.cell(9,9)); }
    @Test public void keryoPoofDownLeft()  { BoardState s = replay(Variant.O_PENTE, idx(8,10), idx(10,8), idx(7,11), idx(6,12), idx(9,9)); assertEquals(3, s.whiteCaptures); assertEquals(0, s.cell(8,10)); assertEquals(0, s.cell(7,11)); assertEquals(0, s.cell(9,9)); }

    // ── KERYO_POOF symmetric: opp-my-F-my-opp (F flanked by two own white stones, blacks at both ends) — exercises the symmetric branch unique to keryo-poof. ──
    // White at (9,8) & (9,10) with placed F at (9,9), blacks at (9,7) & (9,11): the trio self-poofs (+3 white); the flanking blacks remain.
    @Test public void keryoPoofSymmetricHorizontal() {
        BoardState s = replay(Variant.O_PENTE, idx(9,8), idx(9,7), idx(9,10), idx(9,11), idx(9,9));
        assertEquals(3, s.whiteCaptures);
        assertEquals(0, s.blackCaptures);
        assertEquals(0, s.cell(9,8));
        assertEquals(0, s.cell(9,9));
        assertEquals(0, s.cell(9,10));
        assertEquals(2, s.cell(9,7));
        assertEquals(2, s.cell(9,11));
    }
}
