package be.submanifold.pentelive;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.os.Build;
import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.core.content.IntentCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

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
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

import be.submanifold.pentelive.net.AuthedHttp;

public class BoardActivity extends AppCompatActivity {

    private BoardView board;
    private View messageView;
    private AlertDialog messageWindow;
    public Animation rotation;
    public ImageView messageIcon;
//    private int untilMove;

    private Game game;
    private final char[] coordinateLetters = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T'};

    private ResignTask resignTask;
    private CancelTask cancelTask;

    // TB renju draw offer: armed = DRAW? toggled on, sent with the next move; the two guard
    // flags make the incoming-offer dialog / pending-offer toast fire once per offer, not per frame.
    private boolean renjuDrawArmed = false;
    private boolean drawDialogShown = false;
    private boolean drawPendingToastShown = false;

    // InflateParams: messageView is AlertDialog content (setView) and messageIcon is a menu action view
    // (setActionView); neither has a parent at inflation time.
    @SuppressLint("InflateParams")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_board);
        Toolbar toolbar = findViewById(R.id.toolbar);

        messageView = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.in_game_message, null, false);
        messageIcon = (ImageView) ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.message_icon, null);
        messageIcon.setImageTintList(Helpers.tintList(this));
        rotation = AnimationUtils.loadAnimation(BoardActivity.this, R.anim.rotation_animation);
        rotation.setRepeatCount(Animation.INFINITE);

        board = findViewById(R.id.boardView);
        board.setBoardActivity(this);
        this.game = IntentCompat.getParcelableExtra(getIntent(), "game", Game.class);
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

        renjuDrawArmed = false;
        Button renjuPass = findViewById(R.id.renjuPassButton);
        Button renjuDraw = findViewById(R.id.renjuDrawButton);
        if (renjuPass != null) renjuPass.setOnClickListener(v -> {
            if (!game.isActive()) return;
            // PASS = renju move 225 (gridSize*gridSize); plain move, so renjuAction is null.
            // Carries the armed draw offer when DRAW? is toggled on.
            game.submitMove("225", msg(), null, renjuDrawArmed);
            finish();
        });
        if (renjuDraw != null) renjuDraw.setOnClickListener(v -> {
            renjuDrawArmed = !renjuDrawArmed;
            updateRenjuTbButtons();
            if (renjuDrawArmed) {
                Toast.makeText(this, getString(R.string.draw_offer_armed), Toast.LENGTH_LONG).show();
            }
        });

        Button button = findViewById(R.id.playAsWhiteButton);
        if (button != null) button.setOnClickListener(v -> {
            if (game.isRenju() && "SWAP".equals(game.renjuPhase)) {
                // take over the opponent's side: no stone (server ignores the payload).
                board.renjuChosen = true;
                game.submitMove("1", msg(), "swap");
                finish();
                return;
            }
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
            if (game.isRenju() && "SWAP".equals(game.renjuPhase)) {
                board.renjuChosen = true;
                int window = game.getMovesList().size();
                // reveal the board and submit the placement as a single `move` request
                findViewById(R.id.dPenteLayout).setVisibility(View.INVISIBLE);
                findViewById(R.id.submitLayout).setVisibility(View.VISIBLE);
                if (window >= 4) {
                    // move-4 "Don't swap" = Branch A: place your single 5th stone in the central
                    // 9x9 box (radius 4), sent as one `move`. Branch B (offer ten) is the separate
                    // "Place 10" button (swap2PassButton), not this decline path.
                    board.renjuBoxRadius = 4;
                    board.renjuOfferMode = false;
                } else {
                    // windows 1-3: place the single bundled stone in the central box. Buttons-
                    // only UI: no instructional toast — the green box overlay shows the legal area.
                    board.renjuBoxRadius = window; // 1/2/3 -> 3x3/5x5/7x7
                }
                // Re-draw so styleRenjuSubmit renders the initial greyed/disabled submit
                // ("submit 0/10" for offer collection, or "submit" for single placement).
                board.invalidate();
                return;
            }
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
            if (game.isRenju() && "SWAP".equals(game.renjuPhase)) {
                // move-4 "Place 10" = Branch B: decline the swap and offer ten 5th-move
                // candidates, submitted as one `move` once the tenth is placed.
                board.renjuChosen = true;
                findViewById(R.id.dPenteLayout).setVisibility(View.INVISIBLE);
                findViewById(R.id.submitLayout).setVisibility(View.VISIBLE);
                board.renjuOfferMode = true;
                board.renjuBoxRadius = 0;
                if (board.renjuPicks != null) board.renjuPicks.clear();
                board.invalidate();
                return;
            }
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
            int id = menuItem.getItemId();
            if (id == R.id.action_cancel_resign) {
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
            } else if (id == R.id.action_lock) {
                boolean staywithgame = PrefUtils.getBooleanFromPrefs(BoardActivity.this, PrefUtils.PREFS_STAYWITHGAME_KEY, false);
                if (staywithgame) {
                    menuItem.setIcon(R.drawable.ic_action_lock_open);
                } else {
                    menuItem.setIcon(R.drawable.ic_action_lock_closed);
                }
                PrefUtils.saveBooleanToPrefs(BoardActivity.this, PrefUtils.PREFS_STAYWITHGAME_KEY, !staywithgame);
                return true;
            } else if (id == R.id.go_territory) {
                game.getTerritories();
                board.invalidate();
                AlertDialog.Builder builder = new AlertDialog.Builder(BoardActivity.this);
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
                String renjuAction = null;
                if (game.isRenju() && board.renjuOfferMode) {
                    // move-4 decline OR post-take-over BRANCH: a single Branch-A stone (n==1, in the
                    // 9x9 centre) or a full Branch-B set of ten offers (n==10), submitted as one
                    // `move`; the server infers the branch from the count. Submit is gated ENABLED
                    // only at a complete, valid count (BoardView.styleRenjuSubmit), so the old
                    // count/box/offer-set validation toasts are unreachable and removed.
                    java.util.List<Integer> picks = board.renjuPicks;
                    int n = (picks == null) ? 0 : picks.size();
                    if (n != 1 && n != 10) {
                        return; // guarded: submit is disabled until a valid count
                    }
                    StringBuilder sb = new StringBuilder();
                    for (int k = 0; k < picks.size(); k++) { if (k > 0) sb.append(','); sb.append(picks.get(k)); }
                    moves = sb.toString();
                    renjuAction = "move";
                } else if (game.isRenju() && "SWAP".equals(game.renjuPhase)) {
                    // decline + place a single stone in one `move`: windows 1-3 (next stone) and
                    // move-4 "Don't swap" (Branch A, your 5th stone constrained to the 9x9).
                    if (board.playedMove == -1) {
                        Toast.makeText(BoardActivity.this, getString(R.string.no_momve_played_yet),
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    moves = "" + board.playedMove;
                    renjuAction = "move";
                } else if (game.isRenju() && ("MOVE".equals(game.renjuPhase)
                        || "COMPLETE".equals(game.renjuPhase))) {
                    // COMPLETE (regular renju play) submits a staged stone exactly like MOVE:
                    // a plain move, renjuAction null. PASS (move 225) has its own button path.
                    if (board.playedMove == -1) {
                        Toast.makeText(BoardActivity.this, getString(R.string.no_momve_played_yet),
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    moves = "" + board.playedMove; // plain move, renjuAction stays null
                } else if (game.isRenju() && "SELECTION".equals(game.renjuPhase)) {
                    // atomic 2-stone select: chosen offered black 5th + a white 6th. Submit is
                    // enabled only once both are chosen, so the old "select two" toast is removed.
                    java.util.List<Integer> sel = board.renjuSelection;
                    if (sel == null || sel.size() != 2) {
                        return; // guarded: submit is disabled until both stones are chosen
                    }
                    moves = sel.get(0) + "," + sel.get(1);
                    renjuAction = "select";
                } else if (game.isConnect6()) {
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

                board.renjuBoxRadius = 0;
                board.renjuOfferMode = false;
                board.renjuPicks = null;
                board.renjuSelection = null;
                game.submitMove(moves, ((EditText) messageView.findViewById(R.id.messageInput)).getText().toString(), renjuAction, renjuDrawArmed);
                // Offer is one-shot: consumed by the move just sent. Clear the armed state and
                // resync the DRAW? tint so a subsequent move does not re-offer unintentionally.
                renjuDrawArmed = false;
                updateRenjuTbButtons();

                if (PrefUtils.getBooleanFromPrefs(BoardActivity.this, PrefUtils.PREFS_STAYWITHGAME_KEY, false)) {
                    game.setmGameJson(null);
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
                game.setmGameJson(null);
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
            game.replayGameUntilMove(board);
            board.setReplayed(false);
        }
        ((Button) findViewById(R.id.submitButton)).setText(getString(R.string.submit));
//                ((TextView) findViewById(R.id.capturesLabel)).setText("\u2B24 x " + game.getState().blackCaptures + "\n\u25EF x " + game.getState().whiteCaptures);
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
            game.replayGameUntilMove(board);
            board.setReplayed(false);
//                    ((TextView) findViewById(R.id.capturesLabel)).setText("\u2B24 x " + game.getState().blackCaptures + "\n\u25EF x " + game.getState().whiteCaptures);
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
        ContextCompat.registerReceiver((BoardActivity.this), mMessageReceiver, new IntentFilter("unique_name_computer"), ContextCompat.RECEIVER_NOT_EXPORTED);
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
            DisplayMetrics metrics = getResources().getDisplayMetrics();
            Point size = new Point(metrics.widthPixels, metrics.heightPixels);
//                messageView.setBackgroundColor(Color.WHITE);
            if (!game.isActive() || game.getUntilMove() < game.getMovesList().size()) {
                messageView.findViewById(R.id.messageInput).setVisibility(View.GONE);
            }
            initializeMessageView();
        });
        menu.findItem(R.id.action_new_message).setActionView(messageIcon);

        MenuItem item = menu.findItem(R.id.action_lock);
        boolean staywithgame = PrefUtils.getBooleanFromPrefs(BoardActivity.this, PrefUtils.PREFS_STAYWITHGAME_KEY, false);
        if (staywithgame) {
            item.setIcon(R.drawable.ic_action_lock_closed);
        } else {
            item.setIcon(R.drawable.ic_action_lock_open);
        }
        ColorStateList tintList = Helpers.tintList(this);
        item.setIconTintList(tintList);
        item = menu.findItem(R.id.action_cancel_resign);
        item.setIconTintList(tintList);
        item = menu.findItem(R.id.go_territory);
        item.setIconTintList(tintList);
        item = menu.findItem(R.id.action_new_message);
        item.setIconTintList(tintList);

//        item = menu.findItem(R.id.go_territory);
//        if (!game.isGo()) {
//            item.setVisible(false);
//        } else {
//            item.setVisible(true);
//        }
        return true;
    }

    private void initializeMessageView() {
        if (messageWindow == null) {
            AlertDialog.Builder helpBuilder = new AlertDialog.Builder(this);
//            helpBuilder.setTitle(getString(R.string.table_settings));
            helpBuilder.setView(messageView);
            messageWindow = helpBuilder.create();
            messageWindow.setCanceledOnTouchOutside(true);
        }
        messageWindow.show();
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

    private String msg() {
        return ((EditText) messageView.findViewById(R.id.messageInput)).getText().toString();
    }

    /**
     * Drives the turn-based renju PASS / DRAW? buttons and surfaces any live draw offer.
     * Called from BoardView.onDraw on every render of a renju game (poll refresh and stone-staging
     * both invalidate; BoardView guards the call on game.isRenju()), and again right after a
     * submit. PASS shows only in COMPLETE when no stone is staged; DRAW? shows in COMPLETE
     * regardless; SUBMIT enable/disable stays owned by styleRenjuSubmit.
     */
    void updateRenjuTbButtons() {
        if (game == null || board == null) return;
        Button renjuPass = findViewById(R.id.renjuPassButton);
        Button renjuDraw = findViewById(R.id.renjuDrawButton);
        boolean renjuComplete = game.isRenju() && "COMPLETE".equals(game.renjuPhase)
                && game.isActive();
        boolean staged = board.playedMove > -1;
        if (renjuPass != null) {
            renjuPass.setVisibility(renjuComplete && !staged ? View.VISIBLE : View.GONE);
        }
        if (renjuDraw != null) {
            renjuDraw.setVisibility(renjuComplete ? View.VISIBLE : View.GONE);
            // Armed is carried as the view's selected state; res/color/renju_draw_tint.xml maps that
            // to the green backgroundTint and picks up the values-night variant on its own. The old
            // runtime colour filter took the tint through an `armed ? int : null` ternary, which
            // types as Integer and unboxes to int -- that is what crashed 2.11.10 on the disarm path.
            renjuDraw.setSelected(renjuDrawArmed);
        }
        handleDrawOffer();
    }

    /**
     * A pending draw offer surfaces exactly once: as a bottom chooser when it is my turn to
     * respond (opponent offered), or as a long "pending" toast when I offered and am waiting.
     * The guard flags reset when the offer clears so the next offer surfaces again.
     */
    private void handleDrawOffer() {
        if (game == null || !game.isDrawOffered()) {
            drawDialogShown = false;
            drawPendingToastShown = false;
            return;
        }
        if (game.isActive()) {
            if (!drawDialogShown) {
                drawDialogShown = true;
                // Defer off the draw pass — showing a dialog mid-onDraw is unsafe.
                new android.os.Handler(android.os.Looper.getMainLooper()).post(this::showDrawOfferDialog);
            }
        } else if (!drawPendingToastShown) {
            drawPendingToastShown = true;
            Toast.makeText(this, getString(R.string.draw_offer_pending), Toast.LENGTH_LONG).show();
        }
    }

    private void showDrawOfferDialog() {
        if (isFinishing() || game == null || !game.isDrawOffered() || !game.isActive()) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(BoardActivity.this);
        builder.setTitle(getString(R.string.offers_draw, game.getOpponentName()));
        String[] options = {getString(R.string.accept), getString(R.string.dismiss)};
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                new AcceptDrawTask(game.getGameID()).execute((Void) null);
            }
            // dismiss (or playing a move) declines — no-op.
        });
        builder.show();
    }

    public class ResignTask extends BackgroundTask<Void, Boolean> {

        private final String gid;

        ResignTask(String gid) {
            this.gid = gid;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            try {
                String urlParameters = "gid=" + gid + "&command=resign&mobile=";
                AuthedHttp.Reply reply = AuthedHttp.shared().postForm("/gameServer/tb/resign", urlParameters);
                System.out.println(reply.body);

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

    // Mirrors ResignTask's POST scaffold, but hits the TB game endpoint with command=acceptDraw
    // (MoveServlet routes acceptDraw there, not the resign servlet). Server validates the pending
    // offer; on success we re-poll so the board reflects the now-drawn game.
    public class AcceptDrawTask extends BackgroundTask<Void, Boolean> {

        private final String gid;

        AcceptDrawTask(String gid) {
            this.gid = gid;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            try {
                String urlParameters = "gid=" + gid + "&command=acceptDraw&mobile=";
                AuthedHttp.Reply reply = AuthedHttp.shared().postForm("/gameServer/tb/game", urlParameters);
                System.out.println(reply.body);

            } catch (IOException e1) {
                e1.printStackTrace();
                return false;
            }

            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            if (success) {
                game.setmGameJson(null);
                game.parseGame(board);
            }
        }

        @Override
        protected void onCancelled() {
        }
    }

    public class CancelTask extends BackgroundTask<Void, Boolean> {

        private final String sid;

        CancelTask(String sid) {
            this.sid = sid;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            try {
                String urlParameters = "sid=" + sid + "&command=request&mobile=";
                AuthedHttp.Reply reply = AuthedHttp.shared().postForm("/gameServer/tb/cancel", urlParameters);
                System.out.println(reply.body);

                if (reply.body.contains("Error: Cancel request already exists.")) {
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
