package be.submanifold.pentelive;


import static android.view.View.VISIBLE;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
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
import be.submanifold.pentelive.db.DbGames;
import be.submanifold.pentelive.db.DbView;
import be.submanifold.pentelive.db.ui.CaptureSpans;
import be.submanifold.pentelive.db.ui.DbViewModel;
import be.submanifold.pentelive.db.ui.MovesAdapter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;


public class DatabaseActivity extends AppCompatActivity implements DbView, DBBoardView.BoardListener {

    private DBBoardView board;
    private View settingsView, aiSettingsView;

    private String dbP1Rating = "0", dbP2Rating = "0";
    private ProgressBar progressBar;
    private AlertDialog searchPrmtrsWindow, aiSearchPrmtrsWindow;
    private DbViewModel dbModel;
    private DbController controller;
    private MovesAdapter movesAdapter;
    private TextView dbStatus;
    private PopupWindow subscribePopup;
    private AlertDialog loginDialog;

//    private final char[] coordinateLetters = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T'};

    private DatePickerDialog afterDatePickerDialog, beforeDatePickerDialog;

    // InflateParams: settingsView/aiSettingsView are AlertDialog content (setView), no parent at inflation time.
    // ClickableViewAccessibility: the spinner OnTouchListeners only hide the keyboard and return false, so the
    // spinner's own click handling still runs; calling performClick() there would double-fire it.
    @SuppressLint({"InflateParams", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_database);
        Toolbar toolbar = findViewById(R.id.toolbar);
        // Capture counts now live in the toolbar subtitle (CaptureSpans); this view only keeps the layout.
        findViewById(R.id.capturesView).setVisibility(View.INVISIBLE);
        settingsView = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.database_options, null, false);
        AlertDialog.Builder helpBuilder = new AlertDialog.Builder(this);
        helpBuilder.setTitle(getString(R.string.search_parameters));
        helpBuilder.setView(settingsView);
        searchPrmtrsWindow = helpBuilder.create();
        searchPrmtrsWindow.setOnDismissListener(dialogInterface -> {
            board.setAlpha(1.0f);
        });
        searchPrmtrsWindow.setCanceledOnTouchOutside(true);
        aiSettingsView = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.db_ai_settings, null, false);
        helpBuilder = new AlertDialog.Builder(this);
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
            // The extras game and moves win for this session and are not persisted.
            Bundle extras = getIntent().getExtras();
            String label = extras != null ? extras.getString("game") : null;
            ArrayList<Integer> extraMoves = extras != null ? extras.getIntegerArrayList("moves") : null;
            if (label == null) {
                migrateDbGameIndexForSwap2();
                String[] types = getResources().getStringArray(R.array.database_game_types_array);
                int idx = PrefUtils.getIntFromPrefs(this, PrefUtils.PREFS_DBGAME_KEY, 0);
                label = types[Math.max(0, Math.min(idx, types.length - 1))];
                extraMoves = null;
            }
            Integer gameId = DbGames.fromGameType(label);
            dbModel.setController(new DbController(new DbApi(new AuthedDbTransport()), dbModel.io(), dbModel.main(),
                    gameId == null ? 0 : gameId, extraMoves), label);
        }
        controller = dbModel.controller();
        board.setGame(dbModel.gameLabel());
        updateAiButton();

        toolbar.setTitle(getString(R.string.db_title));
        setSupportActionBar(toolbar);

