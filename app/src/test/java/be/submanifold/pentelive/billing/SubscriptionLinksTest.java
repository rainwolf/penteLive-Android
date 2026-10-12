package be.submanifold.pentelive.billing;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SubscriptionLinksTest {

    @Test
    public void matchesBothHostsAndBothPaths() {
        assertTrue(SubscriptionLinks.isSubscriptionPage("https://www.pente.org/gameServer/subscriptions", null));
        assertTrue(SubscriptionLinks.isSubscriptionPage("https://pente.org/gameServer/subscriptions", null));
        assertTrue(SubscriptionLinks.isSubscriptionPage("https://www.pente.org/gameServer/subscribe.jsp", null));
        assertTrue(SubscriptionLinks.isSubscriptionPage("http://pente.org/gameServer/subscribe.jsp", null));
    }

    @Test
    public void toleratesCaseQueryFragmentSlashAndSessionId() {
        assertTrue(SubscriptionLinks.isSubscriptionPage("https://WWW.Pente.ORG/gameServer/subscriptions?giftee=bob", null));
        assertTrue(SubscriptionLinks.isSubscriptionPage("https://pente.org/gameServer/subscriptions/#top", null));
        assertTrue(SubscriptionLinks.isSubscriptionPage("https://www.pente.org/gameServer/subscriptions;jsessionid=ABC123", null));
        assertTrue(SubscriptionLinks.isSubscriptionPage("  https://www.pente.org/gameServer/subscribe.jsp  ", null));
    }

    @Test
    public void matchesDevelopmentHostOnlyWhenGiven() {
        assertTrue(SubscriptionLinks.isSubscriptionPage("https://10.0.2.2/gameServer/subscriptions", "10.0.2.2"));
        assertFalse(SubscriptionLinks.isSubscriptionPage("https://10.0.2.2/gameServer/subscriptions", null));
    }

    @Test
    public void rejectsOtherPagesHostsAndSchemes() {
        assertFalse(SubscriptionLinks.isSubscriptionPage("https://www.pente.org/gameServer/stairs.jsp?game=1", null));
        assertFalse(SubscriptionLinks.isSubscriptionPage("https://www.pente.org/gameServer/subscriptionsX", null));
        assertFalse(SubscriptionLinks.isSubscriptionPage("https://www.pente.org/gameserver/subscriptions", null));
        assertFalse(SubscriptionLinks.isSubscriptionPage("https://evilpente.org/gameServer/subscriptions", null));
        assertFalse(SubscriptionLinks.isSubscriptionPage("https://pente.org.evil.com/gameServer/subscriptions", null));
        assertFalse(SubscriptionLinks.isSubscriptionPage("https://www.paypal.com/cgi-bin/webscr", null));
        assertFalse(SubscriptionLinks.isSubscriptionPage("javascript:alert(1)", null));
        assertFalse(SubscriptionLinks.isSubscriptionPage("not a url", null));
        assertFalse(SubscriptionLinks.isSubscriptionPage("", null));
        assertFalse(SubscriptionLinks.isSubscriptionPage(null, null));
    }
}
