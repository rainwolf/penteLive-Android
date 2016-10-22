package be.submanifold.pentelive;



import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
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

import com.google.android.gms.ads.AdView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import static android.view.View.VISIBLE;


public class DatabaseActivity extends AppCompatActivity {

        private DBBoardView board;
        private AdView mAdView;
        private PopupWindow settingsWindow;
        private View settingsView;
        public Animation rotation;
        public ImageView messageIcon;

        private ProgressBar progressBar;

//    private int untilMove;

//        private Game game;
        private char coordinateLetters[] = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T'};


        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_database);
            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            toolbar.setSubtitle("\u2B24 x 0 - \u25EF x 0");
            ((TextView) findViewById(R.id.capturesView)).setText("\u2B24 x 0\n\u25EF x 0");
            settingsView = ((LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.database_options, null, false);

            progressBar = (ProgressBar)findViewById(R.id.progressBar);

            board = (DBBoardView) findViewById(R.id.boardView);
            board.setActivity(this);

            toolbar.setTitle("Database");
            setSupportActionBar(toolbar);

            Display display = getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            settingsWindow = new PopupWindow(settingsView, size.x*2/3, ViewGroup.LayoutParams.WRAP_CONTENT, true );
            settingsWindow.setFocusable(true);
            settingsWindow.setOutsideTouchable(true);
            settingsWindow.setBackgroundDrawable(ContextCompat.getDrawable(DatabaseActivity.this, R.drawable.border));
//                        messageWindow.setAnimationStyle(R.anim.animation);

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
                    if (game.equals("Gomoku")) {
                            ((Toolbar) findViewById(R.id.toolbar)).setSubtitle("");
                            ((TextView) findViewById(R.id.capturesView)).setVisibility(View.INVISIBLE);
                    } else {
                            ((Toolbar) findViewById(R.id.toolbar)).setSubtitle("\u2B24 x 0 - \u25EF x 0");
                            ((TextView) findViewById(R.id.capturesView)).setVisibility(VISIBLE);
                    }
                    board.resetState();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
            TextView sortChoice = (TextView) settingsView.findViewById(R.id.sortChoice);
            sortChoice.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    TextView tv = (TextView) v;
                    if (tv.getText().equals("popularity")) {
                        tv.setText("win percentage");
                        PrefUtils.saveToPrefs(DatabaseActivity.this,PrefUtils.PREFS_DBSORT_KEY, "win percentage");
                    } else {
                        tv.setText("popularity");
                        PrefUtils.saveToPrefs(DatabaseActivity.this,PrefUtils.PREFS_DBSORT_KEY, "popularity");
                    }
                }
            });
            settingsWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
                @Override
                public void onDismiss() {
                    board.setAlpha(1.0f);
                }
            });


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



//        final BoardView layout = (BoardView) findViewById(R.id.boardView);
            ViewTreeObserver vto = board.getViewTreeObserver();
            vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    board.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    int width  = board.getMeasuredWidth();
                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) board.getLayoutParams();
                    params.height = width;
                    params.width = width;
//                System.out.println("kitteh " + params.width + " and " + params.height + " and " + width);
                    board.setLayoutParams(params);

                    showDBSettings();
                }
            });


            Button button = (Button) findViewById(R.id.searchButton);
            if (button != null) button.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    SearchTask searchTask = new SearchTask(board.getMovesString(), board.getGame(), (PrefUtils.getFromPrefs(DatabaseActivity.this, PrefUtils.PREFS_DBSORT_KEY, "popularity").equals("popularity")?1:2));
                    searchTask.execute((Void) null);
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

        }

        private void showDBSettings() {
            settingsWindow.showAtLocation(board, Gravity.TOP, 0, 260);

            Spinner spinner = (Spinner) settingsView.findViewById(R.id.gameSpinner);
            spinner.setSelection(PrefUtils.getIntFromPrefs(DatabaseActivity.this, PrefUtils.PREFS_DBGAME_KEY, 0));
            TextView sortOrder = (TextView) settingsView.findViewById(R.id.sortChoice);
            sortOrder.setText(PrefUtils.getFromPrefs(DatabaseActivity.this, PrefUtils.PREFS_DBSORT_KEY, "popularity"));
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

        private String moves;
        private String game;
        private int sortOrder;
        private String searchResult;

        SearchTask(String moves, String game, int sortOrder) {
            this.moves = moves;
            this.game = game;
            this.sortOrder = sortOrder;
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
                    tmpStr = URLEncoder.encode("start_game_num=0&end_game_num=100&player_1_name=&player_2_name=&game=" + game + "&site=All%20Sites&event=All%20Events&round=All%20Rounds&section=All%20Sections&winner=0","UTF-8");
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
                HttpURLConnection connection = (HttpURLConnection)url.openConnection();
                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    System.out.println("response code for submit was " + responseCode);
                    return false;
                }

                StringBuilder output = new StringBuilder();
                BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                System.out.println("output===============" + br);
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
                    moves = dashLine.substring(6).split(",");
                }
                if (dashLine.indexOf("occurrence=") == 0) {
                    occurrences = dashLine.substring(11).split(";");
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
            board.setTextViewHTML(((TextView) findViewById(R.id.playerInfo)), searchResult.replace("<tr bgcolor=\"#deecde\"><td>", "<tr bgcolor=\"#deecde\"><td><br><br>"));

        }

        @Override
        protected void onCancelled() {
        }
    }

}
