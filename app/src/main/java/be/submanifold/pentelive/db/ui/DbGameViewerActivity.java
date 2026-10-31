package be.submanifold.pentelive.db.ui;

import android.content.Context;
import android.content.Intent;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import be.submanifold.pentelive.DBBoardView;
import be.submanifold.pentelive.LoginActivity;
import be.submanifold.pentelive.R;
import be.submanifold.pentelive.db.AuthedDbTransport;
import be.submanifold.pentelive.db.CaptureIcons;
import be.submanifold.pentelive.db.DbApi;
import be.submanifold.pentelive.db.DbDtos;
import be.submanifold.pentelive.db.DbGames;
import be.submanifold.pentelive.db.DbResult;
import be.submanifold.pentelive.db.GameRowFormat;
import be.submanifold.pentelive.db.GameViewerModel;
import be.submanifold.pentelive.db.LatestRequest;
import be.submanifold.pentelive.db.MoveGrid;

/** Read-only replay of one DB game: {@code GET /games/{gid}}, or {@code /collection/{wgid}} for "mine" rows. */
public class DbGameViewerActivity extends AppCompatActivity {
    private static final String EXTRA_GID = "gid";
    private static final String EXTRA_MINE = "mine";
    private static final int GRID_COLUMNS = 4;

    public static void start(Context c, long id, boolean mine) {
        c.startActivity(new Intent(c, DbGameViewerActivity.class)
                .putExtra(EXTRA_GID, id)
                .putExtra(EXTRA_MINE, mine));
    }

    enum State { NONE, LOADING, OK, BLOCKED, UNAUTHORIZED, SUBSCRIPTION_REQUIRED, FAILED }

    /** Keeps the model (cursor), the load state and the IO executor across rotation. */
    public static class ViewerViewModel extends ViewModel {
        private final ExecutorService io = Executors.newSingleThreadExecutor();
        private final Executor main = new Handler(Looper.getMainLooper())::post;
        private final DbApi api = new DbApi(new AuthedDbTransport());
        private final LatestRequest guard = new LatestRequest();
        GameViewerModel model;
        boolean mine;
        State state = State.NONE;
        Runnable onChange;

        void init(long id, boolean mine) {
            if (model != null) return;
            model = new GameViewerModel(id);
            this.mine = mine;
        }

        /** Main thread. A superseded load (retry) is dropped by the guard. */
        void load() {
            state = State.LOADING;
            final int reqId = guard.next();
            final long id = model.requestedId();
            final boolean fromCollection = mine;
            io.execute(() -> {
                final DbResult<DbDtos.GameDetail> r = fromCollection ? api.collectionGame(id) : api.game(id);
                main.execute(() -> onResult(reqId, r));
            });
        }

        private void onResult(int reqId, DbResult<DbDtos.GameDetail> r) {
            if (!guard.isLatest(reqId)) return;
            if (model.accept(r)) {
                state = State.OK;
            } else {
                switch (GameViewerModel.rejection(r)) {
                    case BLOCKED:
                        state = State.BLOCKED;
                        break;
                    case UNAUTHORIZED:
                        state = State.UNAUTHORIZED;
                        break;
                    case SUBSCRIPTION_REQUIRED:
                        state = State.SUBSCRIPTION_REQUIRED;
                        break;
                    default:
                        state = State.FAILED;
                        break;
                }
            }
            if (onChange != null) onChange.run();
        }

        @Override
        protected void onCleared() {
            onChange = null;
            io.shutdown();
        }
    }

