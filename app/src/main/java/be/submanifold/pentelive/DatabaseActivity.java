package be.submanifold.pentelive;



import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

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
import java.util.HashMap;
import java.util.Map;

import static android.view.View.VISIBLE;


public class DatabaseActivity extends AppCompatActivity {

        private DBBoardView board;
        private AdView mAdView;
        private PopupWindow settingsWindow;
        private View settingsView, aiSettingsView;
        public Animation rotation;
        public ImageView messageIcon;

        private String dbRating = "0";
        private ProgressBar progressBar;
        private AlertDialog searchPrmtrsWindow, aiSearchPrmtrsWindow;
//        private Context ctx = MyApplication.getContext();
//    private int untilMove;

//        private Game game;
        private char coordinateLetters[] = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T'};

        private DatePickerDialog afterDatePickerDialog, beforeDatePickerDialog;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_database);
            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            toolbar.setSubtitle("\u2B24 x 0 - \u25EF x 0");
            ((TextView) findViewById(R.id.capturesView)).setText("\u2B24 x 0\n\u25EF x 0");
            settingsView = ((LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.database_options, null, false);
            AlertDialog.Builder helpBuilder = new AlertDialog.Builder(this);
            helpBuilder.setTitle(getString(R.string.search_parameters));
            helpBuilder.setView(settingsView);
            searchPrmtrsWindow = helpBuilder.create();
            searchPrmtrsWindow.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialogInterface) {
                    board.setAlpha(1.0f);
                    if (board.getMovesList().size() == 0 || (board.getMovesList().size()>0 && board.getMovesList().get(0) != 180)) {
                        if (!board.getGame().contains("D-Pente") && !board.getGame().contains("DK-Pente")) {
                            board.resetState();
                        }
                    }
                }
            });
            searchPrmtrsWindow.setCanceledOnTouchOutside(true);
            aiSettingsView = ((LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.db_ai_settings, null, false);
            helpBuilder = new AlertDialog.Builder(this);
            helpBuilder.setTitle(getString(R.string.ai_parameters));
            helpBuilder.setView(aiSettingsView);
            aiSearchPrmtrsWindow= helpBuilder.create();
            aiSearchPrmtrsWindow.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialogInterface) {
                    board.setAlpha(1.0f);
                }
            });
            aiSearchPrmtrsWindow.setCanceledOnTouchOutside(true);

            progressBar = (ProgressBar)findViewById(R.id.progressBar);

            board = (DBBoardView) findViewById(R.id.boardView);
            board.setActivity(this);

            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                String gameStr = extras.getString("game");
                if (gameStr.equals("Pente") || gameStr.equals("Speed Pente") || gameStr.contains("Keryo-Pente")) {
                    ((Button) findViewById(R.id.aiButton)).setVisibility(VISIBLE);
                }
                board.setGame(gameStr);
                board.setMovesList(extras.getIntegerArrayList("moves"));
            }

            if (PentePlayer.mShowAds) {
                ((AdView) findViewById(R.id.dbAdView)).loadAd(new AdRequest.Builder().build());
            } else {
                ((AdView) findViewById(R.id.dbAdView)).setVisibility(View.GONE);
            }


            toolbar.setTitle(getString(R.string.database));
            setSupportActionBar(toolbar);

