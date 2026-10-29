package be.submanifold.pentelive.db.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import be.submanifold.pentelive.DBBoardView;
import be.submanifold.pentelive.R;
import be.submanifold.pentelive.db.AuthedDbTransport;
import be.submanifold.pentelive.db.DbApi;
import be.submanifold.pentelive.db.DbDtos;
import be.submanifold.pentelive.db.DbGames;
import be.submanifold.pentelive.db.DbResult;
import be.submanifold.pentelive.db.GameRowFormat;
import be.submanifold.pentelive.db.GameViewerModel;
import be.submanifold.pentelive.db.LatestRequest;

/** Read-only replay of one DB game: {@code GET /games/{gid}}, or {@code /collection/{wgid}} for "mine" rows. */
public class DbGameViewerActivity extends AppCompatActivity {
    private static final String EXTRA_GID = "gid";
    private static final String EXTRA_MINE = "mine";

    public static void start(Context c, long id, boolean mine) {
        c.startActivity(new Intent(c, DbGameViewerActivity.class)
                .putExtra(EXTRA_GID, id)
                .putExtra(EXTRA_MINE, mine));
    }

    enum State { NONE, LOADING, OK, BLOCKED, FAILED }

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
            } else if (GameViewerModel.isBlocked(r)) {
                state = State.BLOCKED;
            } else {
                state = State.FAILED;
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
    private TextView players;
    private TextView moves;
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
        players = findViewById(R.id.viewerPlayers);
        moves = findViewById(R.id.viewerMoves);
        status = findViewById(R.id.viewerStatus);
        retry = findViewById(R.id.viewerRetry);
        progress = findViewById(R.id.viewerProgress);

        // Spec 3.3: swipe left = back, swipe right = forward.
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
                setStatus(getString(R.string.db_blocked));
                break;
            case FAILED:
                setStatus(getString(R.string.db_game_error));
                break;
            default:
                setStatus(null);
                break;
        }
    }

    private void renderGame() {
        DbDtos.GameHeader h = vm.model.header();
        if (!DbGames.isSupported(h.game)) {
            players.setText(playersLine(h));
            setStatus(getString(R.string.db_unsupported_game));
            return;
        }
        setStatus(null);
        boardGame = DbGames.baseName(h.game);
        board.setGame(boardGame);
        toolbar.setSubtitle((DbGames.isSpeed(h.game) ? "Speed " : "") + boardGame);
        players.setText(playersLine(h));
        showPosition();
    }

    private void showPosition() {
        if (boardGame == null) return;
        board.showPosition(vm.model.movesUpTo());
        board.setTextViewHTML(moves, "");
    }

    private void setStatus(String text) {
        status.setText(text);
        status.setVisibility(text == null ? View.GONE : View.VISIBLE);
    }

    private String playersLine(DbDtos.GameHeader h) {
        String date = GameRowFormat.date(h.playDate, ZonedDateTime.now(), Locale.getDefault());
        String line = player(h.player1) + "   " + GameRowFormat.score(h.winner) + "   " + player(h.player2);
        return date == null || date.isEmpty() ? line : line + "\n" + date;
    }

    private static String player(DbDtos.Player p) {
        if (p == null || p.name == null) return "?";
        return p.rating > 0 ? p.name + " (" + p.rating + ")" : p.name;
    }
}
