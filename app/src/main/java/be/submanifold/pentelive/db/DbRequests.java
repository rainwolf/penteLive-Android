package be.submanifold.pentelive.db;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import java.util.List;

/** Request bodies for /api/db POST endpoints. */
public final class DbRequests {
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    private DbRequests() {}

    public static String positionStats(int game, List<Integer> moves, String scope, JsonObject filtersOrNull) {
        JsonObject o = new JsonObject();
        o.addProperty("game", game);
        o.add("moves", movesArray(moves));
        o.addProperty("scope", scope);
        o.add("filters", filtersOrNull == null ? JsonNull.INSTANCE : filtersOrNull);
        return GSON.toJson(o);
    }

    /** {@code movesOrNull == null} leaves out the {@code moves} key (filter-only search). */
    public static String gamesSearch(int game, List<Integer> movesOrNull, String scope, JsonObject filtersOrNull,
                                     int offset, int limit) {
        JsonObject o = new JsonObject();
        o.addProperty("game", game);
        if (movesOrNull != null) o.add("moves", movesArray(movesOrNull));
        o.addProperty("scope", scope);
        o.add("filters", filtersOrNull == null ? JsonNull.INSTANCE : filtersOrNull);
        o.addProperty("offset", offset);
        o.addProperty("limit", limit);
        return GSON.toJson(o);
    }

    private static JsonElement movesArray(List<Integer> moves) {
        JsonArray a = new JsonArray();
        for (Integer m : moves) a.add(m);
        return a;
    }
}
