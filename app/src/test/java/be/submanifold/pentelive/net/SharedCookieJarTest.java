package be.submanifold.pentelive.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import okhttp3.Cookie;
import okhttp3.HttpUrl;

import org.junit.Test;

public class SharedCookieJarTest {

    private static final HttpUrl WHOS_ONLINE =
            HttpUrl.get("https://www.pente.org/gameServer/mobile/json/whosonlineandlive.jsp");

    @Test
    public void saveFromResponse_passesEachCookieWithDomainAndPath() {
        FakeCookieStorage storage = new FakeCookieStorage();
        Cookie session = Cookie.parse(WHOS_ONLINE, "JSESSIONID=abc; Path=/; HttpOnly");
        Cookie password = Cookie.parse(WHOS_ONLINE, "password2=x/y+z==; Domain=pente.org; Path=/");

        new SharedCookieJar(storage).saveFromResponse(WHOS_ONLINE, Arrays.asList(session, password));

        assertEquals(2, storage.setCalls.size());
        assertEquals(WHOS_ONLINE.toString(), storage.setCalls.get(0)[0]);
        assertTrue(storage.setCalls.get(0)[1].startsWith("JSESSIONID=abc;"));
        assertTrue(storage.setCalls.get(0)[1].contains("path=/"));
        assertTrue(storage.setCalls.get(1)[1].startsWith("password2=x/y+z==;"));
        assertTrue(storage.setCalls.get(1)[1].contains("domain=pente.org"));
    }

    @Test
    public void loadForRequest_mapsTheStoredHeaderBackToNameValueCookies() {
        FakeCookieStorage storage = new FakeCookieStorage();
        storage.put("www.pente.org", "JSESSIONID", "abc");
        storage.put("www.pente.org", "password2", "x/y+z==");

        List<Cookie> cookies = new SharedCookieJar(storage).loadForRequest(WHOS_ONLINE);

        assertEquals(2, cookies.size());
        assertEquals("JSESSIONID", cookies.get(0).name());
        assertEquals("abc", cookies.get(0).value());
        assertEquals("password2", cookies.get(1).name());
        assertEquals("x/y+z==", cookies.get(1).value());
        assertTrue(cookies.get(1).matches(WHOS_ONLINE));
    }

    @Test
    public void loadForRequest_withNoCookies_isEmpty() {
        assertTrue(new SharedCookieJar(new FakeCookieStorage()).loadForRequest(WHOS_ONLINE).isEmpty());
    }
}
