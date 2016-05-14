package be.submanifold.pentelive;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class PrefUtils {
    public static final String PREFS_LOGIN_USERNAME_KEY = "__USERNAME__" ;
    public static final String PREFS_LOGIN_PASSWORD_KEY = "__PASSWORD__" ;
    public static final String PREFS_MESSAGES_COLLAPSED_KEY = "__MESSAGESCOLLAPSED__" ;
    public static final String PREFS_INVITATIONS_COLLAPSED_KEY = "__INVITATIONSCOLLAPSED__" ;
    public static final String PREFS_ACTIVEGAMES_COLLAPSED_KEY = "__ACTIVEGAMESCOLLAPSED__" ;
    public static final String PREFS_PUBLICINVITATIONS_COLLAPSED_KEY = "__PUBLICINVITATIONSCOLLAPSED__" ;
    public static final String PREFS_SENTINVITATIONS_COLLAPSED_KEY = "__SENTINVITATIONSCOLLAPSED__" ;
    public static final String PREFS_NONACTIVEGAMES_COLLAPSED_KEY = "__NONACTIVEGAMESCOLLAPSED__" ;
    public static final String PREFS_OPENINVITATIONCREDIT_KEY = "__OPENINVITATIONCREDIT__" ;
    public static final String PREFS_AUTOCOMPLETEPLAYERS_KEY = "__AUTOCOMPLETEPLAYERS__" ;
    public static final String PREFS_AUTOLOGIN_KEY = "__AUTOLOGIN__" ;
    public static final String PREFS_INVITATIONGAME_KEY = "__INVITATIONGAME__" ;
    public static final String PREFS_INVITATIONDAYS_KEY = "__INVITATIONDAYS__" ;
    public static final String PREFS_INVITATIONRESTRICTION_KEY = "__INVITATIONRESTRICTION__" ;
    public static final String PREFS_TOKEN_KEY = "__TOKEN__" ;
    public static final String PREFS_TOKENLASTSENT_KEY = "__TOKENLASTSENT__" ;

    public static void saveToPrefs(Context context, String key, String value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putString(key,value);
        editor.commit();
    }

    public static String getFromPrefs(Context context, String key, String defaultValue) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        try {
            return sharedPrefs.getString(key, defaultValue);
        } catch (Exception e) {
            e.printStackTrace();
            return defaultValue;
        }
    }

    public static void saveBooleanToPrefs(Context context, String key, boolean value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(key,value);
        editor.commit();
    }

    public static boolean getBooleanFromPrefs(Context context, String key, boolean defaultValue) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        try {
            return sharedPrefs.getBoolean(key, defaultValue);
        } catch (Exception e) {
            e.printStackTrace();
            return defaultValue;
        }
    }

    public static void saveIntToPrefs(Context context, String key, int value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(key,value);
        editor.commit();
    }

    public static int getIntFromPrefs(Context context, String key, int defaultValue) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        try {
            return sharedPrefs.getInt(key, defaultValue);
        } catch (Exception e) {
            e.printStackTrace();
            return defaultValue;
        }
    }

    public static void saveLongToPrefs(Context context, String key, long value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(key,value);
        editor.commit();
    }

    public static long getLongFromPrefs(Context context, String key, long defaultValue) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        try {
            return sharedPrefs.getLong(key, defaultValue);
        } catch (Exception e) {
            e.printStackTrace();
            return defaultValue;
        }
    }

    public static void savePlayerToPrefs(Context context, String value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> players = prefs.getStringSet(PREFS_AUTOCOMPLETEPLAYERS_KEY, null);
        if (players == null) {
            players = new HashSet<String>();
        }
        if (!players.contains(value)) {
            players.add(value);
            final SharedPreferences.Editor editor = prefs.edit();
            editor.putStringSet(PREFS_AUTOCOMPLETEPLAYERS_KEY, players);
            editor.commit();
        }
    }
    public static Set<String> getPlayers(Context context) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        try {
            return (HashSet<String>) sharedPrefs.getStringSet(PREFS_AUTOCOMPLETEPLAYERS_KEY, new HashSet<String>());
        } catch (Exception e) {
            e.printStackTrace();
            return new HashSet<String>();
        }
    }

}