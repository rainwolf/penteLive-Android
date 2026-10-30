package be.submanifold.pentelive.db;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DbTitleTest {
    @Test
    public void supportedGameUsesItsName() {
        int pente = DbGames.baseId("Pente");
        assertEquals("Pente", DbTitle.of(pente, "ignored"));
        assertEquals("Speed Pente", DbTitle.of(DbGames.speedId(pente), "ignored"));
        assertEquals("Keryo-Pente", DbTitle.of(DbGames.baseId("Keryo-Pente"), null));
    }

    @Test
    public void titleFollowsTheGameWhenItChanges() {
        assertEquals("Pente", DbTitle.of(DbGames.baseId("Pente"), "Pente"));
        assertEquals("Connect6", DbTitle.of(DbGames.baseId("Connect6"), "Pente"));
    }

    @Test
    public void unsupportedGameFallsBackToTheLabel() {
        assertEquals("Mystery", DbTitle.of(0, "Mystery"));
        assertEquals("", DbTitle.of(0, null));
    }

    @Test
    public void goAndRenjuUseTheirNames() {
        assertEquals("Go (9x9)", DbTitle.of(21, "ignored"));
        assertEquals("Speed Renju", DbTitle.of(32, null));
        assertEquals("Go (13x13)", DbTitle.of(73, null));
    }

    private static final String[] LEGACY = {"Pente", "Keryo-Pente", "Gomoku", "Speed Pente", "Speed Swap2-Pente"};

    @Test
    public void boardLabelKeepsLegacyLabelsForPenteGames() {
        assertEquals("Pente", DbTitle.boardLabel(1, LEGACY));
        assertEquals("Speed Pente", DbTitle.boardLabel(2, LEGACY));
        assertEquals("Gomoku", DbTitle.boardLabel(5, LEGACY));
    }

    @Test
    public void boardLabelNamesGoAndRenjuInsteadOfPente() {
        // The legacy array has no Go/Renju; falling back to "Pente" drew a 19x19 Pente board.
        assertEquals("Go (9x9)", DbTitle.boardLabel(21, LEGACY));
        assertEquals("Speed Go (13x13)", DbTitle.boardLabel(24, LEGACY));
        assertEquals("Go", DbTitle.boardLabel(19, LEGACY));
        assertEquals("Renju", DbTitle.boardLabel(31, LEGACY));
    }

    @Test
    public void boardLabelUnknownFallsBackToFirst() {
        assertEquals("Pente", DbTitle.boardLabel(0, LEGACY));
        assertEquals("Pente", DbTitle.boardLabel(99, LEGACY));
    }

    @Test
    public void activityLabelsTheBoardThroughBoardLabel() throws java.io.IOException {
        String src = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(
                "src/main/java/be/submanifold/pentelive/DatabaseActivity.java")), java.nio.charset.StandardCharsets.UTF_8);
        org.junit.Assert.assertTrue(src.contains("DbTitle.boardLabel(gameId,"));
        org.junit.Assert.assertFalse(src.contains("return types[0];"));
    }
}
