package be.submanifold.pentelive.net;

import java.io.IOException;

/**
 * The server still answered "not logged in" after one re-login and one retry. The message
 * names the request path only, never its query or body.
 */
public final class NotLoggedInException extends IOException {

    public NotLoggedInException(String message) {
        super(message);
    }
}
