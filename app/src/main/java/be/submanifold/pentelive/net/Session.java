package be.submanifold.pentelive.net;

import okhttp3.CookieJar;

/**
 * Holds the authenticated identity (name/password) plus the cookie store used
 * to persist and replay the server session cookie. Backed by SharedPreferences
 * in production ({@link SharedPrefsSession}) or in-memory in tests.
 */
public interface Session {

    String name();

    String password();

    CookieJar cookieJar();

    void updateCredentials(String name, String password);
}
