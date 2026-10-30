package be.submanifold.pentelive;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;

/** The toolbar title is the current game's name, refreshed wherever the game or position can change. */
public class DatabaseActivityTitleTest {
    private static String src() throws IOException {
        return new String(Files.readAllBytes(Paths.get("src/main/java/be/submanifold/pentelive/DatabaseActivity.java")),
                StandardCharsets.UTF_8);
    }

    private static String methodBody(String src, String signature) {
        int at = src.indexOf(signature);
        assertTrue(signature, at >= 0);
        int open = src.indexOf('{', at);
        int depth = 0;
        for (int i = open; i < src.length(); i++) {
            if (src.charAt(i) == '{') depth++;
            if (src.charAt(i) == '}' && --depth == 0) return src.substring(open, i);
        }
        throw new AssertionError("unbalanced " + signature);
    }

    @Test
    public void genericDatabaseStringOnlyBacksAnEmptyTitle() throws IOException {
        assertTrue(src().contains("getString(R.string.db_title)"));
    }

    @Test
    public void titleComesFromTheGameName() throws IOException {
        String body = methodBody(src(), "private void updateTitle()");
        assertTrue(body.contains("DbTitle.of(controller.gameId(), dbModel.gameLabel(), getString(R.string.db_title))"));
        assertTrue(src().contains(
                "toolbar.setTitle(DbTitle.of(controller.gameId(), dbModel.gameLabel(), getString(R.string.db_title)))"));
    }

    @Test
    public void titleRefreshesOnPositionUnsupportedAndFilterChanges() throws IOException {
        String s = src();
        for (String sig : new String[]{"public void showPosition(", "public void showUnsupported()",
                "private void applyFiltersFromSheet("}) {
            Matcher m = Pattern.compile("updateTitle\\(\\);").matcher(methodBody(s, sig));
            assertTrue(sig + " calls updateTitle()", m.find());
        }
    }
}
