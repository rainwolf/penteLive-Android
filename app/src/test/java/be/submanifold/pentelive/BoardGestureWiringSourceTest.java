package be.submanifold.pentelive;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.Test;

/** The swipe-vs-placement decision lives in BoardGesture; no board view may use release speed. */
public class BoardGestureWiringSourceTest {
    private static final String JAVA = "src/main/java/be/submanifold/pentelive/";

    @Test
    public void boardViewsUseBoardGestureNotVelocity() throws IOException {
        for (String f : new String[] {"MMAIBoardView.java", "BoardView.java", "DBBoardView.java"}) {
            String s = new String(Files.readAllBytes(Paths.get(JAVA + f)), StandardCharsets.UTF_8);
            assertFalse(f + " must not use VelocityTracker", s.contains("VelocityTracker"));
            assertFalse(f + " must not keep handleSwipe", s.contains("handleSwipe"));
            assertTrue(f + " uses BoardGesture", s.contains("new BoardGesture("));
        }
    }
}