//            Display display = getWindowManager().getDefaultDisplay();
//            Point size = new Point();
//            display.getSize(size);
//            settingsWindow = new PopupWindow(settingsView, size.x*2/3, ViewGroup.LayoutParams.WRAP_CONTENT, true );
//            settingsWindow.setFocusable(true);
//            settingsWindow.setOutsideTouchable(true);
//            settingsWindow.setBackgroundDrawable(ContextCompat.getDrawable(DatabaseActivity.this, R.drawable.border));
////                        messageWindow.setAnimationStyle(R.anim.animation);

            Spinner spinner = (Spinner) settingsView.findViewById(R.id.gameSpinner);
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(DatabaseActivity.this,
                    R.array.database_game_types_array, android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    PrefUtils.saveIntToPrefs(DatabaseActivity.this, PrefUtils.PREFS_DBGAME_KEY, position);
                    String game = (String) parent.getItemAtPosition(position);
                    board.setGame(game);
                    if (game.contains("Gomoku") || game.contains("Connect6")) {
                            ((Toolbar) findViewById(R.id.toolbar)).setSubtitle("");
                            ((TextView) findViewById(R.id.capturesView)).setVisibility(View.INVISIBLE);
                    } else {
                            ((Toolbar) findViewById(R.id.toolbar)).setSubtitle("\u2B24 x 0 - \u25EF x 0");
                            ((TextView) findViewById(R.id.capturesView)).setVisibility(VISIBLE);
                    }
                    if (game.equals("Pente") || game.equals("Speed Pente") || game.contains("Keryo-Pente")) {
                        ((Button) findViewById(R.id.aiButton)).setVisibility(VISIBLE);
                    } else {
                        ((Button) findViewById(R.id.aiButton)).setVisibility(View.GONE);
                    }
                    board.resetState();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
            spinner.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    InputMethodManager imm =  (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    return false;
                }
            });
            TextView sortChoice = (TextView) settingsView.findViewById(R.id.sortChoice);
            sortChoice.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    InputMethodManager imm =  (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    TextView tv = (TextView) v;
                    if (tv.getText().equals(getString(R.string.popularity))) {
                        tv.setText(getString(R.string.win_percantage));
                        PrefUtils.saveToPrefs(DatabaseActivity.this,PrefUtils.PREFS_DBSORT_KEY, "win percentage");
                    } else {
                        tv.setText(getString(R.string.popularity));
                        PrefUtils.saveToPrefs(DatabaseActivity.this,PrefUtils.PREFS_DBSORT_KEY, "popularity");
                    }
                }
            });
            TextView winner = (TextView) settingsView.findViewById(R.id.winner);
            winner.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    InputMethodManager imm =  (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    TextView tv = (TextView) v;
                    if (tv.getText().equals(getString(R.string.either))) {
                        tv.setText(getString(R.string.player1));
                    } else if (tv.getText().equals(getString(R.string.player1))) {
                        tv.setText(getString(R.string.player2));
                    } else {
                        tv.setText(getString(R.string.either));
                    }
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



            Button button = (Button) findViewById(R.id.searchButton);
            if (button != null) button.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    doSearch();
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
                        case R.id.action_db_settings:
                            showDBSettings();

                            return true;
                    }

                    return false;
                }
            });
            ((AutoCompleteTextView) settingsView.findViewById(R.id.player1)).setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
