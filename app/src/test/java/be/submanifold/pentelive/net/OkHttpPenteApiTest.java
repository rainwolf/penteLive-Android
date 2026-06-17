package be.submanifold.pentelive.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * HTTP-level tests for {@link OkHttpPenteApi} using MockWebServer.
 *
 * <p>The auth-expiry signal exercised here is the REAL legacy one: a <strong>200</strong>
 * response whose body does not parse into the expected entity (the logged-out body —
 * {@code json.player == null} at PentePlayer.java:701, {@code gameName == null} at
 * Game.java:466). An okhttp3.Authenticator could never see this (it only fires on 401),
 * which is why re-auth lives inside OkHttpPenteApi#withReauth.
 */
public class OkHttpPenteApiTest {

    private MockWebServer server;
    private FakeSession session;
    private OkHttpPenteApi api;

    private static final String WHOS_ONLINE = "whosonlineandlive.jsp";
    private static final String LOGIN = "login.jsp";

    @Before
    public void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        String withSlash = server.url("/").toString();
        final String baseUrl = withSlash.substring(0, withSlash.length() - 1); // drop trailing '/'
        BaseUrlProvider urls = () -> baseUrl;
        session = new FakeSession("alice", "secret");
        api = new OkHttpPenteApi(session, urls);
    }

    @After
    public void tearDown() {
        try {
            server.shutdown();
        } catch (Exception ignored) {
            // already shut down by a network-failure test
        }
    }

    /** (a) N concurrent app-level auth-expiries trigger EXACTLY ONE re-login. */
    @Test
    public void concurrentAuthExpiry_triggersExactlyOneReLogin() throws Exception {
        final AtomicInteger loginHits = new AtomicInteger(0);
        final AtomicBoolean authed = new AtomicBoolean(false);
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String path = request.getRequestUrl().encodedPath();
                if (path.endsWith(LOGIN)) {
                    loginHits.incrementAndGet();
                    authed.set(true);
                    return new MockResponse().setResponseCode(200).setBody("ok");
                }
                if (path.endsWith(WHOS_ONLINE)) {
                    // Logged-out sentinel before auth: 200 with an empty (null-parsing) body.
                    if (!authed.get()) {
                        return new MockResponse().setResponseCode(200).setBody("");
                    }
                    return new MockResponse().setResponseCode(200).setBody("[]");
                }
                return new MockResponse().setResponseCode(404);
            }
        });

        final int n = 6;
        ExecutorService pool = Executors.newFixedThreadPool(n);
        final CountDownLatch start = new CountDownLatch(1);
        List<Future<Result<WhosOnline>>> futures = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            futures.add(pool.submit(() -> {
                start.await();
                return api.whosOnline();
            }));
        }
        start.countDown(); // release all threads at once

        for (Future<Result<WhosOnline>> f : futures) {
            Result<WhosOnline> r = f.get(20, TimeUnit.SECONDS);
            assertTrue("each concurrent call must succeed after the single re-auth", r.isOk());
            assertNotNull(r.value);
        }
        pool.shutdown();

        assertEquals("exactly one re-login despite N concurrent expiries", 1, loginHits.get());
    }

    /** (b) credential-drop regression: re-login AND the retried request carry name2/password2. */
    @Test
    public void reAuthRetry_keepsName2AndPassword2() throws Exception {
        final List<HttpUrl> dataRequests = Collections.synchronizedList(new ArrayList<>());
        final List<HttpUrl> loginRequests = Collections.synchronizedList(new ArrayList<>());
        final AtomicBoolean authed = new AtomicBoolean(false);
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                HttpUrl u = request.getRequestUrl();
                String path = u.encodedPath();
                if (path.endsWith(LOGIN)) {
                    loginRequests.add(u);
                    authed.set(true);
                    return new MockResponse().setResponseCode(200).setBody("ok");
                }
                if (path.endsWith(WHOS_ONLINE)) {
                    dataRequests.add(u);
                    if (!authed.get()) {
                        return new MockResponse().setResponseCode(200).setBody(""); // expiry signal
                    }
                    return new MockResponse().setResponseCode(200).setBody("[]");
                }
                return new MockResponse().setResponseCode(404);
            }
        });

        Result<WhosOnline> result = api.whosOnline();

        assertTrue("call succeeds after a single re-auth retry", result.isOk());
        assertEquals("initial expiry + one retry == two data requests", 2, dataRequests.size());

        HttpUrl retry = dataRequests.get(1);
        assertEquals("name2 preserved on retried data request (regression for PentePlayer.java:678)",
                "alice", retry.queryParameter("name2"));
        assertEquals("password2 preserved on retried data request",
                "secret", retry.queryParameter("password2"));

        assertEquals("exactly one re-login request", 1, loginRequests.size());
        HttpUrl login = loginRequests.get(0);
        assertEquals("re-login carries name2", "alice", login.queryParameter("name2"));
        assertEquals("re-login carries password2", "secret", login.queryParameter("password2"));
    }

    /** Reason mapping: a 500 (non-auth) maps to SERVER with its http code, and never re-auths. */
    @Test
    public void serverError_mapsToServer() {
        final AtomicInteger loginHits = new AtomicInteger(0);
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                if (request.getRequestUrl().encodedPath().endsWith(LOGIN)) {
                    loginHits.incrementAndGet();
                }
                return new MockResponse().setResponseCode(500);
            }
        });

        Result<WhosOnline> result = api.whosOnline();

        assertFalse(result.isOk());
        assertEquals(Result.Reason.SERVER, result.failure.reason);
        assertEquals(500, result.failure.httpCode);
        assertEquals("a 500 is not an auth signal -> no re-login", 0, loginHits.get());
    }

    /** Reason mapping: a connection failure maps to NETWORK. */
    @Test
    public void connectionFailure_mapsToNetwork() throws Exception {
        server.shutdown(); // nothing listening anymore -> connect fails

        Result<WhosOnline> result = api.whosOnline();

        assertFalse(result.isOk());
        assertEquals(Result.Reason.NETWORK, result.failure.reason);
        assertNotNull("network failures retain the IOException cause", result.failure.cause);
    }

    /** Reason mapping: login with the wrong-credentials body marker maps to INVALID_CREDENTIALS. */
    @Test
    public void loginWrongCredentials_mapsToInvalidCredentials() {
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                // login.jsp returns 200 even for bad creds (LoginActivity.java:390).
                return new MockResponse().setResponseCode(200)
                        .setBody("<html>Invalid name or password, please try again.</html>");
            }
        });

        Result<Boolean> result = api.login("alice", "wrong");

        assertFalse(result.isOk());
        assertEquals(Result.Reason.INVALID_CREDENTIALS, result.failure.reason);
    }

    /** Reason mapping: a good login (200, no marker) succeeds. */
    @Test
    public void loginValidCredentials_succeeds() {
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                return new MockResponse().setResponseCode(200).setBody("welcome");
            }
        });

        Result<Boolean> result = api.login("alice", "secret");

        assertTrue(result.isOk());
        assertEquals(Boolean.TRUE, result.value);
    }

    /**
     * The 4-arg submitMove overload appends renjuAction as a query parameter, for each of
     * the three TB Renju wire actions: swap (take-over, no stone), move (1 or 10 stones)
     * and the atomic 2-stone select.
     */
    @Test
    public void submitMoveAppendsRenjuAction() throws Exception {
        server.setDispatcher(new Dispatcher() {
            @Override public MockResponse dispatch(RecordedRequest req) {
                return new MockResponse().setResponseCode(200).setBody("ok");
            }
        });

        // take-over: swap with no appended stone.
        api.submitMove("999", "1", "", "swap");
        HttpUrl swap = server.takeRequest(5, TimeUnit.SECONDS).getRequestUrl();
        assertEquals("move", swap.queryParameter("command"));
        assertEquals("1", swap.queryParameter("moves"));
        assertEquals("swap", swap.queryParameter("renjuAction"));

        // Branch B: ten 5th-move offers carried atomically via the `move` action.
        api.submitMove("999", "113,114,115,116,128,129,130,131,144,145", "", "move");
        HttpUrl tenMove = server.takeRequest(5, TimeUnit.SECONDS).getRequestUrl();
        assertEquals("113,114,115,116,128,129,130,131,144,145", tenMove.queryParameter("moves"));
        assertEquals("move", tenMove.queryParameter("renjuAction"));

        // atomic 2-stone select: chosen black 5th + white 6th.
        api.submitMove("999", "130,200", "", "select");
        HttpUrl sel = server.takeRequest(5, TimeUnit.SECONDS).getRequestUrl();
        assertEquals("130,200", sel.queryParameter("moves"));
        assertEquals("select", sel.queryParameter("renjuAction"));
    }

    /** An expiry that survives the single re-auth maps to AUTH_EXPIRED (retry is capped at one). */
    @Test
    public void authExpiryThatSurvivesReauth_mapsToAuthExpired() {
        final AtomicInteger loginHits = new AtomicInteger(0);
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                if (request.getRequestUrl().encodedPath().endsWith(LOGIN)) {
                    loginHits.incrementAndGet();
                    return new MockResponse().setResponseCode(200).setBody("ok");
                }
                // Always returns the logged-out sentinel, even after re-login.
                return new MockResponse().setResponseCode(200).setBody("");
            }
        });

        Result<WhosOnline> result = api.whosOnline();

        assertFalse(result.isOk());
        assertEquals(Result.Reason.AUTH_EXPIRED, result.failure.reason);
        assertEquals("http code preserved on terminal AUTH_EXPIRED (200 logged-out sentinel)",
                200, result.failure.httpCode);
        assertEquals("re-auth attempted exactly once (no retry loop)", 1, loginHits.get());
    }

    /**
     * Re-auth fix 2: if re-login returns the wrong-credentials body (200 + marker), performLogin
     * returns false → withReauth returns AUTH_EXPIRED and does NOT enter a retry loop.
     */
    @Test
    public void reAuthWrongCredentialsBody_mapsToAuthExpired_noRetryLoop() {
        final AtomicInteger loginHits = new AtomicInteger(0);
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String path = request.getRequestUrl().encodedPath();
                if (path.endsWith(LOGIN)) {
                    loginHits.incrementAndGet();
                    // login.jsp returns 200 even for bad creds (LoginActivity.java:390).
                    return new MockResponse().setResponseCode(200)
                            .setBody("<html>Invalid name or password, please try again.</html>");
                }
                // Data endpoint always returns the auth-expired sentinel.
                return new MockResponse().setResponseCode(200).setBody("");
            }
        });

        Result<WhosOnline> result = api.whosOnline();

        assertFalse(result.isOk());
        assertEquals("wrong creds on re-login → AUTH_EXPIRED, not a spurious success",
                Result.Reason.AUTH_EXPIRED, result.failure.reason);
        assertEquals("login attempted exactly once — no spurious extra retry", 1, loginHits.get());
    }

    /**
     * Re-auth fix 3: if re-login throws IOException (network down during re-auth), the result
     * surfaces as NETWORK, not AUTH_EXPIRED, so callers can distinguish connectivity issues.
     */
    @Test
    public void reAuthNetworkFailure_mapsToNetwork() {
        final AtomicInteger loginHits = new AtomicInteger(0);
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String path = request.getRequestUrl().encodedPath();
                if (path.endsWith(LOGIN)) {
                    loginHits.incrementAndGet();
                    // Drop the connection after receiving the request — causes an IOException
                    // on the client when it tries to read the response.
                    return new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST);
                }
                // Data endpoint always returns the auth-expired sentinel.
                return new MockResponse().setResponseCode(200).setBody("");
            }
        });

        Result<WhosOnline> result = api.whosOnline();

        assertFalse(result.isOk());
        assertEquals("network failure during re-login should surface as NETWORK, not AUTH_EXPIRED",
                Result.Reason.NETWORK, result.failure.reason);
        assertNotNull("IOException cause should be retained", result.failure.cause);
        // OkHttp transparently retries idempotent GET requests on connection failure, so we
        // only assert that at least one login attempt was made rather than pinning the exact count.
        assertTrue("login should have been attempted at least once", loginHits.get() >= 1);
    }
}
