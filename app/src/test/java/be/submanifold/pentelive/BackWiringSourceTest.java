package be.submanifold.pentelive;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;

/** Source-level guards for back wiring (no Robolectric available). */
public class BackWiringSourceTest {
    private static String read(String name) throws Exception {
        Path p = Paths.get("src/main/java/be/submanifold/pentelive/" + name);
        if (!Files.exists(p)) p = Paths.get("app").resolve(p);
        return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
    }

    @Test
    public void swipeCaseDoesNotClearStagedStoneUnconditionally() throws Exception {
        String s = read("BoardView.java");
        int i = s.indexOf("case SWIPE_RIGHT:");
        String body = s.substring(i, s.indexOf("return;", i));
        assertTrue(body.contains("swipeKeepsStagedStone"));
        assertFalse(body.replace("if (!BoardGesture.swipeKeepsStagedStone(action)) resetLoupe();", "")
                .contains("resetLoupe();"));
    }

    @Test
    public void backActionToleratesNullMovesList() throws Exception {
        String s = read("BoardActivity.java");
        int i = s.indexOf("private BackNavigation.Action backAction()");
        String body = s.substring(i, s.indexOf("}\n", s.indexOf("return BackNavigation.decide", i)));
        assertTrue(body.contains("ml == null"));
        assertFalse(body.contains("game.getMovesList().isEmpty()"));
    }

    @Test
    public void goClearShowsPassLabel() throws Exception {
        String s = read("BoardActivity.java");
        int i = s.indexOf("private void clearPlacedStone()");
        String body = s.substring(i, s.indexOf("board.invalidate();", i));
        assertTrue(body.contains("R.string.pass"));
    }
}
