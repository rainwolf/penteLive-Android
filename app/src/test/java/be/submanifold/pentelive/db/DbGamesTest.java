package be.submanifold.pentelive.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class DbGamesTest {
    private static final int[] HEX = {
        0xFFFDDEA3, 0xFFBAFDA3, 0xFFA3FDEB, 0xFFA3CDFD, 0xFFAEA3FD, 0xFFEDA3FD,
        0xFFEDA3FD, 0xFF25BAFF, 0xFFFFA500, 0xFF52BE80, 0xFFE5AA70, 0xFF50C878
    };

    @Test
    public void boardHexPerGame() {
        assertEquals(12, DbGames.BASE_NAMES.size());
        assertEquals("Pente", DbGames.BASE_NAMES.get(0));
        assertEquals("Swap2-Keryo", DbGames.BASE_NAMES.get(11));
        for (int i = 0; i < 12; i++) {
            String n = DbGames.BASE_NAMES.get(i);
            assertEquals(n, 0xFF000000 | HEX[i], DbGames.boardArgb(DbGames.baseId(n)));
        }
    }

    @Test
    public void speedUsesBaseBoard() {
        assertEquals(DbGames.boardArgb(1), DbGames.boardArgb(2));
    }

    @Test
    public void speedIdIsBasePlusOne() {
        assertEquals(2, DbGames.speedId(1));
        assertEquals(26, DbGames.speedId(25));
        assertTrue(DbGames.isSpeed(2));
        assertFalse(DbGames.isSpeed(1));
    }

    @Test
    public void speedNotAllowedForSwap2() {
        assertFalse(DbGames.speedAllowed(27));
        assertFalse(DbGames.speedAllowed(29));
        assertTrue(DbGames.speedAllowed(1));
    }

    @Test
    public void baseNameOfTurnBased() {
        assertEquals("Pente", DbGames.baseName(51));
        assertEquals("Pente", DbGames.baseName(52));
    }

    @Test
    public void fromGameTypeLabels() {
        assertEquals(Integer.valueOf(1), DbGames.fromGameType("Pente"));
        assertEquals(Integer.valueOf(2), DbGames.fromGameType("Speed Pente"));
        assertEquals(Integer.valueOf(4), DbGames.fromGameType("Speed Keryo-Pente"));
        assertEquals(Integer.valueOf(27), DbGames.fromGameType("Speed Swap2-Pente"));
        assertEquals(Integer.valueOf(29), DbGames.fromGameType("Speed Swap2-Keryo"));
        assertNull(DbGames.fromGameType("Go"));
        assertNull(DbGames.fromGameType("Go (9x9)"));
        assertNull(DbGames.fromGameType("Renju"));
        assertNull(DbGames.fromGameType(null));
    }

    @Test
    public void captureIconsHiddenForGomokuAndConnect6() {
        assertFalse(DbGames.showsCaptureIcons(5));
        assertFalse(DbGames.showsCaptureIcons(13));
        assertTrue(DbGames.showsCaptureIcons(1));
    }

    @Test
    public void floorZeroForOffCentreGames() {
        assertEquals(0, DbGames.moveFloor(7));
        assertEquals(1, DbGames.moveFloor(1));
        assertEquals(Collections.emptyList(), DbGames.seed(7));
        assertEquals(Arrays.asList(180), DbGames.seed(1));
    }

    @Test
    public void seedIsMutable() {
        List<Integer> s = DbGames.seed(1);
        s.add(181);
        assertEquals(Arrays.asList(180, 181), s);
        assertEquals(Arrays.asList(180), DbGames.seed(1));
    }

    @Test
    public void supportedExcludesGoRenjuAndUnknown() {
        assertTrue(DbGames.isSupported(1));
        assertTrue(DbGames.isSupported(14));
        assertTrue(DbGames.isSupported(63));
        for (int id : new int[] {0, 19, 21, 23, 31, 81, 99}) assertFalse(String.valueOf(id), DbGames.isSupported(id));
    }

    @Test
    public void gridIs19() {
        assertEquals(19, DbGames.gridSize(1));
    }
}
