package be.submanifold.pentelive.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import be.submanifold.pentelive.PrefUtils;

import org.junit.Test;

public class DbFilterStoreTest {
    private static final DbFilterStore.LegacyLabels LABELS =
            new DbFilterStore.LegacyLabels("Both", "Yes", "Live only", "Turn-based only");

    private final InMemoryKeyValueStore kv = new InMemoryKeyValueStore();
    private final DbFilterStore store = new DbFilterStore(kv, LABELS);

    @Test public void roundTrip() {
        store.load();
        DbFilters f = DbFilters.defaults().toBuilder().baseGameId(3).player1Name("alice").scope("mine").build();
        store.save(f);
        assertEquals(f, new DbFilterStore(kv, LABELS).load());
    }

    @Test public void migratesLegacyIndices() {
        kv.putBoolean(PrefUtils.PREFS_DBGAME_SWAP2SHIFT_KEY, true);
        kv.putInt(PrefUtils.PREFS_DBGAME_KEY, 13);
        kv.putInt(PrefUtils.PREFS_DBP2RATING_KEY, 2);
        DbFilters f = store.load();
        assertEquals(3, f.baseGameId);
        assertTrue(f.speed);
        assertEquals(1700, f.player2MinRating);
        assertEquals(0, f.player1MinRating);
    }

    @Test public void legacyIndexesKeepTheirGame() {
        // The legacy spinner order is a frozen contract: adding Go/Renju to the picker must not remap it.
        String[] legacy = {"Pente", "Keryo-Pente", "Gomoku", "D-Pente", "G-Pente", "Poof-Pente",
                "Connect6", "Boat-Pente", "DK-Pente", "O-Pente", "Swap2-Pente", "Swap2-Keryo"};
        for (int i = 0; i < legacy.length; i++) {
            DbFilters f = migrate(i);
            assertEquals("index " + i, DbGames.baseId(legacy[i]), f.baseGameId);
            assertFalse("index " + i, f.speed);
        }
        DbFilters speedO = migrate(21);
        assertEquals(DbGames.baseId("O-Pente"), speedO.baseGameId);
        assertTrue(speedO.speed);
        assertEquals(DbGames.baseId("Swap2-Pente"), migrate(22).baseGameId);
    }

    private DbFilters migrate(int legacyIndex) {
        InMemoryKeyValueStore fresh = new InMemoryKeyValueStore();
        fresh.putBoolean(PrefUtils.PREFS_DBGAME_SWAP2SHIFT_KEY, true);
        fresh.putInt(PrefUtils.PREFS_DBGAME_KEY, legacyIndex);
        return new DbFilterStore(fresh, LABELS).load();
    }

    @Test public void migrationAppliesSwap2ShiftWhenUnflagged() {
        kv.putInt(PrefUtils.PREFS_DBGAME_KEY, 10); // pre-Swap2 "Speed Pente"
        DbFilters f = store.load();
        assertEquals(1, f.baseGameId);
        assertTrue(f.speed);
    }

    @Test public void legacySpeedSwap2MapsToBase() {
        kv.putBoolean(PrefUtils.PREFS_DBGAME_SWAP2SHIFT_KEY, true);
        kv.putInt(PrefUtils.PREFS_DBGAME_KEY, 22);
        DbFilters f = store.load();
        assertEquals(DbGames.baseId("Swap2-Pente"), f.baseGameId);
        assertFalse(f.speed);
        kv.putInt(DbFilterStore.KEY_VERSION, 0);
        kv.putInt(PrefUtils.PREFS_DBGAME_KEY, 23);
        assertEquals(DbGames.baseId("Swap2-Keryo"), store.load().baseGameId);
    }

    @Test public void migratesLegacyLabels() {
        kv.putString(PrefUtils.PREFS_DBPEITHERORBOTH_KEY, "Either");
        kv.putString(PrefUtils.PREFS_DBEXCLUDETIMEOUTS_KEY, "Yes");
        kv.putString(PrefUtils.PREFS_DBLIVEORTB_KEY, "Live only");
        DbFilters f = store.load();
        assertTrue(f.eitherSeat);
        assertTrue(f.excludeTimeouts);
        assertEquals("live", f.liveOrTurnBased);
    }

    @Test public void bothSeatsLabelMeansNotEither() {
        kv.putString(PrefUtils.PREFS_DBPEITHERORBOTH_KEY, "Both");
        assertFalse(store.load().eitherSeat);
    }

    @Test public void migrationRunsOnce() {
        kv.putInt(PrefUtils.PREFS_DBGAME_KEY, 13);
        kv.putBoolean(PrefUtils.PREFS_DBGAME_SWAP2SHIFT_KEY, true);
        DbFilters first = store.load();
        kv.putInt(PrefUtils.PREFS_DBGAME_KEY, 2);
        assertEquals(first, store.load());
        assertTrue(kv.getInt(DbFilterStore.KEY_VERSION, 0) > 0);
    }
}
