package be.submanifold.pentelive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import be.submanifold.pentelive.db.DbGames;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import org.junit.Test;

/** The game viewer's board for Go/Renju: board label from DbGames.baseName, sized replay. */
public class DbViewerGoBoardTest {

    @Test
    public void viewerBoardForGoNineByNine() {
        DBBoardView b = new DBBoardView(null, null);
        b.setGame(DbGames.baseName(22)); // a Speed Go (9x9) game
        b.showPosition(Arrays.asList(0, 1, 2, 81, 10));
        assertEquals(9, b.grid());
        assertEquals(0, b.abstractBoard[1][0]);
        assertEquals(1, b.whiteCaptures);
        assertEquals(" <b>1.</b> A9-B9 <b>2.</b> C9-PASS <b>3.</b> B8", b.moveListHtml());
    }

    @Test
    public void viewerBoardForRenju() {
        DBBoardView b = new DBBoardView(null, null);
        b.setGame(DbGames.baseName(81));
        b.showPosition(Arrays.asList(112, 113));
        assertEquals(15, b.grid());
        assertEquals(2, b.abstractBoard[7][7]);
        assertEquals(1, b.abstractBoard[8][7]);
    }

    @Test
    public void viewerSetsTheBoardFromTheCatalogName() throws IOException {
        String src = new String(Files.readAllBytes(Paths.get(
                "src/main/java/be/submanifold/pentelive/db/ui/DbGameViewerActivity.java")), StandardCharsets.UTF_8);
        assertTrue(src.contains("boardGame = DbGames.baseName(h.game);"));
        assertTrue(src.contains("board.setGame(boardGame);"));
    }
}
