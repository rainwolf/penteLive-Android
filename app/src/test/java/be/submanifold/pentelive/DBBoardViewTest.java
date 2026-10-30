package be.submanifold.pentelive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import be.submanifold.pentelive.db.DbGames;
import org.junit.Test;

/**
 * DBBoardView listener API (Task 10). The android.jar stubs return defaults
 * (returnDefaultValues = true), so the View can be built with a null context;
 * only the pure model side (move list, abstractBoard, captures, listener) is checked.
 */
public class DBBoardViewTest {

    private static DBBoardView newBoard() {
        return new DBBoardView(null, null);
    }

    private static final class Recorder implements DBBoardView.BoardListener {
        final List<String> calls = new ArrayList<>();

        @Override
        public void onMovePlayed(int cell) {
            calls.add("played " + cell);
        }

        @Override
        public void onAiMove(int cell) {
            calls.add("ai " + cell);
        }
    }

    @Test
    public void showPositionCopiesTheList() {
        DBBoardView b = newBoard();
        b.setGame("Pente");
        List<Integer> moves = Collections.unmodifiableList(Arrays.asList(180, 181));
        b.showPosition(moves);
        assertEquals(moves, b.getMovesList());
        assertNotSame(moves, b.getMovesList());
        assertEquals(1, b.abstractBoard[9][9]);
        assertEquals(2, b.abstractBoard[10][9]);
    }

    @Test
    public void showPositionReplacesTheBoard() {
        DBBoardView b = newBoard();
        b.setGame("Pente");
        b.showPosition(Arrays.asList(180, 181, 0));
        b.showPosition(Collections.singletonList(180));
        assertEquals(1, b.abstractBoard[9][9]);
        assertEquals(0, b.abstractBoard[10][9]);
        assertEquals(0, b.abstractBoard[0][0]);
    }

    @Test
    public void connect6ColoursFollowTurns() {
        DBBoardView b = newBoard();
        b.setGame("Connect6");
        // Connect6: P1 places index 0, P2 indices 1-2, P1 indices 3-4.
        b.showPosition(Arrays.asList(180, 0, 1, 2, 3));
        assertEquals(1, b.abstractBoard[9][9]);
        assertEquals(2, b.abstractBoard[0][0]);
        assertEquals(2, b.abstractBoard[1][0]);
        assertEquals(1, b.abstractBoard[2][0]);
        assertEquals(1, b.abstractBoard[3][0]);
    }

    @Test
    public void connect6HasNoCaptures() {
        DBBoardView b = newBoard();
        b.setGame("Connect6");
        // Row y=0: X at 0 (idx3, P1), O at 1,2 (idx1,2, P2), X at 3 (idx4, P1): a pente capture shape.
        b.showPosition(Arrays.asList(180, 1, 2, 0, 3));
        assertEquals(2, b.abstractBoard[1][0]);
        assertEquals(2, b.abstractBoard[2][0]);
        assertEquals(0, b.whiteCaptures);
        assertEquals(0, b.blackCaptures);
    }

    @Test
    public void penteCaptureCountsComeFromReplay() {
        DBBoardView b = newBoard();
        b.setGame("Pente");
        // P1 180 (K10), P2 181, P1 0, P2 182, P1 183 captures 181/182 (two black stones).
        b.showPosition(Arrays.asList(180, 181, 0, 182, 183));
        assertEquals(0, b.abstractBoard[10][9]);
        assertEquals(0, b.abstractBoard[11][9]);
        assertEquals(2, b.blackCaptures);
        assertEquals(0, b.whiteCaptures);
    }

    @Test
    public void showPositionBeforeGameStoresWithoutReplay() {
        DBBoardView b = newBoard();
        b.showPosition(Arrays.asList(180, 0));
        assertEquals(Arrays.asList(180, 0), b.getMovesList());
        assertEquals(0, b.abstractBoard[0][0]);
    }

