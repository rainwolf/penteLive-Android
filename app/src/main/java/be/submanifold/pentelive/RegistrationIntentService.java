package be.submanifold.pentelive;

/**
 * Created by waliedothman on 11/05/16.
 */

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.gcm.GcmPubSub;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

public class RegistrationIntentService extends IntentService {

    private static final String TAG = "RegIntentService";
    private static final String[] TOPICS = {"global"};

    public RegistrationIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        System.out.println(" register Token ------------------------------------");

        try {
            // [START register_for_gcm]
            // Initially this call goes out to the network to retrieve the token, subsequent calls
            // are local.
            // R.string.gcm_defaultSenderId (the Sender ID) is typically derived from google-services.json.
            // See https://developers.google.com/cloud-messaging/android/start for details on this file.
            // [START get_token]
            InstanceID instanceID = InstanceID.getInstance(this);
            String token = instanceID.getToken(getString(R.string.gcm_defaultSenderId),
                    GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            // [END get_token]
            Log.i(TAG, "GCM Registration Token: " + token);

            // TODO: Implement this method to send any registration to your app's servers.
            sendRegistrationToServer(token);

            // [END register_for_gcm]
        } catch (Exception e) {
            Log.d(TAG, "Failed to complete token refresh", e);
            // If an exception happens while fetching the new token or updating our registration data
            // on a third-party server, this ensures that we'll attempt the update at a later time.
        }
        // Notify UI that registration has completed, so the progress indicator can be hidden.
//        Intent registrationComplete = new Intent(QuickstartPreferences.REGISTRATION_COMPLETE);
//        LocalBroadcastManager.getInstance(this).sendBroadcast(registrationComplete);
    }

    /**
     * Persist registration to third-party servers.
     *
     * Modify this method to associate the user's GCM registration token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    private void sendRegistrationToServer(String token) {
        String storedUserName = PrefUtils.getFromPrefs(RegistrationIntentService.this, PrefUtils.PREFS_LOGIN_USERNAME_KEY, null);
        String storedPassword = PrefUtils.getFromPrefs(RegistrationIntentService.this, PrefUtils.PREFS_LOGIN_PASSWORD_KEY, null);
        String storedToken = PrefUtils.getFromPrefs(RegistrationIntentService.this, PrefUtils.PREFS_TOKEN_KEY, "");

        Date date = new Date(System.currentTimeMillis()); //or simply new Date();
        long millisNow = date.getTime();
        long millisLastPing = PrefUtils.getLongFromPrefs(RegistrationIntentService.this, PrefUtils.PREFS_TOKENLASTSENT_KEY, 0);
        if (((millisNow - millisLastPing)/(1000*3600*24) >= 1 && storedPassword != null && storedUserName != null) || !storedToken.equals(token)) {
            try {
                URL url = new URL("https://pente.org/gameServer/notifications/registerDeviceAndroid.jsp?name=" + storedUserName + "&password=" + storedPassword
                        + "&token=" + token);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    System.out.println("response code for submit was " + responseCode);
                }

                StringBuilder output = new StringBuilder();
                BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                System.out.println("output===============" + br);
                String line = "";
                while ((line = br.readLine()) != null) {
                    output.append(line + "\n");
                }
                br.close();

                if (output.toString().indexOf("It seems to have worked") > -1) {
                    PrefUtils.saveLongToPrefs(RegistrationIntentService.this, PrefUtils.PREFS_TOKENLASTSENT_KEY, millisNow);
                    PrefUtils.saveToPrefs(RegistrationIntentService.this, PrefUtils.PREFS_TOKEN_KEY, token);
                }


            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        // Add custom implementation, as needed.
    }

    /**
     * Subscribe to any GCM topics of interest, as defined by the TOPICS constant.
     *
     * @param token GCM token
     * @throws IOException if unable to reach the GCM PubSub service
     */
    // [START subscribe_topics]
//    private void subscribeTopics(String token) throws IOException {
//        GcmPubSub pubSub = GcmPubSub.getInstance(this);
//        for (String topic : TOPICS) {
//            pubSub.subscribe(token, "/topics/" + topic, null);
//        }
//    }
    // [END subscribe_topics]

}