//            Display display = getWindowManager().getDefaultDisplay();
//            Point size = new Point();
//            display.getSize(size);
//            settingsWindow = new PopupWindow(settingsView, size.x*2/3, ViewGroup.LayoutParams.WRAP_CONTENT, true );
//            settingsWindow.setFocusable(true);
//            settingsWindow.setOutsideTouchable(true);
//            settingsWindow.setBackgroundDrawable(ContextCompat.getDrawable(DatabaseActivity.this, R.drawable.border));
////                        messageWindow.setAnimationStyle(R.anim.animation);

        Spinner spinner = settingsView.findViewById(R.id.gameSpinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(DatabaseActivity.this,
                R.array.database_game_types_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String game = (String) parent.getItemAtPosition(position);
                if (game.equals(dbModel.gameLabel())) {
                    return; // no change (also the programmatic selection when the dialog opens)
                }
                PrefUtils.saveIntToPrefs(DatabaseActivity.this, PrefUtils.PREFS_DBGAME_KEY, position);
                dbModel.setGameLabel(game);
                board.setGame(game);
                Integer dbId = DbGames.fromGameType(game);
                controller.setGame(dbId == null ? 0 : dbId);
                updateAiButton();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        spinner.setOnTouchListener((view, motionEvent) -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            return false;
        });
        TextView sortChoice = settingsView.findViewById(R.id.sortChoice);
        sortChoice.setOnClickListener(v -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            TextView tv = (TextView) v;
            if (tv.getText().equals(getString(R.string.popularity))) {
                tv.setText(getString(R.string.win_percantage));
                PrefUtils.saveToPrefs(DatabaseActivity.this, PrefUtils.PREFS_DBSORT_KEY, "win percentage");
            } else {
                tv.setText(getString(R.string.popularity));
                PrefUtils.saveToPrefs(DatabaseActivity.this, PrefUtils.PREFS_DBSORT_KEY, "popularity");
            }
        });
        TextView winner = settingsView.findViewById(R.id.winner);
        winner.setOnClickListener(v -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            TextView tv = (TextView) v;
            if (tv.getText().equals(getString(R.string.either))) {
                tv.setText(getString(R.string.player1));
            } else if (tv.getText().equals(getString(R.string.player1))) {
                tv.setText(getString(R.string.player2));
            } else {
                tv.setText(getString(R.string.either));
            }
        });
