package be.submanifold.pentelive.db;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** A real {@link DbApi} over a recording transport whose replies are keyed by the request's moves. */
final class FakeDbApi implements DbTransport {
    final List<List<Integer>> statsCalls = new ArrayList<>();
    final List<Integer> statsGames = new ArrayList<>();
    final Map<List<Integer>, Reply> statsReplies = new HashMap<>();
    Reply defaultReply = new Reply(200, "{\"totalGames\":0,\"nextMoves\":[]}");
    final List<List<Integer>> gamesCalls = new ArrayList<>();
    final List<Integer> gamesOffsets = new ArrayList<>();
    final Map<String, Reply> gamesReplies = new HashMap<>();
    Reply defaultGamesReply = new Reply(200, "{\"total\":0,\"games\":[]}");
    final DbApi api = new DbApi(this);

    void gamesReply(List<Integer> moves, int offset, int code, String body) {
        gamesReplies.put(moves + "@" + offset, new Reply(code, body));
    }

    int lastGamesOffset() {
        return gamesOffsets.get(gamesOffsets.size() - 1);
    }

    /** A games/search page body with {@code count} rows starting at gid {@code firstGid}. */
    static String gamesPage(int total, int firstGid, int count) {
        StringBuilder sb = new StringBuilder("{\"total\":").append(total).append(",\"blocked\":false,\"games\":[");
        for (int i = 0; i < count; i++) {
            if (i > 0) sb.append(',');
            sb.append("{\"gid\":").append(firstGid + i).append(",\"source\":\"archive\",\"game\":1}");
        }
        return sb.append("]}").toString();
    }

    void reply(List<Integer> moves, int code, String body) {
        statsReplies.put(new ArrayList<>(moves), new Reply(code, body));
    }

    List<Integer> lastStatsMoves() {
        return statsCalls.get(statsCalls.size() - 1);
    }

    static String stats(int... moveWinPct) {
        StringBuilder sb = new StringBuilder("{\"totalGames\":100,\"totalWinPct\":50,\"blocked\":false,\"nextMoves\":[");
        for (int i = 0; i < moveWinPct.length; i += 2) {
            if (i > 0) sb.append(',');
            sb.append("{\"move\":").append(moveWinPct[i]).append(",\"games\":10,\"wins\":")
                    .append(moveWinPct[i + 1] / 10).append(",\"winPct\":").append(moveWinPct[i + 1]).append('}');
        }
        return sb.append("]}").toString();
    }

    @Override
    public Reply get(String path) throws IOException {
        throw new IOException("unexpected GET " + path);
    }

    @Override
    public Reply postJson(String path, String json) throws IOException {
        boolean games = "/games/search".equals(path);
        if (!games && !"/position-stats".equals(path)) throw new IOException("unexpected POST " + path);
        JsonObject o = JsonParser.parseString(json).getAsJsonObject();
        List<Integer> moves = new ArrayList<>();
        JsonArray a = o.getAsJsonArray("moves");
        for (JsonElement e : a) moves.add(e.getAsInt());
        if (games) {
            int offset = o.get("offset").getAsInt();
            synchronized (this) {
                gamesCalls.add(moves);
                gamesOffsets.add(offset);
            }
            Reply g = gamesReplies.get(moves + "@" + offset);
            return g != null ? g : defaultGamesReply;
        }
        synchronized (this) {
            statsCalls.add(moves);
            statsGames.add(o.get("game").getAsInt());
        }
        Reply r = statsReplies.get(moves);
        return r != null ? r : defaultReply;
    }
}
