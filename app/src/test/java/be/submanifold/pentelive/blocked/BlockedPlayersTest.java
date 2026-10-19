package be.submanifold.pentelive.blocked;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import be.submanifold.pentelive.blocked.BlockedPlayers.Filter;
import be.submanifold.pentelive.blocked.BlockedPlayers.IgnoreEntry;
import be.submanifold.pentelive.blocked.BlockedPlayers.Kind;
import be.submanifold.pentelive.blocked.BlockedPlayers.Reply;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class BlockedPlayersTest {

    private static IgnoreEntry entry(long pid, String name, boolean invite, boolean chat) {
        IgnoreEntry e = new IgnoreEntry();
        e.ignorePid = pid;
        e.name = name;
        e.ignoreInvite = invite;
        e.ignoreChat = chat;
        return e;
    }

    @Test
    public void parseValid() {
        Reply r = BlockedPlayers.parse("{\"success\":true,\"ignores\":[{\"ignorePid\":5,\"name\":\"graviton\",\"ignoreInvite\":true,\"ignoreChat\":false}]}");
        assertNotNull(r);
        assertEquals(1, r.ignores.size());
        assertEquals(5L, r.ignores.get(0).ignorePid);
        assertEquals("graviton", r.ignores.get(0).name);
    }

    @Test
    public void parseHtmlLoginPageIsNull() {
        assertNull(BlockedPlayers.parse("<html>login</html>"));
        assertNull(BlockedPlayers.parse(null));
    }

    @Test
    public void parseFailureIsNull() {
        String body = "{\"success\":false,\"error\":\"Login required\"}";
        assertNull(BlockedPlayers.parse(body));
        assertEquals("Login required", BlockedPlayers.parseError(body));
        assertNull(BlockedPlayers.parseError("<html>login</html>"));
    }

    @Test
    public void filter() {
        IgnoreEntry a = entry(1, "A", true, false);
        IgnoreEntry b = entry(2, "B", false, true);
        IgnoreEntry c = entry(3, "C", true, true);
        List<IgnoreEntry> all = Arrays.asList(a, b, c);
        assertEquals(Arrays.asList(b, c), BlockedPlayers.filter(all, Filter.MESSAGES));
        assertEquals(Arrays.asList(a, c), BlockedPlayers.filter(all, Filter.INVITATIONS));
        assertEquals(Arrays.asList(a, b, c), BlockedPlayers.filter(all, Filter.BOTH));
    }

    @Test
    public void remainingFlags() {
        IgnoreEntry c = entry(3, "C", true, true);
        assertArrayEquals(new boolean[] {true, false}, BlockedPlayers.remainingFlags(c, Filter.MESSAGES));
        assertArrayEquals(new boolean[] {false, false}, BlockedPlayers.remainingFlags(c, Filter.BOTH));
    }

    @Test
    public void kind() {
        assertEquals(Kind.MESSAGES, BlockedPlayers.kind(false, true));
        assertEquals(Kind.INVITATIONS, BlockedPlayers.kind(true, false));
        assertEquals(Kind.BOTH, BlockedPlayers.kind(true, true));
    }

    @Test
    public void isChatBlockedCaseInsensitive() {
        assertTrue(BlockedPlayers.isChatBlocked("Graviton", Arrays.asList(entry(5, "graviton", false, true))));
        assertFalse(BlockedPlayers.isChatBlocked("Graviton", Arrays.asList(entry(5, "graviton", true, false))));
    }

    @Test
    public void formEncodeSpecialChars() {
        assertEquals("ignoreName=a%20b%2Bc%26d", BlockedPlayers.formEncode("ignoreName", "a b+c&d"));
    }
}
