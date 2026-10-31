package be.submanifold.pentelive;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.Test;

/** Move navigation buttons exist in every board layout, are theme-tinted and are wired next to the swipes. */
public class NavButtonsSourceTest {
    private static final String MAIN = "src/main/";

    private static String read(String p) throws IOException {
        return new String(Files.readAllBytes(Paths.get(MAIN + p)), StandardCharsets.UTF_8);
    }

    private static void layoutHas(String layout, String... ids) throws IOException {
        String s = read("res/layout/" + layout);
        for (String id : ids) assertTrue(layout + " has " + id, s.contains("@+id/" + id + "\""));
        assertTrue(layout + " tints icons from the theme", s.contains("?android:attr/textColorPrimary"));
    }

    @Test
    public void layoutsHaveTheButtons() throws IOException {
        layoutHas("activity_board.xml", "firstButton", "backButton", "forwardButton", "lastButton");
        layoutHas("activity_database.xml", "firstButton", "backButton", "forwardButton", "lastButton");
        layoutHas("activity_db_game_viewer.xml", "firstButton", "backButton", "forwardButton", "lastButton");
        layoutHas("activity_mmai.xml", "backButton");
    }

    @Test
    public void activitiesWireButtons() throws IOException {
        String j = "java/be/submanifold/pentelive/";
        String board = read(j + "BoardActivity.java");
        for (String s : new String[] {"R.id.firstButton", "R.id.backButton", "R.id.forwardButton", "R.id.lastButton",
                "navFirst()", "navBack()", "navForward()", "navLast()"}) {
            assertTrue("BoardActivity uses " + s, board.contains(s));
        }
        String db = read(j + "DatabaseActivity.java");
        String viewer = read(j + "db/ui/DbGameViewerActivity.java");
        for (String s : new String[] {"R.id.firstButton", "R.id.backButton", "R.id.forwardButton", "R.id.lastButton"}) {
            assertTrue("DatabaseActivity uses " + s, db.contains(s));
            assertTrue("DbGameViewerActivity uses " + s, viewer.contains(s));
        }
        assertTrue(db.contains("controller.first()") && db.contains("controller.last()"));
        assertTrue(read(j + "MMAIActivity.java").contains("R.id.backButton"));
    }
}
