package be.submanifold.pentelive;

import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.IntentCompat;

import android.os.Bundle;

import androidx.appcompat.widget.Toolbar;

import android.graphics.Color;
import android.graphics.Typeface;
import android.content.res.TypedArray;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.QuoteSpan;
import android.text.style.StyleSpan;
import android.text.util.Linkify;
import android.util.TypedValue;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEventListener;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.util.Date;

import java.util.List;

import androidx.appcompat.app.AlertDialog;

import be.submanifold.pentelive.blocked.BlockedPlayers;
import be.submanifold.pentelive.blocked.BlockedPlayersClient;
import be.submanifold.pentelive.net.AuthedHttp;

public class ReplyMessageActivity extends AppCompatActivity {

    private static final String TAG = "ReplyMessageActivity";

    private String recipient;
    private String subject;
    /** True once the sender is known to be chat-blocked; hides reply UI and the block item. */
    private boolean senderBlocked;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reply_message);
        Toolbar toolbar = findViewById(R.id.toolbar);

        final Message message = IntentCompat.getParcelableExtra(getIntent(), "message", Message.class);
        recipient = message.getAuthor();
        toolbar.setTitle("To: " + recipient);
        ((EditText) findViewById(R.id.subject)).setText(message.getSubject());
        setSupportActionBar(toolbar);

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) findViewById(R.id.sendButton).getLayoutParams();
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        findViewById(R.id.sendButton).setLayoutParams(params);

        Button button = findViewById(R.id.sendButton);
        if (button != null) button.setOnClickListener(v -> {
            if (((EditText) findViewById(R.id.subject)).getText().toString().equals("")) {
                Toast.makeText(ReplyMessageActivity.this, getString(R.string.enter_subject),
                        Toast.LENGTH_LONG).show();
                return;
            }
            String subject = ((EditText) findViewById(R.id.subject)).getText().toString();
            if (subject.indexOf("Re:") != 0) {
                subject = "Re: " + subject;
            }
            SendMessageTask submitTask = new SendMessageTask(recipient, subject, ((EditText) findViewById(R.id.reply)).getText().toString(), message.getMessageID());
            submitTask.execute((Void) null);
        });

        // reply input grows with its text up to half of the space it shares with the message
        final EditText replyInput = findViewById(R.id.reply);
        findViewById(R.id.messageArea).addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            int half = (bottom - top) / 2;
            if (replyInput.getMaxHeight() != half) {
                replyInput.setMaxHeight(half);
            }
        });

        final TextView messageTextView = findViewById(R.id.message);
