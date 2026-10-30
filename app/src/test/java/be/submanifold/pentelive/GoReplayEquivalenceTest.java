package be.submanifold.pentelive;

import be.submanifold.pente.rules.BoardState;
import be.submanifold.pente.rules.GoReplay;
import be.submanifold.pente.rules.Variant;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Differential test: {@link GoReplay} (rules module, used by the DB screen) against the live
 * {@link Game}'s private {@code replayGoGame(int)} on 9x9, 13x13 and 19x19. Hand-written lines
 * plus seeded random games with passes and, sometimes, a double pass followed by dead-stone marks.
 * Board, capture counts and ko cell must match exactly, at every prefix of every game.
 */
public class GoReplayEquivalenceTest {

    private static final int GAMES_PER_SIZE = 150;

    @Test
    public void handWrittenLines() throws Exception {
        compareAllPrefixes(Variant.GO_9, Arrays.asList(0, 1, 2, 40, 10));               // edge capture
        compareAllPrefixes(Variant.GO_9, Arrays.asList(9, 2, 1, 20, 19, 12, 80, 10, 11, 81, 70)); // ko, pass
        compareAllPrefixes(Variant.GO_13, Arrays.asList(13, 0, 14, 1, 2));              // group capture
        compareAllPrefixes(Variant.GO_9, Arrays.asList(40, 41, 81, 81, 41, 40));        // double pass + marks
        compareAllPrefixes(Variant.GO_9, Arrays.asList(1, 80, 9, 0));                   // suicide stays
        compareAllPrefixes(Variant.GO_19, Arrays.asList(0, 1, 200, 19, 361, 360));      // 19x19 corner + pass
    }

    @Test
    public void random9x9() throws Exception {
        runRandom(Variant.GO_9, 0x60090001L);
    }

    @Test
    public void random13x13() throws Exception {
        runRandom(Variant.GO_13, 0x60130001L);
    }

    @Test
    public void random19x19() throws Exception {
        runRandom(Variant.GO_19, 0x60190001L);
    }

    private void runRandom(Variant v, long seed) throws Exception {
        int n = gridOf(v);
        int pass = n * n;
        Random rnd = new Random(seed);
        for (int g = 0; g < GAMES_PER_SIZE; g++) {
            List<Integer> moves = new ArrayList<>();
            int len = 1 + rnd.nextInt(Math.min(80, pass));
            for (int k = 0; k < len; k++) {
                int r = rnd.nextInt(20);
                if (r == 0) {
                    moves.add(pass);
                    continue;
                }
                // Prefer empty cells of the current position, like real games.
                BoardState s = GoReplay.replay(moves, v, moves.size());
                List<Integer> empty = new ArrayList<>();
                for (int c = 0; c < pass; c++) {
                    if (s.board[c / n][c % n] == 0) empty.add(c);
                }
                if (empty.isEmpty()) break;
                moves.add(empty.get(rnd.nextInt(empty.size())));
            }
            if (rnd.nextInt(3) == 0) {
                moves.add(pass);
                moves.add(pass);
                for (int k = 0; k < 3; k++) moves.add(rnd.nextInt(pass));
            }
            compareAllPrefixes(v, moves);
        }
    }

    private void compareAllPrefixes(Variant v, List<Integer> moves) throws Exception {
        int n = gridOf(v);
        for (int until = 0; until <= moves.size(); until++) {
            Game game = liveGoGame(n, moves);
            Method replay = Game.class.getDeclaredMethod("replayGoGame", int.class);
            replay.setAccessible(true);
            replay.invoke(game, until);

            byte[][] live = (byte[][]) get(game, "abstractBoard");
            BoardState s = GoReplay.replay(moves, v, until);
            String ctx = v + " moves=" + moves + " until=" + until;
            for (int r = 0; r < n; r++) {
                for (int c = 0; c < n; c++) {
                    if (live[r][c] != s.board[r][c]) {
                        fail(ctx + " cell(" + r + "," + c + ") live=" + live[r][c] + " port=" + s.board[r][c]);
                    }
                }
            }
            assertEquals(ctx + " whiteCaptures", get(game, "whiteCaptures"), s.whiteCaptures);
            assertEquals(ctx + " blackCaptures", get(game, "blackCaptures"), s.blackCaptures);
            assertEquals(ctx + " koMove", game.koMove, s.koMove);
        }
    }

    private static int gridOf(Variant v) {
        return v == Variant.GO_9 ? 9 : v == Variant.GO_13 ? 13 : 19;
    }

    private static Game liveGoGame(int n, List<Integer> moves) throws Exception {
        String label = n == 9 ? "Go (9x9)" : n == 13 ? "Go (13x13)" : "Go";
        Game game = new Game("go-eq-id", "go-eq-set", label, "opponent", "1500",
                "white", "5", "Not Rated", "false", "1", "0");
        set(game, "gridSize", n);
        set(game, "passMove", n * n);
        set(game, "go", true);
        set(game, "mMovesList", new ArrayList<>(moves));
        return game;
    }

    private static Object get(Game g, String name) throws Exception {
        Field f = Game.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(g);
    }

    private static void set(Game g, String name, Object value) throws Exception {
        Field f = Game.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(g, value);
    }
}
