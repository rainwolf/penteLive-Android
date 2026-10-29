package be.submanifold.pentelive.db;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class LegacyDbQueryTest {
    private static final String LIVE = "live only";
    private static final String TB = "turn-based only";

    @Test
    public void liveOnly() {
        assertEquals("&only_live=yes", LegacyDbQuery.liveOrTbParam("live only", LIVE, TB));
    }

    @Test
    public void turnBasedOnly() {
        assertEquals("&only_turn_based=yes", LegacyDbQuery.liveOrTbParam("turn-based only", LIVE, TB));
    }

    @Test
    public void allIsEmpty() {
        assertEquals("", LegacyDbQuery.liveOrTbParam("all", LIVE, TB));
    }
}
