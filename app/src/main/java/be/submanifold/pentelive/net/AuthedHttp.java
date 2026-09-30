package be.submanifold.pentelive.net;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import be.submanifold.pentelive.MyApplication;
import be.submanifold.pentelive.PentePlayer;
import be.submanifold.pentelive.RedactingLog;

/**
 * Authenticated HttpURLConnection requests that carry no credentials in the URL.
 *
 * <p>Authentication rides on the shared cookie store ({@link SharedCookies}): the session
 * cookie, and the name2/password2 cookies from which the server's LoginFilter re-creates an
 * expired session by itself. The password leaves the device only in the body of the login
 * POST.
 *
 * <p>Each request first makes sure the store holds the login cookies, and logs in if not.
 * If the server still answers "not logged in" ({@link LoggedOutDetector}), it logs in once
 * more (single-flight across threads) and retries the request once. Failures are logged and
 * thrown as IOExceptions, so call sites keep their existing IOException handling:
 * {@link LoginRejectedException} when the stored credentials no longer log in, and
 * {@link NotLoggedInException} when the retry is still logged out.
 */
public final class AuthedHttp {

    private static final String TAG = "AuthedHttp";
    static final int LOGIN_TIMEOUT_MS = 60_000;
    private static final String LOGIN_PATH = "/gameServer/login.jsp";

    /** The final response of one exchange. */
    public static final class Reply {
        /** Status code of the final response. */
        public final int code;
        /** URL path of the final response; GETs follow redirects, POSTs do not. */
        public final String finalPath;
        /** The whole body, decoded as UTF-8. */
        public final String body;

        Reply(int code, String finalPath, String body) {
            this.code = code;
            this.finalPath = finalPath;
            this.body = body;
        }
    }

    /** Builds a form body; evaluated again for the retry after a re-login. */
    public interface FormSupplier {
        String form() throws IOException;
    }

    private interface Exchange {
        Reply run() throws IOException;
    }

    private static AuthedHttp shared;

    public static synchronized AuthedHttp shared() {
        if (shared == null) {
            shared = new AuthedHttp(new StaticBaseUrlProvider(PentePlayer.development),
                    new SharedPrefsSession(MyApplication.getContext()), SharedCookies.storage());
        }
        return shared;
    }

    private final BaseUrlProvider urls;
    private final Session session;
    private final CookieStorage cookies;
    private final Object lock = new Object();
    /** Bumped under {@link #lock} after every successful login; read without it. */
    private volatile int generation = 0;

    AuthedHttp(BaseUrlProvider urls, Session session, CookieStorage cookies) {
        this.urls = urls;
        this.session = session;
        this.cookies = cookies;
    }

    /** GET {@code pathAndQuery} (e.g. "/gameServer/mobile/json/game.jsp?gid=1"), following redirects. */
    public Reply get(String pathAndQuery) throws IOException {
        return send(pathAndQuery, () -> exchangeGet(pathAndQuery));
    }

    /** POST the form body {@code form} to {@code path}, without following a redirect. */
    public Reply postForm(String path, String form) throws IOException {
        return send(path, () -> exchangePost(path, form, 0));
    }

    /** As {@link #postForm(String, String)}, with a body that is built again for the retry. */
    public Reply postForm(String path, FormSupplier form) throws IOException {
        return send(path, () -> exchangePost(path, form.form(), 0));
    }

    /**
     * The value of the server-set password2 cookie (the server-encrypted password), logging
     * in first if the store has no login cookies. mobile/json/index.jsp takes it as its
     * password parameter, so the plaintext does not have to be sent again.
     */
    public String passwordToken() throws IOException {
        ensureLoginCookies();
        String token = storedLoginCookies().get("password2");
        if (token == null) {
            throw new IllegalStateException("password2 cookie missing after the login check");
        }
        return token;
    }

    /**
     * The explicit login from LoginActivity. Clears the whole cookie store first: the server
     * ignores posted credentials while the request carries a valid session, so a leftover
     * session would let a wrong password, or another user, appear to log in.
     */
    public LoginResponse.Outcome loginFresh(String name, String password) throws IOException {
        synchronized (lock) {
            cookies.removeAll();
            Map<String, String> left = storedLoginCookies();
            if (left.containsKey("JSESSIONID") || left.containsKey("name2") || left.containsKey("password2")) {
                throw new IllegalStateException("cookie store still holds login cookies after removeAll");
            }
            LoginResponse.Outcome outcome = login(name, password);
            if (outcome == LoginResponse.Outcome.SUCCESS) {
                generation++;
            }
            cookies.flush();
            return outcome;
        }
    }

