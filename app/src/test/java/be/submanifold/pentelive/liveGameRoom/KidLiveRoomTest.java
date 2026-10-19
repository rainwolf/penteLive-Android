package be.submanifold.pentelive.liveGameRoom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class KidLiveRoomTest {
    @Test public void kidLogin_hasMeParentPid() {
        Map<String, Object> me = new HashMap<>(); me.put("parentPid", 23000000012345L);
        Map<String, Object> login = new HashMap<>(); login.put("me", me);
        assertTrue(KidLiveRoom.isKidLogin(login));
    }
    @Test public void normalLogin_noParentPid() {
        Map<String, Object> login = new HashMap<>(); login.put("me", new HashMap<String, Object>());
        assertFalse(KidLiveRoom.isKidLogin(login));
    }
    @Test public void oldServerOrGuest_noMe() {
        assertFalse(KidLiveRoom.isKidLogin(new HashMap<String, Object>()));
        assertFalse(KidLiveRoom.isKidLogin(null));
    }
    @Test public void parentPidAsInteger_stillKid() {
        Map<String, Object> me = new HashMap<>(); me.put("parentPid", 5);
        Map<String, Object> login = new HashMap<>(); login.put("me", me);
        assertTrue(KidLiveRoom.isKidLogin(login));
    }
    @Test public void quotedSuffix_text() { assertEquals(": \"hi\"", KidLiveRoom.quotedSuffix("hi")); }
    @Test public void quotedSuffix_emptyOrNull_isEmpty() {
        assertEquals("", KidLiveRoom.quotedSuffix("")); assertEquals("", KidLiveRoom.quotedSuffix(null));
    }
}
