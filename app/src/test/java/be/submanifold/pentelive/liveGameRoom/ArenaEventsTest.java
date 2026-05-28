package be.submanifold.pentelive.liveGameRoom;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class ArenaEventsTest {

    @Test
    public void createTable_buildsExpectedJson() {
        String json = ArenaEvents.createTable(true, 5, 3, false, 1, 2, "alice");
        assertEquals(
            "{\"dsgArenaCreateTableEvent\":{\"timed\":true,\"initialMinutes\":5,"
            + "\"incrementalSeconds\":3,\"rated\":false,\"game\":1,\"playAs\":2,"
            + "\"player\":\"alice\",\"table\":-1,\"time\":0}}",
            json);
    }

    @Test
    public void requestJoin_includesPlayerKey() {
        assertEquals(
            "{\"dsgArenaRequestJoinTableEvent\":{\"player\":\"bob\",\"table\":42,\"time\":0}}",
            ArenaEvents.requestJoin("bob", 42));
    }

    @Test
    public void accept_buildsExpectedJson() {
        assertEquals(
            "{\"dsgArenaAcceptTableJoinEvent\":{\"player\":\"alice\",\"playerToAccept\":\"bob\",\"table\":42}}",
            ArenaEvents.accept("alice", "bob", 42));
    }

    @Test
    public void reject_usesCapitalDsgAndCorrectedMessageKey() {
        assertEquals(
            "{\"DSGArenaRejectTableJoinEvent\":{\"player\":\"alice\",\"playerToReject\":\"bob\",\"table\":42,\"message\":null}}",
            ArenaEvents.reject("alice", "bob", 42));
    }
}