//                    System.out.println("kitty");
                    if (!hasFocus) {
                        InputMethodManager imm =  (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    }
                }
            });
            ((AutoCompleteTextView) settingsView.findViewById(R.id.player2)).setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (!hasFocus) {
                        InputMethodManager imm =  (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    }
                }
            });

            Calendar newCalendar = Calendar.getInstance();
            afterDatePickerDialog = new DatePickerDialog(this, DatePickerDialog.THEME_HOLO_LIGHT, new DatePickerDialog.OnDateSetListener() {

                public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                    Calendar newDate = Calendar.getInstance();
                    newDate.set(year, monthOfYear, dayOfMonth);
                    DateFormat dateFormatter = new SimpleDateFormat("MM/dd/yyyy");
                    ((TextView) settingsView.findViewById(R.id.afterDate)).setText(dateFormatter.format(newDate.getTime()));
                }

            },newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH));
            afterDatePickerDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.clear), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    if (which == DialogInterface.BUTTON_NEGATIVE) {
                        // Do Stuff
                        ((TextView) settingsView.findViewById(R.id.afterDate)).setText("");
                    }
                }
            });
            ((TextView) settingsView.findViewById(R.id.afterDate)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    afterDatePickerDialog.show();
                }
            });
            beforeDatePickerDialog = new DatePickerDialog(this, DatePickerDialog.THEME_HOLO_LIGHT, new DatePickerDialog.OnDateSetListener() {

                public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                    Calendar newDate = Calendar.getInstance();
                    newDate.set(year, monthOfYear, dayOfMonth);
                    DateFormat dateFormatter = new SimpleDateFormat("MM/dd/yyyy");
                    ((TextView) settingsView.findViewById(R.id.beforeDate)).setText(dateFormatter.format(newDate.getTime()));
                }

            },newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH));
            beforeDatePickerDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.clear), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    if (which == DialogInterface.BUTTON_NEGATIVE) {
                        // Do Stuff
                        ((TextView) settingsView.findViewById(R.id.beforeDate)).setText("");
                    }
                }
            });
            ((TextView) settingsView.findViewById(R.id.beforeDate)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    beforeDatePickerDialog.show();
                }
            });
            spinner = (Spinner) settingsView.findViewById(R.id.ratingSpinner);
            adapter = ArrayAdapter.createFromResource(DatabaseActivity.this,
                    R.array.db_ratings_array, android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    PrefUtils.saveIntToPrefs(DatabaseActivity.this, PrefUtils.PREFS_DBRATING_KEY, position);
                    String[] ratingArray = getResources().getStringArray(R.array.db_ratings_array);
                    dbRating = ratingArray[position];
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
            spinner.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    InputMethodManager imm =  (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    return false;
                }
            });



            ((Button) findViewById(R.id.aiButton)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showAISettings();
                }
            });
            spinner = (Spinner) aiSettingsView.findViewById(R.id.difficultySpinner);
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
            ((ToggleButton) aiSettingsView.findViewById(R.id.openingBookToggleButton)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    PrefUtils.saveBooleanToPrefs(DatabaseActivity.this, PrefUtils.PREFS_DBAIOPENINGBOOK_KEY, b);
                }
            });
            ((Button) aiSettingsView.findViewById(R.id.runAIbutton)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    board.setSearchResults(null);
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
                            nativeComputer.init(scs, opnbk, tbl);
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
                }
            });

            board.post(new Runnable() {
                public void run() {
                    if (board.getMovesList().size() <= 1) {
                        showDBSettings();
                    } else {
                        doSearch();
                    }
                }
            });

        }

        private void doSearch() {
            board.setRedDot(-1);
            progressBar.setVisibility(View.VISIBLE);
            String player1 = ((AutoCompleteTextView) settingsView.findViewById(R.id.player1)).getText().toString().toLowerCase();
            String player2 = ((AutoCompleteTextView) settingsView.findViewById(R.id.player2)).getText().toString().toLowerCase();
            int winner = 0;
            if (((TextView) settingsView.findViewById(R.id.winner)).getText().equals(getString(R.string.player1))) {
                winner = 1;
            } else if (((TextView) settingsView.findViewById(R.id.winner)).getText().equals(getString(R.string.player2))) {
                winner = 2;
            }
            String afterDate = ((TextView) settingsView.findViewById(R.id.afterDate)).getText().toString();
            if (!"".equals(afterDate)) {
                afterDate = "&after_date="+afterDate;
            }
            String beforeDate = ((TextView) settingsView.findViewById(R.id.beforeDate)).getText().toString();
            if (!"".equals(beforeDate)) {
                beforeDate = "&before_date="+beforeDate;
            }
            SearchTask searchTask = new SearchTask(board.getMovesString(),
                    board.getGame(),
                    (PrefUtils.getFromPrefs(DatabaseActivity.this, PrefUtils.PREFS_DBSORT_KEY, "popularity").equals("popularity")?1:2),
                    player1,
                    player2,
                    winner,
                    afterDate,
                    beforeDate,
                    dbRating);
            searchTask.execute((Void) null);
        }
        private void showDBSettings() {

//            settingsWindow.showAtLocation(board, Gravity.TOP, 0, 260);
            searchPrmtrsWindow.show();


            Spinner spinner = (Spinner) settingsView.findViewById(R.id.gameSpinner);
            spinner.setSelection(PrefUtils.getIntFromPrefs(DatabaseActivity.this, PrefUtils.PREFS_DBGAME_KEY, 0));
            spinner = (Spinner) settingsView.findViewById(R.id.ratingSpinner);
            int position = PrefUtils.getIntFromPrefs(DatabaseActivity.this, PrefUtils.PREFS_DBRATING_KEY, 0);
            spinner.setSelection(position);
            String[] ratingArray = getResources().getStringArray(R.array.db_ratings_array);
            dbRating = ratingArray[position];
            TextView sortOrder = (TextView) settingsView.findViewById(R.id.sortChoice);
            sortOrder.setText(PrefUtils.getFromPrefs(DatabaseActivity.this, PrefUtils.PREFS_DBSORT_KEY, "popularity"));
            AutoCompleteTextView actv = (AutoCompleteTextView) settingsView.findViewById(R.id.player1);
            ArrayAdapter<String> acAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1, new ArrayList<String>(PrefUtils.getPlayers(DatabaseActivity.this)));
            actv.setAdapter(acAdapter);
            actv = (AutoCompleteTextView) settingsView.findViewById(R.id.player2);
            acAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1, new ArrayList<String>(PrefUtils.getPlayers(DatabaseActivity.this)));
            actv.setAdapter(acAdapter);
