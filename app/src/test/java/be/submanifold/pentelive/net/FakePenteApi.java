package be.submanifold.pentelive.net;

import android.graphics.Bitmap;

import be.submanifold.pentelive.JsonModels;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory {@link PenteApi} for unit tests. Records every call (with arguments) into
 * {@link #calls}, returns canned Results from the per-method fields/maps, and can force the
 * NEXT call to fail by setting {@link #nextFailure} (single-shot: consumed on use).
 */
public final class FakePenteApi implements PenteApi {

    /**
     * Ordered log of calls, e.g. "whosOnline", "avatar:bob", "login:alice",
     * "loadGame:42", "submitMove:42|j10|good luck".
     */
    public final List<String> calls = new ArrayList<>();

    /** Canned responses. Map keys: avatar/login by name, game by gid. */
    public WhosOnline whosOnlineResponse;
    public final Map<String, Bitmap> avatars = new HashMap<>();
    public final Map<String, Boolean> logins = new HashMap<>();
    public final Map<String, JsonModels.GameResponse> games = new HashMap<>();

    /** When non-null, the next call returns {@code fail(reason)} and this field is reset to null. */
    public Result.Reason nextFailure;

    @Override
    public Result<WhosOnline> whosOnline() {
        calls.add("whosOnline");
        if (nextFailure != null) {
            return consumeFailure();
        }
        return Result.ok(whosOnlineResponse);
    }

    @Override
    public Result<Bitmap> avatar(String name) {
        calls.add("avatar:" + name);
        if (nextFailure != null) {
            return consumeFailure();
        }
        return Result.ok(avatars.get(name));
    }

    @Override
    public Result<Boolean> login(String name, String password) {
        calls.add("login:" + name);
        if (nextFailure != null) {
            return consumeFailure();
        }
        Boolean canned = logins.get(name);
        return Result.ok(canned != null ? canned : Boolean.TRUE);
    }

    @Override
    public Result<JsonModels.GameResponse> loadGame(String gid) {
        calls.add("loadGame:" + gid);
        if (nextFailure != null) {
            return consumeFailure();
        }
        return Result.ok(games.get(gid));
    }

    @Override
    public Result<Void> submitMove(String gid, String moves, String message) {
        calls.add("submitMove:" + gid + "|" + moves + "|" + message);
        if (nextFailure != null) {
            return consumeFailure();
        }
        return Result.<Void>ok(null);
    }

    private <T> Result<T> consumeFailure() {
        Result.Reason reason = nextFailure;
        nextFailure = null;
        return Result.fail(new Result.Failure(reason, 0, null));
    }
}
