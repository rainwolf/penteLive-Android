package be.submanifold.pentelive.net;

/**
 * Reads the page login.jsp returns. It answers 200 with its login form in every case, so the
 * status code says nothing: a failed login prints the invalid-credentials message, and a
 * successful one renders the logged-in tabs, whose Logout link is the positive success marker.
 */
public final class LoginResponse {

    public enum Outcome { SUCCESS, INVALID_CREDENTIALS, UNEXPECTED }

    /** login.jsp prints this when LoginFilter rejected the name/password. */
    static final String INVALID_MARKER = "Invalid name or password, please try again.";
    /** Rendered by top.jsp from begin.jsp's loggedInTabs, only for a logged-in request. */
    static final String LOGGED_IN_MARKER = "href=\"/gameServer/logout\"";

    private LoginResponse() {
    }

    /**
     * @return {@link Outcome#UNEXPECTED} for anything that is neither marker, such as the
     *         speed-conversion message or an empty body; callers treat that as a failure.
     */
    public static Outcome classify(String body) {
        if (body.contains(INVALID_MARKER)) {
            return Outcome.INVALID_CREDENTIALS;
        }
        if (body.contains(LOGGED_IN_MARKER)) {
            return Outcome.SUCCESS;
        }
        return Outcome.UNEXPECTED;
    }
}
