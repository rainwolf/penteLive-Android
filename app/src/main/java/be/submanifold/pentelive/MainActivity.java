package be.submanifold.pentelive;

import be.submanifold.pentelive.liveGameRoom.LobbyActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;


public class MainActivity extends AppCompatActivity {

    private PentePlayer player;
    private SwipeRefreshLayout swipeRefreshLayout;
    private AdView mAdView;
    private DashboardListAdapter listAdapter;
    private View viewWithOpenButtons = null;
    InterstitialAd mInterstitialAd;
    private PopupWindow popupWindow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        PrefUtils.saveBooleanToPrefs(MainActivity.this, PrefUtils.PREFS_REGISTRATIONSUCCESSFUL_KEY, true);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        myToolbar.setTitle(getString(R.string.home));
        setSupportActionBar(myToolbar);
        this.player = getIntent().getParcelableExtra("pentePlayer");

        if (player.showAds()) {
            mInterstitialAd = new InterstitialAd(this);
            mInterstitialAd.setAdUnitId("ca-app-pub-3326997956703582/8120483448");

            mInterstitialAd.setAdListener(new AdListener() {
                @Override
                public void onAdClosed() {
                    requestNewInterstitial();
                }
            });
            requestNewInterstitial();
        }

        ExpandableListView expandableList = (ExpandableListView) findViewById(R.id.list);
        listAdapter = new DashboardListAdapter(this.player);
        expandableList.setAdapter(listAdapter);
        listAdapter.setInflater((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE), this);


        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefresh);
        swipeRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        refreshPlayer();
                    }
                }
        );
        expandableList.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, final int groupPosition, final int childPosition, long id) {
//                System.out.println("kittycat " + childPosition + " of " + groupPosition );
                if (groupPosition == DashboardListAdapter.KOTHGROUP) {
                    Intent intent = new Intent(getApplicationContext(), KingOfTheHillActivity.class);
                    intent.putExtra("pentePlayer", player);
                    intent.putExtra("kothSummary", player.getHills().get(childPosition));
                    startActivity(intent);
                    return true;
                }
                if (groupPosition == DashboardListAdapter.MESSAGESGROUP) {
                    Intent intent = new Intent(getApplicationContext(), ReplyMessageActivity.class);
                    intent.putExtra("message", player.getMessages().get(childPosition));
                    startActivity(intent);
                    return true;
                }
                if (groupPosition == DashboardListAdapter.TOURNAMENTGROUP) {
                    Intent intent = new Intent(getApplicationContext(), WebViewActivity.class);
                    String url = "google.com";
                    if (player.getTournaments().get(childPosition).getTournamentState().equals("2")) {
                        url = "https://www.pente.org/gameServer/tournaments/status.jsp?eid=" + player.getTournaments().get(childPosition).getTournamentID()+"&name2="+PentePlayer.mPlayerName+"&password2="+ PentePlayer.mPassword;;
//                        url = "https://development.pente.org/gameServer/tournaments/status.jsp?eid=" + player.getTournaments().get(childPosition).getTournamentID();

                    } else if (player.getTournaments().get(childPosition).getTournamentState().equals("1")) {
                        url = "https://www.pente.org/gameServer/tournaments/tournamentConfirm.jsp?eid=" + player.getTournaments().get(childPosition).getTournamentID()+"&name2="+PentePlayer.mPlayerName+"&password2="+ PentePlayer.mPassword;;
//                        url = "https://development.pente.org/gameServer/tournaments/tournamentConfirm.jsp?eid=" + player.getTournaments().get(childPosition).getTournamentID();

                    } else {
                        url = "https://www.pente.org/gameServer/tournaments/statusRound.jsp?eid=" + player.getTournaments().get(childPosition).getTournamentID()
                        + "&round=" + player.getTournaments().get(childPosition).getRound()+"&name2="+PentePlayer.mPlayerName+"&password2="+ PentePlayer.mPassword;;
//                        url = "https://development.pente.org/gameServer/tournaments/statusRound.jsp?eid=" + player.getTournaments().get(childPosition).getTournamentID()
//                        + "&round=" + player.getTournaments().get(childPosition).getRound();

                    }
                    intent.putExtra("url", url);
                    startActivity(intent);
                }
                if (viewWithOpenButtons != null && !viewWithOpenButtons.equals(v)) {
//                    Button button = (Button) findViewById(R.id.acceptButton);

                    viewWithOpenButtons.findViewById(R.id.acceptButton).setVisibility(View.GONE);
                    viewWithOpenButtons.findViewById(R.id.cancelButton).setVisibility(View.GONE);
                    viewWithOpenButtons.findViewById(R.id.dismissButton).setVisibility(View.GONE);
                    viewWithOpenButtons.findViewById(R.id.declineButton).setVisibility(View.GONE);
                    viewWithOpenButtons = null;
                }
                if (groupPosition == DashboardListAdapter.INVITATIONSGROUP || groupPosition == DashboardListAdapter.PUBLICINVITATIONSGROUP || groupPosition == DashboardListAdapter.SENTINVITATIONSGROUP) {
                    if (groupPosition != DashboardListAdapter.SENTINVITATIONSGROUP) {
                        if (v.findViewById(R.id.acceptButton).getVisibility() == View.VISIBLE) {
                            v.findViewById(R.id.acceptButton).setVisibility(View.GONE);
                        } else {
                            v.findViewById(R.id.acceptButton).setVisibility(View.VISIBLE);
                            Button button = (Button) v.findViewById(R.id.acceptButton);
                            button.setOnClickListener(new View.OnClickListener() {
                                public void onClick(View v) {
                                    String setID;
                                    String opponentName = "";
                                    if (groupPosition == DashboardListAdapter.INVITATIONSGROUP) {
                                        setID = player.getInvitations().get(childPosition).getGameID();
                                        opponentName = player.getInvitations().get(childPosition).getOpponentName();
                                    } else {
                                        // check if enough credit.
                                        if (!player.isSubscriber()) {
                                            int remainingCredit = PrefUtils.getIntFromPrefs(MainActivity.this, PrefUtils.PREFS_OPENINVITATIONCREDIT_KEY, 2);
//                                        System.out.println(" remaining credit = " + remainingCredit);
                                            if (remainingCredit < 1) {
                                                Snackbar snackbar = Snackbar
                                                        .make(getCurrentFocus(), getString(R.string.not_enough_credit), Snackbar.LENGTH_LONG)
                                                        .setAction(getString(R.string.post_now), new View.OnClickListener() {
                                                            @Override
                                                            public void onClick(View view) {
                                                                Intent intent = new Intent(getApplicationContext(), InvitationActivity.class);
                                                                startActivity(intent);
                                                            }
                                                        });

                                                snackbar.show();
                                                return;
                                            } else {
                                                remainingCredit -= 1;
                                                PrefUtils.saveIntToPrefs(MainActivity.this, PrefUtils.PREFS_OPENINVITATIONCREDIT_KEY, remainingCredit);
                                            }
                                        }
                                        setID = player.getPublicInvitations().get(childPosition).getGameID();
                                        opponentName = player.getPublicInvitations().get(childPosition).getOpponentName();
                                    }
                                    player.respondInvitation(setID, true, listAdapter);
                                    PrefUtils.savePlayerToPrefs(MainActivity.this, opponentName);

                                    if (player.showAds()) {
                                        if (mInterstitialAd.isLoaded()) {
                                            mInterstitialAd.show();
                                        }
                                    }
                                    viewWithOpenButtons.findViewById(R.id.acceptButton).setVisibility(View.GONE);
                                    viewWithOpenButtons.findViewById(R.id.cancelButton).setVisibility(View.GONE);
                                    viewWithOpenButtons.findViewById(R.id.dismissButton).setVisibility(View.GONE);
                                    viewWithOpenButtons.findViewById(R.id.declineButton).setVisibility(View.GONE);
                                }
                            });
                            viewWithOpenButtons = v;
                        }
                    }
                    if (groupPosition == DashboardListAdapter.INVITATIONSGROUP) {
                        if (v.findViewById(R.id.declineButton).getVisibility() == View.VISIBLE) {
                            v.findViewById(R.id.declineButton).setVisibility(View.GONE);
                        } else {
                            v.findViewById(R.id.declineButton).setVisibility(View.VISIBLE);
                            Button button = (Button) v.findViewById(R.id.declineButton);
                            button.setOnClickListener(new View.OnClickListener() {
                                public void onClick(View v) {
                                    player.respondInvitation(player.getInvitations().get(childPosition).getGameID(), false, listAdapter);
                                }
                            });
                        }
                    }
                    if (groupPosition == DashboardListAdapter.SENTINVITATIONSGROUP) {
                        if (v.findViewById(R.id.cancelButton).getVisibility() == View.VISIBLE) {
                            v.findViewById(R.id.cancelButton).setVisibility(View.GONE);
                        } else {
                            v.findViewById(R.id.cancelButton).setVisibility(View.VISIBLE);
                            Button button = (Button) v.findViewById(R.id.cancelButton);
                            button.setOnClickListener(new View.OnClickListener() {
                                public void onClick(View v) {
                                    player.cancelInvitation(player.getSentInvitations().get(childPosition).getGameID(), listAdapter);
                                }
                            });
                        }
                    }
                    if (v.findViewById(R.id.dismissButton).getVisibility() == View.VISIBLE) {
                        v.findViewById(R.id.dismissButton).setVisibility(View.GONE);
                    } else {
                        v.findViewById(R.id.dismissButton).setVisibility(View.VISIBLE);
                        viewWithOpenButtons = v;
                        Button button = (Button) v.findViewById(R.id.dismissButton);
                        button.setOnClickListener(new View.OnClickListener() {
                            public void onClick(View view) {
                                viewWithOpenButtons.findViewById(R.id.acceptButton).setVisibility(View.GONE);
                                viewWithOpenButtons.findViewById(R.id.declineButton).setVisibility(View.GONE);
                                viewWithOpenButtons.findViewById(R.id.dismissButton).setVisibility(View.GONE);
                            }
                        });
                    }
                }
                if (groupPosition == DashboardListAdapter.ACTIVEGAMESGROUP || groupPosition == DashboardListAdapter.NONACTIVEGAMESGROUP) {
                    Game game;
                    if (groupPosition == DashboardListAdapter.ACTIVEGAMESGROUP) {
                        game = player.getActiveGames().get(childPosition);
                    } else {
                        game = player.getNonActiveGames().get(childPosition);
                    }
                    Intent intent = new Intent(getApplicationContext(), BoardActivity.class);
                    intent.putExtra("game", game);
                    startActivity(intent);
                }
                return false;
            }
        });
        if (!PrefUtils.getBooleanFromPrefs(MainActivity.this, PrefUtils.PREFS_MESSAGES_COLLAPSED_KEY, false)) {
            expandableList.expandGroup(DashboardListAdapter.MESSAGESGROUP);
        }
        if (!PrefUtils.getBooleanFromPrefs(MainActivity.this, PrefUtils.PREFS_INVITATIONS_COLLAPSED_KEY, false)) {
            expandableList.expandGroup(DashboardListAdapter.INVITATIONSGROUP);
        }
        if (!PrefUtils.getBooleanFromPrefs(MainActivity.this, PrefUtils.PREFS_ACTIVEGAMES_COLLAPSED_KEY, false)) {
            expandableList.expandGroup(DashboardListAdapter.ACTIVEGAMESGROUP);
        }
        if (!PrefUtils.getBooleanFromPrefs(MainActivity.this, PrefUtils.PREFS_PUBLICINVITATIONS_COLLAPSED_KEY, false)) {
            expandableList.expandGroup(DashboardListAdapter.PUBLICINVITATIONSGROUP);
        }
        if (!PrefUtils.getBooleanFromPrefs(MainActivity.this, PrefUtils.PREFS_SENTINVITATIONS_COLLAPSED_KEY, false)) {
            expandableList.expandGroup(DashboardListAdapter.SENTINVITATIONSGROUP);
        }
        if (!PrefUtils.getBooleanFromPrefs(MainActivity.this, PrefUtils.PREFS_NONACTIVEGAMES_COLLAPSED_KEY, false)) {
            expandableList.expandGroup(DashboardListAdapter.NONACTIVEGAMESGROUP);
        }
        if (!PrefUtils.getBooleanFromPrefs(MainActivity.this, PrefUtils.PREFS_TOURNAMENTS_COLLAPSED_KEY, false)) {
            expandableList.expandGroup(DashboardListAdapter.TOURNAMENTGROUP);
        }
        if (!PrefUtils.getBooleanFromPrefs(MainActivity.this, PrefUtils.PREFS_KOTH_COLLAPSED_KEY, false)) {
            expandableList.expandGroup(DashboardListAdapter.KOTHGROUP);
        }
        this.player.loadPlayer(this.listAdapter, PrefUtils.getBooleanFromPrefs(MainActivity.this, PrefUtils.PREFS_LOADAVATARS_KEY, false));
