package be.submanifold.pentelive.db;

import be.submanifold.pentelive.PrefUtils;

/** Persists {@link DbFilters} as one JSON string and migrates the legacy PREFS_DB* keys once. */
public final class DbFilterStore {
    static final String KEY_FILTERS = "__DBF_FILTERS__";
    static final String KEY_VERSION = "__DBF_VERSION__";
    private static final int VERSION = 1;

    /** Legacy {@code db_ratings_array} (strings.xml), indexed by the stored spinner position. */
    private static final int[] LEGACY_RATINGS = {0, 1600, 1700, 1800, 1900, 2000, 2100, 2200, 2300, 2400, 2500, 2600, 2700};
    /** Legacy {@code database_game_types_array} length. */
    private static final int LEGACY_GAME_COUNT = 24;

    /** Localized strings the legacy dialog saved as preference values. */
    public static final class LegacyLabels {
        public final String bothSeats;
        public final String yes;
        public final String liveOnly;
        public final String turnBasedOnly;

        public LegacyLabels(String bothSeats, String yes, String liveOnly, String turnBasedOnly) {
            this.bothSeats = bothSeats;
            this.yes = yes;
            this.liveOnly = liveOnly;
            this.turnBasedOnly = turnBasedOnly;
        }
    }

    private final KeyValueStore kv;
    private final LegacyLabels labels;

    public DbFilterStore(KeyValueStore kv, LegacyLabels labels) {
        this.kv = kv;
        this.labels = labels;
    }

    public DbFilters load() {
        if (kv.getInt(KEY_VERSION, 0) == 0) {
            DbFilters migrated = migrateLegacy();
            kv.putString(KEY_FILTERS, migrated.toJson());
            kv.putInt(KEY_VERSION, VERSION);
            return migrated;
        }
        return DbFilters.fromJson(kv.getString(KEY_FILTERS, null));
    }

    public void save(DbFilters f) {
        kv.putString(KEY_FILTERS, f.toJson());
        kv.putInt(KEY_VERSION, VERSION);
    }

    private DbFilters migrateLegacy() {
        DbFilters.Builder b = new DbFilters.Builder();

        int idx = kv.getInt(PrefUtils.PREFS_DBGAME_KEY, 0);
        if (!kv.getBoolean(PrefUtils.PREFS_DBGAME_SWAP2SHIFT_KEY, false) && idx >= 10 && idx <= 19) {
            idx = Math.min(idx + 2, LEGACY_GAME_COUNT - 1);
        }
        if (idx < 0 || idx >= LEGACY_GAME_COUNT) idx = 0;
        int baseIdx;
        boolean speed = false;
        if (idx < 12) {
            baseIdx = idx;
        } else if (idx < 22) {
            baseIdx = idx - 12;
            speed = true;
        } else {
            baseIdx = 10 + (idx - 22); // Speed Swap2 has no DB ids; use the non-speed Swap2 base
        }
        b.baseGameId(DbGames.baseId(DbGames.BASE_NAMES.get(baseIdx))).speed(speed);

        b.player1MinRating(legacyRating(kv.getInt(PrefUtils.PREFS_DBP1RATING_KEY, 0)));
        b.player2MinRating(legacyRating(kv.getInt(PrefUtils.PREFS_DBP2RATING_KEY, 0)));

        String seats = kv.getString(PrefUtils.PREFS_DBPEITHERORBOTH_KEY, null);
        b.eitherSeat(seats != null && !seats.equals(labels.bothSeats));
        b.excludeTimeouts(labels.yes != null && labels.yes.equals(kv.getString(PrefUtils.PREFS_DBEXCLUDETIMEOUTS_KEY, null)));
        String live = kv.getString(PrefUtils.PREFS_DBLIVEORTB_KEY, null);
        if (live != null && live.equals(labels.liveOnly)) b.liveOrTurnBased(DbFilters.LIVE_ONLY);
        else if (live != null && live.equals(labels.turnBasedOnly)) b.liveOrTurnBased(DbFilters.TURN_BASED_ONLY);
        return b.build();
    }

    private static int legacyRating(int index) {
        return index >= 0 && index < LEGACY_RATINGS.length ? LEGACY_RATINGS[index] : 0;
    }
}
