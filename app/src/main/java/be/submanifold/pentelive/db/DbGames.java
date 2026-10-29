package be.submanifold.pentelive.db;

import be.submanifold.pente.rules.Variant;
import be.submanifold.pente.rules.Variants;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Game table for the DB screen: names, ids, Speed rules, board colours. Pure Java. */
public final class DbGames {
    public static final List<String> BASE_NAMES = Collections.unmodifiableList(Arrays.asList(
            "Pente", "Keryo-Pente", "Gomoku", "D-Pente", "G-Pente", "Poof-Pente",
            "Connect6", "Boat-Pente", "DK-Pente", "O-Pente", "Swap2-Pente", "Swap2-Keryo"));

    private static final Map<Variant, Integer> BOARD_ARGB = new EnumMap<>(Variant.class);

    static {
        BOARD_ARGB.put(Variant.PENTE, 0xFFFDDEA3);
        BOARD_ARGB.put(Variant.KERYO_PENTE, 0xFFBAFDA3);
        BOARD_ARGB.put(Variant.GOMOKU, 0xFFA3FDEB);
        BOARD_ARGB.put(Variant.D_PENTE, 0xFFA3CDFD);
        BOARD_ARGB.put(Variant.G_PENTE, 0xFFAEA3FD);
        BOARD_ARGB.put(Variant.POOF_PENTE, 0xFFEDA3FD);
        BOARD_ARGB.put(Variant.CONNECT6, 0xFFEDA3FD);
        BOARD_ARGB.put(Variant.BOAT_PENTE, 0xFF25BAFF);
        BOARD_ARGB.put(Variant.DK_PENTE, 0xFFFFA500);
        BOARD_ARGB.put(Variant.O_PENTE, 0xFF52BE80);
        BOARD_ARGB.put(Variant.SWAP2_PENTE, 0xFFE5AA70);
        BOARD_ARGB.put(Variant.SWAP2_KERYO, 0xFF50C878);
    }

    private DbGames() {
    }

    private static Variant variantOf(int anyGameId) {
        int id = anyGameId > 50 ? anyGameId - 50 : anyGameId;
        return Variants.fromGameId(id);
    }

    private static Variant required(int anyGameId) {
        Variant v = variantOf(anyGameId);
        if (v == null) {
            throw new IllegalArgumentException("Unknown game id " + anyGameId);
        }
        return v;
    }

    public static int baseId(String baseName) {
        Variant v = Variants.fromGameType(baseName);
        if (v == null || !BOARD_ARGB.containsKey(v)) {
            throw new IllegalArgumentException("Unknown game " + baseName);
        }
        return Variants.canonicalGameId(v);
    }

    public static String baseName(int anyGameId) {
        return nameOf(required(anyGameId));
    }

    private static String nameOf(Variant v) {
        for (String n : BASE_NAMES) {
            if (Variants.fromGameType(n) == v) {
                return n;
            }
        }
        throw new IllegalArgumentException("No base name for " + v);
    }

    public static Integer fromGameType(String label) {
        Variant v = Variants.fromGameType(label);
        if (v == null || !BOARD_ARGB.containsKey(v)) {
            return null;
        }
        int id = Variants.canonicalGameId(v);
        if (label.startsWith("Speed ") && speedAllowed(id)) {
            id = speedId(id);
        }
        return id;
    }

    public static int speedId(int baseId) {
        return baseId + 1;
    }

    public static boolean speedAllowed(int baseId) {
        Variant v = variantOf(baseId);
        return v != null && !v.isSwap2();
    }

    public static boolean isSpeed(int gameId) {
        int id = gameId > 50 ? gameId - 50 : gameId;
        return id % 2 == 0;
    }

    public static int boardArgb(int anyGameId) {
        return BOARD_ARGB.get(required(anyGameId));
    }

    public static int gridSize(int gameId) {
        return Variants.gridSize(required(gameId));
    }

    public static boolean showsCaptureIcons(int gameId) {
        Variant v = required(gameId);
        return !(v.isGomoku() || v.isConnect6());
    }

    public static int moveFloor(int gameId) {
        Variant v = required(gameId);
        return (v.isDPente() || v.isSwap2()) ? 0 : 1;
    }

    public static List<Integer> seed(int gameId) {
        return moveFloor(gameId) == 1 ? Collections.singletonList(180) : new ArrayList<Integer>();
    }

    public static boolean stepsByTurn(int gameId) {
        return DbCapabilities.CONNECT6_TURN_STEPS && required(gameId).isConnect6();
    }
}
