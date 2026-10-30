package be.submanifold.pentelive.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;

import org.junit.Test;

public class DbStartTest {
    @Test
    public void freshLaunchUsesExtras() {
        DbStart s = DbStart.resolve("Keryo-Pente", Arrays.asList(180, 181), null, null);
        assertEquals(DbGames.fromGameType("Keryo-Pente"), s.gameOverride);
        assertEquals(Arrays.asList(180, 181), s.initialMoves);
        assertEquals("Keryo-Pente", s.label);
    }

    @Test
    public void freshLaunchWithoutGameIgnoresMoves() {
        DbStart s = DbStart.resolve(null, Arrays.asList(180, 181), null, null);
        assertNull(s.gameOverride);
        assertNull(s.initialMoves);
        assertNull(s.label);
    }

    @Test
    public void unknownExtrasGameIsUnsupported() {
        DbStart s = DbStart.resolve("Tombstone", null, null, null);
        assertEquals(Integer.valueOf(0), s.gameOverride);
    }

    @Test
    public void goAndRenjuExtrasResolve() {
        DbStart go = DbStart.resolve("Go (9x9)", Arrays.asList(40, 81, 41), null, null);
        assertEquals(Integer.valueOf(21), go.gameOverride);
        assertEquals(Arrays.asList(40, 81, 41), go.initialMoves);
        assertEquals("Go (9x9)", go.label);
        assertEquals(Integer.valueOf(20), DbStart.resolve("Speed Go", null, null, null).gameOverride);
        assertEquals(Integer.valueOf(32), DbStart.resolve("Speed Renju", null, null, null).gameOverride);
    }

    @Test
    public void restoreUsesSavedGameAndIgnoresExtras() {
        // Launched for Pente, then the filters switched to Gomoku before process death.
        int gomoku = DbGames.fromGameType("Gomoku");
        DbStart s = DbStart.resolve("Pente", Arrays.asList(180), gomoku, "Gomoku");
        assertEquals(Integer.valueOf(gomoku), s.gameOverride);
        assertNull(s.initialMoves); // the saved moves are put back by DbController.restore
        assertEquals("Gomoku", s.label);
    }

    @Test
    public void restoreKeepsSavedUnsupportedGame() {
        DbStart s = DbStart.resolve("Pente", null, 0, "Tombstone");
        assertEquals(Integer.valueOf(0), s.gameOverride);
        assertEquals("Tombstone", s.label);
    }
}
