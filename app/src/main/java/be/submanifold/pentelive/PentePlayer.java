package be.submanifold.pentelive;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.webkit.CookieManager;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by waliedothman on 10/04/16.
 */
public class PentePlayer implements Parcelable {

    public static Boolean development = false;

    public static String mPlayerName = "";
    public static String mPassword = "";
    public static Boolean mShowAds = true;
    public static Boolean mSubscriber = false;
    public static Boolean dbAccess = false;
    private List<Game> mInvitations;
    private List<Game> mSentInvitations;
    private List<Game> mActiveGames;
    private List<Game> mNonActiveGames;
    private List<Game> mPublicInvitations;
    private List<Message> mMessages;
    private final List<RatingStat> mRatingStats;
    private List<KingOfTheHill> mHills;
    private List<Tournament> mTournaments;
    public static int myColor = 0;

    public static List<String> pendingAvatarChecks;
    public static Map<String, Bitmap> avatars;

    public static Map<String, String> onlinePlayerNames;

    public static boolean loadAvatars = false;
    public static boolean showOnlyTB = false;
    public static Boolean emailMe = false;
    public static Boolean personalizeAds = false;

    private int livePlayers;

    public int getLivePlayers() {
        return this.livePlayers;
    }

    private int onlineFollowingers;

    public int getOnlineFollowingers() {
        return onlineFollowingers;
    }

    private int tbRatings, tbHills;

    public int getTbRatings() {
        return tbRatings;
    }

    public int gettbHills() {
        return tbHills;
    }

    public PentePlayer(String playerName, String password) {
        mPlayerName = playerName;
        mPassword = password;
        this.mInvitations = new ArrayList<Game>();
        this.mSentInvitations = new ArrayList<Game>();
        this.mActiveGames = new ArrayList<Game>();
        this.mNonActiveGames = new ArrayList<Game>();
        this.mPublicInvitations = new ArrayList<Game>();
        this.mMessages = new ArrayList<Message>();
        this.mRatingStats = new ArrayList<RatingStat>();
        this.mTournaments = new ArrayList<Tournament>();
        this.mHills = new ArrayList<KingOfTheHill>();
        mShowAds = true;
        emailMe = true;
        personalizeAds = false;
        pendingAvatarChecks = new ArrayList<String>();
        avatars = new HashMap<String, Bitmap>();
        myColor = 0;
        livePlayers = 0;
        tbRatings = 0;
        tbHills = 0;
        onlinePlayerNames = new HashMap<>();
    }

