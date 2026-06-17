package be.submanifold.pente.rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class VariantsTest {

    // (gameId, server label, expected variant) mirroring Table.gameNames (Table.java:54-83).
    private static final Object[][] CASES = {
            {1, "Pente", Variant.PENTE},
            {2, "Speed Pente", Variant.PENTE},
            {3, "Keryo-Pente", Variant.KERYO_PENTE},
            {4, "Speed Keryo-Pente", Variant.KERYO_PENTE},
            {5, "Gomoku", Variant.GOMOKU},
            {6, "Speed Gomoku", Variant.GOMOKU},
            {7, "D-Pente", Variant.D_PENTE},
            {8, "Speed D-Pente", Variant.D_PENTE},
            {9, "G-Pente", Variant.G_PENTE},
            {10, "Speed G-Pente", Variant.G_PENTE},
            {11, "Poof-Pente", Variant.POOF_PENTE},
            {12, "Speed Poof-Pente", Variant.POOF_PENTE},
            {13, "Connect6", Variant.CONNECT6},
            {14, "Speed Connect6", Variant.CONNECT6},
            {15, "Boat-Pente", Variant.BOAT_PENTE},
            {16, "Speed Boat-Pente", Variant.BOAT_PENTE},
            {17, "DK-Pente", Variant.DK_PENTE},
            {18, "Speed DK-Pente", Variant.DK_PENTE},
            {19, "Go", Variant.GO_19},
            {20, "Speed Go", Variant.GO_19},
            {21, "Go (9x9)", Variant.GO_9},
            {22, "Speed Go (9x9)", Variant.GO_9},
            {23, "Go (13x13)", Variant.GO_13},
            {24, "Speed Go (13x13)", Variant.GO_13},
            {25, "O-Pente", Variant.O_PENTE},
            {26, "Speed O-Pente", Variant.O_PENTE},
            {27, "Swap2-Pente", Variant.SWAP2_PENTE},
            {28, "Speed Swap2-Pente", Variant.SWAP2_PENTE},
            {29, "Swap2-Keryo", Variant.SWAP2_KERYO},
            {30, "Speed Swap2-Keryo", Variant.SWAP2_KERYO},
    };

    @Test
    public void fromGameTypeEqualsFromGameIdForEveryKnownPair() {
        for (Object[] c : CASES) {
            int id = (Integer) c[0];
            String label = (String) c[1];
            Variant expected = (Variant) c[2];
            assertEquals("fromGameId(" + id + ")", expected, Variants.fromGameId(id));
            assertEquals("fromGameType(" + label + ")", expected, Variants.fromGameType(label));
            assertEquals(
                    "fromGameType(" + label + ") == fromGameId(" + id + ")",
                    Variants.fromGameId(id),
                    Variants.fromGameType(label));
        }
    }

    @Test
    public void gridSizeMatchesVariant() {
        assertEquals(19, Variants.gridSize(Variant.PENTE));
        assertEquals(19, Variants.gridSize(Variant.BOAT_PENTE));
        assertEquals(19, Variants.gridSize(Variant.KERYO_PENTE));
        assertEquals(19, Variants.gridSize(Variant.G_PENTE));
        assertEquals(19, Variants.gridSize(Variant.POOF_PENTE));
        assertEquals(19, Variants.gridSize(Variant.D_PENTE));
        assertEquals(19, Variants.gridSize(Variant.DK_PENTE));
        assertEquals(19, Variants.gridSize(Variant.O_PENTE));
        assertEquals(19, Variants.gridSize(Variant.SWAP2_PENTE));
        assertEquals(19, Variants.gridSize(Variant.SWAP2_KERYO));
        assertEquals(19, Variants.gridSize(Variant.GOMOKU));
        assertEquals(19, Variants.gridSize(Variant.CONNECT6));
        assertEquals(9, Variants.gridSize(Variant.GO_9));
        assertEquals(13, Variants.gridSize(Variant.GO_13));
        assertEquals(19, Variants.gridSize(Variant.GO_19));
    }

    @Test
    public void captureRuleMatchesVariant() {
        assertEquals(CaptureRule.PENTE_PAIR, Variants.captureRule(Variant.PENTE));
        assertEquals(CaptureRule.PENTE_PAIR, Variants.captureRule(Variant.BOAT_PENTE));
        assertEquals(CaptureRule.PENTE_PAIR, Variants.captureRule(Variant.D_PENTE));
        assertEquals(CaptureRule.PENTE_PAIR, Variants.captureRule(Variant.G_PENTE));
        assertEquals(CaptureRule.PENTE_PAIR, Variants.captureRule(Variant.SWAP2_PENTE));
        assertEquals(CaptureRule.KERYO_TRIO, Variants.captureRule(Variant.KERYO_PENTE));
        assertEquals(CaptureRule.KERYO_TRIO, Variants.captureRule(Variant.DK_PENTE));
        assertEquals(CaptureRule.KERYO_TRIO, Variants.captureRule(Variant.SWAP2_KERYO));
        assertEquals(CaptureRule.POOF, Variants.captureRule(Variant.POOF_PENTE));
        assertEquals(CaptureRule.KERYO_POOF, Variants.captureRule(Variant.O_PENTE));
        assertEquals(CaptureRule.NONE, Variants.captureRule(Variant.GOMOKU));
        assertEquals(CaptureRule.NONE, Variants.captureRule(Variant.CONNECT6));
        assertEquals(CaptureRule.NONE, Variants.captureRule(Variant.GO_9));
        assertEquals(CaptureRule.NONE, Variants.captureRule(Variant.GO_13));
        assertEquals(CaptureRule.NONE, Variants.captureRule(Variant.GO_19));
    }

    @Test
    public void stonesPerTurnMatchesVariant() {
        assertEquals(2, Variants.stonesPerTurn(Variant.CONNECT6));
        for (Variant v : Variant.values()) {
            if (v != Variant.CONNECT6) {
                assertEquals("stonesPerTurn(" + v + ")", 1, Variants.stonesPerTurn(v));
            }
        }
    }

    @Test
    public void variantHelpersClassifyCorrectly() {
        assertTrue(Variant.CONNECT6.isConnect6());
        assertTrue(Variant.GOMOKU.isGomoku());
        assertTrue(Variant.SWAP2_PENTE.isSwap2());
        assertTrue(Variant.SWAP2_KERYO.isSwap2());
        assertTrue(Variant.D_PENTE.isDPente());
        assertTrue(Variant.DK_PENTE.isDPente());
        assertTrue(Variant.GO_9.isGo());
        assertTrue(Variant.GO_13.isGo());
        assertTrue(Variant.GO_19.isGo());
        assertFalse(Variant.PENTE.isGo());
        assertFalse(Variant.PENTE.isSwap2());
        assertFalse(Variant.PENTE.isConnect6());
    }

    @Test
    public void renjuVariantIsRegistered() {
        assertEquals(15, Variants.gridSize(Variant.RENJU));
        assertEquals(CaptureRule.NONE, Variants.captureRule(Variant.RENJU));
        assertEquals(1, Variants.stonesPerTurn(Variant.RENJU));
        assertTrue(Variant.RENJU.isRenju());
        assertFalse(Variant.PENTE.isRenju());
        assertFalse(Variant.GOMOKU.isRenju());
    }

    @Test
    public void unknownInputsReturnNull() {
        assertNull(Variants.fromGameType("Chess"));
        assertNull(Variants.fromGameType(null));
        assertNull(Variants.fromGameId(99));
    }

    @Test
    public void nullVariantThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> Variants.gridSize(null));
        assertThrows(IllegalArgumentException.class, () -> Variants.captureRule(null));
        assertThrows(IllegalArgumentException.class, () -> Variants.stonesPerTurn(null));
    }
}
