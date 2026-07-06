package be.submanifold.pentelive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import be.submanifold.pente.rules.VariantReferee;

/**
 * Regression net tying the extracted {@link VariantReferee} to its authority,
 * react_mmai/src/Classes/GameClass.js.
 *
 * <p>The fixtures under {@code src/test/resources/variant_fixtures/} are produced
 * ONCE by {@code src/test/fixtures/generate_variant_fixtures.mjs}, which plays a
 * fixed-seed set of pseudo-random legal games through the JS authority and records
 * each game's move list plus the final board / captures / winner / gameOver. Here we
 * replay every recorded move list through the Java referee and assert the same
 * final state, so any drift between the mobile referee and the web authority fails
 * the build.
 *
 * <h3>Semantics mapping (authority GameClass.js -> Android VariantReferee)</h3>
 * <ul>
 *   <li><b>Board:</b> {@code GameClass.abstractBoard[x][y]} == {@code board[x][y]},
 *       with {@code x = move % 19}, {@code y = move / 19}. The fixture flattens it
 *       x-major, {@code index = x*19 + y}. Cell values: 0 empty, 1 white, 2 black
 *       (no -1 because the generator uses {@code rated = false}).</li>
 *   <li><b>Captures:</b> {@code GameClass.captures[c]} counts the stones LOST by
 *       color {@code c}. So the fixture's {@code white == captures[1] ==
 *       VariantReferee.whiteCaptures} and {@code black == captures[2] ==
 *       VariantReferee.blackCaptures}. (whiteCaptures is "stones white lost", not
 *       "captures made by white".)</li>
 *   <li><b>Winner:</b> {@code GameClass.winner} is {@code undefined} (no winner),
 *       {@code 1} (white) or {@code 2} (black); the generator maps {@code undefined}
 *       to {@code 0}. {@link VariantReferee#replay} returns / stores the identical
 *       0/1/2 convention. No color flip is applied for games 11/15/25 (GameClass's
 *       {@code player_color} flip is display-only and confined to Go, games 19-24),
 *       so the fixture winner compares directly to the referee winner.</li>
 *   <li><b>gameOver:</b> {@code == (winner != 0)} on both sides.</li>
 * </ul>
 */
public class VariantRefereeFixtureTest {

    private static final int SIZE = 19;
    private static final int[] VARIANTS = {11, 15, 25};

    @Test
    public void refereeMatchesGameClassFixtures() throws Exception {
        int totalGames = 0;
        int totalDecided = 0;
        for (int variant : VARIANTS) {
            JsonObject root = loadFixture(variant);
            assertEquals("fixture variant tag", variant, root.get("variant").getAsInt());
            JsonArray games = root.getAsJsonArray("games");
            assertNotNull("games array for variant " + variant, games);
            assertTrue("variant " + variant + " should hold a body of games",
                    games.size() >= 100);

            VariantReferee referee = new VariantReferee();
            for (int gi = 0; gi < games.size(); gi++) {
                JsonObject game = games.get(gi).getAsJsonObject();
                List<Integer> moves = toIntList(game.getAsJsonArray("moves"));

                byte[][] board = new byte[SIZE][SIZE];
                int winner = referee.replay(variant, board, moves, false);

                String where = "variant " + variant + " game " + gi
                        + " (" + moves.size() + " moves)";

                assertEquals(where + " whiteCaptures (== GameClass captures[1])",
                        game.get("white").getAsInt(), referee.whiteCaptures);
                assertEquals(where + " blackCaptures (== GameClass captures[2])",
                        game.get("black").getAsInt(), referee.blackCaptures);
                assertEquals(where + " winner (0 none / 1 white / 2 black)",
                        game.get("winner").getAsInt(), winner);
                assertEquals(where + " winner field", winner, referee.winner);
                assertEquals(where + " gameOver", game.get("gameOver").getAsBoolean(),
                        referee.gameOver);

                JsonArray expBoard = game.getAsJsonArray("board");
                assertEquals(where + " board length", SIZE * SIZE, expBoard.size());
                for (int x = 0; x < SIZE; x++) {
                    for (int y = 0; y < SIZE; y++) {
                        int expected = expBoard.get(x * SIZE + y).getAsInt();
                        assertEquals(where + " board[" + x + "][" + y + "]",
                                expected, board[x][y]);
                    }
                }

                totalGames++;
                if (winner != 0) {
                    totalDecided++;
                }
            }
        }
        // Sanity: the fixtures must actually exercise win detection, not just
        // quiet boards, or a broken winner rule could pass unnoticed.
        assertTrue("fixtures should contain decided games (got " + totalDecided + ")",
                totalDecided > 0);
        System.out.println("VariantRefereeFixtureTest: " + totalGames
                + " games replayed, " + totalDecided + " decided.");
    }

    private static JsonObject loadFixture(int variant) throws Exception {
        String resource = "/variant_fixtures/game_" + variant + ".json";
        try (InputStream in = VariantRefereeFixtureTest.class.getResourceAsStream(resource)) {
            assertNotNull("missing fixture resource " + resource
                    + " (run app/src/test/fixtures/generate_variant_fixtures.mjs)", in);
            return JsonParser.parseReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private static List<Integer> toIntList(JsonArray arr) {
        List<Integer> out = new ArrayList<>(arr.size());
        for (int i = 0; i < arr.size(); i++) {
            out.add(arr.get(i).getAsInt());
        }
        return out;
    }
}
