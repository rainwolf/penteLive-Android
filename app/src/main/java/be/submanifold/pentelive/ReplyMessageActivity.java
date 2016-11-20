package be.submanifold.pentelive;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEventListener;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.net.ssl.HttpsURLConnection;

public class ReplyMessageActivity extends AppCompatActivity {

    private String recipient;
    private String subject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reply_message);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        final Message message = getIntent().getParcelableExtra("message");
        recipient = message.getAuthor();
        toolbar.setTitle("To: " + recipient);
        ((EditText) findViewById(R.id.subject)).setText(message.getSubject());
        setSupportActionBar(toolbar);

        if (PentePlayer.mShowAds) {
            ((AdView) findViewById(R.id.adView)).loadAd(new AdRequest.Builder().build());
        } else {
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) ((Button) findViewById(R.id.sendButton)).getLayoutParams();
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            ((Button) findViewById(R.id.sendButton)).setLayoutParams(params);
            ((AdView) findViewById(R.id.adView)).setVisibility(View.GONE);
        }

        Button button = (Button) findViewById(R.id.sendButton);
        if (button != null) button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (((EditText) findViewById(R.id.subject)).getText().toString().equals("")) {
                    Toast.makeText(ReplyMessageActivity.this, getString(R.string.enter_subject),
                            Toast.LENGTH_LONG).show();
                    return;
                }
                String subject = ((EditText) findViewById(R.id.subject)).getText().toString();
                if (subject.indexOf("Re:") != 0) {
                    subject = "Re: " + subject;
                }
                SendMessageTask submitTask = new SendMessageTask(recipient, subject, ((EditText) findViewById(R.id.reply)).getText().toString());
                submitTask.execute((Void) null);
            }
        });

        final TextView messageTextView = ((TextView) findViewById(R.id.message));
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
        LoadMessageTask loadTask = new LoadMessageTask(message.getMessageID());
        loadTask.execute((Void) null);

        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch (menuItem.getItemId()){
                    case R.id.action_trash:
                        DeleteMessageTask deleteTask = new DeleteMessageTask(message.getMessageID());
                        deleteTask.execute((Void) null);
                        return true;
                    case R.id.action_challenge:
                        Intent intent = new Intent(getApplicationContext(), InvitationActivity.class);
                        intent.putExtra("opponent", recipient);
                        startActivity(intent);
                        return true;
                }

                return false;
            }
        });

        KeyboardVisibilityEvent.setEventListener(
                ReplyMessageActivity.this,
                new KeyboardVisibilityEventListener() {
                    @Override
                    public void onVisibilityChanged(boolean isOpen) {
                        // some code depending on keyboard visiblity status

                        if (isOpen) {
                            ((Button) findViewById(R.id.sendButton)).setVisibility(View.GONE);
                            if (PentePlayer.mShowAds) {
//                                ((AdView) findViewById(R.id.adView)).setVisibility(View.GONE);
                            }
                        } else {
                            ((Button) findViewById(R.id.sendButton)).setVisibility(View.VISIBLE);
                            if (PentePlayer.mShowAds) {
                                ((AdView) findViewById(R.id.adView)).setVisibility(View.VISIBLE);
                            }
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.message_menu, menu);
        return true;
    }

    protected void makeLinkClickable(SpannableStringBuilder strBuilder, final URLSpan span)
    {
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

    protected void setTextViewHTML(TextView text, String html)
    {
        CharSequence sequence = Html.fromHtml(html);
        SpannableStringBuilder strBuilder = new SpannableStringBuilder(sequence);
        URLSpan[] urls = strBuilder.getSpans(0, sequence.length(), URLSpan.class);
        for(URLSpan span : urls) {
            makeLinkClickable(strBuilder, span);
        }
        text.setText(strBuilder);
        text.setMovementMethod(new ScrollingMovementMethod());
        text.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private class SendMessageTask extends AsyncTask<Void, Void, Boolean> {

        private final String recipient;
        private String subject;
        private String message;

        SendMessageTask(String recipient, String subject, String message) {
            this.recipient = recipient.toLowerCase();
            try {
                this.message = URLEncoder.encode(message,"UTF-8");
            } catch (UnsupportedEncodingException e) {
                this.message = "";
                e.printStackTrace();
            }
            try {
                if ("".equals(subject)) {
                    this.subject = URLEncoder.encode("(no subject)","UTF-8");
                } else {
                    this.subject = URLEncoder.encode(subject,"UTF-8");
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
                String urlParameters  = "command=create&to=" + recipient + "&subject=" + subject + "&body=" + message + "&mobile="
                        +"&name2="+PentePlayer.mPlayerName+"&password2="+ PentePlayer.mPassword;
                byte[] postData       = new byte[0];
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                    postData = urlParameters.getBytes( StandardCharsets.UTF_8 );
                }
                int    postDataLength = postData.length;
                String request        = "https://www.pente.org/gameServer/mymessages";
                URL url            = new URL( request );
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

                if (output.indexOf("Error: Player "+recipient+" not found.") > -1) {
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

    private class LoadMessageTask extends AsyncTask<Void, Void, Boolean> {

        private String messageID;
        private String messageText = "";

        LoadMessageTask(String messageID) {
            this.messageID = messageID;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // TODO: attempt authentication against a network service.

            try {
//                URL url = new URL("https://www.pente.org/gameServer/mymessages?command=view&mid=" + messageID);
                URL url = new URL("https://www.pente.org/gameServer/mymessages?command=view&mid=" + messageID
                        +"&name2="+PentePlayer.mPlayerName+"&password2="+ PentePlayer.mPassword);
                HttpsURLConnection connection = (HttpsURLConnection)url.openConnection();
                String cookies = CookieManager.getInstance().getCookie("https://www.pente.org/");
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
                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    System.out.println("response code for submit was " + responseCode);
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

//                System.out.println("submit output: " + output.toString());
//
//                String urlParameters  = "command=view&mid=" + messageID + "&name2=" + PentePlayer.mPlayerName + "&password2=" + PentePlayer.mPassword;
//                byte[] postData       = new byte[0];
//                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
//                    postData = urlParameters.getBytes( StandardCharsets.UTF_8 );
//                }
//                int    postDataLength = postData.length;
//                String request        = "https://www.pente.org/gameServer/mymessages";
//                URL url            = new URL( request );
//                HttpsURLConnection conn= (HttpsURLConnection) url.openConnection();
//                conn.setDoOutput( true );
//                conn.setInstanceFollowRedirects( false );
//                conn.setRequestMethod( "POST" );
//                conn.setRequestProperty( "Content-Type", "application/x-www-form-urlencoded");
//                conn.setRequestProperty( "charset", "utf-8");
//                conn.setRequestProperty( "Content-Length", Integer.toString( postDataLength ));
//                conn.setUseCaches( false );
//                try {
//                    DataOutputStream wr = new DataOutputStream( conn.getOutputStream());
//                    wr.write( postData );
//                } catch (Exception e) {
//                    e.printStackTrace();
//                    return  false;
//                }
//
//                StringBuilder output = new StringBuilder();
//                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
//                System.out.println("output===============" + br);
//                String line = "";
//                while((line = br.readLine()) != null ) {
//                    output.append(line + System.getProperty("line.separator"));
//                }
//                br.close();
//
//                output.append(System.getProperty("line.separator") + "Response " + System.getProperty("line.separator") + System.getProperty("line.separator"));
////                System.out.println(output);

                String tmpStr1 = output.toString();

                messageText = tmpStr1.substring(tmpStr1.indexOf("        <br>\n          ") + 23, tmpStr1.indexOf("          <br><br>"));;

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
                TextView messageView = ((TextView) findViewById(R.id.message));
                setTextViewHTML(messageView, filterMessage(messageText));
            }
        }

        @Override
        protected void onCancelled() {
        }
    }

    private class DeleteMessageTask extends AsyncTask<Void, Void, Boolean> {

        private String messageID;

        DeleteMessageTask(String messageID) {
            this.messageID = messageID;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // TODO: attempt authentication against a network service.

            try {
//                String urlParameters  ="command=delete&mid=" + messageID + "&mobile=";
                String urlParameters  ="command=delete&mid=" + messageID + "&mobile="
                        +"&name2="+PentePlayer.mPlayerName+"&password2="+ PentePlayer.mPassword;
                byte[] postData       = new byte[0];
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                    postData = urlParameters.getBytes( StandardCharsets.UTF_8 );
                }
                int    postDataLength = postData.length;
                String request        = "https://www.pente.org/gameServer/mymessages";
                URL url            = new URL( request );
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

                return true;

            } catch (IOException e1) {
                e1.printStackTrace();
                return  false;
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

    private String filterMessage(String msgStr) {
//        return msgStr.replace("[host]", "pente.org");

        String tmpStrComma = msgStr.replace("\\1", ",");
        String tmpStrSmiley = tmpStrComma.replace("<img border=\"0\" src=\"http://[host]/gameServer/forums/images/emoticons/happy.gif\" alt=\"\">", ":)");
        String tmpStrWink = tmpStrSmiley.replace("<img border=\"0\" src=\"http://[host]/gameServer/forums/images/emoticons/wink.gif\" alt=\"\">", ";)");
        String tmpStrTongue = tmpStrWink.replace("<img border=\"0\" src=\"http://[host]/gameServer/forums/images/emoticons/silly.gif\" alt=\"\">", ":p");
        String tmpStrGrin = tmpStrTongue.replace("<img border=\"0\" src=\"http://[host]/gameServer/forums/images/emoticons/grin.gif\" alt=\"\">", ":D");
        String tmpStrSad = tmpStrGrin.replace("<img border=\"0\" src=\"http://[host]/gameServer/forums/images/emoticons/sad.gif\" alt=\"\">", ":(");
        String tmpStrLove = tmpStrSad.replace("<img border=\"0\" src=\"http://[host]/gameServer/forums/images/emoticons/love.gif\" alt=\"\">", "<3");
        String tmpStrMischief = tmpStrLove.replace("<img border=\"0\" src=\"http://[host]/gameServer/forums/images/emoticons/mischief.gif\" alt=\"\">", ";\\");
        String tmpStrCool = tmpStrMischief.replace("<img border=\"0\" src=\"http://[host]/gameServer/forums/images/emoticons/cool.gif\" alt=\"\">", "B)");
        String tmpStrDevil = tmpStrCool.replace("<img border=\"0\" src=\"http://[host]/gameServer/forums/images/emoticons/devil.gif\" alt=\"\">", ">:)");
        String tmpStrAngry = tmpStrDevil.replace("<img border=\"0\" src=\"http://[host]/gameServer/forums/images/emoticons/angry.gif\" alt=\"\">", "X(");
        String tmpStrLaugh = tmpStrAngry.replace("<img border=\"0\" src=\"http://[host]/gameServer/forums/images/emoticons/laugh.gif\" alt=\"\">", ":^O");
        String tmpStrBlush = tmpStrLaugh.replace("<img border=\"0\" src=\"http://[host]/gameServer/forums/images/emoticons/blush.gif\" alt=\"\">", ":8)");
        String tmpStrCry = tmpStrBlush.replace("<img border=\"0\" src=\"http://[host]/gameServer/forums/images/emoticons/cry.gif\" alt=\"\">", ":\"(");
        String tmpStrConfused = tmpStrCry.replace("<img border=\"0\" src=\"http://[host]/gameServer/forums/images/emoticons/confused.gif\" alt=\"\">", "?:|");
        String tmpStrShocked = tmpStrConfused.replace("<img border=\"0\" src=\"http://[host]/gameServer/forums/images/emoticons/shocked.gif\" alt=\"\">", ":O");
        String tmpStrPlain = tmpStrShocked.replace("<img border=\"0\" src=\"http://[host]/gameServer/forums/images/emoticons/plain.gif\" alt=\"\">", ":|");
        return tmpStrPlain;
    }

}
