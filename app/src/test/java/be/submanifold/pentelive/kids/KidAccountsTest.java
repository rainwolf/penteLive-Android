package be.submanifold.pentelive.kids;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import be.submanifold.pentelive.kids.KidAccounts.ActiveGame;
import be.submanifold.pentelive.kids.KidAccounts.ActiveGamesReply;
import be.submanifold.pentelive.kids.KidAccounts.Reply;

import org.junit.Test;

public class KidAccountsTest {

    private static final String PARENT_VIEW = "{\"success\":true,\"isKid\":false,\"emailVerified\":true,\"kids\":[{\"kidPid\":23000000012345,\"name\":\"kid_one\",\"copyMessages\":false,\"copyLiveChat\":true,\"whitelist\":[{\"pid\":23000000000777,\"name\":\"graviton\",\"nameColor\":0,\"tourneyWinner\":0}]}]}";

    @Test
    public void parse_parentView() {
        Reply r = KidAccounts.parse(PARENT_VIEW);
        assertFalse(r.isKid);
        assertTrue(r.emailVerified);
        assertEquals(23000000012345L, r.kids.get(0).kidPid);
        assertTrue(r.kids.get(0).copyLiveChat);
        assertEquals("graviton", r.kids.get(0).whitelist.get(0).name);
        assertTrue(r.whitelist.isEmpty());
    }

    @Test
    public void parse_kidView() {
        Reply r = KidAccounts.parse("{\"success\":true,\"isKid\":true,\"emailVerified\":true,\"parentName\":\"iostest\",\"whitelist\":[{\"pid\":23000000000777,\"name\":\"graviton\",\"nameColor\":0,\"tourneyWinner\":0}],\"kids\":[]}");
        assertTrue(r.isKid);
        assertEquals("iostest", r.parentName);
        assertEquals(1, r.whitelist.size());
    }

    @Test
    public void parse_missingListsBecomeEmpty() {
        Reply r = KidAccounts.parse("{\"success\":true,\"kids\":[{\"kidPid\":1,\"name\":\"k\"}]}");
        assertTrue(r.whitelist.isEmpty());
        assertTrue(r.kids.get(0).whitelist.isEmpty());
    }

    @Test
    public void parse_htmlOrNull_isNull() {
        assertNull(KidAccounts.parse("<html>login</html>"));
        assertNull(KidAccounts.parse(null));
    }

    @Test
    public void parse_failure_isNullAndErrorIsReadable() {
        String body = "{\"success\":false,\"error\":\"Kid not found\"}";
        assertNull(KidAccounts.parse(body));
        assertEquals("Kid not found", KidAccounts.parseError(body));
    }

    @Test
    public void findKid_absentIsNull() {
        Reply r = KidAccounts.parse(PARENT_VIEW);
        assertNotNull(KidAccounts.findKid(r, 23000000012345L));
        assertEquals("kid_one", KidAccounts.findKid(r, 23000000012345L).name);
        assertNull(KidAccounts.findKid(r, 5L));
    }

    @Test
    public void forms() throws Exception {
        assertEquals("action=create&kidName=kid_one&kidPassword=pw_1", KidAccounts.createForm("kid_one", "pw_1"));
        assertEquals("action=convert&kidName=kid_one", KidAccounts.convertForm("kid_one"));
        assertEquals("action=password&kidPid=5&kidPassword=abcde", KidAccounts.passwordForm(5L, "abcde"));
        assertEquals("action=whitelistAdd&kidPid=5&playerName=graviton", KidAccounts.whitelistAddForm(5L, "graviton"));
        assertEquals("action=whitelistRemove&kidPid=5&playerPid=777", KidAccounts.whitelistRemoveForm(5L, 777L));
        assertEquals("action=settings&kidPid=5&copyMessages=Y", KidAccounts.settingsForm(5L, true, null));
        assertEquals("action=settings&kidPid=5&copyLiveChat=N", KidAccounts.settingsForm(5L, null, false));
        assertEquals("action=release&kidPid=5", KidAccounts.releaseForm(5L));
    }

    @Test
    public void parseError_loginRequired401Body() {
        assertEquals("Login required", KidAccounts.parseError("{\"success\":false,\"error\":\"Login required\"}"));
        assertNull(KidAccounts.parseError("<html>login</html>"));
    }

    @Test
    public void allowedRecipients_includesParentFirst() {
        Reply r = KidAccounts.parse("{\"success\":true,\"isKid\":true,\"parentName\":\"iostest\",\"whitelist\":[{\"pid\":7,\"name\":\"graviton\"}]}");
        assertEquals(java.util.Arrays.asList("iostest", "graviton"), KidAccounts.allowedRecipients(r));
    }