//        System.out.println("messages " + player.getMessages().size());
        myToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                Intent intent;
                switch (menuItem.getItemId()){
                    case R.id.play_human:
                        intent = new Intent(getApplicationContext(), InvitationActivity.class);
                        startActivity(intent);
                        return true;
                    case R.id.play_computer:
                        intent = new Intent(getApplicationContext(), InviteAIActivity.class);
                        startActivity(intent);
                        return true;
                    case R.id.social:
                        intent = new Intent(getApplicationContext(), SocialActivity.class);
                        startActivity(intent);
                        return true;
                    case R.id.play_mmai:
                        intent = new Intent(getApplicationContext(), MMAIActivity.class);
                        startActivity(intent);
                        return true;
                    case R.id.database:
                        if (player != null && player.isSubscriber()) {
                            intent = new Intent(getApplicationContext(), DatabaseActivity.class);
                            startActivity(intent);
                        } else {
                            Display display = getWindowManager().getDefaultDisplay();
                            Point size = new Point();
                            display.getSize(size);

                            View policyView = ((LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.db_subscribers_only, null, false);
                            policyView.setBackgroundColor(Color.WHITE);
                            ((Button) policyView.findViewById(R.id.subscribeButton)).setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    popupWindow.dismiss();
                                    String url = "https://www.pente.org/gameServer/subscriptions"; // missing 'http://' will cause crashed
                                    Intent intent = new Intent(MainActivity.this, WebViewActivity.class);
                                    intent.putExtra("url", url);
                                    startActivity(intent);
                                }
                            });
                            popupWindow = new PopupWindow(policyView, size.x*9/10, ViewGroup.LayoutParams.WRAP_CONTENT, true );
                            popupWindow.setFocusable(true);
                            popupWindow.setOutsideTouchable(true);
                            popupWindow.setBackgroundDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.border));
                            popupWindow.showAtLocation(getCurrentFocus(), Gravity.TOP, 0, 260);
                            ((TextView) policyView.findViewById(R.id.informationView)).setText(getString(R.string.level_up_your_game));
