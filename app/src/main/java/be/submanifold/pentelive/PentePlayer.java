package be.submanifold.pentelive;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Parcel;
import android.os.Parcelable;
import android.webkit.CookieManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by waliedothman on 10/04/16.
 */
public class PentePlayer implements Parcelable {

    public static Boolean development = false;

    public static String mPlayerName;
    public static String mPassword;
    public static Boolean mShowAds;
    public static Boolean mSubscriber;
    public static Boolean dbAccess;
    private List<Game> mInvitations;
    private List<Game>  mSentInvitations;
    private List<Game>  mActiveGames;
    private List<Game>  mNonActiveGames;
    private List<Game>  mPublicInvitations;
    private List<Message> mMessages;
    private List<RatingStat> mRatingStats;
    private List<KingOfTheHill> mHills;
    private List<Tournament> mTournaments;
    public static int myColor;

    public static List<String> pendingAvatarChecks;
    public static Map<String, Bitmap> avatars;

    public static boolean loadAvatars;
    public static boolean showOnlyTB;
    public static boolean emailMe;

    private int livePlayers;
    public int getLivePlayers() { return this.livePlayers; }

    private int tbRatings, tbHills;
    public int getTbRatings() { return tbRatings; }
    public int gettbHills() { return tbHills; }

    public PentePlayer(String playerName, String password) {
        this.mPlayerName = playerName;
        this.mPassword = password;
        this.mInvitations = new ArrayList<Game>();
        this.mSentInvitations = new ArrayList<Game>();
        this.mActiveGames = new ArrayList<Game>();
        this.mNonActiveGames = new ArrayList<Game>();
        this.mPublicInvitations = new ArrayList<Game>();
        this.mMessages= new ArrayList<Message>();
        this.mRatingStats = new ArrayList<RatingStat>();
        this.mTournaments = new ArrayList<Tournament>();
        this.mHills = new ArrayList<KingOfTheHill>();
        this.mShowAds = true;
        this.emailMe = true;
        pendingAvatarChecks = new ArrayList<String>();
        avatars = new HashMap<String, Bitmap>();
        myColor = 0;
        livePlayers = 0;
        tbRatings = 0;
        tbHills = 0;
    }

    public String getPlayerName() {
        return this.mPlayerName;
    }
    public List<Game> getInvitations() {
        return this.mInvitations;
    }
    public List<Game> getSentInvitations() {
        return this.mSentInvitations;
    }
    public List<Game> getActiveGames() {
        return this.mActiveGames;
    }
    public List<Game> getNonActiveGames() {
        return this.mNonActiveGames;
    }
    public List<Game> getPublicInvitations() {
        return this.mPublicInvitations;
    }
    public List<Message> getMessages() {
        return this.mMessages;
    }
    public List<Tournament> getTournaments() { return this.mTournaments;    }
    public Boolean showAds() { return this.mShowAds; }
    public Boolean isSubscriber() {
        return mSubscriber;
    }
    public void setSubscriber(Boolean mSubscriber) {
        this.mSubscriber = mSubscriber;
    }
    public List<RatingStat> getRatingStats() {
        return mRatingStats;
    }
    public List<KingOfTheHill> getHills() { return mHills; }
    public int getMyColor() {
        return myColor;
    }
    public void setMyColor(int myColor) {
        this.myColor = myColor;
    }
    public static Boolean hasDBAccess() { return dbAccess; }
    public boolean isEmailMe() { return emailMe; }
    public void setEmailMe(boolean emailMe) { this.emailMe = emailMe; }



