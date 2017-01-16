package be.submanifold.pentelive;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;

import java.io.InputStream;


public class MMAIActivity extends AppCompatActivity {

    private MMAIBoardView board;
    private AdView mAdView;
    private PopupWindow settingsWindow;
    private View settingsView;
    public Animation rotation;
    public ImageView messageIcon;

    private ProgressBar progressBar;
    InterstitialAd mInterstitialAd;

//    private int untilMove;

    private Game game;
    private char coordinateLetters[] = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T'};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mmai);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setSubtitle("\u2B24 x 0 - \u25EF x 0");
        ((TextView) findViewById(R.id.capturesView)).setText("\u2B24 x 0\n\u25EF x 0");
        settingsView = ((LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.mmai_settings, null, false);

        progressBar = (ProgressBar)findViewById(R.id.progressBar);

        board = (MMAIBoardView) findViewById(R.id.boardView);
        board.setActivity(this);
        if (PrefUtils.getFromPrefs(MMAIActivity.this, PrefUtils.PREFS_MMAIGAME_KEY, "Pente").equals("Pente")) {
            board.setBackgroundColor(board.penteColor);
            board.setGame(1);
        } else {
            board.setBackgroundColor(board.keryoPenteColor);
            board.setGame(3);
        }
        if (PrefUtils.getFromPrefs(MMAIActivity.this, PrefUtils.PREFS_MMAICOLOR_KEY, "white").equals("white")) {
            board.setMyColor((byte) 1);
        } else {
            board.setMyColor((byte) 2);
        }
        board.setDifficulty(PrefUtils.getIntFromPrefs(MMAIActivity.this, PrefUtils.PREFS_MMAIDIFFICULTY_KEY, 0) + 1);

        toolbar.setTitle("Mark Mammel's AI");
        setSupportActionBar(toolbar);

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        settingsWindow = new PopupWindow(settingsView, size.x*2/3, ViewGroup.LayoutParams.WRAP_CONTENT, true );
        settingsWindow.setFocusable(true);
        settingsWindow.setOutsideTouchable(true);
        settingsWindow.setBackgroundDrawable(ContextCompat.getDrawable(MMAIActivity.this, R.drawable.border));
//                        messageWindow.setAnimationStyle(R.anim.animation);

        Spinner spinner = (Spinner) settingsView.findViewById(R.id.difficultySpinner);
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
        TextView playAs = (TextView) settingsView.findViewById(R.id.playAsChoice);
        playAs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView tv = (TextView) v;
                if (tv.getText().equals(getString(R.string.black))) {
                    tv.setText(getString(R.string.white));
                    PrefUtils.saveToPrefs(MMAIActivity.this,PrefUtils.PREFS_MMAICOLOR_KEY, "white");
                    board.setMyColor((byte) 1);
                } else {
                    tv.setText(getString(R.string.black));
                    PrefUtils.saveToPrefs(MMAIActivity.this,PrefUtils.PREFS_MMAICOLOR_KEY, "black");
                    board.setMyColor((byte) 2);
                }
            }
        });
        TextView gameChoice = (TextView) settingsView.findViewById(R.id.gameChoice);
        gameChoice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView tv = (TextView) v;
                if (tv.getText().equals("Pente")) {
                    tv.setText("Keryo-Pente");
                    PrefUtils.saveToPrefs(MMAIActivity.this,PrefUtils.PREFS_MMAIGAME_KEY, "Keryo-Pente");
                    board.setBackgroundColor(board.keryoPenteColor);
                    board.setGame(3);
                } else {
                    tv.setText("Pente");
                    PrefUtils.saveToPrefs(MMAIActivity.this,PrefUtils.PREFS_MMAIGAME_KEY, "Pente");
                    board.setBackgroundColor(board.penteColor);
                    board.setGame(1);
                }
            }
        });
        settingsWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                board.setAlpha(1.0f);
            }
        });


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
            nativeComputer.init(scs, opnbk, tbl);
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



//        final BoardView layout = (BoardView) findViewById(R.id.boardView);
        ViewTreeObserver vto = board.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
//                board.getViewTreeObserver().removeGlobalOnLayoutListener(this);
//                int width  = board.getMeasuredWidth();
//                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) board.getLayoutParams();
//                params.height = width;
//                params.width = width;
////                System.out.println("kitteh " + params.width + " and " + params.height + " and " + width);
//                board.setLayoutParams(params);

                showAISettings();
            }
        });

        if (PentePlayer.mShowAds) {
            ((AdView) findViewById(R.id.boardAdView)).loadAd(new AdRequest.Builder().build());
        } else {
            ((AdView) findViewById(R.id.boardAdView)).setVisibility(View.GONE);
        }

        Button button = (Button) findViewById(R.id.startButton);
        if (button != null) button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ((Button) v).setText(getString(R.string.restart_game));
                board.setDifficulty(PrefUtils.getIntFromPrefs(MMAIActivity.this, PrefUtils.PREFS_MMAIDIFFICULTY_KEY, 0) + 1);
                board.setMyColor((byte) ("white".equals(PrefUtils.getFromPrefs(MMAIActivity.this, PrefUtils.PREFS_MMAICOLOR_KEY, "white"))?1:2));
                board.startGame();
            }
        });
        button = (Button) findViewById(R.id.backButton);
        if (button != null) button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                board.undoMove();
            }
        });

        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch (menuItem.getItemId()){
                    case R.id.action_mmai_settings:
                        showAISettings();

                        return true;
                }

                return false;
            }
        });
        if (PentePlayer.mShowAds) {
            mInterstitialAd = new InterstitialAd(this);
            mInterstitialAd.setAdUnitId("ca-app-pub-3326997956703582/9477825047");

            mInterstitialAd.setAdListener(new AdListener() {
                //                @Override
//                public void onAdClosed() {
//                    requestNewInterstitial();
//                }
                @Override
                public void onAdLoaded() {
                    mInterstitialAd.show();
                }
            });
            requestNewInterstitial();
        }

    }

    private void showAISettings() {
        settingsWindow.showAtLocation(board, Gravity.TOP, 0, 260);
        Spinner spinner = (Spinner) settingsView.findViewById(R.id.difficultySpinner);
        spinner.setSelection(PrefUtils.getIntFromPrefs(MMAIActivity.this, PrefUtils.PREFS_MMAIDIFFICULTY_KEY, 0));
        TextView playAs = (TextView) settingsView.findViewById(R.id.playAsChoice);
        if (PrefUtils.getFromPrefs(MMAIActivity.this, PrefUtils.PREFS_MMAICOLOR_KEY, "white").equals("white")) {
            playAs.setText(getString(R.string.white));
        } else {
            playAs.setText(getString(R.string.black));
        }
        TextView game = (TextView) settingsView.findViewById(R.id.gameChoice);
        game.setText(PrefUtils.getFromPrefs(MMAIActivity.this, PrefUtils.PREFS_MMAIGAME_KEY, "Pente"));
        board.setAlpha(0.75f);
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
        getMenuInflater().inflate(R.menu.mmaiboard_menu, menu);

        return true;
    }

    public Game getGame() {
        return game;
    }
    public void setGame(Game game) {
        this.game = game;
    }

    private void requestNewInterstitial() {
        AdRequest adRequest = new AdRequest.Builder()
                .build();
        mInterstitialAd.loadAd(adRequest);
    }

}
