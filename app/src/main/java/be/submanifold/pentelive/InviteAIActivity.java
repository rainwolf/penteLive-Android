package be.submanifold.pentelive;

import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;

import be.submanifold.pentelive.net.AuthedHttp;


public class InviteAIActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invite_ai);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Bruce Cropley's AI");
        toolbar.setTitleTextColor(Color.WHITE);
        setSupportActionBar(toolbar);

        Spinner spinner = findViewById(R.id.gameTypeSpinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.ai_game_types_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(PrefUtils.getIntFromPrefs(InviteAIActivity.this, PrefUtils.PREFS_AIINVITATIONGAME_KEY, 0));
        spinner = findViewById(R.id.difficultySpinner);
        adapter = ArrayAdapter.createFromResource(this,
                R.array.ai_difficulty_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(PrefUtils.getIntFromPrefs(InviteAIActivity.this, PrefUtils.PREFS_AIINVITATIONDIFFICULTY_KEY, 0));

        Button button = findViewById(R.id.sendInvitationButton);
        if (button != null) button.setOnClickListener(v -> {
            String gameType = "";
            switch (((Spinner) findViewById(R.id.gameTypeSpinner)).getSelectedItemPosition()) {
                case 0:
                    gameType = "51";
                    break;
                case 1:
                    gameType = "55";
                    break;
            }
            String rated = ((ToggleButton) findViewById(R.id.ratedToggleButton)).isChecked() ? "Y" : "N";
            String playAs = ((ToggleButton) findViewById(R.id.playAsToggleButton)).isChecked() ? "2" : "1";
            String difficulty = "" + (((Spinner) findViewById(R.id.difficultySpinner)).getSelectedItemPosition() + 1);
            SendInvitationTask submitTask = new SendInvitationTask(gameType, rated, difficulty, playAs);
            submitTask.execute((Void) null);
        });

        ((ToggleButton) findViewById(R.id.ratedToggleButton)).setOnCheckedChangeListener((buttonView, isChecked) -> {
            if ((isChecked)) {
                findViewById(R.id.playAsLabel).setVisibility(View.GONE);
                findViewById(R.id.playAsToggleButton).setVisibility(View.GONE);
            } else {
                findViewById(R.id.playAsLabel).setVisibility(View.VISIBLE);
                findViewById(R.id.playAsToggleButton).setVisibility(View.VISIBLE);
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


    public class SendInvitationTask extends BackgroundTask<Void, Boolean> {

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
//                String urlParameters  = "mobile=&difficulty=" + difficulty + "&invitee=computer&game=" + gameType +
//                        "&daysPerMove=30&rated=" + rated +"&invitationRestriction=A&playAs=" + playAs + "&privateGame=N";
                String urlParameters = "mobile=&difficulty=" + difficulty + "&invitee=computer&game=" + gameType +
                        "&daysPerMove=30&rated=" + rated + "&invitationRestriction=A&playAs=" + playAs + "&privateGame=N";
                AuthedHttp.Reply reply = AuthedHttp.shared().postForm("/gameServer/tb/newGame", urlParameters);
                String output = reply.body;
                System.out.println(output);

                return output.indexOf("against the AI player. You can start a new one after finishing the current one") <= -1;

            } catch (IOException e1) {
                e1.printStackTrace();
                return false;
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
                Toast.makeText(InviteAIActivity.this, getString(R.string.finish_some_ai_games_first),
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
