package be.submanifold.pentelive.liveGameRoom;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import android.text.InputType;
import android.text.method.LinkMovementMethod;
import android.text.method.ScrollingMovementMethod;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;

import be.submanifold.pentelive.InviteAIActivity;
import be.submanifold.pentelive.PentePlayer;
import be.submanifold.pentelive.PrefUtils;
import be.submanifold.pentelive.R;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link LiveTableFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link LiveTableFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class LiveTableFragment extends Fragment {
    Table table = null;
    LiveBoardView board;
    Handler timerHandler = new Handler();
    Runnable timerUpdater = new Runnable() {
        @Override
        public void run() {
            try {
                updateTimer(); //this function can change value of mInterval.
            } finally {
                // 100% guarantee that this always happens, even if
                // your update method throws an exception
                timerHandler.postDelayed(timerUpdater, 40);
            }
        }
    };
    CountDownTimer countDownTimer = null;
    AlertDialog waitForPlayerReturnDialog = null;
    int countDownSeconds = 60;

    TextView p1Name, p2Name, p1Timer, p2Timer, settingsText,
            tableTextView, capturesTextView, gameNameView;
    LinearLayout p1Layout, p2Layout;
    Button playButton;
    private String me = "";
    LiveGameRoomActivity activity;
    Timer timer = new Timer();
    View settingsView = null;
    TextView timedChoice, ratedChoice, privateChoice;
    EditText initialMinutesView, incrementalSecondsView;
    Spinner gameSpinner;
    AlertDialog tableSettingsWindow;

    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    public LiveTableFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment LiveTableFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static LiveTableFragment newInstance(String param1, String param2) {
        LiveTableFragment fragment = new LiveTableFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_live_table, container, false);
        setHasOptionsMenu(true);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Toolbar toolbar = getView().findViewById(R.id.toolbar);
//        toolbar.setTitle(getString(R.string.home));
        toolbar.inflateMenu(R.menu.live_table_menu);
        toolbar.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.action_players:
                    if (table.getOwner().equals(me)) {
                        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                        String[] options = {getString(R.string.show_table_players), getString(R.string.boot_player), getString(R.string.invite_player)};
                        builder.setItems(options, (dialog, which) -> {
                            switch (which) {
                                case 0:
                                    showTablePlayers();
                                    break;
                                case 1:
                                    showBootablePlayers();
                                    break;
                                case 2:
                                    showInvitePlayers();
                                    break;
                            }
                        });
                        builder.show();

                    } else {
                        showTablePlayers();
                    }
                    return true;

                case R.id.action_game:
                    if (table.isSeated(me) && table.getGameState().state == State.STARTED) {
                        showGameActions(table.currentPlayerName().equals(me));
                    } else {
                        Toast.makeText(activity, getString(R.string.not_player),
                                Toast.LENGTH_LONG).show();
                    }
                    return true;

                case R.id.action_settings:
                    if (table.getOwner().equals(me)) {
                        initializeSettingsView();
                    } else {
                        Toast.makeText(activity, getString(R.string.not_owner),
                                Toast.LENGTH_LONG).show();
                    }

                    return true;

                default:
                    // If we got here, the user's action was not recognized.
                    // Invoke the superclass to handle it.
                    return false;

            }
        });

        activity = (LiveGameRoomActivity) getActivity();
        me = activity.getMe();
        board = getView().findViewById(R.id.boardView);
        board.setTable(table, me);
        board.setFragment(this);
        p1Name = getView().findViewById(R.id.p1Name);
        p2Name = getView().findViewById(R.id.p2Name);
        p1Timer = getView().findViewById(R.id.p1Timer);
        p2Timer = getView().findViewById(R.id.p2Timer);
        settingsText = getView().findViewById(R.id.settingsText);
        playButton = getView().findViewById(R.id.playButton);
        tableTextView = getView().findViewById(R.id.tableTextView);
        capturesTextView = getView().findViewById(R.id.capturesView);
        gameNameView = getView().findViewById(R.id.gameNameView);
        capturesTextView.setText(table.getCapturesText(capturesTextView.getLineHeight()));
        playButton.setOnClickListener(view14 -> {
            if (playButton.getText().equals(getString(R.string.pass)) && table.isGo() && mListener != null) {
                int passMove = table.getGridSize() * table.getGridSize();
                mListener.sendEvent("{\"dsgMoveTableEvent\":{\"move\":" + passMove + ",\"moves\":[" + passMove + "],\"player\":\"" + me + "\",\"table\":" + table.getId() + ",\"time\":0}}");
            } else if (mListener != null && table.isSeated(me)) {
                mListener.sendEvent("{\"dsgPlayTableEvent\":{\"table\":" + table.getId() + ",\"time\":0}}");
                playButton.setVisibility(View.INVISIBLE);
            }
        });
        p1Layout = getView().findViewById(R.id.p1Layout);
        p2Layout = getView().findViewById(R.id.p2Layout);
        p1Layout.setOnClickListener(view13 -> {
            if (mListener != null) {
                if (table.isSeated(me)) {
                    mListener.sendEvent("{\"dsgStandTableEvent\":{\"table\":" + table.getId() + ",\"time\":0}}");
                } else if (table.getSeats().get(1) == null) {
                    mListener.sendEvent("{\"dsgSitTableEvent\":{\"seat\":1,\"table\":" + table.getId() + ",\"time\":0}}");
                }
            }
        });
        p2Layout.setOnClickListener(view12 -> {
            if (mListener != null) {
                if (table.isSeated(me)) {
                    mListener.sendEvent("{\"dsgStandTableEvent\":{\"table\":" + table.getId() + ",\"time\":0}}");
                } else if (table.getSeats().get(2) == null) {
                    mListener.sendEvent("{\"dsgSitTableEvent\":{\"seat\":2,\"table\":" + table.getId() + ",\"time\":0}}");
                }
            }
        });
        tableTextView.setOnClickListener(view1 -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            final EditText input = new EditText(activity);
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            builder.setView(input);
            builder.setPositiveButton(activity.getString(R.string.send), (dialog, which) -> {
                String m_Text = input.getText().toString();
                if (!"".equals(m_Text)) {
                    String event = "{\"dsgTextTableEvent\":{\"text\":\"" + m_Text + "\",\"table\":" + table.getId() + ",\"time\":0}}";
                    if (mListener != null) {
                        mListener.sendEvent(event);
                    }
                }
            });
            AlertDialog dlg = builder.create();
            dlg.show();
            dlg.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        });
        updateTable();
    }

    @Override
    public void onResume() {
        super.onResume();
        getView().setFocusableInTouchMode(true);
        getView().requestFocus();
        getView().setOnKeyListener((v, keyCode, event) -> {
            System.out.println("w000000000t");
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                System.out.println("whaaaaaaaaaat");
                if (mListener != null) {
                    mListener.sendEvent("{\"dsgExitTableEvent\":{\"forced\":false,\"table\":" + table.getId() + ",\"booted\":false,\"time\":0}}");
                    return true;
                }
            }
            return false;
        });
    }


    public void updateTable() {
        LivePlayer player = table.getSeats().get(1);
        if (player == null) {
            p1Name.setText(getString(R.string.tap_to_sit));
        } else {
            p1Name.setText(player.coloredNameString(p1Name.getLineHeight()));
        }
        player = table.getSeats().get(2);
        if (player == null) {
            p2Name.setText(getString(R.string.tap_to_sit));
        } else {
            p2Name.setText(player.coloredNameString(p2Name.getLineHeight()));
        }
        long initialMnts = table.getTimer().get("initialMinutes");
        long incrementalScnds = table.getTimer().get("incrementalSeconds");
        String ratedStr = "";
        if (table.isRated()) {
            ratedStr = getString(R.string.rated);
        } else {
            ratedStr = getString(R.string.notRated);
        }
        String timerStr = getString(R.string.no_timer);
        if (table.isTimed()) {
            timerStr = getString(R.string.timer) + ": " + initialMnts + "/" + incrementalScnds;
        }
        settingsText.setText(timerStr + "\n" + ratedStr);
        synchronized (this) {
            Map<String, Long> timer1, timer2;
            timer1 = table.getGameState().timers.get(1);
            timer2 = table.getGameState().timers.get(2);
            long minutes = initialMnts;
            long seconds = incrementalScnds;
            long tenths = -1;
            if (timer1.get("millis") != null) {
                minutes = timer1.get("millis") / 1000 / 60;
                seconds = timer1.get("millis") / 1000 % 60;
                if (minutes == 0 && seconds < 12) {
                    tenths = timer1.get("millis") / 100 % 10;
                }
            }
            if (tenths > -1) {
                p1Timer.setText(minutes + ":" + seconds + "." + tenths);
            } else {
                p1Timer.setText(minutes + ":" + seconds);
            }
            minutes = initialMnts;
            seconds = incrementalScnds;
            tenths = -1;
            if (timer2.get("millis") != null) {
                minutes = timer2.get("millis") / 1000 / 60;
                seconds = timer2.get("millis") / 1000 % 60;
                if (minutes == 0 && seconds < 12) {
                    tenths = timer2.get("millis") / 100 % 10;
                }
            }
            if (tenths > -1) {
                p2Timer.setText(minutes + ":" + seconds + "." + tenths);
            } else {
                p2Timer.setText(minutes + ":" + seconds);
            }
        }
        capturesTextView.setText(table.getCapturesText(capturesTextView.getLineHeight()));

        if (table.getSeats().size() == 2 && table.isSeated(me) && (table.getGameState().state == State.NOTSTARTED || table.getGameState().state == State.HALFSET)) {
            playButton.setVisibility(View.VISIBLE);
            playButton.setText(getString(R.string.play));
        } else if (table.isGo() && table.getGameState().state == State.STARTED && table.isMyTurn(me)) {
            playButton.setVisibility(View.VISIBLE);
            playButton.setText(R.string.pass);
        } else {
            playButton.setVisibility(View.GONE);
        }
        if (table.gameHasCaptures()) {
            capturesTextView.setVisibility(View.VISIBLE);
        } else {
            capturesTextView.setVisibility(View.GONE);
        }
        gameNameView.setText(table.getGameName());
        board.setGridSize(table.getGridSize());
        board.invalidate();
    }

    public void addText(String text) {
        String tableText = tableTextView.getText().toString();
        tableTextView.setText(tableText + text + "\n");
        tableTextView.setMovementMethod(new ScrollingMovementMethod());
        tableTextView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        void sendEvent(String event);
    }

    public Table getTable() {
        return table;
    }

    public void setTable(Table table) {
        this.table = table;
    }

    public OnFragmentInteractionListener getListener() {
        return mListener;
    }


    public void addMove(int move) {
        table.addMove(move);
        capturesTextView.setText(table.getCapturesText(capturesTextView.getLineHeight()));
        if (table.isDPente() && table.getMoves().size() == 4 && table.getGameState().dPenteState == DPenteState.NOCHOICE) {
            LivePlayer p2Player = table.getSeats().get(2);
            if (p2Player != null && p2Player.getName().equals(me)) {
                showDPenteChoice();
            }
        }
        if (table.swap2ChoiceWithPass() || table.swap2ChoiceWithoutPass()) {
            if (table.swap2ChoiceWithPass()) {
                LivePlayer p2Player = table.getSeats().get(2);
                if (p2Player != null && p2Player.getName().equals(me)) {
                    showSwap2Choice();
                }
            } else if (table.swap2ChoiceWithoutPass()) {
                LivePlayer p1Player = table.getSeats().get(1);
                if (p1Player != null && p1Player.getName().equals(me)) {
                    showSwap2Choice();
                }
            }
        }
        if (table.isGo() && table.getGameState().state == State.STARTED && table.isMyTurn(me)) {
            playButton.setVisibility(View.VISIBLE);
            playButton.setText(R.string.pass);
        } else {
            playButton.setVisibility(View.GONE);
        }
        if (table.isGo() && (table.getGameState().goState == GoState.MARKSTONES || table.getGameState().goState == GoState.EVALUATESTONES)) {
            board.setGoTerritoryByPlayer(table.getTerritories());
            board.setGoDeadStonesByPlayer(table.getGoDeadStonesByPlayer());
        } else {
            board.clearGoStructures();
        }
        board.invalidate();
        showGoDialog();
    }

    public void addMoves(List<Integer> moves) {
        table.addMoves(moves);
        capturesTextView.setText(table.getCapturesText(capturesTextView.getLineHeight()));
        if (table.isDPente() && table.getMoves().size() == 4 && table.getGameState().dPenteState == DPenteState.NOCHOICE) {
            LivePlayer p2Player = table.getSeats().get(2);
            if (p2Player != null && p2Player.getName().equals(me)) {
                showDPenteChoice();
            }
        }
        if (table.swap2ChoiceWithPass() || table.swap2ChoiceWithoutPass()) {
            if (table.swap2ChoiceWithPass()) {
                LivePlayer p2Player = table.getSeats().get(2);
                if (p2Player != null && p2Player.getName().equals(me)) {
                    showSwap2Choice();
                }
            } else if (table.swap2ChoiceWithoutPass()) {
                LivePlayer p1Player = table.getSeats().get(1);
                if (p1Player != null && p1Player.getName().equals(me)) {
                    showSwap2Choice();
                }
            }
        }
        if (table.isGo() && table.getGameState().state == State.STARTED && table.isMyTurn(me)) {
            playButton.setVisibility(View.VISIBLE);
            playButton.setText(R.string.pass);
        } else {
            playButton.setVisibility(View.GONE);
        }
        if (table.isGo() && (table.getGameState().goState == GoState.MARKSTONES || table.getGameState().goState == GoState.EVALUATESTONES)) {
            board.setGoTerritoryByPlayer(table.getTerritories());
            board.setGoDeadStonesByPlayer(table.getGoDeadStonesByPlayer());
        } else {
            board.clearGoStructures();
        }
        board.invalidate();
        showGoDialog();
    }

    public void rejectGoState() {
        table.rejectAndContinue();
        if (table.isGo() && table.getGameState().state == State.STARTED && table.isMyTurn(me)) {
            playButton.setVisibility(View.VISIBLE);
            playButton.setText(R.string.pass);
        } else {
            playButton.setVisibility(View.GONE);
        }
        board.clearGoStructures();
        board.invalidate();
    }

    private void showGoDialog() {
        if (table.isGo() && table.getGameState().state == State.STARTED) {
            if (table.getGameState().goState == GoState.EVALUATESTONES && table.showEvaluateDialog(me)) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setTitle(table.getScoreMessage());
                String[] options = {getString(R.string.accept), getString(R.string.reject)};
                builder.setItems(options, (dialog, which) -> {
                    int passMove = table.getGridSize() * table.getGridSize();
                    switch (which) {
                        case 0:
                            mListener.sendEvent("{\"dsgMoveTableEvent\":{\"move\":" + passMove + ",\"moves\":[" + passMove + "],\"player\":\"" + me + "\",\"table\":" + table.getId() + ",\"time\":0}}");
                            break;
                        case 1:
                            mListener.sendEvent("{\"dsgRejectGoStateEvent\":{\"player\":\"" + me + "\",\"table\":" + table.getId() + ",\"time\":0}}");
                            break;
                    }
                    // the user clicked on colors[which]
                });
                AlertDialog dlg = builder.create();
                dlg.setCanceledOnTouchOutside(false);
                Window window = dlg.getWindow();
                WindowManager.LayoutParams wlp = window.getAttributes();
                wlp.gravity = Gravity.BOTTOM;
                dlg.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                window.setAttributes(wlp);
                dlg.show();
            } else if (table.getGameState().goState == GoState.MARKSTONES && table.isMyTurn(me)
                    && table.startMarkStones()) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setMessage(activity.getString(R.string.double_pass_live));
                AlertDialog dlg = builder.create();
                Window window = dlg.getWindow();
                WindowManager.LayoutParams wlp = window.getAttributes();
                wlp.gravity = Gravity.BOTTOM;
                dlg.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                window.setAttributes(wlp);
                dlg.show();
            }
        }
    }

    public void updateGameState(int state) {
        if (state == 2 && table.getGameState().state != State.PAUSED) {
            board.clearGoStructures();
        }
        table.updateGameState(state);
        gameStateChanged();
    }

    public void gameStateChanged() {
        if (table.getGameState().state == State.STARTED) {
            board.invalidate();
            if (table.isTimed()) {
                timerUpdater.run();
            }
            if (countDownTimer != null) {
                countDownTimer.cancel();
                countDownTimer = null;
            }
            if (waitForPlayerReturnDialog != null) {
                waitForPlayerReturnDialog.dismiss();
                waitForPlayerReturnDialog = null;
            }
        } else {
            timerHandler.removeCallbacks(timerUpdater);
            if (table.getGameState().state == State.PAUSED && table.isSeated(me)) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setNegativeButton(getString(R.string.resign), (dialog, which) -> sendResign());
                waitForPlayerReturnDialog = builder.create();
                waitForPlayerReturnDialog.setTitle("1:00");
                waitForPlayerReturnDialog.setMessage(getString(R.string.player_disconnected));
                Window window = waitForPlayerReturnDialog.getWindow();
                WindowManager.LayoutParams wlp = window.getAttributes();
                wlp.gravity = Gravity.BOTTOM;
//                waitForPlayerReturnDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                window.setAttributes(wlp);
                waitForPlayerReturnDialog.setOnDismissListener(dialogInterface -> {
                    if (countDownTimer != null) {
                        countDownTimer.cancel();
                        countDownTimer = null;
                        waitForPlayerReturnDialog = null;
                    }
                });
                waitForPlayerReturnDialog.show();
                countDownTimer = new CountDownTimer(60 * 1000, 1000) {
                    @Override
                    public void onTick(long l) {
                        long seconds = l / 1000;
                        waitForPlayerReturnDialog.setTitle((seconds / 60) + ":" + (seconds % 60));
                    }

                    @Override
                    public void onFinish() {
                        if (waitForPlayerReturnDialog != null) {
                            waitForPlayerReturnDialog.dismiss();
                            waitForPlayerReturnDialog = null;
                        }
                    }
                }.start();
            }
        }
        if (table.getSeats().size() == 2 && table.isSeated(me) && (table.getGameState().state == State.NOTSTARTED || table.getGameState().state == State.HALFSET)) {
            playButton.setText(getString(R.string.play));
            playButton.setVisibility(View.VISIBLE);
        } else if (table.isGo() && table.getGameState().state == State.STARTED && table.isMyTurn(me)) {
            playButton.setVisibility(View.VISIBLE);
            playButton.setText(R.string.pass);
        } else {
            playButton.setVisibility(View.GONE);
        }
    }

    public void updateTimer() {
        synchronized (this) {
            int currentPlayer = table.currentPlayer();
            table.updateTimer(false, currentPlayer, -1);
            Map<String, Long> timer = table.getGameState().timers.get(currentPlayer);
            TextView screenTimer = p1Timer;
            if (currentPlayer == 2) {
                screenTimer = p2Timer;
            }

            long minutes = table.getTimer().get("initialMinutes");
            long seconds = table.getTimer().get("incrementalSeconds");
            long tenths = -1;
            if (timer.get("millis") != null) {
                minutes = timer.get("millis") / 1000 / 60;
                seconds = timer.get("millis") / 1000 % 60;
                if (minutes == 0 && seconds < 12) {
                    tenths = timer.get("millis") / 100 % 10;
                }
            }
            if (tenths > -1) {
                screenTimer.setText(minutes + ":" + seconds + "." + tenths);
            } else {
                screenTimer.setText(minutes + ":" + seconds);
            }
        }
    }

    private void initializeSettingsView() {
        if (settingsView == null) {
            settingsView = activity.getLayoutInflater().inflate(R.layout.live_table_settings, null);
            timedChoice = settingsView.findViewById(R.id.timedChoice);
            timedChoice.setOnClickListener(view -> {
                if (timedChoice.getText().toString().equals(getString(R.string.yes))) {
                    timedChoice.setText(getString(R.string.no));
                } else {
                    timedChoice.setText(getString(R.string.yes));
                }
                sendTableChange();
            });
            ratedChoice = settingsView.findViewById(R.id.ratedChoice);
            ratedChoice.setOnClickListener(view -> {
                if (ratedChoice.getText().toString().equals(getString(R.string.yes))) {
                    ratedChoice.setText(getString(R.string.no));
                } else {
                    ratedChoice.setText(getString(R.string.yes));
                }
                sendTableChange();
            });
            privateChoice = settingsView.findViewById(R.id.privateChoice);
            privateChoice.setOnClickListener(view -> {
                if (privateChoice.getText().toString().equals(getString(R.string.public_table))) {
                    privateChoice.setText(getString(R.string.private_table));
                } else {
                    privateChoice.setText(getString(R.string.public_table));
                }
                sendTableChange();
            });
            initialMinutesView = settingsView.findViewById(R.id.initialMinutesInput);
            incrementalSecondsView = settingsView.findViewById(R.id.incrementalSecondsInput);
            gameSpinner = settingsView.findViewById(R.id.gameSpinner);
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(activity,
                    R.array.game_types_array, android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            gameSpinner.setAdapter(adapter);
            gameSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    sendTableChange();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
            gameSpinner.setOnTouchListener((view, motionEvent) -> {
                InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                return false;
            });
            AlertDialog.Builder helpBuilder = new AlertDialog.Builder(getContext());
            helpBuilder.setTitle(getString(R.string.table_settings));
            helpBuilder.setView(settingsView);
            tableSettingsWindow = helpBuilder.create();
            tableSettingsWindow.setCanceledOnTouchOutside(true);
            tableSettingsWindow.setOnDismissListener(dialogInterface -> sendTableChange());
        }
        gameSpinner.setSelection((table.getGame() - 1) / 2);
        if (table.isTimed()) {
            timedChoice.setText(getString(R.string.yes));
        } else {
            timedChoice.setText(getString(R.string.no));
        }
        if (table.isRated()) {
            ratedChoice.setText(getString(R.string.yes));
        } else {
            ratedChoice.setText(getString(R.string.no));
        }
        initialMinutesView.setText(table.getTimer().get("initialMinutes") + "");
        incrementalSecondsView.setText(table.getTimer().get("incrementalSeconds") + "");
        tableSettingsWindow.show();
    }

    private void sendTableChange() {
        String timedStr = "false";
        if (timedChoice.getText().toString().equals(getString(R.string.yes))) {
            timedStr = "true";
        }
        String ratedStr = "false";
        if (ratedChoice.getText().toString().equals(getString(R.string.yes))) {
            ratedStr = "true";
        }
        String typeStr = "1";
        if (privateChoice.getText().toString().equals(getString(R.string.private_table))) {
            typeStr = "2";
        }
        if (mListener != null) {
            mListener.sendEvent("{\"dsgChangeStateTableEvent\":{\"timed\":" + timedStr +
                    ",\"initialMinutes\":" + initialMinutesView.getText() +
                    ",\"incrementalSeconds\":" + incrementalSecondsView.getText() +
                    ",\"rated\":" + ratedStr +
                    ",\"game\":" + (gameSpinner.getSelectedItemPosition() * 2 + 1) +
                    ",\"tableType\":" + typeStr + ",\"player\":\"" + me +
                    "\",\"table\":" + table.getId() + ",\"time\":0}}");
        }
    }

    private void showGameActions(boolean myTurn) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        if (table.isGo()) {
            if (table.getGameState().state == State.STARTED &&
                    ((table.getGameState().goState == GoState.PLAY && !myTurn) || (table.getGameState().goState == GoState.MARKSTONES && myTurn))) {
                String[] options = {getString(R.string.score), getString(R.string.request_undo), getString(R.string.resign), getString(R.string.request_cancel), getString(R.string.dismiss)};
                builder.setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            scoreGame();
                            break;
                        case 1:
                            sendUndoRequest();
                            break;
                        case 2:
                            sendResign();
                            break;
                        case 3:
                            sendCancelRequest();
                            break;
                    }
                });
            } else {
                String[] options = {getString(R.string.score), getString(R.string.resign), getString(R.string.request_cancel), getString(R.string.dismiss)};
                builder.setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            scoreGame();
                            break;
                        case 1:
                            sendResign();
                            break;
                        case 2:
                            sendCancelRequest();
                            break;
                    }
                });
            }

        } else if (!myTurn) {
            String[] options = {getString(R.string.request_undo), getString(R.string.resign), getString(R.string.request_cancel), getString(R.string.dismiss)};
            builder.setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0:
                        sendUndoRequest();
                        break;
                    case 1:
                        sendResign();
                        break;
                    case 2:
                        sendCancelRequest();
                        break;
                }
                // the user clicked on colors[which]
            });
        } else {
            String[] options = {getString(R.string.resign), getString(R.string.request_cancel), getString(R.string.dismiss)};
            builder.setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0:
                        sendResign();
                        break;
                    case 1:
                        sendCancelRequest();
                        break;
                }
                // the user clicked on colors[which]
            });
        }
        builder.show();
    }

    private void sendResign() {
        if (mListener != null) {
            mListener.sendEvent("{\"dsgResignTableEvent\":{\"player\":\"" + me + "\",\"table\":" + table.getId() + ",\"time\":0}}");
        }
    }

    private void sendCancelRequest() {
        if (mListener != null) {
            mListener.sendEvent("{\"dsgCancelRequestTableEvent\":{\"player\":\"" + me + "\",\"table\":" + table.getId() + ",\"time\":0}}");
        }
    }

    private void sendUndoRequest() {
        if (mListener != null) {
            mListener.sendEvent("{\"dsgUndoRequestTableEvent\":{\"table\":" + table.getId() + ",\"time\":0}}");
        }
    }

    private void scoreGame() {
        board.setGoTerritoryByPlayer(table.getTerritories());
        addText("* " + table.getScoreMessage());
        board.invalidate();
    }

    public void cancelRequest(String player) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(activity.getString(R.string.requests_cancellation, player));
        String[] options = {getString(R.string.accept), getString(R.string.decline)};
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    sendCancelReply(true);
                    break;
                case 1:
                    sendCancelReply(false);
                    break;
            }
            // the user clicked on colors[which]
        });
        AlertDialog dlg = builder.create();
        dlg.setCanceledOnTouchOutside(false);
        Window window = dlg.getWindow();
        WindowManager.LayoutParams wlp = window.getAttributes();
        wlp.gravity = Gravity.BOTTOM;
        dlg.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.setAttributes(wlp);
        dlg.show();
    }

    private void sendCancelReply(boolean accept) {
        if (mListener != null) {
            mListener.sendEvent("{\"dsgCancelReplyTableEvent\":{\"accepted\":" + (accept ? "true" : "false") + ",\"player\":\"" + me + "\",\"table\":" + table.getId() + ",\"time\":0}}");
        }
    }

    public void undoRequested(String player) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(activity.getString(R.string.requests_undo, player));
        String[] options = {getString(R.string.accept), getString(R.string.decline)};
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    sendUndoReply(true);
                    break;
                case 1:
                    sendUndoReply(false);
                    break;
            }
            // the user clicked on colors[which]
        });
        AlertDialog dlg = builder.create();
        dlg.setCanceledOnTouchOutside(false);
        Window window = dlg.getWindow();
        WindowManager.LayoutParams wlp = window.getAttributes();
        wlp.gravity = Gravity.BOTTOM;
        dlg.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.setAttributes(wlp);
        dlg.show();
    }

    private void sendUndoReply(boolean accept) {
        if (mListener != null) {
            mListener.sendEvent("{\"dsgUndoReplyTableEvent\":{\"accepted\":" + (accept ? "true" : "false") + ",\"player\":\"" + me + "\",\"table\":" + table.getId() + ",\"time\":0}}");
        }
    }

    public void showDPenteChoice() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(activity.getString(R.string.continue_as));
        String[] options = {getString(R.string.p1white), getString(R.string.p2black)};
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    sendDPenteChoice(true);
                    break;
                case 1:
                    sendDPenteChoice(false);
                    break;
            }
            // the user clicked on colors[which]
        });
        AlertDialog dlg = builder.create();
        dlg.setCanceledOnTouchOutside(false);
        Window window = dlg.getWindow();
        WindowManager.LayoutParams wlp = window.getAttributes();
        wlp.gravity = Gravity.BOTTOM;
