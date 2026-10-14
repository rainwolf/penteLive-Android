package be.submanifold.pentelive.billing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class StoreInfoTest {

    @Test
    public void parsesFullReply() {
        StoreInfo info = StoreInfo.parse("{\"accountId\":\"22.0123456789abcdef0123456789abcdef01234567\","
                + "\"vacationAllowed\":true,\"subscribedUntil\":1791763200000,\"autoRenewing\":true}");
        assertEquals("22.0123456789abcdef0123456789abcdef01234567", info.accountId);
        assertTrue(info.vacationAllowed);
        assertEquals(Long.valueOf(1791763200000L), info.subscribedUntil);
        assertTrue(info.autoRenewing);
    }

    @Test
    public void nullSubscribedUntilAndMissingBooleans() {
        StoreInfo info = StoreInfo.parse("{\"accountId\":\"7.abc\",\"subscribedUntil\":null}");
        assertEquals("7.abc", info.accountId);
        assertNull(info.subscribedUntil);
        assertFalse(info.vacationAllowed);
        assertFalse(info.autoRenewing);
    }

    @Test
    public void rejectsBodiesWithoutUsableAccountId() {
        assertNull(StoreInfo.parse(null));
        assertNull(StoreInfo.parse(""));
        assertNull(StoreInfo.parse("{}"));
        assertNull(StoreInfo.parse("{\"accountId\":\"   \"}"));
        assertNull(StoreInfo.parse("{\"accountId\":\"" + new String(new char[65]).replace('\0', 'a') + "\"}"));
        assertNull(StoreInfo.parse("<html><body>Please log in</body></html>"));
        assertNull(StoreInfo.parse("{\"accountId\":"));
        assertNull(StoreInfo.parse("[1,2]"));
    }

    @Test
    public void acceptsSixtyFourCharacterAccountId() {
        String id = new String(new char[64]).replace('\0', 'a');
        assertEquals(id, StoreInfo.parse("{\"accountId\":\"" + id + "\"}").accountId);
    }
}