    private Reply send(String pathAndQuery, Exchange exchange) throws IOException {
        ensureLoginCookies();
        int seenGeneration = generation;
        String requestPath = new URL(urls.baseUrl() + pathAndQuery).getPath();
        Reply first = exchange.run();
        if (!LoggedOutDetector.isLoggedOut(requestPath, first.finalPath, first.body)) {
            return first;
        }
        relogin(seenGeneration);
        Reply retry = exchange.run();
        if (LoggedOutDetector.isLoggedOut(requestPath, retry.finalPath, retry.body)) {
            NotLoggedInException e = new NotLoggedInException("still logged out after re-login: " + requestPath);
            RedactingLog.e(TAG, "request failed", e);
            throw e;
        }
        return retry;
    }

    private void ensureLoginCookies() throws IOException {
        int seenGeneration = generation;
        Map<String, String> stored = storedLoginCookies();
        if (!stored.containsKey("name2") || !stored.containsKey("password2")) {
            relogin(seenGeneration);
        }
    }

    /**
     * Logs in with the stored credentials, unless another thread already did since
     * {@code seenGeneration}. Unlike {@link #loginFresh}, it keeps the store, except for the
     * session cookie: while the request carries a live session, LoginFilter ignores the posted
     * credentials and sets no name2/password2 cookies.
     */
    private void relogin(int seenGeneration) throws IOException {
        synchronized (lock) {
            if (generation != seenGeneration) {
                return;
            }
            // Tomcat sets JSESSIONID host-only at the ROOT context path "/"; the same name, host
            // and path make CookieManager replace it, and Max-Age=0 then drops it.
            cookies.setCookie(urls.baseUrl() + "/", "JSESSIONID=; Max-Age=0; Path=/");
            LoginResponse.Outcome outcome = login(session.name(), session.password());
            if (outcome != LoginResponse.Outcome.SUCCESS) {
                LoginRejectedException e = new LoginRejectedException("re-login with the stored credentials: " + outcome);
                RedactingLog.e(TAG, "re-login failed", e);
                throw e;
            }
            generation++;
            cookies.flush();
            Log.i(TAG, "re-logged in");
        }
    }

    /** POSTs the credentials to login.jsp; on success, checks that the login cookies were stored for {@code name}. */
    private LoginResponse.Outcome login(String name, String password) throws IOException {
        Reply reply = exchangePost(LOGIN_PATH,
                Forms.encode("mobile", "", "name2", name, "password2", password), LOGIN_TIMEOUT_MS);
        LoginResponse.Outcome outcome = LoginResponse.classify(reply.body);
        if (outcome == LoginResponse.Outcome.SUCCESS) {
            Map<String, String> stored = storedLoginCookies();
            // LoginFilter stores the trimmed, lower-cased name.
            if (!name.trim().equalsIgnoreCase(stored.get("name2")) || !stored.containsKey("password2")) {
                throw new IllegalStateException("login succeeded but its name2/password2 cookies were not stored");
            }
        }
        return outcome;
    }

    private Map<String, String> storedLoginCookies() {
        return CookieHeader.parse(cookies.cookieHeader(urls.baseUrl() + "/gameServer/"));
    }

    private Reply exchangeGet(String pathAndQuery) throws IOException {
        HttpURLConnection connection = open(pathAndQuery, 0);
        return read(connection);
    }

    /** @param timeoutMs connect and read timeout; 0 keeps HttpURLConnection's default (none) */
    private Reply exchangePost(String path, String form, int timeoutMs) throws IOException {
        HttpURLConnection connection = open(path, timeoutMs);
        byte[] body = form.getBytes(StandardCharsets.UTF_8);
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setInstanceFollowRedirects(false);
        connection.setUseCaches(false);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        connection.setFixedLengthStreamingMode(body.length);
        try (OutputStream out = connection.getOutputStream()) {
            out.write(body);
        }
        return read(connection);
    }

    private HttpURLConnection open(String pathAndQuery, int timeoutMs) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(urls.baseUrl() + pathAndQuery).openConnection();
        connection.setConnectTimeout(timeoutMs);
        connection.setReadTimeout(timeoutMs);
        return connection;
    }

    /** getInputStream throws for 4xx/5xx, as the call sites' own reads did. */
    private static Reply read(HttpURLConnection connection) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (InputStream in = connection.getInputStream()) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) != -1) {
                bytes.write(buffer, 0, n);
            }
        }
        return new Reply(connection.getResponseCode(), connection.getURL().getPath(),
                new String(bytes.toByteArray(), StandardCharsets.UTF_8));
    }
}
