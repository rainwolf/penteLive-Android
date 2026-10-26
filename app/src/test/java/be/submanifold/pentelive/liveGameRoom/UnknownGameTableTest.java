package be.submanifold.pentelive.liveGameRoom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/** Live tables with an unknown (experimental) game id are hidden and their events ignored; known ones are unaffected. */
public class UnknownGameTableTest {

    private static Map<String, Object> state(int table, int game) {
        Map<String, Object> m = new HashMap<>();
        m.put("timed", false);
        m.put("initialMinutes", 0);
        m.put("incrementalSeconds", 0);
        m.put("rated", false);
        m.put("game", game);
        m.put("tableType", 1);
        m.put("table", table);
        return m;
    }

    private static Map<String, Object> sit(int table, int seat, String player) {
        Map<String, Object> m = new HashMap<>();
        m.put("table", table);
        m.put("seat", seat);
        m.put("player", player);
        return m;
    }

    @Test
    public void unknownGameTableIsNotStoredAndItsEventsAreIgnored() {
        TablesAndPlayers tp = new TablesAndPlayers("me");
        assertEquals(40, tp.changeTableState(state(40, 33)));
        assertTrue(tp.isHidden(40));
        assertTrue(tp.tables.isEmpty());

        assertNull(tp.tableJoin(40, "someone"));
        assertEquals(40, tp.tableSit(sit(40, 1, "someone")));
        assertEquals(40, tp.tableStand(sit(40, 1, "someone")));
        tp.updateGameState(40, 2);
        tp.tableOwner(40, "someone");
        tp.undoMove(40);
        assertFalse(tp.tableExit(40, "someone"));
        assertTrue(tp.tables.isEmpty());
    }

    @Test
    public void knownTableChangingToUnknownGameDisappears() {
        TablesAndPlayers tp = new TablesAndPlayers("me");
        tp.changeTableState(state(7, 1));
        assertEquals(1, tp.tables.size());
        assertFalse(tp.isHidden(7));

        tp.changeTableState(state(7, 34));
        assertTrue(tp.tables.isEmpty());
        assertTrue(tp.isHidden(7));

        tp.changeTableState(state(7, 3)); // reused id with a known game is visible again
        assertEquals(1, tp.tables.size());
        assertFalse(tp.isHidden(7));
    }

    @Test
    public void knownTablesBehaveAsBefore() {
        TablesAndPlayers tp = new TablesAndPlayers("me");
        tp.changeTableState(state(1, 31));
        tp.changeTableState(state(2, 82));
        assertEquals(2, tp.tables.size());
        assertEquals("Renju", tp.tables.get(1).getGameName());
        assertTrue(tp.tables.get(1).isKnownGame());
        tp.tableSit(sit(1, 1, "a"));
        assertEquals(1, tp.tables.get(1).getSeats().size());
    }

    @Test
    public void tableWithUnknownGameDoesNotCrash() {
        Table t = new Table();
        t.setGame(33);
        t.setTimed(true);
        assertFalse(t.isKnownGame());
        assertNull(t.getGameName());
        assertFalse(t.shouldTimerRun()); // used to NPE on gameNames.get(game).contains
        t.getGameColor();
    }

    @Test
    public void hiddenTableKeepsMembershipAndRestoresItWhenKnownAgain() {
        TablesAndPlayers tp = new TablesAndPlayers("me");
        tp.players.put("ann", new LivePlayer("ann", false, 0, 0));
        tp.players.put("bob", new LivePlayer("bob", false, 0, 0));
        tp.changeTableState(state(7, 1));
        tp.tableJoin(7, "ann");
        tp.changeTableState(state(7, 33));
        assertTrue(tp.tables.isEmpty());
        assertNull(tp.tableJoin(7, "bob")); // tracked, but never returned for opening
        tp.tableSit(sit(7, 2, "bob"));
        tp.changeTableState(state(7, 1));
        Table t = tp.tables.get(7);
        assertEquals(2, t.getPlayers().size());
        assertEquals("ann", t.getOwner());
        assertEquals("bob", t.getSeats().get(2).getName());
        assertFalse(tp.isHidden(7));
    }

    @Test
    public void joinOfMyselfToHiddenTableOpensNothing() {
        TablesAndPlayers tp = new TablesAndPlayers("me");
        tp.players.put("me", new LivePlayer("me", false, 0, 0));
        tp.changeTableState(state(8, 34));
        assertNull(tp.tableJoin(8, "me"));
        assertTrue(tp.isHidden(8));
        assertTrue(tp.tables.isEmpty());
    }
}
