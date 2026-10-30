package be.submanifold.pentelive.db;

import android.content.Context;

import be.submanifold.pentelive.PrefUtils;

/** {@link KeyValueStore} over the app's default shared preferences (same file as the legacy PREFS_DB* keys). */
public final class SharedPrefsKeyValueStore implements KeyValueStore {
    private final Context context;

    public SharedPrefsKeyValueStore(Context context) {
        this.context = context.getApplicationContext() != null ? context.getApplicationContext() : context;
    }

    @Override public String getString(String key, String defaultValue) {
        return PrefUtils.getFromPrefs(context, key, defaultValue);
    }

    @Override public void putString(String key, String value) {
        PrefUtils.saveToPrefs(context, key, value);
    }

    @Override public int getInt(String key, int defaultValue) {
        return PrefUtils.getIntFromPrefs(context, key, defaultValue);
    }

    @Override public void putInt(String key, int value) {
        PrefUtils.saveIntToPrefs(context, key, value);
    }

    @Override public boolean getBoolean(String key, boolean defaultValue) {
        return PrefUtils.getBooleanFromPrefs(context, key, defaultValue);
    }

    @Override public void putBoolean(String key, boolean value) {
        PrefUtils.saveBooleanToPrefs(context, key, value);
    }
}
