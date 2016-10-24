package be.submanifold.pentelive;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

public class InviteAIActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invite_ai);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Play Bruce Cropley's AI");
        toolbar.setTitleTextColor(Color.WHITE);
        setSupportActionBar(toolbar);

        Spinner spinner = (Spinner) findViewById(R.id.gameTypeSpinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.ai_game_types_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(PrefUtils.getIntFromPrefs(InviteAIActivity.this, PrefUtils.PREFS_AIINVITATIONGAME_KEY, 0));
        spinner = (Spinner) findViewById(R.id.difficultySpinner);
        adapter = ArrayAdapter.createFromResource(this,
                R.array.ai_difficulty_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(PrefUtils.getIntFromPrefs(InviteAIActivity.this, PrefUtils.PREFS_AIINVITATIONDIFFICULTY_KEY, 0));

        Button button = (Button) findViewById(R.id.sendInvitationButton);
        if (button != null) button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String gameType = "";
                switch (((Spinner) findViewById(R.id.gameTypeSpinner)).getSelectedItemPosition()) {
                    case 0: gameType = "51"; break;
                    case 1: gameType = "55"; break;
                }
                String rated = ((ToggleButton) findViewById(R.id.ratedToggleButton)).isChecked()?"Y":"N";
                String playAs = ((ToggleButton) findViewById(R.id.playAsToggleButton)).isChecked()?"2":"1";
                String difficulty =  "" + (((Spinner) findViewById(R.id.difficultySpinner)).getSelectedItemPosition()+1);
                SendInvitationTask submitTask = new SendInvitationTask(gameType, rated, difficulty, playAs);
                submitTask.execute((Void) null);
            }
        });

        ((ToggleButton) findViewById(R.id.ratedToggleButton)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if ((isChecked)) {
                    ((TextView) findViewById(R.id.playAsLabel)).setVisibility(View.GONE);
                    ((ToggleButton) findViewById(R.id.playAsToggleButton)).setVisibility(View.GONE);
                } else {
                    ((TextView) findViewById(R.id.playAsLabel)).setVisibility(View.VISIBLE);
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


    public class SendInvitationTask extends AsyncTask<Void, Void, Boolean> {

        private final String gameType;
        private final String difficulty;
        private final String rated;
        private final String playAs;

        SendInvitationTask(String gameType, String rated, String difficulty, String playAs) {
            this.gameType = gameType;
            this.difficulty = difficulty;
            this.rated = rated;
            this.playAs = playAs;
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

//                String urlParameters  = "mobile=&difficulty=" + difficulty + "&invitee=computer&game=" + gameType +
//                        "&daysPerMove=30&rated=" + rated +"&invitationRestriction=A&playAs=" + playAs + "&privateGame=N";
                String urlParameters  = "mobile=&difficulty=" + difficulty + "&invitee=computer&game=" + gameType +
                        "&daysPerMove=30&rated=" + rated +"&invitationRestriction=A&playAs=" + playAs + "&privateGame=N"
                        +"&name2="+PentePlayer.mPlayerName+"&password2="+ PentePlayer.mPassword;
                byte[] postData       = new byte[0];
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                    postData = urlParameters.getBytes( StandardCharsets.UTF_8 );
                }
                int    postDataLength = postData.length;
                String request        = "https://www.pente.org/gameServer/tb/newGame";
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
                System.out.println("output===============" + br);
                String line = "";
                while((line = br.readLine()) != null ) {
                    output.append(line + System.getProperty("line.separator"));
                }
                br.close();

                output.append(System.getProperty("line.separator") + "Response " + System.getProperty("line.separator") + System.getProperty("line.separator"));
                System.out.println(output);

                if (output.indexOf("against the AI player. You can start a new one after finishing the current one") > -1) {
                    return false;
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
                PrefUtils.saveIntToPrefs(InviteAIActivity.this, PrefUtils.PREFS_AIINVITATIONGAME_KEY, ((Spinner) findViewById(R.id.gameTypeSpinner)).getSelectedItemPosition());
                PrefUtils.saveIntToPrefs(InviteAIActivity.this, PrefUtils.PREFS_AIINVITATIONDIFFICULTY_KEY, ((Spinner) findViewById(R.id.difficultySpinner)).getSelectedItemPosition());
                finish();
            } else {
                Toast.makeText(InviteAIActivity.this, "Try again after you finish your current game against the AI player.",
                        Toast.LENGTH_LONG).show();
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
