package be.submanifold.pentelive;

import android.app.Application;
import android.app.NotificationManager;
import android.content.Context;

/**
 * Created by waliedothman on 14/05/16.
 */
public class MyApplication extends Application {

    private static Context mContext;
    private static boolean shouldQuit = false;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
    }

    public static Context getContext() {
        return mContext;
    }

    public static boolean isActivityVisible() {
        return activityVisible;
    }

    public static void activityResumed(Context ctx) {
        try {
            NotificationManager notificationManager =
                    (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancelAll();
        } catch (Exception e) {
            e.printStackTrace();
        }
        activityVisible = true;
    }

    public static void activityPaused() {
        activityVisible = false;
    }

    private static boolean activityVisible;

    public static boolean shouldQuit() {
        return shouldQuit;
    }

    public static void setShouldQuit(boolean shouldQuit) {
        MyApplication.shouldQuit = shouldQuit;
    }

}