//        wlp.flags &= ~WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        window.setAttributes(wlp);
        dlg.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        dlg.show();
    }

    private void sendDPenteChoice(boolean white) {
        if (mListener != null) {
            mListener.sendEvent("{\"dsgSwapSeatsTableEvent\":{\"swap\":" + (white ? "true" : "false") + ",\"silent\":false,\"player\":\"" + me + "\",\"table\":" + table.getId() + ",\"time\":0}}");
        }
    }
    public void showSwap2Choice() {
        System.out.println("swap2choice show");
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(activity.getString(R.string.continue_as));
        if (table.swap2ChoiceWithoutPass()) {
            String[] options = {getString(R.string.p1white), getString(R.string.p2black)};
            builder.setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0:
                        sendSwap2Choice(false);
                        break;
                    case 1:
                        sendSwap2Choice(true);
                        break;
                }
            });
        } else {
            String[] options = {getString(R.string.p1white), getString(R.string.p2black), getString(R.string.swap2pass)};
            builder.setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0:
                        sendSwap2Choice(true);
                        break;
                    case 1:
                        sendSwap2Choice(false);
                        break;
                    case 2:
                        sendSwap2Pass();
                        break;
                }
            });
        }
        AlertDialog dlg = builder.create();
        dlg.setCanceledOnTouchOutside(false);
        Window window = dlg.getWindow();
        WindowManager.LayoutParams wlp = window.getAttributes();
        wlp.gravity = Gravity.BOTTOM;
        window.setAttributes(wlp);
        dlg.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        dlg.show();
    }

    private void sendSwap2Choice(boolean swap) {
        if (mListener != null) {
            mListener.sendEvent("{\"dsgSwapSeatsTableEvent\":{\"swap\":" + (swap ? "true" : "false") + ",\"silent\":false,\"player\":\"" + me + "\",\"table\":" + table.getId() + ",\"time\":0}}");
        }
    }
    private void sendSwap2Pass() {
        if (mListener != null) {
            mListener.sendEvent("{\"dsgSwap2PassTableEvent\":{\"silent\":false,\"player\":\"" + me + "\",\"table\":" + table.getId() + ",\"time\":0}}");
        }
    }

    public void updateBoard() {
        board.invalidate();
    }

    public void undoMove() {
        table.undoMove();
        if (table.isGo() && table.getGameState().state == State.STARTED && table.isMyTurn(me)) {
            playButton.setVisibility(View.VISIBLE);
            playButton.setText(R.string.pass);
        } else {
            playButton.setVisibility(View.GONE);
        }
        if (table.isGo() && (table.getGameState().goState == GoState.MARKSTONES || table.getGameState().goState == GoState.EVALUATESTONES)) {
            board.setGoTerritoryByPlayer(table.getTerritories());
            board.setGoDeadStonesByPlayer(table.getGoDeadStonesByPlayer());
        } else {
            board.clearGoStructures();
        }
        board.invalidate();
        showGoDialog();
    }

    private void showTablePlayers() {
        PlayersListAdapter listAdapter = new PlayersListAdapter(table.getPlayers(), getString(R.string.table_players), table.getGame());
        listAdapter.setInflater(activity.getLayoutInflater());
        View view = activity.getLayoutInflater().inflate(R.layout.onlineusers_listview, null);
        ExpandableListView listView = view.findViewById(R.id.onlineUsersListView);
        listView.setAdapter(listAdapter);

        AlertDialog.Builder helpBuilder = new AlertDialog.Builder(getContext());
        helpBuilder.setView(view);
        AlertDialog tablePlayers = helpBuilder.create();
        tablePlayers.setCanceledOnTouchOutside(true);
        listView.expandGroup(0);
        listView.setOnGroupClickListener((parent, v, groupPosition, id) -> {
            return true; // This way the expander cannot be collapsed
        });

        tablePlayers.show();
    }

    private void showBootablePlayers() {
        final Map<String, LivePlayer> bootablePlayers = new HashMap<>(table.getPlayers());
        bootablePlayers.remove(me);
        final PlayersListAdapter listAdapter = new PlayersListAdapter(bootablePlayers, getString(R.string.boot_player), table.getGame());
        listAdapter.setInflater(activity.getLayoutInflater());
        View view = activity.getLayoutInflater().inflate(R.layout.onlineusers_listview, null);
        ExpandableListView listView = view.findViewById(R.id.onlineUsersListView);
        listView.setAdapter(listAdapter);

        AlertDialog.Builder helpBuilder = new AlertDialog.Builder(getContext());
        helpBuilder.setView(view);
        final AlertDialog tablePlayers = helpBuilder.create();
        tablePlayers.setCanceledOnTouchOutside(true);
        listView.expandGroup(0);
        listView.setOnGroupClickListener((parent, v, groupPosition, id) -> {
            return true; // This way the expander cannot be collapsed
        });

        tablePlayers.show();

        listView.setOnChildClickListener((parent, v, groupPosition, childPosition, id) -> {
            LivePlayer player = listAdapter.playersArray.get(childPosition);
            bootPlayer(player.getName());
            tablePlayers.dismiss();
            return true;
        });

    }

    private void bootPlayer(String player) {
        if (mListener != null) {
            mListener.sendEvent("{\"dsgBootTableEvent\":{\"toBoot\":\"" + player + "\",\"player\":\"" + me + "\",\"table\":" + table.getId() + ",\"time\":0}}");
        }
    }

    private void showInvitePlayers() {
        final Map<String, LivePlayer> invitePlayers = new HashMap<>(activity.tablesAndPlayers.players);
        for (String plr : table.getPlayers().keySet()) {
            invitePlayers.remove(plr);
        }
        for (Table tbl : activity.tablesAndPlayers.tables.values()) {
            if (tbl.getId() == this.table.getId()) {
                continue;
            }
            LivePlayer livePlayer = tbl.getSeats().get(1);
            if (livePlayer != null) {
                invitePlayers.remove(livePlayer.getName());
            }
            livePlayer = tbl.getSeats().get(2);
            if (livePlayer != null) {
                invitePlayers.remove(livePlayer.getName());
            }
        }

        final PlayersListAdapter listAdapter = new PlayersListAdapter(invitePlayers, getString(R.string.invite_player), table.getGame());
        listAdapter.setInflater(activity.getLayoutInflater());
        View view = activity.getLayoutInflater().inflate(R.layout.onlineusers_listview, null);
        ExpandableListView listView = view.findViewById(R.id.onlineUsersListView);
        listView.setAdapter(listAdapter);

        AlertDialog.Builder helpBuilder = new AlertDialog.Builder(getContext());
        helpBuilder.setView(view);
        final AlertDialog tablePlayers = helpBuilder.create();
        tablePlayers.setCanceledOnTouchOutside(true);
        listView.expandGroup(0);
        listView.setOnGroupClickListener((parent, v, groupPosition, id) -> {
            return true; // This way the expander cannot be collapsed
        });

        tablePlayers.show();
        listView.setOnChildClickListener((parent, v, groupPosition, childPosition, id) -> {
            final LivePlayer player = listAdapter.playersArray.get(childPosition);

            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            final EditText invitationText = new EditText(activity);
            invitationText.setHint("(" + getString(R.string.optional_message) + ")");
            invitationText.setInputType(InputType.TYPE_CLASS_TEXT);
            builder.setView(invitationText);
            builder.setTitle(getString(R.string.invite_player_to_table, player.coloredNameString(invitationText.getLineHeight())));
            builder.setPositiveButton(activity.getString(R.string.send), (dialog, which) -> {
                String m_Text = invitationText.getText().toString();
                String event = "{\"dsgInviteTableEvent\":{\"toInvite\":\"" + player.getName() + "\",\"inviteText\":\"" + m_Text + "\",\"player\":\"" + me + "\",\"table\":" + table.getId() + ",\"time\":0}}";
                if (mListener != null) {
                    mListener.sendEvent(event);
                }
            });
//                builder.setNegativeButton(activity.getString(R.string.cancel), new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        dialog.cancel();
//                    }
//                });
            builder.show();


            tablePlayers.dismiss();
            return true;
        });

    }

    public void waitingPlayerReturnTimeUp() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        if (waitForPlayerReturnDialog != null) {
            waitForPlayerReturnDialog.dismiss();
            waitForPlayerReturnDialog = null;
        }

        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(getString(R.string.opponent_unavailable));
        builder.setMessage(getString(R.string.player_not_returned));
        builder.setPositiveButton(getString(R.string.cancel_set_game), (dialog, which) -> sendForceCancelResignTableEvent(true));
        builder.setNeutralButton(getString(R.string.resign), (dialog, which) -> sendResign());
        builder.setNegativeButton(getString(R.string.force_resign), (dialog, which) -> sendForceCancelResignTableEvent(false));
        AlertDialog dlg = builder.create();
        dlg.setCanceledOnTouchOutside(false);
        Window window = dlg.getWindow();
        WindowManager.LayoutParams wlp = window.getAttributes();
        wlp.gravity = Gravity.BOTTOM;
//        wlp.flags &= ~WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        window.setAttributes(wlp);
        dlg.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        dlg.show();

    }

    private void sendForceCancelResignTableEvent(boolean cancel) {
        if (mListener != null) {
            mListener.sendEvent("{\"dsgForceCancelResignTableEvent\":{\"action\":" + (cancel ? 1 : 2) + ",\"player\":\"" + me + "\",\"table\":" + table.getId() + ",\"time\":0}}");
        }
    }
}
