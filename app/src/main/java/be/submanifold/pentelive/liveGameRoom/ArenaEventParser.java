package be.submanifold.pentelive.liveGameRoom;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** Decodes the arena server-to-client events from the Map that LiveGameRoomActivity.jsonToMap
 *  builds (numbers are Integer or Long, arrays List, objects Map). Plain Java so it is unit-tested
 *  without org.json, which is a stub in local unit tests. */
public final class ArenaEventParser {
    private ArenaEventParser() {}

    /** The event's table id, or -1 (matches no table) when it is missing or not a number. */
    public static int table(Map<String, Object> data) {
        Object t = data.get("table");
        return t instanceof Number ? ((Number) t).intValue() : -1;
    }

    /** dsgArenaJoinRequestsEvent: rows in seq order, one per player (the lowest seq wins). */
    public static List<ArenaJoinRequest> joinRequests(Map<String, Object> data) {
        List<ArenaJoinRequest> all = new ArrayList<>();
        Object raw = data.get("requests");
        if (raw instanceof List) {
            for (Object o : (List<?>) raw) {
                if (!(o instanceof Map)) continue;
                Map<?, ?> m = (Map<?, ?>) o;
                Object player = m.get("player");
                Object seq = m.get("seq");
                if (player instanceof String && seq instanceof Number) {
                    all.add(new ArenaJoinRequest((String) player, ((Number) seq).longValue()));
                }
            }
        }
        all.sort(Comparator.comparingLong(r -> r.seq));
        List<ArenaJoinRequest> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (ArenaJoinRequest r : all) {
            if (seen.add(r.player)) out.add(r);
        }
        return out;
    }

    /** dsgArenaMyRequestsEvent: the tables this player has pending requests at. */
    public static Set<Integer> myRequestTables(Map<String, Object> data) {
        Set<Integer> out = new TreeSet<>();
        Object raw = data.get("tables");
        if (raw instanceof List) {
            for (Object o : (List<?>) raw) {
                if (o instanceof Number) out.add(((Number) o).intValue());
            }
        }
        return out;
    }

    public static ArenaRequestEnded requestEnded(Map<String, Object> data) {
        return new ArenaRequestEnded(table(data), str(data.get("player")),
                str(data.get("owner")), str(data.get("reason")));
    }

    private static String str(Object o) {
        return o instanceof String ? (String) o : null;
    }
}
