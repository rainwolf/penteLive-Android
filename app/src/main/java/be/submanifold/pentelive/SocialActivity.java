package be.submanifold.pentelive;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import be.submanifold.pentelive.liveGameRoom.LivePlayer;

public class SocialActivity extends AppCompatActivity {

    private SocialListAdapter followerListAdapter, followingListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_social);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        myToolbar.setTitle(getString(R.string.social));
        setSupportActionBar(myToolbar);

        ((TabLayout) findViewById(R.id.segments)).addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getText() == getString(R.string.followers)) {
                    ((ExpandableListView) findViewById(R.id.followerList)).setVisibility(View.VISIBLE);
                    ((ExpandableListView) findViewById(R.id.followingList)).setVisibility(View.GONE);
                } else {
                    ((ExpandableListView) findViewById(R.id.followerList)).setVisibility(View.GONE);
                    ((ExpandableListView) findViewById(R.id.followingList)).setVisibility(View.VISIBLE);
                }
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        ExpandableListView expandableList = (ExpandableListView) findViewById(R.id.followerList);
        followerListAdapter = new SocialListAdapter();
        expandableList.setAdapter(followerListAdapter);
        followerListAdapter.setInflater(getLayoutInflater());
        expandableList.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView expandableListView, View view, int i, long l) {
                return true;
            }
        });
        expandableList.expandGroup(0);
        expandableList.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView expandableListView, View view, int i, int i1, long l) {

                String url = "https://www.pente.org/gameServer/profile?viewName="+followerListAdapter.getPlayersArray().get(i1).getName();
                Intent intent = new Intent(SocialActivity.this, WebViewActivity.class);
                intent.putExtra("url", url);
                startActivity(intent);

                return true;
            }
        });

        expandableList = (ExpandableListView) findViewById(R.id.followingList);
        followingListAdapter = new SocialListAdapter();
        followingListAdapter.setFollowing(true);
        expandableList.setAdapter(followingListAdapter);
        followingListAdapter.setInflater(getLayoutInflater());
        expandableList.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView expandableListView, View view, int i, long l) {
                return true;
            }
        });
        expandableList.expandGroup(0);
        expandableList.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView expandableListView, View view, int i, int i1, long l) {
                String url = "https://www.pente.org/gameServer/profile?viewName="+followingListAdapter.getPlayersArray().get(i1).getName();
                Intent intent = new Intent(SocialActivity.this, WebViewActivity.class);
                intent.putExtra("url", url);
                startActivity(intent);

                return true;
            }
        });
        expandableList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                int childPosition = ExpandableListView.getPackedPositionChild(id);

                FollowersingTask task = new FollowersingTask(false, followingListAdapter.getPlayersArray().get(childPosition).getName());
                task.execute();
                return true;
            }
        });




        if (PentePlayer.mShowAds) {
            ((AdView) findViewById(R.id.adView)).loadAd(new AdRequest.Builder().build());
        } else {
            findViewById(R.id.adView).setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.social_menu, menu);
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
                invitationText.setHint("("+getString(R.string.enter_username)+")");
                invitationText.setInputType(InputType.TYPE_CLASS_TEXT);
                builder.setView(invitationText);
                builder.setTitle(getString(R.string.follow_player));
                builder.setPositiveButton(getString(R.string.follow), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String m_Text = invitationText.getText().toString();
                        FollowersingTask task = new FollowersingTask(true, m_Text);
                        task.execute();
                    }
                });
                builder.setNegativeButton(getString(R.string.dismiss), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                builder.show();
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

            Toast.makeText(SocialActivity.this, message,
                    Toast.LENGTH_SHORT).show();
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

        LoadFollowersingTask() {
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            try {
                URL url = new URL("https://www.pente.org/gameServer/mobile/followers.jsp?name2="+PentePlayer.mPlayerName+"&password2="+ PentePlayer.mPassword);

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
                List<LivePlayer> followers = new ArrayList<>();
                List<LivePlayer> following = new ArrayList<>();

                String[] dashLines = dashboardString.split("\n");
                for (String dashLine: dashLines) {
                    String[] splitLine = dashLine.split(";");
                    if (splitLine.length > 1) {
                        LivePlayer player = new LivePlayer(splitLine[1], splitLine[2].equals("1"), Integer.parseInt(splitLine[4]), Integer.parseInt(splitLine[3]));
                        if ("1".equals(splitLine[0])) {
                            following.add(player);
                        } else {
                            followers.add(player);
                        }
                    }
                }
                followingListAdapter.setPlayersArray(following);
                followingListAdapter.updateList();
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
                URL url = new URL("https://www.pente.org/gameServer/social?"+
                        (follow?"follow":"unfollow")+"="+player
                        +"&name2="+PentePlayer.mPlayerName+"&password2="+ PentePlayer.mPassword);

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
