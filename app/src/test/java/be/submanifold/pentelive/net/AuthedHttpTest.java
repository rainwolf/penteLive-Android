package be.submanifold.pentelive.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.CookieHandler;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * {@link AuthedHttp} against MockWebServer, through the JDK's HttpURLConnection with the
 * {@link SharedCookieHandler} installed as the default CookieHandler, as on the device.
 */
public class AuthedHttpTest {

    private static final String LOGIN = "/gameServer/login.jsp";
    private static final String GAME = "/gameServer/mobile/json/game.jsp";
    private static final String INDEX = "/gameServer/mobile/json/index.jsp";
    private static final String MOBILE_INDEX = "/gameServer/mobile/index.jsp";
    private static final String GAME_JSON = "{\"gameName\":\"Pente\"}";

    private MockWebServer server;
    private CookieHandler previousHandler;
    private FakeCookieStorage cookies;
    private String base;
    private String host;
    private AuthedHttp http;

    private final List<RecordedRequest> logins = Collections.synchronizedList(new ArrayList<>());
    private final List<RecordedRequest> data = Collections.synchronizedList(new ArrayList<>());
    private final List<String> order = Collections.synchronizedList(new ArrayList<>());

    @Before
    public void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        String withSlash = server.url("/").toString();
        base = withSlash.substring(0, withSlash.length() - 1);
        host = URI.create(base).getHost();
        cookies = new FakeCookieStorage();
        previousHandler = CookieHandler.getDefault();
        CookieHandler.setDefault(new SharedCookieHandler(cookies));
        http = new AuthedHttp(() -> base, new FakeSession("alice", "secret"), cookies);
    }

    @After
    public void tearDown() throws Exception {
        CookieHandler.setDefault(previousHandler);
        server.shutdown();
    }

    private void loggedIn(String token) {
        cookies.put(host, "JSESSIONID", "s0");
        cookies.put(host, "name2", "alice");
        cookies.put(host, "password2", token);
    }

    private static MockResponse loginSuccess(String token) {
        return new MockResponse().setResponseCode(200)
                .addHeader("Set-Cookie", "JSESSIONID=s1; Path=/; HttpOnly")
                .addHeader("Set-Cookie", "name2=alice; Version=1; Path=/")
                .addHeader("Set-Cookie", "password2=" + token + "; Version=1; Path=/")
                .setBody(LoginResponseTest.SUCCESS_PAGE);
    }

    private static MockResponse loginInvalid() {
        return new MockResponse().setResponseCode(200).setBody(LoginResponseTest.INVALID_PAGE);
    }

    private static MockResponse redirect(String location) {
        return new MockResponse().setResponseCode(302).addHeader("Location", location);
    }

    /** Routes login.jsp to {@code login}, the logged-out landing page to its text, the rest to {@code dataResponse}. */
    private void serve(final MockResponse login, final DataResponder dataResponse) {
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String path = request.getRequestUrl().encodedPath();
                if (path.equals(LOGIN)) {
                    logins.add(request);
                    order.add("login");
                    return login;
                }
                if (path.equals(MOBILE_INDEX)) {
                    return new MockResponse().setResponseCode(200)
                            .setBody("Invalid name or password, please try again.");
                }
                data.add(request);
                order.add("data");
                return dataResponse.respond(request, logins.size());
            }
        });
    }

    private interface DataResponder {
        MockResponse respond(RecordedRequest request, int loginsSoFar);
    }

    // (a)
    @Test
    public void loginFresh_clearsTheStoreFirst_thenPostsTheCredentialsInTheBody() throws Exception {
        cookies.put(host, "JSESSIONID", "stale");
        cookies.put(host, "name2", "bob");
        serve(loginSuccess("tok"), (r, n) -> new MockResponse().setResponseCode(404));

        LoginResponse.Outcome outcome = http.loginFresh("alice", "p&w+d ü");

        assertEquals(LoginResponse.Outcome.SUCCESS, outcome);
        assertEquals("removeAll", cookies.events.get(0));
        assertEquals(1, logins.size());
        RecordedRequest login = logins.get(0);
        assertEquals("POST", login.getMethod());
        assertNull("no query string on the login URL", login.getRequestUrl().query());
        assertNull("the stale session was not sent", login.getHeader("Cookie"));
        assertTrue(login.getHeader("Content-Type").startsWith("application/x-www-form-urlencoded"));
        assertEquals("mobile=&name2=alice&password2=p%26w%2Bd+%C3%BC", login.getBody().readUtf8());
        assertEquals("tok", CookieHeader.parse(cookies.cookieHeader(base + "/gameServer/")).get("password2"));
        assertEquals("flush", cookies.events.get(cookies.events.size() - 1));
    }

    // (b)
    @Test
    public void loginFresh_invalidCredentials_isReported_andNothingElseIsRequested() throws Exception {
        serve(loginInvalid(), (r, n) -> new MockResponse().setResponseCode(200).setBody(GAME_JSON));

        assertEquals(LoginResponse.Outcome.INVALID_CREDENTIALS, http.loginFresh("alice", "wrong"));
        assertEquals(1, server.getRequestCount());
        assertTrue(data.isEmpty());
    }

    // (c)
    @Test
    public void loginFresh_pageWithoutLogoutLink_isUnexpected() throws Exception {
        serve(new MockResponse().setResponseCode(200)
                        .setBody("<form name=\"login_form\" method=\"post\" action=\"/gameServer/index.jsp\">"),
                (r, n) -> new MockResponse().setResponseCode(404));

        assertEquals(LoginResponse.Outcome.UNEXPECTED, http.loginFresh("alice", "secret"));
    }

    // (d)
    @Test
    public void loginFresh_successWithoutLoginCookies_throws() throws Exception {
        serve(new MockResponse().setResponseCode(200)
                        .addHeader("Set-Cookie", "JSESSIONID=s1; Path=/")
                        .setBody(LoginResponseTest.SUCCESS_PAGE),
                (r, n) -> new MockResponse().setResponseCode(404));

        try {
            http.loginFresh("alice", "secret");
            fail("expected IllegalStateException");
        } catch (IllegalStateException expected) {
            assertFalse(expected.getMessage().contains("secret"));
        }
    }

    // (e)
    @Test
    public void get_sendsTheStoredCookies_andNoCredentialsInTheUrl() throws Exception {
        loggedIn("tok");
        serve(loginSuccess("tok"), (r, n) -> new MockResponse().setResponseCode(200).setBody(GAME_JSON));

        AuthedHttp.Reply reply = http.get(GAME + "?gid=5");

        assertEquals(200, reply.code);
        assertEquals(GAME, reply.finalPath);
        assertEquals(GAME_JSON, reply.body);
        assertTrue(logins.isEmpty());
        RecordedRequest request = data.get(0);
        assertEquals("gid=5", request.getRequestUrl().query());
        assertTrue(request.getHeader("Cookie").contains("password2=tok"));
        assertTrue(request.getHeader("Cookie").contains("JSESSIONID=s0"));
    }

    // (f)
    @Test
    public void get_redirectToTheLoggedOutPage_logsInOnce_andRetriesOnce() throws Exception {
        loggedIn("old");
        serve(loginSuccess("new"), (r, loginsSoFar) -> loginsSoFar == 0
                ? redirect(MOBILE_INDEX)
                : new MockResponse().setResponseCode(200).setBody(GAME_JSON));

        AuthedHttp.Reply reply = http.get(GAME + "?gid=5");

        assertEquals(GAME_JSON, reply.body);
        assertEquals(1, logins.size());
        assertEquals(2, data.size());
        assertEquals("POST", logins.get(0).getMethod());
        assertEquals("gid=5", data.get(1).getRequestUrl().query());
        assertTrue(data.get(1).getHeader("Cookie").contains("password2=new"));
        assertFalse("an expiry re-login keeps the store", cookies.events.contains("removeAll"));
    }

    // (g)
    @Test
    public void get_stillLoggedOutAfterReLogin_throwsNotLoggedIn() throws Exception {
        loggedIn("tok");
        serve(loginSuccess("tok"), (r, n) -> redirect(MOBILE_INDEX));

        try {
            http.get(GAME + "?gid=5");
            fail("expected NotLoggedInException");
        } catch (NotLoggedInException expected) {
            assertFalse(expected.getMessage().contains("gid="));
        }
        assertEquals(2, data.size());
        assertEquals(1, logins.size());
    }

    // (h)
    @Test
    public void get_reLoginRejected_throwsLoginRejected() throws Exception {
        loggedIn("tok");
        serve(loginInvalid(), (r, n) -> redirect(MOBILE_INDEX));

        try {
            http.get(GAME + "?gid=5");
            fail("expected LoginRejectedException");
        } catch (LoginRejectedException expected) {
            assertFalse(expected.getMessage().contains("secret"));
        }
        assertEquals(1, data.size());
        assertEquals(1, logins.size());
    }

    // (i)
    @Test
    public void get_withoutLoginCookies_logsInBeforeTheFirstRequest() throws Exception {
        serve(loginSuccess("tok"), (r, n) -> new MockResponse().setResponseCode(200).setBody(GAME_JSON));

        AuthedHttp.Reply reply = http.get(GAME + "?gid=5");

        assertEquals(GAME_JSON, reply.body);
        assertEquals(java.util.Arrays.asList("login", "data"), order);
        assertEquals("mobile=&name2=alice&password2=secret", logins.get(0).getBody().readUtf8());
        assertTrue(data.get(0).getHeader("Cookie").contains("password2=tok"));
    }

    // (j)
    @Test
    public void postForm_supplierIsEvaluatedAgainAfterReLogin() throws Exception {
        loggedIn("old");
        serve(loginSuccess("new"), (r, loginsSoFar) -> loginsSoFar == 0
                ? new MockResponse().setResponseCode(200)
                        .setBody("{\"error\":\"Invalid name or password, please try again.\"}")
                : new MockResponse().setResponseCode(200).setBody("{\"player\":{}}"));

        AuthedHttp.Reply reply = http.postForm(INDEX,
                () -> Forms.encode("name", "alice", "password", http.passwordToken()));

        assertEquals("{\"player\":{}}", reply.body);
        assertEquals(2, data.size());
        assertEquals("name=alice&password=old", data.get(0).getBody().readUtf8());
        assertEquals("name=alice&password=new", data.get(1).getBody().readUtf8());
        assertEquals("POST", data.get(1).getMethod());
        assertNull(data.get(1).getRequestUrl().query());
    }

    // (k)
    @Test
    public void postForm_doesNotFollowARedirect() throws Exception {
        loggedIn("tok");
        final AtomicInteger emptyHits = new AtomicInteger();
        serve(loginSuccess("tok"), (r, n) -> {
            if (r.getRequestUrl().encodedPath().equals("/gameServer/mobile/empty.jsp")) {
                emptyHits.incrementAndGet();
                return new MockResponse().setResponseCode(200);
            }
            return redirect("/gameServer/mobile/empty.jsp");
        });

        AuthedHttp.Reply reply = http.postForm("/gameServer/tb/game", "command=move&gid=5");

        assertEquals(302, reply.code);
        assertEquals("/gameServer/tb/game", reply.finalPath);
        assertEquals(0, emptyHits.get());
        assertEquals("command=move&gid=5", data.get(0).getBody().readUtf8());
        assertTrue(logins.isEmpty());
    }

    /**
     * login.jsp as LoginFilter serves it: a request carrying the live session takes the
     * session branch, ignores the posted credentials and sets no login cookies, yet renders
     * the logged-in page; without the session it logs in and sets the login cookies.
     */
    private MockResponse sessionAwareLogin(RecordedRequest request, String liveSession, String token) {
        // A request without any cookie carries no Cookie header at all.
        String cookie = request.getHeader("Cookie");
        if (cookie != null && liveSession.equals(CookieHeader.parse(cookie).get("JSESSIONID"))) {
            return new MockResponse().setResponseCode(200).setBody(LoginResponseTest.SUCCESS_PAGE);
        }
        return loginSuccess(token);
    }

    private void serveSessionAware(final String liveSession, final String token) {
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String path = request.getRequestUrl().encodedPath();
                if (path.equals(LOGIN)) {
                    logins.add(request);
                    order.add("login");
                    return sessionAwareLogin(request, liveSession, token);
                }
                data.add(request);
                order.add("data");
                return new MockResponse().setResponseCode(200).setBody(GAME_JSON);
            }
        });
    }

    @Test
    public void preCheckLogin_withALiveSessionButNoLoginCookies_dropsTheSessionFirst() throws Exception {
        cookies.put(host, "JSESSIONID", "live");
        serveSessionAware("live", "tok");

        AuthedHttp.Reply reply = http.get(GAME + "?gid=5");

        assertEquals(GAME_JSON, reply.body);
        assertEquals(1, logins.size());
        String sent = logins.get(0).getHeader("Cookie");
        assertTrue("the live session was not sent with the login POST: " + sent,
                sent == null || !CookieHeader.parse(sent).containsKey("JSESSIONID"));
        java.util.Map<String, String> stored = CookieHeader.parse(cookies.cookieHeader(base + "/gameServer/"));
        assertEquals("alice", stored.get("name2"));
        assertEquals("tok", stored.get("password2"));
        assertEquals("s1", stored.get("JSESSIONID"));
        assertFalse("only the session cookie is dropped", cookies.events.contains("removeAll"));
    }

    @Test
    public void reLoginAfterLoggedOut_dropsTheSessionFirst() throws Exception {
        loggedIn("old");
        serve(loginSuccess("new"), (r, loginsSoFar) -> loginsSoFar == 0
                ? redirect(MOBILE_INDEX)
                : new MockResponse().setResponseCode(200).setBody(GAME_JSON));

        http.get(GAME + "?gid=5");

        assertEquals(1, logins.size());
        String sent = logins.get(0).getHeader("Cookie");
        assertTrue("the old session was not sent with the login POST: " + sent,
                !CookieHeader.parse(sent).containsKey("JSESSIONID"));
        assertTrue("the login cookies still go along", CookieHeader.parse(sent).containsKey("name2"));
    }

    @Test
    public void concurrentExpiries_logInOnlyOnce() throws Exception {
        loggedIn("old");
        final AtomicBoolean authed = new AtomicBoolean(false);
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                String path = request.getRequestUrl().encodedPath();
                if (path.equals(LOGIN)) {
                    logins.add(request);
                    authed.set(true);
                    return loginSuccess("new");
                }
                if (path.equals(MOBILE_INDEX)) {
                    return new MockResponse().setResponseCode(200).setBody("");
                }
                return authed.get()
                        ? new MockResponse().setResponseCode(200).setBody(GAME_JSON)
                        : redirect(MOBILE_INDEX);
            }
        });
        int n = 6;
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(n);
        final java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
        List<java.util.concurrent.Future<AuthedHttp.Reply>> futures = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            futures.add(pool.submit(() -> {
                start.await();
                return http.get(GAME + "?gid=5");
            }));
        }
        start.countDown();
        for (java.util.concurrent.Future<AuthedHttp.Reply> f : futures) {
            assertEquals(GAME_JSON, f.get(20, TimeUnit.SECONDS).body);
        }
        pool.shutdown();

        assertEquals(1, logins.size());
    }
}
