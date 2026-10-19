package be.submanifold.pentelive.blocked;

import be.submanifold.pentelive.net.Json;

import com.google.gson.JsonParseException;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Pure-Java model and logic for the /gameServer/ignored endpoint.
 * No Android dependencies, so unit-testable on the JVM.
 */
public final class BlockedPlayers {

    private BlockedPlayers() {
    }

    public static final class IgnoreEntry {
        public long ignorePid;
        public String name;
        public boolean ignoreInvite;
        public boolean ignoreChat;
        /** Donor name color as the dashboard decodes it; 0 = none (also when an older server omits it). */
        public int nameColor;
        /** 0 none, 1-3 tournament crowns, above 3 King of the Hill crown; 0 when omitted. */
        public int tourneyWinner;
    }

    /** Which list the user is looking at. */
    public enum Filter { MESSAGES, INVITATIONS, BOTH }

    /** What a single entry blocks. The UI maps these to blocked_kind_* strings. */
    public enum Kind { MESSAGES, INVITATIONS, BOTH }

    public static final class Reply {
        public boolean success;
        public String error;
        public List<IgnoreEntry> ignores;
    }

    public static Kind kind(boolean invite, boolean chat) {
        if (invite && chat) {
            return Kind.BOTH;
        }
        return chat ? Kind.MESSAGES : Kind.INVITATIONS;
    }

    private static Reply parseRaw(String body) {
        if (body == null) {
            return null;
        }
        try {
            return Json.GSON.fromJson(body, Reply.class);
        } catch (JsonParseException e) {
            return null;
        }
    }

    /** The reply, or null for null, non-JSON or success:false bodies. */
    public static Reply parse(String body) {
        Reply r = parseRaw(body);
        if (r == null || !r.success) {
            return null;
        }
        if (r.ignores == null) {
            r.ignores = new ArrayList<>();
        }
        return r;
    }

    /** The server's error message, or null when absent or the body is not JSON. */
    public static String parseError(String body) {
        Reply r = parseRaw(body);
        return r == null ? null : r.error;
    }

    public static List<IgnoreEntry> filter(List<IgnoreEntry> entries, Filter filter) {
        if (entries == null) {
            return Collections.emptyList();
        }
        List<IgnoreEntry> out = new ArrayList<>();
        for (IgnoreEntry e : entries) {
            boolean keep = filter == Filter.BOTH
                    || (filter == Filter.MESSAGES && e.ignoreChat)
                    || (filter == Filter.INVITATIONS && e.ignoreInvite);
            if (keep) {
                out.add(e);
            }
        }
        return out;
    }

    /** Flags {invite, chat} the entry keeps after unblocking the given kind. */
    public static boolean[] remainingFlags(IgnoreEntry e, Filter unblocking) {
        boolean invite = e.ignoreInvite && unblocking == Filter.MESSAGES;
        boolean chat = e.ignoreChat && unblocking == Filter.INVITATIONS;
        return new boolean[] {invite, chat};
    }

    public static boolean isChatBlocked(String name, List<IgnoreEntry> entries) {
        if (name == null || entries == null) {
            return false;
        }
        for (IgnoreEntry e : entries) {
            if (e.ignoreChat && e.name != null && e.name.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    /** key=value pairs joined by &, UTF-8 percent-encoded with spaces as %20. */
    public static String formEncode(String... keyValues) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(encode(keyValues[i])).append('=').append(encode(keyValues[i + 1]));
        }
        return sb.toString();
    }

    private static String encode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
