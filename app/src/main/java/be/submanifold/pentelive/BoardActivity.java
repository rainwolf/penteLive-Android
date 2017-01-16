package be.submanifold.pentelive;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
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
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class BoardActivity extends AppCompatActivity {

    private BoardView board;
    private AdView mAdView;
    private View messageView;
    private PopupWindow messageWindow;
    public Animation rotation;
    public ImageView messageIcon;
//    private int untilMove;

    private Game game;
    private char coordinateLetters[] = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T'};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_board);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        messageView = ((LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.in_game_message, null, false);
        messageIcon = (ImageView)((LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.message_icon, null);
        rotation = AnimationUtils.loadAnimation(BoardActivity.this, R.anim.rotation_animation);
        rotation.setRepeatCount(Animation.INFINITE);
//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        board = (BoardView) findViewById(R.id.boardView);
        board.setBoardActivity(this);
        this.game = getIntent().getParcelableExtra("game");
        game.parseGame(board);
        toolbar.setTitle(game.getGameType());
        setSupportActionBar(toolbar);
        board.setGame(game);




////        final BoardView layout = (BoardView) findViewById(R.id.boardView);
//        ViewTreeObserver vto = board.getViewTreeObserver();
//        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
//            @Override
//            public void onGlobalLayout() {
//                board.getViewTreeObserver().removeGlobalOnLayoutListener(this);
//                int width  = board.getMeasuredWidth();
//                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) board.getLayoutParams();
//                params.height = width;
//                params.width = width;
////                System.out.println("kitteh " + params.width + " and " + params.height + " and " + width);
//                board.setLayoutParams(params);
//            }
//        });

        if (PentePlayer.mShowAds) {
            ((AdView) findViewById(R.id.boardAdView)).loadAd(new AdRequest.Builder().build());
        } else {
//            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) ((Button) findViewById(R.id.sendButton)).getLayoutParams();
//            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
//            ((Button) findViewById(R.id.sendButton)).setLayoutParams(params);
            ((AdView) findViewById(R.id.boardAdView)).setVisibility(View.GONE);
        }
//        mAdView = (AdView) findViewById(R.id.boardAdView);
//        AdRequest adRequest = new AdRequest.Builder().build();
//        mAdView.loadAd(adRequest);
        Button button = (Button) findViewById(R.id.submitButton);
        if (button != null) button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
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
                } else if (game.isDPente() && game.getMovesList().size() == 1) {
                    if (board.dPenteMove1 == -1 || board.dPenteMove2 == -1 || board.dPenteMove3 == -1 ||
                            board.dPenteMove1 == board.dPenteMove2 || board.dPenteMove1 == board.dPenteMove3 || board.dPenteMove3 == board.dPenteMove2) {
                        Toast.makeText(BoardActivity.this, getString(R.string.dpente_needs_3_moves),
                                Toast.LENGTH_LONG).show();
                        return;
                    } else {
                        moves = "" + board.dPenteMove1 + "," + board.dPenteMove2 + "," + board.dPenteMove3;
                    }
                } else if (game.isDPente() && game.dPenteChoice) {
                    if (board.playedMove == -1) {
                        Toast.makeText(BoardActivity.this, getString(R.string.no_momve_played_yet),
                                Toast.LENGTH_LONG).show();
                        return;
                    } else {
                        moves = "1," + board.playedMove;
                    }
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
            }
        });
        button = (Button) findViewById(R.id.playAsWhiteButton);
        if (button != null) button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ((LinearLayout) findViewById(R.id.dPenteLayout)).setVisibility(View.INVISIBLE);
                ((LinearLayout) findViewById(R.id.submitLayout)).setVisibility(View.VISIBLE);
                board.dPenteChosen = true;
//                ((TextView) findViewById(R.id.capturesLabel)).setVisibility(View.VISIBLE);
                Toast.makeText(BoardActivity.this, getString(R.string.place_stone_submit),
                        Toast.LENGTH_LONG).show();
            }
        });
        button = (Button) findViewById(R.id.playAsBlackButton);
        if (button != null) button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                game.submitMove("0", ((EditText) messageView.findViewById(R.id.messageInput)).getText().toString());
                finish();
            }
        });
        button = (Button) findViewById(R.id.backButton);
        if (button != null) button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                goBack();
            }
        });
        button = (Button) findViewById(R.id.forwardButton);
        if (button != null) button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                goForward();
            }
        });

        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch (menuItem.getItemId()){
                    case R.id.action_cancel_resign:
                        if (!game.isActive()) {
                            return false;
                        }
                        final AlertDialog.Builder builder = new AlertDialog.Builder(BoardActivity.this);
                        String options[] = {getString(R.string.resign), getString(R.string.request_cancel), getString(R.string.dismiss)};
                        builder.setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case 0: ResignTask resignTask = new ResignTask(game.getGameID());
                                        resignTask.execute((Void) null);
                                        break;
                                    case 1: CancelTask cancelTask = new CancelTask(game.getSetID());
                                        cancelTask.execute((Void) null);
                                        break;
                                }
                                // the user clicked on colors[which]
                            }
                        });
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
                        return  true;
                }

                return false;
            }
        });
    }

    //This is the handler that will manager to process the broadcast intent
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
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
        if (game.isConnect6() && board.connect6Move1 > -1) {
            board.connect6Move1 = -1;
            board.invalidate();
        } else if (game.isDPente() && game.getMovesList().size() == 1) {
            if (board.dPenteMove3 > -1) {
                board.dPenteMove3 = -1;
                ((Button) findViewById(R.id.submitButton)).setText(getString(R.string.submit) + ": " + coordinateLetters[board.dPenteMove1%19] + "" + (19 - (board.dPenteMove1/19)) +
                        "-" + coordinateLetters[board.dPenteMove2%19] + "" + (19 - (board.dPenteMove2/19)) +
                        "-...");
            }  else if (board.dPenteMove2 > -1) {
                board.dPenteMove2 = -1;
                ((Button) findViewById(R.id.submitButton)).setText(getString(R.string.submit) + ": " + coordinateLetters[board.dPenteMove1%19] + "" + (19 - (board.dPenteMove1/19)) +
                        "-...");
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
        if (game.messages.get(game.getUntilMove()) != null) {
            messageIcon.startAnimation(rotation);
        } else {
            messageIcon.clearAnimation();
        }
    }
    public void  goForward() {
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
        messageIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                messageIcon.clearAnimation();
                if (game.messages.get(game.getUntilMove()) != null) {
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
                messageWindow = new PopupWindow(messageView, size.x - 50, ViewGroup.LayoutParams.WRAP_CONTENT, true );
                messageWindow.setFocusable(true);
                messageWindow.setOutsideTouchable(true);
                messageWindow.setBackgroundDrawable(ContextCompat.getDrawable(BoardActivity.this, R.drawable.border));
//                        messageWindow.setAnimationStyle(R.anim.animation);
                messageWindow.showAtLocation(board, Gravity.TOP, 0, 260);
//                return true;
            }
        });
        menu.findItem(R.id.action_new_message).setActionView(messageIcon);
        //        if (menu.findItem(R.id.action_new_message) == null) {
