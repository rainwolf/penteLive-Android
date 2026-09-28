package be.submanifold.pentelive;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.InputStream;


public class MMAIActivity extends AppCompatActivity {

    // Local-AI variant chooser (§ mobile phase 2). Display label -> canonical
    // engine game ID, cycled by tapping R.id.gameChoice and persisted under
    // PREFS_MMAIGAME_KEY. Connect6 (game 13) is now offered: it plays TWO stones
    // per turn and the AI returns a packed base-362 move, both handled locally.
    // DONE(connect6-local): MMAIBoardView now (a) accepts two human placements per
    // turn (onTouchEvent gates on owner(size) == myColor), (b) decodes the packed AI
    // move (m1 = p/362, m2 = p%362, m2 == 361 = single-stone opening) in
    // processAImove, and (c) colours stones by the 2-per-turn owner rule via the
    // shared owner() helper (mirrored by VariantReferee.colorForMove).
    private static final String[] VARIANT_NAMES =
            {"Pente", "Keryo-Pente", "Poof-Pente", "Boat-Pente", "O-Pente", "Connect6"};
    private static final int[] VARIANT_GAMES = {1, 3, 11, 15, 25, 13};

    private int variantGameFor(String name) {
        for (int i = 0; i < VARIANT_NAMES.length; i++) {
            if (VARIANT_NAMES[i].equals(name)) {
                return VARIANT_GAMES[i];
            }
        }
        return 1; // unknown / legacy value -> plain Pente
    }

    private int variantBackgroundFor(int game) {
        switch (game) {
            case 3:  return board.keryoPenteColor;
            case 11: return board.poofColor;
            case 15: return board.boatColor;
            case 25: return board.openteColor;
            case 13: return board.connect6Color;
            default: return board.penteColor;
        }
    }

    private void applyVariant(String name) {
        int g = variantGameFor(name);
        board.setBackgroundColor(variantBackgroundFor(g));
        board.setGame(g);
    }

    private String nextVariantName(String cur) {
        for (int i = 0; i < VARIANT_NAMES.length; i++) {
            if (VARIANT_NAMES[i].equals(cur)) {
                return VARIANT_NAMES[(i + 1) % VARIANT_NAMES.length];
            }
        }
        return VARIANT_NAMES[0];
    }

    private MMAIBoardView board;
    private PopupWindow settingsWindow;
    private View settingsView;
    public Animation rotation;
    public ImageView messageIcon;

    private ProgressBar progressBar;

//    private int untilMove;