    @Test
    public void allowedRecipients_parentView_isEmpty() {
        assertTrue(KidAccounts.allowedRecipients(KidAccounts.parse("{\"success\":true,\"isKid\":false}")).isEmpty());
    }

    @Test
    public void allowedRecipients_null_isEmpty() {
        assertTrue(KidAccounts.allowedRecipients(null).isEmpty());
    }

    @Test public void parse_copyTbMessages() {
        Reply r = KidAccounts.parse("{\"success\":true,\"kids\":[{\"kidPid\":5,\"name\":\"k\",\"copyMessages\":false,\"copyLiveChat\":false,\"copyTbMessages\":true}]}");
        assertEquals(Boolean.TRUE, r.kids.get(0).copyTbMessages);
    }
    @Test public void parse_missingCopyTbMessages_isNull() {
        Reply r = KidAccounts.parse("{\"success\":true,\"kids\":[{\"kidPid\":5,\"name\":\"k\"}]}");
        assertNull(r.kids.get(0).copyTbMessages);
    }
    @Test public void settingsForm_tbOnly() {
        assertEquals("action=settings&kidPid=5&copyTbMessages=Y", KidAccounts.settingsForm(5L, null, null, true));
        assertEquals("action=settings&kidPid=5&copyTbMessages=N", KidAccounts.settingsForm(5L, null, null, false));
        assertEquals("action=settings&kidPid=5&copyMessages=Y", KidAccounts.settingsForm(5L, true, null));
    }
    @Test public void parseActiveGames_contractSample() {
        ActiveGamesReply r = KidAccounts.parseActiveGames("{\"success\":true,\"kidPid\":23000000012345,\"games\":[{\"gid\":50000000001234,\"game\":51,\"gameName\":\"Pente\",\"opponentPid\":23000000000777,\"opponentName\":\"graviton\",\"kidsTurn\":true,\"lastMoveDate\":1760882580000,\"privateGame\":true},{\"gid\":50000000001235,\"game\":53,\"gameName\":\"Keryo-Pente\",\"opponentPid\":23000000000777,\"opponentName\":\"graviton\",\"kidsTurn\":false,\"lastMoveDate\":0,\"privateGame\":false}]}");
        assertEquals(23000000012345L, r.kidPid);
        assertEquals(2, r.games.size());
        ActiveGame g = r.games.get(0);
        assertEquals(50000000001234L, g.gid); assertEquals(51, g.game); assertEquals("Pente", g.gameName);
        assertEquals("graviton", g.opponentName); assertTrue(g.kidsTurn); assertEquals(1760882580000L, g.lastMoveDate); assertTrue(g.privateGame);
        assertEquals(50000000001235L, r.games.get(1).gid); // server order kept
    }
    @Test public void parseActiveGames_skipsUnknownGames() {
        ActiveGamesReply r = KidAccounts.parseActiveGames("{\"success\":true,\"kidPid\":5,\"games\":["
                + "{\"gid\":1,\"game\":51,\"gameName\":\"Pente\"},"
                + "{\"gid\":2,\"game\":83,\"gameName\":\"New Game\"},"
                + "{\"gid\":3,\"gameName\":\"Keryo-Pente\"},"
                + "{\"gid\":4,\"gameName\":\"Foo\"}]}");
        assertEquals(2, r.games.size());
        assertEquals(1L, r.games.get(0).gid);
        assertEquals(3L, r.games.get(1).gid); // no integer id: falls back to the name
    }
    @Test public void parseActiveGames_emptyAndFailures() {
        assertTrue(KidAccounts.parseActiveGames("{\"success\":true,\"kidPid\":5,\"games\":[]}").games.isEmpty());
        assertNull(KidAccounts.parseActiveGames("{\"success\":false,\"error\":\"Kid not found\"}"));
        assertNull(KidAccounts.parseActiveGames("<html>login</html>"));
        assertNull(KidAccounts.parseActiveGames(null));
    }
    @Test public void parseActiveGames_missingGames_isEmpty() {
        // an old server ignores action=activeGames and returns the plain GET body
        assertTrue(KidAccounts.parseActiveGames("{\"success\":true,\"isKid\":false,\"kids\":[]}").games.isEmpty());
    }
    @Test public void activeGamesPath() {
        assertEquals("/gameServer/kids?action=activeGames&kidPid=5", KidAccounts.activeGamesPath(5L));
    }
    @Test public void turnName() {
        ActiveGame g = new ActiveGame(); g.opponentName = "graviton";
        g.kidsTurn = true; assertEquals("kidstb1", KidAccounts.turnName(g, "kidstb1"));
        g.kidsTurn = false; assertEquals("graviton", KidAccounts.turnName(g, "kidstb1"));
    }
}
