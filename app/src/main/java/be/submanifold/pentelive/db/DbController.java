package be.submanifold.pentelive.db;

import be.submanifold.pentelive.db.DbDtos.GameHeader;
import be.submanifold.pentelive.db.DbDtos.NextMove;
import com.google.gson.JsonObject;
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
    static final int GAMES_PAGE = 25;

    private enum GamesState { NONE, LOADING, OK, BLOCKED, UNAUTHORIZED, SUBSCRIPTION_REQUIRED, ERROR }

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
    private final LatestRequest gamesGuard = new LatestRequest();

    private final DbFilterStore store;
    private DbFilters applied;
    private int gameId;
    private boolean supported;
    private RedoHistory history;
    private DbView view;
    private boolean started;
    /** B3/B4 filters (exclude timeouts, live/turn-based) are sent only when /ping lists both features. */
    private boolean serverFilters = DbCapabilities.SERVER_FILTERS_DEFAULT;
    private boolean featuresRequested;

    // render state
    private CaptureIcons.Counts caps;
    private StatsState statsState = StatsState.NONE;
    private List<MoveRow> rows = Collections.emptyList();
    private Map<Integer, Integer> dots = Collections.emptyMap();
    private int showCount = DEFAULT_SHOW;
    private MoveSort sort;
    /** The Moves row selected for the board flash; null = none. Cleared by every change of the results. */
    private Integer selectedCell;
    private List<NextMove> sortedCache = Collections.emptyList();
    private Set<Integer> occupiedCache = Collections.emptySet();

    // games paging state
    private GamesState gamesState = GamesState.NONE;
    private List<GameHeader> loadedGames = new ArrayList<>();
    private int gamesTotal;
    private int nextOffset; // offset of the next page to request
    private int pendingOffset; // offset of the in-flight (or last failed) request
    private boolean gamesInFlight;

    /**
     * @param gameOverride the extras game (not saved); null = take the game from the stored filters;
     *                     0 = an unsupported game (shows the unsupported state)
     */
    public DbController(DbApi api, Executor io, Executor main, DbFilterStore store, Integer gameOverride,
                        List<Integer> initialMoves) {
        this.api = api;
        this.io = io;
        this.main = main;
        this.store = store;
        this.sort = store.loadSort();
        DbFilters stored = store.load();
        int game;
        if (gameOverride == null) {
            game = stored.gameId();
            applied = stored;
        } else if (gameOverride != 0 && DbGames.isSupported(gameOverride)) {
            game = gameOverride;
            applied = stored.withGame(gameOverride);
        } else {
            game = gameOverride;
            applied = stored;
        }
        List<Integer> initial = initialMoves == null || initialMoves.isEmpty() ? null : initialMoves;
        resetGame(game, initial);
    }

    /** The filters in force for this session (stored ones, or stored plus the extras game). */
    public DbFilters appliedFilters() {
        return applied;
    }

    /** Whether the server takes the B3/B4 filters; gates the wire, the chip row and the sheet controls. */
    public boolean serverFilters() {
        return serverFilters;
    }

    /**
     * One {@code GET /ping} per controller (the controller lives in the ViewModel). When both B3/B4 features
     * are listed, the flag flips, the view re-renders its filter UI, and results are refetched if the stored
     * filters carry B3/B4 values that were held back until now.
     */
    public void loadFeatures() {
        if (featuresRequested) return;
        featuresRequested = true;
        io.execute(new Runnable() {
            @Override
            public void run() {
                final DbResult<DbDtos.Ping> r = api.ping();
                main.execute(new Runnable() {
                    @Override
                    public void run() {
                        onPing(r);
                    }
                });
            }
        });
    }

    private void onPing(DbResult<DbDtos.Ping> r) {
        boolean enabled = r.kind == DbResult.Kind.OK && r.value.features != null
                && r.value.features.contains("excludeTimeouts") && r.value.features.contains("liveOrTurnBased");
        if (enabled == serverFilters) return;
        boolean wireChanged = !equalWire(applied.toWire(serverFilters), applied.toWire(enabled));
        serverFilters = enabled;
        if (view != null) view.showServerFilters(enabled);
        if (wireChanged && started && supported) positionChanged();
    }

    private static boolean equalWire(JsonObject a, JsonObject b) {
        return a == null ? b == null : a.equals(b);
    }

    /** Scope sent with every request, for the list label. */
    public String scope() {
        return applied.scope;
    }

    /** Saves f, clears results once, resets the position if the game changed, refetches stats and games. */
    public void applyFilters(DbFilters f) {
        store.save(f);
        boolean gameChanged = f.gameId() != gameId;
        applied = f;
        if (gameChanged) resetGame(f.gameId(), null);
        started = true;
        if (!supported) {
            if (view != null) view.showUnsupported();
            return;
        }
        positionChanged();
    }

    private void resetGame(int newGameId, List<Integer> initialOrNull) {
        gameId = newGameId;
        supported = newGameId != 0 && DbGames.isSupported(newGameId);
        statsGuard.next(); // invalidate anything in flight for the previous game
        gamesGuard.next();
        resetGames();
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
        selectedCell = null;
        view = null;
    }

    /**
     * Plays cell; an off-board (the Go pass sentinel included), occupied or Go ko-retake cell is ignored
     * (results stay as they are). Passes are never entered here, only kept from an opened line (web parity).
     */
    public void onBoardMove(int cell) {
        if (!supported) return;
        int size = DbGames.gridSize(gameId);
        if (cell < 0 || cell >= size * size) return;
        BoardReplay.Result position = BoardReplay.replay(gameId, history.moves());
        if (position.boardXY[cell % size][cell / size] != 0 || position.koMove == cell) return;
        history.play(cell);
        positionChanged();
    }

    /**
     * An engine reply for the position {@code askedFor} (the moves sent to the engine). Dropped, returning
     * false, when the position has changed since (swipe, tap, filters) or the cell is not playable.
     */
    public boolean playAiMove(int cell, List<Integer> askedFor) {
        if (!supported || askedFor == null || !askedFor.equals(history.moves())) return false;
        int before = history.moves().size();
        onBoardMove(cell);
        return history.moves().size() == before + 1;
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

    public boolean first() {
        if (!supported || !history.toStart()) return false;
        positionChanged();
        return true;
    }

    public boolean last() {
        if (!supported || !history.toEnd()) return false;
        positionChanged();
        return true;
    }

    /** Process-death restore: puts back the saved moves and redo stack, then fetches that position. */
    public void restore(List<Integer> savedMoves, List<Integer> savedRedo) {
        if (!supported) return;
        history.restore(savedMoves, savedRedo);
        started = true;
        positionChanged();
    }

    /** Redo stack, next move first (for saving across process death). */
    public List<Integer> redoMoves() {
        return history.redo();
    }

    public void retry(Endpoint e) {
        if (!supported) return;
        if (e == Endpoint.STATS) {
            positionChanged();
        } else if (!gamesInFlight && gamesState == GamesState.ERROR) {
            fetchGames(pendingOffset); // the failed offset; loaded rows are kept
        }
    }

    /**
     * The single Retry: refetches every endpoint whose last request failed (stats, games or both),
     * leaving a healthy endpoint's results in place.
     */
    public void retryFailed() {
        if (!supported) return;
        if (statsState == StatsState.ERROR) requestStats();
        if (!gamesInFlight && gamesState == GamesState.ERROR) fetchGames(pendingOffset);
    }

    /** Appends the next page. No-op while a request is in flight or once everything is loaded. */
    public void loadMoreGames() {
        if (!supported || gamesInFlight || gamesState != GamesState.OK) return;
        if (loadedGames.size() >= gamesTotal) return;
        fetchGames(nextOffset);
    }

    private void resetGames() {
        gamesState = GamesState.NONE;
        loadedGames = new ArrayList<>();
        gamesTotal = 0;
        nextOffset = 0;
        pendingOffset = 0;
        gamesInFlight = false;
    }

    private void fetchGames(final int offset) {
        final List<Integer> moves = history.moves();
        final int game = gameId;
        final int id = gamesGuard.next();
        final String scope = applied.scope;
        final JsonObject filters = applied.toWire(serverFilters);
        pendingOffset = offset;
        gamesInFlight = true;
        gamesState = GamesState.LOADING;
        io.execute(new Runnable() {
            @Override
            public void run() {
                final DbResult<DbDtos.GamePage> r = api.searchGames(game, moves, scope, filters, offset, GAMES_PAGE);
                main.execute(new Runnable() {
                    @Override
                    public void run() {
                        onGames(id, offset, r);
                    }
                });
            }
        });
    }

    /** Main side: a page must match both the latest id and the offset asked for. */
    private void onGames(int id, int offset, DbResult<DbDtos.GamePage> r) {
        if (!gamesGuard.isLatest(id) || offset != pendingOffset) return;
        gamesInFlight = false;
        switch (r.kind) {
            case OK:
                if (r.value.blocked) {
                    gamesState = GamesState.BLOCKED;
                } else {
                    gamesState = GamesState.OK;
                    gamesTotal = r.value.total;
                    if (r.value.games != null) loadedGames.addAll(r.value.games);
                    nextOffset = offset + GAMES_PAGE;
                }
                break;
            case UNAUTHORIZED:
                gamesState = GamesState.UNAUTHORIZED;
                break;
            case SUBSCRIPTION_REQUIRED:
                gamesState = GamesState.SUBSCRIPTION_REQUIRED;
                break;
            case BLOCKED:
                gamesState = GamesState.BLOCKED;
                break;
            default:
                gamesState = GamesState.ERROR;
                break;
        }
        if (view != null) renderGames();
    }

    private void renderGames() {
        switch (gamesState) {
            case OK:
                view.showGames(new ArrayList<>(loadedGames), gamesTotal, loadedGames.size() < gamesTotal);
                break;
            case LOADING:
                if (!loadedGames.isEmpty()) view.showGames(new ArrayList<>(loadedGames), gamesTotal, false);
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
                // A later page failed: keep the loaded rows (a re-attach starts from clearResults).
                if (!loadedGames.isEmpty()) view.showGames(new ArrayList<>(loadedGames), gamesTotal, false);
                view.showError(Endpoint.GAMES);
                break;
            default:
                break;
        }
    }

    public List<Integer> moves() {
        return history.moves();
    }

    public int gameId() {
        return gameId;
    }

    /** clearResults, showPosition, then stats and games requests, each with a fresh id. */
    private void positionChanged() {
        final List<Integer> moves = history.moves();
        caps = CaptureIcons.of(gameId, moves);
        rows = Collections.emptyList();
        dots = Collections.emptyMap();
        statsState = StatsState.LOADING;
        resetGames();
        clearSelection();
        if (view != null) {
            view.clearResults();
            view.showPosition(moves, caps);
        }
        requestStats();
        fetchGames(0);
    }

    /** Stats request for the current position with a fresh id; results render through onStats. */
    private void requestStats() {
        final List<Integer> moves = history.moves();
        statsState = StatsState.LOADING;
        clearSelection();
        final int id = statsGuard.next();
        final int game = gameId;
        final String scope = applied.scope;
        final JsonObject filters = applied.toWire(serverFilters);
        io.execute(new Runnable() {
            @Override
            public void run() {
                final StatsOutcome out = fetchStats(game, moves, scope, filters);
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
    private StatsOutcome fetchStats(int game, List<Integer> moves, String scope, JsonObject filters) {
        DbResult<DbDtos.PositionStats> r = api.positionStats(game, moves, scope, filters);
        if (r.kind != DbResult.Kind.OK) return new StatsOutcome(r.kind, false, null, null);
        if (r.value.blocked) return new StatsOutcome(DbResult.Kind.OK, true, null, null);
        Set<Integer> occupied = CaptureIcons.occupied(game, moves);
        int ko = DbGames.isGo(game) ? BoardReplay.replay(game, moves).koMove : -1;
        int cells = DbGames.gridSize(game) * DbGames.gridSize(game);
        List<NextMove> usable = new ArrayList<>();
        for (NextMove m : r.value.nextMoves) {
            // Off-board candidates (a pass, or a backend-rotated pass on small boards) get no row or dot.
            if (m != null && m.move >= 0 && m.move < cells && !occupied.contains(m.move)
                    && m.move != ko) usable.add(m);
        }
        return new StatsOutcome(DbResult.Kind.OK, false,
                Collections.unmodifiableList(MoveRanking.sorted(usable)), occupied);
    }

    /** Rows and dots over the top showCount of the cached ranked list (n <= 0 = all). */
    private void derive() {
        List<NextMove> ordered = MoveRanking.sorted(sortedCache, sort);
        List<NextMove> top = MoveRanking.top(ordered, showCount <= 0 ? ordered.size() : showCount);
        Map<Integer, Integer> colours = HeatScale.colours(top, occupiedCache);
        List<MoveRow> out = new ArrayList<>(top.size());
        for (NextMove m : top) out.add(new MoveRow(m, colours.get(m.move)));
        rows = Collections.unmodifiableList(out);
        dots = Collections.unmodifiableMap(new LinkedHashMap<>(colours));
    }

    /** Re-renders rows and dots from the cached reply, no refetch. */
    public void setShowCount(int n) {
        showCount = n;
        clearSelection();
        if (statsState != StatsState.OK) return;
        derive();
        if (view != null) renderStats();
    }

    public MoveSort sort() {
        return sort;
    }

    /** Persists the choice and re-sorts the cached reply (rows and heat), no refetch. */
    public void setSort(MoveSort s) {
        sort = s;
        store.saveSort(s);
        clearSelection();
        if (statsState != StatsState.OK) return;
        derive();
        if (view != null) renderStats();
    }

    public Integer selectedCell() {
        return selectedCell;
    }

    /**
     * A Moves row tap: selects the row's cell (one at a time), or unselects it when it is already
     * selected. Never plays the move; cells that are not in the list are ignored.
     */
    public void toggleSelection(int cell) {
        if (selectedCell != null && selectedCell == cell) {
            clearSelection();
            return;
        }
        boolean listed = false;
        for (MoveRow r : rows) {
            if (r.move.move == cell) listed = true;
        }
        if (!listed) return;
        selectedCell = cell;
        if (view != null) view.showSelection(cell);
    }

    /** The engine is about to think: the selection no longer matches what the user is doing. */
    public void aiStarting() {
        clearSelection();
    }

    /** Drops the selection (and tells the view); a no-op when nothing is selected. */
    public void clearSelection() {
        if (selectedCell == null) return;
        selectedCell = null;
        if (view != null) view.showSelection(null);
    }

    /** Main side: stale check first, before any state change or view call. */
    private void onStats(int id, StatsOutcome out) {
        if (!statsGuard.isLatest(id)) return;
        clearSelection();
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
            case BLOCKED:
                statsState = StatsState.BLOCKED;
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
        renderGames();
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
