package be.submanifold.pentelive;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.os.AsyncTask;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class RegisterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Register new account");
        setSupportActionBar(toolbar);

        ((Button) findViewById(R.id.viewPolicy)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Display display = getWindowManager().getDefaultDisplay();
                Point size = new Point();
                display.getSize(size);

                View policyView = ((LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.popupwindowinformation, null, false);
                policyView.setBackgroundColor(Color.WHITE);
                PopupWindow messageWindow = new PopupWindow(policyView, size.x - 50, size.y*3/4, true );
                messageWindow.setFocusable(true);
                messageWindow.setOutsideTouchable(true);
                messageWindow.setBackgroundDrawable(ContextCompat.getDrawable(RegisterActivity.this, R.drawable.border));
                messageWindow.showAtLocation(getCurrentFocus(), Gravity.TOP, 0, 260);
                ((TextView) policyView.findViewById(R.id.informationView)).setText("Pente.org maintains a rating for you when you play \"rated\" games.  The ratings system is important to help you determine your skill level and to help you find worthy opponents.  Pente.org attempts to ensure that ratings accurately reflect a players skill, and therefore certain guidelines must be followed by all players!\n" +
                        "          \n" +
                        "1. Play rated games using only your brain.  Do not play with any outside assistance.  Just to be clear, here are some examples of what you should NOT do: use another pente board to examine future positions, use a game database to lookup the current or future positions, use a computer opponent to find moves, consult written notes or books. \n" +
                        "2. Play rated games at Pente.org with only one user account. Do not create multiple users at and play rated games with them.\n" +
                        "3. When playing games, you can request to undo your last move, your opponent can choose to accept or deny this request (Pente.org does not care, it is up to you). If you do not plan to accept undo's, you should mention this to your opponent before starting a rated game.\n" +
                        "4. When watching a rated game, do not make comments about specific game moves, this could affect the outcome of the game.  There is plenty of time for analysis after the game.\n" +
                        "5.If your opponent is disconnected from the internet, Pente.org allows him/her 7 minutes to return to resume the game.  After that point you may decide to cancel the game or force your opponent to resign.  This feature was implemented to stop other players from bailing out of a losing game.  However, do not abuse this feature, if you are sure you will lose, you should resign the game.  Do not force your opponent to resign unless you are absolutely sure you will win.\n" +
                        "\n" +
                        "That's it, and remember to have fun of course!");
                ((TextView) policyView.findViewById(R.id.informationView)).setMovementMethod(new ScrollingMovementMethod());
            }
        });
        ((Button) findViewById(R.id.registerButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptRegistration();
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

    private void attemptRegistration() {
        String username = ((EditText) findViewById(R.id.username)).getText().toString().toLowerCase();
        if (username.length() < 5 || username.length() > 10) {
            ((EditText) findViewById(R.id.username)).setError(getString(R.string.username_5_10));
            return;
        }
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            ((EditText) findViewById(R.id.username)).setError(getString(R.string.username_only));
            return;
        }
        String password = ((EditText) findViewById(R.id.password)).getText().toString();
        String password2 = ((EditText) findViewById(R.id.repeatPassword)).getText().toString();
        if (password.length() < 5 || password.length() > 16) {
            ((EditText) findViewById(R.id.password)).setError(getString(R.string.password_6_16));
            return;
        }
        if (!password.matches("^[a-zA-Z0-9_]+$")) {
            ((EditText) findViewById(R.id.password)).setError(getString(R.string.password_only));
            return;
        }
        if (!password.equals(password2)) {
            ((EditText) findViewById(R.id.repeatPassword)).setError(getString(R.string.password_no_match));
            return;
        }
        if (!((ToggleButton) findViewById(R.id.ratedToggleButton)).isChecked()) {
            Toast.makeText(RegisterActivity.this, getString(R.string.accept_policy),
                    Toast.LENGTH_LONG).show();
            return;
        }

        (new RegisterTask(username, password, ((EditText) findViewById(R.id.email)).getText().toString())).execute((Void) null);


    }

    public class RegisterTask extends AsyncTask<Void, Void, Boolean> {

        private String username, password, email;
        private String response;


        RegisterTask(String username, String password, String email) {
            this.username = username.toLowerCase();
            this.password = password;
            try {
                this.email = URLEncoder.encode(email,"UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            try {
                String urlParameters  = "name=" + username + "&registerPassword=" + password + "&registerPasswordConfirm=" + password + "&registerEmail=" + email + "&agreePolicy=Y";
                byte[] postData       = new byte[0];
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                    postData = urlParameters.getBytes( StandardCharsets.UTF_8 );
                }
                int    postDataLength = postData.length;
                String request        = "https://www.pente.org/join";
                if (PentePlayer.development) {
                    request        = "https://development.pente.org/join";
                }
                URL url            = new URL( request );
                HttpURLConnection conn= (HttpURLConnection) url.openConnection();
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
                response = output.toString();

                if (response.indexOf("Registration failed: Requested name " + username + " is already taken, please choose another.") > -1) {
                    return false;
                }

            } catch (IOException e1) {
                e1.printStackTrace();
                return  false;
            }

            return true;
        }
        @Override
        protected void onPostExecute(final Boolean success) {
            if (success) {
                PrefUtils.saveToPrefs(RegisterActivity.this, PrefUtils.PREFS_LOGIN_USERNAME_KEY, username);
                PrefUtils.saveToPrefs(RegisterActivity.this, PrefUtils.PREFS_LOGIN_PASSWORD_KEY, password);
                finish();
            } else {
                if (response.indexOf("Registration failed: Requested name " + username + " is already taken, please choose another.") > -1) {
                    Toast.makeText(RegisterActivity.this, getString(R.string.username_taken, username),
                            Toast.LENGTH_LONG).show();
                }
            }
        }

        @Override
        protected void onCancelled() {
        }
    }


}
