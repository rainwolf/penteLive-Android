package be.submanifold.pentelive.kids;

import be.submanifold.pentelive.RedactingLog;
import be.submanifold.pentelive.net.AuthedHttp;

import java.io.IOException;

/** Blocking calls to /gameServer/kids; run inside a BackgroundTask. */
public final class KidAccountsClient {

    private static final String TAG = "KidAccountsClient";
    private static final String PATH = "/gameServer/kids";

    private KidAccountsClient() {
    }

    /** Failure whose message is the server's error text, or null when none was given. */
    public static final class Failure extends IOException {
        public Failure(String serverError) {
            super(serverError);
        }
    }

    public static KidAccounts.Reply load() throws IOException {
        return result(AuthedHttp.shared().getAllowingErrors(PATH));
    }

    public static KidAccounts.Reply create(String kidName, String kidPassword) throws IOException {
        return result(post(KidAccounts.createForm(kidName, kidPassword)));
    }

    public static KidAccounts.Reply convert(String kidName) throws IOException {
        return result(post(KidAccounts.convertForm(kidName)));
    }

    public static KidAccounts.Reply setPassword(long kidPid, String kidPassword) throws IOException {
        return result(post(KidAccounts.passwordForm(kidPid, kidPassword)));
    }

    public static KidAccounts.Reply whitelistAdd(long kidPid, String playerName) throws IOException {
        return result(post(KidAccounts.whitelistAddForm(kidPid, playerName)));
    }

    public static KidAccounts.Reply whitelistRemove(long kidPid, long playerPid) throws IOException {
        return result(post(KidAccounts.whitelistRemoveForm(kidPid, playerPid)));
    }

    public static KidAccounts.Reply settings(long kidPid, Boolean copyMessages, Boolean copyLiveChat)
            throws IOException {
        return settings(kidPid, copyMessages, copyLiveChat, null);
    }

    public static KidAccounts.Reply settings(long kidPid, Boolean copyMessages, Boolean copyLiveChat,
            Boolean copyTbMessages) throws IOException {
        return result(post(KidAccounts.settingsForm(kidPid, copyMessages, copyLiveChat, copyTbMessages)));
    }

    public static KidAccounts.ActiveGamesReply activeGames(long kidPid) throws IOException {
        AuthedHttp.Reply reply = AuthedHttp.shared().getAllowingErrors(KidAccounts.activeGamesPath(kidPid));
        KidAccounts.ActiveGamesReply parsed = reply.code == 200 ? KidAccounts.parseActiveGames(reply.body) : null;
        if (parsed == null) {
            RedactingLog.e(TAG, "kids activeGames failed, response code " + reply.code, null);
            throw new Failure(KidAccounts.parseError(reply.body));
        }
        return parsed;
    }

    public static KidAccounts.Reply release(long kidPid) throws IOException {
        return result(post(KidAccounts.releaseForm(kidPid)));
    }

    private static AuthedHttp.Reply post(String form) throws IOException {
        return AuthedHttp.shared().postFormAllowingErrors(PATH, form);
    }

    private static KidAccounts.Reply result(AuthedHttp.Reply reply) throws IOException {
        KidAccounts.Reply parsed = reply.code == 200 ? KidAccounts.parse(reply.body) : null;
        if (parsed == null) {
            RedactingLog.e(TAG, "kids request failed, response code " + reply.code, null);
            throw new Failure(KidAccounts.parseError(reply.body));
        }
        return parsed;
    }
}
