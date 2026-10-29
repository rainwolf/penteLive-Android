package be.submanifold.pentelive.db;

/** Crown codes from the server: 1 gold, 2 silver, 3 bronze, 4+ King of the Hill. */
public final class Crowns {
    /** Length of {@code KothCrownDrawables.IDS}; a unit test keeps them equal. */
    public static final int KOTH_COUNT = 39;

    public enum Kind { NONE, GOLD, SILVER, BRONZE, KOTH }

    private Crowns() {
    }

    /** The code when it is 1..(3 + KOTH_COUNT), else 0. */
    public static int safeCode(int crown) {
        return crown >= 1 && crown <= 3 + KOTH_COUNT ? crown : 0;
    }

    public static Kind kind(int crown) {
        switch (safeCode(crown)) {
            case 0:
                return Kind.NONE;
            case 1:
                return Kind.GOLD;
            case 2:
                return Kind.SILVER;
            case 3:
                return Kind.BRONZE;
            default:
                return Kind.KOTH;
        }
    }

    /** Index into {@code KothCrownDrawables.IDS}, or -1 when the crown is not a KOTH crown. */
    public static int kothIndex(int crown) {
        return kind(crown) == Kind.KOTH ? crown - 4 : -1;
    }
}
