package be.submanifold.pentelive.net;

import java.util.ArrayList;
import java.util.List;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

/**
 * OkHttp's view of the shared {@link CookieStorage}. The storage does the domain, path and
 * expiry matching, so the cookies handed back only carry a name and a value; that is all
 * that reaches the request's Cookie header. No extra dependency (okhttp-java-net-cookiejar)
 * is needed.
 */
public final class SharedCookieJar implements CookieJar {

    private final CookieStorage storage;

    public SharedCookieJar(CookieStorage storage) {
        this.storage = storage;
    }

    @Override
    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
        for (Cookie cookie : cookies) {
            storage.setCookie(url.toString(), cookie.toString());
        }
    }

    @Override
    public List<Cookie> loadForRequest(HttpUrl url) {
        List<Cookie> cookies = new ArrayList<>();
        for (String[] pair : CookieHeader.pairs(storage.cookieHeader(url.toString()))) {
            cookies.add(new Cookie.Builder()
                    .name(pair[0])
                    .value(pair[1])
                    .hostOnlyDomain(url.host())
                    .build());
        }
        return cookies;
    }
}
