package be.submanifold.pentelive;


import static android.view.View.VISIBLE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import be.submanifold.pentelive.db.AuthedDbTransport;
import be.submanifold.pentelive.db.CaptureIcons;
import be.submanifold.pentelive.db.DbApi;
import be.submanifold.pentelive.db.DbController;
import be.submanifold.pentelive.db.DbDtos;
import be.submanifold.pentelive.db.DbFilterStore;
import be.submanifold.pentelive.db.DbFilters;
import be.submanifold.pentelive.db.FilterSummary;
import be.submanifold.pentelive.db.DbGames;
import be.submanifold.pentelive.db.DbStart;
import be.submanifold.pentelive.db.DbView;
import be.submanifold.pentelive.db.GameRowFormat;
import be.submanifold.pentelive.db.SharedPrefsKeyValueStore;
import be.submanifold.pentelive.db.ui.DbFiltersSheet;
import be.submanifold.pentelive.db.ui.DbGameViewerActivity;
import be.submanifold.pentelive.db.ui.GamesAdapter;
import be.submanifold.pentelive.db.ui.CaptureSpans;
import be.submanifold.pentelive.db.ui.DbViewModel;
import be.submanifold.pentelive.db.ui.MovesAdapter;
import be.submanifold.pentelive.db.ui.SubscribeGate;


import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class DatabaseActivity extends AppCompatActivity implements DbView, DBBoardView.BoardListener {

    private DBBoardView board;
    private View aiSettingsView;

    private ProgressBar progressBar;
    private AlertDialog aiSearchPrmtrsWindow;
    private DbViewModel dbModel;
    private DbController controller;
    private MovesAdapter movesAdapter;
    private GamesAdapter gamesAdapter;
    private TabLayout.Tab gamesTab;
    private boolean gamesSelected;
    private CharSequence movesStatus;
    private CharSequence gamesStatus;
    private TextView dbStatus;
    private PopupWindow subscribePopup;
    private Snackbar errorSnackbar;
    private AlertDialog loginDialog;
    private TabLayout dbTabs;
    private RecyclerView dbList;
    private android.os.Parcelable pendingListState;

    private static final String STATE_MOVES = "db_moves";
    private static final String STATE_REDO = "db_redo";
    private static final String STATE_TAB = "db_tab";
    private static final String STATE_LIST = "db_list";
    private static final String STATE_GAME = "db_game";
    private static final String STATE_GAME_LABEL = "db_game_label";

//    private final char[] coordinateLetters = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T'};

    // InflateParams: aiSettingsView is AlertDialog content (setView), no parent at inflation time.
    @SuppressLint("InflateParams")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_database);
        Toolbar toolbar = findViewById(R.id.toolbar);
        // Capture counts now live in the toolbar subtitle (CaptureSpans); this view only keeps the layout.
        findViewById(R.id.capturesView).setVisibility(View.INVISIBLE);
        aiSettingsView = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.db_ai_settings, null, false);
        AlertDialog.Builder helpBuilder = new AlertDialog.Builder(this);
        helpBuilder.setTitle(getString(R.string.ai_parameters));
        helpBuilder.setView(aiSettingsView);
        aiSearchPrmtrsWindow = helpBuilder.create();
        aiSearchPrmtrsWindow.setOnDismissListener(dialogInterface -> board.setAlpha(1.0f));
        aiSearchPrmtrsWindow.setCanceledOnTouchOutside(true);

        progressBar = findViewById(R.id.progressBar);

        board = findViewById(R.id.boardView);
        board.setActivity(this);
        board.setBoardListener(this);
        board.setDotsVisible(PrefUtils.getBooleanFromPrefs(this, PrefUtils.PREFS_DBHEAT_KEY, true));

        setUpMovesSheet();

        dbModel = new ViewModelProvider(this).get(DbViewModel.class);
        if (!dbModel.hasController()) {
            // The extras game and moves win for this session and are not persisted. After process death the
            // saved game wins instead, so the saved moves are never replayed on the extras game.
            Bundle extras = getIntent().getExtras();
            boolean restoring = savedInstanceState != null && savedInstanceState.containsKey(STATE_GAME);
            DbStart start = DbStart.resolve(
                    extras != null ? extras.getString("game") : null,
                    extras != null ? extras.getIntegerArrayList("moves") : null,
                    restoring ? savedInstanceState.getInt(STATE_GAME) : null,
                    restoring ? savedInstanceState.getString(STATE_GAME_LABEL) : null);
            DbFilterStore filterStore = new DbFilterStore(new SharedPrefsKeyValueStore(this),
                    new DbFilterStore.LegacyLabels(getString(R.string.both), getString(R.string.yes),
                            getString(R.string.live_only), getString(R.string.turn_based_only)));
            DbController created = new DbController(new DbApi(new AuthedDbTransport()), dbModel.io(), dbModel.main(),
                    filterStore, start.gameOverride, start.initialMoves);
            if (savedInstanceState != null) {
                restoreState(created, savedInstanceState);
            }
            dbModel.setController(created, start.label != null ? start.label : gameTypeLabel(created.gameId()));
            created.loadFeatures(); // one GET /ping per ViewModel
        }
        controller = dbModel.controller();
        board.setGame(dbModel.gameLabel());

        updateAiButton();

        toolbar.setTitle(getString(R.string.db_title));
        setSupportActionBar(toolbar);

