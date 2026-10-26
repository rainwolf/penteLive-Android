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
        if (gameType.contains("Renju")) {
            return Variant.RENJU;
        }
        return null;
    }

    /**
     * Maps a numeric game id to a {@link Variant}. Even ids are the "Speed"
     * sibling of the preceding odd canonical id (canonical = gameId - 1 for
     * even inputs). Returns null for unknown ids.
     */
    public static Variant fromGameId(int gameId) {
        if (gameId == 81) return Variant.RENJU; // TB Renju: no canonical enum id
        int canonical = (gameId % 2 == 0) ? gameId - 1 : gameId;
        return BY_CANONICAL_ID.get(canonical);
    }

    /**
     * True when the app knows this server game id: live ids 1..32 (even = Speed
     * sibling) and turn-based ids 51..82 (live id + 50). Anything else (0, new
     * experimental ids such as 33/34 or 83/84) is unknown and callers skip it.
     */
    public static boolean isKnownGameId(int gameId) {
        int live = gameId > 50 ? gameId - 50 : gameId;
        if (live < 1) {
            return false;
        }
        int canonical = (live % 2 == 0) ? live - 1 : live;
        return BY_CANONICAL_ID.containsKey(canonical);
    }

    /**
     * True when the label is exactly a known server game name, optionally with
     * the "Speed " prefix. Unlike {@link #fromGameType} this is strict: it does
     * not substring-match, so an unknown "Foo-Pente" is not treated as Pente.
     */
    public static boolean isKnownGameType(String gameType) {
        if (gameType == null) {
            return false;
        }
        String base = gameType.startsWith("Speed ") ? gameType.substring(6) : gameType;
        switch (base) {
            case "Pente":
            case "Keryo-Pente":
            case "Gomoku":
            case "D-Pente":
            case "G-Pente":
            case "Poof-Pente":
            case "Connect6":
            case "Boat-Pente":
            case "DK-Pente":
            case "Go":
            case "Go (9x9)":
            case "Go (13x13)":
            case "O-Pente":
            case "Swap2-Pente":
            case "Swap2-Keryo":
            case "Renju":
                return true;
            default:
                return false;
        }
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