    private void populatePlayer(String dashString) {
        if (dashString == null) {
            return;
        }
//        if (dashString.indexOf("No Ads") > -1 && dashString.indexOf("No Ads") < 30) {
//            this.mShowAds = false;
//        }
//        if (dashString.indexOf("tb GamesLimit") > -1 || dashString.indexOf("tb GamesLimit") > 30) {
//            this.mSubscriber = false;
//        } else {
//            this.mSubscriber = true;
//        }

//        System.out.println(dashString);
        String[] dashLines = dashString.split("\n");
        String[] dashLine;
        int idx = 0;
        while (idx < dashLines.length && dashLines[idx].indexOf(mPlayerName.toLowerCase()) != 0) {
            idx += 1;
        }
        if (idx < dashLines.length && dashLines[idx].indexOf(mPlayerName.toLowerCase()) == 0) {
            dashLine = dashLines[idx].split(";", -1);
            this.myColor = Integer.parseInt(dashLine[1]);
            this.mShowAds = !"NoAds".equals(dashLine[2]);
            this.mSubscriber = "subscriber".equals(dashLine[3]);
            this.livePlayers = Integer.parseInt(dashLine[4]);
            this.dbAccess = "dbAccessGranted".equals(dashLine[5]);
            this.emailMe = "emailMe".equals(dashLine[6]);
//            System.out.println(myColor + "," + mShowAds + "," + mSubscriber);
        }

        while (idx < dashLines.length && dashLines[idx].indexOf("King of the Hill") == -1) {
            idx += 1;
        }
        this.mHills.clear();
        tbHills = 0;
        KingOfTheHill hill;
        if (idx < dashLines.length && dashLines[idx].indexOf("King of the Hill") == 0) {
            idx += 1;
            while (idx < dashLines.length && dashLines[idx].indexOf("Rating Stats") == -1) {
                dashLine = dashLines[idx].split(";", -1);
                idx += 1;
                if (dashLine.length < 7) {
                    continue;
                }
                hill = new KingOfTheHill(dashLine[0], dashLine[1], dashLine[4] ,dashLine[2].equals("1"), dashLine[3].equals("1"), dashLine[5].equals("1"), dashLine[6]);
                if (hill.getGameId()>50) {
                    tbHills += 1;
                }
                this.mHills.add(hill);
            }
        }
        while (idx < dashLines.length && dashLines[idx].indexOf("Rating Stats") == -1) {
            idx += 1;
        }
        this.mRatingStats.clear();
        tbRatings = 0;
        RatingStat ratingStat;
        if (idx < dashLines.length && dashLines[idx].indexOf("Rating Stats") == 0) {
            idx += 1;
            while (idx < dashLines.length && dashLines[idx].indexOf("Invitations received") == -1) {
                dashLine = dashLines[idx].split(";");
                idx += 1;
                if (dashLine.length < 5) {
                    continue;
                }
                ratingStat = new RatingStat(dashLine[0], dashLine[1], dashLine[4], dashLine[2], dashLine[3], dashLine[5]);
                if (ratingStat.getGameId()>50) {
                    tbRatings += 1;
                }
                this.mRatingStats.add(ratingStat);
            }
        }
        Game game;
        while (idx < dashLines.length && dashLines[idx].indexOf("Invitations received") == -1) {
            idx += 1;
        }
        this.mInvitations.clear();
        if (idx < dashLines.length && dashLines[idx].indexOf("Invitations received") == 0) {
            idx += 1;
            while (idx < dashLines.length && dashLines[idx].indexOf("Invitations sent") == -1) {
                dashLine = dashLines[idx].split(";");
                idx += 1;
                if (dashLine.length < 9) {
                    continue;
                }
                game = new Game(dashLine[0], null, dashLine[1], dashLine[2], dashLine[3], dashLine[4]
                        , dashLine[5], dashLine[6], null, dashLine[7], dashLine[8]);
                if (loadAvatars && game.getNameColor() != 0) {
                    addUserAvatar(game.getOpponentName());
                }
                this.mInvitations.add(game);
            }
        }
        this.mSentInvitations.clear();
        if (idx < dashLines.length && dashLines[idx].indexOf("Invitations sent") == 0) {
            idx += 1;
            while (idx < dashLines.length && dashLines[idx].indexOf("Active Games - My Turn") == -1) {
                dashLine = dashLines[idx].split(";");
                idx += 1;
                if (dashLine.length < 9) {
                    continue;
                }
                game = new Game(dashLine[0], null, dashLine[1], dashLine[2], dashLine[3], dashLine[4]
                        , dashLine[5], dashLine[6], null, dashLine[7], dashLine[8]);
                if (loadAvatars && game.getNameColor() != 0) {
                    addUserAvatar(game.getOpponentName());
                }
                this.mSentInvitations.add(game);
            }
        }
        this.mActiveGames.clear();
        if (idx < dashLines.length && dashLines[idx].indexOf("Active Games - My Turn") == 0) {
            idx += 1;
            while (idx < dashLines.length && dashLines[idx].indexOf("Active Games - Opponents Turn") == -1) {
                dashLine = dashLines[idx].split(";");
                idx += 1;
                if (dashLine.length < 10) {
                    continue;
                }
                game = new Game(dashLine[0], null, dashLine[1], dashLine[2], dashLine[3], dashLine[4]
                        , dashLine[6], dashLine[7], null, dashLine[8], dashLine[9]);
                if (loadAvatars && game.getNameColor() != 0) {
                    addUserAvatar(game.getOpponentName());
                }
                this.mActiveGames.add(game);
            }
        }
        this.mNonActiveGames.clear();
        if (idx < dashLines.length && dashLines[idx].indexOf("Active Games - Opponents Turn") == 0) {
            idx += 1;
            while (idx < dashLines.length && dashLines[idx].indexOf("Open Invitation Games") == -1) {
                dashLine = dashLines[idx].split(";");
                idx += 1;
                if (dashLine.length < 10) {
                    continue;
                }
                game = new Game(dashLine[0], null, dashLine[1], dashLine[2], dashLine[3], dashLine[4]
                        , dashLine[6], dashLine[7], null, dashLine[8], dashLine[9]);
                if (loadAvatars && game.getNameColor() != 0) {
                    addUserAvatar(game.getOpponentName());
                }
                this.mNonActiveGames.add(game);
            }
        }
        this.mPublicInvitations.clear();
        if (idx < dashLines.length && dashLines[idx].indexOf("Open Invitation Games") == 0) {
            idx += 1;
            while (idx < dashLines.length && dashLines[idx].indexOf("Messages") == -1) {
                dashLine = dashLines[idx].split(";");
                idx += 1;
                if (dashLine.length < 9) {
                    continue;
                }
                game = new Game(dashLine[0], null, dashLine[1], dashLine[2], dashLine[3], dashLine[4]
                        , dashLine[5], dashLine[6], null, dashLine[7], dashLine[8]);
                if (loadAvatars && game.getNameColor() != 0) {
                    addUserAvatar(game.getOpponentName());
                }
                this.mPublicInvitations.add(game);
            }
        }
        this.mMessages.clear();
        Message message;
        if (idx < dashLines.length && dashLines[idx].indexOf("Messages") == 0) {
            idx += 1;
            while (idx < dashLines.length && dashLines[idx].length() != 0 && dashLines[idx].indexOf("Tournaments") != 0) {
                dashLine = dashLines[idx].split(";");
                idx += 1;
                if (dashLine.length < 7) {
                    continue;
                }
                message = new Message(dashLine[0], dashLine[3], dashLine[2], dashLine[4], dashLine[1], dashLine[5], dashLine[6]);
                if (loadAvatars && message.getNameColor() != 0) {
                    addUserAvatar(message.getAuthor());
                }
                this.mMessages.add(message);
            }
        }

        this.mTournaments.clear();
        Tournament tournament;
        while (idx < dashLines.length && dashLines[idx].indexOf("Tournaments") != 0) {
            idx += 1;
        }
        if (idx < dashLines.length && dashLines[idx].indexOf("Tournaments") == 0) {
            idx += 1;
            while (idx < dashLines.length && dashLines[idx].length() != 0) {
                dashLine = dashLines[idx].split(";");
                idx += 1;
                if (dashLine.length < 6) {
                    continue;
                }
                tournament = new Tournament(dashLine[3], dashLine[0], dashLine[1], dashLine[2], dashLine[4], dashLine[5]);
                this.mTournaments.add(tournament);
            }
        }
    }