    private ViewerViewModel vm;
    private DBBoardView board;
    private Toolbar toolbar;
    private TextView p1Name, p1Rating, p2Name, p2Rating, score, captures, date;
    private MoveGridAdapter gridAdapter;
    private AlertDialog loginDialog;
    private android.widget.PopupWindow subscribePopup;
    private TextView status;
    private View retry;
    private View progress;
    private String boardGame;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_db_game_viewer);
        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.db_game_title);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        board = findViewById(R.id.viewerBoard);
        board.setActivity(this);
        board.setPlacementEnabled(false);
        board.setDotsVisible(false);
        p1Name = findViewById(R.id.viewerP1Name);
        p1Rating = findViewById(R.id.viewerP1Rating);
        p2Name = findViewById(R.id.viewerP2Name);
        p2Rating = findViewById(R.id.viewerP2Rating);
        score = findViewById(R.id.viewerScore);
        captures = findViewById(R.id.viewerCaptures);
        date = findViewById(R.id.viewerDate);
        findViewById(R.id.viewerBack).setOnClickListener(v -> finish());
        gridAdapter = new MoveGridAdapter(index -> {
            if (vm.state == State.OK) {
                vm.model.jumpTo(index + 1);
                showPosition();
            }
        });
        RecyclerView grid = findViewById(R.id.viewerMoveGrid);
        grid.setLayoutManager(new GridLayoutManager(this, GRID_COLUMNS));
        grid.setAdapter(gridAdapter);
        // Never show the board's constructor centre stone while there is no game to show.
        showEmptyBoard();
        status = findViewById(R.id.viewerStatus);
        retry = findViewById(R.id.viewerRetry);
        progress = findViewById(R.id.viewerProgress);

        // Spec 3.3: swipe left = back, swipe right = forward; the buttons call the same model methods.
        board.setOnSwipeListener(new DBBoardView.OnSwipeListener() {
            @Override
            public void onSwipeLeft() {
                if (vm.state == State.OK && vm.model.back()) showPosition();
            }

            @Override
            public void onSwipeRight() {
                if (vm.state == State.OK && vm.model.forward()) showPosition();
            }
        });
        findViewById(R.id.backButton).setOnClickListener(v -> {
            if (vm.state == State.OK && vm.model.back()) showPosition();
        });
        findViewById(R.id.forwardButton).setOnClickListener(v -> {
            if (vm.state == State.OK && vm.model.forward()) showPosition();
        });

        vm = new ViewModelProvider(this).get(ViewerViewModel.class);
        vm.init(getIntent().getLongExtra(EXTRA_GID, 0L), getIntent().getBooleanExtra(EXTRA_MINE, false));
        vm.onChange = this::render;
        retry.setOnClickListener(v -> {
            vm.load();
            render();
        });
        if (vm.state == State.NONE) vm.load();
        render();
    }

    @Override
    protected void onDestroy() {
        if (vm != null) vm.onChange = null;
        if (loginDialog != null) loginDialog.dismiss();
        if (subscribePopup != null) {
            subscribePopup.setOnDismissListener(null); // no finish() from a teardown dismiss
            subscribePopup.dismiss();
        }
        super.onDestroy();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void render() {
        progress.setVisibility(vm.state == State.LOADING ? View.VISIBLE : View.GONE);
        retry.setVisibility(vm.state == State.FAILED ? View.VISIBLE : View.GONE);
        switch (vm.state) {
            case OK:
                renderGame();
                break;
            case BLOCKED:
                showEmptyBoard();
                setStatus(getString(R.string.db_blocked));
                break;
            case UNAUTHORIZED:
                showEmptyBoard();
                setStatus(getString(R.string.db_login_required));
                showLoginPrompt();
                break;
            case SUBSCRIPTION_REQUIRED:
                // Same gate as the DB screen; closing it goes back there (nothing to show here).
                showEmptyBoard();
                setStatus(null);
                showSubscribeGate();
                break;
            case FAILED:
                showEmptyBoard();
                setStatus(getString(R.string.db_game_error));
                break;
            default:
                showEmptyBoard();
                setStatus(null);
                break;
        }
    }

    /** Clears the board and the header/grid, so nothing stale or default (centre stone) shows. */
    private void showEmptyBoard() {
        if (boardGame == null) board.setGame("Pente"); // gives showPosition a replay target; stays empty
        board.showPosition(Collections.<Integer>emptyList());
        boardGame = null;
        p1Name.setText("");
        p1Rating.setText("");
        p2Name.setText("");
        p2Rating.setText("");
        score.setText("");
        captures.setText("");
        date.setText("");
        gridAdapter.submit(Collections.<MoveGrid.Cell>emptyList());
    }

    private void showSubscribeGate() {
        if (subscribePopup != null && subscribePopup.isShowing()) return;
        // Posted: render() also runs from onCreate, before the window has a token.
        board.post(() -> {
            if (isFinishing() || isDestroyed() || (subscribePopup != null && subscribePopup.isShowing())) return;
            subscribePopup = SubscribeGate.show(this, board, this::finish);
        });
    }

    private void showLoginPrompt() {
        if (loginDialog != null && loginDialog.isShowing()) return;
        loginDialog = new AlertDialog.Builder(this)
                .setMessage(R.string.db_login_required)
                .setPositiveButton(R.string.login, (DialogInterface d, int w) ->
                        startActivity(new Intent(this, LoginActivity.class)))
                .setNegativeButton(R.string.cancel, null)
                .create();
        loginDialog.show();
    }

    private void renderGame() {
        DbDtos.GameHeader h = vm.model.header();
        if (!DbGames.isSupported(h.game)) {
            showEmptyBoard();
            bindHeader(h, null);
            setStatus(getString(R.string.db_unsupported_game));
            return;
        }
        setStatus(null);
        boardGame = DbGames.baseName(h.game);
        board.setGame(boardGame);
        toolbar.setSubtitle((DbGames.isSpeed(h.game) ? "Speed " : "") + boardGame);
        showPosition();
    }

    private void showPosition() {
        if (boardGame == null) return;
        DbDtos.GameHeader h = vm.model.header();
        board.showPosition(vm.model.movesUpTo());
        bindHeader(h, CaptureIcons.of(h.game, vm.model.movesUpTo()));
        gridAdapter.submit(MoveGrid.cells(h.game, vm.model.allMoves(), vm.model.cursor()));
    }

    private void bindHeader(DbDtos.GameHeader h, CaptureIcons.Counts caps) {
        GamesAdapter.bindPlayer(this, p1Name, p1Rating, h.player1);
        GamesAdapter.bindPlayer(this, p2Name, p2Rating, h.player2);
        score.setText(GameRowFormat.score(h.winner));
        date.setText(GameRowFormat.date(h.playDate, ZonedDateTime.now(), Locale.getDefault()));
        int lineHeight = (int) (18 * getResources().getDisplayMetrics().scaledDensity);
        captures.setText(CaptureSpans.build(this, "", caps, lineHeight));
    }

    private void setStatus(String text) {
        status.setText(text);
        status.setVisibility(text == null ? View.GONE : View.VISIBLE);
    }
}