    private Game game;
    private final char[] coordinateLetters = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T'};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mmai);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setSubtitle("\u2B24 x 0 - \u25EF x 0");
        ((TextView) findViewById(R.id.capturesView)).setText("\u2B24 x 0\n\u25EF x 0");
        settingsView = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.mmai_settings, null, false);

        progressBar = findViewById(R.id.progressBar);

        board = findViewById(R.id.boardView);
        board.setActivity(this);
        applyVariant(PrefUtils.getFromPrefs(MMAIActivity.this, PrefUtils.PREFS_MMAIGAME_KEY, "Pente"));
        if (PrefUtils.getFromPrefs(MMAIActivity.this, PrefUtils.PREFS_MMAICOLOR_KEY, "white").equals("white")) {
            board.setMyColor((byte) 1);
        } else {
            board.setMyColor((byte) 2);
        }
        board.setDifficulty(PrefUtils.getIntFromPrefs(MMAIActivity.this, PrefUtils.PREFS_MMAIDIFFICULTY_KEY, 0) + 1);

        toolbar.setTitle("Mark Mammel's AI");
        setSupportActionBar(toolbar);

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        Point size = new Point(metrics.widthPixels, metrics.heightPixels);
        settingsWindow = new PopupWindow(settingsView, size.x * 2 / 3, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        settingsWindow.setFocusable(true);
        settingsWindow.setOutsideTouchable(true);
//        settingsWindow.setBackgroundDrawable(ContextCompat.getDrawable(MMAIActivity.this, R.drawable.border));
//                        messageWindow.setAnimationStyle(R.anim.animation);

        Spinner spinner = settingsView.findViewById(R.id.difficultySpinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(MMAIActivity.this,
                R.array.mmai_difficulty_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                PrefUtils.saveIntToPrefs(MMAIActivity.this, PrefUtils.PREFS_MMAIDIFFICULTY_KEY, position);
                board.setDifficulty(position + 1);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        TextView playAs = settingsView.findViewById(R.id.playAsChoice);
        playAs.setOnClickListener(v -> {
            TextView tv = (TextView) v;
            if (tv.getText().equals(getString(R.string.black))) {
                tv.setText(getString(R.string.white));
                PrefUtils.saveToPrefs(MMAIActivity.this, PrefUtils.PREFS_MMAICOLOR_KEY, "white");
                board.setMyColor((byte) 1);
            } else {
                tv.setText(getString(R.string.black));
                PrefUtils.saveToPrefs(MMAIActivity.this, PrefUtils.PREFS_MMAICOLOR_KEY, "black");
                board.setMyColor((byte) 2);
            }
        });
        TextView gameChoice = settingsView.findViewById(R.id.gameChoice);
        gameChoice.setOnClickListener(v -> {
            TextView tv = (TextView) v;
            String next = nextVariantName(tv.getText().toString());
            tv.setText(next);
            PrefUtils.saveToPrefs(MMAIActivity.this, PrefUtils.PREFS_MMAIGAME_KEY, next);
            applyVariant(next);
        });
        board.setAlpha(0.05f);
        settingsWindow.setOnDismissListener(() -> board.setAlpha(1.0f));


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
//            nativeComputer.setVisualization(false);

            board.setAiPlayer(nativeComputer);

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

        } catch (Throwable t) {
            Log.v("ai", "error init", t);
        }


////        final BoardView layout = (BoardView) findViewById(R.id.boardView);
//        ViewTreeObserver vto = board.getViewTreeObserver();
//        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
//            @Override
//            public void onGlobalLayout() {
////                board.getViewTreeObserver().removeGlobalOnLayoutListener(this);
////                int width  = board.getMeasuredWidth();
////                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) board.getLayoutParams();
////                params.height = width;
////                params.width = width;
//////                System.out.println("kitteh " + params.width + " and " + params.height + " and " + width);
////                board.setLayoutParams(params);
//
//            }
//        });

        Button button = findViewById(R.id.startButton);
        if (button != null) button.setOnClickListener(v -> {
            ((Button) v).setText(getString(R.string.restart_game));
            board.setDifficulty(PrefUtils.getIntFromPrefs(MMAIActivity.this, PrefUtils.PREFS_MMAIDIFFICULTY_KEY, 0) + 1);
            board.setMyColor((byte) ("white".equals(PrefUtils.getFromPrefs(MMAIActivity.this, PrefUtils.PREFS_MMAICOLOR_KEY, "white")) ? 1 : 2));
            board.startGame();
        });
        button = findViewById(R.id.backButton);
        if (button != null) button.setOnClickListener(v -> board.undoMove());

        toolbar.setOnMenuItemClickListener(menuItem -> {
            if (menuItem.getItemId() == R.id.action_mmai_settings) {
                showAISettings();

                return true;
            }

            return false;
        });
        board.post(() -> showAISettings());
    }

    private void showAISettings() {
        settingsWindow.showAtLocation(board, Gravity.TOP, 0, 400);
        Spinner spinner = settingsView.findViewById(R.id.difficultySpinner);
        spinner.setSelection(PrefUtils.getIntFromPrefs(MMAIActivity.this, PrefUtils.PREFS_MMAIDIFFICULTY_KEY, 0));
        TextView playAs = settingsView.findViewById(R.id.playAsChoice);
        if (PrefUtils.getFromPrefs(MMAIActivity.this, PrefUtils.PREFS_MMAICOLOR_KEY, "white").equals("white")) {
            playAs.setText(getString(R.string.white));
        } else {
            playAs.setText(getString(R.string.black));
        }
        TextView game = settingsView.findViewById(R.id.gameChoice);
        game.setText(PrefUtils.getFromPrefs(MMAIActivity.this, PrefUtils.PREFS_MMAIGAME_KEY, "Pente"));
        board.setAlpha(0.05f);
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

    public void showThinking() {
        progressBar.setVisibility(View.VISIBLE);
    }

    public void hideThinking() {
        progressBar.setVisibility(View.GONE);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        // Inflate the menu; this adds items to the action bar if it is present.
        ColorStateList tintList = Helpers.tintList(this);
        getMenuInflater().inflate(R.menu.mmaiboard_menu, menu);
        MenuItem menuItem = menu.findItem(R.id.action_mmai_settings);
        menuItem.setIconTintList(tintList);

        return true;
    }

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
    }

}
