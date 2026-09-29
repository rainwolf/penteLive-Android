package be.submanifold.pentelive.net;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class LoginResponseTest {

    /** begin.jsp's loggedInTabs, rendered by top.jsp:103 once LoginFilter set the name attribute. */
    static final String SUCCESS_PAGE = "<ul id=\"tabs\"><li><a href=\"/gameServer/index.jsp\">Dashboard</a>"
            + "<li><a href=\"/gameServer/logout\">Logout</a></ul>"
            + "<form name=\"login_form\" method=\"post\" action=\"/gameServer/mobile/empty.jsp\">";
    /** login.jsp:20-29 with invalidLogin=invalid. */
    static final String INVALID_PAGE = "<b><font size=\"2\">\n            Invalid name or password, please try again.\n"
            + "         </font></b><form name=\"login_form\" method=\"post\">";

    @Test
    public void logoutLink_isSuccess() {
        assertEquals(LoginResponse.Outcome.SUCCESS, LoginResponse.classify(SUCCESS_PAGE));
    }

    @Test
    public void invalidMarker_isInvalidCredentials() {
        assertEquals(LoginResponse.Outcome.INVALID_CREDENTIALS, LoginResponse.classify(INVALID_PAGE));
    }

    @Test
    public void speedConvertedMessage_isUnexpected() {
        String speed = "This player has been converted from a \"speed\" player to a \"normal\" player."
                + "<form name=\"login_form\" method=\"post\">";
        assertEquals(LoginResponse.Outcome.UNEXPECTED, LoginResponse.classify(speed));
    }

    @Test
    public void bareLoginForm_isUnexpected() {
        assertEquals(LoginResponse.Outcome.UNEXPECTED,
                LoginResponse.classify("<form name=\"login_form\" method=\"post\" action=\"/gameServer/index.jsp\">"));
    }

    @Test
    public void emptyBody_isUnexpected() {
        assertEquals(LoginResponse.Outcome.UNEXPECTED, LoginResponse.classify(""));
    }
}
