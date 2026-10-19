package be.submanifold.pentelive.kids;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
}