//            InputStream tbl = null;
//            InputStream scs = null;
//            InputStream opnbk = null;
//            try {
//                Resources resources = getResources();
//                tbl = resources.openRawResource(R.raw.pente_tbl);
//                scs = resources.openRawResource(R.raw.pente_scs);
//                opnbk = resources.openRawResource(R.raw.opngbk);
//
//                //computer.setSize(size);
//
//                Ai nativeComputer = new Ai(1, 1, 0, 1, 19);
//                nativeComputer.init(scs, opnbk, tbl);
////            nativeComputer.setVisualization(false);
//
//                board.setAiPlayer(nativeComputer);

//            nativeComputer.addAiListener(new AiListener() {
//                public void aiEvaluateCallBack() {}
//                public void aiVisualizationCallBack(int[] bd) {};
//
//                public void moveReady(int[] moves, final int newMove) {
//                    //Log.d("ai", "move ready " + newMove + " in " + (System.currentTimeMillis() - nativeStart));
//                    // add move on UI thread
//                    board.post(new Runnable() {
//                        public void run() {
//                            // just make sure its computer's turn, shouldn't be needed...
//                            if (state.getCurrentPlayer() == (3 - seat)) {
//                                addMove(newMove, true);
//
//                                updateCaps(true);
//
//                                if (state.isGameOver()) {
//                                    status = STATUS_GAME_OVER;
//                                    alertGameOver();
//                                }
//
//                                board.setAiThinking(false);
//                                if (nativeComputer.getLevel() > 2) {
//                                    board.bringToFront();
//                                    pb.setVisibility(ProgressBar.INVISIBLE);
//                                }
//                            }
//                        }
//                    });
//                }
//                public void startThinking() {};
//
//                public void stopThinking() {}
//            });

