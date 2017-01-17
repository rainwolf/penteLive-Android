package be.submanifold.pentelive.liveGameRoom;

import be.submanifold.pentelive.*;

import android.content.DialogInterface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.StrictMode;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

public class LiveGameRoomActivity extends AppCompatActivity implements DSGEventListener, LiveGameRoomFragment.OnFragmentInteractionListener, LiveTableFragment.OnFragmentInteractionListener {

    private ClientSocketDSGEventHandler eventHandler;
    private LiveGameRoomActivity self;
    public TablesAndPlayers tablesAndPlayers = new TablesAndPlayers();
    private LiveGameRoomFragment roomFragment = null;
    private LiveGameRoom room;
    private String me = PrefUtils.getFromPrefs(MyApplication.getContext(), PrefUtils.PREFS_LOGIN_USERNAME_KEY, "").toLowerCase();
    final static ExecutorService tpe = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_game_room);
        room = getIntent().getParcelableExtra("room");
//        System.out.println(room.getName());

        self = this;

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.activity_live_game_room, LiveGameRoomFragment.newInstance(room.getName()), "liveGameRoomFragment")
                    .commit();
        }
    }

    public void connectSocket() {
        connectSocket(room.getPort());
    }






    private void connectSocket(final int port) {
        (new Thread() {
            public void run() {
                Socket socket = null;
                try {
                    SocketFactory factory = SSLSocketFactory.getDefault();
//                    socket = factory.createSocket("development.pente.org", port);
                    socket = factory.createSocket("pente.org", port);
                    // because client sends many short messages
                    socket.setTcpNoDelay(true);
                    // timeout after 30 seconds
                    // this should be ok because we receive pings every 15 seconds
                    //socket.setSoTimeout(30 * 1000);

                    eventHandler = new ClientSocketDSGEventHandler(socket);
                    eventHandler.addListener(self);
                    String username = PentePlayer.mPlayerName;
                    String password = PentePlayer.mPassword;
                    eventHandler.eventOccurred("{\"dsgLoginEvent\":{\"player\":\""+username+"\",\"password\":\""+password+"\",\"guest\":false,\"time\":0}}");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        System.out.println("onDestroy");
        (new Thread() { public void run() {
            if (eventHandler != null) {
                eventHandler.destroy();
            }
        } }).start();
        super.onDestroy();
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
                                if (jsonEvent.get("dsgJoinMainRoomEvent") != null) {
                                    tablesAndPlayers.joinMainRoom((Map<String, ?>) jsonEvent.get("dsgJoinMainRoomEvent"));
                                    updateMainRoom();
                                    MediaPlayer.create(LiveGameRoomActivity.this, R.raw.newplayersound).start();
                                } else if (jsonEvent.get("dsgJoinMainRoomErrorEvent") != null) {
                                    (new BootMeTask()).execute((Void) null);
                                } else if (jsonEvent.get("dsgLoginEvent") != null) {
                                    tablesAndPlayers.login((Map<String, ?>) jsonEvent.get("dsgLoginEvent"));
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
                                } else if (jsonEvent.get("dsgJoinTableEvent") != null) {
                                    Map<String, Object> data = (Map<String, Object>) jsonEvent.get("dsgJoinTableEvent");
                                    String player = (String) data.get("player");
                                    int tableId = (Integer) data.get("table");
                                    Table table = tablesAndPlayers.tableJoin(tableId, player);
                                    updateMainRoom();
                                    if (table != null) {
                                        LiveTableFragment tableFragment = LiveTableFragment.newInstance("", "");
                                        tableFragment.setTable(table);
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
                                        FragmentManager.BackStackEntry backEntry = (FragmentManager.BackStackEntry) getSupportFragmentManager().getBackStackEntryAt(index);
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
                                    addTableMessage(tableId, accepted?("* "+getString(R.string.undo_accepted)):("* " +getString(R.string.undo_declined)));
                                } else if (jsonEvent.get("dsgUndoReplyTableEvent") != null) {
                                    Map<String, Object> data = (Map<String, Object>) jsonEvent.get("dsgUndoReplyTableEvent");
                                    undoReply(data);
                                } else if (jsonEvent.get("dsgSwapSeatsTableEvent") != null) {
                                    Map<String, Object> data = (Map<String, Object>) jsonEvent.get("dsgSwapSeatsTableEvent");
                                    swapSeats(data);
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
                                            addTableMessage(tableId, "* "+getString(R.string.accepted_your_invitation, player) + ": \""+responseText+"\"");
                                        } else {
                                            addTableMessage(tableId, "* "+getString(R.string.declined_your_invitation, player) + ": \""+responseText+"\"");
                                            if (ignore) {
                                                addTableMessage(tableId, "* "+getString(R.string.is_ignoring_your_invitation, player) + ": \""+responseText+"\"");
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
                                } else if (jsonEvent.get("dsgSystemMessageTableEvent") != null) {
                                    Map<String, Object> data = (Map<String, Object>) jsonEvent.get("dsgSystemMessageTableEvent");
                                    int tableId = (int) data.get("table");
                                    String message = (String) data.get("message");
                                    addTableMessage(tableId, "* " + message);
                                } else if (jsonEvent.get("dsgWaitingPlayerReturnTimeUpTableEvent") != null) {
                                    Map<String, Object> data = (Map<String, Object>) jsonEvent.get("dsgWaitingPlayerReturnTimeUpTableEvent");
                                    waitingPlayerReturnTimeUp(data);
                                }
                                synchronized (this) {
                                    this.notify();
                                }
                            }
                        };
                        synchronized( uiRunnable ) {
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
        eventHandler.eventOccurred(event);
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
    private void addTableText(Map<String,Object> tableText) {
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
    private void updateTableMove(Map<String,Object> data) {
        final int tableId = (Integer) data.get("table");
        final List<Integer> moves = (List<Integer>) data.get("moves");
        final int move = (int) data.get("move");
        LiveTableFragment fragment = (LiveTableFragment)
                getSupportFragmentManager().findFragmentByTag("liveTable");
        if (fragment != null && fragment.table.getId() == tableId) {
            if (move != 0) {
                fragment.addMove(move);
                MediaPlayer.create(LiveGameRoomActivity.this, R.raw.pentelivenotificationsound).start();
            } else {
                fragment.table.resetBoard();
                for (int m: moves) {
                    fragment.addMove(m);
                }
            }
        } else {
            System.out.println("**************** ah crap");
        }
    }
    private void updateTableGameState(Map<String,Object> data) {
        final int tableId = (int) data.get("table");
        int state = (int) data.get("state");
        final String text = (String) data.get("changeText");
        tablesAndPlayers.updateGameState(tableId, state);
        LiveTableFragment fragment = (LiveTableFragment)
                getSupportFragmentManager().findFragmentByTag("liveTable");
        if (fragment != null && fragment.table.getId() == tableId) {
            if (text != null) {
                fragment.addText("* " + text);
            }
            fragment.gameStateChanged();
        } else {
            System.out.println("**************** ah crap");
        }
    }
    private void updateTableTimer(Map<String,Object> data) {
        final int tableId = (int) data.get("table");
        tablesAndPlayers.updateTableTimer(data);
        updateTable(tableId);
    }
    private void cancelRequest(Map<String,Object> data) {
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
    private void undoRequest(Map<String,Object> data) {
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
    private void swapSeats(Map<String,Object> data) {
        final int tableId = (int) data.get("table");
        final boolean swapped = (boolean) data.get("swap");
        final boolean silent = (boolean) data.get("silent");
        Table table = tablesAndPlayers.tables.get(tableId);
        if (table != null) {
            table.swapSeats(swapped, silent);
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

    private void undoReply(Map<String,Object> data) {
        int tableId = (int) data.get("table");
        boolean accepted = (boolean) data.get("accepted");
        addTableMessage(tableId, accepted?("* "+getString(R.string.undo_accepted)):("* " +getString(R.string.undo_declined)));
        if (accepted) {
            tablesAndPlayers.undoMove(tableId);
        }
        LiveTableFragment fragment = (LiveTableFragment)
                getSupportFragmentManager().findFragmentByTag("liveTable");
        if (fragment != null && fragment.table.getId() == tableId) {
            fragment.updateBoard();
        } else {
            System.out.println("**************** ah crap");
        }
    }
    private void receivedInvitation(Map<String,Object> data) {
        MediaPlayer.create(LiveGameRoomActivity.this, R.raw.invitesound).start();
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
            TextView playerText = (TextView) view.findViewById(R.id.playerText);
            LivePlayer plr = tablesAndPlayers.players.get(player);
            SpannableStringBuilder sb = plr.coloredNameString(playerText.getLineHeight());
            sb.append(" - ").append(plr.coloredRatingSquare(plr.getRating(tablesAndPlayers.tables.get(tableId).getGame())));
            sb.append(" ").append(plr.getRating(tablesAndPlayers.tables.get(tableId).getGame())+"");
            playerText.setText(sb);
            String gameStr = tablesAndPlayers.tables.get(tableId).getGameName();
            TextView tableText = (TextView) view.findViewById(R.id.tableText);
            tableText.setText(getString(R.string.invites_you, gameStr));

            TextView invitationText = (TextView) view.findViewById(R.id.invitationText);
            invitationText.setText(getString(R.string.message) + ": \"" + inviteText+"\"");
            final EditText replyText = (EditText) view.findViewById(R.id.replyText);
            replyText.setInputType(InputType.TYPE_CLASS_TEXT);

            builder.setView(view);
            builder.setPositiveButton(getString(R.string.accept), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    replyInvitation(player, replyText.getText().toString(), tableId, true, false);
                    LiveTableFragment fragment = (LiveTableFragment)
                            getSupportFragmentManager().findFragmentByTag("liveTable");
                    if (fragment != null) {
                        sendEvent("{\"dsgExitTableEvent\":{\"forced\":false,\"table\":"+ fragment.table.getId() + ",\"booted\":false,\"time\":0}}");
                    } else {
                        System.out.println("**************** ah crap");
                    }
                    sendEvent("{\"dsgJoinTableEvent\":{\"table\":"+tableId+",\"time\":0}}");
                }
            });
            builder.setNeutralButton(getString(R.string.decline), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    replyInvitation(player, replyText.getText().toString(), tableId, false, false);
                }
            });
            builder.setNegativeButton(getString(R.string.ignore_invitations), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    replyInvitation(player, replyText.getText().toString(), tableId, false, true);
                }
            });
            AlertDialog dlg = builder.create();
            dlg.setCanceledOnTouchOutside(false);
            dlg.show();
        }
    }
    private void replyInvitation(String player, String replyText, int tableId, boolean accept, boolean ignore) {
        sendEvent("{\"dsgInviteResponseTableEvent\":{\"toPlayer\":\""+player+"\",\"responseText\":\""+replyText+"\",\"accept\":"+(accept?"true":"false")+",\"ignore\":"+(ignore?"true":"false")+",\"table\":"+tableId+",\"time\":0}}");
    }
    private void waitingPlayerReturnTimeUp(Map<String,Object> data) {
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

            if(json != JSONObject.NULL) {
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
        while(keysItr.hasNext()) {
            String key = keysItr.next();
            Object value = object.get(key);

            if(value instanceof JSONArray) {
                value = toList((JSONArray) value);
            }

            else if(value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            }
            map.put(key, value);
        }
        return map;
    }

    private List<Object> toList(JSONArray array) throws JSONException {
        List<Object> list = new ArrayList<Object>();
        for(int i = 0; i < array.length(); i++) {
            Object value = array.get(i);
            if(value instanceof JSONArray) {
                value = toList((JSONArray) value);
            }

            else if(value instanceof JSONObject) {
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
            // TODO: attempt authentication against a network service.

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
                return  false;
            }
            // TODO: register the new account here.
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {

            if (success) {
                eventHandler.eventOccurred("{\"dsgLoginEvent\":{\"player\":\""+storedUserName+"\",\"password\":\""+storedPassword+"\",\"guest\":false,\"time\":0}}");
            } else {
                finish();
            }
        }

        @Override
        protected void onCancelled() {
        }
    }

}
