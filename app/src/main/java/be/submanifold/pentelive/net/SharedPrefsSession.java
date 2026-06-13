package be.submanifold.pentelive.net;

import android.content.Context;

import be.submanifold.pentelive.PrefUtils;

import okhttp3.CookieJar;

/**
 * Production Session backed by SharedPreferences via {@link PrefUtils}.
 * Credentials are read from (and credential updates written back to) the
 * PREFS_LOGIN_USERNAME_KEY / PREFS_LOGIN_PASSWORD_KEY entries, replacing the
 * PentePlayer.mPlayerName/mPassword statics (PentePlayer.java:35-36). The
 * cookie store lives in memory for the process lifetime.
 */
public final class SharedPrefsSession implements Session {

    private final Context appContext;
    private final InMemoryCookieJar cookieJar = new InMemoryCookieJar();

    public SharedPrefsSession(Context context) {
        this.appContext = context.getApplicationContext();
    }

    @Override
    public String name() {
        return PrefUtils.getFromPrefs(appContext, PrefUtils.PREFS_LOGIN_USERNAME_KEY, "");
    }

    @Override
    public String password() {
        return PrefUtils.getFromPrefs(appContext, PrefUtils.PREFS_LOGIN_PASSWORD_KEY, "");
    }

    @Override
    public CookieJar cookieJar() {
        return cookieJar;
    }

    @Override
    public void updateCredentials(String name, String password) {
        PrefUtils.saveToPrefs(appContext, PrefUtils.PREFS_LOGIN_USERNAME_KEY, name);
        PrefUtils.saveToPrefs(appContext, PrefUtils.PREFS_LOGIN_PASSWORD_KEY, password);
    }
}
