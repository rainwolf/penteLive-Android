package be.submanifold.pentelive.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Test;

public class DbRequestsTest {
    @Test
    public void positionStatsShape() {
        JsonObject o = JsonParser.parseString(
                DbRequests.positionStats(1, Arrays.asList(180), "archive", null)).getAsJsonObject();
        assertEquals(new HashSet<>(Arrays.asList("game", "moves", "scope", "filters")), o.keySet());
        assertTrue(o.get("filters").isJsonNull());
        assertEquals(1, o.get("game").getAsInt());
        assertEquals("archive", o.get("scope").getAsString());
        assertEquals(1, o.getAsJsonArray("moves").size());
    }

    @Test
    public void filterOnlySearchOmitsMoves() {
        JsonObject o = JsonParser.parseString(
                DbRequests.gamesSearch(1, null, "archive", null, 0, 25)).getAsJsonObject();
        assertFalse(o.has("moves"));
        assertEquals(0, o.get("offset").getAsInt());
        assertEquals(25, o.get("limit").getAsInt());
        assertTrue(o.has("filters"));
        assertTrue(o.get("filters").isJsonNull());
    }

    @Test
    public void searchWithMovesAndFilters() {
        JsonObject f = new JsonObject();
        f.addProperty("player", (String) null);
        f.addProperty("minRating", 1500);
        JsonObject o = JsonParser.parseString(
                DbRequests.gamesSearch(2, Arrays.asList(180, 181), "mine", f, 25, 25)).getAsJsonObject();
        assertEquals(2, o.getAsJsonArray("moves").size());
        assertEquals(1500, o.getAsJsonObject("filters").get("minRating").getAsInt());
        assertTrue(o.getAsJsonObject("filters").has("player"));
    }
}
