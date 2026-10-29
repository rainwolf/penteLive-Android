package be.submanifold.pentelive.db;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URLEncoder;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import be.submanifold.pentelive.net.Json;
import be.submanifold.pentelive.net.LoginRejectedException;
import be.submanifold.pentelive.net.NotLoggedInException;

/** Typed /api/db endpoints. Never throws; failures map to {@link DbResult.Kind}. */
public final class DbApi {
    private interface Call {
        DbTransport.Reply run() throws IOException;
    }

    private final DbTransport transport;

    public DbApi(DbTransport transport) {
        this.transport = transport;
    }

    public DbResult<DbDtos.PositionStats> positionStats(int game, List<Integer> moves, String scope, JsonObject filters) {
        String body = DbRequests.positionStats(game, moves, scope, filters);
        DbResult<DbDtos.PositionStats> r = exec(() -> transport.postJson("/position-stats", body), DbDtos.PositionStats.class);
        if (r.kind == DbResult.Kind.OK && r.value.nextMoves == null) r.value.nextMoves = new ArrayList<>();
        return r;
    }

    public DbResult<DbDtos.GamePage> searchGames(int game, List<Integer> moves, String scope, JsonObject filters,
                                                 int offset, int limit) {
        String body = DbRequests.gamesSearch(game, moves, scope, filters, offset, limit);
        DbResult<DbDtos.GamePage> r = exec(() -> transport.postJson("/games/search", body), DbDtos.GamePage.class);
        if (r.kind == DbResult.Kind.OK && r.value.games == null) r.value.games = new ArrayList<>();
        return r;
    }

    /** Server liveness, auth and {@code features}; {@code features} is never null on OK. */
    public DbResult<DbDtos.Ping> ping() {
        DbResult<DbDtos.Ping> r = exec(() -> transport.get("/ping"), DbDtos.Ping.class);
        if (r.kind == DbResult.Kind.OK && r.value.features == null) r.value.features = new ArrayList<>();
        return r;
    }

    public DbResult<DbDtos.GameDetail> game(long gid) {
        return detail("/games/" + gid);
    }

    /** For {@code source:"mine"} rows, whose gid is the collection wgid. */
    public DbResult<DbDtos.GameDetail> collectionGame(long wgid) {
        return detail("/collection/" + wgid);
    }

    public DbResult<List<String>> players(String q) {
        if (q == null || q.trim().length() < 2) return DbResult.ok(new ArrayList<String>());
        String path = "/players?q=" + encode(q.trim());
        DbResult<DbDtos.PlayersReply> r = exec(() -> transport.get(path), DbDtos.PlayersReply.class);
        if (r.kind != DbResult.Kind.OK) return DbResult.of(r.kind);
        return DbResult.ok(r.value.players == null ? new ArrayList<String>() : r.value.players);
    }

    private DbResult<DbDtos.GameDetail> detail(String path) {
        DbResult<DbDtos.GameDetail> r = exec(() -> transport.get(path), DbDtos.GameDetail.class);
        if (r.kind == DbResult.Kind.OK && r.value.moves == null) r.value.moves = new int[0];
        return r;
    }

    private static <T> DbResult<T> exec(Call call, Class<T> type) {
        DbTransport.Reply reply;
        try {
            reply = call.run();
        } catch (LoginRejectedException | NotLoggedInException e) {
            return DbResult.of(DbResult.Kind.UNAUTHORIZED);
        } catch (IOException e) {
            return DbResult.of(DbResult.Kind.FAILED);
        }
        if (reply.code == 401) return DbResult.of(DbResult.Kind.UNAUTHORIZED);
        if (reply.code == 403) {
            String code = errorCode(reply.body);
            if ("subscription_required".equals(code)) return DbResult.of(DbResult.Kind.SUBSCRIPTION_REQUIRED);
            if ("blocked".equals(code)) return DbResult.of(DbResult.Kind.BLOCKED); // same state as blocked:true
            return DbResult.of(DbResult.Kind.FAILED);
        }
        if (reply.code < 200 || reply.code >= 300) return DbResult.of(DbResult.Kind.FAILED);
        try {
            T value = Json.GSON.fromJson(reply.body, type);
            return value == null ? DbResult.<T>of(DbResult.Kind.FAILED) : DbResult.ok(value);
        } catch (RuntimeException e) {
            return DbResult.of(DbResult.Kind.FAILED);
        }
    }

    private static String encode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8"); // the Charset overload needs API 33
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String errorCode(String body) {
        try {
            return JsonParser.parseString(body).getAsJsonObject().getAsJsonObject("error").get("code").getAsString();
        } catch (RuntimeException e) {
            return null;
        }
    }
}
