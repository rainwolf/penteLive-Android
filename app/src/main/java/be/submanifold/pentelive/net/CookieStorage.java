package be.submanifold.pentelive.net;

/**
 * The one cookie store the app shares between HttpURLConnection (through
 * {@link SharedCookieHandler}), OkHttp (through {@link SharedCookieJar}) and WebView. The
 * production implementation is {@link WebkitCookieStorage}; everything else only sees this
 * interface, so it stays testable on the JVM.
 */
public interface CookieStorage {

    /** The cookies to send to {@code url}, as a request header value ("a=b; c=d"); "" when there are none. */
    String cookieHeader(String url);

    /** Stores one Set-Cookie header value received from {@code url}. */
    void setCookie(String url, String setCookieValue);

    /** Deletes every cookie. */
    void removeAll();

    /** Writes the cookies to persistent storage now. */
    void flush();
}