//            System.out.println(" SHIIIIIIIiiiiiiiiiiit ");
//        } else {
//            System.out.println(" noooooooooot SHIIIIIIIiiiiiiiiiiit ");
//        }

        MenuItem item = menu.findItem(R.id.action_lock);
        boolean staywithgame = PrefUtils.getBooleanFromPrefs(BoardActivity.this, PrefUtils.PREFS_STAYWITHGAME_KEY, false);
        if (staywithgame) {
            item.setIcon(R.drawable.ic_action_lock_closed);
        } else {
            item.setIcon(R.drawable.ic_action_lock_open);
        }
        return true;
    }

    public Game getGame() {
        return game;
    }
    public void setGame(Game game) {
        this.game = game;
    }

    public class ResignTask extends AsyncTask<Void, Void, Boolean> {

        private String gid;

        ResignTask(String gid) {
            this.gid = gid;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            try {
                String urlParameters  = "gid=" + gid + "&command=resign&mobile=" + "&name2=" + PentePlayer.mPlayerName + "&password2=" + PentePlayer.mPassword;
                byte[] postData       = new byte[0];
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                    postData = urlParameters.getBytes( StandardCharsets.UTF_8 );
                }
                int    postDataLength = postData.length;
                String request        = "https://www.pente.org/gameServer/tb/resign";
                URL url            = new URL( request );
                HttpURLConnection conn= (HttpURLConnection) url.openConnection();
                conn.setDoOutput( true );
                conn.setInstanceFollowRedirects( false );
                conn.setRequestMethod( "POST" );
                conn.setRequestProperty( "Content-Type", "application/x-www-form-urlencoded");
                conn.setRequestProperty( "charset", "utf-8");
                conn.setRequestProperty( "Content-Length", Integer.toString( postDataLength ));
                conn.setUseCaches( false );
                try {
                    DataOutputStream wr = new DataOutputStream( conn.getOutputStream());
                    wr.write( postData );
                } catch (Exception e) {
                    e.printStackTrace();
                    return  false;
                }

                StringBuilder output = new StringBuilder();
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
//                System.out.println("output===============" + br);
                String line = "";
                while((line = br.readLine()) != null ) {
                    output.append(line + System.getProperty("line.separator"));
                }
                br.close();

                output.append(System.getProperty("line.separator") + "Response " + System.getProperty("line.separator") + System.getProperty("line.separator"));
                System.out.println(output);

            } catch (IOException e1) {
                e1.printStackTrace();
                return  false;
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

        private String sid;

        CancelTask(String sid) {
            this.sid = sid;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            try {
                String urlParameters  = "sid=" + sid + "&command=request&mobile=" + "&name2=" + PentePlayer.mPlayerName + "&password2=" + PentePlayer.mPassword;
                byte[] postData       = new byte[0];
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                    postData = urlParameters.getBytes( StandardCharsets.UTF_8 );
                }
                int    postDataLength = postData.length;
                String request        = "https://www.pente.org/gameServer/tb/cancel";
                URL url            = new URL( request );
                HttpURLConnection conn= (HttpURLConnection) url.openConnection();
                conn.setDoOutput( true );
                conn.setInstanceFollowRedirects( false );
                conn.setRequestMethod( "POST" );
                conn.setRequestProperty( "Content-Type", "application/x-www-form-urlencoded");
                conn.setRequestProperty( "charset", "utf-8");
                conn.setRequestProperty( "Content-Length", Integer.toString( postDataLength ));
                conn.setUseCaches( false );
                try {
                    DataOutputStream wr = new DataOutputStream( conn.getOutputStream());
                    wr.write( postData );
                } catch (Exception e) {
                    e.printStackTrace();
                    return  false;
                }

                StringBuilder output = new StringBuilder();
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
//                System.out.println("output===============" + br);
                String line = "";
                while((line = br.readLine()) != null ) {
                    output.append(line + System.getProperty("line.separator"));
                }
                br.close();

                output.append(System.getProperty("line.separator") + "Response " + System.getProperty("line.separator") + System.getProperty("line.separator"));
                System.out.println(output);

                if (output.toString().indexOf("Error: Cancel request already exists.") > -1) {
                    return false;
                }

            } catch (IOException e1) {
                e1.printStackTrace();
                return  false;
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
