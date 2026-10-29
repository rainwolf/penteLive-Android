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
    final DbApi api = new DbApi(this);

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
        if (!"/position-stats".equals(path)) throw new IOException("unexpected POST " + path);
        JsonObject o = JsonParser.parseString(json).getAsJsonObject();
        List<Integer> moves = new ArrayList<>();
        JsonArray a = o.getAsJsonArray("moves");
        for (JsonElement e : a) moves.add(e.getAsInt());
        synchronized (this) {
            statsCalls.add(moves);
            statsGames.add(o.get("game").getAsInt());
        }
        Reply r = statsReplies.get(moves);
        return r != null ? r : defaultReply;
    }
}
