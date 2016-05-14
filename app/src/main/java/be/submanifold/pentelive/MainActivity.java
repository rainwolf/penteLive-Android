package be.submanifold.pentelive;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;


public class MainActivity extends AppCompatActivity {

    private PentePlayer player;
    private SwipeRefreshLayout swipeRefreshLayout;
    private AdView mAdView;
    private DashboardListAdapter listAdapter;
    private View viewWithOpenButtons = null;
    InterstitialAd mInterstitialAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        myToolbar.setTitle("Dashboard");
        setSupportActionBar(myToolbar);
        this.player = getIntent().getParcelableExtra("pentePlayer");

        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId("ca-app-pub-3326997956703582/8120483448");

        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                requestNewInterstitial();
            }
        });
        requestNewInterstitial();

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
                if (groupPosition == 0) {
                    Intent intent = new Intent(getApplicationContext(), ReplyMessageActivity.class);
                    intent.putExtra("message", player.getMessages().get(childPosition));
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
                if (groupPosition == 1 || groupPosition == 3 || groupPosition == 4) {
                    if (groupPosition < 4) {
                        if (v.findViewById(R.id.acceptButton).getVisibility() == View.VISIBLE) {
                            v.findViewById(R.id.acceptButton).setVisibility(View.GONE);
                        } else {
                            v.findViewById(R.id.acceptButton).setVisibility(View.VISIBLE);
                            Button button = (Button) v.findViewById(R.id.acceptButton);
                            button.setOnClickListener(new View.OnClickListener() {
                                public void onClick(View v) {
                                    String setID;
                                    if (groupPosition == 1) {
                                        setID = player.getInvitations().get(childPosition).getGameID();
                                    } else {
                                        // check if enough credit.
                                        if (!player.isSubscriber()) {
                                            int remainingCredit = PrefUtils.getIntFromPrefs(MainActivity.this, PrefUtils.PREFS_OPENINVITATIONCREDIT_KEY, 2);
//                                        System.out.println(" remaining credit = " + remainingCredit);
                                            if (remainingCredit < 1) {
                                                Snackbar snackbar = Snackbar
                                                        .make(getCurrentFocus(), "You can accept open invitations again after posting one.", Snackbar.LENGTH_LONG)
                                                        .setAction("Post now!", new View.OnClickListener() {
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
                                    }
                                    player.respondInvitation(setID, true, listAdapter);
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
                    if (groupPosition == 1) {
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
                    if (groupPosition == 4) {
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
                if (groupPosition == 2 || groupPosition == 5) {
                    Game game;
                    if (groupPosition == 2) {
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
            expandableList.expandGroup(0);
        }
        if (!PrefUtils.getBooleanFromPrefs(MainActivity.this, PrefUtils.PREFS_INVITATIONS_COLLAPSED_KEY, false)) {
            expandableList.expandGroup(1);
        }
        if (!PrefUtils.getBooleanFromPrefs(MainActivity.this, PrefUtils.PREFS_ACTIVEGAMES_COLLAPSED_KEY, false)) {
            expandableList.expandGroup(2);
        }
        if (!PrefUtils.getBooleanFromPrefs(MainActivity.this, PrefUtils.PREFS_PUBLICINVITATIONS_COLLAPSED_KEY, false)) {
            expandableList.expandGroup(3);
        }
        if (!PrefUtils.getBooleanFromPrefs(MainActivity.this, PrefUtils.PREFS_SENTINVITATIONS_COLLAPSED_KEY, false)) {
            expandableList.expandGroup(4);
        }
        if (!PrefUtils.getBooleanFromPrefs(MainActivity.this, PrefUtils.PREFS_NONACTIVEGAMES_COLLAPSED_KEY, false)) {
            expandableList.expandGroup(5);
        }
        this.player.loadPlayer(this.listAdapter);
//        System.out.println("messages " + player.getMessages().size());
        myToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                Intent intent;
                switch (menuItem.getItemId()){
                    case R.id.action_new_invitation:
                        intent = new Intent(getApplicationContext(), InvitationActivity.class);
                        startActivity(intent);
                        return true;
                    case R.id.action_new_message:
                        intent = new Intent(getApplicationContext(), SendMessageActivity.class);
                        startActivity(intent);
                        return true;
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
            String message = intent.getStringExtra("message");
            player.loadPlayer(listAdapter);

            Toast.makeText(MainActivity.this, message,
                    Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        MyApplication.activityResumed();
        this.player.loadPlayer(this.listAdapter);
        (MainActivity.this).registerReceiver(mMessageReceiver, new IntentFilter("unique_name"));
    }
    @Override
    protected void onPause() {
        super.onPause();
        MyApplication.activityPaused();
        (MainActivity.this).unregisterReceiver(mMessageReceiver);
    }

    private void refreshPlayer() {
        player.loadPlayer(listAdapter);
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




}
