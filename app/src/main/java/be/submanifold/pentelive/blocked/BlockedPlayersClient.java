package be.submanifold.pentelive.blocked;

import be.submanifold.pentelive.RedactingLog;
import be.submanifold.pentelive.blocked.BlockedPlayers.IgnoreEntry;
import be.submanifold.pentelive.net.AuthedHttp;

import java.io.IOException;
import java.util.List;

/** Blocking calls to /gameServer/ignored; run inside a BackgroundTask. */
public final class BlockedPlayersClient {

    private static final String TAG = "BlockedPlayersClient";
    private static final String PATH = "/gameServer/ignored";

    private BlockedPlayersClient() {
    }

    /** Failure whose message is the server's error text, or null when none was given. */
    public static final class Failure extends IOException {
        public Failure(String serverError) {
            super(serverError);
        }
    }

    public static List<IgnoreEntry> load() throws IOException {
        return result(AuthedHttp.shared().get(PATH));
    }

    public static List<IgnoreEntry> block(String name, boolean invite, boolean chat) throws IOException {
        return result(post(BlockedPlayers.formEncode(
                "action", "add",
                "ignoreName", name,
                "ignoreInvite", invite ? "Y" : "N",
                "ignoreChat", chat ? "Y" : "N")));
    }

    /** Sets the flags of an existing entry; both false lifts the block. */
    public static List<IgnoreEntry> update(long pid, boolean invite, boolean chat) throws IOException {
        return result(post(BlockedPlayers.formEncode(
                "action", "update",
                "ignorePid", Long.toString(pid),
                "ignoreInvite", invite ? "Y" : "N",
                "ignoreChat", chat ? "Y" : "N")));
    }

    private static AuthedHttp.Reply post(String form) throws IOException {
        return AuthedHttp.shared().postForm(PATH, form);
    }

    private static List<IgnoreEntry> result(AuthedHttp.Reply reply) throws IOException {
        BlockedPlayers.Reply parsed = reply.code == 200 ? BlockedPlayers.parse(reply.body) : null;
        if (parsed == null) {
            RedactingLog.e(TAG, "ignore list request failed, response code " + reply.code, null);
            throw new Failure(BlockedPlayers.parseError(reply.body));
        }
        return parsed.ignores;
    }
}