//            board.setAlpha(0.75f);
        }

        private void showAISettings() {
            aiSearchPrmtrsWindow.show();
            Spinner spinner = (Spinner) aiSettingsView.findViewById(R.id.difficultySpinner);
            spinner.setSelection(PrefUtils.getIntFromPrefs(DatabaseActivity.this, PrefUtils.PREFS_DBAIDIFFICULTY_KEY, 0));
            ((ToggleButton) aiSettingsView.findViewById(R.id.openingBookToggleButton)).setChecked(PrefUtils.getBooleanFromPrefs(DatabaseActivity.this, PrefUtils.PREFS_DBAIOPENINGBOOK_KEY, false));
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

            return true;
        }

//        public Game getGame() {
//            return game;
//        }
//        public void setGame(Game game) {
//            this.game = game;
//        }


    public class SearchTask extends AsyncTask<Void, Void, Boolean> {

        private String moves, game, player1, player2, afterDate, beforeDate, aboveRating;
        private int sortOrder, winner;
        private String searchResult;

        SearchTask(String moves, String game, int sortOrder, String player1, String player2, int winner, String afterDate, String beforeDate, String aboveRating) {
            this.moves = moves;
            this.game = game;
            this.sortOrder = sortOrder;
            this.player1 = player1.toLowerCase();
            this.player2 = player2.toLowerCase();
            this.winner = winner;
            this.afterDate = afterDate;
            this.beforeDate = beforeDate;
            this.aboveRating = aboveRating;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            try {
                String tmpStr, getStr = "moves=";
                try {
                    tmpStr = URLEncoder.encode(moves,"UTF-8");
                } catch (UnsupportedEncodingException e) {
                    tmpStr = "";
                }
//                System.out.println("moves: " + tmpStr);
                getStr = getStr + tmpStr + "&response_format=org.pente.gameDatabase.SimpleHtmlGameStorerSearchResponseFormat&response_params=";
                try {
                    tmpStr = URLEncoder.encode("zippedPartNumParam=1","UTF-8");
                } catch (UnsupportedEncodingException e) {
                    tmpStr = "";
                }
//                System.out.println("getstr: " + tmpStr);
                getStr = getStr + tmpStr + "&results_order=" + sortOrder + "&filter_data=";
                try {
                    tmpStr = URLEncoder.encode("start_game_num=0&end_game_num=100&player_1_name="+player1+
                            "&player_2_name="+player2+"&game=" + game +
                            "&site=All%20Sites&event=All%20Events&round=All%20Rounds&section=All%20Sections&winner=" +
                            winner+afterDate+beforeDate+"&rating_above="+aboveRating,"UTF-8");
                } catch (UnsupportedEncodingException e) {
                    tmpStr = "";
                }
//                System.out.println("filter data: " + tmpStr);
                getStr = getStr + tmpStr;
                try {
                    tmpStr = URLEncoder.encode(getStr,"UTF-8");
                } catch (UnsupportedEncodingException e) {
                    tmpStr = "";
                }
//                System.out.println("format data: " + tmpStr);
                URL url = new URL("https://www.pente.org/gameServer/mobileController/search?format_name=org.pente.gameDatabase.SimpleGameStorerSearchRequestFormat&format_data=" + tmpStr);
                if (PentePlayer.development) {
                    url = new URL("https://development.pente.org/gameServer/mobileController/search?format_name=org.pente.gameDatabase.SimpleGameStorerSearchRequestFormat&format_data=" + tmpStr);
                }
                HttpURLConnection connection = (HttpURLConnection)url.openConnection();
                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    System.out.println("response code for submit was " + responseCode);
                    return false;
                }

                StringBuilder output = new StringBuilder();
                BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
//                System.out.println("output===============" + br);
                String line = "";
                while((line = br.readLine()) != null ) {
                    output.append(line + "\n");
                }
                br.close();

//                System.out.println("search output: " + output.toString());
                searchResult = output.toString();

            } catch (IOException e1) {
                e1.printStackTrace();
                return  false;
            }

            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            String[] dashLines = searchResult.split("\n");
            String dashLine;
            String[] moves = null, occurrences = null;
            int idx = 0;
            while (idx < dashLines.length) {
                dashLine = dashLines[idx];
                if (dashLine.indexOf("moves=") == 0) {
                    if (dashLine.length() > 6) {
                        moves = dashLine.substring(6).split(",");
                    }
                }
                if (dashLine.indexOf("occurrence=") == 0) {
                    if (dashLine.length() > 11) {
                        occurrences = dashLine.substring(11).split(";");
                    }
                    break;
                }
                idx += 1;
            }
            Map<Integer, Integer> searchResults = new HashMap<>();
            double max = 0, min = Double.MAX_VALUE;
            if (occurrences != null && moves != null) {
                for (int i = 0; i < occurrences.length; i++ ) {
                    double dbl = Double.parseDouble(occurrences[i]);
                    if (dbl > max) {
                        max = dbl;
                    }
                    if (dbl < min) {
                        min = dbl;
                    }
                }
            }
            if (max == min) {
                max = 100.0;
                min = 0.0;
            }
            if (moves != null) {
                for (int i = 0; i < moves.length; i++ ) {
                    double dblValue = (Double.parseDouble(occurrences[i])-min)/(max-min);
                    int color = 0;
                    if (dblValue <= 0.5) {
                        color = Color.rgb(255, (int)(dblValue*511), 0);
                    } else {
                        color = Color.rgb((int) ((1.0-dblValue)*511), 255, 0);
                    }
                    searchResults.put(Integer.parseInt(moves[i]), color);
                }
            }
//            System.out.println(searchResults);
            board.setSearchResults(searchResults);
            board.invalidate();
            if (searchResults.size() == 0 && !searchResult.contains("https://www.pente.org/gameServer/viewLiveGame?mobile&g=")) {
                board.setTextViewHTML(((TextView) findViewById(R.id.playerInfo)), "<br><br>" + getString(R.string.no_search_results));
            } else {
                if (player1.length() > 0) {
                    PrefUtils.savePlayerToPrefs(DatabaseActivity.this, player1);
                }
                if (player2.length() > 0) {
                    PrefUtils.savePlayerToPrefs(DatabaseActivity.this, player2);
                }
                board.setTextViewHTML(((TextView) findViewById(R.id.playerInfo)), searchResult.replace("<tr bgcolor=\"#deecde\"><td>", "<tr bgcolor=\"#deecde\"><td><br><br>"));
            }
            progressBar.setVisibility(View.GONE);
        }

        @Override
        protected void onCancelled() {
        }
    }


}