//        messageTextView.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                System.out.println("click + " + messageTextView.getSelectionStart() + " + " + messageTextView.getSelectionEnd());
//                return;
////                if (messageTextView.getSelectionStart() != -1 && messageTextView.getSelectionEnd() != -1) {
////                    String url = messageTextView.getText().toString().substring(messageTextView.getSelectionStart(), messageTextView.getSelectionEnd());
////                    System.out.println(url);
////                    if (url.indexOf("viewLiveGame") > 0) {
////                        String gameID = url.substring(url.indexOf("g=") + 2);
////                        System.out.println(gameID);
////                        Game game = new Game(gameID, null, null, null, null, null, null, null, null, null, null);
////                        Intent intent = new Intent(getApplicationContext(), BoardActivity.class);
////                        intent.putExtra("game", game);
////                        startActivity(intent);
////                    }
////                    if (url.indexOf("new.jsp?game=") > 0) {
////
////                    }
////
////
////                }
//            }
//        });
//        messageTextView.setMovementMethod(LinkMovementMethod.getInstance());
        loadSenderBlocked(recipient);
        LoadMessageTask loadTask = new LoadMessageTask(message.getMessageID());
        loadTask.execute((Void) null);

        toolbar.setOnMenuItemClickListener(menuItem -> {
            int id = menuItem.getItemId();
            if (id == R.id.action_trash) {
                DeleteMessageTask deleteTask = new DeleteMessageTask(message.getMessageID());
                deleteTask.execute((Void) null);
                return true;
            } else if (id == R.id.action_challenge) {
                Intent intent = new Intent(getApplicationContext(), InvitationActivity.class);
                intent.putExtra("opponent", recipient);
                startActivity(intent);
                return true;
            } else if (id == R.id.action_block) {
                confirmBlock();
                return true;
            }

            return false;
        });

        KeyboardVisibilityEvent.setEventListener(
                ReplyMessageActivity.this,
                isOpen -> {
                    // some code depending on keyboard visiblity status

                    if (isOpen) {
                        findViewById(R.id.sendButton).setVisibility(View.GONE);
                        if (PentePlayer.mShowAds) {
//                                ((AdView) findViewById(R.id.adView)).setVisibility(View.GONE);
                        }
                    } else if (!senderBlocked) {
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.message_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem block = menu.findItem(R.id.action_block);
        if (block != null) {
            block.setVisible(!senderBlocked);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    private void applyBlockedState() {
        senderBlocked = true;
        findViewById(R.id.reply).setVisibility(View.GONE);
        findViewById(R.id.sendButton).setVisibility(View.GONE);
        invalidateOptionsMenu();
    }

    /** Loads the block list; on failure senderBlocked stays false. */
    private void loadSenderBlocked(final String author) {
        new BackgroundTask<Void, List<BlockedPlayers.IgnoreEntry>>() {
            @Override
            protected List<BlockedPlayers.IgnoreEntry> doInBackground(Void... params) {
                try {
                    return BlockedPlayersClient.load();
                } catch (IOException e) {
                    return null;
                }
            }

            @Override
            protected void onPostExecute(List<BlockedPlayers.IgnoreEntry> entries) {
                if (isFinishing() || isDestroyed() || entries == null) {
                    return;
                }
                if (BlockedPlayers.isChatBlocked(author, entries)) {
                    applyBlockedState();
                }
            }
        }.execute();
    }

    private void confirmBlock() {
        final String author = recipient;
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.block_dialog_title, author))
                .setMessage(getString(R.string.block_message_dialog_text, author))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.block, (d, w) -> block(author))
                .create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
    }

    private void block(final String author) {
        new BackgroundTask<Void, IOException>() {
            @Override
            protected IOException doInBackground(Void... params) {
                try {
                    BlockedPlayersClient.block(author, false, true);
                    return null;
                } catch (IOException e) {
                    return e;
                }
            }

            @Override
            protected void onPostExecute(IOException error) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                if (error == null) {
                    applyBlockedState();
                    return;
                }
                String text = (error instanceof BlockedPlayersClient.Failure)
                        ? error.getMessage() : null;
                new AlertDialog.Builder(ReplyMessageActivity.this)
                        .setTitle(getString(R.string.block_error_title, author))
                        .setMessage(text != null ? text : getString(R.string.try_again))
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            }
        }.execute();
    }

    protected void makeLinkClickable(SpannableStringBuilder strBuilder, final URLSpan span) {
        int start = strBuilder.getSpanStart(span);
        int end = strBuilder.getSpanEnd(span);
        int flags = strBuilder.getSpanFlags(span);
        ClickableSpan clickable = new ClickableSpan() {
            public void onClick(View view) {

                String url = span.getURL();
//                    System.out.println(url);
                if (url.indexOf("viewLiveGame") > 0) {
                    String gameID = url.substring(url.indexOf("g=") + 2);
                    System.out.println(gameID);
                    Game game = new Game(gameID, null, null, null, null, null, null, null, null, null, null);
                    game.setActive(false);
                    Intent intent = new Intent(getApplicationContext(), BoardActivity.class);
                    intent.putExtra("game", game);
                    startActivity(intent);
                } else if (url.indexOf("new.jsp?game=") > 0) {
                    Intent intent = new Intent(getApplicationContext(), InvitationActivity.class);
                    intent.putExtra("opponent", url.substring(url.indexOf("invitee=") + 8));
                    intent.putExtra("gameType", url.substring(url.indexOf("game=") + 5, url.indexOf("game=") + 7));
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(getApplicationContext(), WebViewActivity.class);
                    intent.putExtra("url", url);
                    startActivity(intent);
                }
            }
        };
        strBuilder.setSpan(clickable, start, end, flags);
        strBuilder.removeSpan(span);
    }

    private class SendMessageTask extends BackgroundTask<Void, Boolean> {

        private final String recipient;
        private String subject;
        private String message;
        private final String replyToMid;

        SendMessageTask(String recipient, String subject, String message, String replyToMid) {
            this.replyToMid = replyToMid;
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
        protected Boolean doInBackground(Void... params) {
            // TODO: attempt authentication against a network service.

            try {
//                String urlParameters  = "command=create&to=" + recipient + "&subject=" + subject + "&body=" + message + "&mobile=";
                String urlParameters = "command=create&to=" + recipient + "&subject=" + subject + "&body=" + message + "&mobile=" + "&replyTo=" + replyToMid;
                AuthedHttp.Reply reply = AuthedHttp.shared().postForm("/gameServer/mymessages", urlParameters);
                String output = reply.body;

                return output.indexOf("Error: Player " + recipient + " not found.") <= -1;

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
                PrefUtils.savePlayerToPrefs(ReplyMessageActivity.this, recipient);
                finish();
            } else {
                ((AutoCompleteTextView) findViewById(R.id.recipient)).setError(getString(R.string.no_such_user));
            }
        }

        @Override
        protected void onCancelled() {
        }
    }

    private class LoadMessageTask extends BackgroundTask<Void, Boolean> {

        private final String messageID;
        private JsonModels.MessageThreadResponse thread;

        LoadMessageTask(String messageID) {
            this.messageID = messageID;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                AuthedHttp.Reply reply = AuthedHttp.shared().get("/gameServer/messageJson?mid=" + messageID);
                if (reply.code != 200) {
                    RedactingLog.e(TAG, "loading the message failed, response code " + reply.code, null);
                    return false;
                }
                thread = MessageThread.parse(reply.body);
                if (thread == null) {
                    RedactingLog.e(TAG, "loading the message failed, unexpected response", null);
                    return false;
                }
                return true;
            } catch (IOException e1) {
                RedactingLog.e(TAG, "loading message failed", e1);
                return false;
            }
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            if (success) {
                showThread(thread);
            }
        }

        @Override
        protected void onCancelled() {
        }
    }

    private void showThread(JsonModels.MessageThreadResponse thread) {
        final TextView messageView = findViewById(R.id.message);
        int secondary = Color.GRAY;
        TypedArray ta = getTheme().obtainStyledAttributes(new int[]{android.R.attr.textColorSecondary});
        try {
            secondary = ta.getColor(0, secondary);
        } finally {
            ta.recycle();
        }

        int quoteIndentPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
        DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
        SpannableStringBuilder builder = new SpannableStringBuilder();
        for (JsonModels.MessageThreadEntry entry : MessageThread.entries(thread)) {
            boolean quoted = entry != thread.message;
            if (builder.length() > 0) {
                builder.append("\n\n");
            }
            int entryStart = builder.length();
            String header = entry.from + " \u00b7 " + dateFormat.format(new Date(entry.date));
            builder.append(header);
            builder.setSpan(new StyleSpan(Typeface.BOLD), entryStart, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            // body is raw user text: appended as plain text (never HTML), newlines preserved
            builder.append("\n").append(entry.body == null ? "" : entry.body);
            if (quoted) {
                builder.setSpan(new ForegroundColorSpan(secondary), entryStart, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                builder.setSpan(new LeadingMarginSpan.Standard(quoteIndentPx), entryStart, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                builder.setSpan(new QuoteSpan(secondary), entryStart, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                builder.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.message_text)), entryStart, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        Linkify.addLinks(builder, Linkify.WEB_URLS);
        for (URLSpan span : builder.getSpans(0, builder.length(), URLSpan.class)) {
            makeLinkClickable(builder, span);
        }
        messageView.setText(builder);
        messageView.setMovementMethod(LinkMovementMethod.getInstance());
        messageView.scrollTo(0, 0);
    }

    private class DeleteMessageTask extends BackgroundTask<Void, Boolean> {

        private final String messageID;

        DeleteMessageTask(String messageID) {
            this.messageID = messageID;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // TODO: attempt authentication against a network service.

            try {
//                String urlParameters  ="command=delete&mid=" + messageID + "&mobile=";
                String urlParameters = "command=delete&mid=" + messageID + "&mobile=";
                AuthedHttp.shared().postForm("/gameServer/mymessages", urlParameters);

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
                finish();
            }
        }

        @Override
        protected void onCancelled() {
        }
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }


    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }


}
