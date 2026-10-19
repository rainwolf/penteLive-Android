package be.submanifold.pentelive.liveGameRoom;

import java.util.Map;

/** Kid-account helpers for the native live room. */
final class KidLiveRoom {
    private KidLiveRoom() {
    }

    /** True iff the login event's "me" object carries a numeric parentPid (kid account). */
    static boolean isKidLogin(Map<String, ?> loginEvent) {
        if (loginEvent == null) {
            return false;
        }
        Object me = loginEvent.get("me");
        return me instanceof Map && ((Map<?, ?>) me).get("parentPid") instanceof Number;
    }

    /** ": \"text\"" for non-empty text, otherwise "". */
    static String quotedSuffix(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return ": \"" + text + "\"";
    }
}
