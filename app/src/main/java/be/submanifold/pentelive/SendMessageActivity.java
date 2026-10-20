package be.submanifold.pentelive;

import android.graphics.Color;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import androidx.appcompat.widget.Toolbar;

import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

import be.submanifold.pentelive.kids.KidAccounts;
import be.submanifold.pentelive.kids.KidAccountsClient;
import be.submanifold.pentelive.net.AuthedHttp;


public class SendMessageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_message);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.new_message));
        toolbar.setTitleTextColor(Color.WHITE);
        setSupportActionBar(toolbar);

        final AutoCompleteTextView actv = findViewById(R.id.recipient);
        boolean kid = PentePlayer.isKid();
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,
                kid ? new ArrayList<String>() : new ArrayList<String>(PrefUtils.getPlayers(SendMessageActivity.this)));
        actv.setAdapter(adapter);
        if (kid) {
            new LoadKidRecipientsTask(adapter).execute((Void) null);
        }

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) findViewById(R.id.sendButton).getLayoutParams();
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        findViewById(R.id.sendButton).setLayoutParams(params);

        Button button = findViewById(R.id.sendButton);
        if (button != null) button.setOnClickListener(v -> {
            if (actv.getText().toString().equals("")) {
                Toast.makeText(SendMessageActivity.this, getString(R.string.enter_recipient),
                        Toast.LENGTH_LONG).show();
                return;
            }
            if (((EditText) findViewById(R.id.subject)).getText().toString().equals("")) {
                Toast.makeText(SendMessageActivity.this, getString(R.string.enter_subject),
                        Toast.LENGTH_LONG).show();
                return;
            }
            SendMessageTask submitTask = new SendMessageTask(actv.getText().toString(), ((EditText) findViewById(R.id.subject)).getText().toString(), ((EditText) findViewById(R.id.message)).getText().toString());
            submitTask.execute((Void) null);
        });

        KeyboardVisibilityEvent.setEventListener(
                SendMessageActivity.this,
                isOpen -> {
                    // some code depending on keyboard visiblity status

                    if (isOpen) {
                        findViewById(R.id.sendButton).setVisibility(View.GONE);
                        if (PentePlayer.mShowAds) {
//                                ((AdView) findViewById(R.id.adView)).setVisibility(View.GONE);
                        }
                    } else {
                        findViewById(R.id.sendButton).setVisibility(View.VISIBLE);
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

    private class LoadKidRecipientsTask extends BackgroundTask<Void, KidAccounts.Reply> {

        private final ArrayAdapter<String> adapter;

        LoadKidRecipientsTask(ArrayAdapter<String> adapter) {
            this.adapter = adapter;
        }

        @Override
        protected KidAccounts.Reply doInBackground(Void... params) {
            try {
                return KidAccountsClient.load();
            } catch (IOException e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(final KidAccounts.Reply reply) {
            if (reply == null || isFinishing() || isDestroyed()) {
                return;
            }
            adapter.clear();
            adapter.addAll(KidAccounts.allowedRecipients(reply));
            adapter.notifyDataSetChanged();
        }

        @Override
        protected void onCancelled() {
        }
    }

    private class SendMessageTask extends BackgroundTask<Void, MessageSendReply.Outcome> {

        private final String recipient;
        private String subject;
        private String message;

        SendMessageTask(String recipient, String subject, String message) {
            this.recipient = recipient.toLowerCase(java.util.Locale.ROOT);
            try {
                this.message = URLEncoder.encode(message, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                this.message = "";
                e.printStackTrace();
            }
            try {
                if ("".equals(subject)) {
                    this.subject = URLEncoder.encode("(no subject)", "UTF-8");
                } else {
                    this.subject = URLEncoder.encode(subject, "UTF-8");
                }
            } catch (UnsupportedEncodingException e) {
                this.subject = "nosubject";
                e.printStackTrace();
            }
        }

        @Override
        protected MessageSendReply.Outcome doInBackground(Void... params) {
            // TODO: attempt authentication against a network service.

            try {
//                String urlParameters  = "command=create&to=" + recipient + "&subject=" + subject + "&body=" + message + "&mobile=";
                String urlParameters = "command=create&to=" + recipient + "&subject=" + subject + "&body=" + message + "&mobile=";
                AuthedHttp.Reply reply = AuthedHttp.shared().postForm("/gameServer/mymessages", urlParameters);
                return MessageSendReply.classify(reply.body, recipient);

            } catch (IOException e1) {
                e1.printStackTrace();
                return MessageSendReply.Outcome.NO_SUCH_USER;
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
        protected void onPostExecute(final MessageSendReply.Outcome outcome) {
            if (outcome == MessageSendReply.Outcome.SENT) {
                PrefUtils.savePlayerToPrefs(SendMessageActivity.this, recipient);
                finish();
            } else {
                int error = R.string.no_such_user;
                if (outcome == MessageSendReply.Outcome.NOT_APPROVED) {
                    error = R.string.kid_not_approved_recipient;
                } else if (outcome == MessageSendReply.Outcome.IGNORED) {
                    error = R.string.player_ignoring_messages;
                }
                ((AutoCompleteTextView) findViewById(R.id.recipient)).setError(getString(error));
            }
        }

        @Override
        protected void onCancelled() {
        }
    }

}
