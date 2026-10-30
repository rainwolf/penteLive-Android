package be.submanifold.pentelive.db;

import java.io.IOException;

/** Seam between {@link DbApi} and the network. Paths are relative to /api/db. */
public interface DbTransport {
    final class Reply {
        public final int code;
        public final String body;

        public Reply(int code, String body) {
            this.code = code;
            this.body = body;
        }
    }

    Reply get(String path) throws IOException;

    Reply postJson(String path, String json) throws IOException;
}