//                            ((TextView) policyView.findViewById(R.id.informationView)).setMovementMethod(new ScrollingMovementMethod());
                            popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
                                @Override
                                public void onDismiss() {
                                    ((ExpandableListView) findViewById(R.id.list)).setAlpha(1.0f);
                                }
                            });
                            ((ExpandableListView) findViewById(R.id.list)).setAlpha(0.25f);
                        }
                        return true;
                    case R.id.action_new_message:
                        intent = new Intent(getApplicationContext(), SendMessageActivity.class);
                        startActivity(intent);
                        return true;
                    case R.id.moreSettings:
                        intent = new Intent(getApplicationContext(), SettingsActivity.class);
                        startActivity(intent);
                        return true;
                    case R.id.action_show_stats:
                        Point size = new Point();
                        Display display = getWindowManager().getDefaultDisplay();
                        display.getSize(size);

                        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                        View popUpView = inflater.inflate(R.layout.ratingstats_listview, null);
                        popUpView.setBackgroundColor(Color.BLUE);
                        popupWindow = new PopupWindow(popUpView, size.x*4/5, ViewGroup.LayoutParams.WRAP_CONTENT, true );
                        ExpandableListView ratingListView = (ExpandableListView) findViewById(R.id.ratingStatsListView);
                        ratingListView =  (ExpandableListView) popupWindow.getContentView().findViewById(R.id.ratingStatsListView);
                        RatingStatsListAdapter adapter = new RatingStatsListAdapter(player.getRatingStats());
                        adapter.setInflater(inflater, MainActivity.this);
                        ratingListView.setAdapter(adapter);
                        ratingListView.expandGroup(0);
                        ratingListView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
                            @Override
                            public boolean onGroupClick(ExpandableListView parent, View v,
                                                        int groupPosition, long id) {
                                return true; // This way the expander cannot be collapsed
                            }
                        });
                        ratingListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
                            @Override
                            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                                String username = PentePlayer.mPlayerName;
                                int gameInt = player.getRatingStats().get(childPosition).getGameId();
                                String url = "https://www.pente.org/gameServer/viewLiveGames?p="+username+"&g="+gameInt;
                                Intent intent = new Intent(MainActivity.this, WebViewActivity.class);
                                intent.putExtra("url", url);
                                startActivity(intent);

                                return true;
                            }
                        });

                        popupWindow.setFocusable(true);
                        popupWindow.setBackgroundDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.border));
                        popupWindow.setOutsideTouchable(true);
                        popupWindow.showAtLocation(getCurrentFocus(), Gravity.TOP, 0, 260);
                        popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
                            @Override
                            public void onDismiss() {
                                ((ExpandableListView) findViewById(R.id.list)).setAlpha(1.0f);
                            }
                        });
                        ((ExpandableListView) findViewById(R.id.list)).setAlpha(0.25f);

                        return true;
                    case R.id.onlineUsers:
                        WhosOnlineListAdapter onlineListAdapter = new WhosOnlineListAdapter(player);
                        onlineListAdapter.setInflater((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE), MainActivity.this);

                        LoadWhosOnlineTask loadWhosOnlineTask = new LoadWhosOnlineTask(player, onlineListAdapter);
                        loadWhosOnlineTask.execute((Void) null);

                        return true;
                    case R.id.live_games:
                        intent = new Intent(getApplicationContext(), LobbyActivity.class);
                        startActivity(intent);
                        return true;
                }

                return false;
            }
        });
