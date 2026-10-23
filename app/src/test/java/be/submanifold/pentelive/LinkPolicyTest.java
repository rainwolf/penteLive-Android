package be.submanifold.pentelive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import be.submanifold.pentelive.LinkPolicy.Action;

public class LinkPolicyTest {

    private static boolean pente(String url) {
        return LinkPolicy.isPenteUrl(url, null);
    }

    @Test
    public void penteHostsAllowed() {
        assertTrue(pente("https://pente.org/x"));
        assertTrue(pente("https://www.pente.org/gameServer/index.jsp?a=b#c"));
        assertTrue(pente("http://www.pente.org"));
        assertTrue(pente("https://static.cdn.pente.org/a"));
        assertTrue(pente("HTTPS://WWW.PENTE.ORG/x"));
        assertTrue(pente("https://Pente.Org:8443/x"));
    }

    @Test
    public void lookalikesRejected() {
        assertFalse(pente("https://evilpente.org"));
        assertFalse(pente("https://pente.org.evil.com"));
        assertFalse(pente("https://www.pente.org.evil.com/x"));
        assertFalse(pente("https://evil.com/pente.org"));
        assertFalse(pente("https://evil.com/?u=https://pente.org"));
        assertFalse(pente("https://pente.org@evil.com"));
        assertFalse(pente("https://pente.org:pw@evil.com/x"));
        assertFalse(pente("https://evil.com\\@pente.org"));
        assertFalse(pente("https://evil.com#@pente.org"));
        assertFalse(pente("https://pente.org./x"));
    }

    @Test
    public void userinfoOnPenteHostStillPente() {
        assertTrue(pente("https://user@pente.org/x"));
    }

    @Test
    public void relativeEmptyNullAndBadSchemesRejected() {
        assertFalse(pente(null));
        assertFalse(pente(""));
        assertFalse(pente("/gameServer/x"));
        assertFalse(pente("//pente.org/x"));
        assertFalse(pente("pente.org"));
        assertFalse(pente("ftp://pente.org/x"));
        assertFalse(pente("javascript://pente.org/%0aalert(1)"));
        assertFalse(pente("not a url"));
    }

    @Test
    public void devHostOnlyWhenGiven() {
        assertFalse(LinkPolicy.isPenteUrl("https://10.0.2.2/x", null));
        assertTrue(LinkPolicy.isPenteUrl("https://10.0.2.2/x", "10.0.2.2"));
        assertFalse(LinkPolicy.isPenteUrl("https://10.0.2.3/x", "10.0.2.2"));
    }

    @Test
    public void decideAdult() {
        assertEquals(Action.IN_APP, LinkPolicy.decide("https://www.pente.org/a", false, null));
        assertEquals(Action.EXTERNAL, LinkPolicy.decide("https://www.facebook.com/pente.org", false, null));
        assertEquals(Action.EXTERNAL, LinkPolicy.decide("http://evil.com", false, null));
        assertEquals(Action.EXTERNAL, LinkPolicy.decide("mailto:a@b.com", false, null));
        assertEquals(Action.EXTERNAL, LinkPolicy.decide("tel:+3212345", false, null));
    }

    @Test
    public void decideKid() {
        assertEquals(Action.IN_APP, LinkPolicy.decide("https://www.pente.org/a", true, null));
        assertEquals(Action.BLOCKED_KID, LinkPolicy.decide("https://www.facebook.com/pente.org", true, null));
        assertEquals(Action.BLOCKED_KID, LinkPolicy.decide("https://pente.org@evil.com", true, null));
        assertEquals(Action.BLOCKED_KID, LinkPolicy.decide("mailto:a@b.com", true, null));
        assertEquals(Action.BLOCKED_KID, LinkPolicy.decide("tel:123", true, null));
    }

    @Test
    public void decideBlockedSchemes() {
        String[] urls = {"javascript:alert(1)", "file:///etc/passwd", "intent://x#Intent;scheme=http;end",
                "data:text/html,hi", "market://details?id=x", "fb://facewebmodal/f", "", null, "/relative",
                "https:///nohost"};
        for (String u : urls) {
            assertEquals(u, Action.BLOCKED, LinkPolicy.decide(u, false, null));
            assertEquals(u, Action.BLOCKED, LinkPolicy.decide(u, true, null));
        }
    }

    @Test
    public void shouldTreatAsKidDerivation() {
        assertTrue(LinkPolicy.shouldTreatAsKid(false, false, false)); // logged out
        assertTrue(LinkPolicy.shouldTreatAsKid(false, true, false));  // stale flags, no login
        assertTrue(LinkPolicy.shouldTreatAsKid(true, true, true));    // logged in kid
        assertFalse(LinkPolicy.shouldTreatAsKid(true, true, false));  // known non-kid
        assertTrue(LinkPolicy.shouldTreatAsKid(true, false, false));  // status unknown
    }
}
