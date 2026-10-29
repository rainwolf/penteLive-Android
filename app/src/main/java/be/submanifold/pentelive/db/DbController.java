package be.submanifold.pentelive.db;

import be.submanifold.pentelive.db.DbDtos.NextMove;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * Presenter for the DB screen. Owns the position (via {@link RedoHistory}), fires
 * position-stats with a per-endpoint stale guard, and keeps a small render state so a
 * re-attached view (rotation) gets the current position and results without a refetch.
 * All public methods and all result handling run on the main executor's thread.
 */
public final class DbController {
    public enum Endpoint { STATS, GAMES }

    public static final class MoveRow {
        public final NextMove move;
        public final int argb;

        public MoveRow(NextMove move, int argb) {
            this.move = move;
            this.argb = argb;
        }
    }

    static final int DEFAULT_SHOW = 20;
    static final String SCOPE = "archive";

    private enum StatsState { NONE, LOADING, OK, BLOCKED, UNAUTHORIZED, SUBSCRIPTION_REQUIRED, ERROR }

    /** Derived off the main thread from one stats reply. */
    private static final class StatsOutcome {
        final DbResult.Kind kind;
        final boolean blocked;
        final List<NextMove> sorted; // usable (unoccupied), full ranked list
        final Set<Integer> occupied;

        StatsOutcome(DbResult.Kind kind, boolean blocked, List<NextMove> sorted, Set<Integer> occupied) {
            this.kind = kind;
            this.blocked = blocked;
            this.sorted = sorted;
            this.occupied = occupied;
        }
    }

    private final DbApi api;
    private final Executor io;
    private final Executor main;
    private final LatestRequest statsGuard = new LatestRequest();

    private int gameId;
    private boolean supported;
    private RedoHistory history;
    private DbView view;
    private boolean started;

    // render state
    private CaptureIcons.Counts caps;
    private StatsState statsState = StatsState.NONE;
    private List<MoveRow> rows = Collections.emptyList();
    private Map<Integer, Integer> dots = Collections.emptyMap();
    private int showCount = DEFAULT_SHOW;
    private List<NextMove> sortedCache = Collections.emptyList();
    private Set<Integer> occupiedCache = Collections.emptySet();

    public DbController(DbApi api, Executor io, Executor main, int gameId, List<Integer> initialMoves) {
        this.api = api;
        this.io = io;
        this.main = main;
        List<Integer> initial = initialMoves == null || initialMoves.isEmpty() ? null : initialMoves;
        resetGame(gameId, initial);
    }

    private void resetGame(int newGameId, List<Integer> initialOrNull) {
        gameId = newGameId;
        supported = newGameId != 0 && DbGames.isSupported(newGameId);
        statsGuard.next(); // invalidate anything in flight for the previous game
        rows = Collections.emptyList();
        dots = Collections.emptyMap();
        statsState = StatsState.NONE;
        if (supported) {
            List<Integer> initial = initialOrNull != null ? initialOrNull : DbGames.seed(newGameId);
            history = new RedoHistory(initial, DbGames.moveFloor(newGameId), DbGames.stepsByTurn(newGameId));
            caps = CaptureIcons.of(newGameId, history.moves());
        } else {
            history = new RedoHistory(Collections.<Integer>emptyList(), 0, false);
            caps = null;
        }
    }

    public void attach(DbView v) {
        view = v;
        if (!started) {
            started = true;
            if (supported) {
                positionChanged();
            } else {
                view.showUnsupported();
            }
            return;
        }
        render();
    }

    public void detach() {
        view = null;
    }

    public void onBoardMove(int cell) {
        if (!supported) return;
        history.play(cell);
        positionChanged();
    }

    public boolean back() {
        if (!supported || !history.back()) return false;
        positionChanged();
        return true;
    }

    public boolean forward() {
        if (!supported || !history.forward()) return false;
        positionChanged();
        return true;
    }

    public void retry(Endpoint e) {
        if (e == Endpoint.STATS && supported) positionChanged();
        // GAMES: added with the games list.
    }

    public List<Integer> moves() {
        return history.moves();
    }

    public int gameId() {
        return gameId;
    }

