package be.submanifold.pentelive.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.Test;

public class CaptureIconsTest {
    private static final List<Integer> CAPTURE = Arrays.asList(180, 181, 0, 182, 183);

    @Test
    public void penteCaptureCounted() {
        CaptureIcons.Counts c = CaptureIcons.of(1, CAPTURE);
        assertEquals(2, c.p2Count);
        assertEquals(0, c.p1Count);
    }

    @Test
    public void fillsAreFixed() {
        CaptureIcons.Counts c = CaptureIcons.of(1, CAPTURE);
        assertEquals(0xFFFFFFFF, c.p1Fill);
        assertEquals(0xFF16181D, c.p2Fill);
    }

    @Test
    public void hiddenForGomokuAndConnect6() {
        for (int id : new int[] {5, 13}) {
            CaptureIcons.Counts c = CaptureIcons.of(id, Arrays.asList(180));
            assertFalse(c.visible);
            assertEquals("", CaptureIcons.text(c));
        }
    }

    @Test
    public void allTwelveGamesReplayWithoutError() {
        for (String name : DbGames.BASE_NAMES) {
            int id = DbGames.baseId(name);
            CaptureIcons.Counts c = CaptureIcons.of(id, Arrays.asList(180));
            assertEquals(name, DbGames.showsCaptureIcons(id), c.visible);
        }
    }

    @Test
    public void textFormat() {
        CaptureIcons.Counts c = CaptureIcons.of(1, Arrays.asList(180));
        assertEquals("● × 0  ● × 0", CaptureIcons.text(c));
        assertEquals("● × 0  ● × 2", CaptureIcons.text(CaptureIcons.of(1, CAPTURE)));
    }

    @Test
    public void occupiedExcludesCapturedStones() {
        Set<Integer> occ = CaptureIcons.occupied(1, CAPTURE);
        assertTrue(occ.contains(180));
        assertTrue(occ.contains(0));
        assertTrue(occ.contains(183));
        assertFalse(occ.contains(181));
        assertFalse(occ.contains(182));
    }
}
