package be.submanifold.pentelive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class GameRenjuUnitTest {

    private static Game makeGame(String gameType) {
        return new Game("id", "sid", gameType, "opponent", "1500",
                "white", "5", "Not Rated", "false", "1", "0");
    }

    @Test
    public void isRenjuTrueForRenjuTypes() {
        assertTrue(makeGame("Renju").isRenju());
        assertTrue(makeGame("Speed Renju").isRenju());
        assertFalse(makeGame("Gomoku").isRenju());
        assertFalse(makeGame("Pente").isRenju());
    }

    @Test
    public void gridSizeForGameTypeIsFifteenForRenju() {
        assertEquals(15, Game.gridSizeForGameType("Renju"));
        assertEquals(15, Game.gridSizeForGameType("Speed Renju"));
        assertEquals(9, Game.gridSizeForGameType("Go (9x9)"));
        assertEquals(13, Game.gridSizeForGameType("Go (13x13)"));
        assertEquals(19, Game.gridSizeForGameType("Pente"));
        assertEquals(19, Game.gridSizeForGameType("Go"));
    }

    @Test
    public void buildSubmitMoveUrlOmitsRenjuActionWhenNull() {
        String url = Game.buildSubmitMoveUrl("", "999", "130", "hi", null);
        assertTrue(url.contains("command=move"));
        assertTrue(url.contains("gid=999"));
        assertTrue(url.contains("moves=130"));
        assertFalse(url.contains("renjuAction"));
    }

    @Test
    public void buildSubmitMoveUrlAppendsRenjuAction() {
        String url = Game.buildSubmitMoveUrl("", "999", "0,113", "", "swap");
        assertTrue(url.contains("moves=0,113"));
        assertTrue(url.contains("renjuAction=swap"));
    }
}