    @Test
    public void unsupportedGameSkipsReplay() {
        DBBoardView b = newBoard();
        b.setGame("Tombstone");
        b.showPosition(Arrays.asList(180, 0));
        assertEquals(Arrays.asList(180, 0), b.getMovesList());
        assertEquals(0, b.abstractBoard[0][0]);
    }

    @Test
    public void placementGoesToListenerAndBoardIsUnchanged() {
        DBBoardView b = newBoard();
        b.setGame("Pente");
        b.showPosition(Collections.singletonList(180));
        Recorder r = new Recorder();
        b.setBoardListener(r);
        b.reportPlacement(0);
        assertEquals(Collections.singletonList("played 0"), r.calls);
        assertEquals(Collections.singletonList(180), b.getMovesList());
        assertEquals(0, b.abstractBoard[0][0]);
        assertEquals(-1, b.playedMove);
    }

    @Test
    public void placementWithoutListenerDoesNothing() {
        DBBoardView b = newBoard();
        b.setGame("Pente");
        b.reportPlacement(0);
        assertEquals(Collections.singletonList(180), b.getMovesList());
    }

    @Test
    public void aiMoveGoesToListenerOnly() {
        DBBoardView b = newBoard();
        b.setGame("Pente");
        Recorder r = new Recorder();
        b.setBoardListener(r);
        b.reportAiMove(5);
        assertEquals(Collections.singletonList("ai 5"), r.calls);
        assertEquals(Collections.singletonList(180), b.getMovesList());
        assertEquals(0, b.abstractBoard[5][0]);
    }

    @Test
    public void dotsDrawOnlyWhenVisibleAndSet() {
        DBBoardView b = newBoard();
        assertFalse(b.drawsDots());
        b.setCandidateDots(Collections.singletonMap(0, 0xFF00FF00));
        assertTrue(b.drawsDots());
        b.setDotsVisible(false);
        assertFalse(b.drawsDots());
        b.setDotsVisible(true);
        assertTrue(b.drawsDots());
        b.setCandidateDots(null);
        assertFalse(b.drawsDots());
    }

    // ---- Go / Renju ----

    @Test
    public void goNineByNineReplaysWithCaptures() {
        DBBoardView b = newBoard();
        b.setGame("Go (9x9)");
        b.showPosition(Arrays.asList(0, 1, 2, 40, 10)); // B A9, W B9, B C9, W E5, B B8
        assertEquals(9, b.grid());
        assertEquals(0, b.abstractBoard[1][0]); // [x][y]: captured white B9
        assertEquals(2, b.abstractBoard[0][0]);
        assertEquals(2, b.abstractBoard[2][0]);
        assertEquals(1, b.abstractBoard[4][4]);
        assertEquals(2, b.abstractBoard[1][1]);
        assertEquals(1, b.whiteCaptures);
        assertEquals(0, b.blackCaptures);
    }

    @Test
    public void goPassesReplayWithoutError() {
        DBBoardView b = newBoard();
        b.setGame("Speed Go (9x9)");
        b.showPosition(Arrays.asList(40, 81, 41));
        assertEquals(2, b.abstractBoard[4][4]);
        assertEquals(2, b.abstractBoard[5][4]);
        assertEquals(Arrays.asList(40, 81, 41), b.getMovesList());
    }

    @Test
    public void go13AndGo19Sizes() {
        DBBoardView b = newBoard();
        b.setGame("Go (13x13)");
        assertEquals(13, b.grid());
        b.showPosition(Arrays.asList(168));
        assertEquals(2, b.abstractBoard[12][12]);
        b.setGame("Go");
        assertEquals(19, b.grid());
        b.showPosition(Arrays.asList(360));
        assertEquals(2, b.abstractBoard[18][18]);
        assertEquals(0, b.abstractBoard[12][12]);
    }

    @Test
    public void renjuFirstStoneIsBlack() {
        DBBoardView b = newBoard();
        b.setGame("Renju");
        assertEquals(15, b.grid());
        b.showPosition(Arrays.asList(112, 113, 224));
        assertEquals(2, b.abstractBoard[7][7]);
        assertEquals(1, b.abstractBoard[8][7]);
        assertEquals(2, b.abstractBoard[14][14]);
    }

