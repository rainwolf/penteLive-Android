package be.submanifold.pentelive.liveGameRoom;

import be.submanifold.pentelive.*;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.CookieManager;
import android.widget.ExpandableListView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
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
    public void onResume() {
        super.onResume();
        MyApplication.activityResumed(this);
    }
    @Override
    protected void onPause() {
        super.onPause();
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
                URL url = new URL("https://www.pente.org/gameServer/activeServers?name2="+PentePlayer.mPlayerName+"&password2="+ PentePlayer.mPassword);

                HttpsURLConnection connection = (HttpsURLConnection)url.openConnection();
                String cookies = CookieManager.getInstance().getCookie("https://www.pente.org/");
                if (cookies != null) {
                    String[] splitCookie = cookies.split(";");
                    String cookieStr = "";
                    for (String item: splitCookie) {
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
                while((line = br.readLine()) != null ) {
                    output.append(line + "\n");
                }
                br.close();

//                System.out.println(output.toString());

                dashboardString = output.toString();

            } catch (IOException e1) {
                e1.printStackTrace();
                return  false;
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
                    String[] splitLine = dashLine.split(" ", 2);
                    if (splitLine.length > 1) {
                        LiveGameRoom room = new LiveGameRoom(splitLine[1], Integer.parseInt(splitLine[0]));
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

}
