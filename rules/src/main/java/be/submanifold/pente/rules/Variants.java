package be.submanifold.pente.rules;

import java.util.HashMap;
import java.util.Map;

/** Classifies server game ids and labels into {@link Variant}s, and exposes
 *  the per-variant descriptor data. Pure Java; no Android imports. */
public final class Variants {

    private static final Map<Integer, Variant> BY_CANONICAL_ID = new HashMap<>();

    static {
        for (Variant v : Variant.values()) {
            BY_CANONICAL_ID.put(v.canonicalGameId, v);
        }
    }

    private Variants() {
    }

    // Overlap chains — longer substring MUST precede shorter:
    //   "Swap2-" (handled first, before Keryo/Pente)
    //   "Keryo-Pente" before "Pente"
    //   "DK-Pente" / "D-Pente" before "Pente"
    //   "Go (9x9)" / "Go (13x13)" before "Go"

    /**
     * Classifies a server game-type label (e.g. "Pente", "Speed Keryo-Pente",
     * "Go (9x9)") into a {@link Variant}. The leading "Speed " qualifier is
     * irrelevant. Checks are ordered most-specific first so that substring
     * overlaps do not misclassify. Returns null for unknown or null input.
     */
    public static Variant fromGameType(String gameType) {
        if (gameType == null) {
            return null;
        }
        if (gameType.contains("Swap2-")) {
            return gameType.contains("Keryo") ? Variant.SWAP2_KERYO : Variant.SWAP2_PENTE;
        }
        if (gameType.contains("Connect6")) {
            return Variant.CONNECT6;
        }
        if (gameType.contains("Gomoku")) {
            return Variant.GOMOKU;
        }
        if (gameType.contains("Keryo-Pente")) {
            return Variant.KERYO_PENTE;
        }
        if (gameType.contains("DK-Pente")) {
            return Variant.DK_PENTE;
        }
        if (gameType.contains("D-Pente")) {
            return Variant.D_PENTE;
        }
        if (gameType.contains("Boat-Pente")) {
            return Variant.BOAT_PENTE;
        }
        if (gameType.contains("G-Pente")) {
            return Variant.G_PENTE;
        }
        if (gameType.contains("Poof-Pente")) {
            return Variant.POOF_PENTE;
        }
        if (gameType.contains("O-Pente")) {
            return Variant.O_PENTE;
        }
        if (gameType.contains("Pente")) {
            return Variant.PENTE;
        }
        if (gameType.contains("Go (9x9)")) {
            return Variant.GO_9;
        }
        if (gameType.contains("Go (13x13)")) {
            return Variant.GO_13;
        }
        if (gameType.contains("Go")) {
            return Variant.GO_19;
        }
        return null;
    }

    /**
     * Maps a numeric game id to a {@link Variant}. Even ids are the "Speed"
     * sibling of the preceding odd canonical id (canonical = gameId - 1 for
     * even inputs). Returns null for unknown ids.
     */
    public static Variant fromGameId(int gameId) {
        int canonical = (gameId % 2 == 0) ? gameId - 1 : gameId;
        return BY_CANONICAL_ID.get(canonical);
    }

    public static int gridSize(Variant v) {
        if (v == null) throw new IllegalArgumentException("Variant must not be null");
        return v.gridSize;
    }

    public static CaptureRule captureRule(Variant v) {
        if (v == null) throw new IllegalArgumentException("Variant must not be null");
        return v.captureRule;
    }

    public static int stonesPerTurn(Variant v) {
        if (v == null) throw new IllegalArgumentException("Variant must not be null");
        return v.stonesPerTurn;
    }
}
