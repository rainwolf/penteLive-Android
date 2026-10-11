package be.submanifold.pentelive.liveGameRoom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListUpdateCallback;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Spec C2 rows: matched by player, so a snapshot change moves/removes/inserts rows and an
 *  in-flight mark rebinds one row in place. */
public class ArenaRequestRowsTest {

    private static ArenaJoinRequest req(String player, long seq) {
        return new ArenaJoinRequest(player, seq);
    }

    private static List<ArenaRequestRows.Row> rows(String inFlight, ArenaJoinRequest... rs) {
        return ArenaRequestRows.build(Arrays.asList(rs), inFlight);
    }

    /** Runs the diff and records each update DiffUtil dispatches to the adapter. */
    private static List<String> updates(List<ArenaRequestRows.Row> old, List<ArenaRequestRows.Row> next) {
        final List<String> out = new ArrayList<>();
        DiffUtil.calculateDiff(ArenaRequestRows.diff(old, next)).dispatchUpdatesTo(new ListUpdateCallback() {
            @Override
            public void onInserted(int position, int count) {
                out.add("insert " + position + " " + count);
            }

            @Override
            public void onRemoved(int position, int count) {
                out.add("remove " + position + " " + count);
            }

            @Override
            public void onMoved(int fromPosition, int toPosition) {
                out.add("move " + fromPosition + " " + toPosition);
            }

            @Override
            public void onChanged(int position, int count, Object payload) {
                out.add("change " + position + " " + count);
            }
        });
        return out;
    }

    @Test
    public void build_keepsSnapshotOrder_andMarksOnlyTheInFlightPlayer() {
        List<ArenaRequestRows.Row> r = rows("carol", req("bob", 1), req("carol", 2));
        assertEquals("bob", r.get(0).player);
        assertEquals(1L, r.get(0).seq);
        assertFalse(r.get(0).inFlight);
        assertTrue(r.get(1).inFlight);
        for (ArenaRequestRows.Row row : rows(null, req("bob", 1), req("carol", 2))) {
            assertFalse(row.inFlight);
        }
    }

    @Test
    public void sameRows_dispatchNothing() {
        assertEquals(Collections.emptyList(),
                updates(rows(null, req("bob", 1)), rows(null, req("bob", 1))));
    }

    @Test
    public void inFlightMark_rebindsThatRowInPlace() {
        assertEquals(Collections.singletonList("change 0 1"),
                updates(rows(null, req("bob", 1), req("carol", 2)), rows("bob", req("bob", 1), req("carol", 2))));
    }

    @Test
    public void newSeqForTheSamePlayer_rebindsThatRow() {
        assertEquals(Collections.singletonList("change 0 1"),
                updates(rows(null, req("bob", 1)), rows(null, req("bob", 5))));
    }

    @Test
    public void answeredRow_isRemoved_andTheOthersAreNotRebound() {
        assertEquals(Collections.singletonList("remove 0 1"),
                updates(rows("bob", req("bob", 1), req("carol", 2)), rows(null, req("carol", 2))));
    }

    @Test
    public void newRequester_isInsertedAtTheEnd() {
        assertEquals(Collections.singletonList("insert 1 1"),
                updates(rows(null, req("bob", 1)), rows(null, req("bob", 1), req("carol", 2))));
    }

    @Test
    public void rowsAreMatchedByPlayer_notByPosition() {
        // bob leaves and dave arrives: carol keeps her view; nothing is rebound.
        List<String> u = updates(rows(null, req("bob", 1), req("carol", 2)),
                rows(null, req("carol", 2), req("dave", 3)));
        assertEquals(u.toString(), 2, u.size());
        assertTrue(u.toString(), u.contains("remove 0 1"));
        for (String s : u) {
            assertFalse(u.toString(), s.startsWith("change") || s.startsWith("move"));
        }
    }
}
