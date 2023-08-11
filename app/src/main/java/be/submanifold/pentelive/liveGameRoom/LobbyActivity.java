package be.submanifold.pentelive.liveGameRoom;

import be.submanifold.pentelive.*;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.AsyncTask;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import androidx.appcompat.widget.Toolbar;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

public class LobbyActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        myToolbar.setTitle(getString(R.string.lobby));
        setSupportActionBar(myToolbar);

        ExpandableListView expandableList = (ExpandableListView) findViewById(R.id.list);
        final LobbyListAdapter listAdapter = new LobbyListAdapter();
        expandableList.setAdapter(listAdapter);
        listAdapter.setInflater((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE), this);
        expandableList.expandGroup(0);
        expandableList.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView parent, View v,
                                        int groupPosition, long id) {
                return true; // This way the expander cannot be collapsed
            }
        });
        expandableList.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView expandableListView, View view, int i, int i1, long l) {
                Intent intent = new Intent(LobbyActivity.this, LiveGameRoomActivity.class);
                intent.putExtra("room", listAdapter.rooms.get(i1));
                startActivity(intent);
                return true;
            }
        });
        LoadActiveServersTask loadServersTask = new LoadActiveServersTask(listAdapter);
        loadServersTask.execute();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.lobby_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
//            case R.id.action_settings:
//                // User chose the "Settings" item, show the app settings UI...
//                return true;

            case R.id.broadcast:
                if (PentePlayer.mSubscriber) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    final Spinner gameSpinner = new Spinner(this);
                    final ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                            R.array.live_game_types_array, android.R.layout.simple_spinner_item);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    gameSpinner.setAdapter(adapter);

                    builder.setView(gameSpinner);
                    builder.setTitle(getString(R.string.broadcast));
                    builder.setMessage(getString(R.string.alert_friends_followers));
                    builder.setPositiveButton(getString(R.string.to_followers), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String game = gameSpinner.getSelectedItem().toString();
                            BroadcastTask task = new BroadcastTask(false, game);
                            task.execute();
                        }
                    });
                    builder.setNeutralButton(getString(R.string.to_friends), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String game = gameSpinner.getSelectedItem().toString();
                            BroadcastTask task = new BroadcastTask(true, game);
                            task.execute();
                        }
                    });
//                builder.setNegativeButton(getString(R.string.dismiss), new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        dialog.cancel();
//                    }
//                });
                    builder.show();
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    View infoView = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.db_subscribers_only, null, false);
                    ((TextView) infoView.findViewById(R.id.informationView)).setText(getString(R.string.broadcasting_subscribers_only));
                    infoView.setBackgroundColor(Color.WHITE);
                    builder.setView(infoView);
                    final AlertDialog dlg = builder.create();
                    ((Button) infoView.findViewById(R.id.subscribeButton)).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            dlg.dismiss();
                            String url = "https://www.pente.org/gameServer/subscriptions"; // missing 'http://' will cause crashed
                            Intent intent = new Intent(LobbyActivity.this, WebViewActivity.class);
                            intent.putExtra("url", url);
                            startActivity(intent);
                        }
                    });
                    dlg.show();
