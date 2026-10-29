package be.submanifold.pentelive.db;

import java.util.HashMap;
import java.util.Map;

final class InMemoryKeyValueStore implements KeyValueStore {
    final Map<String, Object> map = new HashMap<>();

    @Override public String getString(String k, String d) { return map.containsKey(k) ? (String) map.get(k) : d; }
    @Override public void putString(String k, String v) { map.put(k, v); }
    @Override public int getInt(String k, int d) { return map.containsKey(k) ? (Integer) map.get(k) : d; }
    @Override public void putInt(String k, int v) { map.put(k, v); }
    @Override public boolean getBoolean(String k, boolean d) { return map.containsKey(k) ? (Boolean) map.get(k) : d; }
    @Override public void putBoolean(String k, boolean v) { map.put(k, v); }
}