//        Intent intent = new Intent(getApplicationContext(), LobbyActivity.class);
//        startActivity(intent);

    }



    //This is the handler that will manager to process the broadcast intent
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            // Extract data included in the Intent
            String message = intent.getStringExtra("message");
            player.loadPlayer(listAdapter, PrefUtils.getBooleanFromPrefs(MainActivity.this, PrefUtils.PREFS_LOADAVATARS_KEY, false));

            Toast.makeText(MainActivity.this, message,
                    Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        MyApplication.activityResumed(this);
        this.player.loadPlayer(this.listAdapter, PrefUtils.getBooleanFromPrefs(MainActivity.this, PrefUtils.PREFS_LOADAVATARS_KEY, false));
        (MainActivity.this).registerReceiver(mMessageReceiver, new IntentFilter("unique_name"));
    }
    @Override
    protected void onPause() {
        super.onPause();
        MyApplication.activityPaused();
        (MainActivity.this).unregisterReceiver(mMessageReceiver);
    }

    private void refreshPlayer() {
        player.loadPlayer(listAdapter, PrefUtils.getBooleanFromPrefs(MainActivity.this, PrefUtils.PREFS_LOADAVATARS_KEY, false));
//        player.loadPlayer(listAdapter, true);
        swipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.dashboard_menu, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
//            case R.id.action_settings:
//                // User chose the "Settings" item, show the app settings UI...
//                return true;

            case R.id.action_new_invitation:
                // User chose the "Favorite" action, mark the current item
                // as a favorite...
                return true;

            case R.id.action_new_message:
                // User chose the "Favorite" action, mark the current item
                // as a favorite...
                return true;

            case R.id.action_show_stats:

                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    private void requestNewInterstitial() {
        AdRequest adRequest = new AdRequest.Builder()
                .build();
        mInterstitialAd.loadAd(adRequest);
    }

    public void ask2GetStarted() {
        Snackbar snackbar = Snackbar
                .make(getCurrentFocus(), getString(R.string.nothing_to_see_here), Snackbar.LENGTH_LONG)
                .setAction(getString(R.string.post_now), new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(getApplicationContext(), InvitationActivity.class);
                        startActivity(intent);
                    }
                });

        snackbar.show();
    }

    private class LoadWhosOnlineTask extends AsyncTask<Void, Void, Boolean> {

        private WhosOnlineListAdapter listAdapter;
        String dashboardString;
        private PentePlayer player;

        LoadWhosOnlineTask(PentePlayer player, WhosOnlineListAdapter listAdapter) {
            this.listAdapter = listAdapter;
            this.player = player;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            try {
//                URL url = new URL("https://www.pente.org/gameServer/mobile/index.jsp?name="+mUsername+"&password="+mPassword);
                URL url = new URL("https://www.pente.org/gameServer/mobile/whosonlineandlive.jsp?name2="+PentePlayer.mPlayerName+"&password2="+ PentePlayer.mPassword);

//                url = new URL("https://development.pente.org/gameServer/mobile/index.jsp?name="+mUsername+"&password="+mPassword);
                HttpsURLConnection connection = (HttpsURLConnection)url.openConnection();
                String cookies = CookieManager.getInstance().getCookie("https://www.pente.org/");
//                System.out.println("cookies: " +cookies);
                if (cookies != null) {
                    String[] splitCookie = cookies.split(";");
                    String cookieStr = "";
                    for (String item: splitCookie) {
                        if (item.contains("name2") || item.contains("password2")) {
                            cookieStr += item + ";";
                        }
                    }
                    connection.setRequestProperty("Cookie", cookieStr);
//                    System.out.println("cookieStr: " +cookieStr);
                }
//                connection.addRequestProperty("Cookie", "name2="+mUsername+"; password2="+mPassword+";");
                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    return false;
                }

                StringBuilder output = new StringBuilder();
                BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
//                System.out.println("output===============" + br);
                String line = "";
                while((line = br.readLine()) != null ) {
                    output.append(line + "\n");
                }
                br.close();

//                System.out.println(output);

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
                final Map<String, List<KothPlayer>> onlinePlayers = new HashMap<>();
                int total = 0;
                String[] dashLines = dashboardString.split("\n");
                int idx = 0;
                while (idx < dashLines.length) {
                    String roomLine = dashLines[idx];
                    String[] splitRoomLine = roomLine.split(":");
                    if (splitRoomLine.length>1) {
                        List<KothPlayer> playersList = new ArrayList<>();
                        String roomName = splitRoomLine[0];
                        String[] users = splitRoomLine[1].split(";");
                        for (String dashLine: users) {
                            String[] splitLine = dashLine.split(",");
                            if (splitLine.length > 4) {
                                KothPlayer player = new KothPlayer(splitLine[0], splitLine[1], splitLine[4], false, Integer.parseInt(splitLine[3]), Integer.parseInt(splitLine[2]));
                                if (PentePlayer.loadAvatars && player.getColor() != 0) {
                                    this.player.addUserAvatar(player.getName());
                                }
                                total = total + 1;
                                playersList.add(player);
                            }
                        }
                        onlinePlayers.put(roomName, playersList);
                    }
                    idx += 1;
                }
                listAdapter.setOnlinePlayers(onlinePlayers);
                Point size = new Point();
                Display display = getWindowManager().getDefaultDisplay();
                display.getSize(size);

                LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View popUpView = inflater.inflate(R.layout.onlineusers_listview, null);
                popUpView.setBackgroundColor(Color.BLUE);
//                popupWindow = new PopupWindow(popUpView, size.x*4/5, ViewGroup.LayoutParams.WRAP_CONTENT, true );
                final float scale = getResources().getDisplayMetrics().density;
                popupWindow = new PopupWindow(popUpView, size.x*4/5, (int) ((30+Math.min(Math.floor((((size.y/scale)*2/3)/44))*44, total*44))*scale), true );
//                popupWindow = new PopupWindow(popUpView, size.x*4/5, (int) ((30+(onlinePlayers.size())*44)*scale), true );
                System.out.println("totaaaaaaaal "+total);
                ExpandableListView onlineUsersListView =  (ExpandableListView) popupWindow.getContentView().findViewById(R.id.onlineUsersListView);
                onlineUsersListView.setDividerHeight(0);
                onlineUsersListView.setAdapter(listAdapter);
                for (int i = 0; i < onlinePlayers.size(); i++ ) {
                    onlineUsersListView.expandGroup(i);
                }
                onlineUsersListView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
                    @Override
                    public boolean onGroupClick(ExpandableListView parent, View v,
                                                int groupPosition, long id) {
                        return true; // This way the expander cannot be collapsed
                    }
                });
                onlineUsersListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
                    @Override
                    public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                        if (!listAdapter.sections.get(groupPosition).equals("Mobile")) {
                            return false;
                        }
                        KothPlayer onlinePlayer = onlinePlayers.get(listAdapter.sections.get(groupPosition)).get(childPosition);
                        if (player.getPlayerName().equals(onlinePlayer.getName())) {
                            return false;
                        }
                        Intent intent = new Intent(getApplicationContext(), InvitationActivity.class);
                        intent.putExtra("opponent", onlinePlayer.getName());
                        startActivity(intent);

                        return true;
                    }
                });

//                if (totalHeight > size.y*4/5) {
//                    popupWindow.setHeight(size.y*4/5);
//                }

                popupWindow.setFocusable(true);
                popupWindow.setBackgroundDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.border));
                popupWindow.setOutsideTouchable(true);
                popupWindow.showAtLocation(getCurrentFocus(), Gravity.TOP, 0, 260);
                popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
                    @Override
                    public void onDismiss() {
                        ((ExpandableListView) findViewById(R.id.list)).setAlpha(1.0f);
                    }
                });
                ((ExpandableListView) findViewById(R.id.list)).setAlpha(0.25f);

//                listAdapter.updateList();
            }
        }

        @Override
        protected void onCancelled() {
        }
    }



}
