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
    // Web STANDARD_GAME_IDS order: 1,3,5,...,31.
    private static final String[] NAMES = {
        "Pente", "Keryo-Pente", "Gomoku", "D-Pente", "G-Pente", "Poof-Pente", "Connect6",
        "Boat-Pente", "DK-Pente", "Go", "Go (9x9)", "Go (13x13)", "O-Pente", "Swap2-Pente",
        "Swap2-Keryo", "Renju"
    };
    private static final int[] HEX = {
        0xFFFDDEA3, 0xFFBAFDA3, 0xFFA3FDEB, 0xFFA3CDFD, 0xFFAEA3FD, 0xFFEDA3FD, 0xFFEDA3FD,
        0xFF25BAFF, 0xFFFFA500, 0xFFFAC832, 0xFFFAC832, 0xFFFAC832, 0xFF52BE80, 0xFFE5AA70,
        0xFF50C878, 0xFFD98880
    };

    @Test
    public void boardHexPerGame() {
        assertEquals(Arrays.asList(NAMES), DbGames.BASE_NAMES);
        for (int i = 0; i < NAMES.length; i++) {
            assertEquals(NAMES[i], 2 * i + 1, DbGames.baseId(NAMES[i]));
        }
        for (int i = 0; i < 16; i++) {
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
        assertEquals(Integer.valueOf(5), DbGames.fromGameType("Gomoku"));
        assertEquals(Integer.valueOf(19), DbGames.fromGameType("Go"));
        assertEquals(Integer.valueOf(20), DbGames.fromGameType("Speed Go"));
        assertEquals(Integer.valueOf(21), DbGames.fromGameType("Go (9x9)"));
        assertEquals(Integer.valueOf(22), DbGames.fromGameType("Speed Go (9x9)"));
        assertEquals(Integer.valueOf(23), DbGames.fromGameType("Go (13x13)"));
        assertEquals(Integer.valueOf(24), DbGames.fromGameType("Speed Go (13x13)"));
        assertEquals(Integer.valueOf(31), DbGames.fromGameType("Renju"));
        assertEquals(Integer.valueOf(32), DbGames.fromGameType("Speed Renju"));
        assertNull(DbGames.fromGameType("Tombstone"));
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
    public void supportedIncludesGoAndRenju() {
        assertTrue(DbGames.isSupported(1));
        assertTrue(DbGames.isSupported(14));
        assertTrue(DbGames.isSupported(63));
        for (int id : new int[] {19, 20, 21, 22, 23, 24, 31, 32, 69, 71, 73, 81}) {
            assertTrue(String.valueOf(id), DbGames.isSupported(id));
        }
        for (int id : new int[] {0, 33, 99}) assertFalse(String.valueOf(id), DbGames.isSupported(id));
    }

    @Test
    public void gridSizes() {
        for (int id : new int[] {1, 2, 13, 51, 19, 20, 69}) assertEquals(String.valueOf(id), 19, DbGames.gridSize(id));
        for (int id : new int[] {21, 22, 71}) assertEquals(String.valueOf(id), 9, DbGames.gridSize(id));
        for (int id : new int[] {23, 24, 73}) assertEquals(String.valueOf(id), 13, DbGames.gridSize(id));
        for (int id : new int[] {31, 32, 81}) assertEquals(String.valueOf(id), 15, DbGames.gridSize(id));
    }

    @Test
    public void seedPerGame() {
        assertEquals(Collections.emptyList(), DbGames.seed(19));
        assertEquals(Collections.emptyList(), DbGames.seed(21));
        assertEquals(Collections.emptyList(), DbGames.seed(24));
        assertEquals(Arrays.asList(112), DbGames.seed(31));
        assertEquals(Arrays.asList(112), DbGames.seed(81));
        assertEquals(Arrays.asList(180), DbGames.seed(1));
        assertEquals(Arrays.asList(180), DbGames.seed(13));
        assertEquals(Collections.emptyList(), DbGames.seed(7));
        assertEquals(Collections.emptyList(), DbGames.seed(27));
    }

    @Test
    public void moveFloorGoIsZeroRenjuIsOne() {
        assertEquals(0, DbGames.moveFloor(19));
        assertEquals(0, DbGames.moveFloor(22));
        assertEquals(1, DbGames.moveFloor(31));
    }

    @Test
    public void capturesShownForGoNotRenju() {
        assertTrue(DbGames.showsCaptureIcons(19));
        assertTrue(DbGames.showsCaptureIcons(21));
        assertTrue(DbGames.showsCaptureIcons(23));
        assertFalse(DbGames.showsCaptureIcons(31));
        assertFalse(DbGames.showsCaptureIcons(81));
    }

    @Test
    public void blackMovesFirstOnlyGoAndRenju() {
        for (int id : new int[] {19, 20, 21, 23, 69, 31, 32, 81}) assertTrue(String.valueOf(id), DbGames.blackMovesFirst(id));
        for (int id : new int[] {1, 3, 5, 7, 13, 25, 27, 29, 51}) assertFalse(String.valueOf(id), DbGames.blackMovesFirst(id));
    }

    @Test
    public void isGoOnlyForGo() {
        for (int id : new int[] {19, 20, 21, 22, 23, 24, 71}) assertTrue(String.valueOf(id), DbGames.isGo(id));
        for (int id : new int[] {1, 5, 31, 81}) assertFalse(String.valueOf(id), DbGames.isGo(id));
    }

    @Test
    public void passCellIsGridSquared() {
        assertEquals(81, DbGames.passCell(21));
        assertEquals(169, DbGames.passCell(23));
        assertEquals(361, DbGames.passCell(19));
        assertEquals(225, DbGames.passCell(31));
    }

    @Test
    public void labelNamesSpeed() {
        assertEquals("Speed Go (9x9)", DbGames.label(22));
        assertEquals("Go", DbGames.label(19));
        assertEquals("Renju", DbGames.label(81));
        assertEquals("Speed Pente", DbGames.label(2));
    }

    @Test
    public void speedAllowedForGoAndRenju() {
        assertTrue(DbGames.speedAllowed(19));
        assertTrue(DbGames.speedAllowed(21));
        assertTrue(DbGames.speedAllowed(23));
        assertTrue(DbGames.speedAllowed(31));
    }

    @Test
    public void filtersSheetChipsFollowBaseNames() throws java.io.IOException {
        // One chip per BASE_NAMES entry, tagged with its base id: Go/Renju chips come from the catalog.
        String src = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(
                "src/main/java/be/submanifold/pentelive/db/ui/DbFiltersSheet.java")), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(src.contains("for (int i = 0; i < DbGames.BASE_NAMES.size(); i++)"));
        assertTrue(src.contains("chip.setTag(DbGames.baseId(name));"));
        assertTrue(src.contains("DbGames.speedAllowed("));
    }
}
