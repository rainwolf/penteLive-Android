package be.submanifold.pentelive;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.util.DisplayMetrics;

import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import androidx.appcompat.widget.Toolbar;

import android.text.method.ScrollingMovementMethod;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
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
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Register new account");
        setSupportActionBar(toolbar);

        findViewById(R.id.viewPolicy).setOnClickListener(v -> {
            DisplayMetrics metrics = getResources().getDisplayMetrics();
            Point size = new Point(metrics.widthPixels, metrics.heightPixels);

            // PopupWindow content: no parent at inflation time.
            @SuppressLint("InflateParams")
            View policyView = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.popupwindowinformation, null, false);
            policyView.setBackgroundColor(Color.WHITE);
            PopupWindow messageWindow = new PopupWindow(policyView, size.x - 50, size.y * 3 / 4, true);
            messageWindow.setFocusable(true);
            messageWindow.setOutsideTouchable(true);
            messageWindow.setBackgroundDrawable(ContextCompat.getDrawable(RegisterActivity.this, R.drawable.border));
            messageWindow.showAtLocation(findViewById(R.id.registerView), Gravity.TOP, 0, 260);
            ((TextView) policyView.findViewById(R.id.informationView)).setText(R.string.rated_play_policy_text);
            ((TextView) policyView.findViewById(R.id.informationView)).setMovementMethod(new ScrollingMovementMethod());
        });
        findViewById(R.id.registerButton).setOnClickListener(v -> attemptRegistration());
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
        String username = ((EditText) findViewById(R.id.username)).getText().toString().toLowerCase(java.util.Locale.ROOT);
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

    public class RegisterTask extends BackgroundTask<Void, Boolean> {

        private final String username;
        private final String password;
        private String email;
        private String response;


        RegisterTask(String username, String password, String email) {
            this.username = username.toLowerCase(java.util.Locale.ROOT);
            this.password = password;
            try {
                this.email = URLEncoder.encode(email, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            try {
                String urlParameters = "name=" + username + "&registerPassword=" + password + "&registerPasswordConfirm=" + password + "&registerEmail=" + email + "&agreePolicy=Y";
                byte[] postData = new byte[0];
                postData = urlParameters.getBytes(StandardCharsets.UTF_8);
                int postDataLength = postData.length;
                String request = "https://www.pente.org/join";
                if (PentePlayer.development) {
                    request = "https://10.0.2.2/join";
                }
                URL url = new URL(request);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
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
                String line = "";
                while ((line = br.readLine()) != null) {
                    output.append(line + System.getProperty("line.separator"));
                }
                br.close();

                output.append(System.getProperty("line.separator") + "Response " + System.getProperty("line.separator") + System.getProperty("line.separator"));
                response = output.toString();

                if (response.contains("Registration failed: Requested name " + username + " is already taken, please choose another.")) {
                    return false;
                }

            } catch (IOException e1) {
                e1.printStackTrace();
                return false;
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
                if (response == null) {
                    Toast.makeText(RegisterActivity.this, "Something went wrong, please try again/later.",
                            Toast.LENGTH_LONG).show();
                } else if (response.indexOf("Registration failed: Requested name " + username + " is already taken, please choose another.") > -1) {
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