//            settingsWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
//                @Override
//                public void onDismiss() {
//                    board.setAlpha(1.0f);
//                }
//            });


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


        Button button = findViewById(R.id.searchButton);
        if (button != null) button.setOnClickListener(v -> doSearch());
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
            if (menuItem.getItemId() == R.id.action_db_settings) {
                showDBSettings();

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
        settingsView.findViewById(R.id.player1).setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
        });
        settingsView.findViewById(R.id.player2).setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
        });

        Calendar newCalendar = Calendar.getInstance();
        afterDatePickerDialog = new DatePickerDialog(this, DatePickerDialog.THEME_HOLO_LIGHT, (view, year, monthOfYear, dayOfMonth) -> {
            Calendar newDate = Calendar.getInstance();
            newDate.set(year, monthOfYear, dayOfMonth);
            DateFormat dateFormatter = new SimpleDateFormat("MM/dd/yyyy", java.util.Locale.US);
            ((TextView) settingsView.findViewById(R.id.afterDate)).setText(dateFormatter.format(newDate.getTime()));
        }, newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH));
        afterDatePickerDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.clear), (dialog, which) -> {
            if (which == DialogInterface.BUTTON_NEGATIVE) {
                // Do Stuff
                ((TextView) settingsView.findViewById(R.id.afterDate)).setText("");
            }
        });
        settingsView.findViewById(R.id.afterDate).setOnClickListener(view -> afterDatePickerDialog.show());
        beforeDatePickerDialog = new DatePickerDialog(this, DatePickerDialog.THEME_HOLO_LIGHT, (view, year, monthOfYear, dayOfMonth) -> {
            Calendar newDate = Calendar.getInstance();
            newDate.set(year, monthOfYear, dayOfMonth);
            DateFormat dateFormatter = new SimpleDateFormat("MM/dd/yyyy", java.util.Locale.US);
            ((TextView) settingsView.findViewById(R.id.beforeDate)).setText(dateFormatter.format(newDate.getTime()));
        }, newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH));
        beforeDatePickerDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.clear), (dialog, which) -> {
            if (which == DialogInterface.BUTTON_NEGATIVE) {
                // Do Stuff
                ((TextView) settingsView.findViewById(R.id.beforeDate)).setText("");
            }
        });
        settingsView.findViewById(R.id.beforeDate).setOnClickListener(view -> beforeDatePickerDialog.show());
        spinner = settingsView.findViewById(R.id.p1RatingSpinner);
        adapter = ArrayAdapter.createFromResource(DatabaseActivity.this,
                R.array.db_ratings_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                PrefUtils.saveIntToPrefs(DatabaseActivity.this, PrefUtils.PREFS_DBP1RATING_KEY, position);
                String[] ratingArray = getResources().getStringArray(R.array.db_ratings_array);
                dbP1Rating = ratingArray[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        spinner.setOnTouchListener((view, motionEvent) -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            return false;
        });
        spinner = settingsView.findViewById(R.id.p2RatingSpinner);
        adapter = ArrayAdapter.createFromResource(DatabaseActivity.this,
                R.array.db_ratings_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                PrefUtils.saveIntToPrefs(DatabaseActivity.this, PrefUtils.PREFS_DBP2RATING_KEY, position);
                String[] ratingArray = getResources().getStringArray(R.array.db_ratings_array);
                dbP2Rating = ratingArray[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        spinner.setOnTouchListener((view, motionEvent) -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            return false;
        });
        TextView txtvw = settingsView.findViewById(R.id.eitherOrBoth);
        txtvw.setText(PrefUtils.getFromPrefs(DatabaseActivity.this, PrefUtils.PREFS_DBPEITHERORBOTH_KEY, getString(R.string.both)));
        txtvw.setOnClickListener(view -> {
            if (((TextView) view).getText().equals(getString(R.string.both))) {
                ((TextView) view).setText(getString(R.string.either));
            } else {
                ((TextView) view).setText(getString(R.string.both));
            }
            PrefUtils.saveToPrefs(DatabaseActivity.this, PrefUtils.PREFS_DBPEITHERORBOTH_KEY, ((TextView) view).getText().toString());
        });
        txtvw = settingsView.findViewById(R.id.excludeTimeouts);
        txtvw.setText(PrefUtils.getFromPrefs(DatabaseActivity.this, PrefUtils.PREFS_DBEXCLUDETIMEOUTS_KEY, getString(R.string.no)));
        txtvw.setOnClickListener(view -> {
            if (((TextView) view).getText().equals(getString(R.string.no))) {
                ((TextView) view).setText(getString(R.string.yes));
            } else {
                ((TextView) view).setText(getString(R.string.no));
            }
            PrefUtils.saveToPrefs(DatabaseActivity.this, PrefUtils.PREFS_DBEXCLUDETIMEOUTS_KEY, ((TextView) view).getText().toString());
        });
        txtvw = settingsView.findViewById(R.id.liveOrTB);
        txtvw.setText(PrefUtils.getFromPrefs(DatabaseActivity.this, PrefUtils.PREFS_DBLIVEORTB_KEY, getString(R.string.all)));
        txtvw.setOnClickListener(view -> {
            if (((TextView) view).getText().equals(getString(R.string.all))) {
                ((TextView) view).setText(getString(R.string.turn_based_only));
            } else if (((TextView) view).getText().equals(getString(R.string.turn_based_only))) {
                ((TextView) view).setText(getString(R.string.live_only));
            } else {
                ((TextView) view).setText(getString(R.string.all));
            }
            PrefUtils.saveToPrefs(DatabaseActivity.this, PrefUtils.PREFS_DBLIVEORTB_KEY, ((TextView) view).getText().toString());
        });


        findViewById(R.id.aiButton).setOnClickListener(view -> showAISettings());
        spinner = aiSettingsView.findViewById(R.id.difficultySpinner);
        adapter = ArrayAdapter.createFromResource(DatabaseActivity.this,
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

        // Unsupported (Go/Renju) keeps its static state; don't let the settings
        // spinner or a search replace it.
        if (savedInstanceState == null && DbGames.isSupported(controller.gameId())) {
            board.post(() -> {
                if (board.getMovesList().size() <= 1) {
                    showDBSettings();
                } else {
                    doSearch();
                }
            });
        }
        controller.attach(this);
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
        tabs.addTab(tabs.newTab().setText(R.string.db_moves));
        // The Games tab arrives with the games list.

        movesAdapter = new MovesAdapter(cell -> controller.playCandidate(cell));
        RecyclerView list = findViewById(R.id.dbMovesList);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(movesAdapter);

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
        dbStatus.setText(text);
        dbStatus.setVisibility(text == null || text.length() == 0 ? View.GONE : View.VISIBLE);
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
        setStatus(rows.isEmpty() ? getString(R.string.db_no_moves) : null);
    }

    @Override
    public void clearResults() {
        board.setCandidateDots(null);
        board.setTextViewHTML(findViewById(R.id.playerInfo), "");
        movesAdapter.submit(java.util.Collections.emptyList());
        setStatus(null);
        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void showBlocked() {
        progressBar.setVisibility(View.GONE);
        board.setCandidateDots(null);
        movesAdapter.submit(java.util.Collections.emptyList());
        setStatus(getString(R.string.db_blocked));
    }

    @Override
    public void showUnsupported() {
        progressBar.setVisibility(View.GONE);
        board.setCandidateDots(null);
        ((Toolbar) findViewById(R.id.toolbar)).setSubtitle(gameName());
        movesAdapter.submit(java.util.Collections.emptyList());
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
    @SuppressLint("InflateParams")
    public void showSubscribeGate() {
        progressBar.setVisibility(View.GONE);
        if (subscribePopup != null && subscribePopup.isShowing()) {
            return;
        }
        android.graphics.Point size = new android.graphics.Point(getResources().getDisplayMetrics().widthPixels,
                getResources().getDisplayMetrics().heightPixels);
        View policyView = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.db_subscribers_only, null, false);
        subscribePopup = new PopupWindow(policyView, size.x * 9 / 10, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        policyView.findViewById(R.id.subscribeButton).setOnClickListener(view -> {
            subscribePopup.dismiss();
            startActivity(new Intent(this, StoreActivity.class));
        });
        subscribePopup.setFocusable(true);
        subscribePopup.setOutsideTouchable(true);
        ((TextView) policyView.findViewById(R.id.informationView)).setText(getString(R.string.level_up_your_game));
        subscribePopup.setOnDismissListener(() -> board.setAlpha(1.0f));
        board.setAlpha(0.05f);
        subscribePopup.showAtLocation(board, Gravity.TOP, 0, 260);
    }

    @Override
    public void showError(DbController.Endpoint e) {
        progressBar.setVisibility(View.GONE);
        Snackbar.make(board, R.string.db_error, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.db_retry, v -> controller.retry(e))
                .setAnchorView(R.id.dbSheet)
                .show();
    }

    @Override
    public void showGames(List<DbDtos.GameHeader> games, int total, boolean hasMore) {
        // Games list arrives with its own task.
    }

    private void doSearch() {
        board.setRedDot(-1);
        progressBar.setVisibility(View.VISIBLE);
        String player1 = ((AutoCompleteTextView) settingsView.findViewById(R.id.player1)).getText().toString().toLowerCase(java.util.Locale.ROOT);
        String player2 = ((AutoCompleteTextView) settingsView.findViewById(R.id.player2)).getText().toString().toLowerCase(java.util.Locale.ROOT);
        int winner = 0;
        if (((TextView) settingsView.findViewById(R.id.winner)).getText().equals(getString(R.string.player1))) {
            winner = 1;
        } else if (((TextView) settingsView.findViewById(R.id.winner)).getText().equals(getString(R.string.player2))) {
            winner = 2;
        }
        String afterDate = ((TextView) settingsView.findViewById(R.id.afterDate)).getText().toString();
        if (!"".equals(afterDate)) {
            afterDate = "&after_date=" + afterDate;
        }
        String beforeDate = ((TextView) settingsView.findViewById(R.id.beforeDate)).getText().toString();
        if (!"".equals(beforeDate)) {
            beforeDate = "&before_date=" + beforeDate;
        }
        String str = ((TextView) (settingsView.findViewById(R.id.eitherOrBoth))).getText().toString();
        String eitherOrBoth = "";
        if (getString(R.string.either).equals(str)) {
            eitherOrBoth = "&p1_or_p2=true";
        }
        str = ((TextView) (settingsView.findViewById(R.id.excludeTimeouts))).getText().toString();
        String excludeTimeouts = "";
        if (getString(R.string.yes).equals(str)) {
            excludeTimeouts = "&exclude_timeout=true";
        }
        str = ((TextView) (settingsView.findViewById(R.id.liveOrTB))).getText().toString();
        String liveOrTB = be.submanifold.pentelive.db.LegacyDbQuery.liveOrTbParam(str,
                getString(R.string.live_only), getString(R.string.turn_based_only));
        SearchTask searchTask = new SearchTask(board.getMovesString(),
                board.getGame(),
                (PrefUtils.getFromPrefs(DatabaseActivity.this, PrefUtils.PREFS_DBSORT_KEY, "popularity").equals("popularity") ? 1 : 2),
                player1,
                player2,
                winner,
                afterDate,
                beforeDate,
                dbP1Rating,
                dbP2Rating,
                eitherOrBoth,
                excludeTimeouts,
                liveOrTB);
        searchTask.execute((Void) null);
    }

    // database_game_types_array gained Swap2-Pente/Swap2-Keryo after O-Pente, which
    // pushed every "Speed ..." entry down by two. The spinner selection is persisted as
    // a raw index, so shift a previously stored Speed index once to keep it pointing at
    // the same game.
    private void migrateDbGameIndexForSwap2() {
        if (PrefUtils.getBooleanFromPrefs(DatabaseActivity.this, PrefUtils.PREFS_DBGAME_SWAP2SHIFT_KEY, false)) {
            return;
        }
        int stored = PrefUtils.getIntFromPrefs(DatabaseActivity.this, PrefUtils.PREFS_DBGAME_KEY, 0);
        int count = getResources().getStringArray(R.array.database_game_types_array).length;
        if (stored >= 10 && stored <= 19) {
            int shifted = Math.min(stored + 2, count - 1);
            PrefUtils.saveIntToPrefs(DatabaseActivity.this, PrefUtils.PREFS_DBGAME_KEY, shifted);
        }
        PrefUtils.saveBooleanToPrefs(DatabaseActivity.this, PrefUtils.PREFS_DBGAME_SWAP2SHIFT_KEY, true);
    }

    private void showDBSettings() {

//            settingsWindow.showAtLocation(board, Gravity.TOP, 0, 260);
        searchPrmtrsWindow.show();
        int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.95);
        int height = (int) (getResources().getDisplayMetrics().heightPixels * 0.70);
        settingsView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        if (settingsView.getMeasuredHeight() > height) {
            searchPrmtrsWindow.getWindow().setLayout(width, height);
        }


        Spinner spinner = settingsView.findViewById(R.id.gameSpinner);
        migrateDbGameIndexForSwap2();
        int gameIdx = PrefUtils.getIntFromPrefs(DatabaseActivity.this, PrefUtils.PREFS_DBGAME_KEY, 0);
        String[] gameTypes = getResources().getStringArray(R.array.database_game_types_array);
        for (int i = 0; i < gameTypes.length; i++) {
            if (gameTypes[i].equals(dbModel.gameLabel())) {
                gameIdx = i; // show the game on screen (an extras game is not the saved one)
                break;
            }
        }
        spinner.setSelection(gameIdx);
        spinner = settingsView.findViewById(R.id.p1RatingSpinner);
        int position = PrefUtils.getIntFromPrefs(DatabaseActivity.this, PrefUtils.PREFS_DBP1RATING_KEY, 0);
        spinner.setSelection(position);
        String[] p1RatingArray = getResources().getStringArray(R.array.db_ratings_array);
        dbP1Rating = p1RatingArray[position];
        spinner = settingsView.findViewById(R.id.p2RatingSpinner);
        position = PrefUtils.getIntFromPrefs(DatabaseActivity.this, PrefUtils.PREFS_DBP2RATING_KEY, 0);
        spinner.setSelection(position);
        String[] p2RatingArray = getResources().getStringArray(R.array.db_ratings_array);
        dbP2Rating = p2RatingArray[position];
        TextView sortOrder = settingsView.findViewById(R.id.sortChoice);
        sortOrder.setText(PrefUtils.getFromPrefs(DatabaseActivity.this, PrefUtils.PREFS_DBSORT_KEY, "popularity"));
        AutoCompleteTextView actv = settingsView.findViewById(R.id.player1);
        ArrayAdapter<String> acAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, new ArrayList<String>(PrefUtils.getPlayers(DatabaseActivity.this)));
        actv.setAdapter(acAdapter);
        actv = settingsView.findViewById(R.id.player2);
        acAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<String>(PrefUtils.getPlayers(DatabaseActivity.this)));
        actv.setAdapter(acAdapter);
        searchPrmtrsWindow.setOnDismissListener(dialogInterface -> board.setAlpha(1.0f));
        board.setAlpha(0.05f);
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
        MenuItem menuItem = menu.findItem(R.id.action_db_settings);
        menuItem.setIconTintList(tintList);
        menu.findItem(R.id.action_db_heat).setChecked(PrefUtils.getBooleanFromPrefs(this, PrefUtils.PREFS_DBHEAT_KEY, true));

        return true;
    }


    public class SearchTask extends BackgroundTask<Void, Boolean> {

        private final String moves;
        private final String game;
        private final String player1;
        private final String player2;
        private final String afterDate;
        private final String beforeDate;
        private final String p1Rating;
        private final String p2Rating;
        private final String eitherOrBoth;
        private final String excludeTimeouts;
        private final String liveOrTB;
        private final int sortOrder;
        private final int winner;
        private String searchResult;

        SearchTask(String moves, String game, int sortOrder, String player1, String player2, int winner,
                   String afterDate, String beforeDate, String p1Rating, String p2Rating, String eitherOrBoth,
                   String excludeTimeouts, String liveOrTB) {
            this.moves = moves;
            this.game = game;
            this.sortOrder = sortOrder;
            this.player1 = player1.toLowerCase(java.util.Locale.ROOT);
            this.player2 = player2.toLowerCase(java.util.Locale.ROOT);
            this.winner = winner;
            this.afterDate = afterDate;
            this.beforeDate = beforeDate;
            this.p1Rating = p1Rating;
            this.p2Rating = p2Rating;
            this.eitherOrBoth = eitherOrBoth;
            this.excludeTimeouts = excludeTimeouts;
            this.liveOrTB = liveOrTB;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            try {
                String tmpStr, getStr = "moves=";
                try {
                    tmpStr = URLEncoder.encode(moves, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    tmpStr = "";
                }
//                System.out.println("moves: " + tmpStr);
                getStr = getStr + tmpStr + "&response_format=org.pente.gameDatabase.SimpleHtmlGameStorerSearchResponseFormat&response_params=";
                try {
                    tmpStr = URLEncoder.encode("zippedPartNumParam=1", "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    tmpStr = "";
                }
//                System.out.println("getstr: " + tmpStr);
                getStr = getStr + tmpStr + "&results_order=" + sortOrder + "&filter_data=";
                try {
                    tmpStr = URLEncoder.encode("start_game_num=0&end_game_num=100&player_1_name=" + player1 +
                            "&player_2_name=" + player2 + "&game=" + game +
                            "&site=All%20Sites&event=All%20Events&round=All%20Rounds&section=All%20Sections&winner=" +
                            winner + afterDate + beforeDate + "&p1_rating_above=" + p1Rating + "&p2_rating_above=" + p2Rating
                            + eitherOrBoth + excludeTimeouts + liveOrTB + "&android=yes", "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    tmpStr = "";
                }
//                System.out.println("filter data: " + tmpStr);
                getStr = getStr + tmpStr;
                try {
                    tmpStr = URLEncoder.encode(getStr, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    tmpStr = "";
                }
//                System.out.println("format data: " + tmpStr);
                URL url = new URL("https://www.pente.org/gameServer/mobileController/search?json=&format_name=org.pente.gameDatabase.SimpleGameStorerSearchRequestFormat&format_data=" + tmpStr);
                if (PentePlayer.development) {
                    url = new URL("https://10.0.2.2/gameServer/mobileController/search?json=&format_name=org.pente.gameDatabase.SimpleGameStorerSearchRequestFormat&format_data=" + tmpStr);
                }
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(200000);
                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    System.out.println("response code for submit was " + responseCode);
                    return false;
                }

                StringBuilder output = new StringBuilder();
                BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
//                System.out.println("output===============" + br);
                String line = "";
                while ((line = br.readLine()) != null) {
                    output.append(line + "\n");
                }
                br.close();

                searchResult = output.toString();
//                System.out.println("search output: " + searchResult);

            } catch (IOException e1) {
                e1.printStackTrace();
                return false;
            }

            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            if (!success || searchResult == null) {
                board.setTextViewHTML(findViewById(R.id.playerInfo), "<br><br>" + getString(R.string.no_search_results));
                progressBar.setVisibility(View.GONE);
                return;
            }
            try {
                JSONObject json = new JSONObject(searchResult);

                if (!json.getBoolean("access") || json.getBoolean("blocked")) {
                    board.setTextViewHTML(findViewById(R.id.playerInfo), "<br><br>" + getString(R.string.no_search_results));
                    progressBar.setVisibility(View.GONE);
                    return;
                }

                // Heat dots now come from /api/db position-stats (DbController); this task lists legacy game links only.
                JSONArray games = json.getJSONArray("games");
                if (games.length() == 0) {
                    board.setTextViewHTML(findViewById(R.id.playerInfo), "<br><br>" + getString(R.string.no_search_results));
                } else {
                    if (!player1.isEmpty()) {
                        for (String name : player1.replace(" ", "").split(",")) {
                            if (!name.contains("*"))
                                PrefUtils.savePlayerToPrefs(DatabaseActivity.this, name);
                        }
                    }
                    if (!player2.isEmpty()) {
                        for (String name : player2.replace(" ", "").split(",")) {
                            if (!name.contains("*"))
                                PrefUtils.savePlayerToPrefs(DatabaseActivity.this, name);
                        }
                    }
                    StringBuilder html = new StringBuilder();
                    for (int i = 0; i < games.length(); i++) {
                        JSONObject game = games.getJSONObject(i);
                        JSONObject p1 = game.getJSONObject("player1");
                        JSONObject p2 = game.getJSONObject("player2");
                        String viewUrl = game.getString("viewUrl");
                        String date = game.getString("date");
                        String p1Name = p1.getString("name");
                        String p2Name = p2.getString("name");
                        String winner = p1.getBoolean("winner") ? p1Name : p2Name;
                        html.append("<br><br><a href=\"https://www.pente.org").append(viewUrl).append("\">")
                                .append(p1Name).append(" vs ").append(p2Name).append("</a>")
                                .append(" &bull; ").append(date)
                                .append(" &bull; W: ").append(winner);
                    }
                    board.setTextViewHTML(findViewById(R.id.playerInfo), html.toString());
                }
            } catch (Exception e) {
                board.setTextViewHTML(findViewById(R.id.playerInfo), "<br><br>" + getString(R.string.no_search_results));
            }
            progressBar.setVisibility(View.GONE);
        }

        @Override
        protected void onCancelled() {
        }
    }


}
