package be.submanifold.pentelive;

import android.content.res.Configuration;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
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
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SendMessageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_message);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("New message");
        setSupportActionBar(toolbar);

        final AutoCompleteTextView actv = (AutoCompleteTextView) findViewById(R.id.recipient);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, new ArrayList<String>(PrefUtils.getPlayers(SendMessageActivity.this)));
        actv.setAdapter(adapter);

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
                if (actv.getText().toString().equals("")) {
                    Toast.makeText(SendMessageActivity.this, "Please enter a recipient.",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                if (((EditText) findViewById(R.id.subject)).getText().toString().equals("")) {
                    Toast.makeText(SendMessageActivity.this, "Please enter a subject.",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                SendMessageTask submitTask = new SendMessageTask(actv.getText().toString(), ((EditText) findViewById(R.id.subject)).getText().toString(), ((EditText) findViewById(R.id.message)).getText().toString());
                submitTask.execute((Void) null);
            }
        });

        KeyboardVisibilityEvent.setEventListener(
                SendMessageActivity.this,
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
                String urlParameters  = "command=create&to=" + recipient + "&subject=" + subject + "&body=" + message + "&mobile=" + "&name2=" + PentePlayer.mPlayerName + "&password2=" + PentePlayer.mPassword;
                byte[] postData       = new byte[0];
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                    postData = urlParameters.getBytes( StandardCharsets.UTF_8 );
                }
                int    postDataLength = postData.length;
                String request        = "https://www.pente.org/gameServer/mymessages";
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
               PrefUtils.savePlayerToPrefs(SendMessageActivity.this, recipient);
               finish();
            } else {
                ((AutoCompleteTextView) findViewById(R.id.recipient)).setError("username does not exist");
            }
        }

        @Override
        protected void onCancelled() {
        }
    }

}
