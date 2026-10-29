package be.submanifold.pentelive.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class DbControllerTest {
    private static final int RED = 0xFFFF0000;
    private static final int GREEN = 0xFF00FF00;

    private FakeDbApi fake;
    private ManualExecutor io;
    private ManualExecutor main;
    private RecordingDbView view;

    @Before
    public void setUp() {
        fake = new FakeDbApi();
        io = new ManualExecutor();
        main = new ManualExecutor();
        view = new RecordingDbView();
        fake.reply(Arrays.asList(180), 200, FakeDbApi.stats(181, 40, 200, 80));
        fake.reply(Arrays.asList(180, 181), 200, FakeDbApi.stats(182, 30, 183, 90));
    }

    private DbController controller(int gameId, List<Integer> initial) {
        return new DbController(fake.api, io, main, gameId, initial);
    }

    /** Pente at [180], attached, initial load settled, view log cleared. */
    private DbController started() {
        DbController c = controller(1, Arrays.asList(180));
        c.attach(view);
        settle();
        view.log.clear();
        view.dots.clear();
        view.rows.clear();
        view.positions.clear();
        return c;
    }

    private void settle() {
        while (io.size() > 0 || main.size() > 0) {
            io.runAll();
            main.runAll();
        }
    }

    private static Map<Integer, Integer> map(int... kv) {
        Map<Integer, Integer> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put(kv[i], kv[i + 1]);
        return m;
    }

    @Test
    public void moveClearsThenFetches() {
        DbController c = started();
        c.onBoardMove(181);
        assertEquals(Arrays.asList("clearResults", "showPosition"), view.log.subList(0, 2));
        settle();
        assertEquals(Arrays.asList(180, 181), fake.lastStatsMoves());
        assertEquals(Arrays.asList(180, 181), view.positions.get(0));
    }

    @Test
    public void aiMoveGoesThroughHistory() {
        DbController c = started();
        int before = fake.statsCalls.size();
        c.onBoardMove(181);
        settle();
        assertTrue(c.back());
        settle();
        assertTrue(c.forward());
        settle();
        assertEquals(Arrays.asList(180, 181), c.moves());
        assertEquals(before + 3, fake.statsCalls.size());
        assertEquals(Arrays.asList(180, 181), fake.lastStatsMoves());
    }

    @Test
    public void staleStatsResponseIsDropped() {
        DbController c = started();
        c.onBoardMove(181); // A
        assertTrue(c.back()); // B
        assertEquals(2, io.size());
        io.run(1); // B io
        main.runAll(); // B main
        io.run(0); // A io
        main.runAll(); // A main
        assertEquals(1, view.count("showDots"));
        assertEquals(map(181, RED, 200, GREEN), view.dots.get(0));
        assertEquals(1, view.count("showMoves"));
    }

    @Test
    public void staleErrorIsDropped() {
        fake.reply(Arrays.asList(180, 181), 500, "{\"error\":{\"code\":\"internal\",\"message\":\"x\"}}");
        DbController c = started();
        c.onBoardMove(181);
        c.back();
        io.run(1);
        main.runAll();
        io.run(0);
        main.runAll();
        assertEquals(0, view.count("showError"));
        assertEquals(1, view.count("showDots"));
        assertEquals(map(181, RED, 200, GREEN), view.dots.get(0));
    }

    @Test
    public void staleUnauthorizedIsDropped() {
        fake.reply(Arrays.asList(180, 181), 401, "{\"error\":{\"code\":\"unauthorized\",\"message\":\"x\"}}");
        DbController c = started();
        c.onBoardMove(181);
        c.back();
        io.run(1);
        main.runAll();
        io.run(0);
        main.runAll();
        assertEquals(0, view.count("showLoginPrompt"));
        assertEquals(1, view.count("showDots"));
    }

    @Test
    public void backPushesRedoAndForwardReplays() {
        DbController c = started();
        c.onBoardMove(181);
        assertTrue(c.back());
        assertEquals(Arrays.asList(180), c.moves());
        assertTrue(c.forward());
        assertEquals(Arrays.asList(180, 181), c.moves());
        assertFalse(c.forward());
    }

    @Test
    public void dotsExcludeOccupied() {
        fake.reply(Arrays.asList(180), 200, FakeDbApi.stats(180, 0, 181, 40, 200, 80));
        DbController c = controller(1, Arrays.asList(180));
        c.attach(view);
        settle();
        assertEquals(1, view.dots.size());
        assertFalse(view.dots.get(0).containsKey(180));
        assertEquals(map(181, RED, 200, GREEN), view.dots.get(0));
        for (DbController.MoveRow r : view.rows.get(0)) assertTrue(r.move.move != 180);
    }

    @Test
    public void rowsCarryTheDotColour() {
        DbController c = controller(1, Arrays.asList(180));
        c.attach(view);
        settle();
        List<DbController.MoveRow> rows = view.rows.get(0);
        assertEquals(2, rows.size());
        assertEquals(200, rows.get(0).move.move); // higher Wilson first
        assertEquals(GREEN, rows.get(0).argb);
        assertEquals(RED, rows.get(1).argb);
    }

    @Test
    public void unauthorizedShowsLogin() {
        fake.reply(Arrays.asList(180), 401, "{\"error\":{\"code\":\"unauthorized\",\"message\":\"x\"}}");
        DbController c = controller(1, Arrays.asList(180));
        c.attach(view);
        settle();
        assertEquals(1, view.count("showLoginPrompt"));
        assertEquals(0, view.count("showDots"));
    }

    @Test
    public void subscriptionRequiredShowsGate() {
        fake.reply(Arrays.asList(180), 403, "{\"error\":{\"code\":\"subscription_required\",\"message\":\"x\"}}");
        DbController c = controller(1, Arrays.asList(180));
        c.attach(view);
        settle();
        assertEquals(1, view.count("showSubscribeGate"));
        assertEquals(0, view.count("showDots"));
    }

    @Test
    public void serverErrorShowsRetryAndNoStaleRows() {
        fake.reply(Arrays.asList(180, 181), 500, "{\"error\":{\"code\":\"internal\",\"message\":\"x\"}}");
        DbController c = started();
        c.onBoardMove(181);
        settle();
        assertEquals(Collections.singletonList(DbController.Endpoint.STATS), view.errors);
        assertEquals(0, view.count("showMoves"));
        assertEquals(0, view.count("showDots"));
        assertEquals("clearResults", view.log.get(0));
    }

    @Test
    public void retryRefetches() {
        fake.reply(Arrays.asList(180), 500, "{}");
        DbController c = controller(1, Arrays.asList(180));
        c.attach(view);
        settle();
        fake.reply(Arrays.asList(180), 200, FakeDbApi.stats(181, 40, 200, 80));
        int before = fake.statsCalls.size();
        c.retry(DbController.Endpoint.STATS);
        settle();
        assertEquals(before + 1, fake.statsCalls.size());
        assertEquals(1, view.count("showDots"));
    }

    @Test
    public void blockedShowsBlockedAndNoDots() {
        fake.reply(Arrays.asList(180), 200, "{\"blocked\":true,\"nextMoves\":[{\"move\":181,\"games\":5,\"wins\":2,\"winPct\":40}]}");
        DbController c = controller(1, Arrays.asList(180));
        c.attach(view);
        settle();
        assertEquals(1, view.count("showBlocked"));
        assertEquals(0, view.count("showDots"));
        assertEquals(0, view.count("showMoves"));
    }

    @Test
    public void connect6StatsAreRequested() {
        DbController c = controller(13, Arrays.asList(180));
        c.attach(view);
        settle();
        view.log.clear();
        view.dots.clear();
        c.onBoardMove(181);
        settle();
        assertEquals(Arrays.asList(180, 181), fake.lastStatsMoves());
        assertEquals(Integer.valueOf(13), fake.statsGames.get(fake.statsGames.size() - 1));
        assertEquals(1, view.count("showMoves"));
        assertEquals(map(182, RED, 183, GREEN), view.dots.get(0));
    }

    @Test
    public void unsupportedGameSendsNothing() {
        DbController c = controller(0, Collections.<Integer>emptyList());
        c.attach(view);
        settle();
        assertEquals(Collections.singletonList("showUnsupported"), view.log);
        assertTrue(fake.statsCalls.isEmpty());
        c.onBoardMove(181);
        assertFalse(c.back());
        settle();
        assertTrue(fake.statsCalls.isEmpty());
    }

    @Test
    public void goAndRenjuAreUnsupported() {
        for (int id : new int[] {19, 21, 23, 31, 81}) { // Go 19x19/9x9/13x13, Renju, TB Renju
            RecordingDbView v = new RecordingDbView();
            DbController c = controller(id, Collections.<Integer>emptyList());
            c.attach(v);
            settle();
            assertEquals(Collections.singletonList("showUnsupported"), v.log);
        }
        assertTrue(fake.statsCalls.isEmpty());
    }

    @Test
    public void setGameResetsToSeedAndRefetches() {
        DbController c = started();
        c.onBoardMove(181);
        settle();
        c.setGame(7); // D-Pente: empty seed
        settle();
        assertEquals(7, c.gameId());
        assertEquals(Collections.<Integer>emptyList(), c.moves());
        assertEquals(Collections.<Integer>emptyList(), fake.lastStatsMoves());
        c.onBoardMove(180);
        assertEquals(Arrays.asList(180), c.moves());
        c.setGame(1);
        assertEquals(Arrays.asList(180), c.moves());
        c.onBoardMove(181); // seed list must be mutable through the history
        assertEquals(Arrays.asList(180, 181), c.moves());
    }

    @Test
    public void emptyInitialMovesUseSeed() {
        DbController c = controller(1, Collections.<Integer>emptyList());
        assertEquals(Arrays.asList(180), c.moves());
    }

    @Test
    public void noViewCallsWhileDetached() {
        DbController c = started();
        c.onBoardMove(181);
        int sizeAtDetach = view.log.size();
        c.detach();
        settle();
        assertEquals(sizeAtDetach, view.log.size());
    }

    private static String bigStats() {
        int[] kv = new int[50];
        for (int i = 0; i < 25; i++) {
            kv[2 * i] = 200 + i;
            kv[2 * i + 1] = 10 + i * 3;
        }
        return FakeDbApi.stats(kv);
    }

    @Test
    public void showCountChangesRowsAndDomain() {
        fake.reply(Arrays.asList(180), 200, bigStats());
        DbController c = controller(1, Arrays.asList(180));
        c.attach(view);
        settle();
        assertEquals(20, view.rows.get(0).size());
        assertTrue(view.dots.get(0).size() <= 20);
        int calls = fake.statsCalls.size();
        c.setShowCount(5);
        assertEquals(0, io.size());
        assertEquals(calls, fake.statsCalls.size());
        List<DbController.MoveRow> last = view.rows.get(view.rows.size() - 1);
        assertEquals(5, last.size());
        Map<Integer, Integer> d = view.dots.get(view.dots.size() - 1);
        assertEquals(5, d.size());
        for (DbController.MoveRow r : last) assertEquals(Integer.valueOf(r.argb), d.get(r.move.move));
        // domain recomputed over the 5 shown: lowest winPct is red, highest is green (heat uses raw winPct)
        DbController.MoveRow lo = last.get(0);
        DbController.MoveRow hi = last.get(0);
        for (DbController.MoveRow r : last) {
            if (r.move.winPct < lo.move.winPct) lo = r;
            if (r.move.winPct > hi.move.winPct) hi = r;
        }
        assertEquals(RED, lo.argb);
        assertEquals(GREEN, hi.argb);
        c.setShowCount(0);
        assertEquals(25, view.rows.get(view.rows.size() - 1).size());
    }

    @Test
    public void rowAndDotColoursMatch() {
        fake.reply(Arrays.asList(180), 200, bigStats());
        DbController c = controller(1, Arrays.asList(180));
        c.attach(view);
        settle();
        Map<Integer, Integer> d = view.dots.get(0);
        for (DbController.MoveRow r : view.rows.get(0)) assertEquals(Integer.valueOf(r.argb), d.get(r.move.move));
    }

    @Test
    public void rowTapPlaysMove() {
        DbController c = started();
        c.playCandidate(181);
        assertEquals(181, (int) c.moves().get(c.moves().size() - 1));
    }

    @Test
    public void resultWhileDetachedIsRenderedOnAttach() {
        DbController c = started();
        c.onBoardMove(181);
        c.detach();
        settle();
        int calls = fake.statsCalls.size();
        RecordingDbView newView = new RecordingDbView();
        c.attach(newView);
        assertEquals(1, newView.count("showPosition"));
        assertEquals(Arrays.asList(180, 181), newView.positions.get(0));
        assertEquals(1, newView.count("showDots"));
        assertEquals(map(182, RED, 183, GREEN), newView.dots.get(0));
        assertEquals(calls, fake.statsCalls.size());
        assertEquals(0, io.size());
    }
}
