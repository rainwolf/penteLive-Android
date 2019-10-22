package be.submanifold.pentelive;

/**
 * Created by waliedothman on 12/05/16.
 */

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import androidx.core.app.NotificationCompat;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.Map;

public class MyFcmListenerService extends FirebaseMessagingService {

    private static final String TAG = "MyFcmListenerService";
    private MediaPlayer mediaPlayer;


    @Override
    public void onNewToken(String newToken) {

//        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
//        Log.d(TAG, "Refreshed token: " + refreshedToken);
//        System.out.println("Refreshed token: " + refreshedToken);
//        System.out.println("Refreshed token: " + newToken);
        // TODO: Implement this method to send any registration to your app's servers.
        sendRegistrationToServer(newToken);
    }
    // [END refresh_token]

    private void sendRegistrationToServer(String token) {
        String storedUserName = PrefUtils.getFromPrefs(MyFcmListenerService.this, PrefUtils.PREFS_LOGIN_USERNAME_KEY, null);
        String storedPassword = PrefUtils.getFromPrefs(MyFcmListenerService.this, PrefUtils.PREFS_LOGIN_PASSWORD_KEY, null);
        String storedToken = PrefUtils.getFromPrefs(MyFcmListenerService.this, PrefUtils.PREFS_TOKEN_KEY, "");

        Date date = new Date(System.currentTimeMillis()); //or simply new Date();
        long millisNow = date.getTime();
        if ((storedPassword != null && storedUserName != null) || !storedToken.equals(token)) {
            try {
                PrefUtils.saveLongToPrefs(MyFcmListenerService.this, PrefUtils.PREFS_TOKENLASTSENT_KEY, 0);
                URL url = new URL("https://www.pente.org/gameServer/notification?device=android&token=" + token);
                if (PentePlayer.development) {
                    url = new URL("https://development.pente.org/gameServer/notification?device=android&token=" + token);
                }
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
                    PrefUtils.saveLongToPrefs(MyFcmListenerService.this, PrefUtils.PREFS_TOKENLASTSENT_KEY, millisNow);
                    PrefUtils.saveToPrefs(MyFcmListenerService.this, PrefUtils.PREFS_TOKEN_KEY, token);
                }


            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        // Add custom implementation, as needed.
    }

    /**
     * Called when message is received.
     *
     *
     * @param from SenderID of the sender.
     * @param data Data bundle containing message data as key/value pairs.
     *             For Set of keys use data.keySet().
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(RemoteMessage message){
        String from = message.getFrom();
        Map data = message.getData();

        String messageStr = (String) data.get("message");
//        Log.d(TAG, "From: " + from);
//        Log.d(TAG, "Message: " + messageStr);

        if (from.startsWith("/topics/")) {
            // message received from some topic.
        } else {
            // normal downstream message.
        }

        // [START_EXCLUDE]
        /**
         * Production applications would usually process the message here.
         * Eg: - Syncing with server.
         *     - Store message in local database.
         *     - Update UI.
         */

        /**
         * In some cases it may be useful to show a notification indicating to the user
         * that a message was received.
         */

//        Map<String, String> params = message.getData();
//        JSONObject object = new JSONObject(params);
//        System.out.println(object.toString());
        sendNotification(message);

        // [END_EXCLUDE]
    }
    // [END receive_message]

