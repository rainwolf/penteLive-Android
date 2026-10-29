package be.submanifold.pentelive.db;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Objects;

/** Immutable filter set for the DB screen (spec 3.8). Pure Java. */
public final class DbFilters {
    public static final String LIVE_ALL = "all";
    public static final String LIVE_ONLY = "live";
    public static final String TURN_BASED_ONLY = "turnBased";
    public static final String SCOPE_ARCHIVE = "archive";
    public static final String SCOPE_MINE = "mine";
    public static final String SCOPE_BOTH = "both";

    public final int baseGameId;
    public final boolean speed;
    public final String player1Name;
    public final String player2Name;
    public final int player1MinRating;
    public final int player2MinRating;
    public final boolean eitherSeat;
    public final boolean excludeTimeouts;
    public final int winner;
    public final String liveOrTurnBased;
    public final String afterDate;
    public final String beforeDate;
    public final String scope;

    private DbFilters(Builder b) {
        baseGameId = b.baseGameId;
        speed = b.speed;
        player1Name = clean(b.player1Name);
        player2Name = clean(b.player2Name);
        player1MinRating = Math.max(0, b.player1MinRating);
        player2MinRating = Math.max(0, b.player2MinRating);
        eitherSeat = b.eitherSeat;
        excludeTimeouts = b.excludeTimeouts;
        winner = (b.winner == 1 || b.winner == 2) ? b.winner : 0;
        liveOrTurnBased = (LIVE_ONLY.equals(b.liveOrTurnBased) || TURN_BASED_ONLY.equals(b.liveOrTurnBased))
                ? b.liveOrTurnBased : LIVE_ALL;
        afterDate = clean(b.afterDate);
        beforeDate = clean(b.beforeDate);
        scope = (SCOPE_MINE.equals(b.scope) || SCOPE_BOTH.equals(b.scope)) ? b.scope : SCOPE_ARCHIVE;
    }

    private static String clean(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    public static DbFilters defaults() {
        return new Builder().build();
    }

    public Builder toBuilder() {
        Builder b = new Builder();
        b.baseGameId = baseGameId;
        b.speed = speed;
        b.player1Name = player1Name;
        b.player2Name = player2Name;
        b.player1MinRating = player1MinRating;
        b.player2MinRating = player2MinRating;
        b.eitherSeat = eitherSeat;
        b.excludeTimeouts = excludeTimeouts;
        b.winner = winner;
        b.liveOrTurnBased = liveOrTurnBased;
        b.afterDate = afterDate;
        b.beforeDate = beforeDate;
        b.scope = scope;
        return b;
    }

    /** Game id for requests: base + 1 when Speed is on and allowed for this base. */
    public int gameId() {
        return speed && DbGames.speedAllowed(baseGameId) ? DbGames.speedId(baseGameId) : baseGameId;
    }

    /** A copy with base and speed taken from {@code gameId} (any DB game id, including Speed ids). */
    public DbFilters withGame(int gameId) {
        int base = DbGames.baseId(DbGames.baseName(gameId));
        return toBuilder().baseGameId(base)
                .speed(DbGames.isSpeed(gameId) && DbGames.speedAllowed(base))
                .build();
    }

    private boolean hasNameOrRating() {
        return player1Name != null || player2Name != null || player1MinRating > 0 || player2MinRating > 0;
    }

    /** True when either-seat actually changes the wire seats. */
    public boolean eitherSeatEffective() {
        return eitherSeat && hasNameOrRating();
    }

    /** Looks only at wire fields; game, speed and scope never make it non-default. */
    public boolean isDefault() {
        return wireIsDefault(true);
    }

    private boolean wireIsDefault(boolean serverFiltersB3B4) {
        return player1Name == null && player2Name == null
                && player1MinRating == 0 && player2MinRating == 0
                && winner == 0 && afterDate == null && beforeDate == null
                && (!serverFiltersB3B4 || (!excludeTimeouts && LIVE_ALL.equals(liveOrTurnBased)));
    }

    /** Wire {@code filters} object, or null when everything sent would be at its default. */
    public JsonObject toWire(boolean serverFiltersB3B4) {
        if (wireIsDefault(serverFiltersB3B4)) return null;
        JsonObject o = new JsonObject();
        put(o, "player1Name", player1Name);
        o.addProperty("player1Seat", eitherSeatEffective() ? 0 : 1);
        put(o, "player2Name", player2Name);
        o.addProperty("player2Seat", eitherSeatEffective() ? 0 : 2);
        o.add("site", JsonNull.INSTANCE);
        o.add("event", JsonNull.INSTANCE);
        o.add("round", JsonNull.INSTANCE);
        o.add("section", JsonNull.INSTANCE);
        put(o, "afterDate", afterDate);
        put(o, "beforeDate", beforeDate);
        o.addProperty("winner", winner);
        o.addProperty("player1RatingAbove", player1MinRating);
        o.addProperty("player2RatingAbove", player2MinRating);
        o.addProperty("excludeTimeouts", serverFiltersB3B4 && excludeTimeouts);
        o.addProperty("liveOrTurnBased", serverFiltersB3B4 ? liveOrTurnBased : LIVE_ALL);
        return o;
    }

    private static void put(JsonObject o, String key, String v) {
        if (v == null) o.add(key, JsonNull.INSTANCE);
        else o.addProperty(key, v);
    }

    /** Store/sheet serialisation (all fields, including non-wire ones). */
    public String toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("baseGameId", baseGameId);
        o.addProperty("speed", speed);
        put(o, "player1Name", player1Name);
        put(o, "player2Name", player2Name);
        o.addProperty("player1MinRating", player1MinRating);
        o.addProperty("player2MinRating", player2MinRating);
        o.addProperty("eitherSeat", eitherSeat);
        o.addProperty("excludeTimeouts", excludeTimeouts);
        o.addProperty("winner", winner);
        o.addProperty("liveOrTurnBased", liveOrTurnBased);
        put(o, "afterDate", afterDate);
        put(o, "beforeDate", beforeDate);
        o.addProperty("scope", scope);
        return o.toString();
    }

