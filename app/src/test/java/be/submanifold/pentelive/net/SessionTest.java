package be.submanifold.pentelive.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;

import okhttp3.Cookie;
import okhttp3.HttpUrl;

import org.junit.Test;

public class SessionTest {

    private static final HttpUrl PENTE = HttpUrl.get("https://www.pente.org/");

    @Test
    public void cookieJar_persistsSetCookie_andReplaysIt() {
        InMemoryCookieJar jar = new InMemoryCookieJar();

        Cookie setCookie = Cookie.parse(PENTE, "JSESSIONID=abc123; domain=pente.org; path=/");
        jar.saveFromResponse(PENTE, Collections.singletonList(setCookie));

        List<Cookie> replayed = jar.loadForRequest(PENTE);

        assertEquals(1, replayed.size());
        assertEquals("JSESSIONID", replayed.get(0).name());
        assertEquals("abc123", replayed.get(0).value());
    }

    @Test
    public void cookieJar_returnsEmpty_forUnknownHost() {
        InMemoryCookieJar jar = new InMemoryCookieJar();
        List<Cookie> replayed = jar.loadForRequest(HttpUrl.get("https://example.com/"));
        assertTrue(replayed.isEmpty());
    }

    @Test
    public void fakeSession_replaysCookies_throughCookieJar() {
        FakeSession session = new FakeSession("alice", "secret");

        Cookie setCookie = Cookie.parse(PENTE, "name2=alice; domain=pente.org; path=/");
        session.cookieJar().saveFromResponse(PENTE, Collections.singletonList(setCookie));

        List<Cookie> replayed = session.cookieJar().loadForRequest(PENTE);

        assertEquals(1, replayed.size());
        assertEquals("alice", replayed.get(0).value());
    }

    @Test
    public void session_updateCredentials_changesNameAndPassword() {
        FakeSession session = new FakeSession("alice", "secret");
        assertEquals("alice", session.name());
        assertEquals("secret", session.password());

        session.updateCredentials("bob", "hunter2");

        assertEquals("bob", session.name());
        assertEquals("hunter2", session.password());
    }

    @Test
    public void baseUrlProvider_returnsProd_whenNotDevelopment() {
        BaseUrlProvider provider = new StaticBaseUrlProvider(false);
        assertEquals("https://www.pente.org", provider.baseUrl());
    }

    @Test
    public void baseUrlProvider_returnsEmulatorHost_whenDevelopment() {
        BaseUrlProvider provider = new StaticBaseUrlProvider(true);
        assertEquals("https://10.0.2.2", provider.baseUrl());
    }
}