    public void setGame(int newGameId) {
        resetGame(newGameId, null);
        started = true;
        if (!supported) {
            if (view != null) view.showUnsupported();
            return;
        }
        positionChanged();
    }

    /** clearResults, showPosition, then a stats request with a fresh id. */
    private void positionChanged() {
        final List<Integer> moves = history.moves();
        caps = CaptureIcons.of(gameId, moves);
        rows = Collections.emptyList();
        dots = Collections.emptyMap();
        statsState = StatsState.LOADING;
        if (view != null) {
            view.clearResults();
            view.showPosition(moves, caps);
        }
        final int id = statsGuard.next();
        final int game = gameId;
        io.execute(new Runnable() {
            @Override
            public void run() {
                final StatsOutcome out = fetchStats(game, moves);
                main.execute(new Runnable() {
                    @Override
                    public void run() {
                        onStats(id, out);
                    }
                });
            }
        });
    }

    /** IO side: request plus pure derivation (ranking, top N, heat). No state is touched. */
    private StatsOutcome fetchStats(int game, List<Integer> moves) {
        DbResult<DbDtos.PositionStats> r = api.positionStats(game, moves, SCOPE, null);
        if (r.kind != DbResult.Kind.OK) return new StatsOutcome(r.kind, false, null, null);
        if (r.value.blocked) return new StatsOutcome(DbResult.Kind.OK, true, null, null);
        Set<Integer> occupied = CaptureIcons.occupied(game, moves);
        List<NextMove> usable = new ArrayList<>();
        for (NextMove m : r.value.nextMoves) {
            if (m != null && !occupied.contains(m.move)) usable.add(m);
        }
        return new StatsOutcome(DbResult.Kind.OK, false,
                Collections.unmodifiableList(MoveRanking.sorted(usable)), occupied);
    }

    /** Rows and dots over the top showCount of the cached ranked list (n <= 0 = all). */
    private void derive() {
        List<NextMove> top = MoveRanking.top(sortedCache, showCount <= 0 ? sortedCache.size() : showCount);
        Map<Integer, Integer> colours = HeatScale.colours(top, occupiedCache);
        List<MoveRow> out = new ArrayList<>(top.size());
        for (NextMove m : top) out.add(new MoveRow(m, colours.get(m.move)));
        rows = Collections.unmodifiableList(out);
        dots = Collections.unmodifiableMap(new LinkedHashMap<>(colours));
    }

    /** Re-renders rows and dots from the cached reply, no refetch. */
    public void setShowCount(int n) {
        showCount = n;
        if (statsState != StatsState.OK) return;
        derive();
        if (view != null) renderStats();
    }

    /** Same path as a board tap. */
    public void playCandidate(int cell) {
        onBoardMove(cell);
    }

    /** Main side: stale check first, before any state change or view call. */
    private void onStats(int id, StatsOutcome out) {
        if (!statsGuard.isLatest(id)) return;
        switch (out.kind) {
            case OK:
                if (out.blocked) {
                    statsState = StatsState.BLOCKED;
                } else {
                    statsState = StatsState.OK;
                    sortedCache = out.sorted;
                    occupiedCache = out.occupied;
                    derive();
                }
                break;
            case UNAUTHORIZED:
                statsState = StatsState.UNAUTHORIZED;
                break;
            case SUBSCRIPTION_REQUIRED:
                statsState = StatsState.SUBSCRIPTION_REQUIRED;
                break;
            default:
                statsState = StatsState.ERROR;
                break;
        }
        if (view != null) renderStats();
    }

    private void render() {
        if (view == null) return;
        if (!supported) {
            view.showUnsupported();
            return;
        }
        view.clearResults();
        view.showPosition(history.moves(), caps);
        renderStats();
    }

    private void renderStats() {
        switch (statsState) {
            case OK:
                view.showMoves(rows);
                view.showDots(dots);
                break;
            case BLOCKED:
                view.showBlocked();
                break;
            case UNAUTHORIZED:
                view.showLoginPrompt();
                break;
            case SUBSCRIPTION_REQUIRED:
                view.showSubscribeGate();
                break;
            case ERROR:
                view.showError(Endpoint.STATS);
                break;
            default:
                break; // NONE / LOADING: nothing yet
        }
    }
}
