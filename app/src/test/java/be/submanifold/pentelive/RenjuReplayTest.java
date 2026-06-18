package be.submanifold.pentelive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;

/** Characterization for replayRenjuGame: black-first, 15x15 decode. */
public class RenjuReplayTest {

    private static Game makeGame(String t) {
        return new Game("id","sid",t,"opponent","1500","white","5","Not Rated","false","1","0");
    }
    private static void setMoves(Game g, Integer... m) throws Exception {
        Field f = Game.class.getDeclaredField("mMovesList");
        f.setAccessible(true); f.set(g, new ArrayList<>(Arrays.asList(m)));
    }
    private static void setGridSize(Game g, int gs) throws Exception {
        Field f = Game.class.getDeclaredField("gridSize");
        f.setAccessible(true); f.setInt(g, gs);
    }
    private static byte[][] board(Game g) throws Exception {
        Field f = Game.class.getDeclaredField("abstractBoard");
        f.setAccessible(true); return (byte[][]) f.get(g);
    }

    @Test
    public void centerMoveIsBlackAndDecodedAt7x7() throws Exception {
        Game g = makeGame("Renju");
        setGridSize(g, 15);
        setMoves(g, 112, 113, 127);
        Method m = Game.class.getDeclaredMethod("replayRenjuGame", int.class);
        m.setAccessible(true); m.invoke(g, 3);
        byte[][] b = board(g);
        assertEquals("center 112 -> (row7,col7) BLACK", 2, b[7][7]);
        assertEquals("113 -> (row7,col8) WHITE", 1, b[7][8]);
        assertEquals("127 -> (row8,col7) BLACK", 2, b[8][7]);
    }

    @Test
    public void matchesGolden() throws Exception {
        Game g = makeGame("Renju");
        setGridSize(g, 15);
        setMoves(g, 112, 113, 127);
        Method m = Game.class.getDeclaredMethod("replayRenjuGame", int.class);
        m.setAccessible(true); m.invoke(g, 3);
        StringBuilder sb = new StringBuilder();
        byte[][] b = board(g);
        for (int i = 0; i < 15; i++) {
            for (int j = 0; j < 15; j++) { if (j>0) sb.append(' '); sb.append(b[i][j]); }
            sb.append('\n');
        }
        Path dir = null;
        for (Path p = Paths.get("").toAbsolutePath(); p != null; p = p.getParent()) {
            Path c = p.resolve("rules/src/test/resources/golden");
            if (Files.isDirectory(c)) { dir = c; break; }
        }
        if (dir == null) fail("golden dir not found");
        String expected = new String(Files.readAllBytes(dir.resolve("renju_blackfirst.txt")));
        assertEquals(expected, sb.toString());
    }
}