//                            ((TextView) policyView.findViewById(R.id.informationView)).setMovementMethod(new ScrollingMovementMethod());

                }
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }


    //This is the handler that will manager to process the broadcast intent
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            // Extract data included in the Intent
            String message = intent.getStringExtra("message");

            Toast.makeText(LobbyActivity.this, message,
                    Toast.LENGTH_SHORT).show();
        }
    };


    @Override
    public void onResume() {
        super.onResume();
        (LobbyActivity.this).registerReceiver(mMessageReceiver, new IntentFilter("unique_name"));
        MyApplication.activityResumed(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        (LobbyActivity.this).unregisterReceiver(mMessageReceiver);
        MyApplication.activityPaused();
    }

    private class LoadActiveServersTask extends AsyncTask<Void, Void, Boolean> {

        private LobbyListAdapter listAdapter;
        String dashboardString;

        LoadActiveServersTask(LobbyListAdapter listAdapter) {
            this.listAdapter = listAdapter;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            try {
                URL url;
                if (PentePlayer.development) {
                    url = new URL("https://development.pente.org/gameServer/mobile/liveServers.jsp?name2=" + PentePlayer.mPlayerName + "&password2=" + PentePlayer.mPassword);
                } else {
                    url = new URL("https://www.pente.org/gameServer/mobile/liveServers.jsp?name2=" + PentePlayer.mPlayerName + "&password2=" + PentePlayer.mPassword);
                }

                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                String cookies = CookieManager.getInstance().getCookie("https://www.pente.org/");
                if (cookies != null) {
                    String[] splitCookie = cookies.split(";");
                    String cookieStr = "";
                    for (String item : splitCookie) {
                        if (item.contains("name2") || item.contains("password2")) {
                            cookieStr += item + ";";
                        }
                    }
                    connection.setRequestProperty("Cookie", cookieStr);
                }
                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    return false;
                }

                StringBuilder output = new StringBuilder();
                BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line = "";
                while ((line = br.readLine()) != null) {
                    output.append(line + "\n");
                }
                br.close();

//                System.out.println(output.toString());

                dashboardString = output.toString();

            } catch (IOException e1) {
                e1.printStackTrace();
                return false;
            }

            // TODO: register the new account here.
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {

            if (success) {
                final List<LiveGameRoom> rooms = new ArrayList<>();

                String[] dashLines = dashboardString.split("\n");
                String dashLine;
                int idx = 0;
                while (idx < dashLines.length) {
                    dashLine = dashLines[idx];
                    String[] splitLine = dashLine.split(":", 2);
                    String[] serverLine = splitLine[0].split(" ", 2);
                    if (serverLine.length > 1) {
                        LiveGameRoom room = new LiveGameRoom(serverLine[1], Integer.parseInt(serverLine[0]));
                        if (splitLine.length > 1) {
                            String[] playersString = splitLine[1].split(";");
                            for (String playerString : playersString) {
                                String[] splitPlayer = playerString.split(",");
                                if (splitPlayer.length < 4) {
                                    continue;
                                }
                                LivePlayer player = new LivePlayer(splitPlayer[0], !"0".equals(splitPlayer[2]), Integer.parseInt(splitPlayer[3]), Integer.parseInt(splitPlayer[2]));
                                room.addPlayer(player);
                            }
                        }
                        rooms.add(room);
                    }
                    idx += 1;
                }
                listAdapter.setRooms(rooms);
                listAdapter.updateList();
                System.out.println(dashboardString);
            }
        }

        @Override
        protected void onCancelled() {
        }
    }


    private class BroadcastTask extends AsyncTask<Void, Void, Boolean> {

        String dashboardString;
        boolean friends = false;
        String game;

        BroadcastTask(boolean friends, String game) {
            this.friends = friends;
            this.game = game;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            try {
                URL url = new URL("https://www.pente.org/gameServer/broadcast?sendTo=" +
                        (friends ? "friends" : "followers") + "&game=" + URLEncoder.encode(game, "UTF-8") +
                        "&mobile="
                        + "&name2=" + PentePlayer.mPlayerName + "&password2=" + PentePlayer.mPassword);

                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                String cookies = CookieManager.getInstance().getCookie("https://www.pente.org/");
                if (cookies != null) {
                    String[] splitCookie = cookies.split(";");
                    String cookieStr = "";
                    for (String item : splitCookie) {
                        if (item.contains("name2") || item.contains("password2")) {
                            cookieStr += item + ";";
                        }
                    }
                    connection.setRequestProperty("Cookie", cookieStr);
                }
                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    return false;
                }

                StringBuilder output = new StringBuilder();
                BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line = "";
                while ((line = br.readLine()) != null) {
                    output.append(line + "\n");
                }
                br.close();

//                System.out.println(output.toString());

                dashboardString = output.toString();

            } catch (IOException e1) {
                e1.printStackTrace();
                return false;
            }

            // TODO: register the new account here.
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {

            if (success) {
                if (dashboardString.contains("Broadcasting to followers or friends is only available to subscribers")) {
                    Toast.makeText(LobbyActivity.this, getString(R.string.broadcasting_subscribers_only),
                            Toast.LENGTH_LONG).show();
                } else if (dashboardString.contains("database error, try again later")) {
                    Toast.makeText(LobbyActivity.this, getString(R.string.database_error),
                            Toast.LENGTH_LONG).show();
                } else if (dashboardString.contains("You can't broadcast more than once per hour")) {
                    Toast.makeText(LobbyActivity.this, getString(R.string.broadcast_once_per_hour),
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(LobbyActivity.this, getString(R.string.broadcast_success),
                            Toast.LENGTH_LONG).show();
                }
            }
        }

        @Override
        protected void onCancelled() {
        }
    }

}
