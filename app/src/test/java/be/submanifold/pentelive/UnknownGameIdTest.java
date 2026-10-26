package be.submanifold.pentelive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;

import org.junit.Test;

import be.submanifold.pente.rules.Variants;

/** Unknown (experimental) game ids/names must be skipped; known items in the same response survive. */
public class UnknownGameIdTest {

    private static final String JSON = "{\"player\":{\"name\":\"me\"},"
            + "\"kingOfTheHill\":[{\"gameId\":1,\"kingName\":\"a\"},{\"gameId\":33,\"kingName\":\"x\"},{\"gameId\":83,\"kingName\":\"y\"}],"
            + "\"ratingStats\":[{\"gameName\":\"Pente\",\"gameId\":1},{\"gameName\":\"New\",\"gameId\":34}],"
            + "\"invitationsReceived\":[{\"setId\":1,\"gameName\":\"Pente\"},{\"setId\":2,\"gameName\":\"Foo-Pente\"}],"
            + "\"invitationsSent\":[{\"setId\":3,\"gameName\":\"Speed Renju\"},{\"setId\":4,\"gameName\":\"Speed Foo\"}],"
            + "\"activeGamesMyTurn\":[{\"gid\":5,\"gameName\":\"Go (9x9)\"},{\"gid\":6,\"gameName\":\"Bar\"}],"
            + "\"activeGamesOpponentTurn\":[{\"gid\":7,\"gameName\":\"Swap2-Keryo\"},{\"gid\":8,\"gameName\":\"Bar\"}],"
            + "\"openInvitationGames\":[{\"setId\":9,\"gameName\":\"Connect6\"},{\"setId\":10,\"gameName\":\"Baz\"}]}";

    @Test
    public void unknownItemsSkippedKnownKept() {
        JsonModels.IndexResponse json = new Gson().fromJson(JSON, JsonModels.IndexResponse.class);
        PentePlayer p = new PentePlayer("t", "pw");
        p.populateGameLists(json);

        assertEquals(1, p.getHills().size());
        assertEquals("a", p.getHills().get(0).getCurrentKing());
        assertEquals(1, p.getRatingStats().size());
        assertEquals(1, p.getInvitations().size());
        assertEquals("1", p.getInvitations().get(0).getGameID());
        assertEquals(1, p.getSentInvitations().size());
        assertEquals("3", p.getSentInvitations().get(0).getGameID());
        assertEquals(1, p.getActiveGames().size());
        assertEquals("5", p.getActiveGames().get(0).getGameID());
        assertEquals(1, p.getNonActiveGames().size());
        assertEquals("7", p.getNonActiveGames().get(0).getGameID());
        assertEquals(1, p.getPublicInvitations().size());
        assertEquals("9", p.getPublicInvitations().get(0).getGameID());
    }

    @Test
    public void knownIdPredicate() {
        for (int id : new int[]{1, 2, 31, 32, 51, 52, 79, 80, 81, 82}) assertTrue("" + id, Variants.isKnownGameId(id));
        for (int id : new int[]{-1, 0, 33, 34, 50, 83, 84, 200}) assertFalse("" + id, Variants.isKnownGameId(id));
    }

    @Test
    public void knownTypePredicate() {
        assertTrue(Variants.isKnownGameType("Speed Go (13x13)"));
        assertFalse(Variants.isKnownGameType("Foo-Pente"));
        assertFalse(Variants.isKnownGameType(null));
    }
}
