package be.submanifold.pentelive;

import android.os.AsyncTask;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;

/**
 * Created by waliedothman on 10/04/16.
 */
public class PentePlayer implements Parcelable {

    public static String mPlayerName;
    public static String mPassword;
    public static Boolean mShowAds;
    private Boolean mSubscriber;
    List<Game> mInvitations;
    List<Game>  mSentInvitations;
    List<Game>  mActiveGames;
    List<Game>  mNonActiveGames;
    List<Game>  mPublicInvitations;
    List<Message> mMessages;
    List<RatingStat> mRatingStats;


    List<Tournament> mTournaments;

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
        this.mShowAds = true;
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


    private void populatePlayer(String dashString) {
        if (dashString == null) {
            return;
        }
        if (dashString.indexOf("No Ads") > -1 && dashString.indexOf("No Ads") < 30) {
            this.mShowAds = false;
        }
        if (dashString.indexOf("tb GamesLimit") > -1 || dashString.indexOf("tb GamesLimit") > 30) {
            this.mSubscriber = false;
        } else {
            this.mSubscriber = true;
        }
        String[] dashLines = dashString.split("\n");
        String[] dashLine;
        int idx = 0;
        while (idx < dashLines.length && dashLines[idx].indexOf("Rating Stats") == -1) {
            idx += 1;
        }
        this.mRatingStats.clear();
        RatingStat ratingStat;
        if (idx < dashLines.length && dashLines[idx].indexOf("Rating Stats") == 0) {
            idx += 1;
            while (idx < dashLines.length && dashLines[idx].indexOf("Invitations received") == -1) {
                dashLine = dashLines[idx].split(";");
                idx += 1;
                if (dashLine.length < 4) {
                    continue;
                }
                ratingStat = new RatingStat(dashLine[0], dashLine[1], dashLine[4], dashLine[2], dashLine[3]);
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

    public void loadPlayer(DashboardListAdapter listAdapter) {
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
                URL url = new URL("https://www.pente.org/gameServer/mobile/index.jsp?name="+mUsername+"&password="+mPassword);
//                url = new URL("https://development.pente.org/gameServer/mobile/index.jsp?name="+mUsername+"&password="+mPassword);
                HttpURLConnection connection = (HttpURLConnection)url.openConnection();
                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    System.out.println("response code for loadplayer was " + responseCode);
                    return false;
                }

                StringBuilder output = new StringBuilder();
                BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                System.out.println("output===============" + br);
                String line = "";
                while((line = br.readLine()) != null ) {
                    output.append(line + "\n");
                }
                br.close();

//                System.out.println(output);

                String dashboardString = output.toString();
                if (dashboardString.indexOf("Invalid name or password, please try again.") != -1) {
                    return false;
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
            listAdapter.updateList();
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
                    url = new URL("https://www.pente.org/gameServer/tb/replyInvitation?mobile=&inviteeMessage=&command=Accept&sid=" + mSetID);
                } else {
                    url = new URL("https://www.pente.org/gameServer/tb/replyInvitation?mobile=&inviteeMessage=&command=Decline&sid=" + mSetID);
                }
                System.out.println("accepting : " + url);

                HttpURLConnection connection = (HttpURLConnection)url.openConnection();
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
            loadPlayer(listAdapter);
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
                url = new URL("https://www.pente.org/gameServer/tb/cancelInvitation?mobile=&command=Cancel&sid=" + mSetID);
                HttpURLConnection connection = (HttpURLConnection)url.openConnection();
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
            loadPlayer(listAdapter);
        }

        @Override
        protected void onCancelled() {
        }
    }

}