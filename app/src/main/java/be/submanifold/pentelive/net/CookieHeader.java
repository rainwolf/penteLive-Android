package be.submanifold.pentelive.net;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Parses a request {@code Cookie} header value ("a=b; c=d") as {@link CookieStorage} returns it. */
final class CookieHeader {

    private CookieHeader() {
    }

    /**
     * Every {@code name=value} pair in order, duplicates included. A value keeps any '=' it
     * contains (the password2 cookie is base64). An entry without '=' is a value with an empty
     * name, as browsers treat it.
     */
    static List<String[]> pairs(String header) {
        List<String[]> pairs = new ArrayList<>();
        for (String entry : header.split(";")) {
            String trimmed = entry.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int eq = trimmed.indexOf('=');
            if (eq < 0) {
                pairs.add(new String[]{"", trimmed});
            } else {
                pairs.add(new String[]{trimmed.substring(0, eq).trim(), trimmed.substring(eq + 1).trim()});
            }
        }
        return pairs;
    }

    /** The cookies by name, in header order; for a repeated name the first (most specific) wins. */
    static Map<String, String> parse(String header) {
        Map<String, String> cookies = new LinkedHashMap<>();
        for (String[] pair : pairs(header)) {
            cookies.putIfAbsent(pair[0], pair[1]);
        }
        return cookies;
    }
}
