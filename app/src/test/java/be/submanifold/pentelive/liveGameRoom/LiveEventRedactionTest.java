package be.submanifold.pentelive.liveGameRoom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LiveEventRedactionTest {

    private static Map<String, Object> map(Object... keysAndValues) {
        Map<String, Object> m = new HashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            m.put((String) keysAndValues[i], keysAndValues[i + 1]);
        }
        return m;
    }

    @Test
    public void loginEchoPasswords_areRedactedAtEveryDepth() {
        // Shape of the server's dsgLoginEvent echo: the plaintext login password, and the
        // player's data (with the stored password) under "me".
        Map<String, Object> me = map("name", "alice", "password", "stored-secret");
        Map<String, Object> login = map("player", "alice", "password", "plain-secret",
                "guest", false, "me", me);
        Map<String, Object> event = map("dsgLoginEvent", login);

        Object redacted = LiveEventRedaction.withPasswordsRedacted(event);

        String printed = String.valueOf(redacted);
        assertFalse(printed, printed.contains("plain-secret"));
        assertFalse(printed, printed.contains("stored-secret"));
        Map<?, ?> redactedLogin = (Map<?, ?>) ((Map<?, ?>) redacted).get("dsgLoginEvent");
        assertEquals("<redacted>", redactedLogin.get("password"));
        assertEquals("<redacted>", ((Map<?, ?>) redactedLogin.get("me")).get("password"));
        assertEquals("alice", redactedLogin.get("player"));
        assertEquals(false, redactedLogin.get("guest"));
        assertEquals("alice", ((Map<?, ?>) redactedLogin.get("me")).get("name"));
    }

    @Test
    public void passwordsInsideLists_areRedacted() {
        List<Object> players = new ArrayList<>(Arrays.asList(
                map("name", "bob", "password", "bob-secret"), "text", 3));
        Map<String, Object> event = map("dsgSomeEvent", map("players", players));

        Object redacted = LiveEventRedaction.withPasswordsRedacted(event);

        List<?> redactedPlayers = (List<?>) ((Map<?, ?>) ((Map<?, ?>) redacted).get("dsgSomeEvent")).get("players");
        assertEquals("<redacted>", ((Map<?, ?>) redactedPlayers.get(0)).get("password"));
        assertEquals("bob", ((Map<?, ?>) redactedPlayers.get(0)).get("name"));
        assertEquals("text", redactedPlayers.get(1));
        assertEquals(3, redactedPlayers.get(2));
    }

    @Test
    public void theEventItselfIsNotModified() {
        Map<String, Object> login = map("player", "alice", "password", "plain-secret");
        Map<String, Object> event = map("dsgLoginEvent", login);

        LiveEventRedaction.withPasswordsRedacted(event);

        assertEquals("plain-secret", login.get("password"));
    }

    @Test
    public void eventsWithoutPasswords_printTheSame() {
        Map<String, Object> event = map("dsgTextMainRoomEvent", map("player", "carol", "text", "hi"));

        assertEquals(event, LiveEventRedaction.withPasswordsRedacted(event));
    }
}