    public String getPlayerName() {
        return mPlayerName;
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

    public List<Tournament> getTournaments() {
        return this.mTournaments;
    }

    public Boolean isSubscriber() {
        return mSubscriber;
    }

    public List<RatingStat> getRatingStats() {
        return mRatingStats;
    }

    public List<KingOfTheHill> getHills() {
        return mHills;
    }

    public static Boolean hasDBAccess() {
        return dbAccess;
    }

    public static void markIfOnline(String name, SpannableStringBuilder attributedString) {
        if (onlinePlayerNames != null && onlinePlayerNames.containsKey(name)) {
            attributedString.append("  \u25CF");
            ForegroundColorSpan fcs = new ForegroundColorSpan(Color.GREEN);
            attributedString.setSpan(fcs, attributedString.length() - 1, attributedString.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        }
    }

    public static void setOnlinePlayerNames(Map<String, String> onlinePlayerNames) {
        PentePlayer.onlinePlayerNames = onlinePlayerNames;
    }

    private void populateFromJson(JsonModels.IndexResponse json) {
        if (json == null || json.player == null) {
            return;
        }

        myColor = json.player.color;
        mShowAds = json.player.showAds;
        if (development) {
            mShowAds = true;
        }
        mSubscriber = json.player.subscriber;
        this.livePlayers = json.player.livePlayers;
        dbAccess = json.player.dbAccess;
        emailMe = json.player.emailMe;
        this.onlineFollowingers = json.player.onlineFollowing;
        personalizeAds = json.player.personalizeAds;
        PrefUtils.saveBooleanToPrefs(MyApplication.getContext(), PrefUtils.PREFS_PERSONALIZEDADS_KEY, personalizeAds);

        // King of the Hill
        List<KingOfTheHill> newKOTH = new ArrayList<>();
        tbHills = 0;
        if (json.kingOfTheHill != null) {
            for (JsonModels.IndexResponse.KothEntry entry : json.kingOfTheHill) {
                KingOfTheHill hill = new KingOfTheHill("", String.valueOf(entry.numPlayers),
                        entry.kingName, entry.amIMember, entry.iAmKing, entry.canChallenge,
                        String.valueOf(entry.gameId));
                if (hill.getGameId() > 50) {
                    tbHills += 1;
                }
                newKOTH.add(hill);
            }
        }
        this.mHills = newKOTH;

        // Rating Stats
        this.mRatingStats.clear();
        tbRatings = 0;
        if (json.ratingStats != null) {
            for (JsonModels.IndexResponse.RatingStatEntry entry : json.ratingStats) {
                RatingStat ratingStat = new RatingStat(entry.gameName, String.valueOf(entry.rating),
                        entry.lastGameDate, String.valueOf(entry.totalGames),
                        String.valueOf(entry.tourneyWinner), String.valueOf(entry.gameId));
                if (ratingStat.getGameId() > 50) {
                    tbRatings += 1;
                }
                this.mRatingStats.add(ratingStat);
            }
        }

        // Invitations received
        List<Game> newInvitations = new ArrayList<>();
        if (json.invitationsReceived != null) {
            for (JsonModels.IndexResponse.InvitationEntry entry : json.invitationsReceived) {
                Game game = new Game(String.valueOf(entry.setId), null, entry.gameName,
                        entry.opponentName, String.valueOf(entry.opponentRating), entry.color,
                        String.valueOf(entry.daysPerMove), entry.rated, null,
                        String.valueOf(entry.opponentColor), String.valueOf(entry.opponentTourneyWinner));
                if (loadAvatars && game.getNameColor() != 0) {
                    addUserAvatar(game.getOpponentName());
                }
                newInvitations.add(game);
            }
        }
        this.mInvitations = newInvitations;

        // Invitations sent
        List<Game> newSentInvitations = new ArrayList<>();
        if (json.invitationsSent != null) {
            for (JsonModels.IndexResponse.InvitationEntry entry : json.invitationsSent) {
                Game game = new Game(String.valueOf(entry.setId), null, entry.gameName,
                        entry.opponentName, String.valueOf(entry.opponentRating), entry.color,
                        String.valueOf(entry.daysPerMove), entry.rated, null,
                        String.valueOf(entry.opponentColor), String.valueOf(entry.opponentTourneyWinner));
                if (loadAvatars && game.getNameColor() != 0) {
                    addUserAvatar(game.getOpponentName());
                }
                newSentInvitations.add(game);
            }
        }
        this.mSentInvitations = newSentInvitations;

        // Active games - my turn
        List<Game> newActive = new ArrayList<>();
        if (json.activeGamesMyTurn != null) {
            for (JsonModels.IndexResponse.GameEntry entry : json.activeGamesMyTurn) {
                Game game = new Game(String.valueOf(entry.gid), null, entry.gameName,
                        entry.opponentName, String.valueOf(entry.opponentRating), entry.color,
                        entry.timeLeft, entry.rated, null,
                        String.valueOf(entry.opponentColor), String.valueOf(entry.opponentTourneyWinner));
                if (loadAvatars && game.getNameColor() != 0) {
                    addUserAvatar(game.getOpponentName());
                }
                newActive.add(game);
            }
        }
        this.mActiveGames = newActive;

        // Active games - opponent's turn
        List<Game> newNonActive = new ArrayList<>();
        if (json.activeGamesOpponentTurn != null) {
            for (JsonModels.IndexResponse.GameEntry entry : json.activeGamesOpponentTurn) {
                Game game = new Game(String.valueOf(entry.gid), null, entry.gameName,
                        entry.opponentName, String.valueOf(entry.opponentRating), entry.color,
                        entry.timeLeft, entry.rated, null,
                        String.valueOf(entry.opponentColor), String.valueOf(entry.opponentTourneyWinner));
                if (loadAvatars && game.getNameColor() != 0) {
                    addUserAvatar(game.getOpponentName());
                }
                newNonActive.add(game);
            }
        }
        this.mNonActiveGames = newNonActive;

        // Open invitation games
        List<Game> newPublic = new ArrayList<>();
        if (json.openInvitationGames != null) {
            for (JsonModels.IndexResponse.OpenInvitationEntry entry : json.openInvitationGames) {
                Game game = new Game(String.valueOf(entry.setId), null, entry.gameName,
                        entry.inviterName, String.valueOf(entry.inviterRating), entry.color,
                        String.valueOf(entry.daysPerMove), entry.rated, null,
                        String.valueOf(entry.inviterColor), String.valueOf(entry.inviterTourneyWinner));
                if (loadAvatars && game.getNameColor() != 0) {
                    addUserAvatar(game.getOpponentName());
                }
                newPublic.add(game);
            }
        }
        this.mPublicInvitations = newPublic;

        // Messages
        List<Message> newMessages = new ArrayList<>();
        if (json.messages != null) {
            for (JsonModels.IndexResponse.MessageEntry entry : json.messages) {
                // Message(messageID, author, subject, timeStamp, unread, nameColor, crown)
                Message message = new Message(String.valueOf(entry.mid), entry.from, entry.subject,
                        entry.date, entry.read ? "0" : "1",
                        String.valueOf(entry.fromColor), String.valueOf(entry.fromTourneyWinner));
                if (loadAvatars && message.getNameColor() != 0) {
                    addUserAvatar(message.getAuthor());
                }
                newMessages.add(message);
            }
        }
        this.mMessages = newMessages;

        // Tournaments
        List<Tournament> newTournament = new ArrayList<>();
        if (json.tournaments != null) {
            for (JsonModels.IndexResponse.TournamentEntry entry : json.tournaments) {
                // Tournament(game, name, tournamentID, round, tournamentState, date)
                Tournament tournament = new Tournament(entry.gameName, entry.name,
                        String.valueOf(entry.eventId), String.valueOf(entry.numRounds),
                        String.valueOf(entry.status), entry.date);
                newTournament.add(tournament);
            }
        }
        this.mTournaments = newTournament;

        // Online players
        if (onlinePlayerNames == null) {
            onlinePlayerNames = new HashMap<>();
        }
        onlinePlayerNames.clear();
        if (json.onlinePlayers != null) {
            for (String name : json.onlinePlayers) {
                onlinePlayerNames.put(name, "");
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
        showOnlyTB = showOnlyTB;
        loadAvatars = loadAvatars;
        if (mPassword == null || mPlayerName == null) {
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
        byte emailVal = in.readByte();
        emailMe = emailVal == 0x02 ? null : emailVal != 0x00;
        byte personalizeAdsVal = in.readByte();
        personalizeAds = personalizeAdsVal == 0x02 ? null : personalizeAdsVal != 0x00;
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
        onlineFollowingers = in.readInt();
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
        if (emailMe == null) {
            dest.writeByte((byte) (0x02));
        } else {
            dest.writeByte((byte) (emailMe ? 0x01 : 0x00));
        }
        if (personalizeAds == null) {
            dest.writeByte((byte) (0x02));
        } else {
            dest.writeByte((byte) (personalizeAds ? 0x01 : 0x00));
        }
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
        dest.writeInt(onlineFollowingers);
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

        private final String mUsername;
        private Bitmap avatar;

        LoadAvatarTask(String username) {
            this.mUsername = username;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            try {
                avatar = null;
                URL url = new URL("https://www.pente.org/gameServer/avatar?name=" + mUsername);
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
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
                return false;
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
        private final DashboardListAdapter listAdapter;

        LoadPlayerTask(String username, String password, DashboardListAdapter listAdapter) {
            this.mUsername = username;
            this.mPassword = password;
            this.listAdapter = listAdapter;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            try {
                URL url = new URL("https://www.pente.org/gameServer/mobile/json/index.jsp?name=" + mUsername + "&password=" + mPassword + "&name2=" + PentePlayer.mPlayerName + "&password2=" + PentePlayer.mPassword);
                if (development) {
                    url = new URL("https://10.0.2.2/gameServer/mobile/json/index.jsp?name=" + mUsername + "&password=" + mPassword + "&name2=" + PentePlayer.mPlayerName + "&password2=" + PentePlayer.mPassword);
                }

                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                String cookies = CookieManager.getInstance().getCookie("https://www.pente.org/");
                if (cookies != null) {
                    String[] splitCookie = cookies.split(";");
                    String cookieStr = "";
                    for (String item : splitCookie) {
                        if (item.contains("name2") || item.contains("password2") || item.contains("domain")) {
                            cookieStr += item + ";";
                        }
                    }
                    connection.setRequestProperty("Cookie", cookieStr);
                }
                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    url = new URL("https://www.pente.org/gameServer/login.jsp?mobile=&name2=" + PentePlayer.mPlayerName + "&password2=" + PentePlayer.mPassword);
                    if (development) {
                        url = new URL("https://10.0.2.2/gameServer/login.jsp?mobile=&name2=" + PentePlayer.mPlayerName + "&password2=" + PentePlayer.mPassword);
                    }
                    connection = (HttpsURLConnection) url.openConnection();
                    responseCode = connection.getResponseCode();
                    if (responseCode != 200) {
                        System.out.println("Logging back in failed");
                        return false;
                    } else {
                        url = new URL("https://www.pente.org/gameServer/mobile/json/index.jsp?name=" + mUsername + "&password=" + mPassword);
                        if (development) {
                            url = new URL("https://10.0.2.2/gameServer/mobile/json/index.jsp?name=" + mUsername + "&password=" + mPassword + "&name2=" + PentePlayer.mPlayerName + "&password2=" + PentePlayer.mPassword);
                        }
                        connection = (HttpsURLConnection) url.openConnection();
                        responseCode = connection.getResponseCode();
                        if (responseCode != 200) {
                            System.out.println("Logging back in and retrieving game failed");
                            return false;
                        }
                    }
                }

                StringBuilder output = new StringBuilder();
                BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line = "";
                while ((line = br.readLine()) != null) {
                    output.append(line);
                }
                br.close();

                Gson gson = new Gson();
                JsonModels.IndexResponse json = gson.fromJson(output.toString(), JsonModels.IndexResponse.class);
                if (json == null || json.player == null) {
                    url = new URL("https://www.pente.org/gameServer/login.jsp?mobile=&name2=" + PentePlayer.mPlayerName + "&password2=" + PentePlayer.mPassword);
                    if (development) {
                        url = new URL("https://10.0.2.2/gameServer/login.jsp?mobile=&name2=" + PentePlayer.mPlayerName + "&password2=" + PentePlayer.mPassword);
                    }
                    connection = (HttpsURLConnection) url.openConnection();
                    responseCode = connection.getResponseCode();
                    if (responseCode != 200) {
                        System.out.println("Logging back in failed");
                        return false;
                    }
                    url = new URL("https://www.pente.org/gameServer/mobile/json/index.jsp?name=" + mUsername + "&password=" + mPassword);
                    if (development) {
                        url = new URL("https://10.0.2.2/gameServer/mobile/json/index.jsp?name=" + mUsername + "&password=" + mPassword + "&name2=" + PentePlayer.mPlayerName + "&password2=" + PentePlayer.mPassword);
                    }
                    connection = (HttpsURLConnection) url.openConnection();
                    responseCode = connection.getResponseCode();
                    if (responseCode != 200) {
                        System.out.println("Logging back in and retrieving game failed");
                        return false;
                    }
                    output = new StringBuilder();
                    br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    line = "";
                    while ((line = br.readLine()) != null) {
                        output.append(line);
                    }
                    br.close();
                    json = gson.fromJson(output.toString(), JsonModels.IndexResponse.class);
                    if (json == null || json.player == null) {
                        return false;
                    }
                }

                populateFromJson(json);

            } catch (IOException e1) {
                e1.printStackTrace();
                return false;
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
        private final boolean accept;
        private final DashboardListAdapter listAdapter;


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
                            + "&name2=" + PentePlayer.mPlayerName + "&password2=" + PentePlayer.mPassword);
                } else {
//                    url = new URL("https://www.pente.org/gameServer/tb/replyInvitation?mobile=&inviteeMessage=&command=Decline&sid=" + mSetID);
                    url = new URL("https://www.pente.org/gameServer/tb/replyInvitation?mobile=&inviteeMessage=&command=Decline&sid=" + mSetID
                            + "&name2=" + PentePlayer.mPlayerName + "&password2=" + PentePlayer.mPassword);
                }
                if (development) {
                    if (this.accept) {
//                    url = new URL("https://www.pente.org/gameServer/tb/replyInvitation?mobile=&inviteeMessage=&command=Accept&sid=" + mSetID);
                        url = new URL("https://10.0.2.2/gameServer/tb/replyInvitation?mobile=&inviteeMessage=&command=Accept&sid=" + mSetID
                                + "&name2=" + PentePlayer.mPlayerName + "&password2=" + PentePlayer.mPassword);
                    } else {
//                    url = new URL("https://www.pente.org/gameServer/tb/replyInvitation?mobile=&inviteeMessage=&command=Decline&sid=" + mSetID);
                        url = new URL("https://10.0.2.2/gameServer/tb/replyInvitation?mobile=&inviteeMessage=&command=Decline&sid=" + mSetID
                                + "&name2=" + PentePlayer.mPlayerName + "&password2=" + PentePlayer.mPassword);
                    }
                }

//                System.out.println("accepting : " + url);

                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                String cookies = CookieManager.getInstance().getCookie("https://www.pente.org/");
                if (cookies != null) {
                    String[] splitCookie = cookies.split(";");
                    String cookieStr = "";
                    for (String item : splitCookie) {
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
                while ((line = br.readLine()) != null) {
                    output.append(line + System.getProperty("line.separator"));
                }
                br.close();

                output.append(System.getProperty("line.separator") + "Response " + System.getProperty("line.separator") + System.getProperty("line.separator"));

                System.out.println(output);


            } catch (IOException e1) {
                e1.printStackTrace();
                return false;
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
        private final DashboardListAdapter listAdapter;


        CancelInvitationTask(String setID, DashboardListAdapter listAdapter) {
            this.mSetID = setID;
            this.listAdapter = listAdapter;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            try {
                URL url;
//                url = new URL("https://www.pente.org/gameServer/tb/cancelInvitation?mobile=&command=Cancel&sid=" + mSetID);
                url = new URL("https://www.pente.org/gameServer/tb/cancelInvitation?mobile=&command=Cancel&sid=" + mSetID
                        + "&name2=" + PentePlayer.mPlayerName + "&password2=" + PentePlayer.mPassword);
                if (development) {
                    url = new URL("https://10.0.2.2/gameServer/tb/cancelInvitation?mobile=&command=Cancel&sid=" + mSetID
                            + "&name2=" + PentePlayer.mPlayerName + "&password2=" + PentePlayer.mPassword);
                }
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                String cookies = CookieManager.getInstance().getCookie("https://www.pente.org/");
                if (cookies != null) {
                    String[] splitCookie = cookies.split(";");
                    String cookieStr = "";
                    for (String item : splitCookie) {
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
                while ((line = br.readLine()) != null) {
                    output.append(line + System.getProperty("line.separator"));
                }
                br.close();

                output.append(System.getProperty("line.separator") + "Response " + System.getProperty("line.separator") + System.getProperty("line.separator"));

//                System.out.println(output);


            } catch (IOException e1) {
                e1.printStackTrace();
                return false;
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