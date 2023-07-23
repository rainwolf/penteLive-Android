package be.submanifold.pentelive;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;

public class InvitationActivity extends AppCompatActivity {


    private String opponent = null;
    private String gameType = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invitation);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Invitations");
        toolbar.setTitleTextColor(Color.WHITE);
        setSupportActionBar(toolbar);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            this.opponent =  extras.getString("opponent");
            this.gameType = extras.getString("gameType");
        }
        if (getOpponent() != null) {
            ((AutoCompleteTextView) findViewById(R.id.opponent)).setText(getOpponent());
        }
        ((AutoCompleteTextView) findViewById(R.id.opponent)).setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                InputMethodManager imm =  (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
        });

        Spinner spinner = findViewById(R.id.gameTypeSpinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.turn_based_game_types_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner = findViewById(R.id.timeoutSpinner);
        adapter = ArrayAdapter.createFromResource(this,
                R.array.timeout_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(6);
        spinner = findViewById(R.id.restrictionSpinner);
        adapter = ArrayAdapter.createFromResource(this,
                R.array.restriction_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        Button button = findViewById(R.id.sendInvitationButton);
        if (button != null) button.setOnClickListener(v -> {
            String opponentName = ((AutoCompleteTextView) findViewById(R.id.opponent)).getText().toString().toLowerCase();
            String gameType = "";
            switch (((Spinner) findViewById(R.id.gameTypeSpinner)).getSelectedItemPosition()) {
                case 0: gameType = "51"; break;
                case 1: gameType = "53"; break;
                case 2: gameType = "55"; break;
                case 3: gameType = "57"; break;
                case 4: gameType = "59"; break;
                case 5: gameType = "61"; break;
                case 6: gameType = "63"; break;
                case 7: gameType = "65"; break;
                case 8: gameType = "67"; break;
                case 9: gameType = "69"; break;
                case 10: gameType = "71"; break;
                case 11: gameType = "73"; break;
                case 12: gameType = "75"; break;
                case 13: gameType = "77"; break;
            }
            String timeout =  ((Spinner) findViewById(R.id.timeoutSpinner)).getSelectedItem().toString();
            String rated = ((ToggleButton) findViewById(R.id.ratedToggleButton)).isChecked()?"Y":"N";
            String restriction = "";
            switch (((Spinner) findViewById(R.id.restrictionSpinner)).getSelectedItemPosition()) {
                case 0: restriction = "B"; break;
                case 1: restriction = "A"; break;
                case 2: restriction = "N"; break;
                case 3: restriction = "L"; break;
                case 4: restriction = "H"; break;
                case 5: restriction = "S"; break;
                case 6: restriction = "C"; break;
            }
            String playAs = ((ToggleButton) findViewById(R.id.playAsToggleButton)).isChecked()?"2":"1";
            String privateGame = ((ToggleButton) findViewById(R.id.privateToggleButton)).isChecked()?"Y":"N";
            SendInvitationTask submitTask = new SendInvitationTask(opponentName, gameType, timeout, rated, restriction, playAs, privateGame);
            submitTask.execute((Void) null);
        });

        AutoCompleteTextView actv = (AutoCompleteTextView) findViewById(R.id.opponent);
        ArrayAdapter<String> acAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1, new ArrayList<String>(PrefUtils.getPlayers(InvitationActivity.this)));
        actv.setAdapter(acAdapter);

        if (getGameType() != null) {
            try {
                int gameInt = Integer.parseInt(getGameType());
                ((Spinner) findViewById(R.id.gameTypeSpinner)).setSelection((gameInt-51)/2);
            } catch (NumberFormatException e) {

            }
        } else {
            ((Spinner) findViewById(R.id.gameTypeSpinner)).setSelection(PrefUtils.getIntFromPrefs(InvitationActivity.this, PrefUtils.PREFS_INVITATIONGAME_KEY, 0));
            ((Spinner) findViewById(R.id.timeoutSpinner)).setSelection(PrefUtils.getIntFromPrefs(InvitationActivity.this, PrefUtils.PREFS_INVITATIONDAYS_KEY, 6));
            ((Spinner) findViewById(R.id.restrictionSpinner)).setSelection(PrefUtils.getIntFromPrefs(InvitationActivity.this, PrefUtils.PREFS_INVITATIONRESTRICTION_KEY, 0));
        }

        ((ToggleButton) findViewById(R.id.ratedToggleButton)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if ((isChecked)) {
                    ((TextView) findViewById(R.id.playAsLabel)).setVisibility(View.GONE);
                    ((TextView) findViewById(R.id.privateLabel)).setVisibility(View.GONE);
                    ((ToggleButton) findViewById(R.id.privateToggleButton)).setVisibility(View.GONE);
                    ((ToggleButton) findViewById(R.id.playAsToggleButton)).setVisibility(View.GONE);
                } else {
                    ((TextView) findViewById(R.id.playAsLabel)).setVisibility(View.VISIBLE);
                    ((TextView) findViewById(R.id.privateLabel)).setVisibility(View.VISIBLE);
                    ((ToggleButton) findViewById(R.id.privateToggleButton)).setVisibility(View.VISIBLE);
                    ((ToggleButton) findViewById(R.id.playAsToggleButton)).setVisibility(View.VISIBLE);
                }
            }
        });
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


    public String getOpponent() {
        return opponent;
    }

    public void setOpponent(String opponent) {
        this.opponent = opponent;
    }

    public String getGameType() {
        return gameType;
    }

    public void setGameType(String gameType) {
        this.gameType = gameType;
    }

    public class SendInvitationTask extends AsyncTask<Void, Void, Boolean> {

        private final String opponentName;
        private final String gameType;
        private final String timeout;
        private final String rated;
        private final String restriction;
        private final String playAs;
        private final String privateGame;

        SendInvitationTask(String opponentName, String gameType, String timeout, String rated, String restriction, String playAs, String privateGame) {
            this.opponentName = opponentName;
            this.gameType = gameType;
            this.timeout = timeout;
            this.rated = rated;
            this.restriction = restriction;
            this.playAs = playAs;
            this.privateGame = privateGame;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // TODO: attempt authentication against a network service.

            try {
//                URL url = new URL("https://www.pente.org/gameServer/tb/newGame?mobile=&invitee=" + opponentName + "&game=" + gameType +
//                        "&daysPerMove=" + timeout + "&rated=" + rated +"&invitationRestriction=" +
//                        restriction + "&playAs=" + playAs + "&privateGame=" + privateGame + "&name2=" + PentePlayer.mPlayerName + "&password2=" + PentePlayer.mPassword);
//                HttpsURLConnection connection = (HttpsURLConnection)url.openConnection();
//                int responseCode = connection.getResponseCode();
//                if (responseCode != 200) {
//                    System.out.println("response code for submit was " + responseCode);
//                    return false;
//                }
//
//                StringBuilder output = new StringBuilder();
//                BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
//                System.out.println("output===============" + br);
//                String line = "";
//                while((line = br.readLine()) != null ) {
//                    output.append(line + "\n");
//                }
//                br.close();

//                String urlParameters  = "mobile=&invitee=" + opponentName + "&game=" + gameType +
//                        "&daysPerMove=" + timeout + "&rated=" + rated +"&invitationRestriction=" +
//                        restriction + "&playAs=" + playAs + "&privateGame=" + privateGame;
                String urlParameters  = "mobile=&invitee=" + opponentName + "&game=" + gameType +
                        "&daysPerMove=" + timeout + "&rated=" + rated +"&invitationRestriction=" +
                        restriction + "&playAs=" + playAs + "&privateGame=" + privateGame
                        +"&name2="+PentePlayer.mPlayerName+"&password2="+ PentePlayer.mPassword;
                byte[] postData       = new byte[0];
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                    postData = urlParameters.getBytes( StandardCharsets.UTF_8 );
                }
                int    postDataLength = postData.length;
                String request        = "https://www.pente.org/gameServer/tb/newGame";
                if (PentePlayer.development) {
                    request        = "https://development.pente.org/gameServer/tb/newGame";
                }
                URL    url            = new URL( request );
                HttpsURLConnection conn= (HttpsURLConnection) url.openConnection();
                String cookies = CookieManager.getInstance().getCookie("https://www.pente.org/");
                if (cookies != null) {
                    String[] splitCookie = cookies.split(";");
                    String cookieStr = "";
                    for (String item: splitCookie) {
                        if (item.contains("name2") || item.contains("password2")) {
                            cookieStr += item + ";";
                        }
                    }
                    conn.setRequestProperty("Cookie", cookieStr);
//                    System.out.println("cookieStr: " +cookieStr);
                }
                conn.setDoOutput( true );
                conn.setInstanceFollowRedirects( false );
                conn.setRequestMethod( "POST" );
                conn.setRequestProperty( "Content-Type", "application/x-www-form-urlencoded");
                conn.setRequestProperty( "charset", "utf-8");
                conn.setRequestProperty( "Content-Length", Integer.toString( postDataLength ));
                conn.setUseCaches( false );
                try {
                    DataOutputStream wr = new DataOutputStream( conn.getOutputStream());
                    wr.write( postData );
                } catch (Exception e) {
                    e.printStackTrace();
                    return  false;
                }

                StringBuilder output = new StringBuilder();
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
//                System.out.println("output===============" + br);
                String line = "";
                while((line = br.readLine()) != null ) {
                    output.append(line + System.getProperty("line.separator"));
                }
                br.close();

                output.append(System.getProperty("line.separator") + "Response " + System.getProperty("line.separator") + System.getProperty("line.separator"));
//                System.out.println(output);

                if (output.indexOf("Creating set failed: Player not found: " + opponentName) > -1) {
                    return false;
                }
                if (!opponentName.equals("")) {
                    PrefUtils.savePlayerToPrefs(InvitationActivity.this, opponentName);
                }

                return true;

            } catch (IOException e1) {
                e1.printStackTrace();
                return  false;
            }
//            for (String credential : DUMMY_CREDENTIALS) {
//                String[] pieces = credential.split(":");
//                if (pieces[0].equals(mEmail)) {
//                    // Account exists, return true if the password matches.
//                    return pieces[1].equals(mPassword);
//                }
//            }


            // TODO: register the new account here.
//            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            if (success) {
                if ("".equals(opponentName)) {
                    int remainingCredit = PrefUtils.getIntFromPrefs(InvitationActivity.this, PrefUtils.PREFS_OPENINVITATIONCREDIT_KEY, 2) + 2;
                    PrefUtils.saveIntToPrefs(InvitationActivity.this, PrefUtils.PREFS_OPENINVITATIONCREDIT_KEY, remainingCredit);
                } else {
                    PrefUtils.savePlayerToPrefs(InvitationActivity.this, opponentName);
                }
                PrefUtils.saveIntToPrefs(InvitationActivity.this, PrefUtils.PREFS_INVITATIONDAYS_KEY, ((Spinner) findViewById(R.id.timeoutSpinner)).getSelectedItemPosition());
                PrefUtils.saveIntToPrefs(InvitationActivity.this, PrefUtils.PREFS_INVITATIONGAME_KEY, ((Spinner) findViewById(R.id.gameTypeSpinner)).getSelectedItemPosition());
                PrefUtils.saveIntToPrefs(InvitationActivity.this, PrefUtils.PREFS_INVITATIONRESTRICTION_KEY, ((Spinner) findViewById(R.id.restrictionSpinner)).getSelectedItemPosition());
                finish();
            } else {
                ((AutoCompleteTextView) findViewById(R.id.opponent)).setError("username does not exist");
            }
//            mAuthTask = null;
//            showProgress(false);
//
//            if (success) {
//                PrefUtils.saveToPrefs(LoginActivity.this, PrefUtils.PREFS_LOGIN_USERNAME_KEY, mEmail);
//                PrefUtils.saveToPrefs(LoginActivity.this, PrefUtils.PREFS_LOGIN_PASSWORD_KEY, mPassword);
//                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
//                PentePlayer player = new PentePlayer(mEmail, mPassword);
//                intent.putExtra("pentePlayer", player);
//                startActivity(intent);
////                finish();
//            } else {
//                mPasswordView.setError(getString(R.string.error_incorrect_password));
//                mPasswordView.requestFocus();
//            }
        }

        @Override
        protected void onCancelled() {
//            mAuthTask = null;
//            showProgress(false);
        }
    }

}
