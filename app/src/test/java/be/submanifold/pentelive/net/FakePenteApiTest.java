package be.submanifold.pentelive.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import be.submanifold.pentelive.JsonModels;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FakePenteApiTest {

    @Test
    public void recordsCallsInOrderWithArguments() {
        FakePenteApi api = new FakePenteApi();

        api.whosOnline();
        api.avatar("bob");
        api.login("alice", "secret");
        api.loadGame("42");
        api.submitMove("42", "j10", "good luck");

        assertEquals(
                Arrays.asList(
                        "whosOnline",
                        "avatar:bob",
                        "login:alice",
                        "loadGame:42",
                        "submitMove:42|j10|good luck"),
                api.calls);
    }

    @Test
    public void returnsCannedWhosOnline() {
        FakePenteApi api = new FakePenteApi();
        List<JsonModels.RoomEntry> rooms = new ArrayList<>();
        JsonModels.RoomEntry room = new JsonModels.RoomEntry();
        room.name = "Main";
        rooms.add(room);
        WhosOnline canned = new WhosOnline(rooms);
        api.whosOnlineResponse = canned;

        Result<WhosOnline> result = api.whosOnline();

        assertTrue(result.isOk());
        assertSame(canned, result.value);
        assertEquals(1, result.value.rooms.size());
        assertEquals("Main", result.value.rooms.get(0).name);
    }

    @Test
    public void whosOnlineNeverReturnsNullRooms() {
        FakePenteApi api = new FakePenteApi();
        api.whosOnlineResponse = new WhosOnline(null);

        Result<WhosOnline> result = api.whosOnline();

        assertTrue(result.isOk());
        assertTrue(result.value.rooms.isEmpty());
    }

    @Test
    public void returnsCannedGameKeyedByGid() {
        FakePenteApi api = new FakePenteApi();
        JsonModels.GameResponse game = new JsonModels.GameResponse();
        game.gid = "42";
        game.moves = "j10k11";
        api.games.put("42", game);

        Result<JsonModels.GameResponse> result = api.loadGame("42");

        assertTrue(result.isOk());
        assertSame(game, result.value);
        assertEquals("j10k11", result.value.moves);
    }

    @Test
    public void loginDefaultsToTrueWhenNoCannedValue() {
        FakePenteApi api = new FakePenteApi();

        Result<Boolean> result = api.login("alice", "secret");

        assertTrue(result.isOk());
        assertEquals(Boolean.TRUE, result.value);
    }

    @Test
    public void loginReturnsCannedFalse() {
        FakePenteApi api = new FakePenteApi();
        api.logins.put("alice", false);

        Result<Boolean> result = api.login("alice", "wrong");

        assertTrue(result.isOk());
        assertEquals(Boolean.FALSE, result.value);
    }

    @Test
    public void submitMoveReturnsOkVoidAndRecordsArgs() {
        FakePenteApi api = new FakePenteApi();

        Result<Void> result = api.submitMove("7", "m13", "");

        assertTrue(result.isOk());
        assertNull(result.value);
        assertEquals(Arrays.asList("submitMove:7|m13|"), api.calls);
    }

    @Test
    public void nextFailureForcesSingleFailureThenRecovers() {
        FakePenteApi api = new FakePenteApi();
        api.nextFailure = Result.Reason.NETWORK;

        Result<WhosOnline> failed = api.whosOnline();
        assertFalse(failed.isOk());
        assertEquals(Result.Reason.NETWORK, failed.failure.reason);
        assertNull(failed.value);

        // nextFailure is single-shot: the following call succeeds.
        Result<WhosOnline> ok = api.whosOnline();
        assertTrue(ok.isOk());

        assertEquals(Arrays.asList("whosOnline", "whosOnline"), api.calls);
    }
}
