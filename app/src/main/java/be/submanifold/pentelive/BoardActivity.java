package be.submanifold.pentelive;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class BoardActivity extends AppCompatActivity {

    private BoardView board;
    private View messageView;
    private PopupWindow messageWindow;
    public Animation rotation;
    public ImageView messageIcon;
//    private int untilMove;

    private Game game;
    private final char[] coordinateLetters = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T'};

    private ResignTask resignTask;
    private CancelTask cancelTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_board);
        Toolbar toolbar = findViewById(R.id.toolbar);

        messageView = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.in_game_message, null, false);
        messageIcon = (ImageView) ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.message_icon, null);
        rotation = AnimationUtils.loadAnimation(BoardActivity.this, R.anim.rotation_animation);
        rotation.setRepeatCount(Animation.INFINITE);

        board = findViewById(R.id.boardView);
        board.setBoardActivity(this);
        this.game = getIntent().getParcelableExtra("game");
        game.parseGame(board);
        toolbar.setTitle(game.getGameType());
        setSupportActionBar(toolbar);
        board.setGame(game);


//        if (PentePlayer.mShowAds) {
//            ((AdView) findViewById(R.id.boardAdView)).loadAd(new AdRequest.Builder().build());
//        } else {
//            ((AdView) findViewById(R.id.boardAdView)).setVisibility(View.GONE);
//        }

        setRegularSubmitListener();

        Button button = findViewById(R.id.playAsWhiteButton);
        if (button != null) button.setOnClickListener(v -> {
            if (game.isSwap2()) {
                game.submitMove("0", ((EditText) messageView.findViewById(R.id.messageInput)).getText().toString());
                finish();
            } else {
                findViewById(R.id.dPenteLayout).setVisibility(View.INVISIBLE);
                findViewById(R.id.submitLayout).setVisibility(View.VISIBLE);
                board.dPenteChosen = true;
//                ((TextView) findViewById(R.id.capturesLabel)).setVisibility(View.VISIBLE);
                Toast.makeText(BoardActivity.this, getString(R.string.place_stone_submit),
                        Toast.LENGTH_LONG).show();
            }
        });
        button = findViewById(R.id.playAsBlackButton);
        if (button != null) button.setOnClickListener(v -> {
            if (game.isSwap2()) {
                findViewById(R.id.dPenteLayout).setVisibility(View.INVISIBLE);
                findViewById(R.id.submitLayout).setVisibility(View.VISIBLE);
                board.swap2Chosen = true;
                Toast.makeText(BoardActivity.this, getString(R.string.place_stone_submit),
                        Toast.LENGTH_LONG).show();
            } else {
                game.submitMove("0", ((EditText) messageView.findViewById(R.id.messageInput)).getText().toString());
                finish();
            }
        });

        button = findViewById(R.id.swap2PassButton);
        if (button != null) button.setOnClickListener(v -> {
            if (game.isSwap2()) {
                if (game.swap2Choice) {
                    Toast.makeText(BoardActivity.this, getString(R.string.place_2_stones_submit),
                            Toast.LENGTH_LONG).show();
                    findViewById(R.id.dPenteLayout).setVisibility(View.INVISIBLE);
                    findViewById(R.id.submitLayout).setVisibility(View.VISIBLE);
                    board.swap2Chosen = true;
                    board.swap2WillPass = true;
                }
            }
        });
        button = findViewById(R.id.backButton);
        if (button != null) button.setOnClickListener(v -> goBack());
        button = findViewById(R.id.forwardButton);
        if (button != null) button.setOnClickListener(v -> goForward());

        toolbar.setOnMenuItemClickListener(menuItem -> {
            switch (menuItem.getItemId()) {
                case R.id.action_cancel_resign:
                    if (!game.isActive()) {
                        return false;
                    }
                    AlertDialog.Builder builder = new AlertDialog.Builder(BoardActivity.this);
                    if (PentePlayer.mSubscriber && (game.isCanHide() || game.isCanUnHide())) {
                        String[] options = {getString(R.string.resign), getString(R.string.request_cancel), game.getHideString(), getString(R.string.dismiss)};
                        builder.setItems(options, (dialog, which) -> {
                            switch (which) {
                                case 0:
                                    resignTask = new ResignTask(game.getGameID());
                                    askConfirmation(true);
                                    break;
                                case 1:
                                    cancelTask = new CancelTask(game.getSetID());
                                    askConfirmation(false);
                                    break;
                                case 2:
                                    game.changeHideString();
                                    break;
                            }
                            // the user clicked on colors[which]
                        });

                    } else {
                        String[] options = {getString(R.string.resign), getString(R.string.request_cancel), getString(R.string.dismiss)};
                        builder.setItems(options, (dialog, which) -> {
                            switch (which) {
                                case 0:
                                    resignTask = new ResignTask(game.getGameID());
                                    askConfirmation(true);
                                    break;
                                case 1:
                                    cancelTask = new CancelTask(game.getSetID());
                                    askConfirmation(false);
                                    break;
                            }
                        });
                    }
                    builder.show();
                    return true;
                case R.id.action_lock:
                    boolean staywithgame = PrefUtils.getBooleanFromPrefs(BoardActivity.this, PrefUtils.PREFS_STAYWITHGAME_KEY, false);
                    if (staywithgame) {
                        menuItem.setIcon(R.drawable.ic_action_lock_open);
                    } else {
                        menuItem.setIcon(R.drawable.ic_action_lock_closed);
                    }
                    PrefUtils.saveBooleanToPrefs(BoardActivity.this, PrefUtils.PREFS_STAYWITHGAME_KEY, !staywithgame);
                    return true;
                case R.id.go_territory:
                    game.getTerritories();
                    board.invalidate();
                    builder = new AlertDialog.Builder(BoardActivity.this);
                    builder.setTitle(getString(R.string.score));
                    int p1Territory = game.getGoTerritoryByPlayer().get(1).size(),
                            p2Territory = game.getGoTerritoryByPlayer().get(2).size(),
                            p1Stones = game.getMovesForValue(2).size(),
                            p2Stones = game.getMovesForValue(1).size();
                    builder.setMessage(getString(R.string.scorestring, p1Territory, p1Stones, p1Stones + p1Territory, p2Territory, p2Stones, p2Territory + p2Stones + 7));
                    builder.setOnDismissListener(dialogInterface -> {
                        if (!game.isGoMarkStones()) {
                            game.getGoTerritoryByPlayer().get(1).clear();
                            game.getGoTerritoryByPlayer().get(2).clear();
                            board.invalidate();
                        }
                    });
                    AlertDialog dlg = builder.create();
                    dlg.setCanceledOnTouchOutside(true);
                    Window window = dlg.getWindow();
                    WindowManager.LayoutParams wlp = window.getAttributes();
                    wlp.gravity = Gravity.BOTTOM;
//                        dlg.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                    window.setAttributes(wlp);
                    dlg.show();
            }

            return false;
        });
    }

    private void askConfirmation(final boolean trueForResign) {
        final android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(BoardActivity.this);
        builder.setTitle(getString(R.string.rusure));
        builder.setPositiveButton(getString(R.string.yes), (dialog, id) -> {
            if (trueForResign) {
                resignTask.execute((Void) null);
            } else {
                cancelTask.execute((Void) null);
            }
            dialog.dismiss();
        });
        builder.setNegativeButton(getString(R.string.no), (dialog, id) -> dialog.dismiss());
        builder.setOnDismissListener(arg0 -> {
        });
        final android.app.AlertDialog dialog = builder.show();
    }

    public void setRegularSubmitListener() {
        Button button = findViewById(R.id.submitButton);
        if (button != null) {
            if (game.isGo() && !game.isGoMarkStones()) {
                button.setText(R.string.pass);
            } else {
                button.setText(R.string.submit);
            }
            button.setOnClickListener(v -> {
                if (!game.isActive()) {
                    Toast.makeText(BoardActivity.this, getString(R.string.not_your_turn),
                            Toast.LENGTH_LONG).show();
                    return;
                }
                String moves = "";
                if (game.isConnect6()) {
                    if (board.connect6Move1 > -1 && board.playedMove > -1 && board.connect6Move1 != board.playedMove) {
                        moves = "" + board.connect6Move1 + "," + board.playedMove;
                    } else {
                        Toast.makeText(BoardActivity.this, getString(R.string.c6_needs_2_moves),
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                } else if (game.isDPente() && game.getMovesList().isEmpty()) {
                    if (board.dPenteMove1 == -1 || board.dPenteMove2 == -1 || board.dPenteMove3 == -1 || board.dPenteMove4 == -1 ||
                            board.dPenteMove1 == board.dPenteMove2 || board.dPenteMove1 == board.dPenteMove3 || board.dPenteMove1 == board.dPenteMove4
                            || board.dPenteMove3 == board.dPenteMove2 || board.dPenteMove4 == board.dPenteMove2 || board.dPenteMove3 == board.dPenteMove4) {
                        Toast.makeText(BoardActivity.this, getString(R.string.dpente_needs_4_moves),
                                Toast.LENGTH_LONG).show();
                        return;
                    } else {
                        moves = "" + board.dPenteMove1 + "," + board.dPenteMove2 + "," + board.dPenteMove3 + "," + board.dPenteMove4;
                    }
                } else if (game.isSwap2() && game.getMovesList().isEmpty()) {
                    if (board.swap2Move1 == -1 || board.swap2Move2 == -1 || board.swap2Move3 == -1 ||
                            board.swap2Move1 == board.swap2Move2 || board.swap2Move1 == board.swap2Move3
                            || board.swap2Move3 == board.swap2Move2) {
                        Toast.makeText(BoardActivity.this, getString(R.string.swap2_needs_3_moves),
                                Toast.LENGTH_LONG).show();
                        return;
                    } else {
                        moves = "" + board.swap2Move1 + "," + board.swap2Move2 + "," + board.swap2Move3;
                    }
                } else if (game.isSwap2() && game.swap2Choice) {
                    if (board.swap2WillPass) {
                        if (board.swap2Move1 == -1 || board.swap2Move2 == -1 || board.swap2Move1 == board.swap2Move2) {
                            Toast.makeText(BoardActivity.this, getString(R.string.swap2_pass_needs_2_moves),
                                    Toast.LENGTH_LONG).show();
                            return;
                        } else {
                            moves = "2," + board.swap2Move1 + "," + board.swap2Move2;
                        }
                    } else {
                        if (board.playedMove == -1) {
                            Toast.makeText(BoardActivity.this, getString(R.string.no_momve_played_yet),
                                    Toast.LENGTH_LONG).show();
                            return;
                        } else {
                            moves = "1," + board.playedMove;
                        }
                    }
                } else if (game.isDPente() && game.dPenteChoice) {
                    if (board.playedMove == -1) {
                        Toast.makeText(BoardActivity.this, getString(R.string.no_momve_played_yet),
                                Toast.LENGTH_LONG).show();
                        return;
                    } else {
                        moves = "1," + board.playedMove;
                    }
                } else if (game.isGoMarkStones() && game.isGo()) {
                    moves = "" + (game.getGridSize() * game.getGridSize());
                    for (int move : game.getGoDeadStonesByPlayer().get(1)) {
                        moves = move + "," + moves;
                    }
                    for (int move : game.getGoDeadStonesByPlayer().get(2)) {
                        moves = move + "," + moves;
                    }
                } else if (board.playedMove == -1 && game.isGo()) {
                    moves = "" + (game.getGridSize() * game.getGridSize());
                } else if (board.playedMove == -1) {
                    Toast.makeText(BoardActivity.this, getString(R.string.no_momve_played_yet),
                            Toast.LENGTH_LONG).show();
                    return;
                } else {
                    moves = "" + board.playedMove;
                }

                game.submitMove(moves, ((EditText) messageView.findViewById(R.id.messageInput)).getText().toString());

                if (PrefUtils.getBooleanFromPrefs(BoardActivity.this, PrefUtils.PREFS_STAYWITHGAME_KEY, false)) {
                    game.setmGameString(null);
                    game.parseGame(board);
                    ((Button) findViewById(R.id.submitButton)).setText(getString(R.string.submit));
                } else {
                    finish();
                }
            });
        }
    }

    //This is the handler that will manager to process the broadcast intent
    private final BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            // Extract data included in the Intent
            String message = intent.getStringExtra("gameID");
            if (game.getGameID().equals(message)) {
                game.setmGameString(null);
                game.parseGame(board);
            }
//            System.out.println("gameID = " +message + ".");
//            System.out.println("gameIDhere = " +game.getGameID() + ".");
        }
    };


    public void goBack() {
        String str;
        if (game.isConnect6() && board.connect6Move1 > -1) {
            board.connect6Move1 = -1;
            board.invalidate();
        } else if (game.isDPente() && game.getMovesList().isEmpty()) {
            if (board.dPenteMove4 > -1) {
                board.dPenteMove4 = -1;
                str = getString(R.string.submit) + ": " + coordinateLetters[board.dPenteMove1 % 19] + "" + (19 - (board.dPenteMove1 / 19)) +
                        "-" + coordinateLetters[board.dPenteMove2 % 19] + "" + (19 - (board.dPenteMove2 / 19)) +
                        "-" + coordinateLetters[board.dPenteMove3 % 19] + "" + (19 - (board.dPenteMove3 / 19)) +
                        "-...";
                ((Button) findViewById(R.id.submitButton)).setText(str);
            } else if (board.dPenteMove3 > -1) {
                board.dPenteMove3 = -1;
                str = getString(R.string.submit) + ": " + coordinateLetters[board.dPenteMove1 % 19] + "" + (19 - (board.dPenteMove1 / 19)) +
                        "-" + coordinateLetters[board.dPenteMove2 % 19] + "" + (19 - (board.dPenteMove2 / 19)) +
                        "-...";
                ((Button) findViewById(R.id.submitButton)).setText(str);
            } else if (board.dPenteMove2 > -1) {
                board.dPenteMove2 = -1;
                str = getString(R.string.submit) + ": " + coordinateLetters[board.dPenteMove1 % 19] + "" + (19 - (board.dPenteMove1 / 19)) +
                        "-...";
                ((Button) findViewById(R.id.submitButton)).setText(str);
            } else if (board.dPenteMove1 > -1) {
                board.dPenteMove1 = -1;
                ((Button) findViewById(R.id.submitButton)).setText(getString(R.string.submit));
            }
            board.invalidate();
            return;
        } else if (game.getUntilMove() > 1) {
            if (game.isConnect6()) {
                game.setUntilMove(game.getUntilMove() - 2);
            } else {
                game.setUntilMove(game.getUntilMove() - 1);
            }
            game.replayGameUntilMove(board.abstractBoard, board);
            board.setReplayed(false);
        }
        ((Button) findViewById(R.id.submitButton)).setText(getString(R.string.submit));
//                ((TextView) findViewById(R.id.capturesLabel)).setText("\u2B24 x " + game.blackCaptures + "\n\u25EF x " + game.whiteCaptures);
        board.playedMove = -1;

        if (game.messages != null && game.messages.get(game.getUntilMove()) != null) {
            messageIcon.startAnimation(rotation);
        } else {
            messageIcon.clearAnimation();
        }
    }

    public void goForward() {
        if (game.getMovesList() == null) {
            return;
        }
        if (game.getUntilMove() < game.getMovesList().size()) {
            if (game.isConnect6()) {
                game.setUntilMove(game.getUntilMove() + 2);
            } else {
                game.setUntilMove(game.getUntilMove() + 1);
            }
            game.replayGameUntilMove(board.abstractBoard, board);
            board.setReplayed(false);
//                    ((TextView) findViewById(R.id.capturesLabel)).setText("\u2B24 x " + game.blackCaptures + "\n\u25EF x " + game.whiteCaptures);
        }
        if (game.messages.get(game.getUntilMove()) != null) {
            messageIcon.startAnimation(rotation);
        } else {
            messageIcon.clearAnimation();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        (BoardActivity.this).registerReceiver(mMessageReceiver, new IntentFilter("unique_name_computer"));
        MyApplication.activityResumed(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MyApplication.activityPaused();
        (BoardActivity.this).unregisterReceiver(mMessageReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.boardview_menu, menu);
        messageIcon.setOnClickListener(v -> {
            messageIcon.clearAnimation();
            if (game != null && game.messages != null && game.messages.get(game.getUntilMove()) != null) {
                ((TextView) messageView.findViewById(R.id.opponentMessage)).setText(game.messages.get(game.getUntilMove()));
            } else if (!game.isActive()) {
                return;
            }
            Display display = getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
//                messageView.setBackgroundColor(Color.WHITE);
            if (!game.isActive() || game.getUntilMove() < game.getMovesList().size()) {
                messageView.findViewById(R.id.messageInput).setVisibility(View.GONE);
            }
            messageWindow = new PopupWindow(messageView, size.x - 50, ViewGroup.LayoutParams.WRAP_CONTENT, true);
            messageWindow.setFocusable(true);
            messageWindow.setOutsideTouchable(true);
            messageWindow.setBackgroundDrawable(ContextCompat.getDrawable(BoardActivity.this, R.drawable.border));
//                        messageWindow.setAnimationStyle(R.anim.animation);
            messageWindow.showAtLocation(board, Gravity.TOP, 0, 260);
//                return true;
        });
        menu.findItem(R.id.action_new_message).setActionView(messageIcon);

        MenuItem item = menu.findItem(R.id.action_lock);
        boolean staywithgame = PrefUtils.getBooleanFromPrefs(BoardActivity.this, PrefUtils.PREFS_STAYWITHGAME_KEY, false);
        if (staywithgame) {
            item.setIcon(R.drawable.ic_action_lock_closed);
        } else {
            item.setIcon(R.drawable.ic_action_lock_open);
        }

//        item = menu.findItem(R.id.go_territory);
//        if (!game.isGo()) {
//            item.setVisible(false);
//        } else {
//            item.setVisible(true);
//        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.go_territory);
        item.setVisible(game.isGo());
        return super.onPrepareOptionsMenu(menu);
    }

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public class ResignTask extends AsyncTask<Void, Void, Boolean> {

        private final String gid;

        ResignTask(String gid) {
            this.gid = gid;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            try {
                String urlParameters = "gid=" + gid + "&command=resign&mobile=" + "&name2=" + PentePlayer.mPlayerName + "&password2=" + PentePlayer.mPassword;
                byte[] postData = new byte[0];
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                    postData = urlParameters.getBytes(StandardCharsets.UTF_8);
                }
                int postDataLength = postData.length;
                String request = "https://www.pente.org/gameServer/tb/resign";
                if (PentePlayer.development) {
                    request = "https://development.pente.org/gameServer/tb/resign";
                }
                URL url = new URL(request);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setDoOutput(true);
                conn.setInstanceFollowRedirects(false);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setRequestProperty("charset", "utf-8");
                conn.setRequestProperty("Content-Length", Integer.toString(postDataLength));
                conn.setUseCaches(false);
                try {
                    DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
                    wr.write(postData);
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }

                StringBuilder output = new StringBuilder();
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
//                System.out.println("output===============" + br);
                String line = "";
                while ((line = br.readLine()) != null) {
                    output.append(line + System.getProperty("line.separator"));
                }
                br.close();

                output.append(System.getProperty("line.separator") + "Response " + System.getProperty("line.separator") + System.getProperty("line.separator"));
                System.out.println(output);

            } catch (IOException e1) {
                e1.printStackTrace();
                return false;
            }

            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            if (success) {
                finish();
            }
        }

        @Override
        protected void onCancelled() {
        }
    }

    public class CancelTask extends AsyncTask<Void, Void, Boolean> {

        private final String sid;

        CancelTask(String sid) {
            this.sid = sid;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            try {
                String urlParameters = "sid=" + sid + "&command=request&mobile=" + "&name2=" + PentePlayer.mPlayerName + "&password2=" + PentePlayer.mPassword;
                byte[] postData = new byte[0];
                postData = urlParameters.getBytes(StandardCharsets.UTF_8);
                int postDataLength = postData.length;
                String request = "https://www.pente.org/gameServer/tb/cancel";
                if (PentePlayer.development) {
                    request = "https://development.pente.org/gameServer/tb/cancel";
                }
                URL url = new URL(request);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setDoOutput(true);
                conn.setInstanceFollowRedirects(false);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setRequestProperty("charset", "utf-8");
                conn.setRequestProperty("Content-Length", Integer.toString(postDataLength));
                conn.setUseCaches(false);
                try {
                    DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
                    wr.write(postData);
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }

                StringBuilder output = new StringBuilder();
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
//                System.out.println("output===============" + br);
                String line = "";
                while ((line = br.readLine()) != null) {
                    output.append(line + System.getProperty("line.separator"));
                }
                br.close();

                output.append(System.getProperty("line.separator") + "Response " + System.getProperty("line.separator") + System.getProperty("line.separator"));
                System.out.println(output);

                if (output.toString().contains("Error: Cancel request already exists.")) {
                    return false;
                }

            } catch (IOException e1) {
                e1.printStackTrace();
                return false;
            }

            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            if (success) {
                finish();
            } else {
                Toast.makeText(BoardActivity.this, getString(R.string.waiting_for_cancel_reply, game.getOpponentName()),
                        Toast.LENGTH_LONG).show();
            }
        }

        @Override
        protected void onCancelled() {
        }
    }

}
