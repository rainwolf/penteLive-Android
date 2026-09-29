package be.submanifold.pentelive.net;

import java.net.CookieHandler;

import okhttp3.CookieJar;

/**
 * Holds the app's single {@link CookieStorage}. {@code MyApplication.onCreate} calls
 * {@link #install} before any Activity or Service runs; from then on HttpURLConnection
 * (through the default {@link CookieHandler}), OkHttp ({@link #okHttpJar()}) and WebView all
 * read and write the same cookies.
 */
public final class SharedCookies {

    private static CookieStorage storage;

    private SharedCookies() {
    }

    public static synchronized void install(CookieStorage cookieStorage) {
        storage = cookieStorage;
        CookieHandler.setDefault(new SharedCookieHandler(cookieStorage));
    }

    /** @throws IllegalStateException if {@link #install} was never called */
    public static synchronized CookieStorage storage() {
        if (storage == null) {
            throw new IllegalStateException("SharedCookies.install was not called");
        }
        return storage;
    }

    public static CookieJar okHttpJar() {
        return new SharedCookieJar(storage());
    }
}
