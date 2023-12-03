package be.submanifold.pentelive;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;

import com.google.android.material.tabs.TabLayout;

import androidx.core.view.MenuItemCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import androidx.appcompat.widget.Toolbar;

import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import be.submanifold.pentelive.liveGameRoom.LivePlayer;

public class SocialActivity extends AppCompatActivity {

    private SocialListAdapter followerListAdapter, followingListAdapter;
    private static final Map<String, Integer> gameNames;

    static {
        gameNames = new HashMap<>();
        gameNames.put("Pente", 1);
        gameNames.put("Keryo-Pente", 3);
        gameNames.put("Gomoku", 5);
        gameNames.put("D-Pente", 7);
        gameNames.put("G-Pente", 9);
        gameNames.put("Poof-Pente", 11);
        gameNames.put("Connect6", 13);
        gameNames.put("Boat-Pente", 15);
        gameNames.put("DK-Pente", 17);
        gameNames.put("Go", 19);
        gameNames.put("Go (9x9)", 21);
        gameNames.put("Go (13x13)", 23);
        gameNames.put("O-Pente", 25);
        gameNames.put("Turn-based Pente", 51);
        gameNames.put("Turn-based Keryo-Pente", 53);
        gameNames.put("Turn-based Gomoku", 55);
        gameNames.put("Turn-based D-Pente", 57);
        gameNames.put("Turn-based G-Pente", 59);
        gameNames.put("Turn-based Poof-Pente", 61);
        gameNames.put("Turn-based Connect6", 63);
        gameNames.put("Turn-based Boat-Pente", 65);
        gameNames.put("Turn-based DK-Pente", 67);
        gameNames.put("Turn-based Go", 69);
        gameNames.put("Turn-based Go (9x9)", 71);
        gameNames.put("Turn-based Go (13x13)", 73);
        gameNames.put("Turn-based O-Pente", 75);
        gameNames.put("Speed Pente", 2);
        gameNames.put("Speed Keryo-Pente", 4);
        gameNames.put("Speed Gomoku", 6);
        gameNames.put("Speed D-Pente", 8);
        gameNames.put("Speed G-Pente", 10);
        gameNames.put("Speed Poof-Pente", 12);
        gameNames.put("Speed Connect6", 14);
        gameNames.put("Speed Boat-Pente", 16);
        gameNames.put("Speed DK-Pente", 18);
        gameNames.put("Speed Go", 20);
        gameNames.put("Speed Go (9x9)", 22);
        gameNames.put("Speed Go (13x13)", 24);
        gameNames.put("Speed O-Pente", 26);
    }

    List<String> gamesArray;

    private String gameStr = "Turn-based Pente";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_social);

        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        myToolbar.setTitle(getString(R.string.social));
        setSupportActionBar(myToolbar);

        ((TabLayout) findViewById(R.id.segments)).addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getText() == getString(R.string.followers)) {
                    findViewById(R.id.followerList).setVisibility(View.VISIBLE);
                    findViewById(R.id.followingList).setVisibility(View.GONE);
                } else {
                    findViewById(R.id.followerList).setVisibility(View.GONE);
                    findViewById(R.id.followingList).setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        ExpandableListView expandableList = findViewById(R.id.followerList);
        followerListAdapter = new SocialListAdapter();
        expandableList.setAdapter(followerListAdapter);
        followerListAdapter.setInflater(getLayoutInflater());
        expandableList.setOnGroupClickListener((expandableListView, view, i, l) -> true);
        expandableList.expandGroup(0);
        expandableList.setOnChildClickListener((expandableListView, view, i, i1, l) -> {

            String url = "https://www.pente.org/gameServer/profile?viewName=" + followerListAdapter.getPlayersArray().get(i1).getName();
            Intent intent = new Intent(SocialActivity.this, WebViewActivity.class);
            intent.putExtra("url", url);
            startActivity(intent);

            return true;
        });

        expandableList = findViewById(R.id.followingList);
        followingListAdapter = new SocialListAdapter();
        followingListAdapter.setFollowing(true);
        expandableList.setAdapter(followingListAdapter);
        followingListAdapter.setInflater(getLayoutInflater());
        expandableList.setOnGroupClickListener((expandableListView, view, i, l) -> true);
        expandableList.expandGroup(0);
        expandableList.setOnChildClickListener((expandableListView, view, i, i1, l) -> {
            String url = "https://www.pente.org/gameServer/profile?viewName=" + followingListAdapter.getPlayersArray().get(i1).getName();
            Intent intent = new Intent(SocialActivity.this, WebViewActivity.class);
            intent.putExtra("url", url);
            startActivity(intent);

            return true;
        });
        expandableList.setOnItemLongClickListener((parent, view, position, id) -> {
            int childPosition = ExpandableListView.getPackedPositionChild(id);

            FollowersingTask task = new FollowersingTask(false, followingListAdapter.getPlayersArray().get(childPosition).getName());
            task.execute();
            return true;
        });

        gameStr = PrefUtils.getFromPrefs(SocialActivity.this, PrefUtils.PREFS_SOCIALGAME_KEY, "Turn-based Pente");
        gamesArray = Arrays.asList(getResources().getStringArray(R.array.all_game_types_array));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.social_menu, menu);

        MenuItem item = menu.findItem(R.id.gameSpinner);
        Spinner spinner = (Spinner) MenuItemCompat.getActionView(item);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.all_game_types_array)) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                ImageView view = new ImageView(getContext());
                view.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_settings));
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(gamesArray.indexOf(gameStr));
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                gameStr = gamesArray.get(i);
//                System.out.println(gameStr);
                PrefUtils.saveToPrefs(SocialActivity.this, PrefUtils.PREFS_SOCIALGAME_KEY, gameStr);

                LoadFollowersingTask loadfollowersingTask = new LoadFollowersingTask();
                loadfollowersingTask.execute();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
