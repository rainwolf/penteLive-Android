package be.submanifold.pentelive.liveGameRoom;

import static be.submanifold.pentelive.PentePlayer.development;

import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.pente.gameServer.event.ClientSocketDSGEventHandler;
import org.pente.gameServer.event.DSGEventListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import be.submanifold.pentelive.MyApplication;
import be.submanifold.pentelive.PentePlayer;
import be.submanifold.pentelive.PrefUtils;
import be.submanifold.pentelive.R;

public class LiveGameRoomActivity extends AppCompatActivity implements DSGEventListener, LiveGameRoomFragment.OnFragmentInteractionListener, LiveTableFragment.OnFragmentInteractionListener {

    private volatile ClientSocketDSGEventHandler eventHandler;
    private LiveGameRoomActivity self;
    public TablesAndPlayers tablesAndPlayers = new TablesAndPlayers();
    private final LiveGameRoomFragment roomFragment = null;
    private LiveGameRoom room;

    private String me = PrefUtils.getFromPrefs(MyApplication.getContext(), PrefUtils.PREFS_LOGIN_USERNAME_KEY, "guest").toLowerCase();
    private boolean isArena = false;

    public String getMe() {
        return me;
    }

    final static ExecutorService tpe = Executors.newSingleThreadExecutor();
    private boolean silent;

    private static final int NEW_INVITE_SOUND = 0;
    private static final int NEW_PLAYER_SOUND = 1;
    private static final int NEW_MOVE_SOUND = 2;
    private MediaPlayer mediaPlayer;

    final TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[]{};
                }
            }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_game_room);
        room = getIntent().getParcelableExtra("room");
        isArena = room != null && room.getName() != null
                && room.getName().toLowerCase().contains("arena");
