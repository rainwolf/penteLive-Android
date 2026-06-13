package be.submanifold.pentelive.net;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

/**
 * Thread-safe in-memory CookieJar. Stores each response cookie keyed by
 * name+path per host and replays the non-expired, matching cookies on later
 * requests. Pure JVM (no Android deps) so it is unit-testable. Replaces the
 * manual name2/password2 cookie-string assembly in PentePlayer/Game.
 */
public final class InMemoryCookieJar implements CookieJar {

    /** host -&gt; (name\0path -&gt; cookie) — null separator avoids collision with legal name/path chars */
    private final Map<String, Map<String, Cookie>> store = new LinkedHashMap<>();

    @Override
    public synchronized void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
        Map<String, Cookie> hostCookies = store.computeIfAbsent(url.host(), h -> new LinkedHashMap<>());
        for (Cookie cookie : cookies) {
            hostCookies.put(cookie.name() + '\0' + cookie.path(), cookie);
        }
    }

    @Override
    public synchronized List<Cookie> loadForRequest(HttpUrl url) {
        Map<String, Cookie> hostCookies = store.get(url.host());
        if (hostCookies == null) {
            return Collections.emptyList();
        }
        long now = System.currentTimeMillis();
        List<Cookie> result = new ArrayList<>();
        for (Cookie cookie : hostCookies.values()) {
            if (cookie.expiresAt() < now) {
                continue;
            }
            if (cookie.matches(url)) {
                result.add(cookie);
            }
        }
        return result;
    }
}