    public void addUserAvatar(String user) {
        if (pendingAvatarChecks != null && pendingAvatarChecks.contains(user)) {
            return;
        }
        if (pendingAvatarChecks == null) {
            pendingAvatarChecks = new ArrayList<String>();
        }
        pendingAvatarChecks.add(user);
        LoadAvatarTask avatarTask = new LoadAvatarTask(user);
        avatarTask.execute((Void) null);
    }

    public void loadPlayer(DashboardListAdapter listAdapter, boolean loadAvatars, boolean showOnlyTB) {
        this.showOnlyTB = showOnlyTB;
        this.loadAvatars = loadAvatars;
        if (this.mPassword == null || this.mPlayerName == null) {
            return;
        }
        LoadPlayerTask loadTask = new LoadPlayerTask(mPlayerName, mPassword, listAdapter);
        loadTask.execute((Void) null);
    }
    public void respondInvitation(String setID, boolean accept, DashboardListAdapter listAdapter) {
        AcceptDeclineInvitationTask responseTask = new AcceptDeclineInvitationTask(setID, accept, listAdapter);
        responseTask.execute((Void) null);
    }
    public void cancelInvitation(String setID, DashboardListAdapter listAdapter) {
        CancelInvitationTask responseTask = new CancelInvitationTask(setID, listAdapter);
        responseTask.execute((Void) null);
    }

