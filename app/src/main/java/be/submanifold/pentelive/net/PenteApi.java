package be.submanifold.pentelive.net;

import android.graphics.Bitmap;

import be.submanifold.pentelive.JsonModels;

/**
 * Synchronous, thread-agnostic pente.org HTTP API. Implementations MUST be safe to call
 * from a background thread and MUST NOT touch the UI thread or an Android Looper. Every
 * method returns a {@link Result} that is either {@code ok(value)} or {@code fail(Failure)};
 * methods never throw for expected network/auth/parse conditions.
 *
 * <p>First strangle slice: whos-online, avatar, login, single turn-based game load, move submit.
 */
public interface PenteApi {

    /** GET mobile/json/whosonlineandlive.jsp. */
    Result<WhosOnline> whosOnline();

    /** GET the avatar image for {@code name}; PARSE failure if the bytes are not a decodable image. */
    Result<Bitmap> avatar(String name);

    /** Authenticate; {@code ok(true)} on success, {@code fail(INVALID_CREDENTIALS)} on bad credentials. */
    Result<Boolean> login(String name, String password);

    /** GET mobile/json/game.jsp?gid=... for one turn-based game. */
    Result<JsonModels.GameResponse> loadGame(String gid);

    /** POST a move (and optional chat {@code message}) for {@code gid}. */
    Result<Void> submitMove(String gid, String moves, String message);
}
