package be.submanifold.pentelive.net;

import java.io.IOException;

/**
 * A re-login with the stored credentials did not succeed (wrong password, or a page that
 * does not confirm the login). The message never carries the credentials.
 */
public final class LoginRejectedException extends IOException {

    public LoginRejectedException(String message) {
        super(message);
    }
}