    protected PentePlayer(Parcel in) {
        mPlayerName = in.readString();
        mPassword = in.readString();
        byte mShowAdsVal = in.readByte();
        mShowAds = mShowAdsVal == 0x02 ? null : mShowAdsVal != 0x00;
        byte mSubscriberVal = in.readByte();
        mSubscriber = mSubscriberVal == 0x02 ? null : mSubscriberVal != 0x00;
        byte dbVal = in.readByte();
        dbAccess = dbVal == 0x02 ? null : dbVal != 0x00;
        byte emaiValVal = in.readByte();
        emailMe = emaiValVal == 0x02 ? null : dbVal != 0x00;
        if (in.readByte() == 0x01) {
            mInvitations = new ArrayList<Game>();
            in.readList(mInvitations, Game.class.getClassLoader());
        } else {
            mInvitations = null;
        }
        if (in.readByte() == 0x01) {
            mSentInvitations = new ArrayList<Game>();
            in.readList(mSentInvitations, Game.class.getClassLoader());
        } else {
            mSentInvitations = null;
        }
        if (in.readByte() == 0x01) {
            mActiveGames = new ArrayList<Game>();
            in.readList(mActiveGames, Game.class.getClassLoader());
        } else {
            mActiveGames = null;
        }
        if (in.readByte() == 0x01) {
            mNonActiveGames = new ArrayList<Game>();
            in.readList(mNonActiveGames, Game.class.getClassLoader());
        } else {
            mNonActiveGames = null;
        }
        if (in.readByte() == 0x01) {
            mPublicInvitations = new ArrayList<Game>();
            in.readList(mPublicInvitations, Game.class.getClassLoader());
        } else {
            mPublicInvitations = null;
        }
        if (in.readByte() == 0x01) {
            mMessages = new ArrayList<Message>();
            in.readList(mMessages, Message.class.getClassLoader());
        } else {
            mMessages = null;
        }
        if (in.readByte() == 0x01) {
            mRatingStats = new ArrayList<RatingStat>();
            in.readList(mRatingStats, RatingStat.class.getClassLoader());
        } else {
            mRatingStats = null;
        }
        if (in.readByte() == 0x01) {
            mTournaments = new ArrayList<Tournament>();
            in.readList(mTournaments, Tournament.class.getClassLoader());
        } else {
            mTournaments = null;
        }
        if (in.readByte() == 0x01) {
            mHills = new ArrayList<KingOfTheHill>();
            in.readList(mHills, KingOfTheHill.class.getClassLoader());
        } else {
            mHills = null;
        }
//        if (in.readByte() == 0x01) {
//            pendingAvatarChecks = new ArrayList<String>();
//            in.readList(pendingAvatarChecks, String.class.getClassLoader());
//        } else {
//            pendingAvatarChecks = null;
//        }
//        if (in.readByte() == 0x01) {
//            avatars = in.readHashMap(Bitmap.class.getClassLoader());
//        } else {
//            avatars = null;
//        }
        myColor = in.readInt();
        livePlayers = in.readInt();
        tbRatings = in.readInt();
        tbHills = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mPlayerName);
        dest.writeString(mPassword);
        if (mShowAds == null) {
            dest.writeByte((byte) (0x02));
        } else {
            dest.writeByte((byte) (mShowAds ? 0x01 : 0x00));
        }
        if (mSubscriber == null) {
            dest.writeByte((byte) (0x02));
        } else {
            dest.writeByte((byte) (mSubscriber ? 0x01 : 0x00));
        }
        if (dbAccess == null) {
            dest.writeByte((byte) (0x02));
        } else {
            dest.writeByte((byte) (dbAccess ? 0x01 : 0x00));
        }
        dest.writeByte((byte) (emailMe ? 0x01 : 0x00));
        if (mInvitations == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeList(mInvitations);
        }
        if (mSentInvitations == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeList(mSentInvitations);
        }
        if (mActiveGames == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeList(mActiveGames);
        }
        if (mNonActiveGames == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeList(mNonActiveGames);
        }
        if (mPublicInvitations == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeList(mPublicInvitations);
        }
        if (mMessages == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeList(mMessages);
        }
        if (mRatingStats == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeList(mRatingStats);
        }
        if (mTournaments == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeList(mTournaments);
        }
        if (mHills == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeList(mHills);
        }
//        if (pendingAvatarChecks == null) {
//            dest.writeByte((byte) (0x00));
//        } else {
//            dest.writeByte((byte) (0x01));
//            dest.writeList(pendingAvatarChecks);
//        }
//        if (avatars == null) {
//            dest.writeByte((byte) (0x00));
//        } else {
//            dest.writeByte((byte) (0x01));
//            dest.writeMap(avatars);
//        }
        dest.writeInt(myColor);
        dest.writeInt(livePlayers);
        dest.writeInt(tbRatings);
        dest.writeInt(tbHills);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<PentePlayer> CREATOR = new Parcelable.Creator<PentePlayer>() {
        @Override
        public PentePlayer createFromParcel(Parcel in) {
            return new PentePlayer(in);
        }

        @Override
        public PentePlayer[] newArray(int size) {
            return new PentePlayer[size];
        }
    };

