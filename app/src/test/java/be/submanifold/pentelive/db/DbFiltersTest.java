package be.submanifold.pentelive.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonObject;

import org.junit.Test;

public class DbFiltersTest {
    @Test public void defaultsSerializeNull() {
        assertNull(DbFilters.defaults().toWire(true));
        DbFilters f = DbFilters.defaults().toBuilder().baseGameId(3).speed(true).scope("mine").build();
        assertNull(f.toWire(true));
        assertNull(f.toWire(false));
        assertTrue(f.isDefault());
    }

    @Test public void stringKeysNullNotEmpty() {
        DbFilters f = DbFilters.defaults().toBuilder().player1Name("alice").player2Name("  ").build();
        JsonObject w = f.toWire(true);
        assertEquals("alice", w.get("player1Name").getAsString());
        assertTrue(w.has("player2Name"));
        assertTrue(w.get("player2Name").isJsonNull());
        assertTrue(w.get("site").isJsonNull());
        assertTrue(w.get("afterDate").isJsonNull());
    }

    @Test public void eitherSeatSendsZeroZero() {
        JsonObject w = DbFilters.defaults().toBuilder().eitherSeat(true).player1Name("alice").build().toWire(true);
        assertEquals(0, w.get("player1Seat").getAsInt());
        assertEquals(0, w.get("player2Seat").getAsInt());
    }

    @Test public void eitherSeatWithoutNameOrRatingStaysOneTwo() {
        JsonObject w = DbFilters.defaults().toBuilder().eitherSeat(true).winner(1).build().toWire(true);
        assertEquals(1, w.get("player1Seat").getAsInt());
        assertEquals(2, w.get("player2Seat").getAsInt());
    }

    @Test public void ratingSentAsIs() {
        JsonObject w = DbFilters.defaults().toBuilder().player1MinRating(1700).build().toWire(true);
        assertEquals(1700, w.get("player1RatingAbove").getAsInt());
        assertEquals(0, w.get("player2RatingAbove").getAsInt());
    }

    @Test public void capabilityOffForcesB3B4Defaults() {
        DbFilters f = DbFilters.defaults().toBuilder().player1Name("a").excludeTimeouts(true)
                .liveOrTurnBased("live").build();
        JsonObject w = f.toWire(false);
        assertFalse(w.get("excludeTimeouts").getAsBoolean());
        assertEquals("all", w.get("liveOrTurnBased").getAsString());
        // only B3/B4 set and capability off: nothing would be sent
        assertNull(DbFilters.defaults().toBuilder().excludeTimeouts(true).build().toWire(false));
    }

    @Test public void capabilityOnSendsB3B4() {
        JsonObject w = DbFilters.defaults().toBuilder().excludeTimeouts(true).liveOrTurnBased("turnBased")
                .build().toWire(true);
        assertTrue(w.get("excludeTimeouts").getAsBoolean());
        assertEquals("turnBased", w.get("liveOrTurnBased").getAsString());
    }

    @Test public void speedGameId() {
        assertEquals(4, DbFilters.defaults().toBuilder().baseGameId(3).speed(true).build().gameId());
        assertEquals(3, DbFilters.defaults().toBuilder().baseGameId(3).build().gameId());
    }

    @Test public void speedIgnoredForSwap2() {
        assertEquals(27, DbFilters.defaults().toBuilder().baseGameId(27).speed(true).build().gameId());
    }

    @Test public void withGameTakesBaseAndSpeed() {
        DbFilters f = DbFilters.defaults().withGame(4);
        assertEquals(3, f.baseGameId);
        assertTrue(f.speed);
        assertEquals(1, f.withGame(1).baseGameId);
        assertFalse(f.withGame(1).speed);
    }

    @Test public void jsonRoundTrip() {
        DbFilters f = DbFilters.defaults().toBuilder().baseGameId(3).speed(true).player1Name("alice")
                .player2Name("bob").player1MinRating(1700).player2MinRating(2000).eitherSeat(true)
                .excludeTimeouts(true).winner(2).liveOrTurnBased("live").afterDate("2020-01-02")
                .beforeDate("2021-03-04").scope("both").build();
        assertEquals(f, DbFilters.fromJson(f.toJson()));
    }

    @Test public void badJsonGivesDefaults() {
        assertEquals(DbFilters.defaults(), DbFilters.fromJson("not json"));
        assertEquals(DbFilters.defaults(), DbFilters.fromJson(null));
    }

    @Test public void withGoGameKeepsBaseAndSpeed() {
        DbFilters f = DbFilters.defaults().withGame(22);
        assertEquals(21, f.baseGameId);
        assertTrue(f.speed);
        assertEquals(22, f.gameId());
        assertEquals(31, DbFilters.defaults().withGame(81).baseGameId);
        assertEquals(23, DbFilters.defaults().withGame(73).gameId());
    }

    @Test public void fromJsonKeepsGoAndRenjuBases() {
        DbFilters r = DbFilters.defaults().toBuilder().baseGameId(31).speed(true).build();
        assertEquals(r, DbFilters.fromJson(r.toJson()));
        DbFilters g = DbFilters.defaults().toBuilder().baseGameId(23).build();
        assertEquals(23, DbFilters.fromJson(g.toJson()).baseGameId);
        DbFilters unknown = DbFilters.defaults().toBuilder().baseGameId(33).build();
        assertEquals(1, DbFilters.fromJson(unknown.toJson()).baseGameId);
    }

    @Test public void goWireGameIds() {
        int g9 = DbFilters.defaults().toBuilder().baseGameId(21).build().gameId();
        int g13s = DbFilters.defaults().toBuilder().baseGameId(23).speed(true).build().gameId();
        int rs = DbFilters.defaults().toBuilder().baseGameId(31).speed(true).build().gameId();
        assertEquals(21, wireGame(DbRequests.positionStats(g9, java.util.Arrays.asList(40), "archive", null)));
        assertEquals(24, wireGame(DbRequests.gamesSearch(g13s, null, "archive", null, 0, 25)));
        assertEquals(32, wireGame(DbRequests.positionStats(rs, java.util.Arrays.asList(112), "archive", null)));
    }

    private static int wireGame(String body) {
        return com.google.gson.JsonParser.parseString(body).getAsJsonObject().get("game").getAsInt();
    }
}
