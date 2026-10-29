package be.submanifold.pentelive.db;

import java.io.IOException;

import be.submanifold.pentelive.net.AuthedHttp;

/** Production {@link DbTransport} over {@link AuthedHttp}; 30 s timeout on every request. */
public final class AuthedDbTransport implements DbTransport {
    static final String PREFIX = "/api/db";
    static final int TIMEOUT_MS = 30_000;

    @Override
    public Reply get(String path) throws IOException {
        AuthedHttp.Reply r = AuthedHttp.shared().getAllowingErrors(PREFIX + path, TIMEOUT_MS);
        return new Reply(r.code, r.body);
    }

    @Override
    public Reply postJson(String path, String json) throws IOException {
        AuthedHttp.Reply r = AuthedHttp.shared().postJsonAllowingErrors(PREFIX + path, json);
        return new Reply(r.code, r.body);
    }
}