    @Test
    public void switchingFromGoToPenteClearsTheBoard() {
        DBBoardView b = newBoard();
        b.setGame("Go (9x9)");
        b.showPosition(Arrays.asList(0, 80));
        b.setGame("Pente");
        b.showPosition(Arrays.asList(180));
        assertEquals(19, b.grid());
        assertEquals(0, b.abstractBoard[0][0]);
        assertEquals(0, b.abstractBoard[8][8]);
        assertEquals(1, b.abstractBoard[9][9]);
    }

    @Test
    public void goPlacementMapsToNineByNineCell() {
        DBBoardView b = newBoard();
        b.setGame("Go (9x9)");
        b.showPosition(Collections.<Integer>emptyList());
        assertEquals(40, b.placementCell(450, 450, 900));
        assertEquals(0, b.placementCell(1, 1, 900));
        assertEquals(80, b.placementCell(900, 900, 900));
        assertEquals(-1, b.placementCell(901, 10, 900));
        b.showPosition(Arrays.asList(40));
        assertEquals(-1, b.placementCell(450, 450, 900)); // occupied
        Recorder r = new Recorder();
        b.setBoardListener(r);
        b.reportPlacement(b.placementCell(150, 250, 900));
        assertEquals(Collections.singletonList("played 19"), r.calls);
    }

    @Test
    public void pentePlacementUnchanged() {
        DBBoardView b = newBoard();
        b.setGame("Pente");
        b.showPosition(Collections.singletonList(180));
        assertEquals(181, b.placementCell(390f * 10.5f / 19, 390f * 9.5f / 19, 390));
        assertEquals(-1, b.placementCell(195, 195, 390)); // centre is occupied
        assertEquals(0, b.placementCell(0, 0, 390));
    }

    @Test
    public void goKoCellIsBlocked() {
        DBBoardView b = newBoard();
        b.setGame("Go (9x9)");
        b.showPosition(Arrays.asList(9, 2, 1, 20, 19, 12, 80, 10, 11));
        assertEquals(-1, b.abstractBoard[1][1]); // cell 10 = x 1, y 1
        assertEquals(-1, b.placementCell(150, 150, 900));
        b.showPosition(Arrays.asList(9, 2, 1, 20, 19, 12, 80, 10, 11, 70));
        assertEquals(0, b.abstractBoard[1][1]);
    }

    @Test
    public void placementColourPerGame() {
        DBBoardView b = newBoard();
        b.setGame("Go (9x9)");
        b.showPosition(Collections.<Integer>emptyList());
        assertEquals(2, b.placementColour());
        b.showPosition(Arrays.asList(40));
        assertEquals(1, b.placementColour());
        b.setGame("Renju");
        b.showPosition(Arrays.asList(112));
        assertEquals(1, b.placementColour());
        b.setGame("Pente");
        b.showPosition(Arrays.asList(180));
        assertEquals(2, b.placementColour());
        b.setGame("Connect6");
        b.showPosition(Arrays.asList(180, 181));
        assertEquals(2, b.placementColour());
        b.showPosition(Arrays.asList(180, 181, 182));
        assertEquals(1, b.placementColour());
    }

    @Test
    public void notationUsesBoardLabelsAndPass() {
        DBBoardView b = newBoard();
        b.setGame("Go (9x9)");
        b.showPosition(Arrays.asList(40, 81, 41));
        assertEquals(" <b>1.</b> E5-PASS <b>2.</b> F5", b.moveListHtml());
        b.setGame("Pente");
        b.showPosition(Arrays.asList(180, 181));
        assertEquals(" <b>1.</b> K10-L10", b.moveListHtml());
        b.setGame("Connect6");
        b.showPosition(Arrays.asList(180, 181, 182));
        assertEquals("<b>1.</b> K10 - L10-M10", b.moveListHtml());
    }

