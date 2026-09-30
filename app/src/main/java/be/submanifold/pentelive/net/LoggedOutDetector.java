package be.submanifold.pentelive.net;

/**
 * Recognises the server's "you are not logged in" answers. The endpoints differ (see
 * LoginFilter, SimpleLoginAccessController and the mobile JSPs), and none of them uses an
 * HTTP error status for it.
 */
final class LoggedOutDetector {

    private LoggedOutDetector() {
    }

    /**
     * @param requestPath the path that was requested
     * @param finalPath   the path of the final response, after any followed redirect
     * @param body        the final response body
     */
    static boolean isLoggedOut(String requestPath, String finalPath, String body) {
        // mobile/json/index.jsp's JSON error, mobile/index.jsp, and a failed cookie login.
        return body.contains(LoginResponse.INVALID_MARKER)
                // LoginFilter forwards a login-required URI (/tb/*, mymessages) to login.jsp.
                || body.contains("name=\"login_form\"")
                // error.jsp, from the social, broadcast and notification servlets.
                || body.contains("Error: not logged in")
                // json/game.jsp, liveServers.jsp and whosonlineandlive.jsp redirect here.
                || finalPath.equals("/gameServer/mobile/index.jsp")
                // followers.jsp redirects to empty.jsp. Successful mobile writes also land on
                // empty.jsp, so that redirect only means "logged out" for followers.jsp.
                || (requestPath.equals("/gameServer/mobile/followers.jsp")
                        && finalPath.equals("/gameServer/mobile/empty.jsp"));
    }
}
