package be.submanifold.pentelive;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.appcompat.widget.Toolbar;

import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;


public class KingOfTheHillActivity extends AppCompatActivity {

    private List<List<KothPlayer>> hill;
    private PentePlayer player;
    private KingOfTheHill kothSummary;
    private SwipeRefreshLayout swipeRefreshLayout;
    private KingOfTheHillListAdapter listAdapter;
    private PopupWindow popupWindow;
    private View challengeView;
    private ExpandableListView expandableList;
    String challengedUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_king_of_the_hill);
        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        this.player = getIntent().getParcelableExtra("pentePlayer");
        this.kothSummary = getIntent().getParcelableExtra("kothSummary");
        myToolbar.setTitle(kothSummary.getGame());
        setSupportActionBar(myToolbar);

        challengeView = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.koth_challenge_layout, null, false);


        expandableList = findViewById(R.id.list);
        listAdapter = new KingOfTheHillListAdapter(this.player);
        expandableList.setAdapter(listAdapter);
        listAdapter.setInflater((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE), this);
        listAdapter.setKothSummary(kothSummary);
        expandableList.setOnGroupClickListener((parent, v, groupPosition, id) -> {
            return true; // This way the expander cannot be collapsed
        });

        swipeRefreshLayout = findViewById(R.id.swipeRefreshKOTH);
        swipeRefreshLayout.setOnRefreshListener(
                () -> refreshPlayer()
        );
        if (kothSummary.getGameId() > 50) {
            expandableList.setOnChildClickListener((parent, v, groupPosition, childPosition, id) -> {
//                System.out.println("kittycat " + childPosition + " of " + groupPosition );
                if (groupPosition > 0) {
                    if (hill.get(groupPosition - 1).get(childPosition).isCanBeChallenged()) {
                        challengedUser = hill.get(groupPosition - 1).get(childPosition).getName();
                        ((TextView) challengeView.findViewById(R.id.titleLabel)).setText(getString(R.string.challenge, challengedUser));
                        challengeView.findViewById(R.id.restrictionLayout).setVisibility(View.GONE);
                        popupWindow.showAtLocation(findViewById(R.id.list), Gravity.TOP, 0, 300);
                        expandableList.setAlpha(0.5f);
                    } else {
                        String url = "https://www.pente.org/gameServer/profile?viewName=" + hill.get(groupPosition - 1).get(childPosition).getName() + "&name2=" + PentePlayer.mPlayerName + "&password2=" + PentePlayer.mPassword;
                        Intent intent = new Intent(KingOfTheHillActivity.this, WebViewActivity.class);
                        intent.putExtra("url", url);
                        startActivity(intent);
                    }
                    return true;
                }
                return false;
            });
            expandableList.setOnItemLongClickListener((parent, view, position, id) -> {
                int groupPosition = ExpandableListView.getPackedPositionGroup(id);
                int childPosition = ExpandableListView.getPackedPositionChild(id);
                if (groupPosition == 0 && childPosition == 0) {
                    JoinLeaveHillTask joinLeaveTask = new JoinLeaveHillTask(kothSummary.getGameId(), !kothSummary.isMember());
                    joinLeaveTask.execute((Void) null);

                    return true;
                }

                return false;
            });
        } else {
            expandableList.setOnChildClickListener((parent, v, groupPosition, childPosition, id) -> {
                if (groupPosition > 0) {
                    String url = "https://www.pente.org/gameServer/profile?viewName=" + hill.get(groupPosition - 1).get(childPosition).getName() + "&name2=" + PentePlayer.mPlayerName + "&password2=" + PentePlayer.mPassword;
                    Intent intent = new Intent(KingOfTheHillActivity.this, WebViewActivity.class);
                    intent.putExtra("url", url);
                    startActivity(intent);
                    return true;
                }
                return false;
            });
        }

        LoadHillTask loadTask = new LoadHillTask(kothSummary.getGameId());
        loadTask.execute((Void) null);

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        popupWindow = new PopupWindow(challengeView, size.x * 4 / 5, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popupWindow.setFocusable(true);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setBackgroundDrawable(ContextCompat.getDrawable(KingOfTheHillActivity.this, R.drawable.border));
//                        messageWindow.setAnimationStyle(R.anim.animation);

        Spinner spinner = challengeView.findViewById(R.id.timeoutSpinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(KingOfTheHillActivity.this,
                R.array.timeout_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(PrefUtils.getIntFromPrefs(KingOfTheHillActivity.this, PrefUtils.PREFS_KOTHTIMEOUT_KEY, 6));
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                PrefUtils.saveIntToPrefs(KingOfTheHillActivity.this, PrefUtils.PREFS_KOTHTIMEOUT_KEY, position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        spinner = challengeView.findViewById(R.id.restrictionSpinner);
        adapter = ArrayAdapter.createFromResource(KingOfTheHillActivity.this,
                R.array.restriction_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(PrefUtils.getIntFromPrefs(KingOfTheHillActivity.this, PrefUtils.PREFS_KOTHRESTRICTION_KEY, 0));
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                PrefUtils.saveIntToPrefs(KingOfTheHillActivity.this, PrefUtils.PREFS_KOTHRESTRICTION_KEY, position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        popupWindow.setOnDismissListener(() -> expandableList.setAlpha(1.0f));
        challengeView.findViewById(R.id.sendChallengeButton).setOnClickListener(v -> {
            String restriction = "A";
            switch (PrefUtils.getIntFromPrefs(KingOfTheHillActivity.this, PrefUtils.PREFS_KOTHRESTRICTION_KEY, 0)) {
                case 0:
                    restriction = "A";
                    break;
                case 1:
                    restriction = "N";
                    break;
                case 2:
                    restriction = "L";
                    break;
                case 3:
                    restriction = "H";
                    break;
                case 4:
                    restriction = "S";
                    break;
                case 5:
                    restriction = "C";
                    break;
            }

            SendInvitationTask inviteTask = new SendInvitationTask(challengedUser, kothSummary.getGameId(), "" + (PrefUtils.getIntFromPrefs(KingOfTheHillActivity.this, PrefUtils.PREFS_KOTHTIMEOUT_KEY, 6) + 1), restriction);
            inviteTask.execute((Void) null);
            popupWindow.dismiss();
        });

    }


    //This is the handler that will manager to process the broadcast intent
    private final BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            // Extract data included in the Intent
            String message = intent.getStringExtra("message");

            if (message != null && !message.isEmpty()) {
                Toast.makeText(KingOfTheHillActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        MyApplication.activityResumed(this);
        ContextCompat.registerReceiver((KingOfTheHillActivity.this), mMessageReceiver, new IntentFilter("unique_name"), ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MyApplication.activityPaused();
        (KingOfTheHillActivity.this).unregisterReceiver(mMessageReceiver);
    }

    private void refreshPlayer() {
        LoadHillTask loadTask = new LoadHillTask(kothSummary.getGameId());
        loadTask.execute((Void) null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.koth_menu, menu);
        if (!kothSummary.canIchallenge() || kothSummary.getGameId() < 50) {
            menu.findItem(R.id.action_post_open_koth).setEnabled(false);
            menu.findItem(R.id.action_post_open_koth).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_web_koth:
                int game = kothSummary.getGameId();
                String url = "https://www.pente.org/gameServer/stairs.jsp?game=" + game + "&name2=" + PentePlayer.mPlayerName + "&password2=" + PentePlayer.mPassword;
                Intent intent = new Intent(KingOfTheHillActivity.this, WebViewActivity.class);
                intent.putExtra("url", url);
                startActivity(intent);

                return true;
            case R.id.action_post_open_koth:
                if (!kothSummary.canIchallenge() && !PentePlayer.mSubscriber) {
//                if (true) {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(KingOfTheHillActivity.this);
                    builder.setTitle(getString(R.string.public_invitations_limit_reached));
                    builder.setMessage(getString(R.string.koth_limit));
                    builder.setPositiveButton(getString(R.string.dismiss), (dialog, which) -> {
                    });
                    builder.setNeutralButton(getString(R.string.subscribe_now), (dialog, which) -> {
                        String url1 = "https://www.pente.org/gameServer/subscriptions" + "?name2=" + PentePlayer.mPlayerName + "&password2=" + PentePlayer.mPassword;
                        Intent intent1 = new Intent(KingOfTheHillActivity.this, WebViewActivity.class);
                        intent1.putExtra("url", url1);
                        startActivity(intent1);
                    });
                    AlertDialog dlg = builder.create();
                    dlg.show();
                } else if (kothSummary.canIchallenge()) {
                    ((TextView) challengeView.findViewById(R.id.titleLabel)).setText(getString(R.string.send_open_challenge));
                    challengedUser = "";
                    popupWindow.showAtLocation(findViewById(R.id.list), Gravity.TOP, 0, 260);
                    challengeView.findViewById(R.id.restrictionLayout).setVisibility(View.VISIBLE);
                    expandableList.setAlpha(0.5f);
                }
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    private void loadHill(String jsonString) {
        hill = new ArrayList<>();
        Type listType = new TypeToken<List<List<JsonModels.KothPlayerEntry>>>(){}.getType();
        List<List<JsonModels.KothPlayerEntry>> steps = new Gson().fromJson(jsonString, listType);
        boolean isMember = false;
        if (steps != null) {
            for (List<JsonModels.KothPlayerEntry> stepEntries : steps) {
                List<KothPlayer> step = new ArrayList<>();
                for (JsonModels.KothPlayerEntry entry : stepEntries) {
                    KothPlayer player = new KothPlayer(entry.name, String.valueOf(entry.rating), entry.lastGame, entry.canChallenge, entry.tourneyWinner, entry.color);
                    if (PentePlayer.loadAvatars && player.getColor() != 0) {
                        this.player.addUserAvatar(player.getName());
                    }
                    if (entry.name != null && entry.name.equals(PentePlayer.mPlayerName)) {
                        isMember = true;
                    }
                    step.add(player);
                }
                if (!step.isEmpty()) {
                    hill.add(0, step);
                }
            }
        }
        kothSummary.setMember(isMember);
        while (!hill.isEmpty() && hill.get(0).isEmpty()) {
            hill.remove(0);
        }
        listAdapter.setHill(hill);
        swipeRefreshLayout.setRefreshing(false);
    }

    private class LoadHillTask extends AsyncTask<Void, Void, Boolean> {

        private final int mGame;
        private String htmlString;

        LoadHillTask(int game) {
            this.mGame = game;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            try {
                String request = "https://www.pente.org/gameServer/mobile/json/koth.jsp?game=" + mGame
                        + "&name=" + PentePlayer.mPlayerName
                        + "&name2=" + PentePlayer.mPlayerName + "&password2=" + PentePlayer.mPassword;
                if (PentePlayer.development) {
                    request = "https://10.0.2.2/gameServer/mobile/json/koth.jsp?game=" + mGame
                            + "&name=" + PentePlayer.mPlayerName
                            + "&name2=" + PentePlayer.mPlayerName + "&password2=" + PentePlayer.mPassword;
                }
                URL url = new URL(request);
                HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                String cookies = CookieManager.getInstance().getCookie("https://www.pente.org/");
                if (cookies != null) {
                    String[] splitCookie = cookies.split(";");
                    String cookieStr = "";
                    for (String item : splitCookie) {
                        if (item.contains("name2") || item.contains("password2")) {
                            cookieStr += item + ";";
                        }
                    }
                    conn.setRequestProperty("Cookie", cookieStr);
                }
                int responseCode = conn.getResponseCode();
                if (responseCode != 200) {
                    return false;
                }

                StringBuilder output = new StringBuilder();
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line;
                while ((line = br.readLine()) != null) {
                    output.append(line);
                }
                br.close();

                htmlString = output.toString();

            } catch (IOException e1) {
                e1.printStackTrace();
                return false;
            }

            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            loadHill(htmlString);
            listAdapter.updateList();
            for (int i = 0; i < listAdapter.getGroupCount(); ++i) {
                expandableList.expandGroup(i);
            }
        }

        @Override
        protected void onCancelled() {
        }
    }

    private class JoinLeaveHillTask extends AsyncTask<Void, Void, Boolean> {

        private final int mGame;
        private final boolean join;

        JoinLeaveHillTask(int game, boolean join) {
            this.mGame = game;
            this.join = join;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            try {
                String urlParameters = "game=" + mGame + "&name2=" + PentePlayer.mPlayerName + "&password2=" + PentePlayer.mPassword;
//                String urlParameters  = "game=" + mGame+"&name2="+PentePlayer.mPlayerName;
                if (join) {
                    urlParameters += "&join=";
                }
                byte[] postData = new byte[0];
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                    postData = urlParameters.getBytes(StandardCharsets.UTF_8);
                }
                int postDataLength = postData.length;
                String request = "https://www.pente.org/gameServer/koth";
//                request        = "https://10.0.2.2/gameServer/koth";
                URL url = new URL(request);
                HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                String cookies = CookieManager.getInstance().getCookie("https://www.pente.org/");
                if (cookies != null) {
                    String[] splitCookie = cookies.split(";");
                    String cookieStr = "";
                    for (String item : splitCookie) {
                        if (item.contains("name2") || item.contains("password2")) {
                            cookieStr += item + ";";
                        }
                    }
                    conn.setRequestProperty("Cookie", cookieStr);
//                    System.out.println("cookieStr: " +cookieStr);
                }
                conn.setDoOutput(true);
                conn.setInstanceFollowRedirects(false);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setRequestProperty("charset", "utf-8");
                conn.setRequestProperty("Content-Length", Integer.toString(postDataLength));
                conn.setUseCaches(false);
                try {
                    DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
                    wr.write(postData);
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }

                StringBuilder output = new StringBuilder();
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
//                System.out.println("output===============" + br);
                String line = "";
                while ((line = br.readLine()) != null) {
                    output.append(line + System.getProperty("line.separator"));
                }
                br.close();

                output.append(System.getProperty("line.separator") + "Response " + System.getProperty("line.separator") + System.getProperty("line.separator"));
//                System.out.println(output.toString());

            } catch (IOException e1) {
                e1.printStackTrace();
                return false;
            }

            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            LoadHillTask loadTask = new LoadHillTask(mGame);
            loadTask.execute((Void) null);
        }

        @Override
        protected void onCancelled() {
        }
    }

    public class SendInvitationTask extends AsyncTask<Void, Void, Boolean> {

        private final String opponentName;
        private final int gameType;
        private final String timeout;
        private final String restriction;

        SendInvitationTask(String opponentName, int gameType, String timeout, String restriction) {
            this.opponentName = opponentName;
            this.gameType = gameType;
            this.timeout = timeout;
            this.restriction = restriction;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // TODO: attempt authentication against a network service.

            try {

//                String urlParameters  = "koth=&mobile=&invitee=" + opponentName + "&game=" + gameType +
//                        "&invitationRestriction=" + restriction + "&daysPerMove=" + timeout + "&rated=Y";
                String urlParameters = "koth=&mobile=&invitee=" + opponentName + "&game=" + gameType +
                        "&invitationRestriction=" + restriction + "&daysPerMove=" + timeout + "&rated=Y"
                        + "&name2=" + PentePlayer.mPlayerName + "&password2=" + PentePlayer.mPassword;
                byte[] postData = new byte[0];
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                    postData = urlParameters.getBytes(StandardCharsets.UTF_8);
                }
                int postDataLength = postData.length;
                String request = "https://www.pente.org/gameServer/tb/newGame";
//                request        = "https://10.0.2.2/gameServer/tb/newGame";
                URL url = new URL(request);
                HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                String cookies = CookieManager.getInstance().getCookie("https://www.pente.org/");
                if (cookies != null) {
                    String[] splitCookie = cookies.split(";");
                    String cookieStr = "";
                    for (String item : splitCookie) {
                        if (item.contains("name2") || item.contains("password2")) {
                            cookieStr += item + ";";
                        }
                    }
                    conn.setRequestProperty("Cookie", cookieStr);
//                    System.out.println("cookieStr: " +cookieStr);
                }
                conn.setDoOutput(true);
                conn.setInstanceFollowRedirects(false);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setRequestProperty("charset", "utf-8");
                conn.setRequestProperty("Content-Length", Integer.toString(postDataLength));
                conn.setUseCaches(false);
                try {
                    DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
                    wr.write(postData);
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }

                StringBuilder output = new StringBuilder();
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
//                System.out.println("output===============" + br);
                String line = "";
                while ((line = br.readLine()) != null) {
                    output.append(line + System.getProperty("line.separator"));
                }
                br.close();

                output.append(System.getProperty("line.separator") + "Response " + System.getProperty("line.separator") + System.getProperty("line.separator"));
//                System.out.println(output.toString());


                return true;

            } catch (IOException e1) {
                e1.printStackTrace();
                return false;
            }


            // TODO: register the new account here.
//            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            if (success) {
                PrefUtils.saveIntToPrefs(KingOfTheHillActivity.this, PrefUtils.PREFS_KOTHTIMEOUT_KEY, ((Spinner) challengeView.findViewById(R.id.timeoutSpinner)).getSelectedItemPosition());
                if ("".equals(opponentName)) {
                    int remainingCredit = PrefUtils.getIntFromPrefs(KingOfTheHillActivity.this, PrefUtils.PREFS_OPENINVITATIONCREDIT_KEY, 2) + 2;
                    PrefUtils.saveIntToPrefs(KingOfTheHillActivity.this, PrefUtils.PREFS_OPENINVITATIONCREDIT_KEY, remainingCredit);
                } else {
                    PrefUtils.savePlayerToPrefs(KingOfTheHillActivity.this, opponentName);
                }
                LoadHillTask loadTask = new LoadHillTask(gameType);
                loadTask.execute((Void) null);

//                finish();
            }
        }

        @Override
        protected void onCancelled() {
        }
    }


}