    private class LoadAvatarTask extends AsyncTask<Void, Void, Boolean> {

        private String mUsername;
        private Bitmap avatar;

        LoadAvatarTask(String username) {
            this.mUsername= username;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            try {
                avatar = null;
                URL url = new URL("https://www.pente.org/gameServer/avatar?name="+mUsername);
                HttpsURLConnection connection = (HttpsURLConnection)url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    System.out.println("response code for LoadAvatarTask was " + responseCode);
                    return false;
                }

                InputStream input = connection.getInputStream();
                avatar = BitmapFactory.decodeStream(input);


            } catch (IOException e1) {
                e1.printStackTrace();
                return  false;
            }

            // TODO: register the new account here.
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            if (avatar != null) {
                if (avatars == null) {
                    avatars = new HashMap<String, Bitmap>();
                }
                avatars.put(mUsername, avatar);
            }

        }

        @Override
        protected void onCancelled() {
        }
    }


    private class LoadPlayerTask extends AsyncTask<Void, Void, Boolean> {

        private final String mUsername, mPassword;
        private DashboardListAdapter listAdapter;

        LoadPlayerTask(String username, String password, DashboardListAdapter listAdapter) {
            this.mUsername= username;
            this.mPassword = password;
            this.listAdapter = listAdapter;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            try {
//                URL url = new URL("https://www.pente.org/gameServer/mobile/index.jsp?name="+mUsername+"&password="+mPassword);
                URL url = new URL("https://www.pente.org/gameServer/mobile/index.jsp?name="+mUsername+"&password="+mPassword+"&name2="+PentePlayer.mPlayerName+"&password2="+ PentePlayer.mPassword);
                if (development) {
                    url = new URL("https://development.pente.org/gameServer/mobile/index.jsp?name="+mUsername+"&password="+mPassword+"&name2="+PentePlayer.mPlayerName+"&password2="+ PentePlayer.mPassword);
                }

//                url = new URL("https://development.pente.org/gameServer/mobile/index.jsp?name="+mUsername+"&password="+mPassword);
                HttpsURLConnection connection = (HttpsURLConnection)url.openConnection();
                String cookies = CookieManager.getInstance().getCookie("https://www.pente.org/");
//                System.out.println("cookies: " +cookies);
                if (cookies != null) {
                    String[] splitCookie = cookies.split(";");
                    String cookieStr = "";
                    for (String item: splitCookie) {
                        if (item.contains("name2") || item.contains("password2") || item.contains("domain")) {
                            cookieStr += item + ";";
                        }
                    }
                    connection.setRequestProperty("Cookie", cookieStr);
//                    System.out.println("cookieStr: " +cookieStr);
                }
//                connection.addRequestProperty("Cookie", "name2="+mUsername+"; password2="+mPassword+";");
                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    System.out.println("response code for loadplayer was " + responseCode);
                    url = new URL("https://www.pente.org/gameServer/login.jsp?mobile=&name2="+PentePlayer.mPlayerName+"&password2="+ PentePlayer.mPassword);
                    if (development) {
                        url = new URL("https://development.pente.org/gameServer/login.jsp?mobile=&name2="+PentePlayer.mPlayerName+"&password2="+ PentePlayer.mPassword);
                    }
                    connection = (HttpsURLConnection)url.openConnection();
                    responseCode = connection.getResponseCode();
                    if (responseCode != 200) {
                        System.out.println("Logging back in failed");
                        return false;
                    } else {
                        url = new URL("https://www.pente.org/gameServer/mobile/index.jsp?name="+mUsername+"&password="+mPassword);
                        if (development) {
                            url = new URL("https://development.pente.org/gameServer/mobile/index.jsp?name="+mUsername+"&password="+mPassword+"&name2="+PentePlayer.mPlayerName+"&password2="+ PentePlayer.mPassword);
                        }
                        connection = (HttpsURLConnection)url.openConnection();
                        responseCode = connection.getResponseCode();
                        if (responseCode != 200) {
                            System.out.println("Logging back in and retrieving game failed");
                            return false;
                        }
                    }
                }

                StringBuilder output = new StringBuilder();
                BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
//                System.out.println("output===============" + br);
                String line = "";
                while((line = br.readLine()) != null ) {
                    output.append(line + "\n");
                }
                br.close();

//                System.out.println(output);

                String dashboardString = output.toString();
                if (dashboardString.indexOf("Invalid name or password, please try again.") != -1) {
                    return false;
                } else if (!dashboardString.contains("Invitations sent")) {
                    url = new URL("https://www.pente.org/gameServer/login.jsp?mobile=&name2="+PentePlayer.mPlayerName+"&password2="+ PentePlayer.mPassword);
                    if (development) {
                        url = new URL("https://development.pente.org/gameServer/login.jsp?mobile=&name2="+PentePlayer.mPlayerName+"&password2="+ PentePlayer.mPassword);
                    }
                    connection = (HttpsURLConnection)url.openConnection();
                    responseCode = connection.getResponseCode();
                    if (responseCode != 200) {
                        System.out.println("Logging back in failed");
                        return false;
                    } else {
                        url = new URL("https://www.pente.org/gameServer/mobile/index.jsp?name="+mUsername+"&password="+mPassword);
                        if (development) {
                            url = new URL("https://development.pente.org/gameServer/mobile/index.jsp?name="+mUsername+"&password="+mPassword+"&name2="+PentePlayer.mPlayerName+"&password2="+ PentePlayer.mPassword);
                        }
                        connection = (HttpsURLConnection)url.openConnection();
                        responseCode = connection.getResponseCode();
                        if (responseCode != 200) {
                            System.out.println("Logging back in and retrieving game failed");
                            return false;
                        }
                    }
                }

                populatePlayer(dashboardString);

            } catch (IOException e1) {
                e1.printStackTrace();
                return  false;
            }

            // TODO: register the new account here.
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {

            if (success) {
                listAdapter.updateList();
            }
        }

        @Override
        protected void onCancelled() {
        }
    }

    private class AcceptDeclineInvitationTask extends AsyncTask<Void, Void, Boolean> {

        private final String mSetID;
        private boolean accept;
        private DashboardListAdapter listAdapter;


        AcceptDeclineInvitationTask(String setID, boolean accept, DashboardListAdapter listAdapter) {
            this.mSetID = setID;
            this.accept = accept;
            this.listAdapter = listAdapter;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            try {
                URL url;
                if (this.accept) {
//                    url = new URL("https://www.pente.org/gameServer/tb/replyInvitation?mobile=&inviteeMessage=&command=Accept&sid=" + mSetID);
                    url = new URL("https://www.pente.org/gameServer/tb/replyInvitation?mobile=&inviteeMessage=&command=Accept&sid=" + mSetID
                            +"&name2="+PentePlayer.mPlayerName+"&password2="+ PentePlayer.mPassword);
                } else {
//                    url = new URL("https://www.pente.org/gameServer/tb/replyInvitation?mobile=&inviteeMessage=&command=Decline&sid=" + mSetID);
                    url = new URL("https://www.pente.org/gameServer/tb/replyInvitation?mobile=&inviteeMessage=&command=Decline&sid=" + mSetID
                            +"&name2="+PentePlayer.mPlayerName+"&password2="+ PentePlayer.mPassword);
                }
                if (development) {
                    if (this.accept) {
//                    url = new URL("https://www.pente.org/gameServer/tb/replyInvitation?mobile=&inviteeMessage=&command=Accept&sid=" + mSetID);
                        url = new URL("https://development.pente.org/gameServer/tb/replyInvitation?mobile=&inviteeMessage=&command=Accept&sid=" + mSetID
                                +"&name2="+PentePlayer.mPlayerName+"&password2="+ PentePlayer.mPassword);
                    } else {
//                    url = new URL("https://www.pente.org/gameServer/tb/replyInvitation?mobile=&inviteeMessage=&command=Decline&sid=" + mSetID);
                        url = new URL("https://development.pente.org/gameServer/tb/replyInvitation?mobile=&inviteeMessage=&command=Decline&sid=" + mSetID
                                +"&name2="+PentePlayer.mPlayerName+"&password2="+ PentePlayer.mPassword);
                    }
                }

//                System.out.println("accepting : " + url);

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

                StringBuilder output = new StringBuilder();
                BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                System.out.println("accept invitationt output===============" + br);
                String line = "";
                while((line = br.readLine()) != null ) {
                    output.append(line + System.getProperty("line.separator"));
                }
                br.close();

                output.append(System.getProperty("line.separator") + "Response " + System.getProperty("line.separator") + System.getProperty("line.separator"));

                System.out.println(output);


            } catch (IOException e1) {
                e1.printStackTrace();
                return  false;
            }

            // TODO: register the new account here.
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            loadPlayer(listAdapter, loadAvatars, showOnlyTB);
        }

        @Override
        protected void onCancelled() {
        }
    }

    private class CancelInvitationTask extends AsyncTask<Void, Void, Boolean> {

        private final String mSetID;
        private DashboardListAdapter listAdapter;


        CancelInvitationTask(String setID,  DashboardListAdapter listAdapter) {
            this.mSetID = setID;
            this.listAdapter = listAdapter;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            try {
                URL url;
//                url = new URL("https://www.pente.org/gameServer/tb/cancelInvitation?mobile=&command=Cancel&sid=" + mSetID);
                url = new URL("https://www.pente.org/gameServer/tb/cancelInvitation?mobile=&command=Cancel&sid=" + mSetID
                        +"&name2="+PentePlayer.mPlayerName+"&password2="+ PentePlayer.mPassword);
                if (development) {
                    url = new URL("https://development.pente.org/gameServer/tb/cancelInvitation?mobile=&command=Cancel&sid=" + mSetID
                            +"&name2="+PentePlayer.mPlayerName+"&password2="+ PentePlayer.mPassword);
                }
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

                StringBuilder output = new StringBuilder();
                BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                System.out.println("cancel invitation output===============" + br);
                String line = "";
                while((line = br.readLine()) != null ) {
                    output.append(line + System.getProperty("line.separator"));
                }
                br.close();

                output.append(System.getProperty("line.separator") + "Response " + System.getProperty("line.separator") + System.getProperty("line.separator"));

//                System.out.println(output);


            } catch (IOException e1) {
                e1.printStackTrace();
                return  false;
            }

            // TODO: register the new account here.
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            loadPlayer(listAdapter, loadAvatars, showOnlyTB);
        }

        @Override
        protected void onCancelled() {
        }
    }

}