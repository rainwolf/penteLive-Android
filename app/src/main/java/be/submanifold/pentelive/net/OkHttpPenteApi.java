package be.submanifold.pentelive.net;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import be.submanifold.pentelive.JsonModels;

import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Synchronous, thread-agnostic OkHttp implementation of {@link PenteApi}.
 *
 * <p>One shared {@link OkHttpClient} carries the {@link Session} cookie jar (so the
 * server session cookie is replayed on every request) and sane connect/read timeouts.
 * Every method calls {@code Call.execute()} on the calling thread — there is no
 * threading here; scheduling lives in Task 5's {@code PenteApiClient}. Default TLS
 * uses the platform {@code SSLSocketFactory}, which honors
 * {@code res/xml/network_security_configuration.xml} (the ISRG roots) automatically,
 * so no custom factory is wired.
 *
 * <h3>Auth-expiry signal &amp; re-auth mechanism</h3>
 * Requests carry no credentials: authentication rides on the shared cookie store
 * ({@link SharedCookies}). The server signals "not logged in" at the <em>application
 * level</em>, NOT with an HTTP 401: the data JSPs redirect to
 * {@code /gameServer/mobile/index.jsp} (OkHttp follows it) and other pages answer 200
 * with a login form or error text ({@link LoggedOutDetector}); a 200 body that parses to
 * no entity is treated the same way. Because an {@link okhttp3.Authenticator} only fires
 * on HTTP 401/403, it could never observe these signals; therefore the single-flight
 * re-auth lives inside {@link #withReauth} here, triggered when an attempt yields
 * {@link Result.Reason#AUTH_EXPIRED} (a logged-out answer, or a literal 401/403).
 *
 * <h3>Single-flight re-login</h3>
 * On the expiry signal, {@link #reauth} re-logs in <em>exactly once</em> even under N
 * concurrent expiries: it is {@code synchronized} on a private lock and guarded by a
 * generation counter, so the first thread performs the network login and bumps the
 * generation while the others observe the bump and skip straight to the retry. The
 * re-login POSTs {@code name2}/{@code password2} from the {@link Session} in the form
 * body of login.jsp, never in the URL; the server answers with fresh session and login
 * cookies, which the retried <em>same</em> {@link Request} then carries. Retry is capped
 * at one attempt to avoid loops: an expiry that survives re-auth maps to
 * {@link Result.Reason#AUTH_EXPIRED}.
 */
public final class OkHttpPenteApi implements PenteApi {

    private static final long CONNECT_TIMEOUT_SECONDS = 15;
    private static final long READ_TIMEOUT_SECONDS = 30;
    private static final long WRITE_TIMEOUT_SECONDS = 30;

    private final Session session;
    private final BaseUrlProvider urls;
    private final OkHttpClient client;

    /** Single-flight re-auth state, guarded by {@link #authLock}. */
    private final Object authLock = new Object();
    /**
     * Monotone counter bumped inside {@code synchronized(authLock)} on each successful re-login.
     * Declared {@code volatile} so {@link #currentGeneration()} can read it without acquiring the
     * lock — avoiding serialization of all concurrent callers behind a {@code performLogin()} call.
     */
    private volatile int authGeneration = 0;

    public OkHttpPenteApi(Session session, BaseUrlProvider urls) {
        this.session = session;
        this.urls = urls;
        this.client = new OkHttpClient.Builder()
                .cookieJar(session.cookieJar())
                .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();
    }

    private interface Parser<T> {
        T parse(String body) throws Exception;
    }

    private HttpUrl.Builder base() {
        return HttpUrl.get(urls.baseUrl()).newBuilder();
    }

    private static Request get(HttpUrl url) {
        return new Request.Builder().url(url).get().build();
    }

    /** POSTs the credentials in the form body; the URL carries no query. */
    private Request loginRequest(String name, String password) {
        HttpUrl url = base().addPathSegments("gameServer/login.jsp").build();
        FormBody form = new FormBody.Builder(StandardCharsets.UTF_8)
                .add("mobile", "")
                .add("name2", name)
                .add("password2", password)
                .build();
        return new Request.Builder().url(url).post(form).build();
    }

    /** OkHttp follows redirects, so the final request's path is the redirect target. */
    private static boolean isLoggedOut(Request request, Response response, String body) {
        return LoggedOutDetector.isLoggedOut(request.url().encodedPath(),
                response.request().url().encodedPath(), body);
    }

    // -------------------------------------------------------------------------
    // PenteApi
    // -------------------------------------------------------------------------

    @Override
    public Result<WhosOnline> whosOnline() {
        HttpUrl url = base()
                .addPathSegments("gameServer/mobile/json/whosonlineandlive.jsp")
                .build();
        Request request = get(url);
        return withReauth(() -> attemptJson(request, body -> {
            Type listType = new TypeToken<List<JsonModels.RoomEntry>>() {}.getType();
            List<JsonModels.RoomEntry> rooms = Json.GSON.fromJson(body, listType);
            // Logged-out sentinel: server returned a 200 with no parseable list.
            if (rooms == null) {
                return null;
            }
            return new WhosOnline(rooms);
        }));
    }

    @Override
    public Result<Bitmap> avatar(String name) {
        HttpUrl url = base()
                .addPathSegments("gameServer/avatar")
                .addQueryParameter("name", name)
                .build();
        Request request = get(url);
        return withReauth(() -> attemptBitmap(request));
    }

    @Override
    public Result<Boolean> login(String name, String password) {
        // login IS the auth action, so it never re-auths/retries.
        return doLoginRequest(name, password);
    }

    @Override
    public Result<JsonModels.GameResponse> loadGame(String gid) {
        HttpUrl url = base()
                .addPathSegments("gameServer/mobile/json/game.jsp")
                .addQueryParameter("gid", gid)
                .build();
        Request request = get(url);
        return withReauth(() -> attemptJson(request, body -> {
            JsonModels.GameResponse game = Json.GSON.fromJson(body, JsonModels.GameResponse.class);
            // Logged-out sentinel: a 200 whose body has no game (legacy gameName == null).
            if (game == null || game.gameName == null) {
                return null;
            }
            return game;
        }));
    }

    @Override
    public Result<Void> submitMove(String gid, String moves, String message) {
        return submitMove(gid, moves, message, null);
    }

    public Result<Void> submitMove(String gid, String moves, String message, String renjuAction) {
        HttpUrl.Builder b = base()
                .addPathSegments("gameServer/tb/game")
                .addQueryParameter("command", "move")
                .addQueryParameter("mobile", "")
                .addQueryParameter("gid", gid)
                .addQueryParameter("moves", moves)
                .addQueryParameter("message", message);
        if (renjuAction != null && !renjuAction.isEmpty()) {
            b.addQueryParameter("renjuAction", renjuAction);
        }
        Request request = get(b.build());
        return withReauth(() -> attemptVoid(request));
    }

    // -------------------------------------------------------------------------
    // Single-flight re-auth
    // -------------------------------------------------------------------------

    /**
     * Runs {@code attempt}; if it signals {@link Result.Reason#AUTH_EXPIRED}, performs
     * a single-flight re-login and retries the attempt exactly once. Any other outcome
     * (ok / NETWORK / SERVER / PARSE) is returned immediately.
     */
    private <T> Result<T> withReauth(Supplier<Result<T>> attempt) {
        int generationBeforeAttempt = currentGeneration();
        Result<T> first = attempt.get();
        if (first.isOk() || first.failure.reason != Result.Reason.AUTH_EXPIRED) {
            return first;
        }
        boolean reauthSucceeded;
        try {
            reauthSucceeded = reauth(generationBeforeAttempt);
        } catch (IOException e) {
            // Network failure during re-login: surface as NETWORK rather than AUTH_EXPIRED so
            // callers can distinguish a connectivity problem from a genuine session expiry.
            return Result.fail(new Result.Failure(Result.Reason.NETWORK, 0, e));
        }
        if (!reauthSucceeded) {
            // Wrong credentials on re-login: the session cannot be refreshed.
            return Result.fail(new Result.Failure(Result.Reason.AUTH_EXPIRED, first.failure.httpCode, null));
        }
        // Retry the ORIGINAL request once; it now carries the fresh login cookies.
        // Whatever it yields now is terminal (an AUTH_EXPIRED here will not loop).
        return attempt.get();
    }

    /** Volatile read — no lock needed; write side is protected by {@link #authLock}. */
    private int currentGeneration() {
        return authGeneration;
    }

    /**
     * Re-login at most once per generation. If another thread already re-authed since
     * {@code seenGeneration} was read, this is a no-op success (reuse its fresh session).
     *
     * @throws IOException if the network call to login.jsp fails; caller maps this to NETWORK.
     */
    private boolean reauth(int seenGeneration) throws IOException {
        synchronized (authLock) {
            if (authGeneration != seenGeneration) {
                // Another concurrent expiry already refreshed the session — reuse it.
                return true;
            }
            boolean loggedIn = performLogin(session.name(), session.password());
            if (loggedIn) {
                authGeneration++;
            }
            return loggedIn;
        }
    }

    /**
     * Re-login: POSTs name2/password2 in the form body of login.jsp.
     *
     * @return {@code true} only if the response is 2xx AND the page confirms the login
     *         ({@link LoginResponse#classify}; login.jsp returns 200 even for bad creds).
     * @throws IOException on network failure; propagates to {@link #reauth} so
     *         {@link #withReauth} can surface it as {@link Result.Reason#NETWORK}.
     */
    private boolean performLogin(String name, String password) throws IOException {
        try (Response response = client.newCall(loginRequest(name, password)).execute()) {
            if (!response.isSuccessful()) {
                return false;
            }
            return LoginResponse.classify(bodyString(response)) == LoginResponse.Outcome.SUCCESS;
        }
        // IOException propagates — caller (reauth) distinguishes network failure from wrong creds.
    }

    // -------------------------------------------------------------------------
    // Per-attempt exchange helpers (map one round trip to a Result)
    // -------------------------------------------------------------------------

    private <T> Result<T> attemptJson(Request request, Parser<T> parser) {
        try (Response response = client.newCall(request).execute()) {
            int code = response.code();
            Result<T> authOrServer = classifyNon2xx(code);
            if (authOrServer != null) {
                return authOrServer;
            }
            String text = bodyString(response);
            if (isLoggedOut(request, response, text)) {
                return Result.fail(new Result.Failure(Result.Reason.AUTH_EXPIRED, code, null));
            }
            try {
                T value = parser.parse(text);
                if (value == null) {
                    // App-level logged-out sentinel on a 200 (legacy json.player == null).
                    return Result.fail(new Result.Failure(Result.Reason.AUTH_EXPIRED, code, null));
                }
                return Result.ok(value);
            } catch (Exception e) {
                return Result.fail(new Result.Failure(Result.Reason.PARSE, code, e));
            }
        } catch (IOException e) {
            return Result.fail(new Result.Failure(Result.Reason.NETWORK, 0, e));
        }
    }

    private Result<Bitmap> attemptBitmap(Request request) {
        try (Response response = client.newCall(request).execute()) {
            int code = response.code();
            Result<Bitmap> authOrServer = classifyNon2xx(code);
            if (authOrServer != null) {
                return authOrServer;
            }
            ResponseBody body = response.body();
            if (body == null) {
                return Result.fail(new Result.Failure(Result.Reason.PARSE, code, null));
            }
            InputStream in = body.byteStream();
            Bitmap bitmap = BitmapFactory.decodeStream(in);
            if (bitmap == null) {
                return Result.fail(new Result.Failure(Result.Reason.PARSE, code, null));
            }
            return Result.ok(bitmap);
        } catch (IOException e) {
            return Result.fail(new Result.Failure(Result.Reason.NETWORK, 0, e));
        }
    }

    private Result<Void> attemptVoid(Request request) {
        try (Response response = client.newCall(request).execute()) {
            int code = response.code();
            Result<Void> authOrServer = classifyNon2xx(code);
            if (authOrServer != null) {
                return authOrServer;
            }
            if (isLoggedOut(request, response, bodyString(response))) {
                return Result.fail(new Result.Failure(Result.Reason.AUTH_EXPIRED, code, null));
            }
            return Result.ok(null);
        } catch (IOException e) {
            return Result.fail(new Result.Failure(Result.Reason.NETWORK, 0, e));
        }
    }

    private Result<Boolean> doLoginRequest(String name, String password) {
        try (Response response = client.newCall(loginRequest(name, password)).execute()) {
            int code = response.code();
            if (code == 401 || code == 403) {
                return Result.fail(new Result.Failure(Result.Reason.INVALID_CREDENTIALS, code, null));
            }
            if (!is2xx(code)) {
                return Result.fail(new Result.Failure(Result.Reason.SERVER, code, null));
            }
            // login.jsp returns 200 even for bad credentials; the page says which it was.
            LoginResponse.Outcome outcome = LoginResponse.classify(bodyString(response));
            if (outcome == LoginResponse.Outcome.INVALID_CREDENTIALS) {
                return Result.fail(new Result.Failure(Result.Reason.INVALID_CREDENTIALS, code, null));
            }
            if (outcome == LoginResponse.Outcome.UNEXPECTED) {
                // Neither marker: the page did not confirm the login.
                return Result.fail(new Result.Failure(Result.Reason.PARSE, code, null));
            }
            return Result.ok(Boolean.TRUE);
        } catch (IOException e) {
            return Result.fail(new Result.Failure(Result.Reason.NETWORK, 0, e));
        }
    }

    /**
     * @return a terminal {@code Result} for an auth (401/403 -> AUTH_EXPIRED) or
     * server (other non-2xx -> SERVER) status, or {@code null} when the code is 2xx
     * and the caller should parse the body.
     */
    private static <T> Result<T> classifyNon2xx(int code) {
        if (code == 401 || code == 403) {
            return Result.fail(new Result.Failure(Result.Reason.AUTH_EXPIRED, code, null));
        }
        if (!is2xx(code)) {
            return Result.fail(new Result.Failure(Result.Reason.SERVER, code, null));
        }
        return null;
    }

    private static boolean is2xx(int code) {
        return code >= 200 && code < 300;
    }

    private static String bodyString(Response response) throws IOException {
        ResponseBody body = response.body();
        return body == null ? "" : body.string();
    }
}
