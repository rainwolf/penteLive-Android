package be.submanifold.pentelive;

        import android.content.Intent;
        import android.util.Log;

        import com.google.firebase.iid.FirebaseInstanceId;
        import com.google.firebase.iid.FirebaseInstanceIdService;

        import java.io.BufferedReader;
        import java.io.IOException;
        import java.io.InputStreamReader;
        import java.net.HttpURLConnection;
        import java.net.URL;
        import java.util.Date;

public class MyInstanceIDListenerService extends FirebaseInstanceIdService {

    private static final String TAG = "MyInstanceIDLS";


    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. This call is initiated by the
     * InstanceID provider.
     */
    // [START refresh_token]
    @Override
    public void onTokenRefresh() {

        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG, "Refreshed token: " + refreshedToken);
        System.out.println("Refreshed token: " + refreshedToken);
        // TODO: Implement this method to send any registration to your app's servers.
        sendRegistrationToServer(refreshedToken);
    }
    // [END refresh_token]

    private void sendRegistrationToServer(String token) {
        String storedUserName = PrefUtils.getFromPrefs(MyInstanceIDListenerService.this, PrefUtils.PREFS_LOGIN_USERNAME_KEY, null);
        String storedPassword = PrefUtils.getFromPrefs(MyInstanceIDListenerService.this, PrefUtils.PREFS_LOGIN_PASSWORD_KEY, null);
        String storedToken = PrefUtils.getFromPrefs(MyInstanceIDListenerService.this, PrefUtils.PREFS_TOKEN_KEY, "");

        Date date = new Date(System.currentTimeMillis()); //or simply new Date();
        long millisNow = date.getTime();
        if ((storedPassword != null && storedUserName != null) || !storedToken.equals(token)) {
            try {
                PrefUtils.saveLongToPrefs(MyInstanceIDListenerService.this, PrefUtils.PREFS_TOKENLASTSENT_KEY, 0);
                URL url = new URL("https://www.pente.org/gameServer/notification?device=android&token=" + token);
//                URL url = new URL("https://www.pente.org/gameServer/notifications/registerDeviceAndroid.jsp?name=" + storedUserName + "&password=" + storedPassword
//                        + "&token=" + token);
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

                if (output.toString().contains("It seems to have worked")) {
                    PrefUtils.saveLongToPrefs(MyInstanceIDListenerService.this, PrefUtils.PREFS_TOKENLASTSENT_KEY, millisNow);
                    PrefUtils.saveToPrefs(MyInstanceIDListenerService.this, PrefUtils.PREFS_TOKEN_KEY, token);
                }


            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        // Add custom implementation, as needed.
    }

}
