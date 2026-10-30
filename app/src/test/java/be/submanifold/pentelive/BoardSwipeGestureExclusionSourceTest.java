package be.submanifold.pentelive;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.Test;

/**
 * Board swipes starting near a screen edge are claimed by the system Back gesture (Android 10+),
 * which finishes the activity. Every swipeable board view must opt its area out of it, and no
 * swipe listener may itself finish the screen.
 */
public class BoardSwipeGestureExclusionSourceTest {
    private static final String JAVA = "src/main/java/be/submanifold/pentelive/";

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }

    @Test
    public void everySwipeableBoardViewExcludesItselfFromSystemGestures() throws IOException {
        for (String f : new String[] {"MMAIBoardView.java", "BoardView.java", "DBBoardView.java"}) {
            String s = read(JAVA + f);
            int at = s.indexOf("void onSizeChanged(");
            assertTrue(f + " overrides onSizeChanged", at >= 0);
            assertTrue(f + " sets exclusion rects in onSizeChanged",
                    s.indexOf("setSystemGestureExclusionRects(", at) > at);
            assertTrue(f + " uses SwipeGeometry", s.contains("SwipeGeometry.exclusionBand("));
        }
    }

    @Test
    public void swipeListenersNeverFinishTheScreen() throws IOException {
        String[] files = {"MMAIActivity.java", "BoardActivity.java", "DatabaseActivity.java",
                "db/ui/DbGameViewerActivity.java"};
        for (String f : files) {
            String s = read(JAVA + f);
            int at = s.indexOf("OnSwipeListener()");
            assertTrue(f + " has a swipe listener", at >= 0);
            int end = s.indexOf("});", at);
            String body = s.substring(at, end);
            assertTrue(f + " swipe listener must not finish", !body.contains("finish(") && !body.contains("onBackPressed"));
        }
    }
}
