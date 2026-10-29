package be.submanifold.pentelive.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonObject;
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
    private InMemoryKeyValueStore kv;
    private DbFilterStore store;

    @Before
    public void setUp() {
        fake = new FakeDbApi();
        io = new ManualExecutor();
        main = new ManualExecutor();
        view = new RecordingDbView();
        kv = new InMemoryKeyValueStore();
        store = new DbFilterStore(kv, new DbFilterStore.LegacyLabels("Both", "Yes", "Live only", "Turn-based only"));
        store.save(DbFilters.defaults());
        fake.reply(Arrays.asList(180), 200, FakeDbApi.stats(181, 40, 200, 80));
        fake.reply(Arrays.asList(180, 181), 200, FakeDbApi.stats(182, 30, 183, 90));
    }

    private DbController controller(int gameId, List<Integer> initial) {
        return new DbController(fake.api, io, main, store, gameId, initial);
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
        assertEquals(4, io.size()); // stats + games per position
        io.run(2); // B stats io
        main.runAll(); // B main
        io.run(0); // A stats io
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
        io.run(2);
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
        io.run(2);
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
    public void changingGameResetsToSeedAndRefetches() {
        DbController c = started();
        c.onBoardMove(181);
        settle();
        c.applyFilters(c.appliedFilters().withGame(7)); // D-Pente: empty seed
        settle();
        assertEquals(7, c.gameId());
        assertEquals(Collections.<Integer>emptyList(), c.moves());
        assertEquals(Collections.<Integer>emptyList(), fake.lastStatsMoves());
        c.onBoardMove(180);
        assertEquals(Arrays.asList(180), c.moves());
        c.applyFilters(c.appliedFilters().withGame(1));
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

    private static List<Integer> m180() {
        return Arrays.asList(180);
    }

    private DbController withGames(int total, int firstPage) {
        fake.gamesReply(m180(), 0, 200, FakeDbApi.gamesPage(total, 1000, firstPage));
        DbController c = controller(1, m180());
        c.attach(view);
        settle();
        return c;
    }

    @Test
    public void positionChangeFetchesGamesOffset0() {
        withGames(60, 25);
        assertEquals(Collections.singletonList(0), fake.gamesOffsets);
        assertEquals(m180(), fake.gamesCalls.get(0));
        assertEquals(1, view.games.size());
        assertEquals(25, view.games.get(0).size());
        assertEquals(Integer.valueOf(60), view.totals.get(0));
        assertTrue(view.hasMores.get(0));
        fake.gamesOffsets.clear();
        view.games.clear();
        DbController c = controller(13, m180()); // Connect6 fetches games too
        c.attach(new RecordingDbView());
        settle();
        assertEquals(Collections.singletonList(0), fake.gamesOffsets);
    }

    @Test
    public void loadMoreAppendsOffset25() {
        DbController c = withGames(60, 25);
        fake.gamesReply(m180(), 25, 200, FakeDbApi.gamesPage(60, 2000, 25));
        c.loadMoreGames();
        settle();
        assertEquals(25, fake.lastGamesOffset());
        List<DbDtos.GameHeader> shown = view.games.get(view.games.size() - 1);
        assertEquals(50, shown.size());
        assertEquals(1000, shown.get(0).gid);
        assertEquals(2024, shown.get(49).gid);
        assertTrue(view.hasMores.get(view.hasMores.size() - 1));
    }

    @Test
    public void stopsAtTotal() {
        DbController c = withGames(30, 25);
        fake.gamesReply(m180(), 25, 200, FakeDbApi.gamesPage(30, 2000, 5));
        c.loadMoreGames();
        settle();
        assertEquals(30, view.games.get(view.games.size() - 1).size());
        assertFalse(view.hasMores.get(view.hasMores.size() - 1));
        int calls = fake.gamesOffsets.size();
        c.loadMoreGames();
        settle();
        assertEquals(calls, fake.gamesOffsets.size());
    }

    @Test
    public void loadMoreWhileInFlightIsNoOp() {
        DbController c = withGames(60, 25);
        c.loadMoreGames();
        c.loadMoreGames();
        assertEquals(1, io.size());
        settle();
        assertEquals(2, fake.gamesOffsets.size());
    }

    @Test
    public void staleGamesResponseIsDropped() {
        DbController c = started(); // at [180]
        fake.gamesReply(Arrays.asList(180, 181), 0, 200, FakeDbApi.gamesPage(5, 3000, 5));
        fake.gamesReply(m180(), 0, 200, FakeDbApi.gamesPage(7, 1000, 7));
        c.onBoardMove(181); // A: stats, games
        assertTrue(c.back()); // B: stats, games
        assertEquals(4, io.size());
        io.run(3); // B games io
        main.runAll();
        io.run(1); // A games io
        main.runAll();
        assertEquals(1, view.count("showGames"));
        assertEquals(Integer.valueOf(7), view.totals.get(view.totals.size() - 1));
    }

    @Test
    public void positionChangeDuringLoadMoreUnsticksShowMore() {
        DbController c = withGames(60, 25);
        fake.gamesReply(Arrays.asList(180, 181), 0, 200, FakeDbApi.gamesPage(60, 3000, 25));
        c.loadMoreGames(); // io: [more]
        c.onBoardMove(181); // io: [more, stats, games]
        settle();
        // the superseded page never rendered: shown lists are page 1 (25) then the new position's page (25)
        assertEquals(2, view.games.size());
        assertEquals(25, view.games.get(1).size());
        assertEquals(3000, view.games.get(1).get(0).gid);
        int calls = fake.gamesOffsets.size();
        fake.gamesReply(Arrays.asList(180, 181), 25, 200, FakeDbApi.gamesPage(60, 4000, 25));
        c.loadMoreGames();
        settle();
        assertEquals(calls + 1, fake.gamesOffsets.size());
        assertEquals(50, view.games.get(view.games.size() - 1).size());
    }

    @Test
    public void gamesErrorShowsRetryGames() {
        fake.gamesReply(m180(), 0, 500, "{}");
        DbController c = controller(1, m180());
        c.attach(view);
        settle();
        assertEquals(Collections.singletonList(DbController.Endpoint.GAMES), view.errors);
        assertEquals(0, view.count("showGames"));
    }

    @Test
    public void retryGamesRefetchesFailedOffset() {
        DbController c = withGames(60, 25);
        fake.gamesReply(m180(), 25, 500, "{}");
        c.loadMoreGames();
        settle();
        assertEquals(Collections.singletonList(DbController.Endpoint.GAMES), view.errors);
        fake.gamesReply(m180(), 25, 200, FakeDbApi.gamesPage(60, 2000, 25));
        int stats = fake.statsCalls.size();
        c.retry(DbController.Endpoint.GAMES);
        settle();
        assertEquals(25, fake.lastGamesOffset());
        assertEquals(stats, fake.statsCalls.size());
        assertEquals(50, view.games.get(view.games.size() - 1).size());
    }

    @Test
    public void retryGamesAfterFirstPageFailureUsesOffset0() {
        fake.gamesReply(m180(), 0, 500, "{}");
        DbController c = controller(1, m180());
        c.attach(view);
        settle();
        fake.gamesReply(m180(), 0, 200, FakeDbApi.gamesPage(3, 1000, 3));
        c.retry(DbController.Endpoint.GAMES);
        settle();
        assertEquals(0, fake.lastGamesOffset());
        assertEquals(3, view.games.get(0).size());
    }

    @Test
    public void gamesBlockedShowsBlocked() {
        fake.gamesReply(m180(), 0, 200, "{\"total\":0,\"blocked\":true,\"games\":[]}");
        DbController c = controller(1, m180());
        c.attach(view);
        settle();
        assertEquals(1, view.count("showBlocked"));
        assertEquals(0, view.count("showGames"));
    }

    @Test
    public void reattachAfterPage2FailureKeepsLoadedRows() {
        DbController c = withGames(60, 25);
        fake.gamesReply(m180(), 25, 500, "{}");
        c.loadMoreGames();
        settle();
        c.detach();
        RecordingDbView v2 = new RecordingDbView();
        c.attach(v2);
        assertEquals(1, v2.count("showGames"));
        assertEquals(25, v2.games.get(0).size());
        assertEquals(Integer.valueOf(60), v2.totals.get(0));
        assertFalse(v2.hasMores.get(0));
        assertEquals(Collections.singletonList(DbController.Endpoint.GAMES), v2.errors);
        assertTrue(v2.log.indexOf("showGames") < v2.log.indexOf("showError"));
        assertEquals(0, io.size());
    }

    @Test
    public void blocked403ShowsBlockedForStatsAndGames() {
        String body = "{\"error\":{\"code\":\"blocked\",\"message\":\"m\"}}";
        fake.reply(m180(), 403, body);
        fake.gamesReply(m180(), 0, 403, body);
        DbController c = controller(1, m180());
        c.attach(view);
        settle();
        assertEquals(2, view.count("showBlocked"));
        assertEquals(0, view.count("showError"));
    }

    @Test
    public void gamesRenderedOnReattach() {
        DbController c = withGames(60, 25);
        c.detach();
        RecordingDbView v2 = new RecordingDbView();
        c.attach(v2);
        assertEquals(1, v2.count("showGames"));
        assertEquals(25, v2.games.get(0).size());
        assertEquals(0, io.size());
    }

    private DbFilters scopeFilters(String scope) {
        return DbFilters.defaults().toBuilder().scope(scope).build();
    }

    @Test
    public void applyClearsOnceThenRefetchesBoth() {
        DbController c = started();
        int stats = fake.statsCalls.size();
        int games = fake.gamesCalls.size();
        c.applyFilters(DbFilters.defaults().toBuilder().winner(1).player1MinRating(1800).build());
        assertEquals(1, view.count("clearResults"));
        settle();
        assertEquals(stats + 1, fake.statsCalls.size());
        assertEquals(games + 1, fake.gamesCalls.size());
        assertEquals(0, fake.lastGamesOffset());
        assertEquals(Arrays.asList(180), fake.lastStatsMoves());
        JsonObject expected = c.appliedFilters().toWire(c.serverFilters());
        assertEquals(expected, fake.statsFilters.get(fake.statsFilters.size() - 1));
        assertEquals(expected, fake.gamesFilters.get(fake.gamesFilters.size() - 1));
        assertEquals(c.appliedFilters(), store.load());
    }

    @Test
    public void applyUsesScope() {
        DbController c = started();
        c.applyFilters(scopeFilters("mine"));
        settle();
        assertEquals("mine", fake.statsScopes.get(fake.statsScopes.size() - 1));
        assertEquals("mine", fake.gamesScopes.get(fake.gamesScopes.size() - 1));
        assertEquals("mine", c.scope());
    }

    @Test
    public void mine401ShowsLogin() {
        DbController c = started();
        fake.reply(Arrays.asList(180), 401, "{\"error\":{\"code\":\"unauthorized\",\"message\":\"x\"}}");
        fake.gamesReply(Arrays.asList(180), 0, 401, "{\"error\":{\"code\":\"unauthorized\",\"message\":\"x\"}}");
        c.applyFilters(scopeFilters("mine"));
        settle();
        assertTrue(view.count("showLoginPrompt") >= 1);
        assertEquals(0, view.count("showDots"));
        assertEquals(0, view.count("showGames"));
    }

    @Test
    public void gameChangeResetsPosition() {
        DbController c = started();
        c.onBoardMove(181);
        settle();
        c.applyFilters(DbFilters.defaults().toBuilder().baseGameId(7).build());
        settle();
        assertEquals(7, c.gameId());
        assertEquals(Collections.<Integer>emptyList(), c.moves());
        assertEquals(Collections.<Integer>emptyList(), fake.lastStatsMoves());
        assertEquals(7, (int) fake.statsGames.get(fake.statsGames.size() - 1));
        // same game, new filters: the position is kept
        c.onBoardMove(180);
        c.applyFilters(c.appliedFilters().toBuilder().winner(2).build());
        assertEquals(Arrays.asList(180), c.moves());
    }

    @Test
    public void extrasGameOverridesStoredGameWithoutSaving() {
        DbController c = controller(2, Arrays.asList(180, 181));
        c.attach(view);
        settle();
        assertEquals(2, (int) fake.statsGames.get(0));
        assertEquals(2, (int) fake.gamesGames.get(0));
        assertEquals(Arrays.asList(180, 181), fake.statsCalls.get(0));
        assertEquals(2, c.gameId());
        assertEquals(1, store.load().gameId());
    }

    @Test
    public void reattachAfterApplyKeepsAppliedFilters() {
        DbController c = started();
        c.applyFilters(scopeFilters("both"));
        settle();
        c.detach();
        c.attach(view);
        assertEquals("both", c.scope());
    }

    @Test
    public void restoreFetchesRestoredPosition() {
        DbController c = started();
        int before = fake.statsCalls.size();
        c.restore(Arrays.asList(180, 181), Arrays.asList(182));
        assertEquals(Arrays.asList(180, 181), c.moves());
        settle();
        assertEquals(before + 1, fake.statsCalls.size());
        assertEquals(Arrays.asList(180, 181), fake.lastStatsMoves());
        assertEquals(Arrays.asList(180, 181), view.positions.get(0));
        assertTrue(c.forward());
        assertEquals(Arrays.asList(180, 181, 182), c.moves());
    }

    // Server filters (B3/B4) gated at runtime on /ping features

    private static final String PING_NO_FEATURES = "{\"ok\":true,\"auth\":\"bob\",\"dbAccess\":true}";
    private static final String PING_B3_B4 =
            "{\"ok\":true,\"auth\":\"bob\",\"dbAccess\":true,\"features\":[\"excludeTimeouts\",\"liveOrTurnBased\",\"badges\"]}";

    /** Stored filters as a legacy migration leaves them: timeouts excluded, live games only. */
    private void storeMigrated() {
        store.save(DbFilters.defaults().toBuilder().excludeTimeouts(true).liveOrTurnBased(DbFilters.LIVE_ONLY).build());
    }

    private static boolean sendsServerFilters(com.google.gson.JsonElement wire) {
        if (wire == null || wire.isJsonNull()) return false;
        JsonObject o = wire.getAsJsonObject();
        return o.get("excludeTimeouts").getAsBoolean() || !"all".equals(o.get("liveOrTurnBased").getAsString());
    }

    @Test
    public void featuresAbsentHidesAndDoesNotSendServerFilters() {
        storeMigrated();
        fake.pingReply = new FakeDbApi.Reply(200, PING_NO_FEATURES);
        DbController c = controller(1, Arrays.asList(180));
        c.loadFeatures();
        c.attach(view);
        settle();
        assertFalse(c.serverFilters());
        assertFalse(sendsServerFilters(fake.statsFilters.get(fake.statsFilters.size() - 1)));
        assertFalse(sendsServerFilters(fake.gamesFilters.get(fake.gamesFilters.size() - 1)));
        assertEquals("Pente", FilterSummary.text(c.appliedFilters(), c.serverFilters()));
        assertFalse(view.serverFilters.contains(true));
        assertTrue(c.appliedFilters().excludeTimeouts); // the stored value is kept, only gated
    }

    @Test
    public void featuresPresentSendsServerFiltersAndRerenders() {
        storeMigrated();
        fake.pingReply = new FakeDbApi.Reply(200, PING_B3_B4);
        DbController c = controller(1, Arrays.asList(180));
        c.loadFeatures();
        c.attach(view);
        settle();
        assertTrue(c.serverFilters());
        assertEquals(Collections.singletonList(true), view.serverFilters);
        JsonObject stats = fake.statsFilters.get(fake.statsFilters.size() - 1).getAsJsonObject();
        assertTrue(stats.get("excludeTimeouts").getAsBoolean());
        assertEquals("live", stats.get("liveOrTurnBased").getAsString());
        JsonObject games = fake.gamesFilters.get(fake.gamesFilters.size() - 1).getAsJsonObject();
        assertTrue(games.get("excludeTimeouts").getAsBoolean());
        assertEquals("Pente · no timeouts · live only", FilterSummary.text(c.appliedFilters(), c.serverFilters()));
    }

    @Test
    public void featuresArrivingWithDefaultFiltersDoNotRefetch() {
        fake.pingReply = new FakeDbApi.Reply(200, PING_B3_B4);
        DbController c = controller(1, Arrays.asList(180));
        c.attach(view);
        settle();
        int stats = fake.statsCalls.size();
        c.loadFeatures();
        settle();
        assertTrue(c.serverFilters());
        assertEquals(stats, fake.statsCalls.size());
    }

    @Test
    public void oneFeatureAloneIsNotEnough() {
        fake.pingReply = new FakeDbApi.Reply(200, "{\"ok\":true,\"features\":[\"excludeTimeouts\"]}");
        DbController c = controller(1, Arrays.asList(180));
        c.loadFeatures();
        settle();
        assertFalse(c.serverFilters());
    }

    @Test
    public void pingFailureMeansNoServerFilters() {
        fake.pingReply = null;
        DbController c = controller(1, Arrays.asList(180));
        c.loadFeatures();
        settle();
        assertFalse(c.serverFilters());
    }

    @Test
    public void pingIsSentOncePerController() {
        fake.pingReply = new FakeDbApi.Reply(200, PING_B3_B4);
        DbController c = controller(1, Arrays.asList(180));
        c.loadFeatures();
        c.loadFeatures();
        settle();
        c.loadFeatures();
        settle();
        assertEquals(1, fake.pingCalls);
    }

    @Test
    public void migratedValueSurvivesApplyWhileFeaturesAbsent() {
        storeMigrated();
        fake.pingReply = new FakeDbApi.Reply(200, PING_NO_FEATURES);
        DbController c = controller(1, Arrays.asList(180));
        c.loadFeatures();
        c.attach(view);
        settle();
        // The sheet with hidden controls reads its draft with the B3/B4 fields at their defaults.
        DbFilters draft = DbFilters.defaults().toBuilder().winner(1).build();
        c.applyFilters(draft.keepServerFiltersFrom(c.appliedFilters(), c.serverFilters()));
        settle();
        DbFilters saved = store.load();
        assertEquals(1, saved.winner);
        assertTrue(saved.excludeTimeouts);
        assertEquals(DbFilters.LIVE_ONLY, saved.liveOrTurnBased);
        assertFalse(sendsServerFilters(fake.statsFilters.get(fake.statsFilters.size() - 1)));
    }

    @Test
    public void visibleControlsWinWhenFeaturesPresent() {
        DbFilters original = DbFilters.defaults().toBuilder().excludeTimeouts(true).build();
        DbFilters draft = DbFilters.defaults();
        assertFalse(draft.keepServerFiltersFrom(original, true).excludeTimeouts);
    }

    // One Retry for every failed endpoint

    @Test
    public void retryFailedRefetchesBothFailedEndpoints() {
        fake.reply(m180(), 500, "{}");
        fake.gamesReply(m180(), 0, 500, "{}");
        DbController c = controller(1, m180());
        c.attach(view);
        settle();
        assertEquals(Arrays.asList(DbController.Endpoint.STATS, DbController.Endpoint.GAMES), view.errors);
        fake.reply(m180(), 200, FakeDbApi.stats(181, 40, 200, 80));
        fake.gamesReply(m180(), 0, 200, FakeDbApi.gamesPage(3, 1000, 3));
        int stats = fake.statsCalls.size();
        int games = fake.gamesCalls.size();
        c.retryFailed();
        settle();
        assertEquals(stats + 1, fake.statsCalls.size());
        assertEquals(games + 1, fake.gamesCalls.size());
        assertEquals(0, fake.lastGamesOffset());
        assertEquals(1, view.count("showDots"));
        assertEquals(3, view.games.get(view.games.size() - 1).size());
    }

    @Test
    public void retryFailedLeavesHealthyEndpointAlone() {
        fake.gamesReply(m180(), 0, 500, "{}");
        DbController c = controller(1, m180());
        c.attach(view);
        settle();
        fake.gamesReply(m180(), 0, 200, FakeDbApi.gamesPage(3, 1000, 3));
        int stats = fake.statsCalls.size();
        c.retryFailed();
        settle();
        assertEquals(stats, fake.statsCalls.size());
        assertEquals(3, view.games.get(view.games.size() - 1).size());
    }

    @Test
    public void retryFailedStatsOnlyKeepsLoadedGames() {
        fake.reply(m180(), 500, "{}");
        fake.gamesReply(m180(), 0, 200, FakeDbApi.gamesPage(3, 1000, 3));
        DbController c = controller(1, m180());
        c.attach(view);
        settle();
        fake.reply(m180(), 200, FakeDbApi.stats(181, 40, 200, 80));
        int games = fake.gamesCalls.size();
        view.log.clear();
        c.retryFailed();
        settle();
        assertEquals(games, fake.gamesCalls.size());
        assertEquals(0, view.count("clearResults"));
        assertEquals(1, view.count("showDots"));
    }

    // Board input guards

    @Test
    public void occupiedCellIsIgnored() {
        DbController c = started();
        int stats = fake.statsCalls.size();
        c.onBoardMove(180);
        settle();
        assertEquals(Arrays.asList(180), c.moves());
        assertEquals(stats, fake.statsCalls.size());
        assertEquals(0, view.count("clearResults"));
    }

    @Test
    public void offBoardCellIsIgnored() {
        DbController c = started();
        c.onBoardMove(-1);
        c.onBoardMove(19 * 19);
        assertEquals(Arrays.asList(180), c.moves());
    }

    @Test
    public void aiMoveForCurrentPositionIsPlayed() {
        DbController c = started();
        List<Integer> askedFor = c.moves();
        assertTrue(c.playAiMove(181, askedFor));
        assertEquals(Arrays.asList(180, 181), c.moves());
    }

    @Test
    public void staleAiMoveIsDropped() {
        DbController c = started();
        c.onBoardMove(181);
        settle();
        List<Integer> askedFor = c.moves();
        c.back(); // the position changes while the engine thinks
        settle();
        int stats = fake.statsCalls.size();
        assertFalse(c.playAiMove(182, askedFor));
        assertEquals(Arrays.asList(180), c.moves());
        assertEquals(stats, fake.statsCalls.size());
    }

    @Test
    public void aiMoveWithoutRequestIsDropped() {
        DbController c = started();
        assertFalse(c.playAiMove(181, null));
        assertEquals(Arrays.asList(180), c.moves());
    }
}
