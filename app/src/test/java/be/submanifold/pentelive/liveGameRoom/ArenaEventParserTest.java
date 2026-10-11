package be.submanifold.pentelive.liveGameRoom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/** Shapes follow spec "Wire protocol": jsonToMap yields Integer or Long numbers and List/Map containers. */
public class ArenaEventParserTest {

    private static Map<String, Object> entry(Object player, Object seq) {
        Map<String, Object> m = new HashMap<>();
        if (player != null) m.put("player", player);
        if (seq != null) m.put("seq", seq);
        return m;
    }

    private static Map<String, Object> snapshot(int table, Object requests) {
        Map<String, Object> m = new HashMap<>();
        m.put("table", table);
        if (requests != null) m.put("requests", requests);
        return m;
    }

    private static List<String> players(List<ArenaJoinRequest> rs) {
        List<String> out = new ArrayList<>();
        for (ArenaJoinRequest r : rs) out.add(r.player);
        return out;
    }

    @Test
    public void table_readsIntegerOrLong() {
        assertEquals(7, ArenaEventParser.table(snapshot(7, null)));
        Map<String, Object> m = new HashMap<>();
        m.put("table", 9L);
        assertEquals(9, ArenaEventParser.table(m));
    }

    @Test
    public void table_missingOrNonNumeric_isMinusOne() {
        // Never a real table id, so the event matches no table instead of throwing on the UI thread
        // (a throw there would also leave the event worker stuck in uiRunnable.wait()).
        assertEquals(-1, ArenaEventParser.table(new HashMap<>()));
        Map<String, Object> m = new HashMap<>();
        m.put("table", "7");
        assertEquals(-1, ArenaEventParser.table(m));
    }

    @Test
    public void joinRequests_sortsBySeqAscending() {
        List<Object> raw = Arrays.<Object>asList(entry("carol", 5), entry("bob", 2), entry("dave", 9));
        List<ArenaJoinRequest> rs = ArenaEventParser.joinRequests(snapshot(7, raw));
        assertEquals(Arrays.asList("bob", "carol", "dave"), players(rs));
        assertEquals(2L, rs.get(0).seq);
    }

    @Test
    public void joinRequests_acceptsLongSeq() {
        List<Object> raw = Arrays.<Object>asList(entry("bob", 3000000000L), entry("carol", 1));
        List<ArenaJoinRequest> rs = ArenaEventParser.joinRequests(snapshot(7, raw));
        assertEquals(Arrays.asList("carol", "bob"), players(rs));
        assertEquals(3000000000L, rs.get(1).seq);
    }

    @Test
    public void joinRequests_keepsOneRowPerPlayer_theLowestSeq() {
        List<Object> raw = Arrays.<Object>asList(entry("bob", 8), entry("carol", 4), entry("bob", 3));
        List<ArenaJoinRequest> rs = ArenaEventParser.joinRequests(snapshot(7, raw));
        assertEquals(Arrays.asList(new ArenaJoinRequest("bob", 3), new ArenaJoinRequest("carol", 4)), rs);
    }

    @Test
    public void joinRequests_missingOrMalformed_isEmptyOrSkipped() {
        assertTrue(ArenaEventParser.joinRequests(snapshot(7, null)).isEmpty());
        assertTrue(ArenaEventParser.joinRequests(snapshot(7, "not a list")).isEmpty());
        assertTrue(ArenaEventParser.joinRequests(snapshot(7, Collections.emptyList())).isEmpty());
        // new Object() stands in for org.json's JSONObject.NULL, which is not a String or Number.
        List<Object> raw = Arrays.<Object>asList(entry(null, 1), entry("bob", null),
                entry(new Object(), 2), entry("carol", new Object()), "junk", entry("dave", 6));
        assertEquals(Collections.singletonList("dave"), players(ArenaEventParser.joinRequests(snapshot(7, raw))));
    }

    @Test
    public void myRequestTables_readsTheSet() {
        Map<String, Object> m = new HashMap<>();
        m.put("tables", Arrays.<Object>asList(3, 5L, 3));
        assertEquals(new HashSet<>(Arrays.asList(3, 5)), ArenaEventParser.myRequestTables(m));
    }

    @Test
    public void myRequestTables_missingOrMalformed_isEmpty() {
        assertTrue(ArenaEventParser.myRequestTables(new HashMap<>()).isEmpty());
        Map<String, Object> m = new HashMap<>();
        m.put("tables", Arrays.<Object>asList("x", new Object()));
        assertTrue(ArenaEventParser.myRequestTables(m).isEmpty());
    }

    @Test
    public void requestEnded_readsAllFields() {
        Map<String, Object> m = new HashMap<>();
        m.put("table", 7);
        m.put("player", "bob");
        m.put("owner", "alice");
        m.put("reason", "DECLINED");
        ArenaRequestEnded e = ArenaEventParser.requestEnded(m);
        assertEquals(7, e.table);
        assertEquals("bob", e.player);
        assertEquals("alice", e.owner);
        assertEquals("DECLINED", e.reason);
    }

    @Test
    public void requestEnded_nonStringFieldsBecomeNull() {
        Map<String, Object> m = new HashMap<>();
        m.put("table", 7);
        m.put("owner", new Object());
        ArenaRequestEnded e = ArenaEventParser.requestEnded(m);
        assertNull(e.player);
        assertNull(e.owner);
        assertNull(e.reason);
    }
}
