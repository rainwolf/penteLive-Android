package be.submanifold.pentelive.db;

import static org.junit.Assert.assertEquals;

import java.util.Locale;
import org.junit.Test;

public class MoveRowFormatTest {
    @Test
    public void pctRounds() {
        assertEquals("58%", MoveRowFormat.pct(57.6));
    }

    @Test
    public void gamesSeparators() {
        assertEquals("12,345", MoveRowFormat.games(12345, Locale.US));
    }
}
