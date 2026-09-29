package be.submanifold.pentelive.net;

import java.net.CookieHandler;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * The process-wide {@link CookieHandler} for HttpURLConnection, backed by the shared
 * {@link CookieStorage}. Installed by {@link SharedCookies#install}, so every
 * HttpURLConnection request sends and stores the same cookies as OkHttp and WebView.
 */
public final class SharedCookieHandler extends CookieHandler {

    private final CookieStorage storage;

    public SharedCookieHandler(CookieStorage storage) {
        this.storage = storage;
    }

    @Override
    public Map<String, List<String>> get(URI uri, Map<String, List<String>> requestHeaders) {
        String header = storage.cookieHeader(uri.toString());
        if (header.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.singletonMap("Cookie", Collections.singletonList(header));
    }

    @Override
    public void put(URI uri, Map<String, List<String>> responseHeaders) {
        for (Map.Entry<String, List<String>> header : responseHeaders.entrySet()) {
            // HttpURLConnection lists the status line under a null key; equalsIgnoreCase(null) is false.
            if ("Set-Cookie".equalsIgnoreCase(header.getKey())) {
                for (String value : header.getValue()) {
                    storage.setCookie(uri.toString(), value);
                }
            }
        }
    }
}
