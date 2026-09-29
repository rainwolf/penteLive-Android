package be.submanifold.pentelive.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class SharedCookieHandlerTest {

    private static final URI LOGIN = URI.create("https://www.pente.org/gameServer/login.jsp");
    private static final URI FIREBASE =
            URI.create("https://firebaseinstallations.googleapis.com/v1/projects/p/installations");
    private static final PenteHosts PROD = new PenteHosts(new StaticBaseUrlProvider(false));
    private static final PenteHosts DEV = new PenteHosts(new StaticBaseUrlProvider(true));

    private static Map<String, List<String>> setCookieHeaders(String... values) {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put(null, Collections.singletonList("HTTP/1.1 200 OK"));
        headers.put("Set-Cookie", Arrays.asList(values));
        return headers;
    }

    @Test
    public void put_storesEachSetCookieValue_withTheRequestUri() throws Exception {
        FakeCookieStorage storage = new FakeCookieStorage();
        SharedCookieHandler handler = new SharedCookieHandler(storage, PROD);
        Map<String, List<String>> headers = new HashMap<>();
        // Android's HttpURLConnection puts the status line under the null key.
        headers.put(null, Collections.singletonList("HTTP/1.1 200 OK"));
        headers.put("Set-Cookie", Arrays.asList(
                "JSESSIONID=abc; Path=/; HttpOnly",
                "password2=x/y+z==; Version=1; Domain=pente.org; Path=/"));
        headers.put("set-cookie", Collections.singletonList("name2=alice; Path=/"));
        headers.put("Content-Type", Collections.singletonList("text/html"));

        handler.put(LOGIN, headers);

        assertEquals(3, storage.setCalls.size());
        for (String[] call : storage.setCalls) {
            assertEquals(LOGIN.toString(), call[0]);
        }
        assertEquals("abc", storage.byHost.get("www.pente.org").get("JSESSIONID"));
        assertEquals("x/y+z==", storage.byHost.get("www.pente.org").get("password2"));
        assertEquals("alice", storage.byHost.get("www.pente.org").get("name2"));
    }

    @Test
    public void put_withoutSetCookie_storesNothing() throws Exception {
        FakeCookieStorage storage = new FakeCookieStorage();
        Map<String, List<String>> headers = new HashMap<>();
        headers.put(null, Collections.singletonList("HTTP/1.1 200 OK"));
        headers.put("Content-Type", Collections.singletonList("text/html"));

        new SharedCookieHandler(storage, PROD).put(LOGIN, headers);

        assertTrue(storage.setCalls.isEmpty());
    }

    @Test
    public void get_returnsOneCookieHeader_withTheStoredCookies() throws Exception {
        FakeCookieStorage storage = new FakeCookieStorage();
        storage.put("www.pente.org", "JSESSIONID", "abc");
        storage.put("www.pente.org", "password2", "x/y+z==");

        Map<String, List<String>> result =
                new SharedCookieHandler(storage, PROD).get(LOGIN, Collections.<String, List<String>>emptyMap());

        assertEquals(1, result.size());
        assertEquals(Collections.singletonList("JSESSIONID=abc; password2=x/y+z=="), result.get("Cookie"));
    }

    @Test
    public void get_withNoCookies_returnsEmptyMap() throws Exception {
        Map<String, List<String>> result = new SharedCookieHandler(new FakeCookieStorage(), PROD)
                .get(LOGIN, Collections.<String, List<String>>emptyMap());

        assertTrue(result.isEmpty());
    }

    @Test
    public void get_forANonPenteHost_sendsNoCookie_andNeverReadsTheStore() throws Exception {
        FakeCookieStorage storage = new FakeCookieStorage();
        storage.put(FIREBASE.getHost(), "tracker", "t");

        Map<String, List<String>> result =
                new SharedCookieHandler(storage, PROD).get(FIREBASE, Collections.<String, List<String>>emptyMap());

        assertTrue(result.isEmpty());
        assertTrue("the store (CookieManager) was not touched", storage.headerCalls.isEmpty());
    }

    @Test
    public void put_forANonPenteHost_storesNothing() throws Exception {
        FakeCookieStorage storage = new FakeCookieStorage();

        new SharedCookieHandler(storage, PROD).put(FIREBASE, setCookieHeaders("tracker=t; Path=/"));

        assertTrue(storage.setCalls.isEmpty());
        assertTrue(storage.byHost.isEmpty());
    }

    @Test
    public void lookalikeHosts_areNotPenteHosts() throws Exception {
        FakeCookieStorage storage = new FakeCookieStorage();
        SharedCookieHandler handler = new SharedCookieHandler(storage, PROD);
        for (String url : Arrays.asList("https://evil.pente.org/", "https://pente.org.evil.com/",
                "https://notpente.org/", "https://10.0.2.2/")) {
            URI uri = URI.create(url);
            storage.put(uri.getHost(), "c", "v");
            assertTrue(url, handler.get(uri, Collections.<String, List<String>>emptyMap()).isEmpty());
            handler.put(uri, setCookieHeaders("d=w; Path=/"));
        }
        assertTrue(storage.headerCalls.isEmpty());
        assertTrue(storage.setCalls.isEmpty());
    }

    @Test
    public void penteHosts_sendAndStoreCookies_inProduction() throws Exception {
        assertSendsAndStores(PROD, "https://www.pente.org/gameServer/index.jsp");
        assertSendsAndStores(PROD, "https://pente.org/gameServer/index.jsp");
        assertSendsAndStores(PROD, "https://WWW.Pente.ORG/gameServer/index.jsp");
    }

    @Test
    public void penteHosts_sendAndStoreCookies_inDevelopment() throws Exception {
        assertSendsAndStores(DEV, "https://10.0.2.2/gameServer/index.jsp");
        // Screens that still hard-code https://www.pente.org keep their cookies.
        assertSendsAndStores(DEV, "https://www.pente.org/gameServer/index.jsp");
        assertSendsAndStores(DEV, "https://pente.org/gameServer/index.jsp");
    }

    private static void assertSendsAndStores(PenteHosts hosts, String url) throws Exception {
        URI uri = URI.create(url);
        FakeCookieStorage storage = new FakeCookieStorage();
        SharedCookieHandler handler = new SharedCookieHandler(storage, hosts);

        handler.put(uri, setCookieHeaders("name2=alice; Path=/"));
        Map<String, List<String>> result = handler.get(uri, Collections.<String, List<String>>emptyMap());

        assertEquals(url, 1, storage.setCalls.size());
        assertEquals(url, Collections.singletonList("name2=alice"), result.get("Cookie"));
    }
}
