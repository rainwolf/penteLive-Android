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
}