//        System.out.println(room.getName());

        self = this;
        silent = PrefUtils.getBooleanFromPrefs(LiveGameRoomActivity.this, PrefUtils.PREFS_INAPPSOUNDSOFF_KEY, false);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.activity_live_game_room, LiveGameRoomFragment.newInstance(room.getName()), "liveGameRoomFragment")
                    .commit();
        }
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


    public void connectSocket() {
        connectSocket(room.getPort());
    }


    private void connectSocket(final int port) {
        (new Thread() {
            public void run() {
                Socket socket = null;
                try {
                    SocketFactory factory;
                    if (development) {
                        final SSLContext sslContext = SSLContext.getInstance("SSL");
                        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
                        factory = sslContext.getSocketFactory();
                        socket = factory.createSocket("10.0.2.2", port);
                    } else {
                        factory = SSLSocketFactory.getDefault();
                        socket = factory.createSocket("pente.org", port);
                    }
                    // because client sends many short messages
                    socket.setTcpNoDelay(true);
                    // timeout after 30 seconds
                    // this should be ok because we receive pings every 15 seconds
                    //socket.setSoTimeout(30 * 1000);

                    eventHandler = new ClientSocketDSGEventHandler(socket);
                    eventHandler.addListener(self);
                    String username = PentePlayer.mPlayerName;
                    String password = PentePlayer.mPassword;
                    if (self.me.startsWith("guest")) {
                        eventHandler.eventOccurred("{\"dsgLoginEvent\":{\"guest\":true,\"time\":0}}");
                    } else {
                        eventHandler.eventOccurred("{\"dsgLoginEvent\":{\"player\":\"" + username + "\",\"password\":\"" + password + "\",\"guest\":false,\"time\":0}}");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                } catch (KeyManagementException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        System.out.println("onDestroy");
        (new Thread() {
            public void run() {
                if (eventHandler != null) {
                    eventHandler.destroy();
                }
            }
        }).start();
        super.onDestroy();
    }

    private void playSound(int sound) {
        if (!silent) {
            Uri soundUri;
            switch (sound) {
                case NEW_INVITE_SOUND:
                    soundUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.invitesound);
                    break;
                case NEW_PLAYER_SOUND:
                    soundUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.newplayersound);
                    break;
                case NEW_MOVE_SOUND:
                    soundUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.pentelivenotificationsound);
                    break;
                default:
                    soundUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.pentelivenotificationsound);
                    break;
            }
            if (mediaPlayer == null) {
                mediaPlayer = new MediaPlayer();
            }
//            if (mediaPlayer.isPlaying()) {
            mediaPlayer.reset();
//            }
            try {
                mediaPlayer.setDataSource(getApplicationContext(), soundUri);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    AudioAttributes att = new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build();
                    mediaPlayer.setAudioAttributes(att);
                } else {
                    mediaPlayer.setAudioStreamType(AudioManager.STREAM_NOTIFICATION);
                }
                mediaPlayer.setOnPreparedListener(mediaPlayer -> mediaPlayer.start());
                mediaPlayer.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void eventOccurred(String dsgEvent) {
        final Map<String, Object> jsonEvent = jsonToMap(dsgEvent);
        if (jsonEvent != null) {
            if (jsonEvent.get("dsgPingEvent") != null) {
                eventHandler.eventOccurred(dsgEvent);
            } else {
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        Runnable uiRunnable = new Runnable() {
                            @Override
                            public void run() {
                                if (development) {
                                    System.out.println("jsonEvent: " + jsonEvent);
                                }
                                if (jsonEvent.get("dsgJoinMainRoomEvent") != null) {
                                    tablesAndPlayers.joinMainRoom((Map<String, ?>) jsonEvent.get("dsgJoinMainRoomEvent"));
                                    updateMainRoom();
                                    // In an arena room, don't play the new-player sound when other players
                                    // join the room. It plays instead when you receive a join request and
                                    // when a player joins your arena table.
                                    if (!isArena) {
                                        playSound(NEW_PLAYER_SOUND);
                                    }
                                } else if (jsonEvent.get("dsgJoinMainRoomErrorEvent") != null) {
                                    (new BootMeTask()).execute((Void) null);
                                } else if (jsonEvent.get("dsgLoginEvent") != null) {
                                    tablesAndPlayers.login((Map<String, ?>) jsonEvent.get("dsgLoginEvent"));
                                    self.me = tablesAndPlayers.getMe();
                                    updateMainRoom();
                                } else if (jsonEvent.get("dsgTextMainRoomEvent") != null) {
                                    tablesAndPlayers.addMainRoomText((Map<String, ?>) jsonEvent.get("dsgTextMainRoomEvent"));
                                    updateMainRoom();
                                } else if (jsonEvent.get("dsgExitMainRoomEvent") != null) {
                                    tablesAndPlayers.exitMainRoom((Map<String, ?>) jsonEvent.get("dsgExitMainRoomEvent"));
                                    updateMainRoom();
                                } else if (jsonEvent.get("dsgUpdatePlayerDataEvent") != null) {
                                    tablesAndPlayers.updatePlayerData((Map<String, ?>) jsonEvent.get("dsgUpdatePlayerDataEvent"));
                                    updateMainRoom();
                                } else if (jsonEvent.get("dsgChangeStateTableEvent") != null) {
                                    int tableId = tablesAndPlayers.changeTableState((Map<String, Object>) jsonEvent.get("dsgChangeStateTableEvent"));
                                    updateMainRoom();
                                    updateTable(tableId);
                                } else if (jsonEvent.get("dsgArenaRequestJoinTableEvent") != null) {
                                    Map<String, Object> data =
                                            (Map<String, Object>) jsonEvent.get("dsgArenaRequestJoinTableEvent");
                                    String player = (String) data.get("player");
                                    int tableId = (Integer) data.get("table");
                                    LiveTableFragment fragment = (LiveTableFragment)
                                            getSupportFragmentManager().findFragmentByTag("liveTable");
                                    if (fragment != null && fragment.table != null
                                            && fragment.table.getId() == tableId) {
                                        // Play the new-player sound when you receive a join request for your arena table.
                                        playSound(NEW_PLAYER_SOUND);
                                        fragment.arenaTableRequestJoinEvent(player);
                                    }
                                } else if (jsonEvent.get("dsgJoinTableEvent") != null) {
                                    Map<String, Object> data = (Map<String, Object>) jsonEvent.get("dsgJoinTableEvent");
                                    String player = (String) data.get("player");
                                    int tableId = (Integer) data.get("table");
                                    Table table = tablesAndPlayers.tableJoin(tableId, player);
                                    updateMainRoom();
                                    LiveTableFragment arenaTable = (LiveTableFragment)
                                            getSupportFragmentManager().findFragmentByTag("liveTable");
                                    if (arenaTable != null && arenaTable.isArenaTable
                                            && arenaTable.table != null
                                            && arenaTable.table.getId() == tableId
                                            && arenaTable.table.getPlayers().size() >= 2) {
                                        // Play the new-player sound when a player joins your arena table.
                                        playSound(NEW_PLAYER_SOUND);
                                        arenaTable.dismissArenaJoinRequest();
                                    }
                                    if (table != null) {
                                        LiveTableFragment tableFragment = LiveTableFragment.newInstance("", "");
                                        tableFragment.setTable(table);
                                        tableFragment.setArena(isArena);
                                        getSupportFragmentManager()
                                                .beginTransaction()
                                                .add(R.id.activity_live_game_room, tableFragment, "liveTable").addToBackStack("liveTable")
                                                .commit();
                                    } else {
                                        addTableMessage(tableId, getString(R.string.join_table, player));
                                    }
                                } else if (jsonEvent.get("dsgOwnerTableEvent") != null) {
                                    Map<String, Object> data = (Map<String, Object>) jsonEvent.get("dsgOwnerTableEvent");
                                    String player = (String) data.get("player");
                                    int tableId = (Integer) data.get("table");
                                    tablesAndPlayers.tableOwner(tableId, player);
                                    addTableMessage(tableId, getString(R.string.owner, player));
                                } else if (jsonEvent.get("dsgSitTableEvent") != null) {
                                    int tableId = tablesAndPlayers.tableSit((Map<String, Object>) jsonEvent.get("dsgSitTableEvent"));
                                    updateMainRoom();
                                    updateTable(tableId);
                                } else if (jsonEvent.get("dsgStandTableEvent") != null) {
                                    int tableId = tablesAndPlayers.tableStand((Map<String, Object>) jsonEvent.get("dsgStandTableEvent"));
                                    updateMainRoom();
                                    updateTable(tableId);
                                } else if (jsonEvent.get("dsgTextTableEvent") != null) {
                                    addTableText((Map<String, Object>) jsonEvent.get("dsgTextTableEvent"));
                                } else if (jsonEvent.get("dsgExitTableEvent") != null) {
                                    Map<String, Object> data = (Map<String, Object>) jsonEvent.get("dsgExitTableEvent");
                                    String player = (String) data.get("player");
                                    int tableId = (Integer) data.get("table");
                                    if (tablesAndPlayers.tableExit(tableId, player)) {
                                        int index = getSupportFragmentManager().getBackStackEntryCount() - 1;
                                        FragmentManager.BackStackEntry backEntry = getSupportFragmentManager().getBackStackEntryAt(index);
                                        String tag = backEntry.getName();
                                        if ("liveTable".equals(tag)) {
                                            getSupportFragmentManager().popBackStack();
                                        }
                                    } else {
                                        addTableMessage(tableId, getString(R.string.leave_table, player));
                                    }
                                    updateMainRoom();
                                } else if (jsonEvent.get("dsgMoveTableEvent") != null) {
                                    updateTableMove((Map<String, Object>) jsonEvent.get("dsgMoveTableEvent"));
                                } else if (jsonEvent.get("dsgGameStateTableEvent") != null) {
                                    updateTableGameState((Map<String, Object>) jsonEvent.get("dsgGameStateTableEvent"));
                                } else if (jsonEvent.get("dsgTimerChangeTableEvent") != null) {
                                    updateTableTimer((Map<String, Object>) jsonEvent.get("dsgTimerChangeTableEvent"));
                                } else if (jsonEvent.get("dsgCancelRequestTableEvent") != null) {
                                    cancelRequest((Map<String, Object>) jsonEvent.get("dsgCancelRequestTableEvent"));
                                } else if (jsonEvent.get("dsgUndoRequestTableEvent") != null) {
                                    undoRequest((Map<String, Object>) jsonEvent.get("dsgUndoRequestTableEvent"));
                                } else if (jsonEvent.get("dsgUndoRequestTableEvent") != null) {
                                    Map<String, Object> data = (Map<String, Object>) jsonEvent.get("dsgUndoRequestTableEvent");
                                    int tableId = (int) data.get("table");
                                    boolean accepted = (boolean) data.get("accepted");
                                    addTableMessage(tableId, accepted ? ("* " + getString(R.string.undo_accepted)) : ("* " + getString(R.string.undo_declined)));
                                } else if (jsonEvent.get("dsgUndoReplyTableEvent") != null) {
                                    Map<String, Object> data = (Map<String, Object>) jsonEvent.get("dsgUndoReplyTableEvent");
                                    undoReply(data);
                                } else if (jsonEvent.get("dsgSwapSeatsTableEvent") != null) {
                                    Map<String, Object> data = (Map<String, Object>) jsonEvent.get("dsgSwapSeatsTableEvent");
                                    swapSeats(data);
                                } else if (jsonEvent.get("dsgRenjuTaraguchiSwapTableEvent") != null) {
                                    // Renju Taraguchi-10 SWAP decision echo. Runs on the UI thread (this
                                    // dispatch chain is posted to the UI thread before dispatch), so we
                                    // mutate renjuState and refresh the dialog/board directly.
                                    Map<String, Object> p = (Map<String, Object>) jsonEvent.get("dsgRenjuTaraguchiSwapTableEvent");
                                    int tbl = ((Number) p.get("table")).intValue();
                                    boolean swap = Boolean.TRUE.equals(p.get("swap"));
                                    Table t = tablesAndPlayers.tables.get(tbl);
                                    if (t != null && t.isRenju()) {
                                        t.getGameState().renjuState.applySwap(swap, t.getMoves().size());
                                        LiveTableFragment fragment = (LiveTableFragment)
                                                getSupportFragmentManager().findFragmentByTag("liveTable");
                                        if (fragment != null) {
                                            fragment.onRenjuDecisionEcho(tbl); // dismiss/refresh dialog + board + arm SELECTION (self-wraps in runOnUiThread)
                                        }
                                    }
                                } else if (jsonEvent.get("dsgRenjuTaraguchiOffer10TableEvent") != null) {
                                    // Renju Taraguchi-10 OFFER-10 decision echo (Branch B 10-stone offer).
                                    Map<String, Object> p = (Map<String, Object>) jsonEvent.get("dsgRenjuTaraguchiOffer10TableEvent");
                                    int tbl = ((Number) p.get("table")).intValue();
                                    int[] moves = toIntArray((List<?>) p.get("moves"));
                                    Table t = tablesAndPlayers.tables.get(tbl);
                                    if (t != null && t.isRenju()) {
                                        t.getGameState().renjuState.applyOffer10(moves);
                                        LiveTableFragment fragment = (LiveTableFragment)
                                                getSupportFragmentManager().findFragmentByTag("liveTable");
                                        if (fragment != null) {
                                            fragment.onRenjuDecisionEcho(tbl);
                                        }
                                    }
                                } else if (jsonEvent.get("dsgRenjuTaraguchi10Select1TableEvent") != null) {
                                    // Renju Taraguchi-10 SELECT-1 decision echo (pick one of the 10 offered).
                                    Map<String, Object> p = (Map<String, Object>) jsonEvent.get("dsgRenjuTaraguchi10Select1TableEvent");
                                    int tbl = ((Number) p.get("table")).intValue();
                                    int move = ((Number) p.get("move")).intValue();
                                    Table t = tablesAndPlayers.tables.get(tbl);
                                    if (t != null && t.isRenju()) {
                                        t.getGameState().renjuState.applySelect1(move);
                                        LiveTableFragment fragment = (LiveTableFragment)
                                                getSupportFragmentManager().findFragmentByTag("liveTable");
                                        if (fragment != null) {
                                            fragment.onRenjuDecisionEcho(tbl);
                                        }
                                    }
                                } else if (jsonEvent.get("dsgSwap2PassTableEvent") != null) {
                                    Map<String, Object> data = (Map<String, Object>) jsonEvent.get("dsgSwap2PassTableEvent");
                                    swap2Pass(data);
                                } else if (jsonEvent.get("dsgBootTableEvent") != null) {
                                    Map<String, Object> data = (Map<String, Object>) jsonEvent.get("dsgBootTableEvent");
                                    String player = (String) data.get("player");
                                    String toBoot = (String) data.get("toBoot");
                                    int tableId = (int) data.get("table");
                                    if (toBoot.equals(me)) {
                                        Toast.makeText(LiveGameRoomActivity.this, getString(R.string.not_player),
                                                Toast.LENGTH_LONG).show();

                                    } else {
                                        addTableMessage(tableId, "* " + getString(R.string.player_booted_by, toBoot, player));
                                    }
                                } else if (jsonEvent.get("dsgInviteResponseTableEvent") != null) {
                                    Map<String, Object> data = (Map<String, Object>) jsonEvent.get("dsgInviteResponseTableEvent");
                                    int tableId = (int) data.get("table");
                                    String player = (String) data.get("player");
                                    String toPlayer = (String) data.get("toPlayer");
                                    String responseText = (String) data.get("responseText");
                                    boolean accepted = (boolean) data.get("accept");
                                    boolean ignore = (boolean) data.get("ignore");
                                    if (toPlayer.equals(me)) {
                                        if (accepted) {
                                            addTableMessage(tableId, "* " + getString(R.string.accepted_your_invitation, player) + ": \"" + responseText + "\"");
                                        } else {
                                            addTableMessage(tableId, "* " + getString(R.string.declined_your_invitation, player) + ": \"" + responseText + "\"");
                                            if (ignore) {
                                                addTableMessage(tableId, "* " + getString(R.string.is_ignoring_your_invitation, player) + ": \"" + responseText + "\"");
                                            }
                                        }
                                    }
                                } else if (jsonEvent.get("dsgInviteTableEvent") != null) {
                                    Map<String, Object> data = (Map<String, Object>) jsonEvent.get("dsgInviteTableEvent");
                                    receivedInvitation(data);
                                } else if (jsonEvent.get("dsgJoinTableErrorEvent") != null) {
                                    Map<String, Object> data = (Map<String, Object>) jsonEvent.get("dsgJoinTableErrorEvent");
                                    int error = (int) data.get("error");
                                    if (error == 22) {
                                        Toast.makeText(LiveGameRoomActivity.this, getString(R.string.were_booted),
                                                Toast.LENGTH_LONG).show();
                                    }
                                } else if (jsonEvent.get("dsgMoveTableErrorEvent") != null) {
                                    // The server rejects a renju decision/move and echoes the error only to
                                    // the sender. Nothing else clears the board's RENJU_PENDING state, so for
                                    // renju tables ask the fragment to recover (state is unchanged on reject).
                                    Map<String, Object> p = (Map<String, Object>) jsonEvent.get("dsgMoveTableErrorEvent");
                                    int tbl = ((Number) p.get("table")).intValue();
                                    Table t = tablesAndPlayers.tables.get(tbl);
                                    if (t != null && t.isRenju()) {
                                        LiveTableFragment fragment = (LiveTableFragment)
                                                getSupportFragmentManager().findFragmentByTag("liveTable");
                                        if (fragment != null) {
                                            fragment.onRenjuMoveError(tbl);
                                        }
                                    }
                                } else if (jsonEvent.get("dsgSystemMessageTableEvent") != null) {
                                    Map<String, Object> data = (Map<String, Object>) jsonEvent.get("dsgSystemMessageTableEvent");
                                    int tableId = (int) data.get("table");
                                    String message = (String) data.get("message");
                                    addTableMessage(tableId, "* " + message);
                                } else if (jsonEvent.get("dsgWaitingPlayerReturnTimeUpTableEvent") != null) {
                                    Map<String, Object> data = (Map<String, Object>) jsonEvent.get("dsgWaitingPlayerReturnTimeUpTableEvent");
                                    waitingPlayerReturnTimeUp(data);
                                } else if (jsonEvent.get("dsgRejectGoStateEvent") != null) {
                                    Map<String, Object> data = (Map<String, Object>) jsonEvent.get("dsgRejectGoStateEvent");
                                    rejectGoState(data);
                                }
                                synchronized (this) {
                                    this.notify();
                                }
                            }
                        };
                        synchronized (uiRunnable) {
                            runOnUiThread(uiRunnable);
                            try {
                                uiRunnable.wait(); // unlocks myRunable while waiting
                            } catch (InterruptedException e) {
                                System.out.println("InterruptedException " + e);
                            }
                        }
                    }
                };
                tpe.submit(runnable);
//                }
            }
        }
    }

    @Override
    public void sendEvent(String event) {
        // Socket connects on a background thread; ignore sends issued before it is ready.
        ClientSocketDSGEventHandler handler = eventHandler;
        if (handler != null) {
            handler.eventOccurred(event);
        }
    }


    private void updateMainRoom() {
        LiveGameRoomFragment fragment = (LiveGameRoomFragment)
                getSupportFragmentManager().findFragmentByTag("liveGameRoomFragment");
        if (fragment != null) {
            fragment.updateMainRoom();
        }
    }

    private void updateTable(final int tableId) {
        LiveTableFragment fragment = (LiveTableFragment)
                getSupportFragmentManager().findFragmentByTag("liveTable");
        if (fragment != null && fragment.table.getId() == tableId) {
            fragment.updateTable();
        }
    }

    private void addTableText(Map<String, Object> tableText) {
        final String player = (String) tableText.get("player");
        final String text = (String) tableText.get("text");
        final int tableId = (int) tableText.get("table");
        LiveTableFragment fragment = (LiveTableFragment)
                getSupportFragmentManager().findFragmentByTag("liveTable");
        if (fragment != null && fragment.table.getId() == tableId) {
            fragment.addText(player + ": " + text);
        } else {
            System.out.println("**************** ah crap");
        }
    }

    private void addTableMessage(final int tableId, final String text) {
        LiveTableFragment fragment = (LiveTableFragment)
                getSupportFragmentManager().findFragmentByTag("liveTable");
        if (fragment != null && fragment.table.getId() == tableId) {
            fragment.addText(text);
        } else {
            System.out.println("**************** ah crap");
        }
    }

    private void updateTableMove(Map<String, Object> data) {
        final int tableId = (Integer) data.get("table");
        final List<Integer> moves = (List<Integer>) data.get("moves");
        final int move = (int) data.get("move");
        LiveTableFragment fragment = (LiveTableFragment)
                getSupportFragmentManager().findFragmentByTag("liveTable");
        if (fragment != null && fragment.table.getId() == tableId) {
            if (move != 0) {
                fragment.addMove(move);
                playSound(NEW_MOVE_SOUND);
            } else {
                // Bulk move replay (rejoin / initial state). For Renju Taraguchi-10 this routes to
                // Table.advanceRenjuAfterMove(true) (Task C1). The server sends the renju decision
                // signal (silent dsgSwapSeatsTableEvent / re-sent dsgRenjuTaraguchiOffer10TableEvent /
                // dsgRenjuTaraguchi10Select1TableEvent) BEFORE this bulk move list, so renjuState is
                // already in the correct pre-state when the bulk replay runs with isRejoin=true.
                fragment.addMoves(moves);
            }
        } else {
            System.out.println("**************** ah crap");
        }
    }

    private void updateTableGameState(Map<String, Object> data) {
        final int tableId = (int) data.get("table");
        int state = (int) data.get("state");
        final String text = (String) data.get("changeText");
        LiveTableFragment fragment = (LiveTableFragment)
                getSupportFragmentManager().findFragmentByTag("liveTable");
        if (fragment != null && fragment.table.getId() == tableId) {
            if (text != null) {
                fragment.addText("* " + text);
            }
            fragment.updateGameState(state);
//            fragment.gameStateChanged();
        } else {
            tablesAndPlayers.updateGameState(tableId, state);
        }
    }

    private void updateTableTimer(Map<String, Object> data) {
        final int tableId = (int) data.get("table");
        tablesAndPlayers.updateTableTimer(data);
        updateTable(tableId);
    }

    private void cancelRequest(Map<String, Object> data) {
        final int tableId = (int) data.get("table");
        final String player = (String) data.get("player");
        addTableMessage(tableId, "* " + getString(R.string.cancellation_requested));
        LiveTableFragment fragment = (LiveTableFragment)
                getSupportFragmentManager().findFragmentByTag("liveTable");
        if (fragment != null && fragment.table.getId() == tableId && !player.equals(me)) {
            fragment.cancelRequest(player);
        } else {
            System.out.println("**************** ah crap");
        }
    }

    private void undoRequest(Map<String, Object> data) {
        final int tableId = (int) data.get("table");
        final String player = (String) data.get("player");
        addTableMessage(tableId, "* " + getString(R.string.undo_requested));
        LiveTableFragment fragment = (LiveTableFragment)
                getSupportFragmentManager().findFragmentByTag("liveTable");
        if (fragment != null && fragment.table.getId() == tableId && !player.equals(me)) {
            fragment.undoRequested(player);
        } else {
            System.out.println("**************** ah crap");
        }
    }

    private void rejectGoState(Map<String, Object> data) {
        final int tableId = (int) data.get("table");
        final String player = (String) data.get("player");
        addTableMessage(tableId, "* " + getString(R.string.reject_go_state, player));
        LiveTableFragment fragment = (LiveTableFragment)
                getSupportFragmentManager().findFragmentByTag("liveTable");
        if (fragment != null && fragment.table.getId() == tableId) {
            fragment.rejectGoState();
        } else {
            System.out.println("**************** ah crap");
        }
    }

    private void swapSeats(Map<String, Object> data) {
        final int tableId = (int) data.get("table");
        final boolean swapped = (boolean) data.get("swap");
        final boolean silent = (boolean) data.get("silent");
        Table table = tablesAndPlayers.tables.get(tableId);
        if (table != null) {
            table.swapSeats(swapped, silent);
            // Renju Taraguchi-10: a swapSeats event also resolves the renju opening window.
            // This covers BOTH a live take-over (non-silent) AND the silent rejoin marker;
            // either way the renju SWAP window must be marked resolved (awaitingSwap=false,
            // swapTaken=true) before the bulk move replay runs advanceRenjuAfterMove(true).
            if (table.isRenju()) {
                table.getGameState().renjuState.applySwapSeats(table.getMoves().size());
                LiveTableFragment fragment = (LiveTableFragment)
                        getSupportFragmentManager().findFragmentByTag("liveTable");
                if (fragment != null) {
                    fragment.onRenjuDecisionEcho(tableId);
                }
            }
        }
        if (swapped && !silent) {
            updateTable(tableId);
        }
        if (!silent) {
            if (swapped) {
                addTableMessage(tableId, "* " + getString(R.string.seats_swapped));
            } else {
                addTableMessage(tableId, "* " + getString(R.string.seats_not_swapped));
            }
        }
    }

    private void swap2Pass(Map<String, Object> data) {
        final int tableId = (int) data.get("table");
        final boolean silent = (boolean) data.get("silent");
        Table table = tablesAndPlayers.tables.get(tableId);
        if (table != null) {
            table.swap2Pass(silent);
        }
    }

    private void undoReply(Map<String, Object> data) {
        int tableId = (int) data.get("table");
        boolean accepted = (boolean) data.get("accepted");
        addTableMessage(tableId, accepted ? ("* " + getString(R.string.undo_accepted)) : ("* " + getString(R.string.undo_declined)));
        if (accepted) {
            LiveTableFragment fragment = (LiveTableFragment)
                    getSupportFragmentManager().findFragmentByTag("liveTable");
            if (fragment != null && fragment.table.getId() == tableId) {
                fragment.undoMove();
            } else {
                System.out.println("**************** ah crap");
            }
        }
    }

    private void receivedInvitation(Map<String, Object> data) {
        playSound(NEW_INVITE_SOUND);
        final int tableId = (int) data.get("table");
        final String player = (String) data.get("player");
        String toInvite = (String) data.get("toInvite");
        String inviteText = (String) data.get("inviteText");
        if (toInvite.equals(me)) {
            LiveTableFragment fragment = (LiveTableFragment)
                    getSupportFragmentManager().findFragmentByTag("liveTable");
            if (fragment != null) {
                Table table = fragment.table;
                if (table.isSeated(me) && table.getGameState().state != State.NOTSTARTED) {
                    replyInvitation(player, "I can 't accept your invitation because I'm currently playing. This is an automated response", tableId, false, false);
                    return;
                }
            } else {
                System.out.println("**************** ah crap");
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(LiveGameRoomActivity.this);
            View view = getLayoutInflater().inflate(R.layout.invitation_response_layout, null);
            TextView playerText = view.findViewById(R.id.playerText);
            LivePlayer plr = tablesAndPlayers.players.get(player);
            SpannableStringBuilder sb = plr.coloredNameString(playerText.getLineHeight());
            sb.append(" - ").append(plr.coloredRatingSquare(plr.getRating(tablesAndPlayers.tables.get(tableId).getGame())));
            sb.append(" ").append(plr.getRating(tablesAndPlayers.tables.get(tableId).getGame()) + "");
            playerText.setText(sb);
            String gameStr = tablesAndPlayers.tables.get(tableId).getGameName();
            TextView tableText = view.findViewById(R.id.tableText);
            tableText.setText(getString(R.string.invites_you, gameStr));

            TextView invitationText = view.findViewById(R.id.invitationText);
            invitationText.setText(getString(R.string.message) + ": \"" + inviteText + "\"");
            final EditText replyText = view.findViewById(R.id.replyText);
            replyText.setInputType(InputType.TYPE_CLASS_TEXT);

            builder.setView(view);
            builder.setPositiveButton(getString(R.string.accept), (dialog, which) -> {
                replyInvitation(player, replyText.getText().toString(), tableId, true, false);
                LiveTableFragment fragment1 = (LiveTableFragment)
                        getSupportFragmentManager().findFragmentByTag("liveTable");
                if (fragment1 != null) {
                    sendEvent("{\"dsgExitTableEvent\":{\"forced\":false,\"table\":" + fragment1.table.getId() + ",\"booted\":false,\"time\":0}}");
                } else {
                    System.out.println("**************** ah crap");
                }
                sendEvent("{\"dsgJoinTableEvent\":{\"table\":" + tableId + ",\"time\":0}}");
            });
            builder.setNeutralButton(getString(R.string.decline), (dialog, which) -> replyInvitation(player, replyText.getText().toString(), tableId, false, false));
            builder.setNegativeButton(getString(R.string.ignore_invitations), (dialog, which) -> replyInvitation(player, replyText.getText().toString(), tableId, false, true));
            AlertDialog dlg = builder.create();
            dlg.setCanceledOnTouchOutside(false);
            dlg.show();
        }
    }

    private void replyInvitation(String player, String replyText, int tableId, boolean accept, boolean ignore) {
        sendEvent("{\"dsgInviteResponseTableEvent\":{\"toPlayer\":\"" + player + "\",\"responseText\":\"" + replyText + "\",\"accept\":" + (accept ? "true" : "false") + ",\"ignore\":" + (ignore ? "true" : "false") + ",\"table\":" + tableId + ",\"time\":0}}");
    }

    private void waitingPlayerReturnTimeUp(Map<String, Object> data) {
        final int tableId = (int) data.get("table");
        final String player = (String) data.get("player");
        LiveTableFragment fragment = (LiveTableFragment)
                getSupportFragmentManager().findFragmentByTag("liveTable");
        if (fragment != null && fragment.table.getId() == tableId && player.equals(me)) {
            fragment.waitingPlayerReturnTimeUp();
        } else {
            System.out.println("**************** ah crap");
        }
    }


    private Map<String, Object> jsonToMap(String jsonStr) {
        try {
            JSONObject json = new JSONObject(jsonStr);
            Map<String, Object> retMap = null;

            if (json != JSONObject.NULL) {
                retMap = toMap(json);
                return retMap;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Map<String, Object> toMap(JSONObject object) throws JSONException {
        Map<String, Object> map = new HashMap<String, Object>();

        Iterator<String> keysItr = object.keys();
        while (keysItr.hasNext()) {
            String key = keysItr.next();
            Object value = object.get(key);

            if (value instanceof JSONArray) {
                value = toList((JSONArray) value);
            } else if (value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            }
            map.put(key, value);
        }
        return map;
    }

    // Converts a JSON-decoded list (org.json yields Number/Integer/Long elements) to an int[].
    private int[] toIntArray(List<?> list) {
        if (list == null) {
            return new int[0];
        }
        int[] arr = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            arr[i] = ((Number) list.get(i)).intValue();
        }
        return arr;
    }

    private List<Object> toList(JSONArray array) throws JSONException {
        List<Object> list = new ArrayList<Object>();
        for (int i = 0; i < array.length(); i++) {
            Object value = array.get(i);
            if (value instanceof JSONArray) {
                value = toList((JSONArray) value);
            } else if (value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            }
            list.add(value);
        }
        return list;
    }

    public class BootMeTask extends AsyncTask<Void, Void, Boolean> {
        String storedUserName = PrefUtils.getFromPrefs(LiveGameRoomActivity.this, PrefUtils.PREFS_LOGIN_USERNAME_KEY, null);
        String storedPassword = PrefUtils.getFromPrefs(LiveGameRoomActivity.this, PrefUtils.PREFS_LOGIN_PASSWORD_KEY, null);

        BootMeTask() {
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            try {
                try {
                    URL url = new URL("https://www.pente.org/gameServer/bootMeMobile.jsp?name2=" + storedUserName + "&password2=" + storedPassword);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    int responseCode = connection.getResponseCode();
                    if (responseCode != 200) {
                        System.out.println("response code for submit was " + responseCode);
                    }

                    StringBuilder output = new StringBuilder();
                    BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String line = "";
                    while ((line = br.readLine()) != null) {
                        output.append(line + "\n");
                    }
                    br.close();
//                        System.out.println("output===============" + "\n" + output.toString());


                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                // Add custom implementation, as needed.

            } catch (Exception e1) {
                e1.printStackTrace();
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {

            if (success) {
                eventHandler.eventOccurred("{\"dsgLoginEvent\":{\"player\":\"" + storedUserName + "\",\"password\":\"" + storedPassword + "\",\"guest\":false,\"time\":0}}");
            } else {
                finish();
            }
        }

        @Override
        protected void onCancelled() {
        }
    }

}
