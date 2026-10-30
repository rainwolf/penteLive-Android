package be.submanifold.pentelive.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public class GameViewerModelTest {
    private static DbResult<DbDtos.GameDetail> detail(long gid, int game, int... moves) {
        DbDtos.GameDetail d = new DbDtos.GameDetail();
        d.header = new DbDtos.GameHeader();
        d.header.gid = gid;
        d.header.game = game;
        d.moves = moves;
        return DbResult.ok(d);
    }

    @Test
    public void mismatchedGidIsIgnored() {
        GameViewerModel m = new GameViewerModel(50000000000123L);
        assertFalse(m.accept(detail(123L, 1, 180, 181)));
        assertNull(m.header());
        assertTrue(m.accept(detail(50000000000123L, 1, 180, 181)));
        assertEquals(50000000000123L, m.header().gid);
    }

    @Test
    public void blockedOrNullHeaderIsRejected() {
        DbResult<DbDtos.GameDetail> blocked = detail(7, 1, 180);
        blocked.value.blocked = true;
        DbDtos.GameDetail noHeader = new DbDtos.GameDetail();
        DbResult<DbDtos.GameDetail> nullHeader = DbResult.ok(noHeader);

        GameViewerModel m = new GameViewerModel(7);
        assertFalse(m.accept(blocked));
        assertFalse(m.accept(nullHeader));
        assertTrue(GameViewerModel.isBlocked(blocked));
        assertFalse(GameViewerModel.isBlocked(nullHeader));
        assertNull(m.header());
    }

    @Test
    public void blocked403IsRejectedAndBlocked() {
        DbResult<DbDtos.GameDetail> r = DbResult.of(DbResult.Kind.BLOCKED);
        assertFalse(new GameViewerModel(7).accept(r));
        assertTrue(GameViewerModel.isBlocked(r));
        assertFalse(GameViewerModel.isBlocked(DbResult.<DbDtos.GameDetail>of(DbResult.Kind.FAILED)));
        assertFalse(GameViewerModel.isBlocked(DbResult.<DbDtos.GameDetail>of(DbResult.Kind.UNAUTHORIZED)));
    }

    @Test
    public void failedResultIsRejected() {
        assertFalse(new GameViewerModel(7).accept(DbResult.<DbDtos.GameDetail>of(DbResult.Kind.FAILED)));
    }

    @Test
    public void acceptStartsAtEnd() {
        GameViewerModel m = new GameViewerModel(7);
        assertTrue(m.accept(detail(7, 1, 180, 181, 182)));
        assertEquals(3, m.cursor());
        assertEquals(Arrays.asList(180, 181, 182), m.movesUpTo());
    }

    @Test
    public void backForwardAndBounds() {
        GameViewerModel m = new GameViewerModel(7);
        m.accept(detail(7, 1, 180, 181, 182));
        assertFalse(m.forward());
        assertTrue(m.back());
        assertEquals(Arrays.asList(180, 181), m.movesUpTo());
        assertTrue(m.back());
        assertTrue(m.back());
        assertEquals(0, m.cursor());
        assertEquals(Collections.<Integer>emptyList(), m.movesUpTo());
        assertFalse(m.back());
        assertTrue(m.forward());
        assertEquals(Arrays.asList(180), m.movesUpTo());
    }

    @Test
    public void beforeAcceptNothingMoves() {
        GameViewerModel m = new GameViewerModel(7);
        assertFalse(m.back());
        assertFalse(m.forward());
        assertEquals(0, m.cursor());
        assertTrue(m.movesUpTo().isEmpty());
    }

    @Test
    public void jumpClamps() {
        GameViewerModel m = new GameViewerModel(7);
        m.accept(detail(7, 1, 180, 181, 182));
        m.jumpTo(1);
        assertEquals(1, m.cursor());
        m.jumpTo(-4);
        assertEquals(0, m.cursor());
        m.jumpTo(99);
        assertEquals(3, m.cursor());
    }

    @Test
    public void connect6FollowsStepsByTurn() {
        GameViewerModel m = new GameViewerModel(7);
        m.accept(detail(7, 13, 180, 181, 182, 183, 184));
        m.back();
        int expected = DbGames.stepsByTurn(13) ? 3 : 4;
        assertEquals(expected, m.cursor());
        m.forward();
        assertEquals(5, m.cursor());
    }

    @Test
    public void unknownGameStepsOneStone() {
        GameViewerModel m = new GameViewerModel(7);
        assertTrue(m.accept(detail(7, 999, 180, 181)));
        assertTrue(m.back());
        assertEquals(1, m.cursor());
    }

    @Test
    public void allMovesIgnoresCursor() {
        GameViewerModel m = new GameViewerModel(7);
        m.accept(detail(7, 1, 180, 181, 182));
        m.jumpTo(1);
        assertEquals(java.util.Arrays.asList(180, 181, 182), m.allMoves());
        assertEquals(java.util.Arrays.asList(180), m.movesUpTo());
    }

    @Test
    public void rejectionMapsSubscriptionRequiredToItsOwnOutcome() {
        assertEquals(GameViewerModel.Rejection.SUBSCRIPTION_REQUIRED, GameViewerModel.rejection(
                DbResult.<DbDtos.GameDetail>of(DbResult.Kind.SUBSCRIPTION_REQUIRED)));
        assertEquals(GameViewerModel.Rejection.BLOCKED,
                GameViewerModel.rejection(DbResult.<DbDtos.GameDetail>of(DbResult.Kind.BLOCKED)));
        assertEquals(GameViewerModel.Rejection.UNAUTHORIZED,
                GameViewerModel.rejection(DbResult.<DbDtos.GameDetail>of(DbResult.Kind.UNAUTHORIZED)));
        assertEquals(GameViewerModel.Rejection.FAILED,
                GameViewerModel.rejection(DbResult.<DbDtos.GameDetail>of(DbResult.Kind.FAILED)));
        assertEquals(GameViewerModel.Rejection.FAILED, GameViewerModel.rejection(detail(8, 1, 180)));
    }

    @Test
    public void subscription403FromApiIsNotARetry() {
        DbApiTest.Fake f = new DbApiTest.Fake(403, "{\"error\":{\"code\":\"subscription_required\"}}");
        DbResult<DbDtos.GameDetail> r = new DbApi(f).game(7);
        assertFalse(new GameViewerModel(7).accept(r));
        assertEquals(GameViewerModel.Rejection.SUBSCRIPTION_REQUIRED, GameViewerModel.rejection(r));
    }

    // ---- Go / Renju games in the viewer (DbGameViewerActivity.renderGame/showPosition composition) ----

    @Test
    public void goNineByNineGameStepsThroughPassesWithCaptures() {
        // B A9, W B9, B C9, W pass, B B8 captures W B9; then two passes.
        GameViewerModel m = new GameViewerModel(9);
        assertTrue(m.accept(detail(9, 21, 0, 1, 2, 81, 10, 81, 81)));
        assertTrue(DbGames.isSupported(m.header().game));
        assertEquals("Go (9x9)", DbGames.baseName(m.header().game));
        assertEquals(7, m.moveCount());
        m.jumpTo(5);
        assertEquals(Arrays.asList(0, 1, 2, 81, 10), m.movesUpTo());
        CaptureIcons.Counts caps = CaptureIcons.of(m.header().game, m.movesUpTo());
        assertTrue(caps.visible);
        assertEquals(1, caps.p1Count);
        java.util.List<MoveGrid.Cell> cells = MoveGrid.cells(m.header().game, m.allMoves(), m.cursor());
        assertEquals("PASS", cells.get(3).label);
        assertEquals("B8", cells.get(4).label);
        assertEquals("PASS", cells.get(6).label);
        assertTrue(m.back());
        assertEquals(0, CaptureIcons.of(m.header().game, m.movesUpTo()).p1Count);
    }

    @Test
    public void turnBasedRenjuGameIsViewable() {
        GameViewerModel m = new GameViewerModel(10);
        assertTrue(m.accept(detail(10, 81, 112, 113, 98)));
        assertTrue(DbGames.isSupported(81));
        assertEquals("Renju", DbGames.baseName(81));
        assertFalse(CaptureIcons.of(81, m.allMoves()).visible);
        assertEquals("H8", MoveGrid.cells(81, m.allMoves(), 3).get(0).label);
    }
}