    @Test
    public void goAndRenjuBoardColoursMatchTheCatalog() {
        DBBoardView b = newBoard();
        assertEquals(DbGames.boardArgb(19), b.goColor);
        assertEquals(DbGames.boardArgb(31), b.renjuColor);
    }

    @Test
    public void sourceHasNoParentCastsOrSelfMutation() throws IOException {
        Path p = Paths.get("src/main/java/be/submanifold/pentelive/DBBoardView.java");
        List<String> offending = new ArrayList<>();
        for (String line : Files.readAllLines(p, StandardCharsets.UTF_8)) {
            String code = line.trim();
            if (code.startsWith("//")) {
                continue;
            }
            // Telling a scrolling parent not to intercept the aim-drag is allowed: no cast, no
            // reach into the activity.
            code = code.replace("getParent() != null", "")
                    .replace("getParent().requestDisallowInterceptTouchEvent(true)", "");
            if (code.contains("getParent()") || code.contains("findViewById")
                    || code.contains("DatabaseActivity)") || code.contains("movesList.add")
                    || code.contains("searchResults = null")) {
                offending.add(code);
            }
        }
        assertEquals(Collections.emptyList(), offending);
    }

    // ---- selected-move flash ----

    /** Counts animator starts/stops instead of creating a real ValueAnimator (the stubs have none). */
    private static final class FlashBoard extends DBBoardView {
        int starts;
        int stops;

        FlashBoard() {
            super(null, null);
        }

        @Override
        void startFlash() {
            starts++;
        }

        @Override
        void stopFlash() {
            stops++;
            super.stopFlash();
        }

        void detach() {
            onDetachedFromWindow();
        }
    }

    private static FlashBoard flashBoard(boolean animationsOn) {
        FlashBoard b = new FlashBoard();
        b.setGame("Pente");
        b.setAnimationsEnabledForTest(() -> animationsOn);
        return b;
    }

    @Test
    public void selectedCellFlashesWhenAnimationsAreOn() {
        FlashBoard b = flashBoard(true);
        b.setSelectedCell(200, 0xFFFF0000);
        assertEquals(200, b.selectedCell());
        assertTrue(b.isFlashing());
        assertEquals(1, b.starts);
    }

    @Test
    public void selectedCellIsAStaticRingWhenAnimationsAreOff() {
        FlashBoard b = flashBoard(false);
        b.setSelectedCell(200, 0xFFFF0000);
        assertEquals(200, b.selectedCell());
        assertFalse(b.isFlashing());
        assertEquals(0, b.starts);
    }

    @Test
    public void clearingTheSelectionStopsTheFlash() {
        FlashBoard b = flashBoard(true);
        b.setSelectedCell(200, 0xFFFF0000);
        int stopsBefore = b.stops;
        b.clearSelectedCell();
        assertEquals(-1, b.selectedCell());
        assertFalse(b.isFlashing());
        assertEquals(stopsBefore + 1, b.stops);
    }

    @Test
    public void onlyOneCellFlashesAtATime() {
        FlashBoard b = flashBoard(true);
        b.setSelectedCell(200, 0xFFFF0000);
        b.setSelectedCell(201, 0xFF00FF00);
        assertEquals(201, b.selectedCell());
        assertEquals(2, b.starts);
        assertTrue("the first flash was stopped before the second started", b.stops >= 2);
    }

    @Test
    public void detachingFromTheWindowStopsTheFlash() {
        FlashBoard b = flashBoard(true);
        b.setSelectedCell(200, 0xFFFF0000);
        int stopsBefore = b.stops;
        b.detach();
        assertEquals(-1, b.selectedCell());
        assertFalse(b.isFlashing());
        assertEquals(stopsBefore + 1, b.stops);
    }

    @Test
    public void flashUsesTheSystemAnimatorScaleSetting() throws IOException {
        String src = new String(Files.readAllBytes(Paths.get("src/main/java/be/submanifold/pentelive/DBBoardView.java")),
                StandardCharsets.UTF_8);
        assertTrue(src.contains("Settings.Global.ANIMATOR_DURATION_SCALE"));
    }
}
