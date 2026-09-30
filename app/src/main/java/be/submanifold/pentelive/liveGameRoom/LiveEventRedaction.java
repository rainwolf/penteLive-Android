package be.submanifold.pentelive.liveGameRoom;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Copies of decoded live game room events that are safe to print: the server echoes the login
 * password in dsgLoginEvent (and dsgLoginErrorEvent), and player data can carry the stored one.
 */
final class LiveEventRedaction {

    private static final String REDACTED = "<redacted>";

    private LiveEventRedaction() {
    }

    /**
     * A copy of {@code value} (a decoded event: maps, lists and plain values) with the value of
     * every "password" key, at any depth, replaced by "&lt;redacted&gt;". {@code value} is not
     * modified.
     */
    static Object withPasswordsRedacted(Object value) {
        if (value instanceof Map) {
            Map<Object, Object> copy = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                copy.put(entry.getKey(), "password".equals(entry.getKey())
                        ? REDACTED
                        : withPasswordsRedacted(entry.getValue()));
            }
            return copy;
        }
        if (value instanceof List) {
            List<Object> copy = new ArrayList<>();
            for (Object element : (List<?>) value) {
                copy.add(withPasswordsRedacted(element));
            }
            return copy;
        }
        return value;
    }
}
