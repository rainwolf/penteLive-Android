package be.submanifold.pentelive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;

/**
 * Read-only kid games have no remaining time (getLocalizedTime() is null). getBoardString must
 * not print the "Remaining time: null" segment; source-level pin, no Activity harness.
 */
public class GameBoardStringSourceTest {

    private static final String GAME = "src/main/java/be/submanifold/pentelive/Game.java";

    private static String getBoardStringBody() throws IOException {
        String src = new String(Files.readAllBytes(Paths.get(GAME)), StandardCharsets.UTF_8);
        int start = src.indexOf("public String getBoardString()");
        int end = src.indexOf("public String getHideString()", start);
        assertTrue(start >= 0 && end > start);
        return src.substring(start, end);
    }

    private static int count(String s, String needle) {
        int n = 0;
        for (int i = s.indexOf(needle); i >= 0; i = s.indexOf(needle, i + 1)) {
            n++;
        }
        return n;
    }

    @Test
    public void remainingTimeSegmentIsGuardedByNullCheck() throws IOException {
        String body = getBoardStringBody();
        assertTrue("remaining-time segment must be guarded by getLocalizedTime() != null",
                body.contains("getLocalizedTime() != null"));
    }

    @Test
    public void unguardedRemainingTimeConcatenationIsGone() throws IOException {
        String body = getBoardStringBody();
        // Both branches (" vs " and else) must go through the guarded helper, not concatenate
        // the time directly.
        assertEquals("only the guarded helper may concatenate the time segment", 1,
                count(body, "timeStr + \" \" + getLocalizedTime()"));
        assertEquals("both branches must use the guarded time segment", 2,
                count(body, "mOpponentRating + timeSeg"));
    }
}
