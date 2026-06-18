package be.submanifold.pente.rules;

/**
 * Every game variant supported by pente.org.
 *
 * <p>Each constant carries its descriptor data, which is the single source of
 * truth for the rules module: the canonical (non-speed) game id, the board grid
 * size, the capture rule, and the number of stones placed per turn.
 *
 * <p>Go variants are classified and sized here for completeness; their rules
 * are out of scope for v1.
 */
public enum Variant {
    PENTE(1, 19, CaptureRule.PENTE_PAIR, 1),
    BOAT_PENTE(15, 19, CaptureRule.PENTE_PAIR, 1),
    KERYO_PENTE(3, 19, CaptureRule.KERYO_TRIO, 1),
    G_PENTE(9, 19, CaptureRule.PENTE_PAIR, 1),
    POOF_PENTE(11, 19, CaptureRule.POOF, 1),
    D_PENTE(7, 19, CaptureRule.PENTE_PAIR, 1),
    DK_PENTE(17, 19, CaptureRule.KERYO_TRIO, 1),
    O_PENTE(25, 19, CaptureRule.KERYO_POOF, 1),
    SWAP2_PENTE(27, 19, CaptureRule.PENTE_PAIR, 1),
    SWAP2_KERYO(29, 19, CaptureRule.KERYO_TRIO, 1),
    GOMOKU(5, 19, CaptureRule.NONE, 1),
    CONNECT6(13, 19, CaptureRule.NONE, 2),
    // TODO(rules): GoRules seam — Go is classified + sized only; no rule engine in v1.
    GO_9(21, 9, CaptureRule.NONE, 1),
    GO_13(23, 13, CaptureRule.NONE, 1),
    GO_19(19, 19, CaptureRule.NONE, 1), // gameId 19 coincidentally equals the 19x19 board size
    RENJU(31, 15, CaptureRule.NONE, 1);

    /** Descriptor fields are package-private; external code must go through {@link Variants}. */
    final int canonicalGameId;
    final int gridSize;
    final CaptureRule captureRule;
    final int stonesPerTurn;

    Variant(int canonicalGameId, int gridSize, CaptureRule captureRule, int stonesPerTurn) {
        this.canonicalGameId = canonicalGameId;
        this.gridSize = gridSize;
        this.captureRule = captureRule;
        this.stonesPerTurn = stonesPerTurn;
    }

    public boolean isConnect6() {
        return this == CONNECT6;
    }

    public boolean isGomoku() {
        return this == GOMOKU;
    }

    public boolean isSwap2() {
        return this == SWAP2_PENTE || this == SWAP2_KERYO;
    }

    public boolean isDPente() {
        return this == D_PENTE || this == DK_PENTE;
    }

    public boolean isGo() {
        return this == GO_9 || this == GO_13 || this == GO_19;
    }

    public boolean isRenju() {
        return this == RENJU;
    }
}
