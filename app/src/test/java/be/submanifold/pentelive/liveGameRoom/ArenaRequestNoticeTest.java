package be.submanifold.pentelive.liveGameRoom;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/** Spec C4: one short notice per dsgArenaRequestEndedEvent reason. */
public class ArenaRequestNoticeTest {

    private static ArenaRequestEnded ended(String reason) {
        return new ArenaRequestEnded(7, "bob", "alice", reason);
    }

    @Test
    public void declined_namesTheOwner() {
        ArenaRequestNotice n = ArenaRequestNotice.of(ended("DECLINED"));
        assertEquals(ArenaRequestNotice.Kind.DECLINED, n.kind);
        assertArrayEquals(new Object[]{"alice"}, n.args);
    }

    @Test
    public void duplicate_namesTheOwnerFromTheEvent() {
        ArenaRequestNotice n = ArenaRequestNotice.of(ended("DUPLICATE"));
        assertEquals(ArenaRequestNotice.Kind.DUPLICATE, n.kind);
        assertArrayEquals(new Object[]{"alice"}, n.args);
    }

    @Test
    public void booted_namesTheOwnerAndTheTable() {
        ArenaRequestNotice n = ArenaRequestNotice.of(ended("BOOTED"));
        assertEquals(ArenaRequestNotice.Kind.BOOTED, n.kind);
        assertArrayEquals(new Object[]{"alice", 7}, n.args);
    }

    @Test
    public void tableReasons_nameTheTable() {
        String[] reasons = {"TABLE_FULL", "TABLE_CLOSED", "BLOCKED", "NOT_AVAILABLE"};
        ArenaRequestNotice.Kind[] kinds = {
                ArenaRequestNotice.Kind.TABLE_FULL, ArenaRequestNotice.Kind.TABLE_CLOSED,
                ArenaRequestNotice.Kind.BLOCKED, ArenaRequestNotice.Kind.NOT_AVAILABLE};
        for (int i = 0; i < reasons.length; i++) {
            ArenaRequestNotice n = ArenaRequestNotice.of(ended(reasons[i]));
            assertEquals(reasons[i], kinds[i], n.kind);
            assertArrayEquals(reasons[i], new Object[]{7}, n.args);
        }
    }

    @Test
    public void guestRated_hasNoArguments() {
        ArenaRequestNotice n = ArenaRequestNotice.of(ended("GUEST_RATED"));
        assertEquals(ArenaRequestNotice.Kind.GUEST_RATED, n.kind);
        assertEquals(0, n.args.length);
    }

    @Test
    public void noLongerAvailable_namesTheRequester() {
        ArenaRequestNotice n = ArenaRequestNotice.of(ended("NO_LONGER_AVAILABLE"));
        assertEquals(ArenaRequestNotice.Kind.NO_LONGER_AVAILABLE, n.kind);
        assertArrayEquals(new Object[]{"bob"}, n.args);
    }

    @Test
    public void unknownOrMissingReason_givesNoNotice() {
        assertNull(ArenaRequestNotice.of(ended("SOMETHING_NEWER")));
        assertNull(ArenaRequestNotice.of(ended(null)));
        assertNull(ArenaRequestNotice.of(null));
    }

    @Test
    public void everyKindHasAString() {
        for (ArenaRequestNotice.Kind k : ArenaRequestNotice.Kind.values()) {
            assertNotEquals(k.name(), 0, k.resId);
        }
    }
}