    /** Parses {@link #toJson()}; null, blank or malformed input gives the defaults. */
    public static DbFilters fromJson(String json) {
        if (json == null || json.trim().isEmpty()) return defaults();
        try {
            JsonObject o = JsonParser.parseString(json).getAsJsonObject();
            Builder b = new Builder();
            int base = intOf(o, "baseGameId", 1);
            b.baseGameId = DbGames.isSupported(base) ? base : 1;
            b.speed = boolOf(o, "speed");
            b.player1Name = strOf(o, "player1Name");
            b.player2Name = strOf(o, "player2Name");
            b.player1MinRating = intOf(o, "player1MinRating", 0);
            b.player2MinRating = intOf(o, "player2MinRating", 0);
            b.eitherSeat = boolOf(o, "eitherSeat");
            b.excludeTimeouts = boolOf(o, "excludeTimeouts");
            b.winner = intOf(o, "winner", 0);
            b.liveOrTurnBased = strOf(o, "liveOrTurnBased");
            b.afterDate = strOf(o, "afterDate");
            b.beforeDate = strOf(o, "beforeDate");
            b.scope = strOf(o, "scope");
            return b.build();
        } catch (RuntimeException e) {
            return defaults();
        }
    }

    private static String strOf(JsonObject o, String k) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : null;
    }

    private static int intOf(JsonObject o, String k, int d) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsInt() : d;
    }

    private static boolean boolOf(JsonObject o, String k) {
        return o.has(k) && !o.get(k).isJsonNull() && o.get(k).getAsBoolean();
    }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof DbFilters)) return false;
        DbFilters f = (DbFilters) other;
        return baseGameId == f.baseGameId && speed == f.speed
                && Objects.equals(player1Name, f.player1Name) && Objects.equals(player2Name, f.player2Name)
                && player1MinRating == f.player1MinRating && player2MinRating == f.player2MinRating
                && eitherSeat == f.eitherSeat && excludeTimeouts == f.excludeTimeouts
                && winner == f.winner && liveOrTurnBased.equals(f.liveOrTurnBased)
                && Objects.equals(afterDate, f.afterDate) && Objects.equals(beforeDate, f.beforeDate)
                && scope.equals(f.scope);
    }

    @Override public int hashCode() {
        return Objects.hash(baseGameId, speed, player1Name, player2Name, player1MinRating, player2MinRating,
                eitherSeat, excludeTimeouts, winner, liveOrTurnBased, afterDate, beforeDate, scope);
    }

    @Override public String toString() {
        return "DbFilters" + toJson();
    }

    public static final class Builder {
        int baseGameId = 1;
        boolean speed;
        String player1Name;
        String player2Name;
        int player1MinRating;
        int player2MinRating;
        boolean eitherSeat;
        boolean excludeTimeouts;
        int winner;
        String liveOrTurnBased = LIVE_ALL;
        String afterDate;
        String beforeDate;
        String scope = SCOPE_ARCHIVE;

        public Builder baseGameId(int v) { baseGameId = v; return this; }
        public Builder speed(boolean v) { speed = v; return this; }
        public Builder player1Name(String v) { player1Name = v; return this; }
        public Builder player2Name(String v) { player2Name = v; return this; }
        public Builder player1MinRating(int v) { player1MinRating = v; return this; }
        public Builder player2MinRating(int v) { player2MinRating = v; return this; }
        public Builder eitherSeat(boolean v) { eitherSeat = v; return this; }
        public Builder excludeTimeouts(boolean v) { excludeTimeouts = v; return this; }
        public Builder winner(int v) { winner = v; return this; }
        public Builder liveOrTurnBased(String v) { liveOrTurnBased = v; return this; }
        public Builder afterDate(String v) { afterDate = v; return this; }
        public Builder beforeDate(String v) { beforeDate = v; return this; }
        public Builder scope(String v) { scope = v; return this; }

        public DbFilters build() {
            return new DbFilters(this);
        }
    }
}
