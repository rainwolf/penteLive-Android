package be.submanifold.pentelive.kids;

import be.submanifold.pente.rules.Variants;
import be.submanifold.pentelive.net.Forms;
import be.submanifold.pentelive.net.Json;

import com.google.gson.JsonParseException;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure-Java model, parser and form builders for the /gameServer/kids endpoint.
 * No Android dependencies, so unit-testable on the JVM.
 */
public final class KidAccounts {

    private KidAccounts() {
    }

    public static final class Player {
        public long pid;
        public String name;
        /** Donor name color as the dashboard decodes it; 0 = none. */
        public int nameColor;
        /** 0 none, 1-3 tournament crowns, above 3 King of the Hill crown. */
        public int tourneyWinner;
    }

    public static final class Kid {
        public long kidPid;
        public String name;
        public boolean copyMessages;
        public boolean copyLiveChat;
        /** Boxed: a missing field stays null, meaning the server lacks the feature (switch hidden). */
        public Boolean copyTbMessages;
        public List<Player> whitelist;
    }

    public static final class ActiveGame {
        public long gid;
        public int game;
        public String gameName;
        public long opponentPid;
        public String opponentName;
        public boolean kidsTurn;
        public long lastMoveDate;
        public boolean privateGame;
    }

    public static final class ActiveGamesReply {
        public boolean success;
        public String error;
        public long kidPid;
        public List<ActiveGame> games;
    }

    public static final class Reply {
        public boolean success;
        public String error;
        public String message;
        public boolean isKid;
        public boolean emailVerified;
        public String parentName;
        public List<Player> whitelist;
        public List<Kid> kids;
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
        if (r.whitelist == null) {
            r.whitelist = new ArrayList<>();
        }
        if (r.kids == null) {
            r.kids = new ArrayList<>();
        }
        for (Kid kid : r.kids) {
            if (kid.whitelist == null) {
                kid.whitelist = new ArrayList<>();
            }
        }
        return r;
    }

    /**
     * The active-games reply, or null for null, non-JSON or success:false bodies.
     * A missing games list becomes empty; the server's order is kept.
     */
    public static ActiveGamesReply parseActiveGames(String body) {
        if (body == null) {
            return null;
        }
        ActiveGamesReply r;
        try {
            r = Json.GSON.fromJson(body, ActiveGamesReply.class);
        } catch (JsonParseException e) {
            return null;
        }
        if (r == null || !r.success) {
            return null;
        }
        if (r.games == null) {
            r.games = new ArrayList<>();
        }
        // Skip games this app does not know (e.g. experimental ids): prefer the integer id, else the name.
        List<ActiveGame> known = new ArrayList<>();
        for (ActiveGame g : r.games) {
            if (g == null) {
                continue;
            }
            boolean ok = g.game != 0 ? Variants.isKnownGameId(g.game) : Variants.isKnownGameType(g.gameName);
            if (ok) {
                known.add(g);
            }
        }
        r.games = known;
        return r;
    }

    public static String activeGamesPath(long kidPid) {
        return "/gameServer/kids?action=activeGames&kidPid=" + kidPid;
    }

    /** Whose turn it is: the kid's name when it is the kid's turn, else the opponent's. */
    public static String turnName(ActiveGame g, String kidName) {
        return g.kidsTurn ? kidName : g.opponentName;
    }

    /** The server's error message, or null when absent or the body is not JSON. */
    public static String parseError(String body) {
        Reply r = parseRaw(body);
        return r == null ? null : r.error;
    }

    /** The kid with {@code kidPid}, or null when absent (e.g. after release). */
    public static Kid findKid(Reply r, long kidPid) {
        if (r == null || r.kids == null) {
            return null;
        }
        for (Kid kid : r.kids) {
            if (kid.kidPid == kidPid) {
                return kid;
            }
        }
        return null;
    }

    /**
     * Composer autocomplete suggestions for a kid: parent first, then the whitelist.
     * Never used to refuse a send; the server decides.
     */
    public static List<String> allowedRecipients(Reply r) {
        List<String> names = new ArrayList<>();
        if (r == null || !r.isKid) {
            return names;
        }
        if (r.parentName != null) {
            names.add(r.parentName);
        }
        if (r.whitelist != null) {
            for (Player p : r.whitelist) {
                names.add(p.name);
            }
        }
        return names;
    }

    public static String createForm(String kidName, String kidPassword) {
        return form("action", "create", "kidName", kidName, "kidPassword", kidPassword);
    }

    public static String convertForm(String kidName) {
        return form("action", "convert", "kidName", kidName);
    }

    public static String passwordForm(long kidPid, String kidPassword) {
        return form("action", "password", "kidPid", Long.toString(kidPid), "kidPassword", kidPassword);
    }

    public static String whitelistAddForm(long kidPid, String playerName) {
        return form("action", "whitelistAdd", "kidPid", Long.toString(kidPid), "playerName", playerName);
    }

    public static String whitelistRemoveForm(long kidPid, long playerPid) {
        return form("action", "whitelistRemove", "kidPid", Long.toString(kidPid),
                "playerPid", Long.toString(playerPid));
    }

    /** A null flag is omitted; true and false become Y and N. */
    public static String settingsForm(long kidPid, Boolean copyMessages, Boolean copyLiveChat) {
        return settingsForm(kidPid, copyMessages, copyLiveChat, null);
    }

    /** As the 3-arg form, plus copyTbMessages after the other two when non-null. */
    public static String settingsForm(long kidPid, Boolean copyMessages, Boolean copyLiveChat,
            Boolean copyTbMessages) {
        List<String> kv = new ArrayList<>();
        kv.add("action");
        kv.add("settings");
        kv.add("kidPid");
        kv.add(Long.toString(kidPid));
        if (copyMessages != null) {
            kv.add("copyMessages");
            kv.add(copyMessages ? "Y" : "N");
        }
        if (copyLiveChat != null) {
            kv.add("copyLiveChat");
            kv.add(copyLiveChat ? "Y" : "N");
        }
        if (copyTbMessages != null) {
            kv.add("copyTbMessages");
            kv.add(copyTbMessages ? "Y" : "N");
        }
        return form(kv.toArray(new String[0]));
    }

    public static String releaseForm(long kidPid) {
        return form("action", "release", "kidPid", Long.toString(kidPid));
    }

    private static String form(String... namesAndValues) {
        try {
            return Forms.encode(namesAndValues);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
