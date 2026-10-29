package be.submanifold.pentelive.db;

/** Tiny typed key-value seam so filter persistence is JVM-testable. */
public interface KeyValueStore {
    String getString(String key, String defaultValue);

    void putString(String key, String value);

    int getInt(String key, int defaultValue);

    void putInt(String key, int value);

    boolean getBoolean(String key, boolean defaultValue);

    void putBoolean(String key, boolean value);
}
