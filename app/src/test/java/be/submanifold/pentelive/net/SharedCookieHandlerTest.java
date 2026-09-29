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

    @Test
    public void put_storesEachSetCookieValue_withTheRequestUri() throws Exception {
        FakeCookieStorage storage = new FakeCookieStorage();
        SharedCookieHandler handler = new SharedCookieHandler(storage);
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

        new SharedCookieHandler(storage).put(LOGIN, headers);

        assertTrue(storage.setCalls.isEmpty());
    }

    @Test
    public void get_returnsOneCookieHeader_withTheStoredCookies() throws Exception {
        FakeCookieStorage storage = new FakeCookieStorage();
        storage.put("www.pente.org", "JSESSIONID", "abc");
        storage.put("www.pente.org", "password2", "x/y+z==");

        Map<String, List<String>> result =
                new SharedCookieHandler(storage).get(LOGIN, Collections.<String, List<String>>emptyMap());

        assertEquals(1, result.size());
        assertEquals(Collections.singletonList("JSESSIONID=abc; password2=x/y+z=="), result.get("Cookie"));
    }

    @Test
    public void get_withNoCookies_returnsEmptyMap() throws Exception {
        Map<String, List<String>> result = new SharedCookieHandler(new FakeCookieStorage())
                .get(LOGIN, Collections.<String, List<String>>emptyMap());

        assertTrue(result.isEmpty());
    }
}
