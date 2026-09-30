package be.submanifold.pentelive.net;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LoggedOutDetectorTest {

    @Test
    public void indexJsonError_isLoggedOut() {
        assertTrue(LoggedOutDetector.isLoggedOut("/gameServer/mobile/json/index.jsp",
                "/gameServer/mobile/json/index.jsp",
                "{\"error\":\"Invalid name or password, please try again.\"}"));
    }

    @Test
    public void forwardedLoginPage_isLoggedOut() {
        assertTrue(LoggedOutDetector.isLoggedOut("/gameServer/tb/game", "/gameServer/tb/game",
                "<form name=\"login_form\" method=\"post\" action=\"/gameServer/index.jsp\">"));
    }

    @Test
    public void errorPageNotLoggedIn_isLoggedOut() {
        assertTrue(LoggedOutDetector.isLoggedOut("/gameServer/social", "/gameServer/social",
                "<html>Error: not logged in</html>"));
    }

    @Test
    public void redirectToMobileIndex_isLoggedOut() {
        assertTrue(LoggedOutDetector.isLoggedOut("/gameServer/mobile/json/game.jsp",
                "/gameServer/mobile/index.jsp", ""));
    }

    @Test
    public void followersRedirectToEmpty_isLoggedOut() {
        assertTrue(LoggedOutDetector.isLoggedOut("/gameServer/mobile/followers.jsp",
                "/gameServer/mobile/empty.jsp", ""));
    }

    @Test
    public void normalJson_isNotLoggedOut() {
        assertFalse(LoggedOutDetector.isLoggedOut("/gameServer/mobile/json/game.jsp",
                "/gameServer/mobile/json/game.jsp", "{\"gameName\":\"Pente\",\"gid\":1}"));
    }

    @Test
    public void successfulWriteRedirectToEmpty_isNotLoggedOut() {
        // MoveServlet and the other mobile writes answer success with a 302 to empty.jsp.
        assertFalse(LoggedOutDetector.isLoggedOut("/gameServer/tb/game",
                "/gameServer/mobile/empty.jsp", ""));
        assertFalse(LoggedOutDetector.isLoggedOut("/gameServer/tb/game",
                "/gameServer/tb/game", ""));
    }

    @Test
    public void errorPageWithAnotherError_isNotLoggedOut() {
        assertFalse(LoggedOutDetector.isLoggedOut("/gameServer/social", "/gameServer/social",
                "<html>Error: player not found</html>"));
    }
}
