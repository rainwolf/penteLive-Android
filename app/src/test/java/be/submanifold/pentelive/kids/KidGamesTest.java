package be.submanifold.pentelive.kids;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import be.submanifold.pentelive.Game;
import be.submanifold.pentelive.kids.KidAccounts.ActiveGame;

import org.junit.Test;

public class KidGamesTest {

    @Test
    public void readOnlyGame_isInactiveWithIdsOnly() {
        ActiveGame g = new ActiveGame();
        g.gid = 50000000001234L; g.gameName = "Pente"; g.opponentName = "graviton"; g.privateGame = true;
        Game game = KidGames.readOnlyGame(g);
        assertFalse(game.isActive());
        assertEquals("50000000001234", game.getGameID());
        assertEquals("Pente", game.getGameType());
        assertNull(game.getOpponentName());   // else getBoardString() freezes "rating null" before the JSON load
        assertNull(game.getPrivateGame());
    }
}
