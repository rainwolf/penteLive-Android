package be.submanifold.pentelive.net;

import android.content.Context;

import be.submanifold.pentelive.PrefUtils;

import okhttp3.CookieJar;

/**
 * Production Session backed by SharedPreferences via {@link PrefUtils}.
 * Credentials are read from (and credential updates written back to) the
 * PREFS_LOGIN_USERNAME_KEY / PREFS_LOGIN_PASSWORD_KEY entries, replacing the
 * PentePlayer.mPlayerName/mPassword statics (PentePlayer.java:35-36). Cookies
 * go to the app's one shared, persistent store ({@link SharedCookies}).
 */
public final class SharedPrefsSession implements Session {

    private final Context appContext;

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
        return SharedCookies.okHttpJar();
    }

    @Override
    public void updateCredentials(String name, String password) {
        PrefUtils.saveToPrefs(appContext, PrefUtils.PREFS_LOGIN_USERNAME_KEY, name);
        PrefUtils.saveToPrefs(appContext, PrefUtils.PREFS_LOGIN_PASSWORD_KEY, password);
    }
}