    /**
     * Create and show a simple notification containing the received GCM message.
     *
     * @param message GCM message received.
     */
    private void sendNotification(RemoteMessage message) {
        String from = message.getFrom();
        Map data = message.getData();

        String messageStr = (String) data.get("message");
        boolean silent = "silentNotification".equals(messageStr);

        String localMsgStr = "";
        if (messageStr.contains("device has been registered for notifications")) {
            localMsgStr = getString(R.string.registered_for_notifications);
        } else if (messageStr.contains("your move")) {
            String[] splitStr = messageStr.replace("It's your move in a game of ","").split(" against ");
            localMsgStr = getString(R.string.is_your_move_against, splitStr[1], splitStr[0]);
        } else if (messageStr.contains("new message")) {
            String[] splitStr = messageStr.split(" sent you a new message! ");
            localMsgStr = getString(R.string.new_message_from, splitStr[0], splitStr[1]);
        } else if (messageStr.contains("invited you")) {
            String[] splitStr = messageStr.split(" has invited you to a game of ");
            localMsgStr = getString(R.string.has_invited_you, splitStr[0], splitStr[1]);
        } else if (messageStr.contains("Live Game Alert") && messageStr.contains("wants to play live")) {
            String player = (String) data.get("liveBroadCastPlayer");
            String game = (String) data.get("liveBroadCastGame");
            localMsgStr = getString(R.string.live_game_alert, player, game);
        }


        if (!MyApplication.isActivityVisible()) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                    PendingIntent.FLAG_ONE_SHOT);

            if (!silent) {
                Uri notificationSoundUri;
                String notificationChannel = "";
                if (messageStr.contains("Live Game Alert") && messageStr.contains("wants to play live")) {
                    notificationSoundUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.newplayersound);
                    notificationChannel = "penteLive_livePlay_channel";
                } else {
                    notificationSoundUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.pentelivenotificationsound);
                    notificationChannel = "penteLive_turnBased_channel";
                }
                NotificationCompat.Builder notificationBuilder;
                notificationBuilder = new NotificationCompat.Builder(this, notificationChannel)
                        .setSmallIcon(R.drawable.ic_radio_button_unchecked)
                        .setContentTitle("Pente Live")
//                    .setContentText("")
                        .setStyle(new androidx.core.app.NotificationCompat.BigTextStyle().bigText(localMsgStr))
                        .setAutoCancel(true)
                        .setSound(notificationSoundUri)
                        .setContentIntent(pendingIntent);

                Notification notification = notificationBuilder.build();
                notification.sound = notificationSoundUri;
                notification.defaults = ~Notification.DEFAULT_SOUND;

                NotificationManager notificationManager =
                        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    AudioAttributes att = new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build();
                    NotificationChannel channel = new NotificationChannel(notificationChannel, "penteLive", NotificationManager.IMPORTANCE_DEFAULT);
                    channel.setSound(notificationSoundUri, att);
                    notificationManager.createNotificationChannel(channel);
                }


                notificationManager.notify(0 /* ID of notification */, notification);
            }
        } else {
            if (!silent) {
                silent = PrefUtils.getBooleanFromPrefs(MyApplication.getContext(), PrefUtils.PREFS_INAPPSOUNDSOFF_KEY, false);
            }
            Uri notificationSoundUri;
            if (messageStr.contains("Live Game Alert") && messageStr.contains("wants to play live")) {
                notificationSoundUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.newplayersound);
            } else {
                notificationSoundUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.pentelivenotificationsound);
            }
            try {
                if (!silent) {
                    if (mediaPlayer == null) {
                        mediaPlayer = new MediaPlayer();
                    }
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.reset();
                    }
                    mediaPlayer = new MediaPlayer();
                    mediaPlayer.setDataSource(getApplicationContext(), notificationSoundUri);
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                        AudioAttributes att = new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build();
                        mediaPlayer.setAudioAttributes(att);
                    } else {
                        mediaPlayer.setAudioStreamType(AudioManager.STREAM_NOTIFICATION);
                    }
                    mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mediaPlayer) {
                            mediaPlayer.start();
                        }
                    });
                    mediaPlayer.prepare();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (messageStr.contains("your move")) {
                Intent intent = new Intent("unique_name_computer");
                intent.putExtra("gameID", (String) data.get("gameID"));
                sendBroadcast(intent);

                intent = new Intent("unique_name");
                //put whatever data you want to send, if any
                intent.putExtra("message", localMsgStr);

                //send broadcast
                sendBroadcast(intent);
            } else {
                Intent intent = new Intent("unique_name");
                //put whatever data you want to send, if any
                if (!silent) {
                    intent.putExtra("message", localMsgStr);
                }

                //send broadcast
                sendBroadcast(intent);
            }

        }
    }
}