//            } catch (Throwable t) {
//                Log.v("ai", "error init", t);
//            }


        // Swipe left goes back, swipe right redoes (the controller owns the position).
        board.setOnSwipeListener(new DBBoardView.OnSwipeListener() {
            @Override
            public void onSwipeLeft() {
                controller.back();
            }

            @Override
            public void onSwipeRight() {
                controller.forward();
            }
        });

        toolbar.setOnMenuItemClickListener(menuItem -> {
            if (menuItem.getItemId() == R.id.action_db_filters) {
                showFilters();
                return true;
            }
            if (menuItem.getItemId() == R.id.action_db_heat) {
                boolean on = !menuItem.isChecked();
                menuItem.setChecked(on);
                PrefUtils.saveBooleanToPrefs(DatabaseActivity.this, PrefUtils.PREFS_DBHEAT_KEY, on);
                board.setDotsVisible(on);
                return true;
            }

            return false;
        });
        findViewById(R.id.aiButton).setOnClickListener(view -> showAISettings());
        Spinner spinner = aiSettingsView.findViewById(R.id.difficultySpinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(DatabaseActivity.this,
                R.array.ai_difficulty_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                PrefUtils.saveIntToPrefs(DatabaseActivity.this, PrefUtils.PREFS_DBAIDIFFICULTY_KEY, position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        ((ToggleButton) aiSettingsView.findViewById(R.id.openingBookToggleButton)).setOnCheckedChangeListener((compoundButton, b) -> PrefUtils.saveBooleanToPrefs(DatabaseActivity.this, PrefUtils.PREFS_DBAIOPENINGBOOK_KEY, b));
        aiSettingsView.findViewById(R.id.runAIbutton).setOnClickListener(view -> {
            board.setCandidateDots(null);
            aiSearchPrmtrsWindow.dismiss();
            if (board.getAiPlayer() == null) {
                InputStream tbl = null;
                InputStream scs = null;
                InputStream opnbk = null;
                try {
                    Resources resources = getResources();
                    tbl = resources.openRawResource(R.raw.pente_tbl);
                    scs = resources.openRawResource(R.raw.pente_scs);
                    opnbk = resources.openRawResource(R.raw.opngbk);

                    //computer.setSize(size);

                    Ai nativeComputer = new Ai(1, 1, 0, 1, 19);
                    nativeComputer.init(scs, opnbk, tbl, getFilesDir());
                    board.setAiPlayer(nativeComputer);
                } catch (Throwable t) {
                    Log.v("ai", "error init", t);
                }
            }
            Ai ai = board.getAiPlayer();
            boolean useBook = ((ToggleButton) aiSettingsView.findViewById(R.id.openingBookToggleButton)).isChecked();
            ai.useOpeningBook(useBook);
            if (board.getGame().contains("Keryo-Pente")) {
                ai.setGame(3);
            } else if (board.getGame().equals("Pente") || board.getGame().equals("Speed Pente")) {
                ai.setGame(1);
            }
            ai.setLevel(((Spinner) aiSettingsView.findViewById(R.id.difficultySpinner)).getSelectedItemPosition() + 1);
            ai.setDbBoard(board);
            int[] moves = new int[board.getMovesList().size()];
            for (int i = 0; i < board.getMovesList().size(); ++i) {
                moves[i] = board.getMovesList().get(i).intValue();
            }
            ai.getMove(moves);
        });

        getSupportFragmentManager().setFragmentResultListener(DbFiltersSheet.RESULT_KEY, this,
                (key, bundle) -> applyFiltersFromSheet(DbFilters.fromJson(bundle.getString(DbFiltersSheet.RESULT_JSON))));
        findViewById(R.id.dbFilterChip).setOnClickListener(v -> showFilters());
        updateFilterChip();
        controller.attach(this);
    }

    /**
     * Process death only (the ViewModel is new): puts back the saved position and redo stack, the selected
     * tab, and the list scroll position, which is applied once the first results for that tab render.
     */
    private void restoreState(DbController created, Bundle state) {
        ArrayList<Integer> moves = state.getIntegerArrayList(STATE_MOVES);
        if (moves != null) {
            ArrayList<Integer> redo = state.getIntegerArrayList(STATE_REDO);
            created.restore(moves, redo == null ? new ArrayList<Integer>() : redo);
        }
        int tab = state.getInt(STATE_TAB, 0);
        TabLayout.Tab t = dbTabs.getTabAt(tab);
        if (t != null && tab != 0) {
            t.select();
        }
        pendingListState = state.getParcelable(STATE_LIST);
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        if (controller == null) return;
        out.putInt(STATE_GAME, controller.gameId());
        out.putString(STATE_GAME_LABEL, dbModel.gameLabel());
        out.putIntegerArrayList(STATE_MOVES, new ArrayList<>(controller.moves()));
        out.putIntegerArrayList(STATE_REDO, new ArrayList<>(controller.redoMoves()));
        out.putInt(STATE_TAB, gamesSelected ? 1 : 0);
        RecyclerView.LayoutManager lm = dbList.getLayoutManager();
        if (lm != null) {
            out.putParcelable(STATE_LIST, lm.onSaveInstanceState());
        }
    }

    /** Applies the saved scroll state once, after the matching tab has rows to scroll. */
    private void applyPendingListState(boolean forGames) {
        if (pendingListState == null || gamesSelected != forGames) return;
        RecyclerView.LayoutManager lm = dbList.getLayoutManager();
        android.os.Parcelable state = pendingListState;
        pendingListState = null;
        if (lm != null) {
            dbList.post(() -> lm.onRestoreInstanceState(state));
        }
    }

    @Override
    protected void onDestroy() {
        if (controller != null) {
            controller.detach();
        }
        if (subscribePopup != null) {
            subscribePopup.dismiss();
        }
        if (loginDialog != null) {
            loginDialog.dismiss();
        }
        super.onDestroy();
    }

    private static final int[] SHOW_COUNTS = {10, 20, 50, 0};

    private void setUpMovesSheet() {
        dbStatus = findViewById(R.id.dbStatus);
        BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(findViewById(R.id.dbSheet));
        behavior.setFitToContents(false);
        behavior.setHalfExpandedRatio(0.5f);
        behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        TabLayout tabs = findViewById(R.id.dbTabs);
        dbTabs = tabs;
        tabs.addTab(tabs.newTab().setText(R.string.db_moves));
        tabs.addTab(tabs.newTab().setText(R.string.db_games));
        gamesTab = tabs.getTabAt(1);

        movesAdapter = new MovesAdapter(cell -> controller.playCandidate(cell));
        RecyclerView list = findViewById(R.id.dbMovesList);
        dbList = list;
        list.setLayoutManager(new LinearLayoutManager(this));
        gamesAdapter = new GamesAdapter(
                h -> DbGameViewerActivity.start(this, h.gid, h.isMine()),
                () -> controller.loadMoreGames());
        list.setAdapter(movesAdapter);
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                gamesSelected = tab.getPosition() == 1;
                list.setAdapter(gamesSelected ? gamesAdapter : movesAdapter);
                findViewById(R.id.dbShowRow).setVisibility(gamesSelected ? View.GONE : View.VISIBLE);
                refreshStatus();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        Spinner show = findViewById(R.id.dbShowSpinner);
        String[] labels = new String[SHOW_COUNTS.length];
        for (int i = 0; i < SHOW_COUNTS.length; i++) {
            labels[i] = SHOW_COUNTS[i] == 0 ? getString(R.string.db_show_all) : getString(R.string.db_show_n, SHOW_COUNTS[i]);
        }
        ArrayAdapter<String> showAdapter = new ArrayAdapter<>(show.getContext(), android.R.layout.simple_spinner_item, labels);
        showAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        show.setAdapter(showAdapter);
        show.setSelection(1, false); // 20, the controller's default
        show.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (controller != null) controller.setShowCount(SHOW_COUNTS[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setStatus(CharSequence text) {
        movesStatus = text;
        gamesStatus = text;
        refreshStatus();
    }

    private void refreshStatus() {
        CharSequence text = gamesSelected ? gamesStatus : movesStatus;
        dbStatus.setText(text);
        dbStatus.setVisibility(text == null || text.length() == 0 ? View.GONE : View.VISIBLE);
    }

    private void setGamesTotal(int total) {
        gamesTab.setText(total > 0 ? getString(R.string.db_games_count, total) : getString(R.string.db_games));
    }

    private void updateAiButton() {
        int id = controller.gameId();
        boolean pente = id != 0 && DbGames.isSupported(id) && isAiGame(DbGames.baseName(id));
        findViewById(R.id.aiButton).setVisibility(pente ? VISIBLE : View.GONE);
    }

    private static boolean isAiGame(String baseName) {
        return baseName.equals("Pente") || baseName.contains("Keryo-Pente");
    }

    // BoardListener

    @Override
    public void onMovePlayed(int cell) {
        controller.onBoardMove(cell);
    }

    @Override
    public void onAiMove(int cell) {
        aiStopped();
        controller.onBoardMove(cell);
    }

    // DbView

    @Override
    public void showPosition(List<Integer> moves, CaptureIcons.Counts caps) {
        board.showPosition(moves);
        board.setTextViewHTML(findViewById(R.id.playerInfo), "");
        Toolbar toolbar = findViewById(R.id.toolbar);
        int lineHeight = (int) (18 * getResources().getDisplayMetrics().scaledDensity);
        toolbar.setSubtitle(CaptureSpans.build(this, gameName(), caps, lineHeight));
    }

    private String gameName() {
        int id = controller.gameId();
        if (id == 0 || !DbGames.isSupported(id)) {
            return dbModel.gameLabel() == null ? "" : dbModel.gameLabel();
        }
        return (DbGames.isSpeed(id) ? "Speed " : "") + DbGames.baseName(id);
    }

    @Override
    public void showDots(Map<Integer, Integer> cellArgb) {
        progressBar.setVisibility(View.GONE);
        board.setCandidateDots(cellArgb);
    }

    @Override
    public void showMoves(List<DbController.MoveRow> rows) {
        progressBar.setVisibility(View.GONE);
        movesAdapter.setGridSize(DbGames.gridSize(controller.gameId()));
        movesAdapter.submit(rows);
        movesStatus = rows.isEmpty() ? getString(R.string.db_no_moves) : null;
        refreshStatus();
        applyPendingListState(false);
    }

    @Override
    public void clearResults() {
        board.setCandidateDots(null);
        board.setTextViewHTML(findViewById(R.id.playerInfo), "");
        movesAdapter.submit(java.util.Collections.emptyList());
        gamesAdapter.submit(java.util.Collections.emptyList(), false, "");
        setGamesTotal(0);
        setStatus(null);
        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void showBlocked() {
        progressBar.setVisibility(View.GONE);
        board.setCandidateDots(null);
        movesAdapter.submit(java.util.Collections.emptyList());
        gamesAdapter.submit(java.util.Collections.emptyList(), false, "");
        setGamesTotal(0);
        setStatus(getString(R.string.db_blocked));
    }

    @Override
    public void showUnsupported() {
        progressBar.setVisibility(View.GONE);
        board.setCandidateDots(null);
        ((Toolbar) findViewById(R.id.toolbar)).setSubtitle(gameName());
        movesAdapter.submit(java.util.Collections.emptyList());
        gamesAdapter.submit(java.util.Collections.emptyList(), false, "");
        setGamesTotal(0);
        setStatus(getString(R.string.db_unsupported_game));
        updateAiButton();
    }

    @Override
    public void showLoginPrompt() {
        progressBar.setVisibility(View.GONE);
        if (loginDialog != null && loginDialog.isShowing()) {
            return;
        }
        loginDialog = new AlertDialog.Builder(this)
                .setMessage(R.string.db_login_required)
                .setPositiveButton(R.string.login, (d, w) -> startActivity(new Intent(this, LoginActivity.class)))
                .setNegativeButton(R.string.cancel, null)
                .create();
        loginDialog.show();
    }

    @Override
    public void showSubscribeGate() {
        progressBar.setVisibility(View.GONE);
        if (subscribePopup != null && subscribePopup.isShowing()) {
            return;
        }
        subscribePopup = SubscribeGate.show(this, board, null);
    }

    /** One Snackbar for both endpoints: its Retry refetches whichever failed, so neither error hides the other. */
    @Override
    public void showError(DbController.Endpoint e) {
        progressBar.setVisibility(View.GONE);
        if (errorSnackbar != null && errorSnackbar.isShownOrQueued()) return;
        errorSnackbar = Snackbar.make(board, R.string.db_error, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.db_retry, v -> controller.retryFailed())
                .setAnchorView(R.id.dbSheet);
        errorSnackbar.show();
    }

    @Override
    public void showServerFilters(boolean enabled) {
        updateFilterChip();
        androidx.fragment.app.Fragment sheet = getSupportFragmentManager().findFragmentByTag(DbFiltersSheet.TAG);
        if (sheet instanceof DbFiltersSheet) {
            ((DbFiltersSheet) sheet).setServerFilters(enabled);
        }
    }

    @Override
    public void showGames(List<DbDtos.GameHeader> games, int total, boolean hasMore) {
        gamesAdapter.submit(games, hasMore,
                GameRowFormat.listHeader(controller.scope(), controller.moves().size()));
        setGamesTotal(total);
        gamesStatus = games.isEmpty() ? getString(R.string.db_no_games) : null;
        refreshStatus();
        applyPendingListState(true);
    }

    /** The database_game_types_array label whose DB id is gameId (first entry when none matches). */
    private String gameTypeLabel(int gameId) {
        String[] types = getResources().getStringArray(R.array.database_game_types_array);
        for (String t : types) {
            Integer id = DbGames.fromGameType(t);
            if (id != null && id == gameId) return t;
        }
        return types[0];
    }

    private void showFilters() {
        if (getSupportFragmentManager().findFragmentByTag(DbFiltersSheet.TAG) != null) return;
        DbFiltersSheet.newInstance(controller.appliedFilters()).show(getSupportFragmentManager(), DbFiltersSheet.TAG);
    }

    private void applyFiltersFromSheet(DbFilters f) {
        int newGameId = f.gameId();
        if (newGameId != controller.gameId()) {
            String label = gameTypeLabel(newGameId);
            dbModel.setGameLabel(label);
            board.setGame(label);
        }
        controller.applyFilters(f);
        updateAiButton();
        updateFilterChip();
    }

    private void updateFilterChip() {
        ((TextView) findViewById(R.id.dbFilterText)).setText(FilterSummary.text(controller.appliedFilters(), controller.serverFilters()));
    }

    private void showAISettings() {
        aiSearchPrmtrsWindow.show();
        Spinner spinner = aiSettingsView.findViewById(R.id.difficultySpinner);
        spinner.setSelection(PrefUtils.getIntFromPrefs(DatabaseActivity.this, PrefUtils.PREFS_DBAIDIFFICULTY_KEY, 0));
        ((ToggleButton) aiSettingsView.findViewById(R.id.openingBookToggleButton)).setChecked(PrefUtils.getBooleanFromPrefs(DatabaseActivity.this, PrefUtils.PREFS_DBAIOPENINGBOOK_KEY, false));
        aiSearchPrmtrsWindow.setOnDismissListener(dialogInterface -> board.setAlpha(1.0f));
        board.setAlpha(0.05f);
    }

    private void startAI() {
        progressBar.setVisibility(View.VISIBLE);
    }

    public void aiStopped() {
        progressBar.setVisibility(View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        MyApplication.activityResumed(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MyApplication.activityPaused();
    }

//        public void showThinking() {
//            progressBar.setVisibility(View.VISIBLE);
//        }
//        public void hideThinking() {
//            progressBar.setVisibility(View.GONE);
//        }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.db_menu, menu);

        ColorStateList tintList = Helpers.tintList(this);
        MenuItem menuItem = menu.findItem(R.id.action_db_filters);
        menuItem.setIconTintList(tintList);
        menu.findItem(R.id.action_db_heat).setChecked(PrefUtils.getBooleanFromPrefs(this, PrefUtils.PREFS_DBHEAT_KEY, true));

        return true;
    }

}
