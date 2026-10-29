package be.submanifold.pentelive.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import be.submanifold.pentelive.net.LoginRejectedException;
import be.submanifold.pentelive.net.NotLoggedInException;

public class DbApiTest {
    static final class Fake implements DbTransport {
        final List<String> calls = new ArrayList<>();
        Reply reply = new Reply(200, "{}");
        IOException toThrow;

        Fake(int code, String body) {
            reply = new Reply(code, body);
        }

        @Override
        public Reply get(String path) throws IOException {
            calls.add("GET " + path);
            if (toThrow != null) throw toThrow;
            return reply;
        }

        @Override
        public Reply postJson(String path, String json) throws IOException {
            calls.add("POST " + path);
            if (toThrow != null) throw toThrow;
            return reply;
        }
    }

    private static DbApi api(Fake f) {
        return new DbApi(f);
    }

    private static DbResult<DbDtos.PositionStats> stats(Fake f) {
        return api(f).positionStats(1, Arrays.asList(180), "archive", null);
    }

    @Test
    public void okParses() {
        Fake f = new Fake(200, "{\"totalGames\":3,\"nextMoves\":[{\"move\":181,\"games\":3,\"wins\":2,\"winPct\":66.7}],\"rotation\":2,\"x\":1}");
        DbResult<DbDtos.PositionStats> r = stats(f);
        assertEquals(DbResult.Kind.OK, r.kind);
        assertEquals(3, r.value.totalGames);
        assertEquals(1, r.value.nextMoves.size());
        assertEquals(181, r.value.nextMoves.get(0).move);
        assertEquals("POST /position-stats", f.calls.get(0));
    }

    @Test
    public void nullNextMovesBecomeEmpty() {
        DbResult<DbDtos.PositionStats> r = stats(new Fake(200, "{\"totalGames\":0}"));
        assertEquals(DbResult.Kind.OK, r.kind);
        assertNotNull(r.value.nextMoves);
        assertTrue(r.value.nextMoves.isEmpty());
    }

    @Test
    public void missingNameColorAndCrownDefaultToZero() {
        Fake f = new Fake(200, "{\"total\":1,\"games\":[{\"gid\":5,\"player1\":{\"name\":\"a\",\"rating\":1500}}]}");
        DbResult<DbDtos.GamePage> r = api(f).searchGames(1, null, "archive", null, 0, 25);
        DbDtos.Player p = r.value.games.get(0).player1;
        assertEquals(0, p.nameColor);
        assertEquals(0, p.crown);
        assertEquals(1500, p.rating);
    }

    @Test
    public void nullPlayerStaysNull() {
        Fake f = new Fake(200, "{\"total\":1,\"games\":[{\"gid\":5,\"player1\":null}]}");
        DbDtos.GameHeader h = api(f).searchGames(1, null, "archive", null, 0, 25).value.games.get(0);
        assertNull(h.player1);
        assertNull(h.player2);
        assertNull(h.playDate);
    }

    @Test
    public void gidAboveIntRangeParsesAsLong() {
        Fake f = new Fake(200, "{\"total\":1,\"games\":[{\"gid\":50000000000123}]}");
        assertEquals(50000000000123L, api(f).searchGames(1, null, "archive", null, 0, 25).value.games.get(0).gid);
    }

    @Test
    public void blockedFlagParsed() {
        DbResult<DbDtos.PositionStats> r = stats(new Fake(200, "{\"totalGames\":0,\"blocked\":true}"));
        assertTrue(r.value.blocked);
        assertFalse(stats(new Fake(200, "{\"totalGames\":0}")).value.blocked);
    }

    @Test
    public void mineSourceParsed() {
        Fake f = new Fake(200, "{\"header\":{\"gid\":9,\"source\":\"mine\"},\"moves\":[180,181]}");
        DbResult<DbDtos.GameDetail> r = api(f).game(9);
        assertTrue(r.value.header.isMine());
        assertEquals(2, r.value.moves.length);
        Fake g = new Fake(200, "{\"header\":{\"gid\":9,\"source\":\"archive\"}}");
        assertFalse(api(g).game(9).value.header.isMine());
        assertEquals(0, api(g).game(9).value.moves.length);
    }

    @Test
    public void gameUsesGamesPath() {
        Fake f = new Fake(200, "{\"header\":{\"gid\":9}}");
        api(f).game(9);
        assertEquals("GET /games/9", f.calls.get(0));
    }

    @Test
    public void collectionGameUsesCollectionPath() {
        Fake f = new Fake(200, "{\"header\":{\"gid\":42}}");
        api(f).collectionGame(42);
        assertEquals("GET /collection/42", f.calls.get(0));
    }

    @Test
    public void unauthorized() {
        assertEquals(DbResult.Kind.UNAUTHORIZED, stats(new Fake(401, "{\"error\":{\"code\":\"x\",\"message\":\"m\"}}")).kind);
    }

    @Test
    public void loginRejectedIsUnauthorized() {
        Fake f = new Fake(200, "{}");
        f.toThrow = new LoginRejectedException("no");
        assertEquals(DbResult.Kind.UNAUTHORIZED, stats(f).kind);
    }

    @Test
    public void notLoggedInIsUnauthorized() {
        Fake f = new Fake(200, "{}");
        f.toThrow = new NotLoggedInException("no");
        assertEquals(DbResult.Kind.UNAUTHORIZED, stats(f).kind);
    }

    @Test
    public void subscriptionRequired() {
        Fake f = new Fake(403, "{\"error\":{\"code\":\"subscription_required\",\"message\":\"m\"}}");
        assertEquals(DbResult.Kind.SUBSCRIPTION_REQUIRED, stats(f).kind);
    }

    @Test
    public void blocked403IsBlocked() {
        String body = "{\"error\":{\"code\":\"blocked\",\"message\":\"m\"}}";
        assertEquals(DbResult.Kind.BLOCKED, api(new Fake(403, body)).game(9).kind);
        assertEquals(DbResult.Kind.BLOCKED, api(new Fake(403, body)).collectionGame(9).kind);
        assertEquals(DbResult.Kind.BLOCKED, stats(new Fake(403, body)).kind);
    }

    @Test
    public void other403AndServerErrorFail() {
        assertEquals(DbResult.Kind.FAILED, stats(new Fake(403, "{\"error\":{\"code\":\"other\"}}")).kind);
        assertEquals(DbResult.Kind.FAILED, stats(new Fake(403, "not json")).kind);
        assertEquals(DbResult.Kind.FAILED, stats(new Fake(500, "{\"error\":{\"code\":\"boom\"}}")).kind);
        assertEquals(DbResult.Kind.FAILED, stats(new Fake(200, "{not json")).kind);
        assertEquals(DbResult.Kind.FAILED, stats(new Fake(200, "")).kind);
        Fake f = new Fake(200, "{}");
        f.toThrow = new IOException("net");
        assertEquals(DbResult.Kind.FAILED, stats(f).kind);
    }

    @Test
    public void playersParse() {
        Fake f = new Fake(200, "{\"players\":[\"ann\",\"anna\"]}");
        DbResult<List<String>> r = api(f).players("an");
        assertEquals(Arrays.asList("ann", "anna"), r.value);
        assertTrue(f.calls.get(0).startsWith("GET /players"));
    }

    @Test
    public void shortPlayerQueryNoCall() {
        Fake f = new Fake(200, "{}");
        DbResult<List<String>> r = api(f).players(" a ");
        assertEquals(DbResult.Kind.OK, r.kind);
        assertTrue(r.value.isEmpty());
        assertEquals(0, f.calls.size());
    }
}
