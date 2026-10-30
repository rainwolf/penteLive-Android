package be.submanifold.pentelive.db;

/** Order of the Moves list (and the board heat slice). Mirrors the web app's SORT_MODES. */
public enum MoveSort {
    /** Win %: the Wilson lower-bound ranking. The default. */
    WINRATE("winrate"),
    /** Popularity: most games first, Wilson order for equal counts. */
    GAMES("games");

    public final String key;

    MoveSort(String key) {
        this.key = key;
    }

    /** The sort stored under key; anything unknown or missing is the default, WINRATE. */
    public static MoveSort fromKey(String key) {
        for (MoveSort s : values()) {
            if (s.key.equals(key)) return s;
        }
        return WINRATE;
    }
}
