package be.submanifold.pentelive.net;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Test-only in-memory {@link CookieStorage}: cookies keyed by host, then by name. Domain and
 * path attributes are ignored; {@code Max-Age=0} deletes the cookie. Every call is recorded in
 * {@link #events} so tests can check ordering.
 */
final class FakeCookieStorage implements CookieStorage {

    final Map<String, Map<String, String>> byHost = new LinkedHashMap<>();
    final List<String> events = new ArrayList<>();
    /** Raw (url, Set-Cookie value) pairs, in arrival order. */
    final List<String[]> setCalls = new ArrayList<>();
    /** Every url passed to {@link #cookieHeader}, in call order. */
    final List<String> headerCalls = new ArrayList<>();

    @Override
    public synchronized String cookieHeader(String url) {
        headerCalls.add(url);
        Map<String, String> cookies = byHost.get(URI.create(url).getHost());
        if (cookies == null) {
            return "";
        }
        StringBuilder header = new StringBuilder();
        for (Map.Entry<String, String> c : cookies.entrySet()) {
            if (header.length() > 0) {
                header.append("; ");
            }
            header.append(c.getKey()).append('=').append(c.getValue());
        }
        return header.toString();
    }

    @Override
    public synchronized void setCookie(String url, String setCookieValue) {
        events.add("set");
        setCalls.add(new String[]{url, setCookieValue});
        String[] parts = setCookieValue.split(";");
        String pair = parts[0].trim();
        int eq = pair.indexOf('=');
        String name = pair.substring(0, eq);
        String value = pair.substring(eq + 1);
        Map<String, String> cookies =
                byHost.computeIfAbsent(URI.create(url).getHost(), h -> new LinkedHashMap<>());
        for (int i = 1; i < parts.length; i++) {
            if (parts[i].trim().toLowerCase(Locale.ROOT).equals("max-age=0")) {
                cookies.remove(name);
                return;
            }
        }
        cookies.put(name, value);
    }

    @Override
    public synchronized void removeAll() {
        events.add("removeAll");
        byHost.clear();
    }

    @Override
    public synchronized void flush() {
        events.add("flush");
    }

    synchronized void put(String host, String name, String value) {
        byHost.computeIfAbsent(host, h -> new LinkedHashMap<>()).put(name, value);
    }
}