//            case R.id.action_settings:
//                // User chose the "Settings" item, show the app settings UI...
//                return true;

            case R.id.follow_user:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                final EditText invitationText = new EditText(this);
                invitationText.setHint("(" + getString(R.string.enter_username) + ")");
                invitationText.setInputType(InputType.TYPE_CLASS_TEXT);
                builder.setView(invitationText);
                builder.setTitle(getString(R.string.follow_player));
                builder.setPositiveButton(getString(R.string.follow), (dialog, which) -> {
                    String m_Text = invitationText.getText().toString();
                    FollowersingTask task = new FollowersingTask(true, m_Text);
                    task.execute();
                });
                builder.setNegativeButton(getString(R.string.dismiss), (dialog, which) -> dialog.cancel());
                builder.show();
                return true;

            case R.id.gameSpinner:


                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }


    //This is the handler that will manager to process the broadcast intent
    private final BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            // Extract data included in the Intent
            String message = intent.getStringExtra("message");

            if (message != null && !message.isEmpty()) {
                Toast.makeText(SocialActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        MyApplication.activityResumed(this);
        (SocialActivity.this).registerReceiver(mMessageReceiver, new IntentFilter("unique_name"));
        LoadFollowersingTask loadfollowersingTask = new LoadFollowersingTask();
        loadfollowersingTask.execute();
    }

    @Override
    protected void onPause() {
        super.onPause();
        MyApplication.activityPaused();
        (SocialActivity.this).unregisterReceiver(mMessageReceiver);
    }

    private class LoadFollowersingTask extends AsyncTask<Void, Void, Boolean> {

        String dashboardString;
        int game = gameNames.get(gameStr);

        LoadFollowersingTask() {
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            try {
                URL url = new URL("https://www.pente.org/gameServer/mobile/followers.jsp?game=" + game + "&name2=" + PentePlayer.mPlayerName + "&password2=" + PentePlayer.mPassword);

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
                List<LivePlayer> followers = new ArrayList<>();
                List<LivePlayer> following = new ArrayList<>();

                String[] dashLines = dashboardString.split("\n");
                for (String dashLine : dashLines) {
                    String[] splitLine = dashLine.split(";");
                    if (splitLine.length > 5) {
                        LivePlayer player = new LivePlayer(splitLine[1], splitLine[2].equals("1"), Integer.parseInt(splitLine[4]), Integer.parseInt(splitLine[3]));
                        player.addRating(game, Integer.parseInt(splitLine[5]));
                        if ("1".equals(splitLine[0])) {
                            following.add(player);
                        } else {
                            followers.add(player);
                        }
                    }
                }
                Collections.sort(followers, (o1, o2) -> o2.getRating(game) - o1.getRating(game));
                Collections.sort(following, (o1, o2) -> o2.getRating(game) - o1.getRating(game));
                followingListAdapter.setGame(game);
                followingListAdapter.setPlayersArray(following);
                followingListAdapter.updateList();
                followerListAdapter.setGame(game);
                followerListAdapter.setPlayersArray(followers);
                followerListAdapter.updateList();
//                System.out.println(dashboardString);
            }
        }

        @Override
        protected void onCancelled() {
        }
    }

    private class FollowersingTask extends AsyncTask<Void, Void, Boolean> {

        String dashboardString;
        boolean follow = false;
        String player;

        FollowersingTask(boolean follow, String player) {
            this.follow = follow;
            this.player = player;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            try {
                URL url = new URL("https://www.pente.org/gameServer/social?" +
                        (follow ? "follow" : "unfollow") + "=" + player
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
                if (dashboardString.contains("non-subscribers. Subscribers can follow an unlimited number of players.")) {
                    Toast.makeText(SocialActivity.this, getString(R.string.limited_followers),
                            Toast.LENGTH_LONG).show();
                } else if (dashboardString.contains("player not found")) {
                    Toast.makeText(SocialActivity.this, getString(R.string.no_such_user),
                            Toast.LENGTH_LONG).show();
                } else if (dashboardString.contains("database error, try again later")) {
                    Toast.makeText(SocialActivity.this, getString(R.string.database_error),
                            Toast.LENGTH_LONG).show();
                } else {
                    LoadFollowersingTask loadfollowersingTask = new LoadFollowersingTask();
                    loadfollowersingTask.execute();
                    if (follow) {
                        Toast.makeText(SocialActivity.this, getString(R.string.now_following, player),
                                Toast.LENGTH_LONG).show();
                    }
                }
            }
        }

        @Override
        protected void onCancelled() {
        }
    }

}
